package com.bcp.workshop.auth;

record UserAccount(long id, String username, String passwordHash, String displayName, String role) {}
