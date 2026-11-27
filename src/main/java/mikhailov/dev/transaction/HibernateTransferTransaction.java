package mikhailov.dev.transaction;

import mikhailov.dev.dao.hibernate.HibernateAccountRepository;
import mikhailov.dev.dao.hibernate.HibernateTransferRepository;
import mikhailov.dev.repository.AccountRepository;
import mikhailov.dev.repository.TransferRepository;
import org.hibernate.SessionFactory;

import java.util.function.BiConsumer;

public class HibernateTransferTransaction implements TransferTransaction {
    private final SessionFactory sessionFactory;

    public HibernateTransferTransaction(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void inTransaction(BiConsumer<AccountRepository, TransferRepository> action) {
        sessionFactory.inTransaction(session -> {
            HibernateAccountRepository accountRepository =
                    new HibernateAccountRepository(sessionFactory, session);

            HibernateTransferRepository transferRepository =
                    new HibernateTransferRepository(sessionFactory, session);

            action.accept(accountRepository, transferRepository);
        });
    }
}
