package mikhailov.dev;

import mikhailov.dev.config.HibernateUtil;
import mikhailov.dev.config.JdbcConnectionFactory;
import mikhailov.dev.dao.hibernate.HibernateAccountRepository;
import mikhailov.dev.dao.hibernate.HibernateTransferRepository;
import mikhailov.dev.dao.jdbc.JdbcAccountRepository;
import mikhailov.dev.dao.jdbc.JdbcTransferRepository;
import mikhailov.dev.service.ReportService;
import org.hibernate.SessionFactory;

import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Укажи один режим: jdbc или hibernate");
        }

        String url = "jdbc:postgresql://localhost:5432/mydb";
        String username = "postgres";
        String password = "postgres";

        switch (args[0]) {
            case "jdbc" -> {
                JdbcConnectionFactory factory = new JdbcConnectionFactory(url, username, password);
                printReports(new ReportService(
                        new JdbcAccountRepository(factory),
                        new JdbcTransferRepository(factory)
                ));
            }
            case "hibernate" -> {
                try (SessionFactory factory = HibernateUtil.buildSessionFactory(url, username, password)) {
                    HibernateDemo.demonstrationLoading(factory, 1L);
                    HibernateDemo.demonstrateFirstLevelCache(factory, 1L);
                    HibernateDemo.demonstrateDirtyChecking(factory, 1L);
                    HibernateDemo.demonstrateNPlusOne(factory);

                    printReports(new ReportService(
                            new HibernateAccountRepository(factory),
                            new HibernateTransferRepository(factory)
                    ));
                }
            }
            default -> System.err.println("Неизвестный режим. Укажи jdbc или hibernate.");
        }
    }

    private static void printReports(ReportService reports) {
        System.out.println("=== Сводка балансов клиентов ===");
        reports.printClientBalanceSummary();

        System.out.println("\n=== Счета клиента ===");
        reports.printClientAccountsReport(1L);

        System.out.println("\n=== История переводов ===");
        reports.printAccountTransferHistory("40817810000000000001");

        System.out.println("\n=== Клиенты с балансом выше 100 ===");
        reports.printRichClients(BigDecimal.valueOf(100));

        System.out.println("\n=== Счета с переводами ===");
        reports.printAccountsWithTransfers();
    }
}