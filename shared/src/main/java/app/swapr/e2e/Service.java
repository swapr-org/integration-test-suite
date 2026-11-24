package app.swapr.e2e;

import java.util.Optional;

/** The services the suite knows about, each with its QA base URL setting and optional test-data deletion path. */
public enum Service {

  USER_SERVICE("user-service", "USER_SERVICE_URL", "/internal/v1/test-data/users/{id}"),
  LISTING_SERVICE("listing-service", "LISTING_SERVICE_URL", null);

  private final String displayName;
  private final String urlSetting;
  private final String testDataDeletionPath;

  Service(String displayName, String urlSetting, String testDataDeletionPath) {
    this.displayName = displayName;
    this.urlSetting = urlSetting;
    this.testDataDeletionPath = testDataDeletionPath;
  }

  String displayName() {
    return displayName;
  }

  String urlSetting() {
    return urlSetting;
  }

  Optional<String> testDataDeletionPath() {
    return Optional.ofNullable(testDataDeletionPath);
  }

  String baseUrl() {
    return Config.stripTrailingSlash(Config.require(urlSetting));
  }
}
