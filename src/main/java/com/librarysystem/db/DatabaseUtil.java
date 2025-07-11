package com.librarysystem.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUtil.class);

    // TODO: Externalize these connection details into a properties file
    private static final String DB_URL = "jdbc:mysql://localhost:3306/library_db";
    private static final String DB_USER = "library_user"; // Replace with your DB username
    private static final String DB_PASSWORD = "library_password"; // Replace with your DB password
    private static final String SCHEMA_FILE_PATH = "src/main/resources/schema.sql"; // Path to your schema file

    static {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            LOGGER.info("MySQL JDBC Driver registered successfully.");
            // Optional: Initialize schema if database/tables don't exist (basic check)
            // initializeSchemaIfNeeded();
        } catch (ClassNotFoundException e) {
            LOGGER.error("MySQL JDBC Driver not found.", e);
            throw new RuntimeException("Failed to load MySQL JDBC Driver", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        if (connection == null) {
            LOGGER.error("Failed to make connection to database at {} with user {}", DB_URL, DB_USER);
            throw new SQLException("Could not establish database connection.");
        }
        LOGGER.debug("Database connection established successfully to {}", DB_URL);
        return connection;
    }

    /**
     * Initializes the database schema by executing the schema.sql file.
     * This is a basic implementation. A more robust solution might involve versioning (e.g., Flyway, Liquibase).
     * This method attempts to create tables if they don't exist.
     */
    public static void initializeSchema() {
        LOGGER.info("Attempting to initialize database schema from: {}", SCHEMA_FILE_PATH);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            String sqlContent = Files.lines(Paths.get(SCHEMA_FILE_PATH)).collect(Collectors.joining("\n"));
            // Split script into individual statements (basic split on ';')
            // This might not work for complex scripts with ';' in comments or strings, or stored procedures.
            String[] individualStatements = sqlContent.split(";\\s*(\\r?\\n|\\r)");

            for (String statement : individualStatements) {
                if (statement.trim().isEmpty()) {
                    continue;
                }
                try {
                    stmt.execute(statement);
                    LOGGER.info("Executed SQL: {}", statement.substring(0, Math.min(statement.length(), 100)).replaceAll("\\s+", " ") + "...");
                } catch (SQLException e) {
                    // It's common for "CREATE TABLE IF NOT EXISTS" to succeed without issues,
                    // but other errors might be important.
                    if (e.getMessage().contains("already exists")) {
                         LOGGER.warn("Table/Index in statement already exists (expected for IF NOT EXISTS): {}", statement.substring(0, Math.min(statement.length(),100)).replaceAll("\\s+", " ") + "...");
                    } else {
                        LOGGER.error("Error executing SQL statement: '{}'. Error: {}", statement.replaceAll("\\s+", " "), e.getMessage());
                        // Optionally re-throw or handle more gracefully
                    }
                }
            }
            LOGGER.info("Database schema initialization script executed successfully.");
        } catch (SQLException e) {
            LOGGER.error("SQL error during schema initialization.", e);
            // Consider if this should be a fatal error for the application
        } catch (IOException e) {
            LOGGER.error("Could not read schema file: {}", SCHEMA_FILE_PATH, e);
            // Consider if this should be a fatal error
        }
    }

    // Example of how it might be called, e.g., in Main.java or a setup routine
    public static void main(String[] args) {
        try {
            // Test connection
            Connection conn = getConnection();
            if (conn != null) {
                LOGGER.info("Successfully connected to the database!");
                conn.close();

                // Initialize schema (run this once or ensure it's idempotent)
                // initializeSchema();
            } else {
                LOGGER.error("Failed to connect to the database.");
            }
        } catch (SQLException e) {
            LOGGER.error("Database connection test failed.", e);
        }
    }
}
