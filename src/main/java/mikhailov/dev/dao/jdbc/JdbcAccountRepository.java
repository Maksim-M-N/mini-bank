package mikhailov.dev.dao.jdbc;

import lombok.extern.slf4j.Slf4j;
import mikhailov.dev.config.JdbcConnectionFactory;
import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.enums.AccountStatus;
import mikhailov.dev.domain.enums.CurrencyCode;
import mikhailov.dev.repository.AccountRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class JdbcAccountRepository implements AccountRepository {

    private final JdbcConnectionFactory connectionFactory;
    private final String SQL = "SELECT * FROM accounts WHERE id = ?";

    public JdbcAccountRepository(JdbcConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }


    @Override
    public Optional<Account> findById(Long id) {

        try (Connection connection = connectionFactory.getConnection()) {

            PreparedStatement statement = connection.prepareStatement(SQL);

            statement.setLong(1, id);


            try (ResultSet result = statement.executeQuery()){
                if (result.next()) {
                    return new Account(
                            result.getString("account_number"),
                            CurrencyCode.valueOf(result.getString("currency")),
                    );
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            log.error("", e);
        }


        return Optional.empty();
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
