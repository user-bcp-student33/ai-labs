# Tareas: validación de tokens de autenticación en API

Componente: `api/`. Issue: [#25](https://github.com/user-bcp-student33/ai-labs/issues/25) (H-AUTH-03). Fuentes aprobadas: [proposal.md](proposal.md), [design.md](design.md) y [specs/api-token-validation/spec.md](specs/api-token-validation/spec.md).

Este checklist es la unidad de implementación del cambio OpenSpec. El Issue sigue siendo la unidad de trabajo del producto.

## Reglas de ejecución

- Git se ejecuta desde la raíz de `ai-labs`; las pruebas se ejecutan con `python tools/lab.py test api` (pytest dentro de `api/`).
- Solo se modifican `api/app/main.py`, `api/tests/test_api.py` y, si se aprueba, `api/requirements-dev.txt` o `compose.yaml`. `auth/` y `web/` no se modifican.
- Las pruebas nuevas usan solo secretos de prueba aislados (`TEST_SECRET`, `OTHER_SECRET` o equivalentes). Ningún secreto real ni valor de `compose.yaml` se copia a pruebas o documentos.
- Ninguna tarea funcional empieza sin su prueba de caracterización en verde y su decisión humana registrada.
- No se usan `# noqa`, exclusiones de scanner ni valores mágicos: los números y textos repetidos se nombran como constantes.

Línea base observada antes de este checklist: `python tools/lab.py test api` → 25 pruebas aprobadas, 0 fallidas, cobertura total 92.98%.

## Fuera de alcance (no implementar)

- Separar `api/` en capas o mover la autenticación fuera de `main.py` (refactor aparte).
- Cambiar `POST /auth/login`, el formato del token o los claims emitidos por `auth`.
- Cambiar rutas, formatos de éxito o el cuerpo de error de `api/` sin decisión aprobada (G5).
- Validar `web/` en el navegador o cambiar el transporte del token.
- H-AUTH-01, H-AUTH-06 (SCA), `?owner=` en `web/` y validación del nombre de archivo en `POST /api/requirements`.
- Inventariar o contratar consumidores externos no confirmados.

## Fase 0 — Aprobaciones previas

Bloquea todas las fases siguientes.

- [ ] **0.1 Revisión humana del paquete OpenSpec.** Confirmar `proposal.md`, `design.md` y la spec, y definir dónde se guardará la evidencia general de H-AUTH-03 en la raíz (con componente `api`, conservando las secciones existentes).
  - Verificación: aprobación registrada fuera del repositorio o en el PR correspondiente.
- [ ] **0.2 Confirmar el estado de H-AUTH-03.** Registrar la discrepancia: el Issue y `HU-01.md` dicen `Pendiente`; `HALLAZGOS.md` dice `Resuelto`; el código ya verifica firma. Esta tarea solo confirma el estado; no edita documentos.
  - Verificación: estado confirmado por revisión humana.

## Fase 1 — Caracterización de comportamiento actual

Solo pruebas nuevas en [api/tests/test_api.py](../../../api/tests/test_api.py). No se modifica [api/app/main.py](../../../api/app/main.py) en esta fase. Cada prueba debe pasar con el código actual, porque documenta lo que hoy ocurre.

- [x] **1.1 Caracterizar token sin prefijo `Bearer` (G1).** Enviar un token válido como `Authorization` sin prefijo y fijar el estado actual (`200`).
  - Verificación: `python tools/lab.py test api` pasa con la nueva prueba.
- [x] **1.2 Caracterizar secreto corto (G3).** Con `AUTH_TOKEN_SECRET` de menos de 32 bytes y un token firmado con ese secreto, fijar la aceptación actual (`200`).
  - Verificación: la prueba pasa sin cambiar `main.py`.
- [x] **1.3 Caracterizar `sub` vacío o no cadena (G4).** Tokens firmados con `sub` `""` y con `sub` numérico, con `role` y `exp` válidos; fijar la aceptación actual (`200`).
  - Verificación: ambas pruebas pasan sin cambiar `main.py`.
  - Resultado: `sub` `""` se acepta (`200`). `sub` numérico se **rechaza** (`401`, "Subject must be a string" de PyJWT 2.15.1); la prueba fija ese comportamiento observado, no el `200` que esperaba el diseño. Discrepancia pendiente de revisión humana (G4).
- [x] **1.4 Verificar la base de caracterización.** Ejecutar la suite completa.
  - Verificación: `python tools/lab.py test api` → 30 pruebas aprobadas, 0 fallidas (25 previas + 5 nuevas), cobertura total 92.98% ≥ 85%.

## Fase 2 — Contrato cruzado con `auth`

Puede ejecutarse en paralelo con la Fase 1, después de la 0.1.

- [x] **2.1 Generar una fixture fija de token de `auth`.** Reproducir el formato de [TokenSigner.java](../../../auth/src/main/java/com/bcp/workshop/auth/TokenSigner.java) (HS256 con `sub`, `role`, `exp`) con `TEST_SECRET`. Usar un `exp` lejano y fijo para no depender del reloj. Leer `auth/` solo como referencia y registrar cómo se generó la fixture.
  - Verificación: la fixture se valida manualmente con `jwt.decode` en el entorno de `api/`.
  - Resultado: fixture `AUTH_ISSUED_TOKEN` en `api/tests/test_api.py`, generada con Python replicando el header compacto y la firma HMAC-SHA256 de `TokenSigner` (no se ejecutó Java/Maven); `sub` `student`, `role` `STUDENT`, `exp` 4102444800. Decodificada con `jwt.decode` y `require: [sub, role, exp]`: correcta.
- [x] **2.2 Probar la fixture contra la API.** Añadir una prueba que llame a `GET /api/tasks` con `Authorization: Bearer <fixture>` y espere `200`.
  - Verificación: la prueba pasa; la prueba existente `test_accepts_token_issued_by_auth_contract` se conserva.
  - Resultado: `test_accepts_fixed_auth_issued_token_fixture` pasa (`200`) en la suite de 30 pruebas.

## Fase 3 — Decisiones humanas

Bloquea la Fase 4. Ninguna decisión se toma por defecto en silencio.

- [ ] **3.1 Decidir G1–G4 por separado.** Para cada brecha: aprobar, rechazar o diferir. Si se aprueba G1, se exige el prefijo `Bearer`; si G3, la longitud mínima; si G4, `sub` y `role` como cadenas no vacías.
  - Verificación: decisión registrada por brecha.
- [ ] **3.2 Decidir G2 (default de secreto en `compose.yaml`, líneas 6 y 12).** Eliminar el default o limitarlo a un perfil de desarrollo. Este cambio toca la raíz y afecta a `auth/`; si se aprueba, se trata como ampliación de alcance y no se aplica silenciosamente.
  - Verificación: decisión registrada y alcance ampliado documentado.
- [ ] **3.3 Decidir G5 (cuerpo del `401`).** Pregunta 6 del [ADR-H-AUTH-03.md](../../../ADR-H-AUTH-03.md). Por defecto no se cambia el formato.
  - Verificación: decisión registrada.
- [ ] **3.4 Decidir la herramienta de lint.** `pyflakes` aparece en el diseño, pero no está en [api/requirements-dev.txt](../../../api/requirements-dev.txt). Añadirlo requiere aprobación; si no se aprueba, no se usa lint adicional para `api/`.
  - Verificación: decisión registrada.

## Fase 4 — Cambios funcionales incrementales (solo lo aprobado)

Cada tarea sigue el orden: la prueba de caracterización de la Fase 1 cambia a rojo, se ajusta al contrato aprobado y pasa a verde. Cambio mínimo en `verify_bearer_token` o `current_identity`; sin tocar rutas ni handlers.

- [ ] **4.1 Exigir prefijo `Bearer` (G1, si se aprueba en 3.1).** Responder `401` cuando falte el prefijo, usando la constante `BEARER_PREFIX` existente y sin `removeprefix` silencioso. Actualizar la prueba 1.1 al contrato nuevo.
  - Verificación: `python tools/lab.py test api` en verde.
- [ ] **4.2 Exigir longitud mínima del secreto (G3, si se aprueba en 3.1).** Constante con nombre (por ejemplo `MIN_TOKEN_SECRET_BYTES = 32`). Un secreto corto se trata como secreto no configurado: `401`, sin aceptar el token. Actualizar la prueba 1.2.
  - Verificación: suite en verde; `TEST_SECRET` y `OTHER_SECRET` ya superan 32 bytes.
- [ ] **4.3 Validar `sub` y `role` como cadenas no vacías (G4, si se aprueba en 3.1).** Rechazar con `401` cuando no cumplan. Actualizar la prueba 1.3.
  - Verificación: suite en verde.
- [ ] **4.4 Ajustar el cuerpo del `401` (G5, solo si se aprueba en 3.3).** Sin decisión aprobada, esta tarea se omite.
  - Verificación: suite en verde; si se omite, se registra como "no aplicado".
- [ ] **4.5 Aplicar G2 (solo si se aprueba en 3.2).** Quitar el default de `AUTH_TOKEN_SECRET` en `compose.yaml` o limitarlo a desarrollo, documentando el impacto en `auth/`.
  - Verificación: `docker compose config` muestra la variable sin default en el perfil elegido; las pruebas de `api/` no cambian.

## Fase 5 — Evidencia end-to-end (requiere confirmación)

Ejecuta los servicios reales, sin modificar código de `auth/`.

- [ ] **5.1 Caso positivo.** Levantar `auth` y `api` con el mismo `AUTH_TOKEN_SECRET` de desarrollo (variable de entorno, no el valor del repositorio). Obtener un token con `POST /auth/login` y llamar a `GET /api/tasks` con `Bearer`.
  - Verificación: comandos y códigos HTTP reales registrados en la evidencia.
- [ ] **5.2 Caso negativo.** Alterar un byte del token y llamar a la API.
  - Verificación: `401`, registrado en la evidencia.

## Fase 6 — Verificación y evidencia

- [ ] **6.1 Suite de `api/`.** Ejecutar `python tools/lab.py test api`.
  - Verificación: número de pruebas aprobadas y fallidas, y cobertura de `app/` ≥ 85%, registrados.
- [ ] **6.2 Lint.** Ejecutar el lint aprobado en 3.4; si no se aprobó, registrar "no aplicado".
  - Resultado: no aplicado; 3.4 (`pyflakes`) sin aprobación.
- [x] **6.3 Contratos intactos.** Verificar que no cambian `auth/`, `web/` ni `api/requirements.txt`, salvo lo aprobado en 3.2.
  - Verificación: `git --no-pager status --short` y `git --no-pager diff --stat` desde la raíz.
  - Resultado: el diff solo toca `api/tests/test_api.py` y `openspec/changes/api-validates-auth-tokens/tasks.md`. `.vscode/mcp.json` tenía cambios previos ajenos a este trabajo.
- [x] **6.4 Calidad de código.** Buscar `# noqa`, secretos reales y números o textos mágicos introducidos.
  - Verificación: sin hallazgos en el diff de `api/`.
  - Resultado: sin `# noqa` ni secretos reales; los literales nuevos son valores de prueba aislados (`TEST_SECRET`, `short-test-secret`, `sub` 42) y fixtures.
- [ ] **6.5 Evidencia general.** Registrar en la raíz la evidencia de H-AUTH-03 para el componente `api`: inventario de consumidores (`auth` emisor, `api` verificador, `web` transportador; externos no confirmados), comandos, resultados y decisiones de la Fase 3. Conservar las secciones existentes.
  - Verificación: revisión humana de la evidencia.

## Fase 7 — Documentos y cierre (solo con evidencia y revisión humana)

- [ ] **7.1 Reconciliar el estado de H-AUTH-03.** Actualizar solo las referencias a H-AUTH-03 en [HU-01.md](../../../HU-01.md), [HALLAZGOS.md](../../../HALLAZGOS.md), [ADR-H-AUTH-03.md](../../../ADR-H-AUTH-03.md) y [docs/TESTING.md](../../../docs/TESTING.md), según la evidencia de la Fase 6. Esta tarea corresponde al paso 1 del diseño; se ejecuta al final porque el estado depende de la evidencia.
  - Verificación: revisión humana; ninguna otra sección cambia.
- [ ] **7.2 Cierre del Issue.** Comentar el resultado en [#25](https://github.com/user-bcp-student33/ai-labs/issues/25). No cerrar el Issue sin aprobación humana.
  - Verificación: aprobación registrada.

## Dependencias

```text
0.1 → 1.1, 1.2, 1.3 → 1.4
0.1 → 2.1 → 2.2
0.1 → 3.1, 3.2, 3.3, 3.4 (decisiones humanas)
1.x + 3.1 → 4.1, 4.2, 4.3
3.3 → 4.4
1.x + 3.2 → 4.5
4.x → 5.1 → 5.2
4.x + 5.x → 6.1 → 6.2 → 6.3 → 6.4 → 6.5
6.x + revisión humana → 7.1 → 7.2
```
