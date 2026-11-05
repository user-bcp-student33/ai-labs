# user_story.md

## H-AUTH-01 — Usar consultas parametrizadas en el login de auth

**Como** persona que inicia sesión,  
**quiero** que el servicio de autenticación consulte mis credenciales mediante parámetros SQL,  
**para** que los valores enviados no puedan alterar la consulta ni permitir un acceso no autorizado.

### Contexto y alcance

El hallazgo H-AUTH-01 identifica una consulta construida mediante concatenación en `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`, método `findByCredentials`. La modificación se limita a `auth/` y conserva el contrato de `POST /auth/login`.

El material recibido nombra `LegacyDatabase.findByCredentials` y `AuthResourceTest`, pero no incluye el código fuente ni las declaraciones de las interfaces existentes. Por ello, los límites e interfaces descritos abajo son la propuesta arquitectónica; antes de implementarla, Backend debe cotejarlos con el código actual y conservar los tipos y formatos que ya estén publicados.

### Componentes y límites

La solución debe mantener las dependencias dirigidas hacia el dominio:

- **Adaptador de entrada — `AuthResource` (REST):** recibe `POST /auth/login`, valida o transforma la solicitud y delega en el puerto de entrada. No construye SQL ni accede directamente a la base de datos.
- **Puerto de entrada — caso de uso de login:** expone la operación de autenticación a los adaptadores. Puede conservarse la interfaz existente si cumple esta función.
- **Servicio de aplicación — servicio de login:** coordina la búsqueda de credenciales y, si esta tiene éxito, el comportamiento actual de emisión del token. Depende de puertos del dominio, no de clases de infraestructura.
- **Dominio:** contiene los modelos necesarios para representar al usuario autenticado y el resultado del caso de uso. No importa clases de infraestructura.
- **Puerto de salida — búsqueda de credenciales:** define la operación que requiere la aplicación para localizar un usuario por las credenciales recibidas.
- **Adaptador de salida — `LegacyDatabase`:** implementa la búsqueda con `PreparedStatement`, asignando `username` y `password` a parámetros. La consulta debe tener estructura fija; los valores no se concatenan en el SQL.
- **Token y configuración:** se conserva el mecanismo actual de emisión del token. Si depende de un servicio externo, la aplicación lo consume mediante su puerto correspondiente.

`LegacyDatabase` debe quedar en la capa de infraestructura, aunque su ubicación actual no se haya podido verificar. Esta feature no requiere cambiar el formato del token ni ampliar el alcance fuera de `auth/`.

### Contrato de API

**Ruta:** `POST /auth/login`  
**Content-Type:** `application/json`

**Entrada:**

```json
{
  "username": "student",
  "password": "Workshop123!"
}
```

Se asume que estos nombres de campos corresponden al contrato existente; Backend debe confirmarlo con la implementación actual.

**Éxito — `200 OK`:**

```json
{
  "user": {
    "username": "student"
  },
  "token": "<token emitido por el mecanismo actual>"
}
```

La respuesta debe conservar los campos que ya entregue el endpoint, incluyendo `user.username` y `token`.

**Credenciales incorrectas o intento de inyección — `401 Unauthorized`:**

La respuesta no debe incluir `token`. El cuerpo siguiente es ilustrativo; debe conservarse el formato de error existente si ya está definido:

```json
{
  "error": "Invalid credentials"
}
```

**Otros errores:** para solicitudes malformadas o incompletas se conserva el comportamiento vigente del endpoint. Los errores internos no deben exponer SQL, credenciales ni detalles de infraestructura. El status y el formato actuales para estos casos deben confirmarse durante la revisión del código.

### Recorrido de una petición

1. El cliente envía `username` y `password` a `POST /auth/login`.
2. El adaptador REST traduce la solicitud y llama al caso de uso de login.
3. El servicio de aplicación solicita al puerto de salida la búsqueda del usuario.
4. `LegacyDatabase` ejecuta la consulta con una estructura fija y vincula los valores como parámetros —por ejemplo, con la forma `WHERE username = ? AND password = ?`, adaptada al SQL existente—. Los valores recibidos no se incorporan a la cadena SQL.
5. Si no hay coincidencia, el caso de uso devuelve el resultado de credenciales inválidas. El adaptador REST responde `401` sin token.
6. Si hay coincidencia, se conserva el comportamiento actual de autenticación y emisión del token. El adaptador responde `200` con `user.username` y `token`.

### Ejemplos

**Inicio de sesión válido**

Solicitud:

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "username": "student",
  "password": "Workshop123!"
}
```

Respuesta esperada:

```http
HTTP/1.1 200 OK
```

```json
{
  "user": {
    "username": "student"
  },
  "token": "<token emitido por el mecanismo actual>"
}
```

**Intento de inyección como usuario**

```json
{
  "username": "' OR '1'='1",
  "password": "Workshop123!"
}
```

**Intento de inyección como contraseña**

```json
{
  "username": "student",
  "password": "' OR '1'='1"
}
```

Para ambos casos, con las credenciales del usuario de prueba indicadas, la respuesta esperada es `401 Unauthorized` y no contiene `token`. Los valores deben tratarse como datos literales, no como parte de la consulta SQL.

### Criterios verificables

- [ ] `findByCredentials` utiliza `PreparedStatement` y vincula `username` y `password` mediante parámetros.
- [ ] La consulta SQL no concatena los valores de `username` ni de `password`.
- [ ] `POST /auth/login` con `student` y `Workshop123!` responde `200` e incluye `user.username` y `token`.
- [ ] Un intento de inyección en el campo `username` responde `401` y no incluye `token`.
- [ ] Un intento de inyección en el campo `password` responde `401` y no incluye `token`.
- [ ] `AuthResourceTest` incluye pruebas de los intentos de inyección y verifica que no se devuelve un token.
- [ ] Las pruebas existentes y nuevas pasan ejecutando `mvn test` desde `auth/`.

### Supuestos pendientes y decisiones

**Datos**

- Confirmar el SQL, los nombres de columnas y el esquema de credenciales que usa actualmente `LegacyDatabase`.
- Confirmar que los datos de prueba hacen que los dos intentos de inyección anteriores sean credenciales inválidas.
- Revisar si la comparación o el almacenamiento de contraseñas tienen particularidades que deban preservarse. Esta feature no propone cambiar su política.

**Backend**

- Revisar las interfaces y el flujo actuales de `AuthResource`, el servicio de login y `LegacyDatabase` para asignar la solución a los puertos y adaptadores existentes sin introducir dependencias hacia infraestructura.
- Confirmar el formato actual del cuerpo de error `401` y cualquier comportamiento para solicitudes inválidas; mantenerlos salvo que exista una decisión independiente de cambio de contrato.
- Conservar el mecanismo y el formato actuales del token. No emitirlo cuando las credenciales no coincidan.
- Añadir las pruebas de inyección para ambos campos y ejecutar `mvn test` desde `auth/`.

**Frontend**

- No se requiere modificar la ruta ni los nombres de los campos si se confirma el contrato actual.
- Verificar que el cliente maneje `401` como credenciales inválidas y no dependa de la presencia de `token` en respuestas de error.
- No enviar ni registrar contraseñas en mensajes de error o telemetría.