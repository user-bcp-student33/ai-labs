# Spec Delta

## Purpose

Define el comportamiento observable de `POST /auth/login` para que solo las credenciales válidas permitan iniciar sesión y los intentos de alterar su verificación sean rechazados.

## ADDED Requirements

### Requirement: Successful login preserves its response contract

El sistema SHALL conservar el login exitoso de credenciales válidas y la respuesta observable que el Issue #15 requiere preservar.

#### Scenario: Existing student logs in with valid credentials

- **WHEN** se envía `student` y `Workshop123!` a `POST /auth/login`
- **THEN** la respuesta tiene estado `200`, `user.username` es `student` y contiene `token`

### Requirement: Login requires matching credentials

El sistema SHALL autenticar únicamente cuando el usuario y la contraseña enviados coinciden con las credenciales de una cuenta existente.

#### Scenario: Injection attempt in username is rejected

- **WHEN** se envía `' OR '1'='1` como usuario a `POST /auth/login`
- **THEN** la respuesta tiene estado `401` y no contiene `token`

#### Scenario: Injection attempt in password is rejected

- **WHEN** se envía `' OR '1'='1` como contraseña a `POST /auth/login`
- **THEN** la respuesta tiene estado `401` y no contiene `token`
