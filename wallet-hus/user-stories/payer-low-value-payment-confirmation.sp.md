# Confirmación de pagos de bajo monto

Como pagador,
quiero completar una transferencia de bajo monto y recibir una confirmación definitiva del resultado,
para confiar en si el pago se realizó o no.

## Criterios de aceptación

1. Antes de la confirmación final, la pantalla de pago muestra la identidad del destino y el monto de la transferencia.
2. El pagador recibe un único resultado terminal por intento: exitoso, fallido o expirado.
3. Si el pago es exitoso, se muestra una referencia de transacción y queda persistida en el historial.
4. Si el pago falla o expira, el pagador recibe una siguiente acción clara (reintentar o volver) sin crear cobros duplicados.
5. Para la cohorte MVP en Perú, la tasa de caída en checkout de este flujo de pago se reduce de 30% a 8% dentro del trimestre posterior al lanzamiento.
6. Las transacciones exitosas del pagador emiten el evento de activación usado en el cálculo de MAU, para que el reporte de meses 1-3 pueda verificar el objetivo de MAU entre 1,000 y 2,000.

## Contexto

- Épica: [MVP de wallet en Perú](../epic/peru-wallet-mvp.sp.md).
- Esta historia contribuye directamente al objetivo de la épica: reducir la caída en checkout de 30% a 8%.
- Esta historia también contribuye al seguimiento de MAU mediante el evento de activación del pagador para el reporte de meses 1-3.
- Los límites transaccionales por proveedor pueden estar por debajo del máximo regulatorio; la validación de monto visible al usuario debe aplicarse antes del envío.
- El comportamiento de confirmación debe contemplar transiciones asíncronas de estado de pago.

## Supuestos

- Las etiquetas de los estados terminales que verá el usuario quedan por definir con producto.
- El rango objetivo de MAU (1,000-2,000) y el objetivo de CAC (PEN 10) fueron definidos por el Product Owner.

## Preguntas abiertas

- ¿Qué regla inicial de monto bajo se configurará para el lanzamiento MVP por proveedor/canal?
