# Idempotencia en pagos

Como analista de operaciones y confiabilidad,
quiero que los intentos de pago duplicados se manejen con comportamiento idempotente,
para evitar duplicidad financiera y desvíos en conciliación cuando hay reintentos.

## Criterios de aceptación

1. Cada intento de pago se envía con una clave de idempotencia única por pagador, monto, destino y ventana de intento.
2. Una solicitud repetida con la misma clave de idempotencia devuelve el resultado original y no crea una nueva transacción financiera.
3. Los eventos de solicitud duplicada se registran con identificadores de solicitud y identificadores de correlación para auditoría.
4. Las salidas de conciliación distinguen claramente intentos originales de reintentos duplicados.

## Contexto

- Épica: [MVP de wallet en Perú](../epic/peru-wallet-mvp.sp.md).
- El riesgo de idempotencia está documentado explícitamente con comportamiento de payload duplicado en la evidencia del ecosistema.
- Las condiciones multirriel exigen controles transaccionales consistentes, independientemente del camino de procesamiento subyacente.

## Preguntas abiertas

- ¿Cuál es la ventana de retención de claves de idempotencia para el MVP por canal?
