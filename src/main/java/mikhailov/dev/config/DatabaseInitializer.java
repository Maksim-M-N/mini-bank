package mikhailov.dev.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize(JdbcConnectionFactory connectionFactory,
                                  String path) throws SQLException, IOException {
        String sql = readScript("/" + path);

        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static String readScript(String path) throws IOException {
        try (InputStream input = DatabaseInitializer.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Файл не найден: " + path);

            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
