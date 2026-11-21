package app.swapr.e2e;

/** The services the suite knows about, each with the setting that holds its QA base URL. */
public enum Service {

  USER_SERVICE("user-service", "USER_SERVICE_URL");

  private final String displayName;
  private final String urlSetting;

  Service(String displayName, String urlSetting) {
    this.displayName = displayName;
    this.urlSetting = urlSetting;
  }

  String displayName() {
    return displayName;
  }

  String urlSetting() {
    return urlSetting;
  }

  String baseUrl() {
    return Config.stripTrailingSlash(Config.require(urlSetting));
  }
}
