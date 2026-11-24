package app.swapr.e2e;

import static io.restassured.RestAssured.given;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class CreatedUsers implements AutoCloseable {

  static final CreatedUsers INSTANCE = new CreatedUsers();

  private static final Logger LOG = System.getLogger(CreatedUsers.class.getName());
  private static final int NO_CONTENT = 204;

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
    List<Service> dataServices = new ArrayList<>();
    for (Service service : Service.values()) {
      if (service.testDataDeletionPath().isPresent()) {
        dataServices.add(service);
      }
    }
    int dataDeleted = 0;
    List<Entry> failed = new ArrayList<>();
    for (Entry user : toDelete) {
      for (Service service : dataServices) {
        if (deleteData(service, user)) {
          dataDeleted++;
        }
      }
      try {
        SupabaseAuth.deleteUser(user.id());
      } catch (RuntimeException e) {
        failed.add(user);
        LOG.log(Level.WARNING, "Could not delete test user id=" + user.id() + " email=" + user.email() + ": "
            + e.getMessage());
      }
    }
    int deleted = toDelete.size() - failed.size();
    int dataAttempts = toDelete.size() * dataServices.size();
    LOG.log(Level.INFO, "Test user cleanup: deleted test data " + dataDeleted + " of " + dataAttempts
        + " time(s) and " + deleted + " of " + toDelete.size() + " user(s)");
    if (!failed.isEmpty()) {
      LOG.log(Level.WARNING, "Test users left behind: " + failed);
    }
  }

  private static boolean deleteData(Service service, Entry user) {
    String failure;
    try {
      int status = given()
          .spec(Requests.internal(service))
          .when()
          .delete(service.testDataDeletionPath().orElseThrow(), user.id())
          .statusCode();
      if (status == NO_CONTENT) {
        return true;
      }
      failure = "HTTP " + status;
    } catch (RuntimeException e) {
      failure = e.getMessage();
    }
    LOG.log(Level.WARNING, "Could not delete " + service.displayName() + " test data for user id=" + user.id()
        + " email=" + user.email() + ": " + failure);
    return false;
  }

  private record Entry(UUID id, String email) {
  }
}
