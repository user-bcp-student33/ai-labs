package com.bcp.workshop.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class LegacyDatabase {
    private static final String TABLE_NAME = "legacy_users";
    private static final String MIGRATION_TABLE_NAME = "legacy_users_migrating";

    @ConfigProperty(name = "auth.db.url", defaultValue = "jdbc:sqlite:./data/workshop.db")
    String databaseUrl;

    @Inject
    PasswordHasher passwordHasher;

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            initializeDatabase();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to initialize authentication database", error);
        }
    }

    public UserAccount findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, display_name, role FROM legacy_users WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new UserAccount(
                        result.getLong("id"),
                        result.getString("username"),
                        result.getString("password_hash"),
                        result.getString("display_name"),
                        result.getString("role")
                );
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Unable to retrieve user account", error);
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl)) {
            connection.setAutoCommit(false);
            try {
                ensureSchema(connection);
                insertDemoUser(connection, "student", "Workshop123!", "Workshop Student", "STUDENT");
                insertDemoUser(connection, "trainer", "Trainer123!", "Workshop Trainer", "TRAINER");
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        }
    }

    private void ensureSchema(Connection connection) throws SQLException {
        if (!tableExists(connection)) {
            createUsersTable(connection, TABLE_NAME);
            return;
        }

        Set<String> columns = getColumnNames(connection);
        if (columns.contains("password") && !columns.contains("password_hash")) {
            migrateLegacyPasswords(connection);
            return;
        }
        if (columns.contains("password_hash") && !columns.contains("password")) {
            return;
        }
        throw new SQLException("Unsupported authentication database schema");
    }

    private boolean tableExists(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, TABLE_NAME);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private Set<String> getColumnNames(Connection connection) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(legacy_users)")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }
        return columns;
    }

    private void migrateLegacyPasswords(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + MIGRATION_TABLE_NAME);
        }
        createUsersTable(connection, MIGRATION_TABLE_NAME);

        String selectSql = "SELECT id, username, password, display_name, role FROM " + TABLE_NAME;
        String insertSql = "INSERT INTO " + MIGRATION_TABLE_NAME
                + "(id, username, password_hash, display_name, role) VALUES (?, ?, ?, ?, ?)";
        try (Statement select = connection.createStatement();
             ResultSet users = select.executeQuery(selectSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            while (users.next()) {
                insert.setLong(1, users.getLong("id"));
                insert.setString(2, users.getString("username"));
                insert.setString(3, passwordHasher.hash(users.getString("password")));
                insert.setString(4, users.getString("display_name"));
                insert.setString(5, users.getString("role"));
                insert.addBatch();
            }
            insert.executeBatch();
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE " + TABLE_NAME);
            statement.executeUpdate("ALTER TABLE " + MIGRATION_TABLE_NAME + " RENAME TO " + TABLE_NAME);
        }
    }

    private void createUsersTable(Connection connection, String tableName) throws SQLException {
        String sql = "CREATE TABLE " + tableName
                + " (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, "
                + "password_hash TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void insertDemoUser(
            Connection connection,
            String username,
            String password,
            String displayName,
            String role
    ) throws SQLException {
        try (PreparedStatement lookup = connection.prepareStatement(
                "SELECT 1 FROM " + TABLE_NAME + " WHERE username = ?")) {
            lookup.setString(1, username);
            try (ResultSet existingUser = lookup.executeQuery()) {
                if (existingUser.next()) {
                    return;
                }
            }
        }

        String sql = "INSERT INTO " + TABLE_NAME
                + "(username, password_hash, display_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            insert.setString(1, username);
            insert.setString(2, passwordHasher.hash(password));
            insert.setString(3, displayName);
            insert.setString(4, role);
            insert.executeUpdate();
        }
    }
}
