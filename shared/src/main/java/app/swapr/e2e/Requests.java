package app.swapr.e2e;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/** Builds RestAssured request specifications for known services. */
public final class Requests {

  private Requests() {
  }

  /** Returns an anonymous request specification for the service's base URL. */
  public static RequestSpecification spec(Service service) {
    return builder(service).build();
  }

  /** Returns a request specification for the service's base URL, acting as the given test user. */
  public static RequestSpecification spec(Service service, TestUser user) {
    return builder(service)
        .addHeader("Authorization", "Bearer " + user.accessToken())
        .build();
  }

  /** Returns a request specification for the service's base URL with the internal token and no user JWT. */
  public static RequestSpecification internal(Service service) {
    return builder(service)
        .addHeader("X-Internal-Token", Config.require(Config.INTERNAL_TOKEN))
        .build();
  }

  private static RequestSpecBuilder builder(Service service) {
    return new RequestSpecBuilder()
        .setBaseUri(service.baseUrl())
        .setAccept(ContentType.JSON)
        .setContentType(ContentType.JSON);
  }
}
