package com.bcp.workshop.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@ApplicationScoped
public class TokenSigner {
    private static final String SIGNING_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final int MINIMUM_SECRET_LENGTH_BYTES = 32;
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final byte[] signingKey;
    private final ObjectMapper objectMapper;

    @Inject
    public TokenSigner(
            @ConfigProperty(name = "auth.token.secret") String secret,
            ObjectMapper objectMapper
    ) {
        byte[] configuredSecret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (configuredSecret.length < MINIMUM_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException("AUTH_TOKEN_SECRET must contain at least 32 bytes");
        }
        this.signingKey = configuredSecret;
        this.objectMapper = objectMapper;
    }

    public String create(String username, String role, int expirationSeconds) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("Token expiration must be positive");
        }

        String encodedHeader = encode(TOKEN_HEADER.getBytes(StandardCharsets.UTF_8));
        String encodedClaims = encode(serializeClaims(username, role, expirationSeconds));
        String signingInput = encodedHeader + "." + encodedClaims;
        return signingInput + "." + sign(signingInput);
    }

    private byte[] serializeClaims(String username, String role, int expirationSeconds) {
        Map<String, Object> claims = Map.of(
                "sub", username,
                "role", role,
                "exp", Instant.now().plusSeconds(expirationSeconds).getEpochSecond()
        );
        try {
            return objectMapper.writeValueAsBytes(claims);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to serialize authentication token", error);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac signer = Mac.getInstance(SIGNING_ALGORITHM);
            signer.init(new SecretKeySpec(signingKey, SIGNING_ALGORITHM));
            return encode(signer.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Unable to sign authentication token", error);
        }
    }

    private String encode(byte[] value) {
        return BASE64_URL_ENCODER.encodeToString(value);
    }
}
