package app.swapr.e2e.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import app.swapr.e2e.Requests;
import app.swapr.e2e.Service;
import app.swapr.e2e.TestUser;
import app.swapr.e2e.TestUsers;
import io.restassured.response.ValidatableResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileValidationTest {

  private static final Object MISSING = new Object();

  private static TestUser user;
  private static Object zurichId;

  @BeforeAll
  static void createUser() {
    user = TestUsers.create();
    zurichId = Profiles.zurich(user).get("id");
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        Arguments.of("blank displayName", "displayName", "   "),
        Arguments.of("51-character displayName", "displayName", "a".repeat(51)),
        Arguments.of("radiusKm 0", "radiusKm", 0),
        Arguments.of("radiusKm 101", "radiusKm", 101),
        Arguments.of("radiusKm 2.5", "radiusKm", 2.5),
        Arguments.of("missing cityId", "cityId", MISSING),
        Arguments.of("unknown cityId", "cityId", UUID.randomUUID()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidFields")
  void invalidProfileIsRejectedNamingTheField(String description, String field, Object value) {
    Map<String, Object> body = new HashMap<>(Profiles.body("Ben", zurichId, Profiles.RADIUS_KM));
    if (value == MISSING) {
      body.remove(field);
    } else {
      body.put(field, value);
    }

    ValidatableResponse response = given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .body(body)
        .when()
        .put("/v1/users/me")
        .then();
    assertValidationProblem(response, field);
  }

  @Test
  void tooShortCityQueryIsRejectedNamingTheField() {
    ValidatableResponse response = given()
        .spec(Requests.spec(Service.USER_SERVICE, user))
        .queryParam("query", "z")
        .when()
        .get("/v1/cities")
        .then();
    assertValidationProblem(response, "query");
  }

  private static void assertValidationProblem(ValidatableResponse response, String field) {
    response
        .statusCode(400)
        .body("type", anyOf(nullValue(), equalTo("about:blank")))
        .body("title", equalTo("Bad Request"))
        .body("status", equalTo(400))
        .body("detail", equalTo("The request has invalid fields."))
        .body("errors.field", contains(field));
  }
}
