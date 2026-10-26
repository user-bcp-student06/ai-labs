# Pruebas del componente auth

## Alcance y resultados

La suite cubre los endpoints documentados para `auth`: `GET /q/health` y
`POST /auth/login`. Las ejecuciones se relacionan con el componente `auth` y
con el hallazgo `H-AUTH-01` cuando comprueban el flujo de login y su consulta
parametrizada.

| Endpoint | Caso | Tipo | Resultado esperado |
|---|---|---|---|
| `GET /q/health` | Responde con estado `UP` | Camino feliz | HTTP 200 y `status` igual a `UP` |
| `GET /q/health` | No hay checks configurados | Borde | HTTP 200 y `checks` vacío |
| `GET /q/health` | Estado `DOWN` o fallo de salud | Pendiente; no hay error documentado ni check configurable en el componente | No se afirma código ni cuerpo esperado |
| `POST /auth/login` | Credenciales de laboratorio válidas con `client` | Camino feliz | HTTP 200 y cuerpo con token, expiración y datos del usuario |
| `POST /auth/login` | Credenciales válidas sin `client` opcional | Borde | HTTP 200 |
| `POST /auth/login` | Request JSON `null` | Error 400 | `{"message":"Request is null"}` |
| `POST /auth/login` | Falta `username` | Error 400 | `{"message":"Username is required"}` |
| `POST /auth/login` | `username` es `null` | Error 400 | `{"message":"Username is required"}` |
| `POST /auth/login` | `username` vacío | Error 400 | `{"message":"Username is required"}` |
| `POST /auth/login` | `username` contiene solo espacios | Error 400 | `{"message":"Username is required"}` |
| `POST /auth/login` | Falta `password` | Error 400 | `{"message":"Password is required"}` |
| `POST /auth/login` | `password` es `null` | Error 400 | `{"message":"Password is required"}` |
| `POST /auth/login` | `password` vacío | Error 400 | `{"message":"Password is required"}` |
| `POST /auth/login` | `password` contiene solo espacios | Error 400 | `{"message":"Password is required"}` |
| `POST /auth/login` | JSON malformado | Error 400 | Cuerpo vacío, según la respuesta observada del framework |
| `POST /auth/login` | Credenciales desconocidas | Error 401 | `{"message":"Invalid user unknown-user or password"}` |
| `POST /auth/login` | Username contiene comilla | Borde de entrada / error 401 | HTTP 401; la entrada no autentica al usuario |
| `POST /auth/login` | Se usa `GET` en vez de `POST` | Error 405 | Cuerpo vacío, según la respuesta observada del framework |
| `POST /auth/login` | `Accept: text/plain` | Error 406 | Cuerpo vacío, según la respuesta observada del framework |
| `POST /auth/login` | `Content-Type: text/plain` | Error 415 | Cuerpo vacío, según la respuesta observada del framework |
| `POST /auth/login` | Excepción inesperada lanzada por el repositorio de prueba | Error 500, comprobación de recurso | HTTP 500 y entidad con `message: "Unknown error"` y la clase de la excepción |

### Ejecución real

- Componente: `auth`.
- Hallazgo relacionado: `H-AUTH-01` (consulta de credenciales parametrizada).
- Comando ejecutado: `mvn verify`.
- Directorio de trabajo: `auth/`.
- Código de salida: `0`.
- Resultado: BUILD SUCCESS; 20 tests, 0 fallos, 0 errores, 0 omitidos.
- Cobertura: JaCoCo `0.8.12`, ejecutado en la fase `verify` por
  `jacoco-maven-plugin`; medición por líneas (`covered / (covered + missed)`).
  Reportes generados en `auth/target/site/jacoco/index.html` y
  `auth/target/site/jacoco/jacoco.xml`.

| Clase de producción | Líneas cubiertas | Cobertura de líneas |
|---|---:|---:|
| `AuthResource` | 25 / 25 | 100,0% |
| `LegacyDatabase` | 20 / 23 | 87,0% |
| Bundle completo de auth (5 clases) | 50 / 53 | 94,3% |

El umbral de líneas de 85% se cumple en las dos clases de producción
modificadas. La cobertura de ramas de `LegacyDatabase` es 62,5% (5 de 8); no
se presenta como cobertura de líneas ni como si alcanzara el 85%.

El test del error 500 invoca `AuthResource.login` directamente con una
implementación de `LegacyDatabase` de prueba que lanza `NullPointerException`;
no es una petición HTTP end-to-end. Los errores 400, 401, 405, 406 y 415 sí
se comprueban enviando solicitudes al endpoint.

## Fallos

No hubo fallos en la ejecución indicada.

## Pendientes y límites

- No se probó una respuesta `DOWN`/503 de health. El endpoint no declara checks
  propios y `docs/ENDPOINTS.md` deja abierto el contrato en ese estado.
- `doesNotAuthenticateQuoteContainingUsername` comprueba el resultado 401
  para un username con comilla, pero por sí solo no distingue una consulta
  parametrizada de una concatenada que falle y termine en el mismo 401. La
  evidencia de la parametrización debe revisarse en la implementación y en
  una prueba que inspeccione el enlace de parámetros si se necesita una
  garantía de prueba independiente.
- El test de excepción inesperada comprueba la conversión de la respuesta en
  el recurso, pero no fuerza una excepción a través del servidor HTTP.
- No se ejecutaron CodeQL ni Dependency Review remotos en esta validación local.

## Código de los tests ejecutados

Fuente canónica: [`AuthResourceTest.java`](../auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java).
Este bloque reproduce la clase ejecutada por `mvn verify`.

```java
package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuthResourceTest {
    private static final String LOGIN_PATH = "/auth/login";
    private static final String HEALTH_PATH = "/q/health";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String STUDENT_USERNAME = "student";
    private static final String STUDENT_PASSWORD = "Workshop123!";
    private static final String INVALID_PASSWORD = "incorrect-password";

    @Test
    void healthEndpointReportsUp() {
        given()
                .when().get(HEALTH_PATH)
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void healthEndpointReportsNoChecksWhenNoneAreConfigured() {
        given()
                .when().get(HEALTH_PATH)
                .then()
                .statusCode(200)
                .body("checks", hasSize(0));
    }

    @Test
    void logsInWithWorkshopUser() {
        postLogin(Map.of(
                "username", STUDENT_USERNAME,
                "password", STUDENT_PASSWORD,
                "client", "angular-workshop"
        ))
                .statusCode(200)
                .body("token", notNullValue())
                .body("expiresIn", is(3600))
                .body("user.username", is(STUDENT_USERNAME))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("debug", is("authenticated-with-legacy-flow"));
    }

    @Test
    void logsInWithoutOptionalClientField() {
        postLogin(Map.of("username", STUDENT_USERNAME, "password", STUDENT_PASSWORD))
                .statusCode(200);
    }

    @Test
    void rejectsNullRequest() {
        postLogin("null")
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void rejectsMissingUsername() {
        postLogin(Map.of("password", STUDENT_PASSWORD))
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsNullUsername() {
        postLogin("{\"username\":null,\"password\":\"Workshop123!\"}")
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsEmptyUsername() {
        postLogin(Map.of("username", "", "password", STUDENT_PASSWORD))
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsWhitespaceUsername() {
        postLogin(Map.of("username", "   ", "password", STUDENT_PASSWORD))
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsMissingPassword() {
        postLogin(Map.of("username", STUDENT_USERNAME))
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsNullPassword() {
        postLogin("{\"username\":\"student\",\"password\":null}")
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsEmptyPassword() {
        postLogin(Map.of("username", STUDENT_USERNAME, "password", ""))
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsWhitespacePassword() {
        postLogin(Map.of("username", STUDENT_USERNAME, "password", "   "))
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsMalformedJsonWithEmptyFrameworkBody() {
        String responseBody = postLogin("{")
                .statusCode(400)
                .extract()
                .asString();

        assertTrue(responseBody.isEmpty());
    }

    @Test
    void rejectsUnknownCredentials() {
        postLogin(Map.of("username", "unknown-user", "password", INVALID_PASSWORD))
                .statusCode(401)
                .body("message", is("Invalid user unknown-user or password"));
    }

    @Test
    void doesNotAuthenticateQuoteContainingUsername() {
        postLogin(Map.of("username", "student'", "password", INVALID_PASSWORD))
                .statusCode(401);
    }

    @Test
    void rejectsWrongHttpMethodWithEmptyFrameworkBody() {
        String responseBody = given()
                .when().get(LOGIN_PATH)
                .then()
                .statusCode(405)
                .extract()
                .asString();

        assertTrue(responseBody.isEmpty());
    }

    @Test
    void rejectsUnacceptableResponseTypeWithEmptyFrameworkBody() {
        String responseBody = given()
                .accept("text/plain")
                .contentType(JSON_CONTENT_TYPE)
                .body(Map.of("username", STUDENT_USERNAME, "password", STUDENT_PASSWORD))
                .when().post(LOGIN_PATH)
                .then()
                .statusCode(406)
                .extract()
                .asString();

        assertTrue(responseBody.isEmpty());
    }

    @Test
    void rejectsUnsupportedRequestTypeWithEmptyFrameworkBody() {
        String responseBody = given()
                .contentType("text/plain")
                .body("{}")
                .when().post(LOGIN_PATH)
                .then()
                .statusCode(415)
                .extract()
                .asString();

        assertTrue(responseBody.isEmpty());
    }

    @Test
    void mapsUnexpectedExceptionToDocumentedServerError() {
        LoginRequest request = new LoginRequest();
        request.username = STUDENT_USERNAME;
        request.password = STUDENT_PASSWORD;

        AuthResource resource = new AuthResource();
        resource.database = new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                throw new NullPointerException();
            }
        };
        Response response = resource.login(request);

        assertEquals(500, response.getStatus());
        assertEquals(Map.of(
                "message", "Unknown error",
                "exception", NullPointerException.class.getName()
        ), response.getEntity());
    }

    private ValidatableResponse postLogin(Object requestBody) {
        return given()
                .contentType(JSON_CONTENT_TYPE)
                .body(requestBody)
                .when()
                .post(LOGIN_PATH)
                .then();
    }
}
```
