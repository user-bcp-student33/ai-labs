# Pruebas de API

Componente: `api`. Contratos tomados de la seccion API de `docs/ENDPOINTS.md`. Cada caso de esta tabla tiene su codigo en `api/tests/test_api.py`; el nombre del test permite localizarlo sin duplicar la implementacion dentro de esta guia. El token de prueba es el formato legacy del propio componente, generado con datos ficticios. El backend de health se sustituye por stubs y no se contacta un servicio externo.

## Matriz de casos

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `GET /api/tasks` | `test_lists_seeded_tasks` | Feliz | `200`, dos tareas sembradas y claves documentadas. PASO. |
| `GET /api/tasks` | `test_list_tasks_with_empty_owner_uses_token_identity` | Borde | `200`, query `owner` vacio cae al `username` del token. PASO. |
| `GET /api/tasks` | `test_list_tasks_unknown_owner_returns_empty_list` | Borde | `200` y lista vacia. PASO. |
| `GET /api/tasks` | `test_list_tasks_rejects_cross_owner_access` | Seguridad / H-API-02 | No devolver tareas ajenas. FALLO: responde `200` e incluye tarea de `trainer`. |
| `GET /api/tasks` | `test_list_tasks_requires_authentication`, `test_rejects_invalid_token` | Error | `401` y `{"detail":"Invalid token"}` para header ausente/token invalido. PASO. |
| `GET /api/tasks` | `test_list_tasks_reports_internal_failure` | Error | `500` y texto `Internal Server Error` ante fallo SQLite no manejado. PASO. |
| `POST /api/tasks` | `test_creates_task_and_summary` | Feliz | `201`, tarea creada, propietario por defecto y resumen actualizado. PASO. |
| `POST /api/tasks` | `test_create_task_preserves_apostrophes` | Borde / H-API-03 | `201`; titulo y descripcion con apostrofes se conservan. PASO. |
| `POST /api/tasks` | `test_create_task_empty_payload_uses_defaults` | Borde | `{}` produce `201` con defaults observados. PASO. |
| `POST /api/tasks` | `test_create_task_rejects_owner_override` | Seguridad / H-API-02 | La propiedad efectiva debe ser el usuario del token. FALLO: responde `201` con `owner: trainer`. |
| `POST /api/tasks` | `test_create_task_without_body_returns_validation_error` | Error | `422` con ubicacion `body`. PASO. |
| `POST /api/tasks` | `test_create_task_malformed_json_returns_validation_error` | Error | `422` con tipo `json_invalid`. PASO. |
| `POST /api/tasks` | `test_create_task_requires_authentication` | Error | `401` y detalle generico. PASO. |
| `POST /api/tasks` | `test_create_task_reports_internal_failure` | Error | `500` y texto `Internal Server Error` ante fallo SQLite. PASO. |
| `GET /api/summary` | `test_summary_counts_statuses_and_high_priority_tasks` | Feliz / H-API-05 | `200`, conteos exactos por estado y tareas abiertas de prioridad alta. PASO. |
| `GET /api/summary` | `test_summary_unknown_owner_returns_zero_counts` | Borde / H-API-05 | `200`, cero total, mapa vacio y cero tareas abiertas. PASO. |
| `GET /api/summary` | `test_summary_counts_repeated_states_and_excludes_closed_high_priority` | Borde / H-API-05 | Cuenta estados repetidos; no incluye `DONE` ni `ARCHIVED` en `highPriorityOpen`. PASO. |
| `GET /api/summary` | `test_summary_rejects_cross_owner_access` | Seguridad / H-API-02 | El resumen debe quedar limitado al propietario del token. FALLO: devuelve `owner: trainer` y sus datos. |
| `GET /api/summary` | `test_summary_requires_authentication` | Error | `401` y detalle generico. PASO. |
| `GET /api/summary` | `test_summary_reports_internal_failure` | Error | `500` y texto `Internal Server Error` ante fallo SQLite. PASO. |
| `POST /api/requirements` | `test_uploads_requirement` | Feliz | `201`, campos documentados y bytes guardados. PASO. |
| `POST /api/requirements` | `test_uploads_zero_byte_file` | Borde | Archivo vacio aceptado con `size: 0`. PASO; el contrato no fija un minimo. |
| `POST /api/requirements` | `test_uploads_file_without_extension` | Borde | `201`; se conserva el nombre sin extension. PASO (`1 passed` focal). |
| `POST /api/requirements` | `test_upload_keeps_traversal_filename_inside_upload_directory` | Seguridad / H-API-04 | El destino resuelto debe permanecer bajo uploads. FALLO: `..\\escape.txt` se escribe en el directorio padre. |
| `POST /api/requirements` | `test_upload_requires_file_part` | Error | `422` con ubicacion `body/file`. PASO. |
| `POST /api/requirements` | `test_upload_requires_authentication` | Error | `401` y detalle generico. PASO. |
| `POST /api/requirements` | `test_upload_reports_internal_write_failure` | Error | `500` y texto `Internal Server Error` al fallar la escritura. PASO. |
| `GET /api/backend-health` | `test_backend_health_preserves_success_contract` | Feliz | `200`, `requestedBy`, codigo y cuerpo del servicio simulado. PASO. |
| `GET /api/backend-health` | `test_backend_health_hides_internal_error` | Borde/error de transporte | `200`, `status: 0` y cuerpo generico cuando urllib3 lanza `HTTPError`. PASO. |
| `GET /api/backend-health` | `test_backend_health_replaces_invalid_utf8` | Borde | `200`; bytes UTF-8 invalidos se reemplazan por `�`. PASO. |
| `GET /api/backend-health` | `test_backend_health_requires_authentication` | Error | `401` y detalle generico. PASO. |
| `GET /api/backend-health` | `test_backend_health_reports_unexpected_internal_failure` | Error | `500` y texto `Internal Server Error` para excepcion no manejada. PASO. |

## Codigo de las pruebas

El codigo ejecutable completo de la matriz esta en [../api/tests/test_api.py](../api/tests/test_api.py). Estos casos muestran las aserciones de seguridad que deben permanecer rojas hasta que se corrija el comportamiento:

```python
def test_list_tasks_rejects_cross_owner_access(api_client):
	response = api_client.get("/api/tasks?owner=trainer", headers=headers())
	assert response.status_code != 200 or all(
		task["owner"] == "student" for task in response.json()
	)


def test_summary_rejects_cross_owner_access(api_client):
	response = api_client.get("/api/summary?owner=trainer", headers=headers())
	assert response.status_code != 200 or response.json()["owner"] == "student"


def test_create_task_rejects_owner_override(api_client):
	response = api_client.post(
		"/api/tasks",
		headers=headers(),
		json={"title": "Other owner", "owner": "trainer"},
	)
	assert response.status_code != 201 or response.json()["owner"] == "student"


def test_upload_keeps_traversal_filename_inside_upload_directory(api_client):
	upload_dir = Path(api_client.app.state.upload_dir).resolve()
	response = api_client.post(
		"/api/requirements",
		headers=headers(),
		files={"file": ("..\\escape.txt", b"outside", "text/plain")},
	)
	escaped_path = upload_dir.parent / "escape.txt"
	assert not escaped_path.exists()
	if response.status_code == 201:
		stored_path = Path(response.json()["storedAt"]).resolve()
		assert stored_path.parent == upload_dir
```

Los tests de todos los caminos felices, casos borde y errores documentados también están en ese archivo. Los casos de autorización y traversal mantienen las aserciones de seguridad; no se cambiaron para aceptar el comportamiento vulnerable.

## Ejecuciones

Caracterización focal de resumen, directorio de trabajo `api/`:

```text
.venv/Scripts/python.exe -m pytest -q tests/test_api.py::test_summary_counts_repeated_states_and_excludes_closed_high_priority
Exit code: 0
Resultado: 1 passed
Componente: api; ID: H-API-05.
```

Suite de endpoints, directorio de trabajo `api/`:

```text
.venv/Scripts/python.exe -m pytest -q tests/test_api.py
Exit code: 1
Resultado: 30 passed, 4 failed
Componente: api; IDs ejercitados: H-API-01 (solo autenticacion legacy), H-API-02, H-API-03, H-API-04, H-API-05 y H-API-07.
```

Suite con cobertura, directorio de trabajo `api/`:

```text
.venv/Scripts/python.exe -m pytest --cov=app --cov-report=term-missing
Exit code: 1
Resultado: 30 passed, 4 failed
Componente: api; IDs ejercitados: H-API-01 (solo autenticacion legacy), H-API-02, H-API-03, H-API-04, H-API-05 y H-API-07.
Cobertura global app: 179/179 statements, 8/8 branches, 100%.
Metodo: pytest-cov, configuracion pyproject.toml `[tool.coverage.run] source = ["app"]`, informe de terminal.
Cobertura diferencial del diff no medida; no se modifico codigo de produccion en este paso. Cobertura global informada por separado.
```

## Fallos

- H-API-02: `GET /api/tasks?owner=trainer`, `GET /api/summary?owner=trainer` y el body `owner: trainer` de `POST /api/tasks` permiten leer o crear datos para otro propietario. Evidencia: `api/app/services.py:29`, `api/app/services.py:33`, `api/app/services.py:44`; tests `test_list_tasks_rejects_cross_owner_access`, `test_summary_rejects_cross_owner_access` y `test_create_task_rejects_owner_override`.
- H-API-04: `POST /api/requirements` acepta el nombre `..\\escape.txt` y resuelve la escritura fuera del directorio de uploads. Evidencia: `api/app/services.py:70`, `api/app/services.py:71`, `api/app/services.py:72`; test `test_upload_keeps_traversal_filename_inside_upload_directory`.

## Pendientes

- H-API-01: las pruebas usan el token Base64 legacy observado; no prueban JWT firmado, emisor, audiencia ni claims porque esa validacion aún no existe y el contrato del emisor sigue abierto.
- H-API-04: no se fijó tamaño maximo ni allowlist de extensiones. El caso de borde de cero bytes está probado; falta acordar política antes de afirmar límites. No se asignó un umbral arbitrario a una prueba de tamaño.
- H-API-06: esta ejecución no es un análisis SCA y no permite concluir si las versiones fijadas tienen advisories aplicables.
- Se emitieron avisos de deprecación de FastAPI por `on_event`; no afectan el resultado funcional de estos casos.

## Pruebas propuestas

- Tras implementar H-API-02, repetir los tres tests de propietario y exigir que no se lea ni cree información ajena, manteniendo el status acordado en el contrato.
- Tras implementar H-API-04, repetir el test de confinamiento; cuando se acuerden extensión y tamaño, añadir casos sin extensión y justo debajo/en/encima del máximo.
- Tras implementar H-API-01, añadir casos JWT firmado valido y de firma, issuer, audience, expiracion y claims invalidas.
