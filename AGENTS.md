# AGENTS.md — Data Manager Application

> **Purpose of this file:** Guide AI agents (and human contributors) working on this codebase with
> the context, conventions, constraints, and patterns needed to make safe and consistent changes.
> Update this file whenever a significant architectural decision is made or project conventions change.

---

## Requirements and Issue Governance

### Requirement ID Scheme

All requirements must follow the domain-based ID structure:

    <DOMAIN>-<TYPE>-<NN>

Where:

-   **DOMAIN** --- functional area (e.g. `API`, `PROJECT`, `SAMPLE`,
    `MEASUREMENT`, `DATA`, `FAIR`, `CARE`, `QUALITY`, `LAB`, `AUTH`)
-   **TYPE** --- requirement type:
    -   `R` = Functional requirement (system capability)
    -   `NFR` = Non-functional requirement (quality attribute)
    -   `C` = Constraint (solution boundary)
-   **NN** --- sequential number per domain and type (e.g. `01`, `02`,
    `03`)

#### Examples

    API-R-01
    API-NFR-01
    API-C-01
    SAMPLE-R-02
    FAIR-NFR-01
    LAB-C-01

#### Rules

-   IDs must be stable and must never be renumbered.
-   IDs must not encode sprint numbers, versions, or document order.
-   One requirement may be referenced by multiple stories.
-   Constraints (`C`) influence architecture and must not be converted
    into user stories unless user value is directly involved.

------------------------------------------------------------------------

### Requirement Structure

All requirements must be documented in `docs/requirements.md`.

Each requirement must contain:

-   **ID**
-   **Statement** --- clear, capability-level description
-   **Rationale** --- why this requirement exists (strategic,
    regulatory, stakeholder-driven)
-   **Source (optional but recommended)** --- link to:
    -   PRD section
    -   FAIR / CARE principle
    -   Regulatory document
    -   Stakeholder request
    -   ADR

#### Example

    API-R-01 The system shall provide authenticated API access to project metadata.

    Rationale:
    Enables integration with partner laboratories and automated analysis pipelines.

    Source:
    PRD §2.3 Partner Integration

------------------------------------------------------------------------

### Issues: Use the Repo Templates

When creating issues, use the correct template for the scenario.

------------------------------------------------------------------------

#### Story (`.github/ISSUE_TEMPLATE/story.yml`)

Use when creating issues for **user-facing functionality** derived from
the PRD or requirements.

Required fields:

-   **Requirement IDs**
    -   At least one `R-xx` reference
    -   May include related `NFR-xx`
-   **User Story** Written as: \> As a `<role>`, I want `<goal>`, so
    that `<benefit>`.
-   **Acceptance Criteria** One or more testable conditions in Given /
    When / Then format.

Rules:

-   Stories describe value and workflow context.
-   Stories must not introduce new system capabilities not covered by an
    existing requirement.
-   If new capability is needed, update `docs/requirements.md` first.

------------------------------------------------------------------------

#### Task (`.github/ISSUE_TEMPLATE/task.yml`)

Use when creating issues for **concrete technical implementation work**
derived from, and linked to, a story.

Required fields:

-   **Parent Story**
    -   Link to the parent story issue (e.g. `#123`)
-   **Requirement IDs**
    -   At least one `R-xx` or `NFR-xx` reference
-   **Description**
    -   What needs to be implemented
-   **Technical Notes (optional)**
    -   Design hints
    -   Constraints (`C-xx`)
    -   Related ADRs

Rules:

-   Tasks must not redefine acceptance criteria.
-   Tasks must not expand requirement scope.
-   Constraints (`C-xx`) may be referenced here when relevant.

------------------------------------------------------------------------

### Traceability Rules

- Every Story must reference at least one requirement ID.
- Every Task must reference:
    -   A parent Story
    -   At least one requirement ID
-   Every PR must:
    -   Reference the issue it implements
    -   List the requirement IDs addressed
-   If implementation changes behavior:
    -   Update the corresponding requirement
    -   Or explicitly justify why no requirement update is needed

------------------------------------------------------------------------

### Requirement Edits

When editing requirements:

-   Use a Pull Request.
-   Include a changelog entry in the PR description summarizing:
    -   Added / Modified / Removed requirement IDs
    -   Reason for change
    -   Stakeholder or source reference (if applicable)

Agents must never modify requirements silently.


## 1. Project Overview

**Data Manager** is a web-based, multi-omics research data management platform developed by
[QBiC (Quantitative Biology Center)](https://qbic.uni-tuebingen.de/) at the University of Tübingen.
It enables FAIR-compliant (Findable, Accessible, Interoperable, Reusable) data access for the
biomedical life sciences.

**Version:** 1.11.0  
**License:** AGPL-3.0-or-later  
**Repository:** https://github.com/qbicsoftware/data-manager-app  
**DOI:** [10.5281/zenodo.10371779](https://doi.org/10.5281/zenodo.10371779)

### What it does (from the code)

- Multi-user project management for omics research (NGS, proteomics, etc.)
- Experimental design: experiments, experimental groups, variables, confounding variables
- Sample registration and batch management (with OpenBIS integration)
- Measurement metadata management (NGS + proteomics)
- Raw data file tracking and download
- FAIR data export (RO-Crate format support)
- User identity management: local credentials, ORCID OAuth2 login
- Role-based access control (Spring Security ACL)
- Financial/offer tracking (purchase offers linked to projects)
- Notifications, announcements, email confirmations, password reset
- Background job processing (JobRunr)
- Ontology/terminology lookup (TIB Terminology Service, OpenBIS vocabularies)
- Organisation lookup (ROR API)
- Personal access tokens for API use

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (primary), Groovy 4.x (Groovy sources + Spock tests) |
| **Build** | Maven (multi-module, Maven Wrapper `mvnw`) |
| **Framework** | Spring Boot 3.5.x |
| **UI Framework** | Vaadin 24.9 (server-side Java + Flow; CSS theme: `datamanager`) |
| **Frontend build** | Vite (via Vaadin Maven plugin) |
| **Security** | Spring Security, Spring Security ACL, OAuth2 client (ORCID) |
| **Persistence** | Spring Data JPA + Hibernate; MariaDB/MySQL driver |
| **Messaging** | Apache ActiveMQ Artemis (JMS, pub-sub) |
| **Background Jobs** | JobRunr 7.x (with its own dashboard on port 8000) |
| **Caching** | EHCache 3 (via JCache/JSR-107) |
| **Distributed Lock** | ShedLock |
| **File Processing** | Apache POI (XLSX/XLS), docx4j (DOCX), Apache Commons IO |
| **Testing** | Spock Framework 2.4 (Groovy), JUnit 5, Spring Boot Test |
| **Code Quality** | SonarCloud, Google Java Style (`GoogleStyle.xml`) |
| **Security Tooling** | sigstore/cosign (artifact signing), CycloneDX SBOM, PGP verify |
| **Secret Storage** | Custom PKCS12 keystore vault (AES encryption) |
| **External Services** | OpenBIS (data repository), TIB Terminology Service, ORCID API, ROR API |

---

## 3. Repository / Module Structure

This is a **Maven multi-module project**. The root `pom.xml` declares all modules:

```
data-manager-app/
├── datamanager-bom/               # Bill of Materials — all dependency versions live here
├── datamanager-app/               # ⭐ Deployable Spring Boot + Vaadin application (runnable JAR)
│   ├── frontend/                  # CSS themes (themes/datamanager/), JavaScript, Vite config
│   └── src/main/java/life/qbic/datamanager/
│       ├── views/                 # Vaadin @Route views (UI layer)
│       │   ├── projects/          # Project, experiment, sample, measurement, rawdata views
│       │   ├── account/           # User profile, personal access tokens
│       │   ├── login/             # Login, password reset
│       │   ├── register/          # User registration, ORCID registration, email confirmation
│       │   ├── general/           # Shared dialog, grid, upload, download, notification components
│       │   └── navigation/        # Side navigation component
│       ├── security/              # Spring Security config, OIDC user details
│       ├── files/                 # File parsing (XLSX, TSV), export, download logic
│       ├── configuration/         # Spring @Configuration beans
│       └── exceptionhandling/     # Vaadin error/not-found pages
├── domain-concept/                # Core DDD building blocks: DomainEvent, EventStore, Dispatcher
├── application-commons/           # Shared Result/Error types, common utilities
├── logging/                       # QBiC logging facade (wraps SLF4J/Logback)
├── broadcasting/                  # Integration event infrastructure (Artemis/JMS)
├── identity/                      # Identity bounded context — domain layer
│   └── src/main/java/life/qbic/identity/
│       ├── domain/                # User aggregate, token model, domain events, policies
│       └── application/           # User registration, password reset, token services
├── identity-api/                  # Identity context — shared API interfaces (anti-corruption)
├── identity-infrastructure/       # Identity JPA repositories, token store
├── project-management/            # Project management bounded context — domain + application layer
│   └── src/main/java/life/qbic/projectmanagement/
│       ├── domain/model/          # Project, Experiment, Sample, Measurement, Batch aggregates
│       ├── domain/service/        # Domain services
│       ├── application/           # Application services (AsyncProjectService, measurement, sample…)
│       └── application/api/       # AsyncProjectService API + FAIR/RO-Crate support
├── project-management-infrastructure/  # JPA repos, OpenBIS connector, Artemis consumer, template generation
├── finances/                      # Finances bounded context — Offer domain model
├── finances-api/                  # Finances API interfaces
├── finances-infrastructure/       # Finances JPA repositories
├── subscription-api/              # Subscription/notification API interfaces
└── subscription-provider/         # Email subscription provider (SMTP via Spring Mail)
```

**Key architectural principle:** Domain-Driven Design (DDD) with explicit bounded contexts
(`identity`, `project-management`, `finances`). Each context has a `domain` layer, an `application` layer (use-case services, orchestration), and an `infrastructure`
layer (JPA, external connectors). The `datamanager-app` module is the composition root.

---

## 4. How to Build and Run

### Prerequisites

- **Java 21** (Zulu/OpenJDK)
- **Maven** (or use the included `./mvnw` wrapper)
- A running **MariaDB or MySQL** database
- A running **Apache ActiveMQ Artemis** broker
- An **SMTP server** for email
- A **PKCS12 keystore** for the vault (see `README.md` for setup)
- An **OpenBIS** instance for measurement data integration

### Common commands

```bash
# Build and run in development mode (mocks OpenBIS/TIB)
./mvnw spring-boot:run -pl datamanager-app -Pdevelopment

# Run all tests
./mvnw clean verify

# Production build (includes Vaadin frontend bundling)
./mvnw clean package -Pproduction

# Run the production JAR
java -jar datamanager-app/target/datamanager-app-1.11.0.jar
```

### Maven profiles

| Profile | Purpose |
|---|---|
| `development` | Mocks external integrations (OpenBIS, TIB). Fastest for local dev. |
| `production` | Full Vaadin frontend build; no mocks. Required for deployment. |
| `sigcheck` | Enables PGP signature verification + CycloneDX SBOM generation. |
| `it` | Runs integration tests (starts/stops Spring Boot around test phase). |

### Required environment variables (minimal set)

```
DATAMANAGEMENT_DB_URL, 
DATAMANAGEMENT_DB_USER_NAME, 
DATAMANAGEMENT_DB_USER_PW
FINANCE_DB_URL, 
FINANCE_DB_USER_NAME, 
FINANCE_DB_USER_PW
MAIL_HOST, MAIL_PORT, 
MAIL_USERNAME, 
MAIL_PASSWORD
ARTEMIS_MODE, 
ARTEMIS_BROKER_URL, 
ARTEMIS_USER, 
ARTEMIS_PASSWORD
DATAMANAGER_VAULT_KEY, 
DATAMANAGER_VAULT_PATH, 
DATAMANAGER_VAULT_ENTRY_PASSWORD
```

See `README.md` and `datamanager-app/src/main/resources/application.properties` for the full
reference, including ORCID OAuth, OpenBIS, TIB, and ROR configuration.

---

## 5. Coding Conventions

### Code style

- **Google Java Style Guide** — the `GoogleStyle.xml` (IntelliJ) and `.prettierrc.js` enforce
  formatting. Always run the formatter before committing.
- **Package root:** `life.qbic`
- **Language:** Java 21 for all production code. Groovy (4.x) is acceptable for tests only (Spock).

### Domain-Driven Design conventions

- **Aggregates** live in `domain/model/` packages. They emit **domain events** extending
  `DomainEvent` (from `domain-concept`).
- **Application services** orchestrate domain objects and live in `application/`. They must not
  depend on infrastructure (JPA, HTTP) directly — use interfaces/ports.
- **Infrastructure** implementations belong in the corresponding `*-infrastructure` module.
- **Do not bypass bounded context APIs** — the `identity-api` and `finances-api` modules provide
  the contracts. Other contexts must talk through these, not directly to the domain model.

### Exception handling

Follow the guidelines in `ExceptionHandling.md`. Specifically:

- **Never swallow exceptions silently** (no empty catch blocks, no `printStackTrace()` without
  logging).
- **Never log-and-rethrow** the same exception — this pollutes logs.
- **Do not use exceptions as control flow.** Return `Result` types (from `application-commons`)
  for expected failure paths.
- **Throw early, handle late** — prefer failing fast before mutating state.
- **Checked exceptions** should be wrapped in unchecked exceptions at infrastructure boundaries;
  do not leak implementation details to callers.
- Vaadin global exception handlers (`UiExceptionHandler`, `ErrorPage`, `ExceptionErrorPage`) are
  the safety net — they must never be removed.

### Service API conventions

Follow the patterns in `service_api.md`:

- Use `Mono<T>` / `Flux<T>` (Project Reactor) for `AsyncProjectService` operations.
- Request/response objects carry a `requestId` (correlation ID) and the resource identifier.
- Partial updates are supported — provide the smallest meaningful information unit.
- Deletions pass both the parent resource ID and the sub-resource ID.

### Testing

- **Unit tests:** Spock `*Spec.groovy` files in `src/test/groovy`. Prefer Spock for new tests.
- **Integration tests:** `*IT.java` or `*IT.groovy`, run under the `it` Maven profile.
- Test classes match exactly `**/*Spec.class` or `**/*Test.class` (configured in `maven-surefire-plugin`).
- Do **not** use `@SpringBootTest` for pure domain/application layer unit tests — keep them fast
  and framework-free.

### Frontend (Vaadin)

- Views are server-side Java classes annotated with `@Route`.
- Custom reusable dialog components follow the `AppDialog`/`StepperDialog` pattern documented
  in `datamanager-app/front-end-components.md`.
- CSS theme files live in `frontend/themes/datamanager/components/`.
- Vaadin components must not be used in domain or application layer classes.
- The UI design system lives in a separate repository:
  https://github.com/qbicsoftware/data-manager-app-design-system

---

## 6. Key Workflows and Integration Points

### Authentication

1. Local username/password — users register via `/register`, confirm via email token.
2. ORCID OAuth2 — users can log in or link their ORCID via `/register/oidc`.
3. Application access tokens — stored encrypted in the vault; used for programmatic API access.

### Event-driven communication

- Domain events are dispatched via `DomainEventDispatcher` (in-process) within a bounded context.
- Cross-context integration events are published/consumed via **ActiveMQ Artemis** (JMS pub-sub).
  The Artemis topic for identity events is configured under `qbic.broadcasting.identity.topic`.
- The `broadcasting` module provides the integration event infrastructure.

### Data synchronisation (remote measurements)

- The `sync` package in `project-management` handles background synchronisation of measurement
  data from external repositories.
- **ShedLock** prevents duplicate execution in multi-instance deployments.
- **JobRunr** is used for scheduled background jobs (dashboard on port 8000 by default).

### File handling

- Sample and measurement metadata can be bulk-uploaded as XLSX or TSV files.
- The `files/parsing/` package handles parsing; `files/structure/` defines the expected schemas.
- Downloads are served via `DownloadProvider` (Vaadin stream resource pattern).

### FAIR data

- Projects can be exported as **RO-Crate** bundles (see `docs/fair/research-objects.md`).
- The `ROCreateBuilder` and `RoCrateFactory` in `project-management-infrastructure` produce crate
  metadata. The `ro-crate-java` library (1.1.1) is used.

---

## 7. Branching and Release Workflow

- **Main branches:** `development` (integration) → `main` (production/releases).
- **Feature branches:** named `feature/*`
- **Bug fix branches:** named `fix/*` or `hotfix/*`
- **Chore/docs/refactor branches:** named `chore/*`, `docs/*`, `ci/*`, `refactor/*`
- Labels are applied automatically by `.github/labeler.yml`.
- **Releases** are created via the `create-release.yml` workflow (manual trigger with a
  semantic version tag). This bumps POM versions across all modules, builds the production JAR,
  publishes to QBiC Nexus, generates SLSA provenance, and opens a version-bump PR against `main`.
- **Snapshot builds** are published from `development` via `nexus-publish-snapshots.yml`.
- All artifacts are signed with **sigstore/cosign** (OIDC-based). Verify with:
  ```bash
  cosign verify-blob --bundle <artifact>.jar.sigstore.json \
    --certificate-oidc-issuer https://token.actions.githubusercontent.com \
    --certificate-identity '<workflow-url>@refs/heads/development' \
    <artifact>.jar
  ```

---

## 8. CI/CD Pipelines (GitHub Actions)

| Workflow | Trigger | What it does |
|---|---|---|
| `build_package.yml` | Push to any branch | `mvn package` build check |
| `run_tests.yml` | Push to any branch | `mvn clean verify` (unit tests) |
| `sonarcloud.yml` | Push/PR to `development`, `main` | SonarCloud static analysis |
| `codeql-analysis.yml` | Scheduled / PR | CodeQL security scan |
| `nexus-publish-snapshots.yml` | Push to `development` | Publish SNAPSHOT to QBiC Nexus |
| `create-release.yml` | Manual (`workflow_dispatch`) | Full release build + publish + sign |
| `test-version-commit.yml` | Various | Version consistency tests |
| `label-pull-requests.yml` | PR opened/edited | Auto-label PRs |
| `label_new_issues.yml` | Issue opened | Auto-label issues |
| `inactive_issue_handling.yml` | Scheduled | Close stale issues |

**SonarCloud dashboard:** https://sonarcloud.io/project/overview?id=qbicsoftware_data-manager-app

**Quality gate thresholds** are enforced on PRs to `development` and `main`.

---

## 9. Database

- **Engine:** MariaDB / MySQL (utf8mb4_unicode_ci)
- **Schema:** Defined in `sql/complete-schema.sql` (DDL only — apply manually or via migration tool).
- **Default data:** `sql/insert-default-values.sql` (roles, permissions, ACL class entries).
- **DDL auto-update** is controlled via `DATAMANAGEMENT_DB_DDL_AUTO` / `FINANCE_DB_DDL_AUTO`
  (default: `none` — do **not** rely on Hibernate to manage schema in production).
- There are **two separate datasources**: `data-management` (main app data) and `finance`
  (offers/purchases). Each has its own JPA entity manager.
- Spring Security ACL tables (`acl_class`, `acl_sid`, `acl_object_identity`, `acl_entry`) are
  present in the schema. Do not drop or modify these without understanding ACL implications.
- Key SQL views: `project_overview`, `project_measurements`, `project_userinfo`,
  `v_ngs_measurement_sample_json`, `v_pxp_measurement_sample_json`.

---

## 10. Security Notes (for Agents)

- **Do not commit secrets.** All secrets are injected via environment variables or the PKCS12
  vault. Never hardcode credentials, tokens, or passwords.
- The vault requires a password with entropy > 100 bits (enforced at startup).
- Do not downgrade Spring Security, Spring Boot, or Vaadin versions without careful review — this
  project has explicit PGP signature verification on transitive dependencies (`sigcheck` profile).
- Stack traces must never be exposed to end users — the `UiExceptionHandler` and Vaadin error
  pages are the safety net. Do not remove or weaken them.
- Personal access tokens are stored **encrypted** in the database. The encryption key comes from
  the vault.
- ORCID OAuth client credentials (`ORCID_CLIENT_ID`, `ORCID_CLIENT_SECRET`) must be kept secret.

---

## 11. Agent Delegation Patterns

When working on this codebase, an AI agent should:

### Before making changes

1. **Identify the bounded context** the change belongs to (`identity`, `project-management`,
   `finances`, or cross-cutting).
2. **Identify the layer** (`domain`, `application`, `infrastructure`, `views`/UI).
3. Check `ExceptionHandling.md` and `service_api.md` for patterns relevant to the change.
4. Read existing tests in the same module before writing new code.

### Making domain changes

- New domain concepts go in `domain/model/` of the relevant bounded context.
- Emit `DomainEvent` subclasses for significant state changes.
- New aggregates need a corresponding JPA entity in the `*-infrastructure` module and a
  repository interface in the `domain/repository/` package (implemented in infrastructure).
- Add Spock specs in `src/test/groovy/...` matching the package of the class under test.

### Making UI changes

- New views need a `@Route` class in `views/` inside `datamanager-app`.
- Consult `front-end-components.md` for dialog and component composition patterns.
- Add the route to the side navigation if it should be accessible from the project navigation
  (`ProjectSideNavigationComponent`).
- Do not import domain model classes directly in views — go through `application` layer services.

### Adding external integrations

- New external service clients belong in the relevant `*-infrastructure` module.
- Add a configuration property block in `application.properties` following the existing naming
  convention (e.g., `qbic.external-service.<name>.<property>`).
- Mock the integration for the `development` Maven profile so local dev does not require the
  external system.

### Dependency management

- All third-party dependency versions must be declared in `datamanager-bom/pom.xml`.
- Do not add `<version>` tags to dependencies in child modules — they must inherit from the BOM.
- Check PGP signatures exist for new dependencies if adding them under the `sigcheck` profile.

---

## 12. What to Ask a Human Before Proceeding

An agent should pause and request human review/approval before:

- Adding or removing Maven modules.
- Changing or dropping database schema elements (tables, columns, views, constraints).
- Modifying Spring Security configuration (`SecurityConfiguration.java`, ACL setup).
- Changing the vault/keystore configuration or encryption logic.
- Bumping major versions of Vaadin, Spring Boot, or Spring Security.
- Adding new OAuth2 provider configurations.
- Modifying the release or snapshot CI workflows.
- Making changes that affect FAIR data export (RO-Crate output format).
- Changing the Artemis messaging topic names or JMS consumer configuration.

---

## 13. Key Files Quick Reference

| File / Path | Purpose |
|---|---|
| `README.md` | Setup, configuration reference, how to run |
| `ExceptionHandling.md` | Exception handling conventions (read before touching error handling) |
| `service_api.md` | Service API design patterns (Mono/Flux, request/response shapes) |
| `datamanager-app/front-end-components.md` | UI component composition patterns |
| `docs/fair/research-objects.md` | FAIR/RO-Crate integration notes |
| `docs/processes/` | Sequence diagrams (sample registration process) |
| `sql/complete-schema.sql` | Full database DDL |
| `sql/insert-default-values.sql` | Seed data (roles, permissions, ACL classes) |
| `datamanager-app/src/main/resources/application.properties` | Full application configuration with env var mappings |
| `datamanager-bom/pom.xml` | All dependency version pins |
| `.github/labeler.yml` | PR/branch labeling rules |
| `.github/ISSUE_TEMPLATE/story.yml` | Issue template for user-facing functionality (user stories) |
| `.github/ISSUE_TEMPLATE/task.yml` | Issue template for concrete technical implementation tasks |
| `.github/release.yml` | Release changelog categories |
| `GoogleStyle.xml` | IntelliJ Google Java Style formatter config |
