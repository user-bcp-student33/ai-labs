# Revisión técnica final — H-AUTH-01

## VEREDICTO: RECHAZADO

La evidencia recibida no demuestra el cumplimiento integral de los criterios de aceptación: el fragmento presentado usa parámetros SQL, pero no se ha verificado que esté integrado en la clase usada por el endpoint; faltan el código del recurso y de `AuthResourceTest`; y no hay resultados de pruebas ejecutadas. El informe de pruebas declara explícitamente **NO EJECUTADO**. Por tanto, la corrección y la compatibilidad del contrato siguen pendientes de verificación.

Este veredicto es técnico y no otorga, reemplaza ni modifica las aprobaciones humanas del flujo.

## Evaluación criterio por criterio

| Criterio | Estado | Evidencia y evaluación |
|---|---|---|
| `findByCredentials` usa `PreparedStatement` para `username` y `password`, sin concatenar valores en el SQL. | **No demostrado en la implementación integrada** | El fragmento de `LegacyDatabase` incluye una consulta con marcadores `?` y asigna ambos valores con `setString`. Esto demuestra el patrón en el fragmento recibido, no que sea el archivo usado por el endpoint. El origen identifica `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`; el fragmento declara `package pe.bcp.login.service`. Además, su SQL presupone una tabla `users` y columnas `username` y `password`, esquema que no fue confirmado. |
| `POST /auth/login` con `student` y `Workshop123!` responde 200 con `user.username` y `token`. | **No demostrado** | No se recibió la implementación completa de `AuthResource` ni un resultado de una petición o prueba ejecutada. La especificación frontend y el contrato arquitectónico describen la respuesta esperada, pero no prueban el comportamiento del backend. |
| El payload `' OR '1'='1` como usuario o contraseña responde 401 y no devuelve `token`. | **No demostrado** | El informe de pruebas enumera ambos casos como pruebas previstas, pero marca API-02 y API-03 como **NO EJECUTADO**. No se proporcionó evidencia de respuestas reales del endpoint ni de aserciones ejecutadas. |
| `AuthResourceTest` incluye una prueba de inyección y `mvn test` pasa desde `auth/`. | **No demostrado** | No se proporcionó el código de `AuthResourceTest` ni una salida de Maven. El informe declara INT-01 y BUILD-01 **NO EJECUTADO**. Tampoco se puede confirmar que la prueba cubra ambos campos. |

## Compatibilidad entre etapas

- **Arquitectura:** Se propone un flujo por adaptadores y puertos: `AuthResource` delega en el caso de uso y `LegacyDatabase` actúa como adaptador de persistencia. Es una propuesta, no una arquitectura contrastada con el código existente. No se recibieron las interfaces ni la implementación del recurso y del servicio que permitan comprobar las dependencias y el recorrido real.
- **Persistencia:** El fragmento de backend propone `SELECT username FROM users WHERE username = ? AND password = ?`, con parámetros. No se proporcionaron el esquema real, las migraciones, la configuración de base de datos ni pruebas de persistencia. La compatibilidad del SQL y de los tipos con los datos existentes queda sin confirmar.
- **Implementación y contrato HTTP:** La especificación frontend y el servicio Angular visible usan `POST /auth/login` y esperan `user.username` y `token`, lo cual coincide con el contrato de aceptación. Sin embargo, no se recibió la implementación completa del backend que permita confirmar que entrega esa respuesta y conserva el comportamiento de error 401. El fragmento del componente Angular está truncado y tampoco acredita la integración completa.
- **Pruebas:** El plan describe comprobaciones útiles, pero no equivale a resultados observados. No se informa ninguna ejecución satisfactoria.
- **Seguridad:** El informe de seguridad considera que la parametrización es correcta en el fragmento recibido, pero califica su integración como no verificada. No se ejecutaron peticiones HTTP ni pruebas automatizadas. Por tanto, no hay resultado de seguridad que demuestre que el riesgo original quedó corregido en el artefacto integrado.

## Hallazgos y acciones requeridas

### H-01 — Integración de la corrección SQL no verificada

- **Severidad:** Alta; bloqueante para esta aceptación.
- **Evidencia:** El origen ubica la clase en `com.bcp.workshop.auth`; el fragmento recibido declara `pe.bcp.login.service`. El propio desarrollador indica que el fragmento asume el esquema y que no pudo escribir una prueba ejecutable de `AuthResourceTest` sin el código existente.
- **Responsable:** Backend.
- **Cambio requerido:** Confirmar e integrar la consulta parametrizada en la clase `LegacyDatabase` efectivamente utilizada por `POST /auth/login`. Cotejar el SQL con el esquema real y comprobar que el endpoint usa esa implementación, sin una ruta alternativa que concatene credenciales.
- **Cómo verificar:** Revisar el archivo integrado y el flujo de dependencias del endpoint; comprobar que `username` y `password` se enlazan mediante parámetros y que no se incorporan a la cadena SQL. Ejecutar las pruebas de integración indicadas abajo.

### H-02 — Contrato de login y respuestas de autenticación sin evidencia de ejecución

- **Severidad:** Bloqueante para los criterios HTTP.
- **Evidencia:** No se proporcionaron el recurso REST completo ni resultados de solicitudes. Las respuestas `200` y `401` constan como contrato esperado, no como resultados observados.
- **Responsable:** Backend; QA para la verificación.
- **Cambio requerido:** Confirmar que el login válido conserva `user.username` y `token`; que las credenciales inválidas, incluidos ambos payloads, producen `401` sin la propiedad `token`; y que los errores no exponen SQL ni detalles de infraestructura.
- **Cómo verificar:** Ejecutar pruebas de integración contra el endpoint en un entorno aislado con la cuenta de prueba y registrar las aserciones de estado y cuerpo, sin guardar ni divulgar tokens.

### H-03 — Cobertura y ejecución de `AuthResourceTest` y Maven pendientes

- **Severidad:** Bloqueante para el cuarto criterio.
- **Evidencia:** No se recibió `AuthResourceTest` ni una salida de `mvn test`. El informe de pruebas marca INT-01 y BUILD-01 **NO EJECUTADO**.
- **Responsable:** Backend, para implementar o actualizar las pruebas; QA, para ejecutarlas y reportar resultados.
- **Cambio requerido:** Añadir o confirmar pruebas de inyección en `username` y en `password`, con aserciones de `401` y ausencia de `token`, y conservar una prueba de login válido.
- **Cómo verificar:** Ejecutar `mvn test` desde `auth/` y entregar la salida completa que demuestre que la suite terminó correctamente, junto con la identificación de los casos ejecutados.

### H-04 — Compatibilidad del SQL y cuestiones de seguridad asociadas sin confirmar

- **Severidad:** Pendiente de evaluación; no se afirma que exista un defecto adicional.
- **Evidencia:** El fragmento consulta y compara directamente una columna llamada `password`, pero no se proporcionaron el esquema ni la política de almacenamiento. El informe de seguridad señala que no puede confirmar si hay hashing. Tampoco se proporcionó el manejo integrado de excepciones del endpoint.
- **Responsable:** Backend y responsable de persistencia; Seguridad, para la revisión.
- **Cambio requerido:** Confirmar que la consulta corresponde al esquema y al mecanismo de credenciales vigentes. Revisar, sin ampliar automáticamente el alcance de H-AUTH-01, si el almacenamiento de contraseñas y el manejo de errores requieren una corrección independiente.
- **Cómo verificar:** Inspeccionar el esquema y el flujo real de autenticación; comprobar que los errores de infraestructura no devuelven SQL, credenciales ni detalles internos. Si se identifica un problema distinto, registrarlo y aprobar su alcance por el proceso correspondiente.

## Pruebas pendientes e impacto

1. **Inspección de la implementación integrada de `LegacyDatabase`:** necesaria para demostrar el primer criterio y descartar que el fragmento sea solo una propuesta no conectada al endpoint.
2. **Confirmación del esquema y prueba de persistencia:** necesaria para verificar que el SQL parametrizado consulta las tablas y columnas reales y que credenciales inválidas no encuentran un usuario.
3. **Prueba de integración de login válido:** necesaria para demostrar el estado 200 y la presencia de `user.username` y `token`.
4. **Prueba de inyección en usuario y contraseña:** necesaria para demostrar que ambos valores se tratan como datos y que el endpoint responde 401 sin `token`.
5. **Inspección de `AuthResourceTest`:** necesaria para demostrar que la prueba solicitada existe y cubre los casos pertinentes.
6. **Ejecución de `mvn test` desde `auth/`:** necesaria para demostrar que las pruebas del módulo pasan. No se proporcionó resultado.
7. **Verificación de errores y de la revisión de seguridad:** necesaria para confirmar que no se filtran detalles internos y para cerrar las cuestiones de seguridad señaladas como no evaluadas.

Mientras estas verificaciones no se completen, no puede afirmarse que el hallazgo de severidad alta esté cerrado ni que todos los criterios de aceptación estén satisfechos.