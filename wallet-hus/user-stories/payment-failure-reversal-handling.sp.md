# Manejo de fallas de pago y reversos

Como especialista de soporte al cliente,
quiero que los pagos fallidos, expirados y disputados sigan rutas de atención explícitas,
para que los usuarios reciban guía consistente y soporte pueda resolver incidentes de forma predecible.

## Criterios de aceptación

1. Los resultados de pago fallido y expirado se clasifican y almacenan con un código de motivo legible por máquina.
2. La vista de soporte muestra si un pago es elegible para reintento, investigación o flujo de disputa.
3. La ruta de ayuda al usuario diferencia una falla técnica de un escenario de transferencia ya ejecutada.
4. Las acciones de soporte se registran con referencia de transacción y estado de resolución.

## Contexto

- Épica: [MVP de wallet en Perú](../epic/peru-wallet-mvp.sp.md).
- La línea base de evidencia indica que no existe una regla pública uniforme para cancelar unilateralmente transferencias P2P ya ejecutadas.
- La dirección regulatoria incrementa obligaciones sobre fraude, disputas y manejo de devoluciones/reversos desde 2027.

## Supuestos

- Los niveles de escalamiento de soporte y los SLA quedan por definir.

## Preguntas abiertas

- ¿Qué categorías de disputa deben habilitarse en MVP y cuáles se difieren para post-MVP?
