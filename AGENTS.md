# AGENTS.md

Orientation for AI agents working in this repository. This file is deliberately
short: it tells you where the authoritative rules live and what this project does
differently from a generic Java codebase. It does not restate the documentation
under `docs/`, because two copies of a rule drift and the copy in `docs/` is the
one CI and human reviewers check.

**Version note.** This repository is on Spring Boot 4.x and Vaadin 25.x. The
AGENTS.md that was deleted in `c72aef880` still claimed Spring Boot 3.5.x and
Vaadin 24.9 — two major versions stale. When you read anything outside this file,
prefer the POM over prose.

## Where the rules actually live

Read the relevant source rather than trusting a summary.

| Topic | Authoritative source |
|---|---|
| Requirements (IDs, format, editing, retiring) | [`docs/requirements-guide.md`](docs/requirements-guide.md) |
| What is currently required | [`docs/requirements.md`](docs/requirements.md) |
| Features and stories, with status | [`docs/features.md`](docs/features.md) |
| Architecture decisions and their rationale | [`docs/adr/README.md`](docs/adr/README.md) |
| Exception handling conventions | [`ExceptionHandling.md`](ExceptionHandling.md) |
| Service API patterns (Mono/Flux, request/response) | [`service_api.md`](service_api.md) |
| UI dialog/component composition | [`datamanager-app/front-end-components.md`](datamanager-app/front-end-components.md) |
| Full configuration reference | `README.md` and `datamanager-app/src/main/resources/application.properties` |
| Issue templates | `.github/ISSUE_TEMPLATE/` |

## Shape of the codebase

Maven multi-module, ~19 modules, Java 21, package root `life.qbic`. Domain-Driven
Design with explicit bounded contexts:

- `identity` — users, tokens, password policy
- `project-management` — projects, experiments, samples, measurements, batches
- `finances` — offers and purchases
- `user-groups` — group membership and group SIDs

Each context is split into `domain` (aggregates, value objects, domain events),
`application` (use-case services), and a sibling `*-infrastructure` module (JPA,
external connectors). `datamanager-app` is the composition root and holds the
Vaadin views.

Two rules that are easy to violate by accident:

- **Cross-context calls go through the `*-api` modules** (`identity-api`,
  `finances-api`, `user-groups-api`). Do not reach into another context's domain
  model directly.
- **All dependency versions are declared in `datamanager-bom/pom.xml`.** Child
  modules must not carry `<version>` tags.

## Working without infrastructure

The app normally needs two MariaDB databases, an Artemis broker, an SMTP server,
a PKCS12 vault and eight environment variables. None of that is needed to look at
a screen:

```sh
./scripts/mockup/run.sh          # ~1 min after the first build; http://localhost:48013
./scripts/mockup/run.sh --stop
```

Port 48013 is already authenticated. Use this for any visual review or
screenshot instead of provisioning the stack. It reads the jar version from the
POM, so it survives a release bump. Its limits and the fixture rules that matter
when you change seed data are documented in
[`scripts/mockup/README.md`](scripts/mockup/README.md) — read the "If you are an
agent" section before editing `Fixtures.java`, because breaking certain fixture
invariants produces a 500 that looks like an application bug.

Running the full application instead:

```sh
./mvnw spring-boot:run -pl datamanager-app -Pdevelopment   # mocks OpenBIS, TIB, ROR
./mvnw clean verify                                        # tests (this is what CI runs)
./mvnw clean package -Pproduction                          # release build
```

`-Pdevelopment` swaps external integrations for mock beans (`@Profile("development")`
in the infrastructure modules); `-Pproduction` uses the real ones.

## Conventions worth knowing before you type

- **Tests are Spock specs.** ~236 `*Spec.groovy` files against 7 `*Test.java`.
  Surefire runs only `**/*Spec.class` and `**/*Test.class`, so a test named
  otherwise is silently never executed. Prefer Spock for new tests. Do not use
  `@SpringBootTest` for domain/application unit tests.
- **Never swallow exceptions, never log-and-rethrow, never use exceptions for
  control flow.** Return `Result` from `application-commons` for expected
  failures. See `ExceptionHandling.md`.
- **The Vaadin error pages and `UiExceptionHandler` are the safety net** that
  keeps stack traces from reaching users. Do not weaken or remove them.
- **Views must not import domain model classes.** Go through the application
  layer.
- **`ddl-auto` defaults to `none`.** The schema in `sql/complete-schema.sql` is
  authoritative and applied deliberately; do not rely on Hibernate to migrate.
- **Commits follow Conventional Commits** (`feat:`, `fix:`, `docs:`, `chore:`,
  `refactor:`, `build:`) with an optional scope, e.g.
  `fix(mockup): seed access control and project fields the domain requires`.
  Branches are `feature/*`, `fix/*`, `chore/*`, `docs/*`, `refactor/*`.

## Pause and ask a human

Do not proceed on these without explicit approval:

- Adding or removing a Maven module.
- Changing the database schema (`sql/complete-schema.sql`) or dropping schema
  elements.
- Editing `docs/requirements.md`. Requirement changes need their own PR, separate
  from implementation, with a changelog in the description — see
  `docs/requirements-guide.md`.
- Renaming or retiring a Feature slug that Stories already reference.
- Modifying Spring Security config, the vault/keystore, or OAuth2 providers.
- Changing Artemis topic names or JMS consumer configuration.
- Changing the RO-Crate export format.
- Creating a new ADR, and adding it to the index in `docs/adr/README.md`.

## A note on stale documents

This file replaces one that grew to 806 lines and was deleted because it had
drifted from the code it described. If you find a statement here that no longer
matches the repository, fix it in the same PR rather than working around it — and
if the correct answer already lives under `docs/`, link to it instead of copying
it.
