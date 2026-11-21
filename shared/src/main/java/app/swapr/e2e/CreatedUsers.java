package app.swapr.e2e;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class CreatedUsers implements AutoCloseable {

  static final CreatedUsers INSTANCE = new CreatedUsers();

  private static final Logger LOG = System.getLogger(CreatedUsers.class.getName());

  private final List<Entry> users = new ArrayList<>();

  private CreatedUsers() {
  }

  synchronized void record(UUID id, String email) {
    users.add(new Entry(id, email));
  }

  @Override
  public void close() {
    List<Entry> toDelete;
    synchronized (this) {
      toDelete = List.copyOf(users);
      users.clear();
    }
    if (toDelete.isEmpty()) {
      return;
    }
    List<Entry> failed = new ArrayList<>();
    for (Entry user : toDelete) {
      try {
        SupabaseAuth.deleteUser(user.id());
      } catch (RuntimeException e) {
        failed.add(user);
        LOG.log(Level.WARNING, "Could not delete test user id=" + user.id() + " email=" + user.email() + ": "
            + e.getMessage());
      }
    }
    int deleted = toDelete.size() - failed.size();
    LOG.log(Level.INFO, "Test user cleanup: deleted " + deleted + " of " + toDelete.size() + " user(s)");
    if (!failed.isEmpty()) {
      LOG.log(Level.WARNING, "Test users left behind: " + failed);
    }
  }

  private record Entry(UUID id, String email) {
  }
}
