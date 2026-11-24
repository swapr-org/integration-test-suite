package app.swapr.e2e;

import static io.restassured.RestAssured.given;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CreatedUsers implements AutoCloseable {

  static final CreatedUsers INSTANCE = new CreatedUsers();

  private static final Logger LOG = System.getLogger(CreatedUsers.class.getName());
  private static final int NO_CONTENT = 204;

  private final Map<UUID, TestUser> users = new LinkedHashMap<>();

  private CreatedUsers() {
  }

  synchronized void record(UUID id, String email) {
    users.put(id, new TestUser(id, email, null));
  }

  synchronized void signedIn(TestUser user) {
    users.put(user.id(), user);
  }

  @Override
  public void close() {
    List<TestUser> toDelete;
    synchronized (this) {
      toDelete = List.copyOf(users.values());
      users.clear();
    }
    if (toDelete.isEmpty()) {
      return;
    }
    List<Service> dataServices = new ArrayList<>();
    for (Service service : Service.values()) {
      if (service.userDataDeletionPath().isPresent()) {
        dataServices.add(service);
      }
    }
    int dataDeleted = 0;
    List<TestUser> failed = new ArrayList<>();
    for (TestUser user : toDelete) {
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
    LOG.log(Level.INFO, "Test user cleanup: deleted user data " + dataDeleted + " of " + dataAttempts
        + " time(s) and " + deleted + " of " + toDelete.size() + " user(s)");
    if (!failed.isEmpty()) {
      LOG.log(Level.WARNING, "Test users left behind: " + failed);
    }
  }

  private static boolean deleteData(Service service, TestUser user) {
    String failure;
    if (user.accessToken() == null) {
      failure = "the user never signed in";
    } else {
      try {
        int status = given()
            .spec(Requests.spec(service, user))
            .when()
            .delete(service.userDataDeletionPath().orElseThrow())
            .statusCode();
        if (status == NO_CONTENT) {
          return true;
        }
        failure = "HTTP " + status;
      } catch (RuntimeException e) {
        failure = e.getMessage();
      }
    }
    LOG.log(Level.WARNING, "Could not delete " + service.displayName() + " data for user id=" + user.id()
        + " email=" + user.email() + ": " + failure);
    return false;
  }
}
