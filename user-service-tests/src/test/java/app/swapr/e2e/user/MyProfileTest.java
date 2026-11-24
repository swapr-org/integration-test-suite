package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import app.swapr.e2e.TestUsers;
import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

  @Test
  void concurrentFirstUseCreatesOneProfile() throws InterruptedException, ExecutionException {
    TestUser user = TestUsers.create();
    CountDownLatch start = new CountDownLatch(1);
    List<Future<String>> ids;
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      ids = List.of(executor.submit(() -> fetchOwnId(user, start)), executor.submit(() -> fetchOwnId(user, start)));
      start.countDown();
    }

    assertEquals(user.id().toString(), ids.get(0).get());
    assertEquals(user.id().toString(), ids.get(1).get());
  }

  private static String fetchOwnId(TestUser user, CountDownLatch start) throws InterruptedException {
    start.await();
    return given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .when()
        .get("/v1/users/me")
        .then()
        .statusCode(200)
        .extract()
        .path("id");
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
