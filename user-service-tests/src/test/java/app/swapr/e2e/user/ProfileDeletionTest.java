package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import app.swapr.e2e.TestUsers;
import org.junit.jupiter.api.Test;

class ProfileDeletionTest {

  @Test
  void deletingMyProfileRemovesItAndCanBeRepeated() {
    TestUser user = TestUsers.create();
    TestUser other = TestUsers.create();
    Profiles.setUp(user, "Ben");
    Profiles.setUp(other, "Alex");

    deleteMe(user);

    given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .when()
        .get("/v1/users/me")
        .then()
        .statusCode(200)
        .body("displayName", nullValue())
        .body("city", nullValue())
        .body("radiusKm", equalTo(25));

    deleteMe(user);

    given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .when()
        .get("/v1/users/{id}", other.id())
        .then()
        .statusCode(200)
        .body("displayName", equalTo("Alex"));
  }

  private static void deleteMe(TestUser user) {
    given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .when()
        .delete("/v1/users/me")
        .then()
        .statusCode(204);
  }
}
