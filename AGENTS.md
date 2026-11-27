# AGENTS.md

Orientation for AI agents working in this repository: where the authoritative
rules live, and what this project does differently from a generic Java codebase.

## Where the rules live

Read the source rather than a summary.

| Topic | Source |
|---|---|
| Exception handling | [`ExceptionHandling.md`](ExceptionHandling.md) |
| Service API patterns (Mono/Flux, request/response) | [`service_api.md`](service_api.md) |
| UI dialog and component composition | [`datamanager-app/front-end-components.md`](datamanager-app/front-end-components.md) |
| Configuration reference | `README.md`, `datamanager-app/src/main/resources/application.properties` |
| Issue templates | `.github/ISSUE_TEMPLATE/` |

## Codebase shape

Maven multi-module, Java 21, package root `life.qbic`. Domain-Driven Design with
explicit bounded contexts:

- `identity` — users, tokens, password policy
- `project-management` — projects, experiments, samples, measurements, batches
- `finances` — offers and purchases
- `user-groups` — group membership and group SIDs

Each context splits into `domain` (aggregates, value objects, domain events),
`application` (use-case services), and a sibling `*-infrastructure` module (JPA,
external connectors). `datamanager-app` is the composition root and holds the
Vaadin views.

- **Cross-context calls go through the `*-api` modules** (`identity-api`,
  `finances-api`, `user-groups-api`). Do not reach into another context's domain
  model.
- **Dependency versions live in `datamanager-bom/pom.xml`.** Child modules carry
  no `<version>` tags.

## Running it

To look at a screen, use the mock-up:

```sh
./scripts/mockup/run.sh        # http://localhost:48013, already authenticated
./scripts/mockup/run.sh --stop
```

It needs no MariaDB, Artemis, SMTP or vault. Read
[`scripts/mockup/README.md`](scripts/mockup/README.md) before editing
`Fixtures.java` — breaking certain fixture invariants yields a 500 that looks
like an application bug.

The full application:

```sh
./mvnw spring-boot:run -pl datamanager-app -Pdevelopment   # mocks OpenBIS, TIB, ROR
./mvnw clean verify                                        # what CI runs
./mvnw clean package -Pproduction                          # release build
```

## Conventions

- **Tests are Spock specs.** ~236 `*Spec.groovy` against 7 `*Test.java`. Surefire
  runs only `**/*Spec.class` and `**/*Test.class`; a test named otherwise never
  executes. Do not use `@SpringBootTest` for domain or application unit tests.
- **No swallowed exceptions, no log-and-rethrow, no exceptions as control flow.**
  Return `Result` from `application-commons` for expected failures.
- **Views must not import domain model classes.** Go through the application layer.
- **`ddl-auto` defaults to `none`.** `sql/complete-schema.sql` is authoritative.
- **Conventional Commits**, optional scope: `fix(mockup): …`. Branches:
  `feature/*`, `fix/*`, `chore/*`, `docs/*`, `refactor/*`.

## Ask a human first

- Adding or removing a Maven module.
- Changing the database schema in `sql/complete-schema.sql`.
- Spring Security config, the vault/keystore, or OAuth2 providers.
- Artemis topic names or JMS consumer configuration.
- The RO-Crate export format.
