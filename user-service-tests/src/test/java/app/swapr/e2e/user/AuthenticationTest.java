package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import app.swapr.e2e.TestUsers;
import org.junit.jupiter.api.Test;

class AuthenticationTest {

  private static final String UNHANDLED_PATH = "/v1/e2e-auth-probe";

  @Test
  void userServiceAcceptsFreshTestUsersJwt() {
    TestUser user = TestUsers.create();

    given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .when()
        .get(UNHANDLED_PATH)
        .then()
        .statusCode(404);

    given()
        .spec(Requests.spec(Service.USER_SERVICE))
        .when()
        .get(UNHANDLED_PATH)
        .then()
        .statusCode(401);
  }
}
