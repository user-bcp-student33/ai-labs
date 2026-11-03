# API FastAPI

Contrato observado en la implementacion actual de `api/`. No hay un contrato previamente acordado en `docs/ENDPOINTS.md`; las preguntas abiertas quedan marcadas al final. Los ejemplos de respuestas son representativos y sus timestamps, IDs y rutas de archivo dependen de la base de datos y la configuracion local.

## Autenticacion comun

Las cinco rutas requieren el header exacto `Authorization: Bearer <token>`. Para reproducir los ejemplos en el laboratorio, este comando PowerShell genera un token que sigue el formato legacy que acepta el codigo:

```powershell
$token = python -c "import base64,time; print(base64.b64encode(('student|STUDENT|' + str(int(time.time()) + 3600) + '|WORKSHOP_SECRET_2026').encode()).decode())"
```

El token es Base64 de `username|role|expiry|secret`, con expiracion Unix en segundos. El secreto esta embebido en el codigo; este mecanismo es inseguro y sirve solo para reproducir el comportamiento local, no para produccion. Un header ausente o un token que no pase la validacion devuelve `401` y `{"detail":"Invalid token"}`.

## GET /api/tasks

### 1. Que hace

Lista las tareas del propietario seleccionado. Si no se envia `owner` o se envia vacio, usa el `username` del token.

### 2. Autenticacion

Requerida: `Authorization: Bearer <token>`.

### 3. Request

| campo/parametro | tipo | obligatorio | descripcion |
|---|---|---|---|
| `Authorization` | header string | si | Token legacy descrito arriba. |
| `owner` | query string | no | Propietario por el que filtrar. El valor enviado se usa directamente; si falta o esta vacio se usa el `username` del token. |

Ejemplo completo:

```powershell
curl.exe -H "Authorization: Bearer $token" "http://localhost:8000/api/tasks"
```

### 4. Response exitosa

Codigo: `200 OK`.

| campo | tipo | descripcion |
|---|---|---|
| `id` | integer | Identificador SQLite de la tarea. |
| `title` | string | Titulo. |
| `description` | string | Descripcion. |
| `status` | string | Estado de la tarea. |
| `owner` | string | Propietario guardado. |
| `priority` | string | Prioridad. |
| `createdAt` | string | Fecha/hora guardada por SQLite como texto. |

Ejemplo completo:

```json
[
  {
    "id": 1,
    "title": "Refactor login",
    "description": "Separar responsabilidades en frontend",
    "status": "TODO",
    "owner": "student",
    "priority": "HIGH",
    "createdAt": "2026-11-02 22:22:48"
  },
  {
    "id": 2,
    "title": "Review API",
    "description": "Revisar seguridad de endpoints",
    "status": "IN_PROGRESS",
    "owner": "student",
    "priority": "MEDIUM",
    "createdAt": "2026-11-02 22:22:48"
  }
]
```

### 5. Respuestas de error

| codigo | cuando ocurre | cuerpo de ejemplo |
|---|---|---|
| `401 Unauthorized` | Header ausente o token rechazado. | `{"detail":"Invalid token"}` |
| `500 Internal Server Error` | Error interno no manejado, por ejemplo una falla SQLite. Starlette devuelve texto plano. | `Internal Server Error` |

### 6. Notas de seguridad

El filtro `owner` no se compara con la identidad autenticada: permite consultar otro propietario. No enviar datos sensibles suponiendo aislamiento por usuario. El token Base64 tampoco tiene firma criptografica, emisor ni audiencia verificados. Evidencia: `api/app/routes.py:25`, `api/app/services.py:28`, `api/app/main.py:22`.

## POST /api/tasks

### 1. Que hace

Crea una tarea y devuelve el registro creado.

### 2. Autenticacion

Requerida: `Authorization: Bearer <token>`.

### 3. Request

Header `Content-Type: application/json`.

| campo/parametro | tipo | obligatorio | descripcion |
|---|---|---|---|
| `Authorization` | header string | si | Token legacy descrito en Autenticacion comun. |
| body `title` | string | no | Titulo; si se omite usa `""`. Los valores recibidos se convierten a texto. |
| body `description` | string | no | Descripcion; si se omite usa `""`. |
| body `status` | string | no | Estado; si se omite usa `"TODO"`. |
| body `priority` | string | no | Prioridad; si se omite usa `"MEDIUM"`. |
| body `owner` | string o null | no | Si se omite, usa el `username` del token. Si se envia, el valor se convierte a texto y se guarda como propietario. Enviar `null` se convierte actualmente en `"None"`. |
| otras propiedades JSON | cualquier valor JSON | no | Se aceptan propiedades adicionales y no se usan al crear la tarea. |

Aunque todos los campos del modelo tienen defaults, el body JSON como objeto es requerido por la ruta.

Ejemplo completo:

```powershell
curl.exe -X POST "http://localhost:8000/api/tasks" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  --data-raw '{"title":"Add tests","description":"Improve coverage"}'
```

### 4. Response exitosa

Codigo: `201 Created`.

| campo | tipo | descripcion |
|---|---|---|
| `id` | integer | Identificador SQLite de la tarea. |
| `title` | string | Titulo guardado. |
| `description` | string | Descripcion guardada. |
| `status` | string | Estado guardado. |
| `owner` | string | Propietario guardado. |
| `priority` | string | Prioridad guardada. |
| `createdAt` | string | Fecha/hora guardada por SQLite como texto. |

Ejemplo completo:

```json
{
  "id": 5,
  "title": "Add tests",
  "description": "Improve coverage",
  "status": "TODO",
  "owner": "student",
  "priority": "MEDIUM",
  "createdAt": "2026-11-02 22:22:48"
}
```

### 5. Respuestas de error

| codigo | cuando ocurre | cuerpo de ejemplo |
|---|---|---|
| `401 Unauthorized` | Header ausente o token rechazado. | `{"detail":"Invalid token"}` |
| `422 Unprocessable Entity` | Body ausente, JSON malformado o no es un objeto compatible. Ejemplo observado con body ausente: | `{"detail":[{"type":"missing","loc":["body"],"msg":"Field required","input":null}]}` |
| `500 Internal Server Error` | Error interno no manejado durante persistencia. Starlette devuelve texto plano. | `Internal Server Error` |

### 6. Notas de seguridad

`owner` se toma del body sin ligarlo al `username` del token. Un cliente puede crear una tarea para otro propietario. La ruta conserva esta conducta actual; no es una autorizacion segura. El body tambien permite propiedades extras que se ignoran. Evidencia: `api/app/routes.py:32`, `api/app/services.py:32`, `api/app/services.py:33`, `api/app/schemas.py:11`.

## GET /api/summary

### 1. Que hace

Calcula el total de tareas, el conteo por estado y las tareas abiertas de prioridad alta para un propietario.

### 2. Autenticacion

Requerida: `Authorization: Bearer <token>`.

### 3. Request

| campo/parametro | tipo | obligatorio | descripcion |
|---|---|---|---|
| `Authorization` | header string | si | Token legacy descrito en Autenticacion comun. |
| `owner` | query string | no | Propietario por el que resumir. Si falta o esta vacio se usa el `username` del token. |

Ejemplo completo:

```powershell
curl.exe -H "Authorization: Bearer $token" "http://localhost:8000/api/summary"
```

### 4. Response exitosa

Codigo: `200 OK`.

| campo | tipo | descripcion |
|---|---|---|
| `owner` | string | Propietario consultado. |
| `total` | integer | Numero total de tareas del propietario. |
| `byStatus` | object con claves string y valores integer | Numero de tareas agrupadas por estado. |
| `highPriorityOpen` | integer | Tareas con prioridad `HIGH` cuyo estado no es `DONE` ni `ARCHIVED`. |

Ejemplo completo:

```json
{
  "owner": "student",
  "total": 2,
  "byStatus": {
    "TODO": 1,
    "IN_PROGRESS": 1
  },
  "highPriorityOpen": 1
}
```

### 5. Respuestas de error

| codigo | cuando ocurre | cuerpo de ejemplo |
|---|---|---|
| `401 Unauthorized` | Header ausente o token rechazado. | `{"detail":"Invalid token"}` |
| `500 Internal Server Error` | Error interno no manejado durante la consulta SQLite. Starlette devuelve texto plano. | `Internal Server Error` |

### 6. Notas de seguridad

El query `owner` se utiliza directamente y puede seleccionar tareas de otro propietario. No se aplica una regla de acceso por identidad o rol. Evidencia: `api/app/routes.py:39`, `api/app/services.py:43`, `api/app/services.py:44`.

## POST /api/requirements

### 1. Que hace

Guarda el archivo de requisitos recibido en el directorio configurado para uploads.

### 2. Autenticacion

Requerida: `Authorization: Bearer <token>`.

### 3. Request

Tipo de contenido: `multipart/form-data`.

| campo/parametro | tipo | obligatorio | descripcion |
|---|---|---|---|
| `Authorization` | header string | si | Token legacy descrito en Autenticacion comun. |
| `file` | archivo multipart | si | Archivo cargado; el nombre enviado se usa como nombre de archivo. |

Ejemplo completo:

```powershell
curl.exe -X POST "http://localhost:8000/api/requirements" `
  -H "Authorization: Bearer $token" `
  -F "file=@requirements.txt;type=text/plain"
```

### 4. Response exitosa

Codigo: `201 Created`.

| campo | tipo | descripcion |
|---|---|---|
| `owner` | string | `username` del token. |
| `fileName` | string | Nombre recibido en la parte multipart. |
| `storedAt` | string | Ruta del archivo guardado, como texto; depende de `WORKSHOP_UPLOAD_DIR` y del directorio de ejecucion. |
| `size` | integer | Numero de bytes escritos. |

Ejemplo completo (la ruta concreta depende de la configuracion local):

```json
{
  "owner": "student",
  "fileName": "requirements.txt",
  "storedAt": "C:\\Users\\Usuario\\AppData\\Local\\Temp\\tmpv6sb8858\\uploads\\requirements.txt",
  "size": 24
}
```

### 5. Respuestas de error

| codigo | cuando ocurre | cuerpo de ejemplo |
|---|---|---|
| `401 Unauthorized` | Header ausente o token rechazado. | `{"detail":"Invalid token"}` |
| `422 Unprocessable Entity` | Falta la parte obligatoria `file` o el multipart no se puede validar. Ejemplo observado sin archivo: | `{"detail":[{"type":"missing","loc":["body","file"],"msg":"Field required","input":null}]}` |
| `500 Internal Server Error` | Error no manejado al leer o escribir el archivo. Starlette devuelve texto plano. | `Internal Server Error` |

### 6. Notas de seguridad

El nombre se concatena al directorio sin normalizarse y se lee el archivo completo en memoria. No hay validacion de extension ni limite de tamaño. No cargar archivos no confiables ni grandes. Evidencia: `api/app/routes.py:46`, `api/app/services.py:70`, `api/app/services.py:71`, `api/app/services.py:72`.

## GET /api/backend-health

### 1. Que hace

Consulta la ruta de salud del servicio de autenticacion configurada para la API y devuelve su estado y cuerpo.

### 2. Autenticacion

Requerida: `Authorization: Bearer <token>`.

### 3. Request

| campo/parametro | tipo | obligatorio | descripcion |
|---|---|---|---|
| `Authorization` | header string | si | Token legacy descrito en Autenticacion comun. |
| `AUTH_HEALTH_URL` | variable de entorno del servidor | no | URL consultada; default `http://localhost:8080/q/health`. No es un parametro enviado por el cliente. |

Ejemplo completo:

```powershell
curl.exe -H "Authorization: Bearer $token" "http://localhost:8000/api/backend-health"
```

### 4. Response exitosa

Codigo: `200 OK`, incluso cuando la solicitud HTTP al servicio destino falla con una excepcion de urllib3 manejada por la API.

| campo | tipo | descripcion |
|---|---|---|
| `requestedBy` | string | `username` del token. |
| `status` | integer | Codigo HTTP del servicio destino, o `0` si urllib3 produce un `HTTPError`. |
| `body` | string | Cuerpo recibido del servicio destino, o `"Health check unavailable"` cuando se captura `HTTPError`. |

Ejemplo completo de respuesta del servicio destino:

```json
{
  "requestedBy": "student",
  "status": 200,
  "body": "{\"status\":\"UP\"}"
}
```

Ejemplo completo cuando se captura un error de transporte:

```json
{
  "requestedBy": "student",
  "status": 0,
  "body": "Health check unavailable"
}
```

### 5. Respuestas de error

| codigo | cuando ocurre | cuerpo de ejemplo |
|---|---|---|
| `401 Unauthorized` | Header ausente o token rechazado. | `{"detail":"Invalid token"}` |
| `500 Internal Server Error` | Excepcion distinta de `urllib3.exceptions.HTTPError` no manejada por el servicio. Starlette devuelve texto plano. | `Internal Server Error` |

### 6. Notas de seguridad

La URL de destino se controla con configuracion del servidor; la respuesta incluye el body obtenido y debe tratarse como dato externo. El manejo de `HTTPError` oculta el texto de excepcion en la respuesta, pero lo registra en el log. La autenticacion sigue usando el token legacy descrito arriba. Evidencia: `api/app/main.py:48`, `api/app/services.py:86`, `api/app/services.py:95`.

## Preguntas abiertas

- ¿Que claims, algoritmo, emisor, audiencia y mecanismo de claves acordara el emisor antes de reemplazar el token Base64 actual?
- ¿Debe restringirse `owner` a la identidad del token o hay roles administrativos que habiliten acceso entre propietarios?
- ¿Que extensiones y tamaño máximo deben permitirse en `/api/requirements`?
- ¿Debe `GET /api/backend-health` conservar `200` con `status: 0` ante fallos de transporte, o se acordara otro codigo HTTP?
