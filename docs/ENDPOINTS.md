# Contratos HTTP — auth

## Estado del contrato

- **Observado:** esta página documenta el comportamiento del componente
  `auth/`, comprobado en el código y mediante una ejecución local con una base
  SQLite temporal.
- **Acordado:** no se encontró una aprobación humana ni una especificación
  externa que convierta estos comportamientos observados en un contrato
  formalmente aprobado. No se afirma que exista tal aprobación.
- **Riesgos y preguntas pendientes:** se indican en cada endpoint. En
  particular, no se inspeccionaron los consumidores de tokens fuera de
  `auth/`.

## `GET /q/health`

### 1. Qué hace

Indica si el servicio de autenticación está respondiendo; no autentica a un
usuario ni comprueba sus credenciales. La ruta la proporciona la extensión de
health de Quarkus declarada en `auth/pom.xml:40`.

### 2. Autenticación y headers

- **Autenticación requerida:** no.
- **Header `Authorization`:** ninguno.
- **Header de solicitud necesario:** ninguno para una solicitud GET sin cuerpo.

### 3. Request

Este GET no acepta un cuerpo JSON.

| campo | tipo | obligatorio | descripción |
|---|---|---|---|
| — | — | — | No hay campos de request; el endpoint no recibe cuerpo JSON. |

Ejemplo de solicitud:

```http
GET /q/health HTTP/1.1
Host: localhost:8080
```

### 4. Response exitosa

- **Estado:** `200 OK`.
- **Resultado observado en ejecución:** `status` fue `UP` y `checks` fue una
  lista vacía.

| campo | tipo | descripción |
|---|---|---|
| `status` | string | Estado general del servicio. El valor observado con la configuración actual es `UP`. |
| `checks` | array de objetos | Resultados de los checks registrados. La ejecución observada devolvió una lista vacía. |

Ejemplo JSON completo observado:

```json
{
  "status": "UP",
  "checks": []
}
```

### 5. Respuestas de error

No hay una respuesta de error propia implementada en `auth/` para este
endpoint, ni se observó una durante la ejecución local. No se encontró un
health check personalizado en el código del componente.

- **Pregunta abierta — posible estado `503`:** confirmar si la configuración
  del despliegue añade health checks o extensiones que puedan marcar el
  servicio como no saludable. El código actual no permite verificar qué
  condición produciría ese estado ni fijar un ejemplo JSON de error; por eso
  no se inventa uno.
- Una conexión fallida o una ruta incorrecta no es una respuesta JSON de este
  endpoint y no tiene un cuerpo de error definido en el código de `auth/`.

### 6. Notas de seguridad

La ruta es pública y revela el estado de disponibilidad del servicio. No se
debe interpretar `UP` como prueba de que un usuario esté autenticado ni como
prueba de que una operación de login vaya a completarse correctamente.

## `POST /auth/login`

### 1. Qué hace

Valida el username y la contraseña de una cuenta y, si coinciden, devuelve
los datos públicos de esa cuenta junto con un token de acceso. La ruta y la
respuesta están implementadas en
`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:16-64`.

### 2. Autenticación y headers

- **Autenticación previa requerida:** no; este endpoint inicia la sesión.
- **Header `Authorization`:** no se requiere.
- **Header requerido para enviar el JSON:** `Content-Type: application/json`.
- **CORS:** los orígenes se configuran mediante `AUTH_CORS_ORIGINS`; el valor
  predeterminado es `http://localhost:4200`. Los métodos y headers CORS
  permitidos se configuran en
  `auth/src/main/resources/application.properties:4-7`.

### 3. Request

El body se deserializa como `LoginRequest`; sus campos son públicos y de tipo
`String` (`auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-6`).
`username` y `password` no pueden ser nulos ni estar vacíos o compuestos solo
por espacios. `client` es opcional y el recurso actual no lo usa
(`AuthResource.java:32-44`).

| campo | tipo | obligatorio | descripción |
|---|---|---|---|
| `username` | string | sí | Username de la cuenta. Debe tener contenido distinto de espacios. |
| `password` | string | sí | Contraseña de la cuenta. Debe tener contenido distinto de espacios. |
| `client` | string | no | Campo aceptado por el DTO; no afecta el resultado de autenticación actual. |

Ejemplo JSON completo, con la cuenta ficticia de demostración:

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

El ejemplo de credenciales y el nombre mostrado corresponden a la prueba de
contrato `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:37-50`.

### 4. Response exitosa

- **Estado:** `200 OK`.
- **Duración comunicada:** `expiresIn` expresa segundos; el valor actual es
  `3600`.

| campo | tipo | descripción |
|---|---|---|
| `token` | string | JWT compacto firmado, emitido para la cuenta autenticada. El valor cambia en cada emisión. |
| `expiresIn` | integer | Duración del token en segundos; actualmente `3600`. |
| `user` | object | Datos públicos de la cuenta autenticada. |
| `user.username` | string | Username de la cuenta. |
| `user.displayName` | string | Nombre mostrado; el recurso escapa caracteres HTML. |
| `user.role` | string | Rol de la cuenta. |
| `debug` | string | Valor constante actual `authenticated-with-legacy-flow`; no usarlo para autorizar operaciones. |

Ejemplo JSON completo. El valor de `token` es un marcador de documentación:
en una respuesta real es una cadena JWT firmada y debe usarse el valor recibido,
no copiarse este marcador.

```json
{
  "token": "<JWT emitido por el servidor; valor dinámico>",
  "expiresIn": 3600,
  "user": {
    "username": "student",
    "displayName": "Workshop Student",
    "role": "STUDENT"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

La construcción de todos estos campos está en
`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:51-61`; los
valores de la cuenta de demostración y la forma verificada están en
`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:37-50`.

### 5. Respuestas de error

Los errores de aplicación devuelven un objeto JSON con el campo `message`.

| estado | cuándo ocurre | ejemplo de cuerpo |
|---|---|---|
| `400 Bad Request` | El body JSON es `null`. | `{"message":"Request is null"}` |
| `400 Bad Request` | Falta `username`, es `null`, vacío o solo contiene espacios. | `{"message":"Username is required"}` |
| `400 Bad Request` | Falta `password`, es `null`, vacío o solo contiene espacios. | `{"message":"Password is required"}` |
| `401 Unauthorized` | El username no existe o la contraseña no coincide. | `{"message":"Invalid user student or password"}` |
| `500 Internal Server Error` | La autenticación lanza `IllegalArgumentException` o `IllegalStateException`; por ejemplo, si falla la lectura de la cuenta o el hash guardado es inválido. | `{"message":"Authentication failed"}` |

Las respuestas y condiciones anteriores están en
`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:32-64`;
los cuerpos se comprueban en
`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:107-157,263-273`.

- **Pregunta abierta:** el código no define los cuerpos de error de la capa
  HTTP/Jackson para JSON malformado, tipos de contenido no admitidos u otras
  excepciones no capturadas por el recurso. No se les atribuye aquí un estado
  ni un JSON que no estén comprobados.
- Un preflight CORS con un origen no permitido se probó como
  `OPTIONS /auth/login` y obtuvo `403`, sin header
  `Access-Control-Allow-Origin`
  (`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:84-104`).
  No es una respuesta de `POST /auth/login`; la prueba no fija el cuerpo de
  ese preflight.

### 6. Notas de seguridad

- El token se firma como JWT compacto con HS256/HMAC-SHA256 y contiene los
  claims `sub`, `role` y `exp`. Su expiración se calcula en el momento de
  emitirlo; el endpoint establece una duración de `3600` segundos
  (`auth/src/main/java/com/bcp/workshop/auth/TokenSigner.java:19-21,45-68`;
  `AuthResource.java:21,51`).
- La clave se obtiene de `AUTH_TOKEN_SECRET` y el emisor requiere al menos
  32 bytes (`auth/src/main/resources/application.properties:9`;
  `auth/src/main/java/com/bcp/workshop/auth/TokenSigner.java:28-36`). No
  incluyas la clave en el cliente ni en el repositorio.
- En `auth/` se emite el token, pero no se implementa un endpoint de
  validación. El consumidor `api` verifica la firma HS256 con `AUTH_TOKEN_SECRET`,
  exige `sub`, `role` y `exp`, y rechaza el token si el secreto no está
  configurado (`api/app/main.py`, `verify_bearer_token`). `web` solo reenvía el
  token en `Authorization`. HALLAZGOS.md registra H-AUTH-03 como resuelto.
- La respuesta `401` incluye el username proporcionado en `message`; evita
  mostrar ese detalle directamente a usuarios finales si no es necesario.
- `debug` es un campo heredado presente en la respuesta observada. No representa
  un claim ni una comprobación de autorización.

## Documentos consultados

- `README.md`
- `RETO.md`
- `docs/GOOD_PRACTICES.md`
- `HALLAZGOS.md`
- `PLAN.md`
- Código y pruebas consultados dentro de `auth/`: `AuthResource`,
  `LoginRequest`, `TokenSigner`, `AuthService`, `LegacyDatabase`,
  `application.properties`, `pom.xml` y `AuthResourceTest`.
