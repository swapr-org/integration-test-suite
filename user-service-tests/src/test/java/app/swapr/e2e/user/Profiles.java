package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import java.util.List;
import java.util.Map;

final class Profiles {

  static final String ZURICH_QUERY = "zur";
  static final String ZURICH_NAME = "Zürich";
  static final String ZURICH_COUNTRY = "CH";
  static final int RADIUS_KM = 40;

  private Profiles() {
  }

  static Map<String, Object> zurich(TestUser user) {
    List<Map<String, Object>> cities = given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .queryParam("query", ZURICH_QUERY)
        .when()
        .get("/v1/cities")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getList("$");
    return cities.stream()
        .filter(city -> ZURICH_NAME.equals(city.get("name")) && ZURICH_COUNTRY.equals(city.get("countryCode")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("City search '" + ZURICH_QUERY + "' did not return "
            + ZURICH_NAME + " (" + ZURICH_COUNTRY + "): " + cities));
  }

  static Map<String, Object> setUp(TestUser user, String displayName) {
    Map<String, Object> city = zurich(user);
    given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .body(body(displayName, city.get("id"), RADIUS_KM))
        .when()
        .put("/v1/users/me")
        .then()
        .statusCode(200);
    return city;
  }

  static Map<String, Object> body(String displayName, Object cityId, Object radiusKm) {
    return Map.of("displayName", displayName, "cityId", cityId, "radiusKm", radiusKm);
  }
}
