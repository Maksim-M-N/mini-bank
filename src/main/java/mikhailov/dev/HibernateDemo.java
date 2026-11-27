package mikhailov.dev;

import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.Client;
import mikhailov.dev.exception.AccountNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;

public class HibernateDemo {
    private HibernateDemo() {}

    public static void demonstrationLoading(SessionFactory factory, Long accountId) {
        System.out.printf("%n=== Ленивая загрузка владельца счёта (ID %d) ===%n", accountId);

        try (Session session = factory.openSession()) {
            Account account = session.find(Account.class, accountId);

            if (account == null) {
                throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден");
            }

            Client client = account.getClient();

            System.out.println("Владелец представлен прокси: " + (client instanceof HibernateProxy));
            System.out.println("Данные загружены до чтения ФИО: " + Hibernate.isInitialized(client));

            System.out.println("Читаем ФИО владельца...");
            String fullName = client.getFullName();

            System.out.println("ФИО владельца: " + fullName);
            System.out.println("Данные загружены после чтения ФИО: " + Hibernate.isInitialized(client));
        }
    }

    public static void demonstrateFirstLevelCache(SessionFactory factory, Long accountId) {
        System.out.printf("%n=== Кеш первого уровня (счёт с ID %d) ===%n", accountId);

        try (Session session = factory.openSession()) {
            System.out.println("Первый поиск счёта...");
            Account first = session.find(Account.class, accountId);

            if (first == null) {
                throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден");
            }

            System.out.println("Повторный поиск в той же сессии...");
            Account second = session.find(Account.class, accountId);

            System.out.println("Получен тот же объект (first == second): " + (first == second));
        }
    }

    public static void demonstrateDirtyChecking(SessionFactory factory, Long accountId) {
        System.out.printf("%n=== Dirty checking (счёт с ID %d) ===%n", accountId);

        BigDecimal originalBalance;

        try (Session session = factory.openSession()) {
            var transaction = session.beginTransaction();

            try {
                Account account = session.find(Account.class, accountId);

                if (account == null) {
                    throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден");
                }

                originalBalance = account.getBalance();
                System.out.println("Баланс до изменения: " + originalBalance);

                account.changeBalance(originalBalance.add(new BigDecimal("1.00")));
                System.out.println("Баланс объекта после изменения: " + account.getBalance());

                System.out.println("Вызываем flush — ожидаем UPDATE...");
                session.flush();
            } finally {
                if (transaction.isActive()) {
                    transaction.rollback();
                    System.out.println("Транзакция откачена.");
                }
            }
        }

        try (Session session = factory.openSession()) {
            Account account = session.find(Account.class, accountId);

            if (account == null) {
                throw new AccountNotFoundException("Счёт с ID " + accountId + " не найден");
            }

            BigDecimal balanceAfterRollback = account.getBalance();
            System.out.println("Баланс в БД после отката: " + balanceAfterRollback);
            System.out.println("Исходный баланс сохранён: "
                    + (originalBalance.compareTo(balanceAfterRollback) == 0));
        }
    }

    public static void demonstrateNPlusOne(SessionFactory factory) {
        System.out.println("\n=== Демонстрация N+1 ===");

        System.out.println("\n--- Обычная загрузка счетов ---");
        printAccountOwners(factory, """
            SELECT a
            FROM Account a
            ORDER BY a.id
            """);

        System.out.println("\n--- Загрузка счетов и владельцев через JOIN FETCH ---");
        printAccountOwners(factory, """
            SELECT a
            FROM Account a
            JOIN FETCH a.client
            ORDER BY a.id
            """);
    }

    private static void printAccountOwners(SessionFactory factory, String hql) {
        try (Session session = factory.openSession()) {
            var accounts = session.createSelectionQuery(hql, Account.class).getResultList();

            for (Account account : accounts) {
                System.out.printf("Счёт: %s | Владелец: %s%n",
                        account.getAccountNumber(),
                        account.getClient().getFullName());
            }
        }
    }
}
