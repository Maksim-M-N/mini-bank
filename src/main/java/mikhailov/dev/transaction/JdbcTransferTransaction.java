package mikhailov.dev.transaction;

import mikhailov.dev.config.JdbcConnectionFactory;
import mikhailov.dev.dao.jdbc.JdbcAccountRepository;
import mikhailov.dev.dao.jdbc.JdbcTransferRepository;
import mikhailov.dev.repository.AccountRepository;
import mikhailov.dev.repository.TransferRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.BiConsumer;

public class JdbcTransferTransaction implements TransferTransaction{
    private final JdbcConnectionFactory connectionFactory;

    public JdbcTransferTransaction(JdbcConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void inTransaction(BiConsumer<AccountRepository, TransferRepository> action) {
        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try {
                AccountRepository accountRepository = new JdbcAccountRepository(connectionFactory, connection);
                TransferRepository transferRepository = new JdbcTransferRepository(connectionFactory, connection);

                action.accept(accountRepository, transferRepository);

                connection.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }

                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при выполнении транзакции перевода", e);
        }
    }
}
