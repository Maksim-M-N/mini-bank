package mikhailov.dev.dao.hibernate;

import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.enums.AccountStatus;
import mikhailov.dev.domain.enums.CurrencyCode;
import mikhailov.dev.dto.ClientBalanceSummary;
import mikhailov.dev.exception.AccountNotFoundException;
import mikhailov.dev.repository.AccountRepository;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.SelectionQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class HibernateAccountRepository implements AccountRepository {
    private final SessionFactory sessionFactory;
    private final Session session;

    public HibernateAccountRepository(SessionFactory sessionFactory) {
        this(sessionFactory, null);
    }

    public HibernateAccountRepository(SessionFactory sessionFactory, Session session) {
        this.sessionFactory = sessionFactory;
        this.session = session;
    }

    @Override
    public Optional<Account> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID счёта не должен быть null");
        }

        try (Session session = sessionFactory.openSession()) {
            return session.createSelectionQuery(
                    "FROM Account a JOIN FETCH a.client WHERE a.id = :id", Account.class)
                    .setParameter("id", id)
                    .uniqueResultOptional();
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
        validateActiveSession();

        SelectionQuery<Account> selectionQuery = session.createSelectionQuery(
                        "FROM Account a WHERE a.accountNumber = :number", Account.class);

        return selectionQuery
                .setParameter("number", accountNumber)
                .setHibernateLockMode(LockMode.PESSIMISTIC_WRITE)
                .uniqueResultOptional();
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
                    "FROM Account a WHERE a.status = :status ORDER BY a.id",
                    Account.class);

            return selectionQuery
                    .setParameter("status", AccountStatus.ACTIVE)
                    .getResultList();
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
                    "FROM Account a WHERE a.status = :status ORDER BY a.id",
                    Account.class);

            return selectionQuery
                    .setParameter("status", AccountStatus.BLOCKED)
                    .getResultList();
        }
    }

    @Override
    public void updateBalance(Long accountId, BigDecimal newBalance) {
        if (session != null) {
            validateActiveSession();
            executeBalanceUpdate(session, accountId, newBalance);
        } else {
            sessionFactory.inTransaction(newSession ->
                    executeBalanceUpdate(newSession, accountId, newBalance));
        }
    }

    @Override
    public void updateStatus(Long accountId, AccountStatus status) {
        if (session != null) {
            validateActiveSession();
            executeStatusUpdate(session, accountId, status);
        } else {
            sessionFactory.inTransaction(newSession ->
                    executeStatusUpdate(newSession, accountId, status));
        }
    }

    @Override
    public List<ClientBalanceSummary> getClientBalanceSummary() {
        try (Session session = sessionFactory.openSession()) {
            return session.createSelectionQuery("""
                SELECT c.fullName,
                       COUNT(a.id),
                       COALESCE(SUM(a.balance), 0),
                       ROUND(CAST(AVG(a.balance) AS BigDecimal), 2)
                FROM Client c
                LEFT JOIN c.accounts a
                GROUP BY c.id, c.fullName
                ORDER BY c.id
                """, ClientBalanceSummary.class)
                    .getResultList();
        }
    }

    @Override
    public List<ClientBalanceSummary> getRichClients(BigDecimal minTotalBalance) {
        try (Session session = sessionFactory.openSession()) {
            return session.createSelectionQuery("""
                SELECT c.fullName,
                       COUNT(a.id),
                       COALESCE(SUM(a.balance), 0),
                       ROUND(CAST(AVG(a.balance) AS BigDecimal), 2)
                FROM Client c
                LEFT JOIN c.accounts a
                GROUP BY c.id, c.fullName
                HAVING COALESCE(SUM(a.balance), 0) > :minTotalBalance
                ORDER BY COALESCE(SUM(a.balance), 0) DESC, c.id
                """, ClientBalanceSummary.class)
                    .setParameter("minTotalBalance", minTotalBalance)
                    .getResultList();
        }
    }

    @Override
    public List<Account> findAccountsWithTransfers() {
        try (Session session = sessionFactory.openSession()) {
            return session.createSelectionQuery("""
                    FROM Account a
                    JOIN FETCH a.client
                    WHERE EXISTS (
                        FROM Transfer t
                        WHERE t.fromAccount = a OR t.toAccount = a
                    )
                    ORDER BY a.id
                    """, Account.class).getResultList();
        }
    }


    private void executeBalanceUpdate(Session session, Long accountId,
                                      BigDecimal newBalance) {
        Account account = session.find(Account.class, accountId);

        if (account == null) {
            throw new AccountNotFoundException("Счёт с указанным ID не найден");
        }

        account.changeBalance(newBalance);
    }

    private void executeStatusUpdate(Session session, Long accountId, AccountStatus status) {
        Account account = session.find(Account.class, accountId);

        if (account == null) {
            throw new AccountNotFoundException("Счёт с указанным ID не найден");
        }

        account.changeStatus(status);
    }

    private void validateActiveSession() {
        if (session == null || !session.isOpen() || !session.getTransaction().isActive()) {
            throw new IllegalStateException(
                    "Для выполнения операции необходима открытая сессия с активной транзакцией.");
        }
    }
}
