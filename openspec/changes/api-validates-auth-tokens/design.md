# Diseño: validación de tokens de autenticación en API

Componente elegido: `api/`. Este diseño no implementa código funcional. Lee [proposal.md](proposal.md) y [specs/api-token-validation/spec.md](specs/api-token-validation/spec.md).

## Contexto técnico

### Estado observado

La verificación ya existe en la rama actual. No se parte de cero:

- [api/app/main.py](../../../api/app/main.py): `verify_bearer_token` decodifica con `jwt.decode(token, secret, algorithms=["HS256"], options={"require": ["sub", "role", "exp"]})`. Sin secreto responde `401`. Cualquier `PyJWTError` responde `401`. `current_identity` es la dependencia común de las cinco rutas protegidas. La identidad sale solo de `sub` y `role` verificados.
- Las consultas SQL usan placeholders `?`. `owner` de query o payload se ignora; el propietario siempre es el `sub` verificado.
- [api/requirements.txt](../../../api/requirements.txt): `PyJWT==2.15.1`.
- [api/tests/test_api.py](../../../api/tests/test_api.py): cubre token del contrato, firma ajena, claims alterados, expiración, falta de `role`, `alg: none`, token Base64 legado, ausencia de secreto y rutas protegidas con token forjado o ausente.
- Línea base ejecutada en este diseño: `pytest tests` desde `api/` terminó sin fallos. El registro de [PLAN.md](../../../PLAN.md) indica 25 pruebas y cobertura de 92.98%.
- Commit `1ffdea2` (verificación JWT) está en `HEAD`. Los commits `7be1827`, `aeb8a6f` y `d6d2c62` aparecen en `origin/feat/h-auth-03-firma-token` y no en `HEAD`; ver riesgos.

### Emisor y transporte

- `auth/` emite JWT HS256 con `sub`, `role` y `exp` (3600 s). Su secreto mínimo es de 32 bytes, según [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md).
- [web/src/app/dashboard/dashboard.component.ts](../../../web/src/app/dashboard/dashboard.component.ts) línea 44 envía `Authorization: 'Bearer ' + token`. No decodifica claims ni verifica firma.

### Discrepancias documentales

| Documento | Afirmación | Estado real |
|---|---|---|
| [HU-01.md](../../../HU-01.md) | H-AUTH-03 `Pendiente`; verificación de consumidores no demostrada | Desactualizado respecto al código y a PLAN.md |
| [HALLAZGOS.md](../../../HALLAZGOS.md) | H-AUTH-03 `Resuelto` | Coincide con el código, pero el inventario de consumidores externos sigue sin confirmar |
| [ADR-H-AUTH-03.md](../../../ADR-H-AUTH-03.md) | `api` usa `legacy_identity` Base64 sin firma | Describe el estado previo al cambio; no el actual |
| [docs/TESTING.md](../../../docs/TESTING.md) línea 167 | H-AUTH-03 pendiente respecto a validación | Desactualizado |

Este diseño no corrige esos documentos; la propuesta de corrección queda en la estrategia (paso 1).

## Goals

- Confirmar que cada ruta protegida de `api/` autentica solo con token cuya firma HS256 y claims requeridos fueron verificados.
- Caracterizar el comportamiento actual antes de cualquier ajuste.
- Registrar el inventario de consumidores: `auth` emisor, `api` verificador, `web` transportador.
- Cerrar las brechas identificadas en esta revisión, solo tras aprobación humana.

## Non-goals

- Cambiar `POST /auth/login`, sus campos, respuestas ni el formato del token.
- Cambiar rutas, formatos de éxito ni el cuerpo de los `401` de `api/` sin decisión aprobada (ADR, pregunta 6).
- Rediseñar la aplicación, separar `api/` en capas o mover la lógica de autenticación fuera de `main.py`. Ese refactor es un cambio aparte.
- Cambiar `web/`; no validar firma en el navegador.
- Resolver H-AUTH-01, H-AUTH-06 (SCA) ni la limpieza de `?owner=` en `web/`.
- Validar nombres de archivo en `POST /api/requirements` (`Path(uploads) / file.filename`). Es un hallazgo distinto.

## Contratos protegidos

| Contrato | Regla |
|---|---|
| `POST /auth/login` | Ruta, `username`, `password`, `client` opcional, `400`/`401`/`500`, JSON `200` con `token`, `expiresIn`, `user`, `debug`. |
| Token de `auth` | JWT HS256; claims `sub`, `role`, `exp`. |
| Rutas de `api/` | `GET /api/tasks`, `POST /api/tasks`, `GET /api/summary`, `POST /api/requirements`, `GET /api/backend-health`: rutas, formatos de éxito y forma de respuesta sin cambios. |
| Identidad | Solo claims verificados; `owner` de query o payload no altera el resultado. |
| Secreto | Solo en variable de entorno `AUTH_TOKEN_SECRET`; nunca en repositorio ni en `web/`. |

## Componentes afectados

| Componente | Archivo | Rol | Cambio previsto |
|---|---|---|---|
| `api` | [api/app/main.py](../../../api/app/main.py) | Verificador | Ninguno sin aprobación; ajustes puntuales si se aprueban G1-G3. |
| `api` | [api/tests/test_api.py](../../../api/tests/test_api.py) | Pruebas | Caracterización de brechas antes de cambiar comportamiento. |
| `api` | [api/requirements.txt](../../../api/requirements.txt) | Dependencia | Sin cambios; confirmar versión y advisories (H-AUTH-06 pendiente). |
| `auth` | [TokenSigner.java](../../../auth/src/main/java/com/bcp/workshop/auth/TokenSigner.java) | Emisor | Ninguno; solo referencia del contrato. |
| `web` | [dashboard.component.ts](../../../web/src/app/dashboard/dashboard.component.ts) | Transportador | Ninguno. |
| Ejecución | [compose.yaml](../../../compose.yaml) | Configuración | Revisar default de `AUTH_TOKEN_SECRET` (G2). |

## Inventario de consumidores

| Consumidor | Rol | Evidencia | Estado |
|---|---|---|---|
| `auth` | Emisor | `AuthResource`, `TokenSigner` | Confirmado |
| `api` | Verificador | `verify_bearer_token`, `current_identity` | Confirmado |
| `web` | Transportador | `Authorization: 'Bearer ' + token` | Confirmado |
| Clientes externos | Desconocido | Sin evidencia en el repositorio | No confirmado; no se presume |

## Decisiones técnicas

### D1. Verificación centralizada en la dependencia común de `api`

Se mantiene `current_identity` como único punto de verificación. Todas las rutas protegidas lo reutilizan.

- Alternativa: verificar en cada ruta. Descartada: duplica lógica y aumenta el riesgo de omisión.
- Alternativa: validar en `web/`. Descartada: obligaría a exponer el secreto al navegador; no es frontera de seguridad.
- Alternativa: gateway. Descartada: no existe en la topología de [compose.yaml](../../../compose.yaml).

### D2. HS256 fijo y `exp`, `sub`, `role` requeridos

Se conserva `algorithms=[TOKEN_ALGORITHM]` y `options={"require": REQUIRED_TOKEN_CLAIMS}`. PyJWT verifica `exp` por defecto. El test `alg: none` ya prueba el rechazo.

### D3. Fallo cerrado sin secreto

Si `AUTH_TOKEN_SECRET` está vacío, `verify_bearer_token` responde `401` antes de decodificar. Cubierto por `test_rejects_every_token_when_secret_is_not_configured`.

### D4. Secreto leído al crear la aplicación

`token_secret` se lee con `os.getenv` en `create_app`. Las pruebas fijan la variable con `monkeypatch` antes de crear la aplicación. Alternativa: leer por solicitud. No se cambia: añadiría coste sin beneficio demostrado.

## Brechas identificadas (requieren decisión antes de cambiar)

| Id | Brecha | Evidencia | Riesgo | Propuesta |
|---|---|---|---|---|
| G1 | `authorization.removeprefix("Bearer ")` acepta un token sin prefijo `Bearer`. | `verify_bearer_token` | Bajo; la spec pide Bearer. `web` sí envía el prefijo. | Caracterizar primero; exigir prefijo y responder `401` si falta. |
| G2 | `compose.yaml` define default `dev-only-change-me-workshop-token-secret` para `auth` y `api`. | [compose.yaml](../../../compose.yaml) líneas 6 y 12 | Alto en despliegues con defaults: cualquiera con el valor puede forjar tokens. Fallo cerrado solo aplica con variable vacía. | Decidir si el default se elimina o se restringe a perfil de desarrollo. |
| G3 | `api` no exige longitud mínima del secreto; `auth` sí (32 bytes). | `main.py` vs `TokenSigner` | Medio: verificador y emisor no comparten la misma política. | Caracterizar; alinear con 32 bytes si se aprueba. |
| G4 | `sub` y `role` no se validan como cadenas no vacías. | `verify_bearer_token` | Bajo: `sub` no vacío no está en la spec. | Caracterizar; validar tipo y no vacío si se aprueba. |
| G5 | El cuerpo del `401` incluye texto de PyJWT y "secret not configured". | `HTTPException(detail=...)` | Bajo: divulgación de configuración. | Pendiente de decisión (pregunta 6 del ADR). No cambiar el formato sin aprobación. |

## Estrategia incremental

1. Documentos: reconciliar H-AUTH-03 en [HU-01.md](../../../HU-01.md), [HALLAZGOS.md](../../../HALLAZGOS.md), [ADR-H-AUTH-03.md](../../../ADR-H-AUTH-03.md) y [docs/TESTING.md](../../../docs/TESTING.md). Solo con aprobación humana.
2. Caracterización (solo pruebas, sin cambiar `main.py`): añadir pruebas que documenten el comportamiento actual de G1, G3 y G4:
   - token válido sin prefijo `Bearer` (estado actual: aceptado);
   - secreto corto (menor de 32 bytes) y su aceptación actual;
   - `sub` no cadena o vacío.
   Estas pruebas fijan el comportamiento antes de cualquier cambio.
3. Contrato cruzado: añadir una prueba en `api/` con un token generado por `auth` (fixture fija generada desde `TokenSigner`, sin secreto real), para detectar deriva del formato.
4. Decisiones humanas: aprobar o rechazar G1-G5 y el default de G2.
5. Cambio mínimo aprobado en `current_identity` o `verify_bearer_token`, con pruebas que pasen de rojo a verde. Sin tocar rutas ni handlers.
6. Evidencia end-to-end: levantar `auth` con `AUTH_TOKEN_SECRET` de desarrollo y llamar a `api` con el token real. Registrar comandos y resultados reales.
7. Verificación: `python tools/lab.py test api` (cobertura del código modificado >= 85%) y `pyflakes` sobre `api/app` y `api/tests`. Las reglas de [AGENTS.md](../../../AGENTS.md) aplican: sin `# noqa`, consultas parametrizadas, sin valores mágicos.
8. Cierre: actualizar estados de H-AUTH-03 solo con evidencia de los pasos anteriores y revisión humana.

## Pruebas de caracterización necesarias antes de modificar comportamiento

| Caso | Estado actual esperado | Antes de cambiar |
|---|---|---|
| Token válido con `Bearer` | `200` | Ya cubierto |
| Token sin prefijo `Bearer` | Aceptado (G1) | Nueva prueba |
| Firma ajena, contenido alterado, expirado, sin `role`, `alg: none`, token legado, sin secreto | `401` | Ya cubierto |
| Secreto de menos de 32 bytes | Aceptado (G3) | Nueva prueba |
| `sub` vacío o no cadena | Aceptado (G4) | Nueva prueba |
| Token generado por `auth` | Aceptado | Nueva prueba con fixture |
| `owner` en query y payload ignorado | Propietario = `sub` | Ya cubierto |

Las pruebas nuevas no deben usar secretos reales; solo valores de prueba aislados.

## Riesgos y trade-offs

- Secreto HS256 compartido: quien tenga el secreto puede emitir tokens. Es inherente a HS256; el secreto no va al navegador.
- Default de Compose (G2): puede dar falsa seguridad. Es el riesgo de mayor impacto de este diseño.
- Rama y commits: el registro de [PLAN.md](../../../PLAN.md) referencia commits en `feat/h-auth-03-firma-token` que no están en `HEAD`. Antes de cualquier reversión hay que fijar el conjunto de commits real de la rama actual.
- Cambiar G1 podría romper clientes que envían el token sin prefijo. En el repositorio solo `web` consume la API y envía el prefijo.
- Dependencia `PyJWT==2.15.1`: confirmar disponibilidad y advisories. H-AUTH-06 (SCA) sigue pendiente.
- Inventario externo: no confirmado. Cualquier cliente externo que dependa del formato legado quedaría afectado.

## Rollback

- Unidad de reversión: los commits de H-AUTH-03 que contienen la verificación. Confirmar la lista exacta antes de revertir.
- Revertir solo `api` reintroduce la aceptación del token Base64 sin firma, que es la vulnerabilidad original. Solo como contención temporal, con aprobación explícita y registro en [HALLAZGOS.md](../../../HALLAZGOS.md).
- Revertir `api` también hace que `web` deje de funcionar contra `api`. Es un efecto esperado; no se parchea en `web/`.
- Para los cambios de la fase 5 (G1-G4): revertir el commit de ese cambio. Las pruebas de caracterización deben volver a su estado previo junto con él.
- Las pruebas de caracterización de la fase 2 se pueden mantener aunque se revierta la fase 5, siempre que documenten el comportamiento revertido.
- No hay migración de datos; revertir no requiere limpieza de base.
- Retirar `PyJWT` de `requirements.txt` solo si se revierte el commit de la dependencia. `AUTH_TOKEN_SECRET` en Compose puede quedar sin uso.
