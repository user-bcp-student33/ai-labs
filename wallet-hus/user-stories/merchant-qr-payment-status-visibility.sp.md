# Visibilidad del estado de pagos QR para comercios

Como pequeño comercio que cobra con QR,
quiero ver el estado confiable de cada transacción de cliente,
para decidir correctamente cuándo entregar el producto o servicio.

## Criterios de aceptación

1. El comercio puede visualizar cada intento de pago QR con una referencia única de transacción.
2. Cada intento muestra un estado actual dentro del conjunto de estados habilitado para el MVP.
3. La vista de estado se actualiza cuando callbacks/eventos del proveedor cambian el estado de la transacción.
4. El comercio puede filtrar el historial de estados por fecha y referencia de transacción para su operación diaria.

## Contexto

- Épica: [MVP de wallet en Perú](../epic/peru-wallet-mvp.sp.md).
- La interoperabilidad QR y los rieles de adquirencia tienen presencia material en la línea base del mercado.
- La dependencia de terceros y la confiabilidad de confirmación son riesgos explícitos para la ejecución del MVP.

## Supuestos

- El canal de consulta del comercio (app, web o ambos) queda por definir.

## Preguntas abiertas

- ¿Cuál es el conjunto mínimo de estados requerido para operación de comercios en el lanzamiento MVP?
