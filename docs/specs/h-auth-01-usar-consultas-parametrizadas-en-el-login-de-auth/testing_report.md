# testing_report.md

## H-AUTH-01 — Usar consultas parametrizadas en el login de auth

**Estado general:** NO EJECUTADO  
**Alcance:** `auth/`, persistencia de credenciales y contrato de `POST /auth/login`.  
**Base del informe:** criterios de aceptación y fragmento de `LegacyDatabase` incluidos en el historial. No se proporcionó una salida real de pruebas ni el código completo del proyecto.

> Los resultados siguientes son esperados, no resultados observados. No se afirma que las pruebas hayan corrido ni que los cambios estén integrados.

## Preparación

1. Trabajar en una copia local del repositorio y en un entorno de pruebas; no ejecutar estos casos contra producción.
2. Configurar el entorno de pruebas del proyecto y preparar en su base de datos aislada la cuenta:
   - Usuario: `student`
   - Contraseña: `Workshop123!`
3. Usar la configuración de datos y el procedimiento de arranque existentes en el proyecto. El esquema y el comando para iniciar el servicio no fueron proporcionados; no se presupone ninguno.
4. Para las pruebas HTTP, disponer de `bash`, `curl` y `jq`, y definir `AUTH_BASE_URL` con la URL base real del servicio.
5. No imprimir ni registrar tokens o contraseñas. El script de abajo elimina sus archivos temporales al terminar.

## Relación de criterios de aceptación y pruebas

| Criterio | Prueba(s) | Resultado esperado | Resultado observado |
|---|---|---|---|
| `findByCredentials` usa `PreparedStatement` para `username` y `password`, sin concatenarlos en el SQL. | DB-01 y DB-02 | SQL de estructura fija; ambos valores se vinculan como parámetros, en los índices correspondientes. | **NO EJECUTADO.** El fragmento propuesto muestra ese patrón, pero no se verificó el archivo integrado ni el esquema real. |
| Login válido conserva `200`, `user.username` y `token`. | API-01 | `200 OK`; `user.username` es `student`; `token` existe y no está vacío. | **NO EJECUTADO.** |
| Inyección en usuario o contraseña responde `401` y no devuelve `token`. | API-02 y API-03 | En ambos casos, `401 Unauthorized` y ausencia de la propiedad `token` en la respuesta. | **NO EJECUTADO.** |
| `AuthResourceTest` incluye prueba de inyección y `mvn test` pasa desde `auth/`. | INT-01 y BUILD-01 | La prueba del recurso cubre ambos campos, verifica el `401` y la ausencia de token; Maven termina correctamente. | **NO EJECUTADO.** No se proporcionó `AuthResourceTest` ni una salida de Maven. |

## Casos de prueba

### DB-01 — Consulta parametrizada en `findByCredentials`

**Tipo:** persistencia / inspección de código.  
**Preparación:** revisar la implementación integrada de `LegacyDatabase.findByCredentials`.

**Comprobar:**

- La consulta tiene estructura fija y usa marcadores de parámetros.
- El método prepara la consulta mediante `PreparedStatement`.
- `username` se asigna al primer parámetro y `password` al segundo, o a los índices que correspondan al SQL existente.
- Ninguno de los valores se concatena en la cadena SQL.

**Resultado esperado:** la consulta permanece fija y los dos valores se envían como datos enlazados.  
**Resultado observado:** **NO EJECUTADO.**

### DB-02 — Resultado sin coincidencia y entradas de borde del adaptador

**Tipo:** persistencia / borde.  
**Preparación:** usar el mecanismo de pruebas de persistencia del proyecto. No se conoce el framework ni la configuración de base de datos existentes.

**Comprobaciones:**

1. Con credenciales sin coincidencia, el método devuelve el resultado de “sin usuario” que use el contrato actual, sin convertir la entrada en parte del SQL.
2. Si se conserva el comportamiento del fragmento propuesto, llamar al método con `username == null` o `password == null` devuelve `Optional.empty()` sin abrir una consulta.

**Resultado esperado:** no se autentica a un usuario inexistente y las entradas nulas manejadas por el fragmento propuesto no provocan una consulta.  
**Observado:** **NO EJECUTADO.** El comportamiento de nulos descrito corresponde al fragmento propuesto; no se verificó contra la implementación real.

### API-01 — Login válido

**Tipo:** positivo / integración HTTP.  
**Datos:** `student` / `Workshop123!`.

**Resultado esperado:** HTTP `200`; la respuesta incluye `user.username == "student"` y un `token` no vacío.

**Observado:** **NO EJECUTADO.**

### API-02 — Intento de inyección en `username`

**Tipo:** negativo / integración HTTP.  
**Datos:**

- `username`: `' OR '1'='1`
- `password`: `Workshop123!`

**Resultado esperado:** HTTP `401`; la respuesta no incluye la propiedad `token`.

**Observado:** **NO EJECUTADO.**

### API-03 — Intento de inyección en `password`

**Tipo:** negativo / integración HTTP.  
**Datos:**

- `username`: `student`
- `password`: `' OR '1'='1`

**Resultado esperado:** HTTP `401`; la respuesta no incluye la propiedad `token`.

**Observado:** **NO EJECUTADO.**

### API-04 — Contraseña incorrecta no especial

**Tipo:** negativo / integración HTTP.  
**Datos:** usuario `student` y una contraseña inválida de prueba, por ejemplo `invalid-password`.

**Resultado esperado:** HTTP `401` y ausencia de `token`, de acuerdo con el contrato descrito para credenciales incorrectas.

**Observado:** **NO EJECUTADO.**

### API-05 — Campos vacíos o solicitud incompleta

**Tipo:** borde.  
**Preparación:** enviar solicitudes con campos vacíos o incompletos solo si se desea comprobar el comportamiento existente.

**Resultado esperado:** registrar el comportamiento vigente; no se fija un status ni un cuerpo para estos casos porque ese contrato no está especificado en el material recibido. No debe emitirse un token por una solicitud sin credenciales válidas.

**Observado:** **NO EJECUTADO.**

### INT-01 — Pruebas de `AuthResourceTest`

**Tipo:** integración.  
**Acción:** añadir o confirmar casos para inyección en `username` e inyección en `password`. Cada caso debe comprobar el status `401` y que la respuesta no contiene `token`. Mantener las convenciones de configuración, fixtures y framework existentes.

**Resultado esperado:** `AuthResourceTest` cubre ambos campos y las aserciones indicadas pasan.  
**Observado:** **NO EJECUTADO.** No se proporcionó el contenido de la clase.

### BUILD-01 — Suite Maven

**Tipo:** integración / regresión.

**Resultado esperado:** `mvn test`, ejecutado desde `auth/`, termina con éxito y no reporta pruebas fallidas.  
**Observado:** **NO EJECUTADO.**

## Pruebas HTTP reproducibles

Guardar el contenido siguiente como `tests/auth-login-contract.sh` en la raíz del repositorio:

```bash
#!/usr/bin/env bash
set -euo pipefail

: "${AUTH_BASE_URL:?Define AUTH_BASE_URL con la URL base del servicio}"

command -v curl >/dev/null 2>&1 || {
  echo "Falta curl" >&2
  exit 2
}
command -v jq >/dev/null 2>&1 || {
  echo "Falta jq" >&2
  exit 2
}

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

post_login() {
  local name="$1"
  local username="$2"
  local password="$3"
  local request_body

  request_body="$(
    jq -cn \
      --arg username "$username" \
      --arg password "$password" \
      '{username: $username, password: $password}'
  )"

  curl -sS \
    -o "$tmpdir/$name.json" \
    -w '%{http_code}' \
    -H 'Content-Type: application/json' \
    --data "$request_body" \
    "${AUTH_BASE_URL%/}/auth/login"
}

assert_status() {
  local name="$1"
  local actual="$2"
  local expected="$3"

  if [[ "$actual" != "$expected" ]]; then
    echo "FALLO: $name esperaba HTTP $expected y recibió HTTP $actual" >&2
    return 1
  fi
}

assert_no_token() {
  local name="$1"
  local response_file="$2"

  # Falla si el cuerpo contiene una propiedad JSON llamada "token".
  if grep -Eiq '"token"[[:space:]]*:' "$response_file"; then
    echo "FALLO: $name devolvió una propiedad token" >&2
    return 1
  fi
}

status="$(post_login valid student 'Workshop123!')"
assert_status "login válido" "$status" "200"

jq -e '
  type == "object"
  and .user.username == "student"
  and (.token | type == "string" and length > 0)
' "$tmpdir/valid.json" >/dev/null

echo "OK: login válido devuelve user.username y token"

status="$(post_login injection-username "' OR '1'='1" 'Workshop123!')"
assert_status "inyección en username" "$status" "401"
assert_no_token "inyección en username" "$tmpdir/injection-username.json"
echo "OK: inyección en username rechazada sin token"

status="$(post_login injection-password student "' OR '1'='1")"
assert_status "inyección en password" "$status" "401"
assert_no_token "inyección en password" "$tmpdir/injection-password.json"
echo "OK: inyección en password rechazada sin token"

status="$(post_login invalid-password student 'invalid-password')"
assert_status "contraseña incorrecta" "$status" "401"
assert_no_token "contraseña incorrecta" "$tmpdir/invalid-password.json"
echo "OK: contraseña incorrecta rechazada sin token"
```

**Ejecución:** arrancar el servicio con la configuración de pruebas documentada en el proyecto y establecer la URL base correspondiente. Por ejemplo, sustituir `8080` por el puerto real:

```bash
AUTH_BASE_URL='http://localhost:8080' bash tests/auth-login-contract.sh
```

**Salida esperada del script:** líneas `OK` para los cuatro casos. Esa salida no se ha producido en este informe.

## Comandos reproducibles para inspección y suite

Desde la raíz del repositorio, inspeccionar el archivo indicado en el hallazgo:

```bash
sed -n '1,180p' auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java
grep -nE 'PreparedStatement|prepareStatement|setString|executeQuery' \
  auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java
```

La búsqueda de concatenaciones siguiente es solo una ayuda de revisión; no sustituye la inspección de la consulta completa:

```bash
grep -nE 'username[[:space:]]*\+|password[[:space:]]*\+' \
  auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java
```

Ejecutar la clase de prueba del recurso, si el proyecto usa el patrón Maven habitual para seleccionarla:

```bash
cd auth
mvn -Dtest=AuthResourceTest test
```

Ejecutar la suite requerida desde `auth/`:

```bash
cd auth
mvn test
```

Comprobar el formato del diff desde la raíz, si los cambios están en un checkout Git:

```bash
git diff --check
```

**Resultado de todos estos comandos en este informe:** **NO EJECUTADO**.

## Revisión documental de la corrección propuesta

El fragmento propuesto de `LegacyDatabase` utiliza una constante SQL con marcadores `?`, llama a `prepareStatement` y asigna `username` y `password` mediante `setString`. **En ese fragmento**, los valores no se concatenan en el SQL y el enfoque satisface estructuralmente el criterio de parametrización.

Esto no confirma la corrección en el repositorio ni su compatibilidad con el sistema existente:

- El fragmento declara el paquete `pe.bcp.login.service`, mientras que el hallazgo identifica una ruta bajo `com/bcp/workshop/auth`.
- El SQL propuesto presupone una tabla `users` y columnas `username` y `password`. El esquema real no fue proporcionado.
- No se proporcionó `AuthResource`, la implementación del caso de uso ni `AuthResourceTest`; por tanto, no se puede confirmar el `401`, la ausencia de token o la preservación del contrato HTTP en la aplicación.
- No se proporcionó una salida de `mvn test` ni de las pruebas HTTP.

Antes de considerar satisfecha la corrección, cotejar el fragmento con las interfaces y el esquema existentes, integrar la consulta en el archivo real, añadir las pruebas de `AuthResourceTest` y ejecutar los comandos anteriores.

## Limpieza

- El script elimina automáticamente el directorio temporal al finalizar.
- No requiere modificar datos de la base de datos.
- Usar únicamente una base de datos aislada de pruebas. Si el entorno de pruebas persiste sesiones u otros datos auxiliares, limpiarlos mediante el mecanismo de teardown existente en el proyecto.
- No se ha ejecutado ninguna prueba ni se ha realizado limpieza de un entorno real.