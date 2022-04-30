package org.usth.ict.query;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class DebugResourceTest {

    @Test
    public void testHealthEndpoint() {
        given()
          .when().get("/api/health")
          .then()
             .statusCode(200)
             .body(is("OK"));
    }

}
