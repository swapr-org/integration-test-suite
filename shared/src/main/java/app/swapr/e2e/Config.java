package app.swapr.e2e;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class Config {

  static final String ROOT_DIR_PROPERTY = "e2e.rootDir";
  static final String SUPABASE_URL = "SUPABASE_URL";
  static final String SUPABASE_PUBLISHABLE_KEY = "SUPABASE_PUBLISHABLE_KEY";
  static final String SUPABASE_SECRET_KEY = "SUPABASE_SECRET_KEY";
  static final String INTERNAL_TOKEN = "INTERNAL_TOKEN";
  static final String WARMUP_TIMEOUT_SECONDS = "WARMUP_TIMEOUT_SECONDS";
  static final String RUN_ID = "RUN_ID";

  private static final long DEFAULT_WARMUP_TIMEOUT_SECONDS = 180;
  private static final Map<String, String> DOT_ENV = loadDotEnv();
  private static final String LOCAL_RUN_ID = "local";

  private Config() {
  }

  static Optional<String> get(String key) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      value = DOT_ENV.get(key);
    }
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(value.trim());
  }

  static String require(String key) {
    return get(key).orElseThrow(() -> new IllegalStateException(
        "Missing required setting " + key + ": set it as an environment variable or in " + dotEnvPath()));
  }

  static String runId() {
    return get(RUN_ID).orElse(LOCAL_RUN_ID);
  }

  static Duration warmupTimeout() {
    return get(WARMUP_TIMEOUT_SECONDS)
        .map(value -> {
          try {
            return Duration.ofSeconds(Long.parseLong(value));
          } catch (NumberFormatException e) {
            throw new IllegalStateException(
                "Setting " + WARMUP_TIMEOUT_SECONDS + " must be a whole number of seconds, got: " + value, e);
          }
        })
        .orElse(Duration.ofSeconds(DEFAULT_WARMUP_TIMEOUT_SECONDS));
  }

  static String supabaseUrl() {
    return stripTrailingSlash(require(SUPABASE_URL));
  }

  static String stripTrailingSlash(String url) {
    String result = url;
    while (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  private static Path dotEnvPath() {
    String rootDir = System.getProperty(ROOT_DIR_PROPERTY);
    Path root = rootDir == null || rootDir.isBlank() ? Path.of("") : Path.of(rootDir);
    return root.resolve(".env").toAbsolutePath();
  }

  private static Map<String, String> loadDotEnv() {
    Path path = dotEnvPath();
    Map<String, String> values = new HashMap<>();
    if (!Files.isRegularFile(path)) {
      return values;
    }
    try {
      for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
        String line = rawLine.strip();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        int separator = line.indexOf('=');
        if (separator <= 0) {
          continue;
        }
        String key = line.substring(0, separator).strip();
        String value = line.substring(separator + 1).strip();
        values.put(key, value);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read " + path, e);
    }
    return values;
  }
}
