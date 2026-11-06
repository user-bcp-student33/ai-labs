# Épica: MVP de wallet en Perú para pagos cotidianos

## Resumen

Lanzar un MVP de wallet enfocado en Perú que reduzca la fricción en checkout y acelere el onboarding para comportamientos de pago frecuentes y de bajo monto. La épica prioriza velocidad de salida a mercado sin perder confianza del usuario, con confirmaciones claras, manejo confiable de estados y límites alineados a cumplimiento.

## Problema y oportunidad

La caída en checkout es alta y el onboarding es lento para personas que hacen transferencias pequeñas frecuentes y pagos con QR. La línea base local de pagos muestra una adopción fuerte de interoperabilidad y un comportamiento claro en flujos basados en alias, lo que abre una oportunidad para lanzar ahora una propuesta de wallet enfocada.

## Objetivos

En un trimestre, lanzar el MVP de wallet y lograr:

- Reducir la caída en checkout de 30% a 8%.
- Reducir el tiempo de finalización de onboarding de 8 minutos a 3 minutos.
- Mantener usuarios activos mensuales (MAU) entre 1,000 y 2,000 durante los primeros 3 meses posteriores al lanzamiento.
- Lograr un costo de adquisición de cliente (CAC) de PEN 10 por cada usuario nuevo durante los primeros 3 meses posteriores al lanzamiento.

## Usuarios objetivo

- Usuarios de smartphone que hacen transferencias diarias de bajo monto: necesitan un recorrido rápido y de baja fricción desde la intención hasta la confirmación del pago.
- Pequeños comercios que cobran con QR: necesitan aceptación predecible, confirmación confiable y estabilidad operativa diaria.

## Alcance

### Incluye

- Flujo de onboarding de wallet para el segmento MVP en Perú.
- Flujos núcleo de pago para transferencias de bajo monto y cobro a comercios con QR.
- Visibilidad de confirmación y estado de pago para usuarios y comercios.
- Instrumentación base del funnel de checkout, finalización de onboarding y activación al mes 3.

### Fuera de alcance

- Despliegue multipaís amplio dentro de esta épica.
- Programas avanzados de lealtad o mecánicas de crecimiento no esenciales.
- Paridad funcional completa entre todos los proveedores/bancos externos desde el día uno.
- Exposición de APIs de pago abiertas y autoservicio para terceros.

## Métricas de éxito

- MVP de wallet lanzado dentro de un trimestre.
- La caída en checkout baja de 30% a 8%.
- El tiempo de finalización de onboarding baja de 8 minutos a 3 minutos.
- El MAU se mantiene entre 1,000 y 2,000 en los meses 1-3 posteriores al lanzamiento.
- El CAC es de PEN 10 por usuario nuevo en los meses 1-3 posteriores al lanzamiento.

## Supuestos y restricciones

- El ecosistema es multirriel; el MVP no debe asumir un único riel de procesamiento de extremo a extremo.
- Las integraciones reales son contractuales y con habilitaciones operativas; el alta de proveedores no es autoservicio inmediato.
- Los límites por proveedor pueden estar por debajo del máximo regulatorio; el producto y los mensajes UX deben contemplarlo.
- El producto debe sostener la expectativa operativa de pagos inmediatos con alias (24x7x365).
- El rango de MAU (1,000-2,000) y el objetivo de CAC (PEN 10) son umbrales de negocio definidos por el Product Owner.

## Dependencias y riesgos

- Dependencia: los partners de adquirencia/gateway seleccionados para QR y procesamiento wallet son necesarios para preparar el lanzamiento.
- Riesgo: reintentos sin idempotencia robusta pueden generar duplicidad financiera y desvíos de conciliación.
- Riesgo: transiciones asíncronas del estado de pago pueden deteriorar la confianza si la confirmación no es clara.
- Riesgo: el manejo operativo/legal de reversos y disputas debe ser explícito porque la cancelación unilateral de P2P no está garantizada de forma uniforme.

## Historias de usuario candidatas

- Como usuario que transfiere a diario, puedo completar onboarding rápidamente y entender el siguiente paso en cada punto del flujo.
- Como pagador, puedo completar un pago de bajo monto y recibir una confirmación clara del resultado.
- Como pequeño comercio, puedo cobrar con QR wallet y validar el estado del pago de forma confiable.
- Como operaciones/compliance, puedo monitorear estados de pago y conciliar transacciones núcleo del MVP.
