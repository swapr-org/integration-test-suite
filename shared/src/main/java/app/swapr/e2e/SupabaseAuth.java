package app.swapr.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class SupabaseAuth {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

  private SupabaseAuth() {
  }

  static UUID createConfirmedUser(String email, String password) {
    String body = JSON.writeValueAsString(Map.of("email", email, "password", password, "email_confirm", true));
    HttpRequest request = adminRequest("/auth/v1/admin/users")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    JsonNode user = sendForJson(request, "create test user " + email);
    JsonNode id = user.get("id");
    if (id == null || !id.isString()) {
      throw new IllegalStateException("Supabase admin API returned no user id for " + email + ": " + user);
    }
    return UUID.fromString(id.asString());
  }

  static String signIn(String email, String password) {
    String body = JSON.writeValueAsString(Map.of("email", email, "password", password));
    HttpRequest request = HttpRequest.newBuilder(uri("/auth/v1/token?grant_type=password"))
        .timeout(REQUEST_TIMEOUT)
        .header("apikey", Config.require(Config.SUPABASE_PUBLISHABLE_KEY))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    JsonNode session = sendForJson(request, "sign in test user " + email);
    JsonNode token = session.get("access_token");
    if (token == null || !token.isString()) {
      throw new IllegalStateException("Supabase sign-in returned no access token for " + email);
    }
    return token.asString();
  }

  static void deleteUser(UUID id) {
    HttpRequest request = adminRequest("/auth/v1/admin/users/" + id).DELETE().build();
    send(request, "delete test user " + id);
  }

  private static HttpRequest.Builder adminRequest(String path) {
    String secretKey = Config.require(Config.SUPABASE_SECRET_KEY);
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
        .timeout(REQUEST_TIMEOUT)
        .header("apikey", secretKey);
    if (!secretKey.startsWith("sb_")) {
      builder.header("Authorization", "Bearer " + secretKey);
    }
    return builder;
  }

  private static URI uri(String path) {
    return URI.create(Config.supabaseUrl() + path);
  }

  private static JsonNode sendForJson(HttpRequest request, String action) {
    return JSON.readTree(send(request, action));
  }

  private static String send(HttpRequest request, String action) {
    HttpResponse<String> response;
    try {
      response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new IllegalStateException("Could not " + action + ": " + e, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while trying to " + action, e);
    }
    if (response.statusCode() / 100 != 2) {
      throw new IllegalStateException(
          "Could not " + action + ": Supabase returned HTTP " + response.statusCode() + ": " + response.body());
    }
    return response.body();
  }
}
