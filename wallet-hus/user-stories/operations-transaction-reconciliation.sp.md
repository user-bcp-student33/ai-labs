# Conciliación operativa de transacciones

Como analista de operaciones,
quiero una conciliación diaria entre intenciones de pago, resultados del proveedor y asientos del ledger de wallet,
para detectar descalces temprano y mantener la integridad financiera del MVP.

## Criterios de aceptación

1. La salida diaria de conciliación compara registros de transacción de wallet contra registros de resultado de proveedor en los canales MVP.
2. Cada descalce se marca con referencia de transacción, tipo de descalce y estado actual de investigación.
3. Los reportes de conciliación se pueden filtrar por fecha y canal de proveedor.
4. Se genera un resumen diario con totales de registros conciliados, no conciliados y resueltos.

## Contexto

- Épica: [MVP de wallet en Perú](../epic/peru-wallet-mvp.sp.md).
- Los estados asíncronos y la conciliación end-to-end son requisitos explícitos del ecosistema.
- El riesgo de dependencia de terceros exige controles operativos antes de considerar una transacción como definitiva.

## Supuestos

- Las herramientas de conciliación se definirán durante la implementación.

## Preguntas abiertas

- ¿Cuál es la tasa máxima diaria de descalce aceptable para preparación del lanzamiento del MVP?
