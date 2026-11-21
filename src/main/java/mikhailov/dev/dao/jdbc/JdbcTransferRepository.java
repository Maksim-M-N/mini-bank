package mikhailov.dev.dao.jdbc;

import mikhailov.dev.config.JdbcConnectionFactory;
import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.Client;
import mikhailov.dev.domain.Transfer;
import mikhailov.dev.domain.enums.AccountStatus;
import mikhailov.dev.domain.enums.CurrencyCode;
import mikhailov.dev.domain.enums.TransferStatus;
import mikhailov.dev.repository.TransferRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JdbcTransferRepository implements TransferRepository {
    private static final String BASE_SELECT = """
            SELECT
                t.id AS transfer_id,
                t.amount AS transfer_amount,
                t.status AS transfer_status,
                t.comment AS transfer_comment,
                t.created_at AS transfer_created_at,
            
                fa.id AS from_account_id,
                fa.account_number AS from_account_number,
                fa.currency AS from_account_currency,
                fa.status AS from_account_status,
                fa.balance AS from_account_balance,
                fa.opened_at AS from_account_opened_at,
                fa.closed_at AS from_account_closed_at,
            
                fc.id AS from_client_id,
                fc.full_name AS from_client_full_name,
                fc.phone AS from_client_phone,
                fc.created_at AS from_client_created_at,
            
                ta.id AS to_account_id,
                ta.account_number AS to_account_number,
                ta.currency AS to_account_currency,
                ta.status AS to_account_status,
                ta.balance AS to_account_balance,
                ta.opened_at AS to_account_opened_at,
                ta.closed_at AS to_account_closed_at,
            
                tc.id AS to_client_id,
                tc.full_name AS to_client_full_name,
                tc.phone AS to_client_phone,
                tc.created_at AS to_client_created_at
            FROM transfers AS t
            JOIN accounts AS fa ON fa.id = t.from_account_id
            JOIN accounts AS ta ON ta.id = t.to_account_id
            JOIN clients AS fc ON fc.id = fa.client_id
            JOIN clients AS tc ON tc.id = ta.client_id
            """;

    private static final String GET_TOTAL_OUTGOING_AMOUNT = """
            SELECT COALESCE(SUM(t.amount), 0) AS total_amount
            FROM transfers AS t
            WHERE t.from_account_id = ?
              AND t.status = 'SUCCESS';
            """;

    private static final String GET_TOTAL_INCOMING_AMOUNT = """
            SELECT COALESCE(SUM(t.amount), 0) AS total_amount
            FROM transfers AS t
            WHERE t.to_account_id = ?
              AND t.status = 'SUCCESS';
            """;

    private static final String ORDER_BY = """
            ORDER BY t.created_at DESC, t.id DESC;
            """;

    private static final String FIND_BY_ACCOUNT_ID = BASE_SELECT + """
            WHERE t.from_account_id = ? OR t.to_account_id = ?
            """ + ORDER_BY;

    private static final String FIND_OUTGOING_TRANSFERS = BASE_SELECT + """
            WHERE t.from_account_id = ?
            """ + ORDER_BY;

    private static final String FIND_INCOMING_TRANSFERS = BASE_SELECT + """
            WHERE t.to_account_id = ?
            """ + ORDER_BY;

    private static final String FIND_SUCCESSFUL_TRANSFERS = BASE_SELECT + """
            WHERE t.status = 'SUCCESS'
            """ + ORDER_BY;

    private static final String FIND_TRANSFERS_WITHOUT_COMMENT = BASE_SELECT + """
            WHERE t.comment IS NULL
            """ + ORDER_BY;

    private static final String FIND_LARGE_TRANSFERS = BASE_SELECT + """
            WHERE t.amount > ?
            """ + ORDER_BY;

    private static final String SAVE = """
            INSERT INTO transfers (amount, status, comment, from_account_id, to_account_id)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, amount, created_at;
            """;


    private final JdbcConnectionFactory connectionFactory;

    public JdbcTransferRepository(JdbcConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Transfer save(Transfer transfer) {

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE)) {

            statement.setBigDecimal(1, transfer.getAmount());
            statement.setString(2, transfer.getStatus().name());
            statement.setString(3, transfer.getComment());
            statement.setLong(4, transfer.getFromAccount().getId());
            statement.setLong(5, transfer.getToAccount().getId());

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return Transfer.restore(
                            result.getLong("id"),
                            result.getBigDecimal("amount"),
                            transfer.getStatus(),
                            transfer.getComment(),
                            result.getTimestamp("created_at").toInstant(),
                            transfer.getFromAccount(),
                            transfer.getToAccount()
                    );
                }

                throw new IllegalStateException(
                        "Не удалось получить данные сохранённого перевода"
                );
            }
        } catch (SQLException e) {
                throw new RuntimeException("Не удалось сохранить перевод", e);
        }
    }

    @Override
    public List<Transfer> findByAccountId(Long accountId) {
        return queryTransfers(
                FIND_BY_ACCOUNT_ID,
                "Не удалось получить историю переводов по счёту с ID: " + accountId,
                statement -> {
                    statement.setLong(1, accountId);
                    statement.setLong(2, accountId);
                }
        );
    }

    @Override
    public List<Transfer> findOutgoingTransfers(Long accountId) {
        return queryTransfers(
                FIND_OUTGOING_TRANSFERS,
                "Не удалось получить исходящие переводы по счёту с ID: " + accountId,
                statement -> statement.setLong(1, accountId)
        );
    }

    @Override
    public List<Transfer> findIncomingTransfers(Long accountId) {
        return queryTransfers(
                FIND_INCOMING_TRANSFERS,
                "Не удалось получить входящие переводы по счёту с ID: " + accountId,
                statement -> statement.setLong(1, accountId)
        );
    }

    @Override
    public List<Transfer> findSuccessfulTransfers() {
        return queryTransfers(
                FIND_SUCCESSFUL_TRANSFERS,
                "Не удалось получить список успешных переводов",
                statement -> {
                }
        );
    }

    @Override
    public List<Transfer> findTransfersWithoutComment() {
        return queryTransfers(
                FIND_TRANSFERS_WITHOUT_COMMENT,
                "Не удалось получить список переводов без комментария",
                statement -> {
                }
        );
    }

    @Override
    public List<Transfer> findLargeTransfers(BigDecimal minAmount) {
        return queryTransfers(
                FIND_LARGE_TRANSFERS,
                "Не удалось получить переводы с суммой больше " + minAmount,
                statement -> statement.setBigDecimal(1, minAmount)
        );
    }

    @Override
    public BigDecimal getTotalOutgoingAmount(Long accountId) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_TOTAL_OUTGOING_AMOUNT)) {

            statement.setLong(1, accountId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getBigDecimal("total_amount");
                }

                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Не удалось получить общую сумму исходящих переводов по счёту с ID: " + accountId, e);
        }
    }

    @Override
    public BigDecimal getTotalIncomingAmount(Long accountId) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_TOTAL_INCOMING_AMOUNT)) {

            statement.setLong(1, accountId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getBigDecimal("total_amount");
                }

                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Не удалось получить общую сумму входящих переводов по счёту с ID: " + accountId, e);
        }
    }

    private List<Transfer> queryTransfers(String sql, String errorMessage, StatementSetter setter) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setter.set(statement);

            try (ResultSet result = statement.executeQuery()) {
                List<Transfer> transfers = new ArrayList<>();

                while (result.next()) {
                    transfers.add(mapTransfer(result));
                }

                return transfers;
            }


        } catch (SQLException e) {
            throw new RuntimeException(errorMessage, e);
        }
    }

    private Transfer mapTransfer(ResultSet result) throws SQLException {

        Timestamp from_closedAtTimestamp = result.getTimestamp("from_account_closed_at");
        Timestamp to_closedAtTimestamp = result.getTimestamp("to_account_closed_at");

        Instant fromClosedAt = null;
        Instant toClosedAt = null;

        if (from_closedAtTimestamp != null) {
            fromClosedAt = from_closedAtTimestamp.toInstant();
        }

        if (to_closedAtTimestamp != null) {
            toClosedAt = to_closedAtTimestamp.toInstant();
        }

        Client fromClient = Client.restore(
                result.getLong("from_client_id"),
                result.getString("from_client_full_name"),
                result.getString("from_client_phone"),
                result.getTimestamp("from_client_created_at").toInstant()
        );

        Client toClient = Client.restore(
                result.getLong("to_client_id"),
                result.getString("to_client_full_name"),
                result.getString("to_client_phone"),
                result.getTimestamp("to_client_created_at").toInstant()
        );

        Account fromAccount = Account.restore(
                result.getLong("from_account_id"),
                result.getString("from_account_number"),
                CurrencyCode.valueOf(result.getString("from_account_currency")),
                AccountStatus.valueOf(result.getString("from_account_status")),
                result.getBigDecimal("from_account_balance"),
                result.getTimestamp("from_account_opened_at").toInstant(),
                fromClosedAt,
                fromClient);


        Account toAccount = Account.restore(
                result.getLong("to_account_id"),
                result.getString("to_account_number"),
                CurrencyCode.valueOf(result.getString("to_account_currency")),
                AccountStatus.valueOf(result.getString("to_account_status")),
                result.getBigDecimal("to_account_balance"),
                result.getTimestamp("to_account_opened_at").toInstant(),
                toClosedAt,
                toClient);

        return Transfer.restore(
                result.getLong("transfer_id"),
                result.getBigDecimal("transfer_amount"),
                TransferStatus.valueOf(result.getString("transfer_status")),
                result.getString("transfer_comment"),
                result.getTimestamp("transfer_created_at").toInstant(),
                fromAccount,
                toAccount
        );
    }

    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement statement) throws SQLException;
    }
}
