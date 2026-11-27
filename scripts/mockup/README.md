# Mock-up runtime

Start the Data Manager as a self-contained, already-logged-in application so a
screen can be opened and screenshotted without provisioning anything.

```sh
./scripts/mockup/run.sh          # build the database if needed, then start
./scripts/mockup/capture.sh      # print the routes that are ready to screenshot
./scripts/mockup/run.sh --clean  # start over, including the vault
```

Then open <http://localhost:48013/projects/list>. It is authenticated as a
fixture user; there is no login step for the person or tool taking the
screenshot.

## If you are an agent: read this first

Anyone asked to look at, review or screenshot a screen should use this instead of
provisioning MariaDB, Artemis, SMTP and the vault — those four are replaced here,
and standing them up is the slowest possible way to see the UI.

- **Port 48013 is the authenticated proxy.** Any URL there is already logged in.
  Use it for screenshots. Port 48014 is the application itself, with a login form.
  Override with `DM_MOCKUP_PORT` / `DM_MOCKUP_APP_PORT`. If your browser tool is
  pinned to a single origin, put the **proxy** on that origin:
  `DM_MOCKUP_PORT=48014 DM_MOCKUP_APP_PORT=48015 ./scripts/mockup/run.sh --detach`.
- **Accounts** (log in with the **email address**, not the username):
  `dev@qbic.local` / `DevPassword123!` and `admin@qbic.local` /
  `AdminPassword123!`.
- **The work directory is `.mockup/`** (git-ignored), with the log at
  `.mockup/mockup.log`.
- **Changed anything in `Fixtures.java`? Restart with `--reset-db`.** The marker
  file `.mockup/.database-ready` makes `run.sh` skip the rebuild otherwise, and
  you will keep testing against the old rows while believing the change did not
  work.
- **A page that 500s:** check `grep -c ExceptionErrorPage .mockup/mockup.log`
  first. Zero means the page is fine and you are looking at a stale capture.

To diagnose a failure properly you need debug logging — the global error page logs
its message at `ERROR` but the stack trace only at `DEBUG`, so without this you
get a bare line such as `Error attempting to apply AttributeConverter` and no
cause:

```sh
DM_LOG_LEVEL=debug ./scripts/mockup/run.sh --reset-db --detach
grep -n 'Caused by' .mockup/mockup.log
```

An `SMTPAppender` also logs "Couldn't connect to host, port 127.0.0.1, 587" after
every error. Mail is not provisioned; that line is expected and is not the failure
you are chasing.

## What it replaces

Booting the application normally requires two MariaDB databases, an Artemis
broker, an SMTP server, a PKCS12 vault holding a specific AES key, and eight
environment variables with no defaults. Each of those is a way to fail before
anything is drawn. The mock-up substitutes an embedded H2 database built from
the repository's own `sql/complete-schema.sql`, the embedded Artemis broker the
application already ships, a throwaway vault, and fixture rows.

## The pieces

| File | Purpose |
|---|---|
| `run.sh` | Entry point: resolves a JDK, builds the database, starts the app and the proxy. |
| `schema-to-h2.js` | Rewrites the MariaDB DDL into the subset H2 accepts. 56 tables and 9 views load. |
| `MockDatabase.java` | Loads the translated schema into H2. |
| `Fixtures.java` | Seeds two accounts, three projects, and the ACL rows that make them visible. |
| `VaultKey.java` | Provisions the AES key the app demands in the PKCS12 vault. |
| `session-proxy.js` | Logs in once and forwards the session, so any URL is already authenticated. |
| `routes.txt` | Named routes for review. |

## What was actually verified

These are not assumptions; each was the difference between a working start and
a specific failure:

- **The real schema runs on H2.** 56/56 tables, 9/9 views. Four MariaDB-only
  constructs had to be rewritten first (column `COMMENT`, `json_valid` checks,
  inline `KEY` declarations, `b'0'` literals).
- **`vaadin.productionMode=true` is required.** Without it the app serves an
  empty shell, because the development bundle expects a Vite dev server on
  another port.
- **The session cookie must lose both of its production defaults.** With
  `Secure=true` on plain HTTP, or `Domain=de.uni-tuebingen.qbic` on localhost, a
  browser silently refuses the cookie and the app shows "Cookies disabled".
- **Login is by email address, not username.** The form field is called
  `username`, which makes this easy to get wrong.
- **Passwords are PBKDF2WithHmacSHA256, 100000 iterations.** SHA512 produces a
  hash the application silently rejects.
- **A user ACL sid holds the user's UUID, not their username** — the
  `project_userinfo` view joins `acl_sid.sid` onto `users.id`.
- **The fixture accounts need a `user_role` row**, or the overview resolves no
  accessible projects and renders the empty state.
- **One ACE per permission, never one combined mask.** Spring's
  `DefaultPermissionGrantingStrategy` matches ACE masks by *equality*, so a single
  entry with `READ|WRITE|CREATE|DELETE|ADMINISTRATION` = 31 matches nothing. The
  permission check falls through to `NotFoundException`, which the evaluator
  reports as plain `false`, and every project view answers with the generic
  access-denied 500. The fixture writes one ACE per bit, exactly as
  `ProjectAccessServiceImpl.insertAce` does.
- **The project rows need `objective` and all three contact email addresses.**
  The DDL marks them nullable and the domain objects reject a NULL. A missing
  objective throws inside `ProjectRepositoryImpl.find` (`ProjectObjective`'s
  converter), which is the 500 on the project page; a missing contact email
  survives the entity load and only fails later, in the view, as "Value cannot be
  null" from the `mailto:` anchor.
- **`./tmp` must exist in the working directory**, and Vaadin wants a writable
  home; the script sets both.
- **Stopping the app by port does not work on this platform.** Tomcat binds an
  IPv6 socket and `lsof -ti:PORT` finds no pid for it, so the previous run
  survived every restart and the orphans then competed for the database — which
  is what made navigation feel slow. The pids are now recorded and stopped
  explicitly.

## What still does not work on H2

Three project-page queries contain MariaDB-only syntax that H2 cannot parse at
all — `COLLATE utf8mb4_unicode_ci` on a column, and `CAST(x AS CHAR CHARACTER
SET utf8mb4)`. H2 has no `COLLATE` clause in any version and no collation
registry, so this cannot be patched from the schema translation:

| Query | Page | What you see |
|---|---|---|
| `ExperimentProgressQuery` | Summary → "Where the project stands" | "Project status could not be loaded." |
| `AssayStandingQuery` | Summary → assay types | "None recorded" |
| `MeasurementDataQuery` | Datasets → measurement datasets | "The project's datasets could not be loaded." |

These are **mock-up fidelity limits, not application bugs** — the same queries run
on the MariaDB deployment. The pages are still worth screenshotting: the failure
is contained to one panel, and each renders its own message rather than a 500.

The `experiments` route additionally needs an experiment id
(`projects/<id>/experiments/<experimentId>/…`). The fixture seeds no experiments,
so the bare `experiments` path correctly reports "Missing experiment id in
route" — seed an experiment before using those routes.

## If navigation feels slow

Check how many instances are running. An orphaned JVM from an earlier run keeps
the previous database open and competes for it, and the application then feels
sluggish without anything obviously failing:

```sh
lsof +D .mockup | grep -c java     # want 1
```

`run.sh` records the pids it starts in `.mockup/app.pid` and
`.mockup/proxy.pid` and stops them on the next run. It deliberately does not
look the processes up by port: Tomcat binds an IPv6 socket here (`netstat`
shows `tcp46 *.48014`) and `lsof -ti:48014` returns nothing for it, so a
port-based lookup silently matched nothing and every restart leaked another
JVM. If you find a pile of them, stop the one in `app.pid` and start again.

The session proxy is optional (`--no-proxy`). It exists so a tool that cannot
fill in a login form can still reach a protected page, and it keeps a keep-alive
connection to the application so `LONG_POLLING` push traffic does not churn
sockets — but it is still one extra hop, so if you are driving the app with a
real browser, `--no-proxy` is the faster and simpler choice.

## Rules when editing `Fixtures.java`

These are not stylistic. Each one was a 500 shown to the user, and each looks
like a bug in the view rather than in the seed data:

- **The fixtures must mirror what the application writes, not what the DDL
  permits.** The schema is far more permissive than the domain model: a column
  the DDL marks `DEFAULT NULL` is routinely rejected by a constructor or an
  `AttributeConverter`.
- **One ACE per permission, never one combined mask.** Spring's
  `DefaultPermissionGrantingStrategy` compares ACE masks by *equality*, so a
  single entry with `READ|WRITE|CREATE|DELETE|ADMINISTRATION` (= 31) matches no
  permission. The check falls through to `NotFoundException`, the evaluator
  reports plain `false`, and every project view answers **access denied** while
  the ACE rows look perfectly correct. Write one ACE per bit, as
  `ProjectAccessServiceImpl.insertAce` does.
- **Project rows need `objective` and all three contact email addresses.**
  `ProjectObjective` rejects null, so the converter throws inside
  `ProjectRepositoryImpl.find` and the project page 500s before rendering.
  `Contact`'s no-arg JPA constructor skips its own validation, so a null address
  survives the entity load and only fails later, in the view, as "Value cannot be
  null" from the `mailto:` anchor.

## What is not covered

- **Fixture depth stops at the project.** The three projects exist as rows and
  their five pages render with real data; experiments, samples and measurements
  do not exist, so routes under `projects/<id>/experiments/...` correctly report
  "Missing experiment id in route". Seed an experiment before adding one to
  `routes.txt`.
- **Not every route is checked.** A route that does not resolve renders the
  not-found page, which looks like a screenshot and reports nothing. Add a route
  to `routes.txt` only after confirming it resolves.
- **The proxy is not a security boundary.** It holds a session and hands it to
  anything that connects to port 48013. Local mock-up only; never point it at a
  real deployment.
- **The schema translation is not exhaustive.** It covers what
  `complete-schema.sql` contains today. A new MariaDB-only construct will fail
  the load loudly, naming the statement — extend `schema-to-h2.js` rather than
  editing the production schema.
