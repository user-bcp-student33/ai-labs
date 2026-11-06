# Diseño técnico

## Contexto técnico

El cambio aprobado aborda el Issue #15 (H-AUTH-01) y se limita a `auth/`.
`AuthResource.login` atiende `POST /auth/login`, valida que usuario y contraseña
no estén vacíos, consulta `LegacyDatabase.findByCredentials` y, si encuentra
una cuenta, genera el token y arma la respuesta. `LegacyDatabase` usa SQLite
mediante JDBC; actualmente construye la consulta con concatenación de los dos
valores y la ejecuta con `Statement`.

La respuesta exitosa actual incluye `token`, `expiresIn` y `user` (con
`username`, `displayName` y `role`), además del campo `debug`. La prueba de
integración existente caracteriza el estado `200`, `user.username` y la
presencia del token, pero no cubre credenciales inválidas ni intentos de
inyección.

## Goals / non-goals

### Goals

- Hacer que los valores de usuario y contraseña se interpreten exclusivamente
  como datos de la consulta de credenciales.
- Rechazar con `401`, sin token, los intentos de inyección indicados por la
  spec, tanto en el usuario como en la contraseña.
- Mantener el login válido y su respuesta observable sin cambios.
- Mantener el cambio pequeño, reversible y limitado al flujo de login de
  `auth/`.

### Non-goals

- Cambiar `POST /auth/login`, el formato JSON de entrada ni la respuesta.
- Cambiar la emisión, formato o validación del token.
- Cambiar el esquema, las cuentas iniciales, el almacenamiento de credenciales
  o la configuración de SQLite.
- Refactorizar las capas de autenticación, cambiar dependencias o modificar
  `api/`, `web/` u otros flujos.
- Resolver el manejo general de errores de base de datos o la deuda de
  seguridad no incluida en la spec.

## Contratos protegidos

- Ruta y método: `POST /auth/login`, con entrada JSON que incluye `username`
  y `password`.
- Login válido: estado `200`, token presente y `user.username` igual al
  usuario autenticado. Se conserva el payload existente, incluidos sus demás
  campos.
- Credenciales no coincidentes o manipuladas: estado `401` y ausencia de
  `token`.
- No se alteran las respuestas de validación de campos vacíos ni la semántica
  del resto del endpoint.

## Componentes afectados

- `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`: única
  modificación funcional prevista, en la consulta de credenciales.
- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`: pruebas de
  integración del endpoint para caracterizar el contrato y cubrir la regresión.
- `AuthResource`, `LegacyTokenUtil`, el esquema SQLite y los demás componentes
  no requieren cambios para cumplir este alcance.

## Decisiones técnicas y alternativas

### Decisión: parámetros JDBC en la consulta existente

Reemplazar la interpolación de valores por una consulta con marcadores (`?`) y
asignar usuario y contraseña con `PreparedStatement.setString`. Mantener las
columnas seleccionadas, el predicado de coincidencia, el método
`findByCredentials` y su contrato de retorno. Así, el motor compara los valores
recibidos con los campos de la cuenta sin interpretar su contenido como SQL.

**Alternativas consideradas**

- **Escapar o filtrar caracteres en la entrada:** descartada; mantiene la
  construcción dinámica de SQL y no es una frontera fiable para separar código
  de datos.
- **Migrar a ORM o rediseñar repositorio/servicio:** descartada; excede la
  corrección puntual y aumenta el riesgo de cambios en autenticación y
  persistencia.
- **Cambiar la consulta, el token o el esquema conjuntamente:** descartada; no
  es necesario para satisfacer la spec y dificulta aislar la causa y revisar
  la regresión.

## Estrategia incremental

1. Añadir primero pruebas en `AuthResourceTest`: conservar y fortalecer la
   caracterización del login válido; cubrir credenciales ordinarias inválidas
   y las dos entradas de inyección especificadas, comprobando `401` y que no
   exista token.
2. Ejecutar las pruebas nuevas antes de cambiar la consulta. Los casos de
   inyección son pruebas de regresión basadas en la spec y se espera que
   evidencien el comportamiento vulnerable actual; no se debe adaptar la
   expectativa para que pase con la consulta antigua.
3. Cambiar únicamente `findByCredentials` para preparar y parametrizar la
   consulta, conservando el mapeo de `UserRecord` y el comportamiento del
   endpoint.
4. Ejecutar la suite de `auth` (`python tools/lab.py test auth`) y revisar el
   informe de JaCoCo para el código modificado. No silenciar ni excluir
   controles de calidad; la meta del componente es al menos 85 % de cobertura
   del código modificado.

## Pruebas de caracterización necesarias

- Login válido del usuario existente: estado `200`, `user.username` y token
  presente; comprobar también que el payload conservado mantenga los campos
  observables existentes.
- Usuario o contraseña incorrectos: estado `401` y ausencia de token.
- `' OR '1'='1` como usuario, con la otra credencial de prueba válida: estado
  `401` y ausencia de token.
- `' OR '1'='1` como contraseña, con el usuario de prueba válido: estado
  `401` y ausencia de token.

Las pruebas deben ejercitar el endpoint y la persistencia real de prueba, sin
introducir secretos reales ni modificar el esquema. Las dos pruebas de
inyección son los criterios de aceptación centrales, no una razón para cambiar
el contrato de otros errores.

## Riesgos / trade-offs

- La prueba de integración depende de la inicialización de SQLite y de sus
  cuentas de prueba; debe usar el aislamiento y configuración ya establecidos
  por `auth`.
- Una consulta parametrizada evita que los valores inyecten sintaxis SQL, pero
  no resuelve otros riesgos de autenticación ni reemplaza almacenamiento seguro
  de contraseñas; esos temas permanecen fuera del alcance aprobado.
- Se conserva el comportamiento actual de errores de base de datos y de
  serialización. Cambiarlo en este trabajo podría modificar respuestas
  observables no definidas por la spec.
- La modificación es deliberadamente estrecha: no añade dependencias ni
  requiere migración de datos.

## Rollback

No hay migración ni escritura de datos asociada al cambio, por lo que no se
requiere rollback de esquema o datos. Revertir la parametrización restauraría
la vulnerabilidad aprobada para corregir; ante una regresión, se prefiere
corregir hacia adelante. Un rollback de despliegue sería solo una medida de
emergencia consciente de ese riesgo y no debe eliminar las pruebas de
regresión.

## Decisiones abiertas

No se identifican decisiones de diseño abiertas para el alcance aprobado. La
implementación debe detenerse para revisión humana antes de ampliar el cambio
a otros componentes o contratos.
