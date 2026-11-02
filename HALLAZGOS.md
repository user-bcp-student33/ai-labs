# Hallazgos

## API

### Contrato observado (derivado del codigo, no acordado)

- Todas las rutas observadas requieren `Authorization: Bearer ...`; el token que acepta el codigo es Base64 de cuatro campos separados por `|` (`username|role|expiry|secret`). Evidencia: `api/app/main.py:17`, `api/app/main.py:22`.
- `GET /api/tasks` admite el query opcional `owner` y devuelve tareas con `id`, `title`, `description`, `status`, `owner`, `priority` y `createdAt`. Evidencia: `api/app/main.py:56`, `api/app/main.py:67`, `api/app/main.py:68`.
- `POST /api/tasks` recibe un objeto JSON y responde `201` con una tarea. Evidencia: `api/app/main.py:77`, `api/app/main.py:78`, `api/app/main.py:88`.
- `GET /api/summary` admite el query opcional `owner` y devuelve `owner`, `total`, `byStatus` y `highPriorityOpen`. Evidencia: `api/app/main.py:90`, `api/app/main.py:91`, `api/app/main.py:94`.
- `POST /api/requirements` recibe un archivo multipart y responde `201` con `owner`, `fileName`, `storedAt` y `size`. Evidencia: `api/app/main.py:107`, `api/app/main.py:108`, `api/app/main.py:112`.
- `GET /api/backend-health` devuelve `requestedBy`, `status` y `body`. Evidencia: `api/app/main.py:114`, `api/app/main.py:115`, `api/app/main.py:119`, `api/app/main.py:121`.

Este resumen describe el comportamiento del codigo; no sustituye un contrato acordado. `docs/ENDPOINTS.md` no existe y queda pendiente definirlo en el paso correspondiente.

### Matriz de seguimiento

| id | componente | hallazgo | tipo | explicacion | evidencia (archivo:linea) | severidad | estado |
|---|---|---|---|---|---|---|---|
| H-API-01 | api | Token Base64 con secreto embebido y sin firma | Vulnerabilidad | El servidor solo decodifica el contenido y compara un secreto fijo; no verifica una firma criptografica, emisor ni audiencia. Un cliente puede construir credenciales que el servidor acepte y la proteccion de integridad del token no existe. | `api/app/main.py:14`, `api/app/main.py:17`, `api/app/main.py:22` | alta | Pendiente |
| H-API-02 | api | Autorizacion de tareas controlada por `owner` del cliente | Vulnerabilidad | `GET /api/tasks` y `GET /api/summary` permiten sustituir la identidad del token con el query `owner`; `POST /api/tasks` tambien acepta un `owner` arbitrario. Esto permite consultar o crear datos asociados a otra identidad. | `api/app/main.py:68`, `api/app/main.py:69`, `api/app/main.py:78`, `api/app/main.py:83`, `api/app/main.py:91`, `api/app/main.py:92` | alta | Pendiente |
| H-API-03 | api | Consultas SQLite construidas por concatenacion | Vulnerabilidad | Los valores de `owner` se concatenan en los filtros y los campos recibidos se concatenan en el `INSERT`. La consulta puede cambiar de significado ante entradas con caracteres SQL y los inserts pueden fallar con valores ordinarios que contengan comillas. | `api/app/main.py:70`, `api/app/main.py:84`, `api/app/main.py:93` | alta | Pendiente |
| H-API-04 | api | Carga de archivos usa directamente `file.filename` | Vulnerabilidad | El nombre recibido se combina con el directorio de uploads sin normalizarlo ni validar extension o tamano. Un nombre no confiable puede escapar el destino previsto y se aceptan archivos sin limite de tamano o tipo visible en esta ruta. | `api/app/main.py:108`, `api/app/main.py:109`, `api/app/main.py:110` | alta | Pendiente |
| H-API-05 | api | Manejo de errores amplio y contratos poco tipados | Vulnerabilidad | Se capturan excepciones con `except Exception` y se devuelve `str(error)` en respuestas de autenticacion y health; esto puede revelar detalles internos. Ademas, el uso de `dict[str, Any]` y condicionales anidados dificulta validar entradas y mantener reglas coherentes. | `api/app/main.py:25`, `api/app/main.py:78`, `api/app/main.py:94`, `api/app/main.py:101`, `api/app/main.py:120`, `api/app/main.py:121` | media | Pendiente |
| H-API-06 | api | Dependencias fijadas requieren verificacion SCA | Sugerencia | El manifiesto fija `urllib3` y `python-multipart` en las versiones enumeradas por el reto. El manifiesto confirma esas versiones, pero no demuestra por si solo un CVE aplicable; la revision de advisories y la decision de actualizar o remover siguen pendientes. | `api/requirements.txt:4`, `api/requirements.txt:5` | baja | Pendiente |
| H-API-07 | api | Rutas, persistencia, reglas y serializacion concentradas en un modulo | Refactor | `create_app` inicializa SQLite y las rutas ejecutan consultas, aplican reglas y forman respuestas en `api/app/main.py`. Esta concentracion acopla HTTP y persistencia y eleva el costo y riesgo de cambiar o probar cada responsabilidad por separado. | `api/app/main.py:29`, `api/app/main.py:37`, `api/app/main.py:56`, `api/app/main.py:67`, `api/app/main.py:77`, `api/app/main.py:90` | baja | Pendiente |