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
