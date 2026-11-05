# security_report.md

## Resumen

La entrega contiene un fragmento de `LegacyDatabase` que usa `PreparedStatement` y vincula `username` y `password` como parámetros. Ese fragmento corrige el patrón de inyección descrito para la implementación anterior. Sin embargo, no se puede confirmar que corresponda al archivo integrado: el origen señala `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`, mientras que el fragmento recibido declara `package pe.bcp.login.service`. Tampoco se proporcionaron el recurso REST completo ni el código de `AuthResourceTest`.

Por lo tanto, la mitigación de H-AUTH-01 es **aparente en el fragmento recibido, pero su integración y sus pruebas están pendientes de verificación**. No se ejecutaron pruebas ni peticiones HTTP.

## Alcance revisado

Se revisó únicamente el material incluido en el historial:

- El contexto y la evidencia del hallazgo original H-AUTH-01.
- El fragmento propuesto de `LegacyDatabase`.
- La especificación de arquitectura y contrato de `POST /auth/login`.
- La especificación frontend y el fragmento visible de `AuthApiService` y `LoginComponent` (este último está truncado).
- El informe de pruebas, que indica que las verificaciones no fueron ejecutadas.

Fronteras de confianza identificadas en el material:

1. El cliente envía credenciales no confiables a `POST /auth/login`.
2. El backend usa esos valores al consultar la base de datos.
3. Tras autenticar, el backend devuelve una respuesta que incluye un token.
4. Según la especificación frontend, el componente entrega la respuesta completa —incluido el token— al componente contenedor mediante `authenticated`.

No se recibieron el código completo del endpoint, la configuración, el esquema de base de datos, la plantilla Angular, la gestión de sesión del contenedor ni las rutas protegidas. No es posible evaluar integralmente autorización, IDOR, almacenamiento del token, configuración de secretos o exposición de errores.

## Hallazgos (riesgos detectados)

### H-01 — Inyección SQL en el login: corrección no verificada en la entrega integrada

- **Severidad:** Alta
- **Archivo:** `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java` (ruta indicada por el origen; no se verificó el archivo integrado).
- **Ubicación:** `LegacyDatabase.findByCredentials`; afecta el flujo `POST /auth/login`.
- **Impacto:** Si la implementación concatenada descrita en el hallazgo sigue activa, una entrada manipulada puede alterar la consulta y permitir autenticación sin credenciales válidas, con posible emisión de un token.
- **Evidencia recibida:**
  - El origen indica literalmente: “`LegacyDatabase.findByCredentials` arma la consulta SQL concatenando `username` y `password`”.
  - También indica: “Un valor como `' OR '1'='1` cambia la consulta y permite iniciar sesión sin conocer la contraseña”.
  - En contraste, el fragmento propuesto contiene SQL parametrizado:
    ```java
    "SELECT username FROM users WHERE username = ? AND password = ?"
    ```
    y asigna los valores mediante:
    ```java
    statement.setString(1, username);
    statement.setString(2, password);
    ```
- **Estado:** El fragmento mostrado evita concatenar credenciales en el SQL. No se puede confirmar que haya reemplazado la clase referida por el hallazgo: el paquete declarado en el fragmento es `pe.bcp.login.service`, distinto del paquete indicado en la ruta de origen (`com.bcp.workshop.auth`). Tampoco se recibió la clase integrada ni una salida de pruebas.
- **Corrección propuesta:** Confirmar que la implementación parametrizada está integrada en el `LegacyDatabase` realmente usado por el endpoint, que los dos valores se enlazan como parámetros y que no existe otro camino de autenticación con SQL concatenado. Añadir o confirmar pruebas de `AuthResourceTest` para ambos campos y conservar el contrato de éxito y error existente.

No se confirma una vulnerabilidad activa en el fragmento parametrizado; el riesgo actual es que la corrección no esté integrada o probada en el artefacto entregado.

## Correcciones propuestas

1. **Cerrar H-01:** Integrar la consulta parametrizada en la clase efectivamente usada por `POST /auth/login`. Mantener la estructura SQL fija y enlazar los valores con `PreparedStatement`.
2. **Probar el contrato:** Incluir en `AuthResourceTest` casos de inyección tanto en `username` como en `password`, verificando `401` y ausencia de la propiedad `token`. Conservar además la prueba de login válido.
3. **Verificar almacenamiento de contraseñas:** El fragmento consulta `WHERE username = ? AND password = ?` y vincula directamente el valor recibido. No se muestra hashing; el esquema y el formato almacenado no fueron proporcionados, así que no se confirma almacenamiento en texto claro. Si la columna contiene contraseñas sin hash, cambiar el flujo para obtener el usuario por nombre y verificar la contraseña con un hash adaptativo y salado, por ejemplo Argon2id, bcrypt o scrypt.
4. **Revisar el manejo de errores y secretos:** Confirmar que el manejo de `DatabaseAccessException` no devuelve al cliente la causa SQL, stack traces ni detalles de infraestructura. Revisar la configuración real del secreto de firma del token y las credenciales de base de datos; no se proporcionó para esta revisión.
5. **Revisar la integración frontend:** Confirmar en la aplicación completa dónde se recibe y gestiona el token emitido por `authenticated`; no se proporcionaron el componente contenedor ni la política de sesión.

## Comprobaciones reproducibles

Ejecutar únicamente en un entorno local o de pruebas aislado. Definir `AUTH_BASE_URL` con la URL real del backend de pruebas. Los siguientes ejemplos describen las peticiones y resultados que se deben comprobar; no se ejecutaron en esta revisión.

### H-01: login válido

Enviar a `POST /auth/login`:

```json
{
  "username": "student",
  "password": "Workshop123!"
}
```

- **Antes de la corrección:** El comportamiento esperado por el contrato es `200`; confirmar que contiene `user.username == "student"` y un `token` no vacío.
- **Después de la corrección:** Debe conservarse el mismo resultado: `200`, `user.username` y `token`.

Ejemplo:

```bash
curl -i -H 'Content-Type: application/json' \
  --data-binary '{"username":"student","password":"Workshop123!"}' \
  "${AUTH_BASE_URL%/}/auth/login"
```

No conservar ni compartir el token de la respuesta.

### H-01: inyección en `username`

Enviar:

```json
{
  "username": "' OR '1'='1",
  "password": "Workshop123!"
}
```

- **Antes de la corrección:** En la implementación vulnerable descrita por el origen, el payload puede alterar la consulta y autenticar indebidamente; comprobarlo solo contra una copia aislada de esa versión y con los datos de prueba correspondientes.
- **Después de la corrección:** Debe responder `401 Unauthorized` y el cuerpo no debe contener la propiedad `token`.

Ejemplo:

```bash
curl -i -H 'Content-Type: application/json' \
  --data-binary @- "${AUTH_BASE_URL%/}/auth/login" <<'JSON'
{"username":"' OR '1'='1","password":"Workshop123!"}
JSON
```

### H-01: inyección en `password`

Enviar:

```json
{
  "username": "student",
  "password": "' OR '1'='1"
}
```

- **Antes de la corrección:** En la implementación vulnerable descrita por el origen, el payload puede alterar la consulta y autenticar indebidamente; comprobarlo solo contra una copia aislada de esa versión y con los datos de prueba correspondientes.
- **Después de la corrección:** Debe responder `401 Unauthorized` y el cuerpo no debe contener la propiedad `token`.

Ejemplo:

```bash
curl -i -H 'Content-Type: application/json' \
  --data-binary @- "${AUTH_BASE_URL%/}/auth/login" <<'JSON'
{"username":"student","password":"' OR '1'='1"}
JSON
```

### Pruebas automatizadas e integración

1. Confirmar que `AuthResourceTest` incluye casos para las dos peticiones de inyección anteriores.
2. En cada caso, verificar estado `401` y ausencia de `token` en el cuerpo.
3. Verificar el login válido con `student` y `Workshop123!`, esperando `200`, `user.username` y `token`.
4. Desde el directorio `auth/`, ejecutar:
   ```bash
   mvn test
   ```
5. Revisar el resultado y confirmar que las pruebas terminan sin fallos.

- **Antes de la corrección:** La prueba de inyección debe reproducir el comportamiento vulnerable en una copia aislada de la versión afectada; la prueba de regresión documenta ese fallo.
- **Después de la corrección:** Las pruebas de inyección deben pasar con `401` y sin token; el login válido debe seguir pasando con `200`.

## Verificaciones ejecutadas

- **Revisión documental del material proporcionado:** realizada; se observó el fragmento parametrizado y la diferencia entre el paquete del fragmento y la ruta indicada por el origen.
- **Peticiones HTTP, inspección del artefacto integrado y ejecución de pruebas:** **NO EJECUTADO**.
- No se recibió salida de `mvn test` ni de `AuthResourceTest`; no se afirma que las pruebas pasen.

## Pendientes

- Obtener el archivo integrado `LegacyDatabase.java` y confirmar su ruta, paquete y uso por el endpoint.
- Obtener `AuthResource`, el servicio de login y el manejo de errores para revisar autenticación, respuestas y exposición de detalles internos.
- Obtener `AuthResourceTest` y la salida real de `mvn test` desde `auth/`.
- Confirmar el esquema de usuarios y si las contraseñas se almacenan mediante hashes adaptativos o en texto claro.
- Revisar la configuración de base de datos y de firma del token para detectar secretos expuestos o configuración insegura.
- Obtener el frontend completo, incluida la plantilla y el componente contenedor, para evaluar el tratamiento del token y el renderizado de datos.
- No se proporcionaron endpoints protegidos ni reglas de autorización; por ello, el acceso a recursos ajenos (IDOR) y la autorización por rol quedan **PENDIENTES**.
- La validación visible del formulario es requerida en cliente; no se recibió suficiente código del backend para evaluar límites, validación de entradas ni el comportamiento ante solicitudes malformadas.

## Conclusión

El fragmento recibido de `LegacyDatabase` muestra una corrección adecuada frente a la inyección SQL: SQL de estructura fija y parámetros enlazados. La evidencia disponible no demuestra que esa implementación esté integrada en la clase señalada por el hallazgo ni que las pruebas requeridas existan o pasen. H-01 queda pendiente de cierre hasta verificar el archivo efectivo y ejecutar las comprobaciones reproducibles. No hay evidencia suficiente para concluir sobre autorización/IDOR, secretos, almacenamiento de contraseñas o gestión integral del token.