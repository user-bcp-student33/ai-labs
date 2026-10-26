package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {
    @Inject
    LegacyDatabase database;

    @Inject
    PasswordHasher passwordHasher;

    public UserRecord authenticate(String username, String password) {
        UserAccount account = database.findByUsername(username);
        if (account == null || !passwordHasher.verify(password, account.passwordHash())) {
            return null;
        }

        return new UserRecord(account.id(), account.username(), account.displayName(), account.role());
    }
}
