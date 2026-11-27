package mikhailov.dev.config;

import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.Client;
import mikhailov.dev.domain.Transfer;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;


public final class HibernateUtil {
    private HibernateUtil() {}

    public static SessionFactory buildSessionFactory(String url, String username, String password) {
        return new Configuration()
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", username)
                .setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.hbm2ddl.auto", "validate")
                .setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.format_sql", "true")
                .addAnnotatedClass(Client.class)
                .addAnnotatedClass(Account.class)
                .addAnnotatedClass(Transfer.class)
                .buildSessionFactory();
    }
}
