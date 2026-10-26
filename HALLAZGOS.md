# Hallazgos

## auth

### Contrato y flujo observado de login

No existe `docs/ENDPOINTS.md`, por lo que lo siguiente es el contrato observado en el código, no un contrato acordado:

- `POST /auth/login` recibe JSON con `username` y `password`; si el cuerpo es nulo o alguno de esos campos falta o está vacío, responde `400`. Evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:28-36`.
- Si no encuentra credenciales coincidentes, responde `401` con un mensaje que incluye el username. Evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:39-43`.
- Si autentica, responde `200` con `token`, `expiresIn`, `user` (`username`, `displayName`, `role`) y `debug`. Evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:46-56`.
- Antes de resolver H-AUTH-02, el recurso consultaba `LegacyDatabase.findByCredentials`; la base SQLite usaba `AUTH_DB_URL` o, por defecto, `jdbc:sqlite:./data/workshop.db`. El esquema guardaba `password` como texto y el inicializador insertaba usuarios de demostración con contraseñas literales. Evidencia previa: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:15,21-24,31-35`.
- Tras autenticar, el recurso delega la emisión a `TokenSigner`, que crea un JWT compacto HS256 con `sub`, `role` y `exp`, firmado con HMAC. En `auth/src/` no se encontró un flujo de validación del token; los consumidores fuera del componente no se inspeccionaron. Evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:51` y `auth/src/main/java/com/bcp/workshop/auth/TokenSigner.java:18-75`.
- La configuración fija el puerto `8080`, limita el host a `127.0.0.1` salvo en producción y habilita CORS con el patrón `/.*/`. Evidencia: `auth/src/main/resources/application.properties:1-7`.
- En el camino de error del recurso, el cliente recibe un mensaje genérico `500`; el detalle se registra en el servidor. Evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:63-64`.

### Estado tras H-AUTH-02

La autenticación consulta la cuenta por username con `PreparedStatement`, verifica
un hash PBKDF2 con salt y compara el resultado en tiempo constante. La migración
transaccional sustituye la columna `password` por `password_hash`; los usuarios
de demostración nuevos pasan por el mismo hasher. Evidencia:
`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:40-45,119-139,151-179`,
`auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:18,26-43,75,91-101`
y `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:14-20`.

### Matriz de seguimiento

| id | componente | hallazgo | tipo | explicacion | evidencia (archivo:linea) | severidad | estado |
|---|---|---|---|---|---|---|---|
| H-AUTH-01 | auth | Concatenación de credenciales en SQL | Vulnerabilidad | El flujo descubierto interpolaba username y password. Como parte de H-AUTH-02, la búsqueda actual usa `PreparedStatement`; H-AUTH-01 permanece Pendiente hasta contar con caracterización y evidencia propias, según PLAN.md. | `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:40-45` | alta | Pendiente |
| H-AUTH-02 | auth | Contraseñas almacenadas en texto plano | Vulnerabilidad | Resuelto: la migración elimina la columna en claro, conserva hashes PBKDF2 versionados con salt y verifica la contraseña antes de autenticar. | `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:119-139,151-179`; `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:18,26-43,75,91-101`; `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:14-20` | alta | Resuelto |
| H-AUTH-03 | auth | Token Base64 con secreto embebido y sin firma | Vulnerabilidad | La emisión ahora crea un JWT HS256 con claims `sub`, `role` y `exp`, firmado con HMAC y sin incluir el secreto en el token. Permanece Pendiente verificar que los consumidores validen la firma; no se inspeccionaron componentes fuera de `auth/`. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:51`; `auth/src/main/java/com/bcp/workshop/auth/TokenSigner.java:18-75` | alta | Pendiente |
| H-AUTH-04 | auth | Secreto fijo y CORS de origen amplio | Vulnerabilidad | Resuelto: el secreto procede de `AUTH_TOKEN_SECRET` y se exige un mínimo de 32 bytes; CORS usa `AUTH_CORS_ORIGINS` con el origen local documentado como valor predeterminado. Las pruebas aceptan el origen configurado y rechazan uno ajeno. | `auth/src/main/java/com/bcp/workshop/auth/TokenSigner.java:21,28-35`; `auth/src/main/resources/application.properties:5,9-11`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:84-104` | alta | Resuelto |
| H-AUTH-05 | auth | La respuesta de error expone detalles internos | Vulnerabilidad | Resuelto: el cliente recibe `Authentication failed` sin mensaje ni clase de excepción; el detalle se registra en el servidor. La prueba comprueba que no se devuelva el campo `exception`. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:63-64`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:263-273` | media | Resuelto |
| H-AUTH-06 | auth | Versiones de dependencias señaladas por RETO.md pendientes de análisis SCA | Sugerencia | Quarkus `3.27.4` y Commons Text `1.9` están declarados, pero no se verificaron advisories; por ahora no se afirma que exista un CVE aplicable. | `auth/pom.xml:13,49-50` | media | Pendiente |
| H-AUTH-07 | auth | AuthResource concentra validación, acceso a datos, generación de token y respuesta | Refactor | El flujo original combinaba validación, persistencia, negocio y respuesta. H-AUTH-02 separó la consulta/verificación en `AuthService`, pero la validación HTTP, creación del token y serialización siguen en el recurso; permanece Pendiente fuera de la selección. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:28-56`; `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:14-20` | baja | Pendiente |

Los siete candidatos del bloque `auth` de `RETO.md` tienen evidencia en el código o en su historial. H-AUTH-03 permanece Pendiente hasta verificar validación de firma en consumidores fuera del alcance. La aplicabilidad de advisories SCA (H-AUTH-06) sigue pendiente; CodeQL y Dependency Review remotos no se ejecutaron en este recorrido local. Las pruebas y coberturas están registradas en `PLAN.md`.
