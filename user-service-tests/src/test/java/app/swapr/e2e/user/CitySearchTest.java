package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import app.swapr.e2e.TestUsers;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CitySearchTest {

  private static TestUser user;

  @BeforeAll
  static void createUser() {
    user = TestUsers.create();
  }

  @Test
  void citySearchIgnoresCaseAndAccents() {
    List<Map<String, Object>> cities = search("ZUR");

    assertTrue(cities.stream().anyMatch(city -> Profiles.ZURICH_NAME.equals(city.get("name"))
        && Profiles.ZURICH_COUNTRY.equals(city.get("countryCode"))), "No Zürich (CH) in " + cities);
  }

  @Test
  void citySearchListsAtMostTenCitiesBiggestFirst() {
    List<Map<String, Object>> cities = search("lon");

    assertFalse(cities.isEmpty());
    assertTrue(cities.size() <= 10, "Expected at most 10 cities, got " + cities.size());
    assertEquals("London", cities.getFirst().get("name"));
    assertEquals("GB", cities.getFirst().get("countryCode"));
    for (Map<String, Object> city : cities) {
      assertEquals(Set.of("id", "name", "region", "countryCode"), city.keySet(), "Fields of " + city);
    }
  }

  private static List<Map<String, Object>> search(String query) {
    return given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .queryParam("query", query)
        .when()
        .get("/v1/cities")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getList("$");
  }
}
