AMBITO
Workspace: ai-labs, abierto en su raiz. Todas las rutas son relativas
a esa raiz. Lee el codigo solo de auth/; escribe codigo, configuracion y
pruebas solo en auth/. No leas ni modifiques codigo de otros componentes.
Lee README.md, RETO.md y docs/GOOD_PRACTICES.md. Lee
HALLAZGOS.md, PLAN.md y los demas documentos de docs/ solo si existen.
Escribe evidencias solo en HALLAZGOS.md, PLAN.md, prompts/,
docs/ENDPOINTS.md, docs/TESTING.md y docs/GOOD_PRACTICES.md de la raiz.
Cuando este paso pida un artefacto ausente, crealo. Si ya existe, conserva
las filas y secciones de otros componentes. No las sobrescribas.
Cada hallazgo lleva componente auth e ID H-AUTH-NN, por ejemplo H-AUTH-01.
Cita codigo como auth/ruta:linea. No renumeres IDs existentes.
Ejecuta Git desde ai-labs/; ejecuta pruebas con cwd auth/.
Coordina dependencias externas sin ampliar AMBITO. No inventes aprobaciones
humanas ni resultados de pruebas. Si falta evidencia, registra el pendiente.

CONTEXTO
Trabaja solo las filas y secciones de auth en los documentos generales.
Conserva contenido previo de otros componentes y el historial de este componente.
Si docs/ENDPOINTS.md existe, distingue contrato observado, contrato acordado y
riesgos pendientes. Si docs/TESTING.md existe, relaciona cada ejecucion con
componente e IDs.
Componente de autenticacion de ai-labs en auth/, con Java y Quarkus. docs/ENDPOINTS.md ya refleja el contrato real
y detallado. Consulta ese archivo y cualquier
otro documento que consideres necesario.

OBJETIVO
Crear o actualizar docs/TESTING.md y completar las pruebas que falten para cada endpoint de la seccion del componente elegido en
docs/ENDPOINTS.md, cubriendo tres categorias: camino feliz, casos borde y
errores.

RESTRICCIONES
No inventes endpoints ni comportamientos que no esten en docs/ENDPOINTS.md. No
adaptes un test para que pase con un bug existente: si encuentras un bug,
repórtalo aparte, no lo escondas dentro del test.

CRITERIOS DE ACEPTACION
Por cada endpoint del componente elegido en docs/ENDPOINTS.md hay al menos: un test de camino feliz
(dato valido), un test de caso borde (campo vacio, duplicado o limite), y un
test por cada respuesta de error documentada. Cobertura del codigo modificado en
85% o superior, con reporte y metodo de medicion explicitos.

FORMATO DE RESPUESTA
docs/TESTING.md creado o actualizado con una tabla (endpoint | caso | tipo | resultado
esperado) y despues el codigo de los tests, con comandos, directorio, codigo de salida y resultados realmente ejecutados;
separa fallos, pendientes y pruebas propuestas. Al final, que
documentos consultaste.