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
Componente de autenticacion de ai-labs en auth/, con Java y Quarkus, ya refactorizado. Si docs/ENDPOINTS.md no
existe, crealo con el contrato comprobado en el codigo actual. Si existe,
actualiza solo la seccion de auth y conserva las secciones de api y web.
Consulta el codigo y los documentos necesarios para verificar cada campo.
NO EDITES CODIGO, esto es solo documentacion.

OBJETIVO
Crear o actualizar docs/ENDPOINTS.md para que un desarrollador pueda integrar
otro componente con la autenticacion de ai-labs sin leer su codigo fuente.

RESTRICCIONES
No inventes campos, tipos o codigos de respuesta que no existan en el codigo. Si
algo no esta claro leyendo el codigo, márcalo como pregunta abierta en vez de
asumirlo. No documentes "GET /q/health" y "POST /auth/login" con una sola linea
cada uno: sigue la estructura de abajo completa.

CRITERIOS DE ACEPTACION
Por cada endpoint, docs/ENDPOINTS.md incluye TODO lo siguiente (nada es
opcional):
1. Que hace (una frase, en terminos de negocio, no solo tecnicos).
2. Si requiere autenticacion y que header exacto.
3. Request: una tabla campo | tipo | obligatorio | descripcion, mas un ejemplo
   JSON real y completo.
4. Response exitosa: codigo de estado, tabla campo | tipo | descripcion, mas un
   ejemplo JSON real y completo.
5. Cada respuesta de error posible: codigo de estado, cuando ocurre, y un
   ejemplo de cuerpo de esa respuesta (no solo "puede fallar").
6. Notas de seguridad relevantes si aplica (por ejemplo, que el token ahora es
   firmado y expira de verdad).
Cada campo y cada ejemplo tienen que poder verificarse contra el codigo real
(archivo + linea si hace falta justificarlo).

FORMATO DE RESPUESTA
docs/ENDPOINTS.md creado o actualizado, siguiendo exactamente esa estructura
de 6 puntos por endpoint. Al final, que documentos consultaste para escribirlo.