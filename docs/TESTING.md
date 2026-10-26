# Pruebas — auth

## Alcance y contrato

Se probaron los endpoints de `auth` descritos en
[`docs/ENDPOINTS.md`](./ENDPOINTS.md): `GET /q/health` y `POST /auth/login`.
Ese documento clasifica el contrato como observado, no aprobado formalmente.
No se asumieron respuestas para errores que no están documentados.

Las pruebas de endpoint están en
[`AuthResourceTest.java`](../auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java).
La ejecución completa también incluye `PasswordHasherTest` y `TokenSignerTest`.
La relación de esta ejecución con los hallazgos de `auth` es:

- **H-AUTH-02:** caracterización de login y verificación del almacenamiento con
  hashes, dentro del alcance de la suite.
- **H-AUTH-03:** se comprueba la emisión y firma del JWT; la validación por
  consumidores externos sigue pendiente y no se afirma cubierta.
- **H-AUTH-05:** se comprueba el error genérico `500` sin detalles de excepción.

## Matriz de casos

| endpoint | caso | tipo | resultado esperado | prueba |
|---|---|---|---|---|
| `GET /q/health` | GET sin cuerpo ni autenticación | Camino feliz | `200`; `status` es `UP` y `checks` es `[]`. | `reportsHealthUpWithoutAuthentication` |
| `GET /q/health` | Endpoint sin campos, cuerpo ni credenciales requeridos | Borde | La solicitud mínima sigue dando `200`, `UP` y `checks` vacío. La misma prueba cubre el camino feliz y este límite porque la ruta no tiene otra entrada documentada. | `reportsHealthUpWithoutAuthentication` |
| `GET /q/health` | Error propio del endpoint | Error documentado | No aplica: `docs/ENDPOINTS.md` no documenta respuestas de error propias para esta ruta. | No aplica |
| `POST /auth/login` | Usuario de demostración y contraseña válidos, con `client` | Camino feliz | `200`; token, `expiresIn: 3600`, usuario y `debug` con los valores/forma documentados. | `logsInWithWorkshopUser` |
| `POST /auth/login` | `client` opcional omitido | Borde | `200` y token JWT firmado válido. | `issuesSignedJwtToken` |
| `POST /auth/login` | Body JSON `null` | Error `400` | `{"message":"Request is null"}`. | `rejectsNullRequest` |
| `POST /auth/login` | Falta `username` | Error `400` | `{"message":"Username is required"}`. | `rejectsMissingUsername` |
| `POST /auth/login` | `username` explícitamente `null` | Error `400` | `{"message":"Username is required"}`. | `rejectsExplicitlyNullUsername` |
| `POST /auth/login` | `username` vacío | Borde / error `400` | `{"message":"Username is required"}`. | `rejectsEmptyUsername` |
| `POST /auth/login` | `username` contiene solo espacios | Borde / error `400` | `{"message":"Username is required"}`. | `rejectsWhitespaceUsername` |
| `POST /auth/login` | Falta `password` | Error `400` | `{"message":"Password is required"}`. | `rejectsMissingPassword` |
| `POST /auth/login` | `password` explícitamente `null` | Error `400` | `{"message":"Password is required"}`. | `rejectsExplicitlyNullPassword` |
| `POST /auth/login` | `password` vacío | Borde / error `400` | `{"message":"Password is required"}`. | `rejectsEmptyPassword` |
| `POST /auth/login` | `password` contiene solo espacios | Borde / error `400` | `{"message":"Password is required"}`. | `rejectsWhitespacePassword` |
| `POST /auth/login` | Usuario inexistente | Error `401` | `{"message":"Invalid user unknown or password"}`. | `rejectsUnknownUser` |
| `POST /auth/login` | Contraseña incorrecta | Error `401` | `{"message":"Invalid user student or password"}`. | `rejectsIncorrectPassword` |
| `POST /auth/login` | Hash guardado inválido que hace fallar la autenticación | Error `500` | `{"message":"Authentication failed"}` y no se devuelve `exception`. | `rejectsLoginWhenStoredPasswordHashIsInvalid` |

La prueba CORS de preflight también se ejecuta en la suite, pero
`OPTIONS /auth/login` no es una respuesta de `POST /auth/login` y no se cuenta
como endpoint adicional de esta matriz.

## Código de las pruebas añadidas

Los siguientes métodos están en `AuthResourceTest.java`; los métodos
preexistentes que completan la matriz se indican en la columna **prueba**.

```java
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
```

## Ejecuciones y cobertura

Comandos ejecutados desde el directorio `auth/`:

| comando | código de salida | resultado |
|---|---:|---|
| `mvn verify` (sin `AUTH_DB_URL`) | `0` | BUILD SUCCESS; 30 pruebas, 0 fallos, 0 errores y 0 omitidas. `AuthResourceTest`: 20; `PasswordHasherTest`: 6; `TokenSignerTest`: 4. La prueba crea y elimina una SQLite temporal. |
| `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-test.db'; mvn verify` (PowerShell, con base aislada) | `0` | BUILD SUCCESS; 30 pruebas, 0 fallos, 0 errores y 0 omitidas. `AuthResourceTest`: 20; `PasswordHasherTest`: 6; `TokenSignerTest`: 4. |

Cuando no se define `AUTH_DB_URL`, el recurso de prueba crea una base SQLite
temporal y la elimina al finalizar. Si la variable está definida, debe apuntar
a una URL `jdbc:sqlite:`. En ambos casos las pruebas usan la configuración
efectiva de Quarkus y no `auth/data/workshop.db`.

**Medición:** JaCoCo `0.8.12`, generado por `mvn verify` y leído desde
`auth/target/site/jacoco/jacoco.csv`. El bundle de producción contiene 8 clases:
744 instrucciones cubiertas de 860, es decir, **86.51%**. `AuthResource` tiene
130/130 instrucciones y 12/12 ramas cubiertas (**100%**). La meta del 85% se
cumple para el bundle medido y para el código de endpoint. El único archivo
Java modificado en esta tarea es de pruebas; JaCoCo no incluye el código de
prueba en su denominador, por lo que no se presenta como cobertura de tests.

## Fallos

- Antes de agregar el recurso de prueba, `mvn verify` fallaba cuando
  `AUTH_DB_URL` no estaba definido. La ejecución sin esa variable ahora crea
  una base temporal automáticamente.
- No se encontró un fallo funcional del contrato documentado en las pruebas
  ejecutadas.

## Pendientes

- La documentación no fija si `/q/health` puede responder `503` ni qué check lo
  produciría. No se inventó ese comportamiento ni una respuesta esperada.
- Los cuerpos para JSON malformado, tipo de contenido no admitido y otras
  excepciones no capturadas no están definidos en `docs/ENDPOINTS.md`; no se
  añadieron expectativas para ellos.
- H-AUTH-03 continúa pendiente respecto a la validación de firma/expiración por
  consumidores externos, fuera del alcance `auth/`.

## Pruebas propuestas

- Si se documenta una condición de indisponibilidad y su respuesta para
  `/q/health`, añadir una prueba de ese estado y cuerpo.
- Si se acuerda el contrato para JSON malformado o tipos de contenido
  incorrectos en `/auth/login`, añadir pruebas para esas respuestas.

## Documentos consultados

- `README.md`
- `RETO.md`
- `docs/GOOD_PRACTICES.md`
- `docs/ENDPOINTS.md`
- `HALLAZGOS.md`
- `PLAN.md`
- Código y pruebas de `auth/`: `AuthResource`, `LoginRequest`, `AuthService`,
  `LegacyDatabase`, `TokenSigner`, `PasswordHasher`, `application.properties`,
  `pom.xml`, `AuthResourceTest`, `PasswordHasherTest` y `TokenSignerTest`.
