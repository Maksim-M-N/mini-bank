package mikhailov.dev;

import mikhailov.dev.config.DatabaseInitializer;

import java.io.IOException;
import java.sql.SQLException;

public class DatabaseSetup {
    public static void main(String[] args) throws SQLException, IOException {
        String datePath = "data.sql";
        String schemaPath = "schema.sql";

        DatabaseInitializer.initialize(
                "jdbc:postgresql://localhost:5432/mydb",
                "postgres",
                "postgres",
                schemaPath
        );
    }
}
