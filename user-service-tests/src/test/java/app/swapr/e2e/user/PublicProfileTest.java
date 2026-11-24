package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import app.swapr.e2e.TestUsers;
import io.restassured.path.json.JsonPath;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicProfileTest {

  @Test
  void anySignedInUserSeesOnlyAnotherUsersNameAndCity() {
    TestUser owner = TestUsers.create();
    TestUser viewer = TestUsers.create();
    final Map<String, Object> zurich = Profiles.setUp(owner, "Ben");

    JsonPath profile = given()
        .spec(Requests.spec(Service.USER_SERVICE, viewer))
        .when()
        .get("/v1/users/{id}", owner.id())
        .then()
        .statusCode(200)
        .extract()
        .jsonPath();

    Map<String, Object> root = profile.getMap("$");
    assertEquals(Set.of("id", "displayName", "city"), root.keySet());
    assertEquals(owner.id().toString(), root.get("id"));
    assertEquals("Ben", root.get("displayName"));
    assertEquals(
        Map.of("name", zurich.get("name"), "region", zurich.get("region"), "countryCode", zurich.get("countryCode")),
        profile.getMap("city"));
  }

  @Test
  void publicProfileIsNotFoundUntilNameAndCityAreSet() {
    TestUser incomplete = TestUsers.create();
    TestUser viewer = TestUsers.create();
    given()
        .spec(Requests.spec(Service.USER_SERVICE, incomplete))
        .when()
        .get("/v1/users/me")
        .then()
        .statusCode(200);

    for (UUID id : new UUID[] {incomplete.id(), UUID.randomUUID()}) {
      given()
          .spec(Requests.spec(Service.USER_SERVICE, viewer))
          .when()
          .get("/v1/users/{id}", id)
          .then()
          .statusCode(404)
          .body("status", equalTo(404))
          .body("detail", equalTo("Profile not found."));
    }
  }

  @Test
  void displayNamesDoNotHaveToBeUnique() {
    Profiles.setUp(TestUsers.create(), "Ben");
    Profiles.setUp(TestUsers.create(), "Ben");
  }
}
