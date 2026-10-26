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
La evidencia es git diff main -- auth/ desde la raiz ai-labs/,
en la rama day1/prompting. Limita la inspeccion al componente indicado en AMBITO.
Revisa tambien las secciones del componente en git diff main -- HALLAZGOS.md
PLAN.md prompts docs. No revises codigo de otros componentes. Comprueba
git status --short y los archivos nuevos, que no salen en el diff.
Esta revision local precede a la integracion humana a main del cierre. No requiere GitHub.
El diff local implementa uno o mas hallazgos de tu componente en HALLAZGOS.md. Consulta los documentos
del repositorio que consideres necesarios para justificar cada observacion (por
ejemplo README.md, RETO.md, docs/ENDPOINTS.md y docs/GOOD_PRACTICES.md)
y al final dime cuales usaste.

OBJETIVO
Revisar el diff local como lider tecnico y especialista AppSec, verificando que cada
hallazgo marcado "Resuelto" en HALLAZGOS.md este realmente corregido en el
codigo, y que respeta AGENTS.md.

RESTRICCIONES
No apruebes un diff que reduzca los quality gates locales configurados o
que marque un hallazgo como "Resuelto" sin corregirlo de verdad. No inventes
riesgos que no puedas senalar con archivo y linea.

CRITERIOS DE ACEPTACION
Cada hallazgo "Resuelto" del componente elegido en HALLAZGOS.md queda verificado con evidencia, o se
regresa a "Pendiente" si el diff no lo corrigio del todo. Se revisan ademas:
regresiones funcionales, SQL injection, secretos, manejo de contrasenas,
validacion de entrada, errores verbosos, autenticacion debil, dependencias
vulnerables, cobertura y complejidad.

FORMATO DE RESPUESTA
Observaciones clasificadas en bloqueantes, importantes y sugerencias. Para cada
bloqueante: por que bloquea el cierre y que evidencia lo respalda. Si algun
hallazgo vuelve a "Pendiente", dilo explicitamente. Al final, que documentos
consultaste.