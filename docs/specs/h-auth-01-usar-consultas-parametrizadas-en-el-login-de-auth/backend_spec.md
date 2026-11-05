```java
// LegacyDatabase.java
package pe.bcp.login.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

public final class LegacyDatabase {
    private static final String FIND_BY_CREDENTIALS_SQL =
            "SELECT username FROM users WHERE username = ? AND password = ?";

    private final DataSource dataSource;

    public LegacyDatabase(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource is required");
        }
        this.dataSource = dataSource;
    }

    public Optional<User> findByCredentials(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(FIND_BY_CREDENTIALS_SQL)) {
            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return Optional.empty();
                }
                return Optional.of(new User(results.getString("username")));
            }
        } catch (SQLException exception) {
            throw new DatabaseAccessException("Unable to authenticate user", exception);
        }
    }

    public static final class User {
        private final String username;

        public User(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }

    public static final class DatabaseAccessException extends RuntimeException {
        public DatabaseAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

La consulta mantiene una estructura fija y vincula ambos valores con `PreparedStatement`. Este archivo asume una tabla `users` con columnas `username` y `password`; sin el código existente no es posible asegurar compatibilidad con sus tipos, esquema ni escribir una prueba ejecutable de `AuthResourceTest`.