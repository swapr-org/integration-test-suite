package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import app.swapr.e2e.TestUsers;
import io.restassured.path.json.JsonPath;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MyProfileTest {

  private static final Set<String> OWN_PROFILE_FIELDS = Set.of("id", "displayName", "radiusKm", "city");
  private static final Set<String> CITY_FIELDS = Set.of("id", "name", "region", "countryCode");

  @Test
  void firstUseCreatesProfileWithDefaultRadius() {
    TestUser user = TestUsers.create();

    JsonPath profile = given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .when()
        .get("/v1/users/me")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath();

    assertEquals(user.id().toString(), profile.getString("id"));
    assertEquals(25, profile.getInt("radiusKm"));
    assertNull(profile.get("displayName"));
    assertNull(profile.get("city"));
  }

  @Test
  void userCanSetNameCityAndRadiusAndStoresOnlyTheCity() {
    TestUser user = TestUsers.create();
    Map<String, Object> zurich = Profiles.zurich(user);

    JsonPath saved = given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .body(Profiles.body("  Ben  ", zurich.get("id"), Profiles.RADIUS_KM))
        .when()
        .put("/v1/users/me")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath();
    assertProfile(user, zurich, saved);

    JsonPath read = given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .when()
        .get("/v1/users/me")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath();
    assertProfile(user, zurich, read);
  }

  private static void assertProfile(TestUser user, Map<String, Object> zurich, JsonPath profile) {
    Map<String, Object> root = profile.getMap("$");
    assertEquals(OWN_PROFILE_FIELDS, root.keySet());
    assertEquals(user.id().toString(), root.get("id"));
    assertEquals("Ben", root.get("displayName"));
    assertEquals(Profiles.RADIUS_KM, root.get("radiusKm"));
    Map<String, Object> city = profile.getMap("city");
    assertEquals(CITY_FIELDS, city.keySet());
    assertEquals(zurich, city);
  }
}
