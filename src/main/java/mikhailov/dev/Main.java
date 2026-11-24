package mikhailov.dev;

import mikhailov.dev.config.DatabaseInitializer;
import mikhailov.dev.config.HibernateUtil;
import mikhailov.dev.dao.hibernate.HibernateAccountRepository;
import mikhailov.dev.domain.Account;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws SQLException, IOException {
        String url = "jdbc:postgresql://localhost:5432/mydb";
        String name = "postgres";
        String password = "postgres";

// Если понадобиться.
//        String datePath = "data.sql";
//        String schemaPath = "schema.sql";
//
//        DatabaseInitializer.initialize(
//                url,
//                name,
//                password,
//                datePath
//        );


        try (SessionFactory sessionFactory = HibernateUtil.buildSessionFactory(url, name, password)) {
            HibernateAccountRepository accountRepository = new HibernateAccountRepository(sessionFactory);

            Optional<Account> optionalAccount = accountRepository.findById(1L);

            if(optionalAccount.isEmpty()) {
                System.out.println("Счёт не найден");
                return;
            }

            Account account = optionalAccount.get();

            System.out.println(account.getAccountNumber() + " " + account.getBalance());
        }
    }
}