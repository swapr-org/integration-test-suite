package app.swapr.e2e;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

/** Creates Supabase test users that are deleted when the test JVM finishes. */
public final class TestUsers {

  private static final SecureRandom RANDOM = new SecureRandom();

  private TestUsers() {
  }

  /** Creates a confirmed test user, records it for cleanup and signs it in. */
  public static TestUser create() {
    String email = "e2e+" + Config.runId() + "-" + randomHex(4) + "@example.com";
    String password = randomHex(24);
    UUID id = SupabaseAuth.createConfirmedUser(email, password);
    CreatedUsers.INSTANCE.record(id, email);
    TestUser user = new TestUser(id, email, SupabaseAuth.signIn(email, password));
    CreatedUsers.INSTANCE.signedIn(user);
    return user;
  }

  private static String randomHex(int bytes) {
    byte[] buffer = new byte[bytes];
    RANDOM.nextBytes(buffer);
    return HexFormat.of().formatHex(buffer);
  }
}
