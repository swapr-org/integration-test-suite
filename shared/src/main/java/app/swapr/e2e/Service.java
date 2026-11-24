package app.swapr.e2e;

import java.util.Optional;

/** The services the suite knows about, each with its QA base URL setting and optional user data deletion path. */
public enum Service {

  USER_SERVICE("user-service", "USER_SERVICE_URL", "/v1/users/me"),
  LISTING_SERVICE("listing-service", "LISTING_SERVICE_URL", null),
  CHAT_SERVICE("chat-service", "CHAT_SERVICE_URL", null);
  FEED_SERVICE("feed-service", "FEED_SERVICE_URL", null);

  private final String displayName;
  private final String urlSetting;
  private final String userDataDeletionPath;

  Service(String displayName, String urlSetting, String userDataDeletionPath) {
    this.displayName = displayName;
    this.urlSetting = urlSetting;
    this.userDataDeletionPath = userDataDeletionPath;
  }

  String displayName() {
    return displayName;
  }

  String urlSetting() {
    return urlSetting;
  }

  Optional<String> userDataDeletionPath() {
    return Optional.ofNullable(userDataDeletionPath);
  }

  String baseUrl() {
    return Config.stripTrailingSlash(Config.require(urlSetting));
  }
}
