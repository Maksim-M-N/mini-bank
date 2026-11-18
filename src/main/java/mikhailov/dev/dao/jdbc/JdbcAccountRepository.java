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
import java.util.List;
import java.util.Optional;

public class JdbcAccountRepository implements AccountRepository {

    private final JdbcConnectionFactory connectionFactory;

    private final static String SQL_FIND_BY_ID = """
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
                WHERE a.id = ?;
            """;

    public JdbcAccountRepository(JdbcConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Optional<Account> findById(Long id) {

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_FIND_BY_ID)) {

            statement.setLong(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
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

                    Account account = Account.restore(
                            result.getLong("account_id"),
                            result.getString("account_number"),
                            CurrencyCode.valueOf(result.getString("currency")),
                            AccountStatus.valueOf(result.getString("status")),
                            result.getBigDecimal("balance"),
                            result.getTimestamp("opened_at").toInstant(),
                            closedAt,
                            client
                    );

                    return Optional.of(account);
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Не удалось загрузить счёт с id=" + id, e);
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return Optional.empty();
    }

    @Override
    public List<Account> findAllByClientId(Long clientId) {
        return List.of();
    }

    @Override
    public List<Account> findActiveAccounts() {
        return List.of();
    }

    @Override
    public List<Account> findAccountsWithBalanceGreaterThan(BigDecimal amount) {
        return List.of();
    }

    @Override
    public List<Account> findAccountsByCurrency(String currency) {
        return List.of();
    }

    @Override
    public List<Account> findBlockedAccounts() {
        return List.of();
    }

    @Override
    public void updateBalance(Long accountId, BigDecimal newBalance) {

    }

    @Override
    public void updateStatus(Long accountId, AccountStatus status) {

    }
}
