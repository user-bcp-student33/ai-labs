# Proposal

## Why

El Issue #15 (H-AUTH-01, severidad alta) describe que valores construidos para alterar la consulta de credenciales pueden permitir iniciar sesión sin la contraseña correcta. Corregirlo evita la autenticación no autorizada y conserva el flujo de login esperado.

`docs/ENDPOINTS.md` documenta la API FastAPI, pero no define `POST /auth/login`; por ello, el alcance y la conducta de preservación de este endpoint se basan en el Issue y en sus criterios de aceptación.

## What Changes

### Alcance

- Hacer que la autenticación valide las credenciales recibidas sin que su contenido cambie las condiciones de coincidencia.
- Rechazar con `401` los intentos de inyección en el usuario o la contraseña, sin emitir ni devolver un token.
- Preservar el login válido de `student` con `Workshop123!`: `200`, `user.username` y `token`.
- Mantener la ruta `POST /auth/login` y la forma de respuesta observable para un login válido.

### Fuera de alcance

- Cambios en `api/`, `web/` o endpoints distintos de `POST /auth/login`.
- Cambios al formato, emisión o validación del token; al esquema de datos o al aprovisionamiento de usuarios.
- Refactorizaciones generales, endurecimiento de otros flujos de autenticación o cambios de dependencias.

## Capabilities

### New Capabilities

- `auth-login`: define la autenticación observable mediante `POST /auth/login`, incluidos el acceso válido y el rechazo de credenciales no coincidentes o manipuladas.

### Modified Capabilities

- Ninguna. El inventario OpenSpec existente no contiene capacidades.

## Impact

- Componente `auth/`: verificación de credenciales del login y pruebas de `POST /auth/login`.
- No se modifican el endpoint ni el contrato exitoso que el Issue pide preservar.
- `docs/ENDPOINTS.md` no contiene una especificación de `POST /auth/login`; los criterios del Issue #15 constituyen el baseline de preservación disponible para este endpoint.
