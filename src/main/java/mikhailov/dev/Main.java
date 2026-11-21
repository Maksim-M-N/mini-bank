package mikhailov.dev;

import mikhailov.dev.config.JdbcConnectionFactory;
import mikhailov.dev.dao.jdbc.JdbcAccountRepository;
import mikhailov.dev.dao.jdbc.JdbcTransferRepository;

public class Main {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/mydb";
        String name = "postgres";
        String password = "postgres";

        JdbcConnectionFactory connectionFactory = new JdbcConnectionFactory(url, name, password);

        JdbcAccountRepository accountRepository = new JdbcAccountRepository(connectionFactory);

        JdbcTransferRepository transferRepository = new JdbcTransferRepository(connectionFactory);

    }
}