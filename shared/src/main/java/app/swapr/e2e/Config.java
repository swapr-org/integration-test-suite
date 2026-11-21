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
import java.util.UUID;

/** Suite settings, read from environment variables first and then from the root {@code .env} file. */
public final class Config {

  public static final String ROOT_DIR_PROPERTY = "e2e.rootDir";
  public static final String SUPABASE_URL = "SUPABASE_URL";
  public static final String SUPABASE_PUBLISHABLE_KEY = "SUPABASE_PUBLISHABLE_KEY";
  public static final String SUPABASE_SECRET_KEY = "SUPABASE_SECRET_KEY";
  public static final String WARMUP_TIMEOUT_SECONDS = "WARMUP_TIMEOUT_SECONDS";
  public static final String RUN_ID = "RUN_ID";

  private static final long DEFAULT_WARMUP_TIMEOUT_SECONDS = 180;
  private static final Map<String, String> DOT_ENV = loadDotEnv();
  private static final String GENERATED_RUN_ID = UUID.randomUUID().toString().substring(0, 8);

  private Config() {
  }

  /** Returns the setting's value, or empty when it is unset or blank. */
  public static Optional<String> get(String key) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      value = DOT_ENV.get(key);
    }
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(value.trim());
  }

  /** Returns the setting's value, or fails with a message naming the missing setting. */
  public static String require(String key) {
    return get(key).orElseThrow(() -> new IllegalStateException(
        "Missing required setting " + key + ": set it as an environment variable or in " + dotEnvPath()));
  }

  /** Returns {@code RUN_ID}, or a random short ID generated once per JVM. */
  public static String runId() {
    return get(RUN_ID).orElse(GENERATED_RUN_ID);
  }

  /** Returns {@code WARMUP_TIMEOUT_SECONDS} as a duration, defaulting to 180 seconds. */
  public static Duration warmupTimeout() {
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

  /** Returns {@code SUPABASE_URL} without a trailing slash. */
  public static String supabaseUrl() {
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
        if (line.startsWith("export ")) {
          line = line.substring("export ".length()).strip();
        }
        int separator = line.indexOf('=');
        if (separator <= 0) {
          continue;
        }
        String key = line.substring(0, separator).strip();
        String value = unquote(line.substring(separator + 1).strip());
        values.put(key, value);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read " + path, e);
    }
    return values;
  }

  private static String unquote(String value) {
    if (value.length() >= 2) {
      char first = value.charAt(0);
      char last = value.charAt(value.length() - 1);
      if ((first == '"' || first == '\'') && first == last) {
        return value.substring(1, value.length() - 1);
      }
    }
    return value;
  }
}
