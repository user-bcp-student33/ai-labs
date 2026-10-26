AMBITO
Workspace: ai-labs, abierto en su raiz. Todas las rutas son relativas
a esa raiz. Lee el codigo solo de auth/; escribe codigo, configuracion y
pruebas solo en auth/. No leas ni modifiques codigo de otros componentes.
Lee README.md, RETO.md, HALLAZGOS.md, PLAN.md y
docs/GOOD_PRACTICES.md. Consulta los demas documentos de docs/ si existen.
Escribe evidencias solo en HALLAZGOS.md, PLAN.md, prompts/,
docs/ENDPOINTS.md, docs/TESTING.md y docs/GOOD_PRACTICES.md de la raiz,
conservando las filas y secciones de otros componentes. No los sobrescribas.
Cada hallazgo lleva componente auth e ID H-AUTH-NN, por ejemplo H-AUTH-01.
Cita codigo como auth/ruta:linea. No renumeres IDs existentes.
Ejecuta Git desde ai-labs/; ejecuta pruebas con cwd auth/.
Coordina dependencias externas sin ampliar AMBITO. No inventes aprobaciones
humanas ni resultados de pruebas. Si falta evidencia, registra el pendiente.

CONTEXTO
Trabaja solo las filas y secciones de auth en los documentos generales.
Conserva contenido previo de otros componentes y el historial de este componente.
Si docs/ENDPOINTS.md existe, distingue contrato observado, contrato acordado y
riesgos pendientes; si no, conserva el comportamiento observado en PLAN.md.
Registra cada ejecucion con componente e IDs en PLAN.md. No crees
docs/ENDPOINTS.md ni docs/TESTING.md hasta sus pasos de documentacion.
Comprueba la aprobacion humana real de los IDs seleccionados en PLAN.md.
Si falta, detente antes de editar codigo. docs/GOOD_PRACTICES.md sigue siendo
la referencia de como debe quedar el codigo.

OBJETIVO
Implementar unicamente lo que dice PLAN.md, hallazgo por hallazgo: la prueba de
caracterizacion primero, despues el cambio.

RESTRICCIONES
No agregues cambios fuera de PLAN.md. Conserva los quality gates locales configurados. CodeQL y
Dependency Review remotos no se ejecutan en este recorrido local; decláralos no ejecutados. No uses secretos reales.

CRITERIOS DE ACEPTACION
El contrato de docs/ENDPOINTS.md, si existe, o el comportamiento observado
registrado en PLAN.md sigue funcionando igual para el cliente.
Cobertura del codigo modificado en 85% o superior (AGENTS.md). El codigo
resultante respeta las reglas de Clean Code de AGENTS.md, no solo el
hallazgo puntual. Cada hallazgo resuelto queda marcado "Resuelto" en
HALLAZGOS.md, con una nota corta de como se resolvio.

FORMATO DE RESPUESTA
Por cada hallazgo: el diff, la prueba de caracterizacion antes y despues, y la prueba de correccion
con su resultado antes del cambio y despues del cambio, y
HALLAZGOS.md actualizado. Al finalizar, explica cualquier desviacion del plan.