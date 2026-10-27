# Propuesta de decisión arquitectónica — H-AUTH-03

**Estado:** aprobada por el usuario tal como está redactada (sesión del día 2).  
**Entrada:** [HU-01 — Validación de firma del token de autenticación](./HU-01.md).

## Impacto

- `auth` emite JWT compacto HS256 con `sub`, `role` y `exp`. La firma usa el secreto configurado mediante `AUTH_TOKEN_SECRET`.
- `api` no valida ese contrato actualmente: `legacy_identity` decodifica un token Base64 con cuatro campos separados por `|`, incluida una constante secreta, y comprueba su expiración. No verifica una firma JWT.
- `web` obtiene el token de `POST /auth/login`, lo guarda en `localStorage` y lo envía como `Authorization: Bearer ...` a la API. Por tanto, el token emitido actualmente no coincide con el formato que `api` interpreta. El flujo de login y acceso a tareas no comparte un contrato de token compatible.
- La dependencia de identidad de `api` protege `GET /api/tasks`, `POST /api/tasks`, `GET /api/summary`, `POST /api/requirements` y `GET /api/backend-health`; corregir el punto central puede cubrir esos puntos sin cambiar cada ruta.
- `compose.yaml` no configura explícitamente `AUTH_TOKEN_SECRET` para `auth` ni para `api`. La configuración de `auth` usa un valor vacío si falta esa variable, mientras que su emisor requiere al menos 32 bytes. La verificación compartida necesita una decisión de aprovisionamiento seguro para ambos servicios.

## Componentes afectados

- **Emisor — `auth`:** [AuthResource.java](./auth/src/main/java/com/bcp/workshop/auth/AuthResource.java) crea el token de login; [TokenSigner.java](./auth/src/main/java/com/bcp/workshop/auth/TokenSigner.java) fija HS256 y los claims emitidos. Se preserva su contrato.
- **Consumidor de seguridad — `api`:** [main.py](./api/app/main.py), en `legacy_identity` y sus dependencias de ruta, concentra hoy la interpretación del token. Los tests existentes están en [test_api.py](./api/tests/test_api.py).
- **Cliente transportador — `web`:** [login.component.ts](./web/src/app/login/login.component.ts) guarda el token recibido; [dashboard.component.ts](./web/src/app/dashboard/dashboard.component.ts) lo reenvía a la API. El navegador no debe ser la autoridad que valida firma o permisos ni recibir el secreto HMAC.
- **Configuración de ejecución — Compose:** [compose.yaml](./compose.yaml) inicia los tres componentes, pero actualmente no muestra una configuración compartida del secreto de token.
- **Documentación de alcance:** [HALLAZGOS.md](./HALLAZGOS.md), [HU-01.md](./HU-01.md) y [docs/ENDPOINTS.md](./docs/ENDPOINTS.md) aún indican que los consumidores no fueron inspeccionados; deberán reflejar el inventario confirmado antes de cerrar H-AUTH-03.

## Contratos protegidos

- **`POST /auth/login`:** preservar ruta, campos de solicitud, códigos de respuesta observados y JSON exitoso con `token`, `expiresIn`, `user` y `debug`.
- **Token emitido:** preservar JWT HS256 y los claims `sub`, `role` y `exp`; no exponer el secreto en el cliente.
- **`/api/tasks`:** preservar rutas y formatos consumidos por el frontend. La autenticación inválida debe seguir rechazándose con `401`; no introducir cambios de contrato de éxito como efecto colateral.
- **Otras rutas protegidas de `api`:** conservar sus respuestas de éxito y cuerpos. No se fija aquí un nuevo cuerpo para tokens rechazados.
- **Transporte web:** mantener el envío del valor de token recibido en el header Bearer; el cliente no decodifica claims para conceder autorización.

## Estrategia incremental

1. **Actualizar la evidencia del flujo, antes de producción:** registrar `api` como consumidor verificador y `web` como cliente que almacena/reenvía el token. Corregir las notas que aún afirman que no hay consumidores inspeccionados. Confirmar si existen clientes fuera del repositorio.
2. **Caracterizar la integración actual:** agregar una prueba que use un token con el contrato emitido por `auth` y registre que hoy no es compatible con el verificador de `api`. Mantener pruebas de regresión para la respuesta de login y los formatos de `/api/tasks`.
3. **Acordar configuración de verificación:** definir cómo reciben `auth` y `api` el mismo secreto en ejecución y en pruebas aisladas. No almacenar valores reales en el repositorio ni entregarlos a `web`.
4. **Cambiar el límite común de autenticación de `api`:** reemplazar la interpretación del token legado en el punto central de dependencia por validación del contrato JWT acordado, y obtener la identidad únicamente de claims verificados. Mantener las rutas y sus handlers existentes.
5. **Verificar el flujo extremo a extremo:** probar un token emitido por `auth` contra `api`, incluir rechazo de firmas inválidas, contenido alterado y expiración, y confirmar que `web` reenvía el token sin transformarlo ni convertirse en verificador.
6. **Cerrar por componente:** ejecutar las pruebas de `auth`, `api` y `web`; conservar los quality gates vigentes y la meta de cobertura aplicable al código modificado. No registrar resultados hasta ejecutar los comandos.

## Pruebas necesarias

- **Auth:** regresión de `POST /auth/login` y del token HS256 con `sub`, `role` y `exp`; usar únicamente secreto de prueba.
- **API — verificador:** aceptar un token válido emitido con el secreto de prueba acordado; rechazar token malformado, firma inválida, contenido alterado y token expirado con `401`.
- **API — rutas:** comprobar que la dependencia común protege todas las rutas que la usan, y que una solicitud autenticada conserva códigos y formas de respuesta existentes, en particular `GET /api/tasks`.
- **Integración:** producir/obtener un token del contrato de `auth` y usarlo como Bearer en `api`; incluir casos de aceptación y rechazo sin levantar dependencias sobre secretos reales.
- **Web:** comprobar que guarda y reenvía el token recibido sin modificarlo y conserva la llamada a `/api/tasks`; no probar verificación criptográfica en el navegador.
- **Cobertura y evidencia:** usar los comandos documentados por el repositorio para cada componente y registrar sus resultados reales. La especificación requiere contabilizar casos aprobados y fallidos por consumidor.

## Riesgos / trade-offs

- **Secreto HMAC compartido:** ambos servicios deben acceder al mismo secreto. Si se compromete el proceso de `api`, la clave simétrica también permitiría emitir tokens; esto es inherente a HS256 y no debe mitigarse enviando la clave al navegador.
- **Configuración no definida:** Compose no pasa hoy el secreto a los servicios. Elegir un método local y de despliegue seguro es requisito previo; un valor de prueba no debe convertirse en valor de producción.
- **Formato legado:** conservar una ruta de aceptación para el token Base64 antiguo podría mantener una forma de autenticación sin firma. La web observada ya envía el JWT nuevo, pero no se ha confirmado si hay otros clientes. No se recomienda fallback silencioso; primero debe confirmarse el inventario.
- **Dependencia de validación:** `api/requirements.txt` no declara una biblioteca JWT. Se debe preferir un verificador mantenido y limitar explícitamente el algoritmo aceptado; escribir parsing y criptografía propios aumenta el riesgo. La biblioteca y su revisión de dependencia quedan por decidir.
- **Cliente y servidor tienen responsabilidades distintas:** `web` solo transporta el token; la protección debe residir en `api`. Validar en el navegador requeriría exponer la clave compartida y no sería una frontera de seguridad confiable.
- **Autorización de recursos no queda resuelta por verificar la firma:** `api` permite actualmente especificar `owner` en query o payload. La política de acceso a recursos no está resuelta por H-AUTH-03; no debe declararse como segura solo por validar el token y cualquier cambio de propiedad debe tener alcance y aprobación propios.
- **Criterio de aceptación de HU-01:** “cada consumidor” necesita distinguir al consumidor de seguridad (`api`) del cliente transportador (`web`). Aplicar al navegador la misma prueba de aceptación/rechazo criptográfico sería una responsabilidad incorrecta; se propone tratarlo como transporte y probar que no toma decisiones de autorización por claims no verificados.

## Decisiones abiertas

1. ¿Se aprueba `api` como único consumidor de seguridad identificado y `web` como cliente transportador, ajustando el criterio por consumidor de HU-01?
2. ¿Hay consumidores de tokens fuera de `api` y `web` que deban incluirse antes de fijar el inventario?
3. ¿Cómo se proporcionará el mismo secreto de prueba/despliegue a `auth` y `api` sin registrarlo en el repositorio, y qué debe hacer Compose cuando falta?
4. ¿Qué biblioteca mantenida se acepta para validar JWT en Python, considerando revisión de dependencia y política de algoritmos?
5. ¿Se confirma que no quedan clientes autorizados a usar el formato legado, para retirar su aceptación sin una transición de compatibilidad?
6. ¿Se mantiene el formato actual de `401` de la API o se requiere especificar el texto de error? La spec protege el rechazo, pero no define su cuerpo.
7. ¿La política de `owner` en endpoints debe tratarse en un hallazgo separado? Esta propuesta no cambia esa semántica.

## Registro de decisión

### Contexto

El emisor ya firma JWT; el backend API sigue interpretando un formato legado sin firma. La web reenvía el token del emisor al backend y no constituye una frontera de autorización.

### Opciones consideradas

1. Verificar el token en la aplicación web.
2. Verificarlo en cada ruta de `api` por separado.
3. Verificarlo una vez en la dependencia común de identidad de `api`.
4. Delegar la validación a un gateway, que no aparece en la topología de [compose.yaml](./compose.yaml).

### Decisión propuesta

Mantener `auth` como emisor y centralizar la verificación criptográfica en la dependencia común de `api`. Mantener `web` como cliente que reenvía el token sin poseer el secreto ni decidir la autorización. No agregar fallback al formato legado salvo que una revisión humana confirme un cliente que requiera transición y apruebe expresamente su política.

### Consecuencias

- Un límite común protege las rutas de `api` sin reescribir cada handler.
- La integración requiere configuración segura del secreto en los dos servicios y pruebas que compartan solo un valor ficticio aislado.
- La interfaz de login y el formato de `/api/tasks` pueden conservarse; la incompatibilidad actual entre emisor y consumidor debe corregirse.
- La elección de biblioteca, el despliegue del secreto, clientes externos y política de transición siguen pendientes de revisión humana.
