package com.wealthcopilot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

class FlywayMigrationTest {

    private static final String DATABASE_URL =
            "jdbc:h2:mem:flywayMigration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    @Test
    void migrationsCreateMarketDataAndApiKeyTables() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(DATABASE_URL, "sa", "")
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertEquals(4, result.migrationsExecuted);

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "")) {
            DatabaseMetaData metadata = connection.getMetaData();
            assertTrue(tableExists(metadata, "price_cache"));
            assertTrue(tableExists(metadata, "api_keys"));
            assertTrue(tableExists(metadata, "conversations"));
            assertTrue(tableExists(metadata, "chat_messages"));
            assertTrue(foreignKeyExists(metadata, "conversations", "user_id", "users", "id"));
            assertTrue(foreignKeyExists(metadata, "chat_messages", "conversation_id", "conversations", "id"));
            assertTrue(primaryKeyContains(metadata, "price_cache", "instrument_id"));
            assertTrue(foreignKeyExists(metadata, "price_cache", "instrument_id", "instruments", "id"));
            assertTrue(uniqueIndexContains(metadata, "api_keys", "key_hash"));
        }
    }

    private boolean tableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet result = metadata.getTables(null, null, tableName, new String[] {"TABLE"})) {
            return result.next();
        }
    }

    private boolean primaryKeyContains(DatabaseMetaData metadata, String tableName, String columnName)
            throws SQLException {
        try (ResultSet result = metadata.getPrimaryKeys(null, null, tableName)) {
            while (result.next()) {
                if (columnName.equalsIgnoreCase(result.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean foreignKeyExists(
            DatabaseMetaData metadata,
            String tableName,
            String columnName,
            String referencedTable,
            String referencedColumn
    ) throws SQLException {
        try (ResultSet result = metadata.getImportedKeys(null, null, tableName)) {
            while (result.next()) {
                if (columnName.equalsIgnoreCase(result.getString("FKCOLUMN_NAME"))
                        && referencedTable.equalsIgnoreCase(result.getString("PKTABLE_NAME"))
                        && referencedColumn.equalsIgnoreCase(result.getString("PKCOLUMN_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean uniqueIndexContains(DatabaseMetaData metadata, String tableName, String columnName)
            throws SQLException {
        try (ResultSet result = metadata.getIndexInfo(null, null, tableName, true, false)) {
            while (result.next()) {
                if (columnName.equalsIgnoreCase(result.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
