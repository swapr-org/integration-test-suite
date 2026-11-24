package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import app.swapr.e2e.TestUsers;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileAuthTest {

  @Test
  void profileAndCityEndpointsRejectRequestsWithoutSignIn() {
    given()
        .spec(Requests.spec(Service.USER_SERVICE))
        .when()
        .get("/v1/users/me")
        .then()
        .statusCode(401);

    given()
        .spec(Requests.spec(Service.USER_SERVICE))
        .body(Profiles.body("Ben", UUID.randomUUID(), Profiles.RADIUS_KM))
        .when()
        .put("/v1/users/me")
        .then()
        .statusCode(401);

    given()
        .spec(Requests.spec(Service.USER_SERVICE))
        .when()
        .delete("/v1/users/me")
        .then()
        .statusCode(401);

    given()
        .spec(Requests.spec(Service.USER_SERVICE))
        .when()
        .get("/v1/users/{id}", UUID.randomUUID())
        .then()
        .statusCode(401);

    given()
        .spec(Requests.spec(Service.USER_SERVICE))
        .queryParam("query", Profiles.ZURICH_QUERY)
        .when()
        .get("/v1/cities")
        .then()
        .statusCode(401);
  }

  @Test
  void noUserCanChangeAnotherUsersProfile() {
    TestUser owner = TestUsers.create();
    TestUser other = TestUsers.create();
    Map<String, Object> zurich = Profiles.setUp(owner, "Ben");

    given()
        .spec(Requests.spec(Service.USER_SERVICE, other))
        .body(Profiles.body("Mallory", zurich.get("id"), 10))
        .when()
        .put("/v1/users/{id}", owner.id())
        .then()
        .statusCode(anyOf(lessThan(200), greaterThanOrEqualTo(300)));

    given()
        .spec(Requests.spec(Service.USER_SERVICE, owner))
        .when()
        .get("/v1/users/me")
        .then()
        .statusCode(200)
        .body("displayName", equalTo("Ben"))
        .body("radiusKm", equalTo(Profiles.RADIUS_KM));
  }
}
