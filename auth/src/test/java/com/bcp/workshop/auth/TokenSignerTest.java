package com.bcp.workshop.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenSignerTest {
    private static final String TEST_SECRET = "test-only-secret-for-auth-token-signing-32-bytes";
    private static final int TOKEN_LIFETIME_SECONDS = 120;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsJwtWithSignedClaimsAndExpiration() throws Exception {
        TokenSigner tokenSigner = new TokenSigner(TEST_SECRET, objectMapper);
        String[] segments = tokenSigner.create("student", "STUDENT", TOKEN_LIFETIME_SECONDS).split("\\.");

        assertEquals(3, segments.length);
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", segments[0]);
        assertEquals(sign(segments[0] + "." + segments[1]), segments[2]);

        JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(segments[1]));
        long now = Instant.now().getEpochSecond();
        assertEquals("student", claims.get("sub").asText());
        assertEquals("STUDENT", claims.get("role").asText());
        assertTrue(claims.get("exp").asLong() >= now);
        assertTrue(claims.get("exp").asLong() <= now + TOKEN_LIFETIME_SECONDS);
    }

    @Test
    void rejectsShortSigningSecret() {
        assertThrows(IllegalStateException.class, () -> new TokenSigner("too-short", objectMapper));
    }

    @Test
    void rejectsNonPositiveExpiration() {
        TokenSigner tokenSigner = new TokenSigner(TEST_SECRET, objectMapper);
        assertThrows(IllegalArgumentException.class, () -> tokenSigner.create("student", "STUDENT", 0));
    }

    @Test
    void reportsClaimSerializationFailure() {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("test serialization failure") {};
            }
        };
        TokenSigner tokenSigner = new TokenSigner(TEST_SECRET, failingMapper);
        assertThrows(IllegalStateException.class, () -> tokenSigner.create("student", "STUDENT", 60));
    }

    private String sign(String signingInput) throws Exception {
        Mac signer = Mac.getInstance("HmacSHA256");
        signer.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signer.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }
}
