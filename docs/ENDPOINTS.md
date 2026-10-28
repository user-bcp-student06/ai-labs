# Contratos de endpoints

## Componente auth

### Estado del contrato

- **Contrato observado:** los campos, estados y cuerpos descritos abajo proceden de la implementación actual de `auth/`; los casos de salud y login indicados se comprobaron en localhost.
- **Contrato acordado:** no hay un contrato acordado independiente en este repositorio. `docs/ENDPOINTS.md` no existía antes de esta documentación.
- **Riesgos y preguntas pendientes:** el token actual no está firmado; la marca de expiración se incluye en su contenido, pero el código de auth no verifica tokens. El recurso expone detalles de excepciones en respuestas 500. No hay checks de salud propios en `auth/`, así que no se pudo observar ni fijar el cuerpo de una respuesta de salud `DOWN`.

La URL base local observada es `http://127.0.0.1:8080`; puede variar por entorno.

## `GET /q/health`

### 1. Qué hace

Permite a un operador o a la plataforma de despliegue comprobar que el servicio de autenticación está respondiendo y conocer su estado de salud.

### 2. Autenticación

No requiere autenticación. No se requiere encabezado `Authorization`.

### 3. Request

Este endpoint no define parámetros ni cuerpo de solicitud.

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| — | — | No | No hay parámetros ni cuerpo definidos. |

Ejemplo de llamada (sin cuerpo JSON):

```http
GET /q/health HTTP/1.1
Host: 127.0.0.1:8080
```

### 4. Response exitosa

**Estado:** `200 OK` cuando el estado reportado es `UP`.

| Campo | Tipo | Descripción |
|---|---|---|
| `status` | string | Estado agregado de salud; el valor observado fue `UP`. |
| `checks` | array | Checks de salud reportados. El array observado está vacío; no hay checks propios declarados en `auth/`. |

Ejemplo observado:

```json
{
  "status": "UP",
  "checks": []
}
```

La ruta la proporciona la dependencia `quarkus-smallrye-health` declarada en `auth/pom.xml:40`; `README.md` anuncia `http://localhost:8080/q/health`.

### 5. Respuestas de error

No se encontró una respuesta de error definida por código de `auth/` para este endpoint. La petición local respondió `200`. **Pregunta abierta:** confirmar si el contrato de despliegue debe documentar una respuesta del framework cuando el estado agregado no sea `UP`; este componente no declara checks que permitan comprobar ese caso ni su cuerpo.

### 6. Notas de seguridad

El endpoint no requiere autenticación y revela el estado del servicio. El código actual no comprueba la salud de SQLite: el resultado observado fue `checks: []`, así que un `UP` no certifica por sí mismo la disponibilidad de la base de datos.

## `POST /auth/login`

### 1. Qué hace

Valida las credenciales de una persona usuaria y, si coinciden con un registro local, devuelve sus datos y un token del flujo de autenticación heredado.

### 2. Autenticación

No requiere un token previo ni encabezado `Authorization`; las credenciales se envían en el cuerpo. El encabezado requerido para el cuerpo es `Content-Type: application/json`. La respuesta se produce como JSON.

### 3. Request

El cuerpo es un objeto JSON de tipo `LoginRequest`.

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `username` | string | Sí | Nombre de usuario. Se rechaza si falta, es `null` o queda vacío tras `trim()`. |
| `password` | string | Sí | Contraseña. Se rechaza si falta, es `null` o queda vacía tras `trim()`. |
| `client` | string | No | Campo del DTO; el recurso actual no lo valida ni lo usa para autenticar. |

Ejemplo completo con la cuenta ficticia de laboratorio documentada en `README.md`:

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

La contraseña del ejemplo es un valor ficticio del laboratorio, no una credencial de producción.

### 4. Response exitosa

**Estado:** `200 OK` cuando las credenciales coinciden.

| Campo | Tipo | Descripción |
|---|---|---|
| `token` | string | Token generado dinámicamente por el flujo heredado. |
| `expiresIn` | integer | Duración declarada de `3600` segundos. |
| `user` | object | Datos de la persona autenticada. |
| `user.username` | string | Nombre de usuario del registro encontrado. |
| `user.displayName` | string | Nombre para mostrar; se escapa con `escapeHtml4`. |
| `user.role` | string | Rol del registro. |
| `debug` | string | Marcador fijo `authenticated-with-legacy-flow`. |

Ejemplo completo de la forma de la respuesta. El valor de `token` es un marcador de posición, no un token reutilizable; el token real cambia en cada autenticación:

```json
{
  "token": "<token generado dinámicamente; valor omitido>",
  "expiresIn": 3600,
  "user": {
    "username": "student",
    "displayName": "Workshop Student",
    "role": "STUDENT"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

### 5. Respuestas de error

| Estado | Cuándo ocurre | Ejemplo de cuerpo |
|---|---|---|
| `400 Bad Request` | El recurso recibe `null` como request. | `{"message":"Request is null"}` |
| `400 Bad Request` | `username` falta, es `null` o está vacío/solo contiene espacios. | `{"message":"Username is required"}` |
| `400 Bad Request` | `password` falta, es `null` o está vacío/solo contiene espacios. | `{"message":"Password is required"}` |
| `400 Bad Request` | El JSON no se puede deserializar. Respuesta observada de Quarkus, con cuerpo vacío. | *(cuerpo vacío)* |
| `401 Unauthorized` | No se encuentra una coincidencia de usuario y contraseña; también es el resultado del repositorio cuando captura un error de consulta. | `{"message":"Invalid user unknown-user or password"}` |
| `405 Method Not Allowed` | Se usa otro método HTTP en `/auth/login`. Respuesta observada de Quarkus, con cuerpo vacío. | *(cuerpo vacío)* |
| `406 Not Acceptable` | El cliente solicita un tipo de respuesta no producido por el recurso. Respuesta observada de Quarkus, con cuerpo vacío. | *(cuerpo vacío)* |
| `415 Unsupported Media Type` | El cuerpo se envía con un tipo distinto de `application/json`. Respuesta observada de Quarkus, con cuerpo vacío. | *(cuerpo vacío)* |
| `500 Internal Server Error` | Una excepción llega al `catch` del recurso. El mensaje y la clase varían según la excepción; el ejemplo muestra la forma del objeto, no valores constantes. | `{"message":"<mensaje de la excepción o Unknown error>","exception":"<nombre completo de la clase>"}` |

### 6. Notas de seguridad

- La búsqueda de credenciales usa `PreparedStatement` y enlaza username y password como parámetros.
- Las contraseñas de los registros sembrados se almacenan en texto plano en SQLite; este endpoint compara la contraseña recibida directamente con ese valor.
- El token es un JWT firmado con HMAC-SHA256 (`HS256`). Incluye `username`, `role`, `iat` y `exp`; la expiración se fija en 3600 segundos y se devuelve como `expiresIn: 3600`. El secreto de firma se lee de `WORKSHOP_AUTH_TOKEN_SECRET`; el servicio no arranca si falta.
- La verificación de `auth/` y de la API rechaza tokens sin firma válida, alterados, expirados o con el formato Base64 heredado.
- El error `401` refleja el username recibido en su mensaje. El error `500` puede exponer el mensaje de excepción y el nombre de su clase.
- La autenticación no depende del campo `client`.

## Evidencia de implementación

- Ruta, validación, estados y cuerpo de respuesta: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:15-57`.
- Campos del request: `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-6`.
- Consulta parametrizada, registros SQLite y captura de errores: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:17-49`.
- Emisión del token firmado: `auth/src/main/java/com/bcp/workshop/auth/TokenSigner.java:16-37`; uso en la respuesta: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:42-45`.
- Endpoint de salud: dependencia `quarkus-smallrye-health` en `auth/pom.xml:40`; URL de referencia en `README.md`.
- Verificación local de salud, respuestas de login y errores del framework: realizada contra `127.0.0.1:8080`; el token de la respuesta se omitió de la salida.
