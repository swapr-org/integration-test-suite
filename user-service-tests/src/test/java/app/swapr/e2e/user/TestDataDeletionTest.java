package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import app.swapr.e2e.TestUsers;
import org.junit.jupiter.api.Test;

class TestDataDeletionTest {

  private static final String DELETION_PATH = "/internal/v1/test-data/users/{id}";

  @Test
  void testDataDeletionRemovesProfileAndCanBeRepeated() {
    TestUser user = TestUsers.create();
    Profiles.setUp(user, "Ben");

    deleteTestData(user);

    given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .when()
        .get("/v1/users/me")
        .then()
        .statusCode(200)
        .body("displayName", nullValue())
        .body("city", nullValue())
        .body("radiusKm", equalTo(25));

    deleteTestData(user);
  }

  @Test
  void testDataDeletionNeedsInternalTokenAndRefusesUserJwt() {
    TestUser user = TestUsers.create();

    given()
        .spec(Requests.spec(Service.USER_SERVICE))
        .when()
        .delete(DELETION_PATH, user.id())
        .then()
        .statusCode(401);

    given()
        .spec(Requests.internal(Service.USER_SERVICE))
        .header("Authorization", "Bearer " + user.accessToken())
        .when()
        .delete(DELETION_PATH, user.id())
        .then()
        .statusCode(403);
  }

  private static void deleteTestData(TestUser user) {
    given()
        .spec(Requests.internal(Service.USER_SERVICE))
        .when()
        .delete(DELETION_PATH, user.id())
        .then()
        .statusCode(204);
  }
}
