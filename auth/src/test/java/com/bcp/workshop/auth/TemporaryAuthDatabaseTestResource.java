package com.bcp.workshop.auth;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TemporaryAuthDatabaseTestResource implements QuarkusTestResourceLifecycleManager {
    private Path temporaryDatabase;

    @Override
    public Map<String, String> start() {
        String databaseUrl = System.getenv("AUTH_DB_URL");
        if (databaseUrl != null) {
            if (!databaseUrl.startsWith("jdbc:sqlite:")) {
                throw new IllegalStateException("AUTH_DB_URL must use SQLite for auth tests");
            }
        } else {
            try {
                temporaryDatabase = Files.createTempFile("auth-tests-", ".db");
                databaseUrl = "jdbc:sqlite:" + temporaryDatabase.toAbsolutePath();
            } catch (IOException error) {
                throw new IllegalStateException("Unable to create temporary authentication database", error);
            }
        }
        return Map.of("auth.db.url", databaseUrl);
    }

    @Override
    public void stop() {
        if (temporaryDatabase == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryDatabase);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to remove temporary authentication database", error);
        }
    }
}
