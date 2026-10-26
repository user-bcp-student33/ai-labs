package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@QuarkusTestResource(TemporaryAuthDatabaseTestResource.class)
class AuthResourceTest {
    private static final String TEST_TOKEN_SECRET = "test-only-secret-for-auth-token-signing-32-bytes";

    @Inject
    @ConfigProperty(name = "auth.db.url")
    String databaseUrl;

    @Test
    void reportsHealthUpWithoutAuthentication() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", org.hamcrest.Matchers.hasSize(0));
    }

    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("expiresIn", is(3600))
                .body("debug", is("authenticated-with-legacy-flow"))
                .body("token", notNullValue());
    }

    @Test
    void issuesSignedJwtToken() throws Exception {
        String token = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");

        String[] segments = token.split("\\.", -1);
        assertEquals(3, segments.length, "Token must use compact JWT form");

        String signingInput = segments[0] + "." + segments[1];
        String expectedSignature = sign(signingInput);
        assertEquals(expectedSignature, segments[2], "JWT must have an HMAC-SHA256 signature");

        String payload = new String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8);
        JsonNode claims = new ObjectMapper().readTree(payload);
        assertEquals("student", claims.get("sub").asText());
        assertEquals("STUDENT", claims.get("role").asText());
        assertTrue(claims.get("exp").asLong() > Instant.now().getEpochSecond());
        assertFalse(token.contains(TEST_TOKEN_SECRET));

        String alteredPayload = payload.replace("\"role\":\"STUDENT\"", "\"role\":\"ADMIN\"");
        String encodedAlteredPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(alteredPayload.getBytes(StandardCharsets.UTF_8));
        assertNotEquals(segments[2], sign(segments[0] + "." + encodedAlteredPayload));
    }

    @Test
    void allowsOnlyConfiguredCorsOrigin() {
        given()
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://localhost:4200");
    }

    @Test
    void rejectsUnconfiguredCorsOrigin() {
        given()
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }

    @Test
    void rejectsIncorrectPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"incorrect\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user student or password"));
    }

    @Test
    void rejectsUnknownUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"unknown\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user unknown or password"));
    }

    @Test
    void rejectsNullRequest() {
        given()
                .contentType("application/json")
                .body("null")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void rejectsMissingUsername() {
        given()
                .contentType("application/json")
                .body("{\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsExplicitlyNullUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":null,\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsEmptyUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsWhitespaceUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"   \",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsMissingPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsExplicitlyNullPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":null}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsEmptyPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsWhitespacePassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"   \"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void persistsVersionedPasswordHashesInsteadOfPlaintext() throws Exception {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200);

        assertTrue(databaseUrl != null && databaseUrl.startsWith("jdbc:sqlite:"),
                "Tests must use a temporary SQLite database");

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(legacy_users)")) {
            Set<String> columnNames = new HashSet<>();
            while (columns.next()) {
                columnNames.add(columns.getString("name"));
            }

            assertFalse(columnNames.contains("password"), "Plaintext password column must be removed");
            assertTrue(columnNames.contains("password_hash"), "Hashed password column must be present");
        }

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement();
             ResultSet credentials = statement.executeQuery(
                     "SELECT password_hash FROM legacy_users WHERE username='student'")) {
            assertTrue(credentials.next(), "Demonstration account must be present");
            String passwordHash = credentials.getString("password_hash");
            assertTrue(passwordHash.startsWith("pbkdf2-sha256$"), "Hash must include its format version");
            assertFalse(passwordHash.contains("Workshop123!"), "Stored hash must not contain the plaintext password");
        }
    }

    @Test
    void initializesFreshDatabaseWithHashedDemoAccounts() throws Exception {
        Path databasePath = Path.of("target", "auth-h02-fresh.db");
        Files.deleteIfExists(databasePath);

        PasswordHasher passwordHasher = new PasswordHasher();
        LegacyDatabase database = new LegacyDatabase();
        database.databaseUrl = "jdbc:sqlite:./target/auth-h02-fresh.db";
        database.passwordHasher = passwordHasher;

        try {
            database.initialize();
            database.initialize();

            UserAccount student = database.findByUsername("student");
            assertTrue(student != null, "Fresh database must contain the demonstration account");
            assertTrue(passwordHasher.verify("Workshop123!", student.passwordHash()));
            assertFalse(student.passwordHash().contains("Workshop123!"));
        } finally {
            Files.deleteIfExists(databasePath);
        }
    }

    @Test
    void rejectsUnsupportedDatabaseSchema() throws Exception {
        Path databasePath = Path.of("target", "auth-h02-invalid.db");
        Files.deleteIfExists(databasePath);
        String databaseUrl = "jdbc:sqlite:./target/auth-h02-invalid.db";

        try {
            try (Connection connection = DriverManager.getConnection(databaseUrl);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE legacy_users (id INTEGER PRIMARY KEY)");
            }

            LegacyDatabase database = new LegacyDatabase();
            database.databaseUrl = databaseUrl;
            database.passwordHasher = new PasswordHasher();

            IllegalStateException error = assertThrows(IllegalStateException.class, database::initialize);
            assertTrue(error.getCause() instanceof java.sql.SQLException);
        } finally {
            Files.deleteIfExists(databasePath);
        }
    }

    @Test
    void rejectsLoginWhenStoredPasswordHashIsInvalid() throws Exception {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200);

        String originalHash;
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement query = connection.prepareStatement(
                     "SELECT password_hash FROM legacy_users WHERE username = ?")) {
            query.setString(1, "student");
            try (ResultSet result = query.executeQuery()) {
                assertTrue(result.next(), "Demonstration account must be present");
                originalHash = result.getString("password_hash");
            }
        }

        try {
            updatePasswordHash(databaseUrl, "unsupported", "student");
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(500)
                    .body("message", is("Authentication failed"))
                    .body("exception", nullValue());
        } finally {
            updatePasswordHash(databaseUrl, originalHash, "student");
        }
    }

    private void updatePasswordHash(String databaseUrl, String passwordHash, String username) throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE legacy_users SET password_hash = ? WHERE username = ?")) {
            update.setString(1, passwordHash);
            update.setString(2, username);
            update.executeUpdate();
        }
    }

    private String sign(String signingInput) throws Exception {
        Mac signer = Mac.getInstance("HmacSHA256");
        signer.init(new SecretKeySpec(TEST_TOKEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signer.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }
}
