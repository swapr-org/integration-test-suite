package app.swapr.e2e;

import java.util.UUID;

/** A confirmed, signed-in Supabase test user. */
public record TestUser(UUID id, String email, String accessToken) {

  @Override
  public String toString() {
    return "TestUser[id=" + id + ", email=" + email + "]";
  }
}
