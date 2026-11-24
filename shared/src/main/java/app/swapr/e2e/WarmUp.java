package app.swapr.e2e;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class WarmUp {

  private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final String HEALTH_PATH = "/actuator/health";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private WarmUp() {
  }

  static Optional<String> run(List<Service> services, Duration timeout) {
    List<String> missing = new ArrayList<>();
    Map<Service, String> urls = new EnumMap<>(Service.class);
    for (Service service : services) {
      Optional<String> url = Config.get(service.urlSetting());
      if (url.isPresent()) {
        urls.put(service, Config.stripTrailingSlash(url.get()) + HEALTH_PATH);
      } else {
        missing.add(service.urlSetting());
      }
    }
    boolean storesUserData = services.stream().anyMatch(service -> service.testDataDeletionPath().isPresent());
    if (storesUserData && Config.get(Config.INTERNAL_TOKEN).isEmpty()) {
      missing.add(Config.INTERNAL_TOKEN);
    }
    if (!missing.isEmpty()) {
      return Optional.of("Warm-up failed: missing required setting(s) " + String.join(", ", missing)
          + ". Set them as environment variables or in the suite's .env file.");
    }

    Instant deadline = Instant.now().plus(timeout);
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    Map<Service, Future<Optional<String>>> results = new EnumMap<>(Service.class);
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      urls.forEach((service, url) -> results.put(service, executor.submit(() -> poll(client, url, deadline))));
    }

    List<String> failures = new ArrayList<>();
    results.forEach((service, future) -> {
      outcome(future).ifPresent(problem ->
          failures.add(service.displayName() + " (" + urls.get(service) + "): " + problem));
    });
    if (failures.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of("Warm-up failed: these services did not report healthy within " + timeout.toSeconds()
        + " seconds (" + Config.WARMUP_TIMEOUT_SECONDS + "):\n  - " + String.join("\n  - ", failures));
  }

  private static Optional<String> outcome(Future<Optional<String>> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.of("warm-up was interrupted");
    } catch (ExecutionException e) {
      return Optional.of("warm-up error: " + e.getCause());
    }
  }

  private static Optional<String> poll(HttpClient client, String url, Instant deadline)
      throws InterruptedException {
    Optional<String> lastProblem = Optional.of("no response yet");
    while (true) {
      Instant attemptStart = Instant.now();
      if (!attemptStart.isBefore(deadline)) {
        return lastProblem;
      }
      Duration remaining = Duration.between(attemptStart, deadline);
      Duration requestTimeout = remaining.compareTo(REQUEST_TIMEOUT) < 0 ? remaining : REQUEST_TIMEOUT;
      lastProblem = check(client, url, requestTimeout);
      if (lastProblem.isEmpty()) {
        return lastProblem;
      }
      Duration wait = POLL_INTERVAL.minus(Duration.between(attemptStart, Instant.now()));
      Duration untilDeadline = Duration.between(Instant.now(), deadline);
      if (untilDeadline.isNegative() || untilDeadline.isZero()) {
        return lastProblem;
      }
      if (!wait.isNegative()) {
        Thread.sleep(wait.compareTo(untilDeadline) < 0 ? wait : untilDeadline);
      }
    }
  }

  private static Optional<String> check(HttpClient client, String url, Duration requestTimeout)
      throws InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(requestTimeout)
        .header("Accept", "application/json")
        .GET()
        .build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      Optional<String> status = healthStatus(response.body());
      if (response.statusCode() == 200 && status.equals(Optional.of("UP"))) {
        return Optional.empty();
      }
      return Optional.of("HTTP " + response.statusCode() + status.map(value -> ", status " + value).orElse(""));
    } catch (InterruptedException e) {
      throw e;
    } catch (Exception e) {
      return Optional.of(e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
    }
  }

  private static Optional<String> healthStatus(String body) {
    try {
      JsonNode status = JSON.readTree(body).get("status");
      return status == null || !status.isString() ? Optional.empty() : Optional.of(status.asString());
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }
}
