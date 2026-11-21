package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import org.junit.jupiter.api.Test;

class HealthTest {

  @Test
  void userServiceReportsHealthyInQa() {
    given()
        .spec(Requests.spec(Service.USER_SERVICE))
        .when()
        .get("/actuator/health")
        .then()
        .statusCode(200)
        .body("status", equalTo("UP"));
  }
}
