# Evidencia DoD — componente `api` — H-AUTH-03

Fecha de evaluación: 2026-10-29.
Componente: `api/`. Hallazgo: H-AUTH-03 (Issue [#25](https://github.com/user-bcp-student33/ai-labs/issues/25)).
PR evaluado: [#31](https://github.com/user-bcp-student33/ai-labs/pull/31), head `day2/skill-mcp@c6c17ae`, estado abierto.
Criterios: skill `reqflow-definition-of-done`.
Cambio OpenSpec: `openspec/changes/api-validates-auth-tokens/`.

Estados usados: `CUMPLIDO`, `PENDIENTE DE OTRO GATE`, `NO APLICA AL ALCANCE`, `FALTA EVIDENCIA`, `BLOQUEADO`.

## 1. Matriz de criterios

| criterio | estado | evidencia | observación |
|---|---|---|---|
| Contrato `POST /auth/login` preservado | CUMPLIDO | PR #31 modifica solo `api/tests/test_api.py` y `openspec/`. Check `auth` en verde sobre `c6c17ae`. Login real ejecutado en la E2E de esta sección (sección 2). | Ningún test del PR prueba el login directamente. |
| Contratos de `api/` (`GET/POST /api/tasks`, `GET /api/summary`, `POST /api/requirements`) | CUMPLIDO | Sin cambios en `api/app/main.py` según archivos del PR. Job `api` en verde. | Evidencia por ausencia de cambios y pruebas existentes en verde. |
| CI en verde | CUMPLIDO | 8/8 check runs `success` en `c6c17ae`: CodeQL, analyze (python, java-kotlin, javascript-typescript), web, auth, api, dependencies. `mergeable_state: clean`. | No se encontró bypass en los workflows revisados. |
| Pruebas ejecutadas | CUMPLIDO | `python tools/lab.py test api` ejecutado el 2026-10-29: 30 passed, 0 failed (sección 2). Job `api` en verde. | `64 warnings` (deprecaciones de FastAPI y `InsecureKeyLengthWarning` del caso G3). |
| Cobertura crítica ≥ 85 % | CUMPLIDO | [ci.yml](./.github/workflows/ci.yml) línea 35 `--cov-fail-under=85`. Ejecución local: total 92.98 %, `app/main.py` 93 %. | El PR no modifica código de aplicación. |
| Dependencias sin High/Critical sin decisión | PENDIENTE DE OTRO GATE | `pip-audit -r api/requirements.txt` (sección 2): `PyJWT==2.15.1` sin vulnerabilidades. `urllib3==2.5.0`, `python-multipart==0.0.21` y `starlette==0.50.0` con 32 vulnerabilidades. | Pip-audit no reporta severidad; no se puede confirmar High/Critical. Requiere decisión de actualización (manifiesto) en PR aparte. Ramas Dependabot existentes para `urllib3` y `python-multipart`. |
| SQL parametrizado | CUMPLIDO | [design.md](./openspec/changes/api-validates-auth-tokens/design.md): placeholders `?`. Tests de `owner` ignorado. | Basado en el diseño y en pruebas. El código fuente de `main.py` no se revisó en esta evidencia. |
| Auth/authz del lado servidor | CUMPLIDO | Spec (requisitos 1 y 2). Tests de firma ajena, `alg: none`, expirado, claims faltantes y sin secreto. E2E: token alterado → `401`, sin token → `401`. | Brechas G1 (acepta sin `Bearer`) y G4 (acepta `sub` vacío) abiertas; decisiones 3.1 pendientes. |
| Secretos fuera del código | PENDIENTE DE OTRO GATE | [compose.yaml](./compose.yaml) líneas 6 y 12 contienen el default `dev-only-change-me-workshop-token-secret` (G2). | Corregirlo toca `auth/` y requiere decisión humana (tareas 3.2, 4.5). Los tests usan secretos de prueba aislados. |
| Archivos controlados | NO APLICA AL ALCANCE | [design.md](./openspec/changes/api-validates-auth-tokens/design.md), "Non-goals": la validación de nombre de archivo en `POST /api/requirements` es otro hallazgo. | — |
| Guardrail: sin datos ni secretos reales | CUMPLIDO | Fixture con `TEST_SECRET` aislado (tarea 2.1). En la E2E, el secreto fue aleatorio y solo estuvo en el entorno de los procesos. | — |
| Guardrail: sin desactivar gates | CUMPLIDO | Archivos del PR: ningún workflow ni configuración de gates modificado. | — |
| PR auditable | CUMPLIDO | Cuerpo del PR #31: comandos, resultados, trade-offs, riesgos residuales, rollback y fuera de alcance. | El PR no refleja aún los resultados de esta sección ni marca 6.1. |
| Issue #25, criterio 1: inventario de consumidores | CUMPLIDO | [design.md](./openspec/changes/api-validates-auth-tokens/design.md), "Inventario de consumidores": `auth` emisor, `api` verificador, `web` transportador. | Consumidores externos no confirmados. Evidencia general en raíz: este documento. |
| Issue #25, criterios 2–4 para `api/` | CUMPLIDO | Tests de token válido, firma inválida y contenido alterado en `api/tests/test_api.py`. Job `api` en verde. | — |
| Issue #25, criterios 2–4 para `web/` | FALTA EVIDENCIA | `web/` aparece como transportador (`dashboard.component.ts` línea 44) sin pruebas de aceptación ni rechazo. | Falta decisión: ¿el Issue exige pruebas en `web/`? |
| Issue #25, criterio 5: comandos y conteos | CUMPLIDO | Sección 2: comando y resultado registrados. | — |
| Issue #25, criterio 6: login y token sin cambios | CUMPLIDO | Archivos del PR; spec, "Preserved Behavior". | — |
| E2E `auth` + `api` (tareas 5.1 y 5.2) | CUMPLIDO | Sección 2: login → `200` en `GET /api/tasks`; token alterado → `401`. | Ejecución local sin Docker: `compose.yaml` no se ejecutó. |
| Lint (tarea 6.2) | PENDIENTE DE OTRO GATE | Tarea 3.4 sin decisión. | Sin aprobación de `pyflakes`, no se aplica. |
| Revisión humana y cierre (tareas 0.1, 0.2, 3.1–3.3, 7.1, 7.2) | PENDIENTE DE OTRO GATE | [tasks.md](./openspec/changes/api-validates-auth-tokens/tasks.md); PR #31 "Revisión humana: Pendiente". | Estado de H-AUTH-03 contradictorio: el Issue y [HU-01.md](./HU-01.md) dicen `Pendiente`; [HALLAZGOS.md](./HALLAZGOS.md) dice `Resuelto`. |
| Evidencia general en raíz (tarea 6.5) | PENDIENTE DE OTRO GATE | Este documento. | Su ubicación y el ID `H-AUTH-03` requieren aprobación (tarea 0.1). |

**Veredicto:** el DoD no se cumple completo. Quedan pendientes el gate de dependencias y las decisiones humanas.

## 2. Ejecuciones de esta sección (2026-10-29)

### 2.1 Suite de `api/`

Comando: `python tools/lab.py test api` (desde la raíz; ejecuta `pytest --cov=app --cov-report=term-missing --cov-report=html --cov-fail-under=85` en `api/`).

Resultado: `30 passed, 64 warnings`. Cobertura: `app\main.py` 93 %, total 92.98 %. `Required test coverage of 85% reached`.

Efecto secundario: se regeneró `api/htmlcov/` (no versionado).

### 2.2 Prueba E2E local (`auth` + `api`)

Procedimiento:
1. Reconstrucción del jar: `mvn -q -f auth/pom.xml package -DskipTests -B` → exit 0.
2. Arranque de `auth` (`java -jar auth/target/quarkus-app/quarkus-run.jar`, puerto 8080) y `api` (`python -m uvicorn app.main:app --port 8000`, desde `api/`). Ambos con `AUTH_TOKEN_SECRET` aleatorio de 48 caracteres, solo en el entorno del proceso.
3. `api` usó una copia temporal de la base de datos (`WORKSHOP_DB_PATH`).

Resultados:

| Caso | Solicitud | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| Salud de `auth` | `GET http://localhost:8080/q/health` | 200 | 200 |
| Login | `POST /auth/login` con `student` (credenciales del laboratorio de `docs/ENDPOINTS.md`) | 200 con token | 200, `expiresIn=3600`, `role=STUDENT` |
| Positivo | `GET http://localhost:8000/api/tasks` con `Bearer <token de login>` | 200 | 200 |
| Negativo | Mismo token con el último carácter alterado | 401 | 401 |
| Sin token | `GET /api/tasks` sin `Authorization` | 401 | 401 |

Los procesos se detuvieron al terminar. El secreto no se guardó en ningún archivo.

### 2.3 Análisis de dependencias (SCA)

Comando: `pip-audit -r api/requirements.txt --progress-spinner off` en un entorno temporal fuera del repositorio.

Resultado: `Found 32 known vulnerabilities in 3 packages`.

| Paquete | Versión | Identificadores | Versión corregida |
|---|---|---|---|
| `urllib3` | 2.5.0 | PYSEC-2026-1994, 1996, 1998, 141, 4175, 4177 | 2.6.0 – 2.8.0 |
| `python-multipart` | 0.0.21 | PYSEC-2026-1852, 3036, 3037, 3038, 3039, 3040 | 0.0.22 – 0.0.31 |
| `starlette` | 0.50.0 | PYSEC-2026-161, 248, 249, 2280, 2281 | 1.0.1 – 1.3.1 |

`PyJWT==2.15.1` no aparece en el reporte.

Pendiente: confirmar severidad (High/Critical) de cada identificador y decidir la actualización. Ese cambio modifica el manifiesto de dependencias y debe hacerse en un PR aparte.

## 3. Decisiones pendientes (no tomadas)

| Tarea | Decisión requerida | Responsable |
|---|---|---|
| 0.1 | Aprobar `proposal.md`, `design.md` y la spec. Confirmar la ubicación de esta evidencia. | Revisor humano |
| 0.2 | Confirmar el estado de H-AUTH-03 (`Pendiente` vs `Resuelto`). | Revisor humano |
| 3.1 | Decidir G1 (`Bearer` obligatorio), G3 (32 bytes), G4 (`sub` y `role` no vacíos). | Revisor humano |
| 3.2 | Decidir G2 (default de `AUTH_TOKEN_SECRET` en `compose.yaml`). Afecta a `auth/`. | Revisor humano |
| 3.3 | Decidir G5 (cuerpo del `401`). | Revisor humano |
| 3.4 | Decidir si se añade `pyflakes`. | Revisor humano |
| SCA | Decidir la actualización de `urllib3`, `python-multipart` y `starlette`, tras confirmar severidad. | Revisor humano |
| 7.1 y 7.2 | Reconciliar documentos y cerrar el Issue #25. | Revisor humano |

## 4. Riesgos residuales

- G2: el default de `compose.yaml` permite forjar tokens en despliegues sin variable definida.
- G1, G3, G4: comportamiento permisivo caracterizado por pruebas. Si no se corrige, las pruebas se vuelven contrato.
- Vulnerabilidades SCA en `urllib3`, `python-multipart` y `starlette` (sección 2.3).
- Consumidores externos de tokens no confirmados.
- Compose no se ejecutó (sin Docker en este entorno).

## 5. Rollback

- Esta evidencia es un documento nuevo; revertirla implica borrar este archivo.
- El rollback de H-AUTH-03 está descrito en [PLAN.md](./PLAN.md) y en el cuerpo del PR #31.
