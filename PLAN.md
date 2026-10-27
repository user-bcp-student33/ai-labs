# Plan de remediación

## auth

### Alcance y selección inicial

Este recorrido cubre únicamente H-AUTH-02, confirmado y todavía `Pendiente` en
`HALLAZGOS.md` al iniciar la implementación original:

- **H-AUTH-02**, por la exposición de contraseñas almacenadas en texto plano.

H-AUTH-04 quedó diferido en la selección inicial: el usuario aprobó inicialmente
H-AUTH-02 y H-AUTH-04, pero eligió diferir H-AUTH-04 al confirmarse que su
resolución completa depende de H-AUTH-03, no seleccionado entonces. Los demás
hallazgos de `auth` también conservaron su estado en ese alcance inicial. La
decisión posterior de ampliar el trabajo se registra al final de este plan.

### Aprobación humana y dependencia detectada

El usuario aprobó expresamente H-AUTH-02 y H-AUTH-04 en esta sesión, antes de
cualquier cambio de código. El análisis confirmó que H-AUTH-04 no puede quedar
completamente resuelto con la selección inicial: el secreto de configuración
también se incluye en el token Base64, y este plan indica que retirarlo depende
de H-AUTH-03, que no estaba seleccionado en ese momento. El usuario eligió
diferir H-AUTH-04 y continuar solo con H-AUTH-02 dentro de la selección inicial.

### Contrato que debe preservarse

No existe `docs/ENDPOINTS.md`; por tanto, se toma como referencia el
comportamiento observado, no un contrato acordado. `POST /auth/login` recibe
`username` y `password`; conserva las respuestas `400` para solicitudes o
campos requeridos inválidos, `401` para credenciales no válidas y `200` con
`token`, `expiresIn`, `user.username`, `user.displayName`, `user.role` y
`debug` para un inicio de sesión correcto. Evidencia observada:
`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:28-56`, resumida
también en `HALLAZGOS.md`.

La configuración CORS quedó sin cambios en el cambio inicial de H-AUTH-02. La
actualización posterior registra su modificación aprobada. Se conservarán las
rutas, los códigos de respuesta y la forma del JSON de login.

### H-AUTH-02 — Contraseñas almacenadas en texto plano

- **Hallazgo de HALLAZGOS.md:** H-AUTH-02, `Pendiente` al iniciar este
  recorrido. El esquema y los datos iniciales guardaban la contraseña en texto.
  Evidencia previa al cambio:
  `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:22-24,32`.
- **Problema:** una lectura de la base de datos expone directamente las
  contraseñas; además, la autenticación actual compara la contraseña recibida
  dentro de la consulta SQL.
- **Cambio propuesto:** almacenar un hash lento con salt único por contraseña,
  usando `PBKDF2WithHmacSHA256` del JDK para no añadir una dependencia. Guardar
  el algoritmo, el salt y el resultado en formato versionado; usar constantes
  con nombre para iteraciones y tamaño del hash, y comparar los valores
  derivados en tiempo constante. Migrar la tabla SQLite existente de forma
  transaccional: derivar hashes para las filas antiguas, eliminar el campo con
  texto plano y crear los usuarios de demostración mediante el mismo hasher.
  Un fallo de migración debe impedir el arranque/autenticación y no dejar una
  conversión parcial silenciosa.
- **Archivos a tocar durante la implementación:**
  - `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java` — migración
    versionada, almacenamiento y lectura del hash.
  - `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java` — nuevo
    servicio pequeño para derivar y verificar hashes.
  - `auth/src/main/java/com/bcp/workshop/auth/AuthService.java` — nuevo límite
    de negocio para buscar la cuenta y verificar la contraseña, manteniendo el
    recurso HTTP separado de la persistencia.
  - `auth/src/main/java/com/bcp/workshop/auth/UserAccount.java` — registro
    interno que conserva el hash fuera del modelo de usuario de la respuesta.
  - `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java` — delegar la
    autenticación y conservar la respuesta HTTP observada.
  - `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y una
    prueba unitaria nueva `auth/src/test/java/com/bcp/workshop/auth/PasswordHasherTest.java`.
- **Prueba de caracterización antes de tocar producción:** ampliar primero
  `AuthResourceTest` para fijar el comportamiento del endpoint con credenciales
  de demostración en una base SQLite temporal: éxito `200` con todos los
  campos existentes, contraseña incorrecta y usuario inexistente `401`, y
  cuerpo/campos requeridos inválidos `400`. No usar ni alterar
  `auth/data/workshop.db`. Después, añadir pruebas de migración de una base
  antigua, verificación positiva/negativa, salts distintos para la misma
  contraseña y ausencia de contraseña en claro en el almacenamiento migrado.
- **Coordinación:** la búsqueda por username necesaria para verificar el hash
  debe usar `PreparedStatement`; esto se solapa con H-AUTH-01. Reutilizar el
  cambio de repositorio y coordinar su revisión, sin considerar H-AUTH-01
  resuelto sin su propia evidencia. Si H-AUTH-07 se aborda después, reutilizar
  `AuthService` en lugar de crear otra capa de negocio.

### H-AUTH-04 — Secreto fijo y CORS de origen amplio

- **Estado en el alcance inicial de H-AUTH-02: diferido.** No se cambió código
  de token ni configuración CORS en esa etapa. La aprobación posterior que
  amplió el alcance se registra al final de este plan.
- **Hallazgo de HALLAZGOS.md:** H-AUTH-04, `Pendiente`. El secreto está
  incrustado en `LegacyTokenUtil` y CORS permite cualquier origen. Evidencia:
  `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:8,13-15` y
  `auth/src/main/resources/application.properties:4-7`.
- **Problema:** el secreto está en el código y también se concatena al
  contenido codificado del token; Base64 no lo protege. La expresión CORS
  `/.*/` acepta orígenes sin una lista de confianza.
- **Cambio propuesto:** parametrizar los orígenes CORS con
  `AUTH_CORS_ORIGINS`, usando como valor local predeterminado
  `http://localhost:4200`, y restringir las respuestas CORS a los orígenes
  configurados. Mover la configuración del secreto a `AUTH_TOKEN_SECRET` y
  leerla mediante configuración de Quarkus, con un valor ficticio aislado en
  pruebas y sin secretos reales. No cambiar el contrato ni el formato de la
  respuesta de login.
- **Archivos a tocar durante la implementación:**
  - `auth/src/main/resources/application.properties` — reemplazar el patrón
    CORS abierto por la configuración externa de orígenes y declarar la
    propiedad externa del secreto.
  - `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java` y
    `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java` — consumir la
    configuración sin constantes secretas en código y conservar los campos
    de respuesta actuales.
  - `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` — cubrir
    los orígenes permitidos/no permitidos y la estabilidad del contrato de
    login.
  - `auth/src/test/resources/application.properties` — solo si hace falta
    aislar valores ficticios para la configuración de pruebas.
- **Prueba de caracterización antes de tocar producción:** ampliar primero
  `AuthResourceTest` para registrar el contrato actual de login (`200` y sus
  campos, más `401` para credenciales no válidas) y el comportamiento CORS
  actual con un origen de prueba. Durante la corrección, sustituir la
  expectativa de origen abierto por pruebas que verifiquen que el origen
  configurado recibe permiso y otro origen no; volver a comprobar que la
  respuesta y el estado de `POST /auth/login` no cambian. Usar únicamente
  valores de prueba, nunca un secreto real.
- **Coordinación y límite:** externalizar el valor no vuelve seguro el token
  actual, porque su contenido Base64 sigue incluyendo el secreto. La corrección
  completa de esa parte depende de H-AUTH-03 (token firmado); coordinar ambos
  cambios antes de retirar la constante y no declarar H-AUTH-04 completamente
  resuelto mientras el secreto siga expuesto en el token. H-AUTH-03 no se añade
  a esta selección ni se cambia aquí su contrato.

### Secuencia y evidencia

1. Aprobación humana comprobada para H-AUTH-02; H-AUTH-04 se difirió por
   decisión del usuario debido a su dependencia de H-AUTH-03.
2. La caracterización del endpoint se añadió y ejecutó antes de modificar
   producción. La prueba correctiva de almacenamiento también se ejecutó antes
   del cambio y falló al encontrar la columna de texto plano.
3. Tras la corrección, la prueba de migración, el contrato HTTP, el verificador
   de hash y las rutas de error pasan. JaCoCo informa 97.1% de cobertura de
   líneas de las clases de producción modificadas (166/171), por encima de la
   meta de 85%.
4. CodeQL y Dependency Review remotos no se ejecutaron en este recorrido local,
   conforme a la restricción indicada; no se silenció ni redujo ningún gate.
5. Los comandos, resultados y cobertura se conservan aquí. `docs/ENDPOINTS.md`
   y `docs/TESTING.md` no se crean hasta sus pasos de documentación.

### Registro de ejecuciones — auth / H-AUTH-02

Las pruebas se ejecutaron con cwd `auth/` y `AUTH_DB_URL=jdbc:sqlite:./target/auth-h02-7cb438d2.db`; nunca se usó `auth/data/workshop.db`. La base temporal se preparó con el esquema antiguo para verificar la migración. También se usaron y eliminaron bases temporales para los casos de esquema vacío e inválido.

| Etapa | Comando | Resultado |
|---|---|---|
| Caracterización antes de producción | `mvn -B -Dtest=AuthResourceTest test` | Pasó: 6 pruebas, 0 fallos. Cubrió login correcto y sus campos, 401 para contraseña/usuario no válidos y 400 para request/campos requeridos inválidos. |
| Prueba correctiva antes de producción | `mvn -B '-Dtest=AuthResourceTest#persistsVersionedPasswordHashesInsteadOfPlaintext' test` | Falló como se esperaba: `Plaintext password column must be removed`; confirmó que la tabla antigua aún conservaba `password`. |
| Compilación durante la implementación | `mvn -B verify` | Falló antes de ejecutar pruebas porque faltaba importar `jakarta.inject.Inject`; se corrigió el import. |
| Primera ejecución completa de integración | `mvn -B verify` | 10 pruebas, 1 fallo: la comprobación directa del esquema ocurrió antes de inicializar el bean/repositorio en esa prueba. La prueba se ajustó para activar primero el login y la migración. |
| Reejecución aislada de la prueba correctiva | `mvn -B '-Dtest=AuthResourceTest#persistsVersionedPasswordHashesInsteadOfPlaintext' test` | Pasó: 1 prueba, 0 fallos sobre el esquema ya migrado. |
| Reproducción con esquema antiguo recreado | `mvn -B verify` | 10 pruebas, 1 fallo por la misma comprobación directa anterior a la inicialización diferida; motivó que la propia prueba activara login antes de consultar el esquema. |
| Verificación tras corregir el orden de inicialización | `mvn -B verify` | Pasó: 10 pruebas, 0 fallos; cobertura agregada en las clases modificadas: 94.2%. |
| Verificación final | `mvn -B verify` | Pasó: 16 pruebas, 0 fallos; JaCoCo cubrió 166/171 líneas en las clases de producción modificadas (97.1%). |

El resultado final conserva la respuesta observada de `POST /auth/login`.
H-AUTH-01 permanece `Pendiente`: su consulta ahora está parametrizada como
parte del cambio de persistencia, pero no se le atribuye resolución sin sus
propias pruebas y revisión.

### Actualización del alcance tras la revisión local de seguridad

Después de la revisión, el usuario eligió `Fix all issues`, ampliando el trabajo
a las remediaciones de seguridad señaladas en auth:

- **H-AUTH-03:** se reemplazó el token Base64 por un JWT HS256 con claims
  `sub`, `role` y `exp`, firmado con HMAC y con secreto externo. La verificación
  del consumidor `api` se implementó después; ver la sección
  «H-AUTH-03 — Token sin firma (api)».
- **H-AUTH-04:** se externalizó el secreto con `AUTH_TOKEN_SECRET`, se exige
  una longitud mínima de 32 bytes y CORS se limita a `AUTH_CORS_ORIGINS`, cuyo
  valor predeterminado es el origen local documentado `http://localhost:4200`.
  Las pruebas cubren el origen permitido y el rechazo de otro origen.
- **H-AUTH-05:** el endpoint devuelve un mensaje genérico en errores `500` y
  conserva los detalles solo en el log del servidor.

H-AUTH-04 y H-AUTH-05 quedan `Resuelto` en `HALLAZGOS.md`. H-AUTH-06 sigue
`Pendiente`: no se ejecutaron SCA, CodeQL ni Dependency Review.

#### Registro de verificación — revisión de seguridad auth

Las pruebas se ejecutaron con cwd `auth/` y una base SQLite temporal bajo
`auth/target/`; no se usó `auth/data/workshop.db`.

| Etapa | Comando | Resultado |
|---|---|---|
| Regresión de token antes de la implementación | `mvn '-Dtest=AuthResourceTest#issuesSignedJwtToken' test` | Falló como se esperaba: el token anterior tenía 1 segmento, no los 3 segmentos JWT requeridos. |
| Integración enfocada | `mvn -B '-Dtest=AuthResourceTest' test` | Pasó: 13 pruebas, 0 fallos; incluyó firma y claims JWT, CORS permitido/denegado y respuesta de error genérica. |
| Verificación final | `mvn -B verify` | Pasó: 23 pruebas, 0 fallos. JaCoCo: `AuthResource` 26/26 líneas, `TokenSigner` 23/25; cobertura agregada de las clases de producción modificadas: 49/51 (96.1%). |

### H-AUTH-03 — Token sin firma (api)

Componente: `api`. Spec `HU-01.md`, decisión `ADR-H-AUTH-03.md`, hallazgo
`HALLAZGOS.md`. Las decisiones se aprobaron en la conversación del día 2: PyJWT,
retiro del formato Base64 legado, secreto por `AUTH_TOKEN_SECRET` en
`compose.yaml` con valor de desarrollo, y commit previo de los documentos.

El consumidor verificado es `api`: `main.py` era el único que decodificaba el
token Base64. `web/src/app/dashboard/dashboard.component.ts:44` solo envía el
header `Authorization`, y `auth/` solo emite el JWT.

| Etapa | Comando | Resultado |
|---|---|---|
| Línea base antes del cambio | `python tools/lab.py test api` | Pasó: 4 pruebas, cobertura 90.18%. |
| Rojo antes de implementar | `api\.venv\Scripts\python.exe -m pytest -p no:warnings --no-cov tests` (desde `api/`) | 6 fallidas, 17 pasadas. Fallaban el contrato JWT de `auth`, la identidad desde claims, el rechazo del token legado y las rutas seeded que usaban el token nuevo. |
| Verde tras implementar | `python tools/lab.py test api` | Pasó: 23 pruebas, cobertura total 93.10% (compuerta 85% cumplida). |
| Lint | `api\.venv\Scripts\python.exe -m pyflakes api\app api\tests` | Sin hallazgos (exit 0). |
| Contrato real | `auth/target/quarkus-app/quarkus-run.jar` con `AUTH_TOKEN_SECRET` de desarrollo; `POST /auth/login` (`student`) y `api` con la misma variable | `GET /api/tasks` con token real: 200. `GET /api/summary` con token real: 200, `owner` = `student`. `GET /api/backend-health` con token real: 200. Token legado Base64: 401. Sin token: 401. |
| Compose | `docker compose config` | No ejecutado: Docker no está instalado. Se validó la sintaxis con PyYAML. |

Pruebas añadidas en `api/tests/test_api.py`: aceptación del token del contrato,
identidad desde claims, rechazo de firma ajena, claims alterados, expiración,
falta de `role`, `alg: none`, token legado, ausencia de secreto (falla
cerrado), y rechazo 401 en cada ruta protegida con token forjado o ausente.

No se ejecutaron CodeQL, Dependency Review ni JaCoCo remotos. Los advisories SCA
siguen fuera de alcance (H-AUTH-06).

#### Parametrización SQL en `api` (alertas CodeQL `py/sql-injection`)

CodeQL reportó tres alertas High en `api/app/main.py` (consultas de
`/api/tasks`, `POST /api/tasks` y `/api/summary`). Las tres consultas usan ahora
placeholders `?` con tupla de parámetros. El comportamiento con entradas
normales no cambia. Por la política de `owner` aprobada, `/api/tasks`,
`POST /api/tasks` y `/api/summary` usan siempre la identidad del token: `?owner=`
y el campo `owner` del payload se ignoran. Se añadieron dos pruebas: `?owner=`
con `x' OR '1'='1'` y un payload con `owner` ajeno no cambian el resultado; un
`title` con comillas y `DROP TABLE` se guarda como texto. Resultado local:
`python tools/lab.py test api` pasó con 25 pruebas y cobertura 92.98%; pyflakes
sin hallazgos. No se verificó que las pruebas nuevas fallen sobre el código
anterior. La alerta se cierra solo cuando CodeQL reanalice el nuevo commit.

Limitaciones que siguen abiertas: la raíz de la inyección es H-AUTH-01
(Pendiente). `web` todavía envía `?owner=`, que ahora se ignora; limpiarlo es un
cambio separado en `web`.

#### Estrategia de reversión (propuesta)

- Unidad de reversión: los commits de H-AUTH-03 en `feat/h-auth-03-firma-token`.
  No hay migración de datos, así que revertir no requiere limpieza de base.
- Revertir solo `api` no es seguro: restaura la aceptación del token Base64 sin
  firma (vulnerabilidad original). Usarlo solo como contención temporal, con
  aprobación explícita y registro en `HALLAZGOS.md`.
- Efecto colateral conocido: con `api` revertido, `web` (que envía el JWT del
  emisor) dejará de funcionar contra `api`. Ese fallo es esperado y no se debe
  parchear en `web`.
- Retirar `PyJWT==2.15.1` de `api/requirements.txt` solo al revertir el commit
  de la dependencia. `AUTH_TOKEN_SECRET` en `compose.yaml` puede quedar sin usar.
- Preferencia: corregir hacia adelante (por ejemplo, ajustar la configuración del
  secreto) antes que revertir.

