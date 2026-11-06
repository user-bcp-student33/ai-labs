# Registro y activación del MVP de wallet

Como usuario de smartphone que realiza transferencias diarias de bajo monto,
quiero completar el onboarding de la wallet con un flujo guiado y señales claras de siguiente paso,
para poder empezar a pagar sin abandonar el proceso.

## Criterios de aceptación

1. El flujo de onboarding muestra la secuencia de pasos requeridos y el paso actual en todo momento.
2. Si falta información obligatoria o hay datos inválidos, el flujo bloquea el avance y muestra un mensaje accionable para corregir.
3. Cuando el onboarding finaliza correctamente, el estado de la wallet cambia a activo y el usuario puede acceder al punto de entrada de pagos de bajo monto.
4. El sistema registra eventos de inicio de onboarding, finalización de cada paso y cierre del flujo para analítica del MVP.
5. Para la cohorte MVP en Perú, el tiempo mediano de finalización de onboarding baja de 8 minutos a 3 minutos dentro del trimestre posterior al lanzamiento.
6. La salida analítica de onboarding incluye conteos mensuales de nuevos usuarios activos y la atribución de gasto de adquisición necesaria para verificar MAU entre 1,000 y 2,000 y CAC de PEN 10 por usuario nuevo en los primeros 3 meses.

## Contexto

- Épica: [MVP de wallet en Perú](../epic/peru-wallet-mvp.sp.md).
- Esta historia contribuye directamente al objetivo de la épica: reducir el tiempo de finalización de onboarding de 8 minutos a 3 minutos.
- Esta historia captura la instrumentación de adquisición/activación necesaria para seguir MAU (1,000-2,000) y CAC (PEN 10) en meses 1-3.
- La historia se apoya en el tamaño de oportunidad documentado para comportamiento wallet interoperable en Perú.
- El MVP debe alinearse con expectativas de usabilidad y continuidad en pagos inmediatos.

## Supuestos

- Los requisitos de KYC y documentación para el onboarding deben definirse con cumplimiento y producto antes de implementar.
- El rango objetivo de MAU (1,000-2,000) y el objetivo de CAC (PEN 10) fueron definidos por el Product Owner.

## Preguntas abiertas

- ¿Qué frontera de eventos analíticos define oficialmente la duración de onboarding (timestamp de inicio/fin)?
