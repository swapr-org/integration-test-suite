# integration-test-suite

HTTP-level end-to-end tests for swapr, run against the deployed QA services. Java 25, JUnit 5 (Jupiter), RestAssured.

## Layout

| Module | Role |
|---|---|
| `shared` | Configuration, the list of known services, warm-up, Supabase test users, cleanup and request helpers. Main code only. |
| `user-service-tests` | End-to-end tests for user-service. |

A test lives in the module of the service whose endpoint triggers the behaviour. It may call other services' endpoints for setup and assertions.

## Settings

Every setting is read from an environment variable first, then from a `.env` file in the suite's root folder.

| Setting | Required | Meaning |
|---|---|---|
| `USER_SERVICE_URL` | yes | QA base URL of user-service. |
| `SUPABASE_URL` | yes | QA Supabase project URL. |
| `SUPABASE_PUBLISHABLE_KEY` | yes | Supabase publishable key, used for password sign-in. |
| `SUPABASE_SECRET_KEY` | yes | Supabase secret key, used for the admin API. Secret. |
| `WARMUP_TIMEOUT_SECONDS` | no | How long warm-up waits for every service to report healthy. Default 180. |
| `RUN_ID` | no | Tags test user emails. CI sets it to `<run id>-<run attempt>`; locally it is `local`. |

A required setting that is missing fails the run with a message naming it.

## Running locally

1. Copy `.env.example` to `.env` and fill in the values from the QA Supabase project and the QA Render service. `.env` is gitignored; never commit it.
2. Set `JAVA_HOME` to a JDK 25.
3. From the suite's root folder:
   - all modules: `./mvnw verify`
   - some modules: `./mvnw verify -pl <module>[,<module>] -am`, for example `./mvnw verify -pl user-service-tests -am`

Always run from the root folder, so `shared` is built first and `.env` is found.

The console shows every test by name with its result. Reports are written to `<module>/target/surefire-reports/`.

## What happens in a run

- **Warm-up:** before the first test class in each module, the suite polls every known service's `/actuator/health` in parallel until each returns 200 with status `UP`. If any service doesn't within `WARMUP_TIMEOUT_SECONDS`, every test in that module fails with a message naming each failing service and its last status or error.
- **Test users:** `TestUsers.create()` creates a confirmed Supabase user with email `e2e+<runId>-<random>@example.com`, signs it in and returns its ID, email and access token. `Requests.spec(Service.USER_SERVICE, user)` returns a RestAssured request specification acting as that user; `Requests.spec(Service.USER_SERVICE)` returns an anonymous one.
- **Cleanup:** when every test in a module has finished, whatever the results, every user the module created is deleted from Supabase. A failed deletion is logged with the user's ID and email and doesn't change the run's result.

## GitHub Actions

The workflow `.github/workflows/e2e.yml` has three triggers:

- **Manual dispatch:** choose `all` (the default), or untick it and choose single modules. Choosing nothing fails the run with a clear message.
- **Reusable workflow call:** another repo's pipeline runs every module and gets the suite's result as its job's result. The suite always runs its own `main` branch.

  ```yaml
  jobs:
    e2e:
      uses: <org>/integration-test-suite/.github/workflows/e2e.yml@main
      secrets: inherit
  ```

- **Schedule:** every Monday at 06:00 UTC, running every module.

Surefire reports are uploaded as a run artifact named `surefire-reports-<run id>-<run attempt>` on every run, pass or fail.

### One-off setup

Create these at the GitHub org level from the QA Supabase project and the QA Render service:

- org secret `SUPABASE_SECRET_KEY`;
- org variables `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` and `USER_SERVICE_URL`.

The suite repo stays public, so any org repo can call its workflow.

## Adding a service

1. Add an entry to `Service` in `shared`, with the service's name and its URL setting, `<SERVICE>_SERVICE_URL`.
2. Add a `<service>-tests` module with the same `pom.xml` shape as `user-service-tests`, and list it in the root `pom.xml`.
3. Add the URL setting to `.env.example`, this README, the org variables and the `Run` step's `env` in the workflow.
4. Add a boolean dispatch input for the module and map it in the workflow's `Build arguments` step.
5. If the service stores user data, add a cleanup step that calls its test-data deletion endpoint before the Supabase users are deleted.
