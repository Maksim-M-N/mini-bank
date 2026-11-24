package mikhailov.dev.dao.hibernate;

import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.enums.AccountStatus;
import mikhailov.dev.domain.enums.CurrencyCode;
import mikhailov.dev.dto.ClientBalanceSummary;
import mikhailov.dev.exception.AccountNotFoundException;
import mikhailov.dev.repository.AccountRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.SelectionQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class HibernateAccountRepository implements AccountRepository {
    private final SessionFactory sessionFactory;

    public HibernateAccountRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<Account> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return Optional.ofNullable(session.find(Account.class, id));
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> selectionQuery = session.createSelectionQuery(
                    "FROM Account a WHERE a.accountNumber = :number", Account.class);

            return selectionQuery.setParameter("number", accountNumber).uniqueResultOptional();
        }
    }

    @Override
    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        return Optional.empty();
    }

    @Override
    public List<Account> findAllByClientId(Long clientId) {
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> selectionQuery = session.createSelectionQuery(
                    "FROM Account a JOIN FETCH a.client WHERE a.client.id = :clientId ORDER BY a.id",
                    Account.class);

            return selectionQuery.setParameter("clientId", clientId).getResultList();
        }
    }

    @Override
    public List<Account> findActiveAccounts() {
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> selectionQuery = session.createSelectionQuery(
                    "FROM Account a WHERE a.status = AccountStatus.ACTIVE ORDER BY a.id",
                    Account.class);

            return selectionQuery.getResultList();
        }
    }

    @Override
    public List<Account> findAccountsWithBalanceGreaterThan(BigDecimal amount) {
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> selectionQuery = session.createSelectionQuery(
                    "FROM Account a WHERE a.balance > :amount ORDER BY a.id", Account.class);

            return selectionQuery.setParameter("amount", amount).getResultList();
        }
    }

    @Override
    public List<Account> findAccountsByCurrency(CurrencyCode currency) {
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> selectionQuery = session.createSelectionQuery(
                    "FROM Account a WHERE a.currencyCode = :currency ORDER BY a.id", Account.class);

            return selectionQuery.setParameter("currency", currency).getResultList();
        }
    }

    @Override
    public List<Account> findBlockedAccounts() {
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> selectionQuery = session.createSelectionQuery(
                    "FROM Account a WHERE a.status = AccountStatus.BLOCKED ORDER BY a.id",
                    Account.class);

            return selectionQuery.getResultList();
        }
    }

    @Override
    public void updateBalance(Long accountId, BigDecimal newBalance) {
        sessionFactory.inTransaction((session -> {
            Account account = session.find(Account.class, accountId);

            if (account == null) {
                throw new AccountNotFoundException("Счёт с указанным ID не найден");
            }

            account.changeBalance(newBalance);
        }));
    }

    @Override
    public void updateStatus(Long accountId, AccountStatus status) {

    }

    @Override
    public List<ClientBalanceSummary> getClientBalanceSummary() {
        return List.of();
    }

    @Override
    public List<ClientBalanceSummary> getRichClients(BigDecimal minTotalBalance) {
        return List.of();
    }

    @Override
    public List<Account> findAccountsWithTransfers() {
        return List.of();
    }
}
