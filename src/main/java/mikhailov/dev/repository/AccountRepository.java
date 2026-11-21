package mikhailov.dev.repository;

import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.enums.AccountStatus;
import mikhailov.dev.domain.enums.CurrencyCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(Long id);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findAllByClientId(Long clientId);
    List<Account> findActiveAccounts();
    List<Account> findAccountsWithBalanceGreaterThan(BigDecimal amount);
    List<Account> findAccountsByCurrency(CurrencyCode currency);
    List<Account> findBlockedAccounts();
    void updateBalance(Long accountId, BigDecimal newBalance);
    void updateStatus(Long accountId, AccountStatus status);
}
