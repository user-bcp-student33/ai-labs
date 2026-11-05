# Tasks

## 1. Pruebas de caracterización

- [ ] 1.1 Ampliar la prueba de login válido en `AuthResourceTest` para comprobar el estado `200` y los campos existentes del contrato; verificar que el caso válido sigue pasando antes del cambio funcional.
- [ ] 1.2 Añadir pruebas de credenciales incorrectas y de inyección por usuario y contraseña; verificar `401` sin token para credenciales incorrectas y documentar que los casos de inyección reproducen el fallo actual antes de modificar la consulta.

## 2. Parametrización de la consulta

- [ ] 2.1 Sustituir en `LegacyDatabase.findByCredentials` la concatenación SQL por `PreparedStatement` con parámetros para usuario y contraseña, conservando el mapeo de `UserRecord`; verificar que pasan las pruebas de login válido, credenciales incorrectas e inyección.

## 3. Validación del componente

- [ ] 3.1 Ejecutar `python tools/lab.py test auth` y revisar JaCoCo; verificar que la suite pasa, que la cobertura del código modificado alcanza al menos 85 % y que no se silencian controles de calidad.
