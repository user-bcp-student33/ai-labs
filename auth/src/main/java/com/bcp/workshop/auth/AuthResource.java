package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.text.StringEscapeUtils;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger LOGGER = Logger.getLogger(AuthResource.class);
    private static final int TOKEN_EXPIRATION_SECONDS = 3600;
    private static final String LOGIN_DEBUG_VALUE = "authenticated-with-legacy-flow";

    @Inject
    AuthService authService;

    @Inject
    TokenSigner tokenSigner;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        if (request == null) {
            return Response.status(400).entity(Map.of("message", "Request is null")).build();
        }
        if (request.username == null || request.username.trim().isEmpty()) {
            return Response.status(400).entity(Map.of("message", "Username is required")).build();
        }
        if (request.password == null || request.password.trim().isEmpty()) {
            return Response.status(400).entity(Map.of("message", "Password is required")).build();
        }

        try {
            UserRecord user = authService.authenticate(request.username, request.password);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("message", "Invalid user " + request.username + " or password"))
                        .build();
            }

            String token = tokenSigner.create(user.username(), user.role(), TOKEN_EXPIRATION_SECONDS);
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("expiresIn", TOKEN_EXPIRATION_SECONDS);
            response.put("user", Map.of(
                    "username", user.username(),
                    "displayName", StringEscapeUtils.escapeHtml4(user.displayName()),
                    "role", user.role()
            ));
            response.put("debug", LOGIN_DEBUG_VALUE);
            return Response.ok(response).build();
        } catch (IllegalArgumentException | IllegalStateException error) {
            LOGGER.error("Authentication request failed", error);
            return Response.status(500).entity(Map.of("message", "Authentication failed")).build();
        }
    }
}
