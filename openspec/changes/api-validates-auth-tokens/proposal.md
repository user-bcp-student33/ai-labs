# Validacion de tokens de autenticacion en API

## Why

El Issue [#25](https://github.com/user-bcp-student33/ai-labs/issues/25) compromete la verificacion de firma de los tokens emitidos por `auth` antes de que un consumidor confie en sus claims. Un consumidor que acepte claims sin verificar podria tratar un token alterado o no emitido por `auth` como autenticado.

La evidencia del repositorio identifica `api/` como consumidor que valida el token y `web/` como cliente que solo lo reenvia. El Issue marca H-AUTH-03 como `Pendiente`, mientras que `HALLAZGOS.md`, `PLAN.md` y `docs/ENDPOINTS.md` registran la verificacion de `api/` como resuelta. Esta propuesta fija el comportamiento comprometido por el Issue para que pueda revisarse y verificarse, sin afirmar que la implementacion local falte.

## What Changes

- Formaliza los requisitos de autenticacion de `api/` basados en tokens verificados.
- Especifica escenarios observables para tokens validos, firmas invalidas, contenido alterado, expiracion, claims requeridos y ausencia del secreto de verificacion.
- Protege el contrato observado del login y el formato de token emitido por `auth`.

No se prescribe una implementacion tecnica.

## Capabilities

- `api-token-validation`: autenticacion de solicitudes de API usando un token cuya firma y claims requeridos fueron validados.

## Impact

- Componente elegido: `api/`.
- Baseline protegido: `POST /auth/login` y JWT HS256 con claims `sub`, `role` y `exp`, segun `docs/ENDPOINTS.md`.
- `auth/` permanece como emisor; `web/` permanece como cliente que reenvia el token y no toma decisiones de autorizacion basadas en claims no verificados.
- Los endpoints protegidos de `api/` y sus respuestas existentes no reciben cambios implicitos.
- El Issue solicita inventariar consumidores reales. La evidencia disponible en el repositorio identifica `api/` como verificador y `web/` como transportador; consumidores externos no estan confirmados y no se presumen.

## Scope

- Verificar en `api/` que la firma se valida antes de confiar en identidad o autorizacion derivadas del token.
- Aceptar tokens validos que cumplen el contrato vigente del emisor.
- Rechazar tokens cuya firma no sea valida, cuyo contenido haya sido alterado, que hayan expirado o que carezcan de claims requeridos.
- Fallar cerrado cuando no se disponga del secreto necesario para verificar.
- Mantener evidencia de los consumidores del repositorio y de la validacion realizada.

## Out of Scope

- Cambiar la ruta, request, respuestas o campos de `POST /auth/login`.
- Cambiar el formato, algoritmo o claims del token emitido por `auth`.
- Cambiar rutas, formatos de respuesta o politica de errores de `api/`.
- Cambiar el transporte del token en `web/` o agregar verificacion criptografica al cliente.
- Definir contratos para consumidores externos no identificados.
- Abordar otros hallazgos, incluida la politica de propiedad de recursos.

## Preserved Behavior

- `POST /auth/login` conserva los campos `username`, `password` y `client` opcional; conserva `400` para solicitud/campos requeridos invalidos, `401` para credenciales invalidas y `200` con `token`, `expiresIn`, `user` (`username`, `displayName`, `role`) y `debug` para autenticacion exitosa.
- El token emitido conserva el formato JWT HS256 y los claims `sub`, `role` y `exp`.
- Las operaciones exitosas de `api/` conservan sus rutas y formatos actuales.
- `web/` continua reenviando el token recibido en el header Bearer; el cliente no recibe el secreto de firma.

## Acceptance Criteria

1. Se registra el rol de cada consumidor identificado en el repositorio: emisor, verificador o transportador; no se asumen consumidores externos.
2. Una solicitud protegida de `api/` con un token valido es autenticada usando claims verificados y conserva la respuesta exitosa existente.
3. Una solicitud protegida con firma invalida o contenido alterado no se trata como autenticada ni autorizada.
4. Una solicitud con token expirado o sin cualquiera de los claims requeridos no se trata como autenticada ni autorizada.
5. Si falta el secreto de verificacion, `api/` no acepta el token ni concede acceso.
6. La evidencia de validacion registra comandos y cantidades reales de casos aprobados y fallidos.
7. La verificacion no modifica el contrato documentado de `POST /auth/login` ni el formato del token.

## Risks and Open Questions

- El Issue y la documentacion local discrepan sobre si H-AUTH-03 sigue pendiente o ya esta resuelto. La revision humana debe confirmar el estado y si la propuesta formaliza evidencia existente o requiere trabajo adicional.
- No se ha confirmado si existen consumidores de tokens fuera de este repositorio.
- Esta propuesta no fija estado HTTP ni cuerpo para rechazos de token; se preserva el comportamiento existente de `api/`.