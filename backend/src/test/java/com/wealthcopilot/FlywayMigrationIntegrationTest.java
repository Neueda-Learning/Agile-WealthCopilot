package com.wealthcopilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("wealthcopilot")
            .withUsername("wealthcopilot")
            .withPassword("wealthcopilot_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void baselineMigrationCreatesUsersTable() throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();

            try (ResultSet tables = metadata.getTables(
                    connection.getCatalog(), null, "users", new String[] {"TABLE"})) {
                assertThat(tables.next()).isTrue();
            }

            try (ResultSet columns = metadata.getColumns(
                    connection.getCatalog(), null, "users", null)) {
                List<String> columnNames = new ArrayList<>();
                while (columns.next()) {
                    columnNames.add(columns.getString("COLUMN_NAME"));
                }
                assertThat(columnNames).containsExactlyInAnyOrder(
                        "id",
                        "email",
                        "password_hash",
                        "display_name",
                        "created_at",
                        "updated_at");
            }
        }
    }
}
