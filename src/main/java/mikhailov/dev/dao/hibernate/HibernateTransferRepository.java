package mikhailov.dev.dao.hibernate;

import mikhailov.dev.domain.Transfer;
import mikhailov.dev.domain.enums.TransferStatus;
import mikhailov.dev.repository.TransferRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.SelectionQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

public class HibernateTransferRepository implements TransferRepository {
    private static final String BASE_SELECT = """
            FROM Transfer t
            JOIN FETCH t.fromAccount
            JOIN FETCH t.toAccount
            """;

    private static final String ORDER_BY = """
            ORDER BY t.createdAt DESC, t.id DESC
            """;

    private final SessionFactory sessionFactory;
    private final Session session;

    public HibernateTransferRepository(SessionFactory sessionFactory) {
        this(sessionFactory, null);
    }

    public HibernateTransferRepository(SessionFactory sessionFactory, Session session) {
        this.sessionFactory = sessionFactory;
        this.session = session;
    }

    @Override
    public Transfer save(Transfer transfer) {
        if (session != null) {
            validateActiveSession();

            session.persist(transfer);

            return transfer;
        }

        return sessionFactory.fromTransaction(newSession -> {
            newSession.persist(transfer);
            return transfer;
        });
    }

    @Override
    public List<Transfer> findByAccountId(Long accountId) {
        return queryTransfers(
                BASE_SELECT + """
                        WHERE t.fromAccount.id = :accountId
                        OR t.toAccount.id = :accountId
                        """ + ORDER_BY,
                query -> query.setParameter("accountId", accountId)
        );
    }

    @Override
    public List<Transfer> findOutgoingTransfers(Long accountId) {
        return queryTransfers(
                BASE_SELECT + """
                        WHERE t.fromAccount.id = :accountId
                        """ + ORDER_BY,
                query -> query.setParameter("accountId", accountId)
        );
    }

    @Override
    public List<Transfer> findIncomingTransfers(Long accountId) {
        return queryTransfers(
                BASE_SELECT + """
                        WHERE t.toAccount.id = :accountId
                        """ + ORDER_BY,
                query -> query.setParameter("accountId", accountId)
        );
    }

    @Override
    public List<Transfer> findSuccessfulTransfers() {
        return queryTransfers(
                BASE_SELECT + """
                        WHERE t.status = :status
                        """ + ORDER_BY,
                query -> query.setParameter("status", TransferStatus.SUCCESS)
        );
    }

    @Override
    public BigDecimal getTotalOutgoingAmount(Long accountId) {
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<BigDecimal> selectionQuery =
                    session.createSelectionQuery("""
                            SELECT COALESCE(SUM(t.amount), 0)
                            FROM Transfer t
                            WHERE t.fromAccount.id = :accountId
                            AND t.status = :status
                            """, BigDecimal.class);

            return selectionQuery
                    .setParameter("accountId", accountId)
                    .setParameter("status", TransferStatus.SUCCESS)
                    .getSingleResult();
        }
    }

    @Override
    public BigDecimal getTotalIncomingAmount(Long accountId) {
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<BigDecimal> selectionQuery =
                    session.createSelectionQuery("""
                            SELECT COALESCE(SUM(t.amount), 0)
                            FROM Transfer t
                            WHERE t.toAccount.id = :accountId
                            AND t.status = :status
                            """, BigDecimal.class);

            return selectionQuery
                    .setParameter("accountId", accountId)
                    .setParameter("status", TransferStatus.SUCCESS)
                    .getSingleResult();
        }
    }

    @Override
    public List<Transfer> findTransfersWithoutComment() {
        return queryTransfers(
                BASE_SELECT + """
                        WHERE t.comment IS NULL
                        """ + ORDER_BY,
                query -> {}
        );
    }

    @Override
    public List<Transfer> findLargeTransfers(BigDecimal minAmount) {
        return queryTransfers(
                BASE_SELECT + """
                        WHERE t.amount > :minAmount
                        """ + ORDER_BY,
                query -> query.setParameter("minAmount", minAmount)
        );
    }

    private List<Transfer> queryTransfers(String hql, Consumer<SelectionQuery<Transfer>> binder) {
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Transfer> query =
                    session.createSelectionQuery(hql, Transfer.class);

            binder.accept(query);

            return query.getResultList();
        }
    }

    private void validateActiveSession() {
        if (session == null || !session.isOpen() || !session.getTransaction().isActive()) {
            throw new IllegalStateException(
                    "Для выполнения операции необходима открытая сессия с активной транзакцией.");
        }
    }
}