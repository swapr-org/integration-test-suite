package app.swapr.e2e;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Auto-detected extension that warms up every known service once per JVM and cleans up test users. */
public final class SuiteExtension implements BeforeAllCallback {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SuiteExtension.class);
  private static final String CREATED_USERS_KEY = "createdUsers";
  private static final String WARM_UP_KEY = "warmUp";

  @Override
  public void beforeAll(ExtensionContext context) {
    ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
    store.computeIfAbsent(CREATED_USERS_KEY, key -> CreatedUsers.INSTANCE, CreatedUsers.class);
    WarmUpResult result = store.computeIfAbsent(WARM_UP_KEY, key -> warmUp(), WarmUpResult.class);
    result.failure().ifPresent(message -> {
      throw new IllegalStateException(message);
    });
  }

  private static WarmUpResult warmUp() {
    List<Service> services = Arrays.asList(Service.values());
    try {
      return new WarmUpResult(WarmUp.run(services, Config.warmupTimeout()));
    } catch (RuntimeException e) {
      return new WarmUpResult(Optional.of("Warm-up failed: " + e.getMessage()));
    }
  }

  private record WarmUpResult(Optional<String> failure) {
  }
}
