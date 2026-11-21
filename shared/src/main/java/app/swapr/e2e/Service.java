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

  /** Returns the service's name, as used in messages. */
  public String displayName() {
    return displayName;
  }

  /** Returns the name of the setting that holds the service's base URL. */
  public String urlSetting() {
    return urlSetting;
  }

  /** Returns the service's base URL without a trailing slash, failing if its setting is missing. */
  public String baseUrl() {
    return Config.stripTrailingSlash(Config.require(urlSetting));
  }
}
