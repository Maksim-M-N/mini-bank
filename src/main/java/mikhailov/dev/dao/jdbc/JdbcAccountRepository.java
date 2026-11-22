package mikhailov.dev.dao.jdbc;

import mikhailov.dev.config.JdbcConnectionFactory;
import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.Client;
import mikhailov.dev.domain.enums.AccountStatus;
import mikhailov.dev.domain.enums.CurrencyCode;
import mikhailov.dev.repository.AccountRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAccountRepository implements AccountRepository {
    private static final String BASE_SELECT = """
        SELECT a.id AS account_id,
               a.account_number,
               a.currency,
               a.status,
               a.balance,
               a.opened_at,
               a.closed_at,
               c.id AS client_id,
               c.full_name AS client_full_name,
               c.phone AS client_phone,
               c.created_at AS client_created_at
        FROM accounts AS a
        JOIN clients AS c ON a.client_id = c.id
        """;

    private static final String ORDER_BY = """
        ORDER BY a.id;
        """;

    private static final String FIND_BY_ID = BASE_SELECT + """
        WHERE a.id = ?;
        """;

    private static final String FIND_BY_ACCOUNT_NUMBER = BASE_SELECT + """
        WHERE a.account_number = ?
        """;

    private static final String FIND_BY_ACCOUNT_NUMBER_FOR_UPDATE = FIND_BY_ACCOUNT_NUMBER + """
        FOR UPDATE OF a
        """;

    private static final String FIND_ALL_BY_CLIENT_ID = BASE_SELECT + """
        WHERE a.client_id = ?
        """ + ORDER_BY;

    private static final String FIND_ACTIVE_ACCOUNTS = BASE_SELECT + """
        WHERE a.status = 'ACTIVE'
        """ + ORDER_BY;

    private static final String FIND_ACCOUNTS_WITH_BALANCE_GREATER_THAN = BASE_SELECT + """
        WHERE a.balance > ?
        """ + ORDER_BY;

    private static final String FIND_ACCOUNTS_BY_CURRENCY = BASE_SELECT + """
        WHERE a.currency = ?
        """ + ORDER_BY;

    private static final String FIND_BLOCKED_ACCOUNTS = BASE_SELECT + """
        WHERE a.status = 'BLOCKED'
        """ + ORDER_BY;

    private static final String UPDATE_BALANCE = """
        UPDATE accounts
        SET balance = ?
        WHERE id = ?;
        """;

    private static final String UPDATE_STATUS = """
        UPDATE accounts
        SET status = ?,
            closed_at = CASE
                WHEN ? = 'CLOSED'
                    THEN COALESCE(closed_at, CURRENT_TIMESTAMP)
            END
        WHERE id = ? AND (? <> 'CLOSED' OR balance = 0);
        """;

    private final JdbcConnectionFactory connectionFactory;
    private final Connection transactionConnection;

    public JdbcAccountRepository(JdbcConnectionFactory connectionFactory) {
        this(connectionFactory, null);
    }

    public JdbcAccountRepository(JdbcConnectionFactory connectionFactory,
                                 Connection transactionConnection) {
        this.connectionFactory = connectionFactory;
        this.transactionConnection = transactionConnection;
    }

    @Override
    public Optional<Account> findById(Long id) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {

            statement.setLong(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapAccount(result));
                }

                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось загрузить счёт с id=" + id, e);
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        try {
            if (transactionConnection != null) {
                return executeFindByAccountNumber(transactionConnection, accountNumber);
            }

            try (Connection connection = connectionFactory.getConnection()) {
                return executeFindByAccountNumber(connection, accountNumber);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось загрузить счёт с номером:" + accountNumber, e);
        }
    }

    @Override
    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        if (transactionConnection == null) {
            throw new IllegalStateException("Блокировка счёта требует активной транзакции");
        }

        try {
            return executeFindByAccountNumberForUpdate(transactionConnection, accountNumber);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Не удалось получить счёт с блокировкой, номер счёта: " + accountNumber, e);
        }
    }

    private Optional<Account> executeFindByAccountNumberForUpdate(Connection connection,
                                                         String accountNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ACCOUNT_NUMBER_FOR_UPDATE)) {
            statement.setString(1, accountNumber);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapAccount(result));
                }

                return Optional.empty();
            }
        }
    }


    @Override
    public List<Account> findAllByClientId(Long clientId) {
        return queryAccounts(
                FIND_ALL_BY_CLIENT_ID,
                "Не удалось получить счета клиента с ID: " + clientId,
                statement -> statement.setLong(1, clientId)
        );
    }

    @Override
    public List<Account> findActiveAccounts() {
        return queryAccounts(
                FIND_ACTIVE_ACCOUNTS,
                "Не удалось получить список активных счетов",
                statement -> {}
        );
    }

    @Override
    public List<Account> findAccountsWithBalanceGreaterThan(BigDecimal amount) {
        return queryAccounts(
                FIND_ACCOUNTS_WITH_BALANCE_GREATER_THAN,
                "Не удалось получить счета с балансом больше " + amount,
                statement -> statement.setBigDecimal(1, amount)
        );
    }

    @Override
    public List<Account> findAccountsByCurrency(CurrencyCode currency) {
        return queryAccounts(
                FIND_ACCOUNTS_BY_CURRENCY,
                "Не удалось получить счета в валюте " + currency,
                statement -> statement.setString(1, currency.name())
        );
    }

    @Override
    public List<Account> findBlockedAccounts() {
        return queryAccounts(
                FIND_BLOCKED_ACCOUNTS,
                "Не удалось получить список заблокированных счетов",
                statement -> {}
        );
    }

    @Override
    public void updateBalance(Long accountId, BigDecimal newBalance) {
        try {
            if (transactionConnection != null) {
                executeBalanceUpdate(transactionConnection, accountId, newBalance);
            } else {
                try (Connection connection = connectionFactory.getConnection()) {
                    executeBalanceUpdate(connection, accountId, newBalance);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось обновить баланс счёта с id=" + accountId, e);
        }
    }

    @Override
    public void updateStatus(Long accountId, AccountStatus status) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {

            statement.setString(1, status.name());
            statement.setString(2, status.name());
            statement.setLong(3, accountId);
            statement.setString(4, status.name());


            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                throw new IllegalStateException(
                        "Счёт с id=" + accountId
                                + " не найден или изменение статуса невозможно");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось обновить статус счёта с id=" + accountId + " на " + status, e);
        }
    }



    private Optional<Account> executeFindByAccountNumber(Connection connection,
                                                         String accountNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ACCOUNT_NUMBER)) {
            statement.setString(1, accountNumber);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapAccount(result));
                }

                return Optional.empty();
            }
        }
    }

    private void executeBalanceUpdate(Connection connection,
                                      Long accountId,
                                      BigDecimal newBalance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_BALANCE)) {
            statement.setBigDecimal(1, newBalance);
            statement.setLong(2, accountId);

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Счёт с id=" + accountId + " не найден");
            }
        }
    }

    private List<Account> queryAccounts(String sql,
                                        String errorMessage,
                                        StatementSetter setter) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setter.set(statement);

            try (ResultSet result = statement.executeQuery()) {
                List<Account> accounts = new ArrayList<>();

                while (result.next()) {
                    accounts.add(mapAccount(result));
                }

                return accounts;
            }
        } catch (SQLException e) {
            throw new RuntimeException(errorMessage, e);
        }
    }

    private Account mapAccount(ResultSet result) throws SQLException{
        Timestamp closedAtTimestamp = result.getTimestamp("closed_at");
        Instant closedAt = null;

        if (closedAtTimestamp != null) {
            closedAt = closedAtTimestamp.toInstant();
        }

        Client client = Client.restore(
                result.getLong("client_id"),
                result.getString("client_full_name"),
                result.getString("client_phone"),
                result.getTimestamp("client_created_at").toInstant()
        );

        return Account.restore(
                result.getLong("account_id"),
                result.getString("account_number"),
                CurrencyCode.valueOf(result.getString("currency")),
                AccountStatus.valueOf(result.getString("status")),
                result.getBigDecimal("balance"),
                result.getTimestamp("opened_at").toInstant(),
                closedAt,
                client
        );
    }

    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement statement) throws SQLException;
    }
}
