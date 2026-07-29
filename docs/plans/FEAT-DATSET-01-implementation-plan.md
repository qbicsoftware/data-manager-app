# Implementation Plan — FEAT-DATSET-01: Connecting Open, Published Datasets

> **Story:** [#1467](https://github.com/qbicsoftware/data-manager-app/issues/1467)  
> **Parent Feature:** [#1466 — FEAT-DATASET-CONNECTION](https://github.com/qbicsoftware/data-manager-app/issues/1466)  
> **ADRs honored:** [0001](../adr/0001-associated-datasets-domain-model.md), [0002](../adr/0002-invenio-rdm-api-client-credentials.md), [0003](../adr/0003-connection-lifecycle-stewardship.md), [0004](../adr/0004-fair-signposting-deferred.md)  
> **UI Prototype:** `AssociatedDatasetsDemoV2.java` (at `datamanager-app/.../views/demo/`)  
> **Date:** 2025-07-14

---

## 1. Scope — What This Iteration Covers

FEAT-DATSET-01 covers **connecting open (public) published datasets** from InvenioRDM instances to a project. The acceptance criteria are:

- AC1: Navigate to a "Datasets" section in the project navigation (below Summary).
- AC2: Select an InvenioRDM instance to search in.
- AC3: Paginated search results shown when no search term is provided.
- AC4: Submitting a search term returns matching results from the selected instance.
- AC5: Clicking "Connect" adds the dataset to the project.
- AC6: Failure is communicated to the user.
- AC7: Success is communicated to the user.
- AC8: Another project member can see the connected dataset.
- AC9: Optionally associate the dataset with a specific experiment.

**Out of scope for this story** (covered by separate stories):
- Access-restricted datasets (FEAT-DATSET-05, FEAT-DATSET-14, FEAT-DATSET-15) — see *Pragmatic scope decision* below.
- Viewing connected datasets (FEAT-DATSET-02) — partially included here: the connect flow implies loading + persisting, but a dedicated "view connected datasets" section is FEAT-DATSET-02's responsibility. However, the UI prototype shows a "Connected Resources" grid. We include a minimal Connected Resources display here so connected datasets are immediately visible (AC8).
- Removing connections (FEAT-DATSET-03)
- Syncing connections (FEAT-DATSET-04)

**Pragmatic scope decision — restricted datasets:**
The long-term view will show open and restricted datasets in one unified listing (per the prototype's design). However, for this iteration, the token/credential flow (Stories 14/15) is not yet implemented, so restricted datasets cannot be searched or connected. The domain model (`AccessLevel` field on the aggregate) is designed to be future-proof for restricted datasets, but the UI in this iteration is functionally equivalent to a **public-only** view. No credentials section, no token management, no restricted-dataset search — the "Repository Access" section from the prototype is omitted entirely.

**Pragmatic scope decision:** Based on the UI prototype, the first iteration will include:
1. **Navigation entry** — "DATASETS" link in the project drawer below "SUMMARY".
2. **Associated Datasets view** — A new `@Route` view within `ProjectMainLayout` showing:
   - A "Connected Resources" section (empty grid initially, with a "Connect Datasets" button).
   - The connect sidebar (from the prototype) — search, instance selection, multi-select, connect.
3. **Domain + Application layer** — The `associated-dataset` aggregate, application service, and `DatasetSource` port.
4. **Infrastructure layer** — InvenioRDM REST client (stateless, instance-parameterised), JPA persistence.
5. **Notification** — Actor toast on success/failure; email to other project members via existing `InformUsersAboutBatchRegistration` pattern (see Task 6).

---

## 2. Requirement IDs — Out of Scope

The story references `DATA-R-01` and `COMM-R-01`. There is a known ID collision with existing entries in `docs/requirements.md`. Requirement document changes are **out of scope** for this development task — the Product Owner handles requirement tracking and ID resolution separately.

---

## 3. Architecture Overview (per ADRs)

### 3.1 Bounded context: `project-management`

Per ADR-0001, the `associated-dataset` package lives in the `project-management` bounded context.

```
project-management/
├── src/main/java/life/qbic/projectmanagement/
│   ├── domain/model/associated_dataset/      ← Domain model
│   │   ├── AssociatedDataset.java            (aggregate root)
│   │   ├── AssociatedDatasetId.java          (value object)
│   │   ├── SourceType.java                   (enum: INVENIO_RDM)
│   │   ├── ConnectionState.java              (enum: CONNECTED, REMOVED)
│   │   ├── AccessLevel.java                  (enum: PUBLIC, RESTRICTED)
│   │   ├── ExternalHandle.java               (value object: external identity)
│   │   ├── resourcemetadata/
│   │   │   ├── ResourceMetadata.java         (sealed interface)
│   │   │   └── InvenioRdmResourceMetadata.java (concrete, source-specific)
│   │   ├── event/
│   │   │   ├── AssociatedDatasetConnectedEvent.java
│   │   │   └── AssociatedDatasetConnectionFailedEvent.java
│   │   └── repository/
│   │       └── AssociatedDatasetRepository.java  (interface)
│   └── application/associated_dataset/       ← Application services
│       ├── AssociatedDatasetService.java      (use-case orchestration)
│       └── DatasetSource.java                 (port — external data source)
```

```
project-management-infrastructure/
├── src/main/java/life/qbic/projectmanagement/infrastructure/
│   ├── dataset/associated/
│   │   ├── AssociatedDatasetJpaRepository.java   (Spring Data JPA)
│   │   ├── AssociatedDatasetEntity.java          (JPA entity)
│   │   └── AssociatedDatasetRepositoryImpl.java  (implements domain repo)
│   └── external/invenio/
│       ├── InvenioRdmDatasetSource.java          (implements DatasetSource port)
│       ├── InvenioRdmClient.java                 (low-level HTTP client)
│       ├── InvenioRdmSearchResult.java           (transient search result mapping)
│       └── InvenioRdmInstanceConfig.java         (instance config binding)
```

```
datamanager-app/
├── src/main/java/life/qbic/datamanager/views/projects/project/datasets/
│   ├── AssociatedDatasetsMain.java               (@Route view — project layout)
│   ├── ConnectedResourcesComponent.java           (grid of connected datasets)
│   └── ConnectDatasetSidebar.java                 (search + connect sidebar)
```

### 3.2 Layer responsibilities

| Layer | Responsibility |
|---|---|
| **Domain** | Aggregate (`AssociatedDataset`), value objects, events, repository interface |
| **Application** | `AssociatedDatasetService` (use cases: search, connect, list), `DatasetSource` port |
| **Infrastructure** | InvenioRDM REST client, JPA repository impl, entity mapping |
| **Views (UI)** | Vaadin route view, grid, sidebar — purely presentation |

### 3.3 Data flow (connect flow)

```
User enters search term + selects instance
  → UI calls AssociatedDatasetService.search(query, instanceId)
    → DatasetSource.search(query, InstanceConfig) [port]
      → InvenioRdmClient calls InvenioRDM REST API (GET /api/records?q=...)
      → Returns SearchResult
    → Returns List<SearchHit> to UI

User clicks "Connect Selected"
  → UI calls AssociatedDatasetService.connect(projectId, recordIds, optionalExperimentId)
    → For each hit: DatasetSource.resolveMetadata(hitId, InstanceConfig) → ResourceMetadata
    → Creates AssociatedDataset aggregate (state=CONNECTED, accessLevel=PUBLIC)
    → Saves via AssociatedDatasetRepository
    → Emits AssociatedDatasetConnectedEvent
    → Event triggers email to other project members (if notification is in scope)
  → UI shows success toast; refreshes connected resources grid
```

---

## 4. Detailed Task Breakdown

### Task 1: Domain model — `associated_dataset` package
**Module:** `project-management`

- Create `life.qbic.projectmanagement.domain.model.associated_dataset` package.

**Classes to create:**

1. **`SourceType`** — enum: `INVENIO_RDM`. Source-agnostic extensibility point.
2. **`ConnectionState`** — enum: `CONNECTED`, `REMOVED`.
3. **`AccessLevel`** — enum: `PUBLIC`, `RESTRICTED`.
4. **`ExternalHandle`** — value object carrying the external record identifier (e.g., Zenodo record ID, DOI).
5. **`ResourceMetadata`** (sealed interface) — source-agnostic metadata container.
6. **`InvenioRdmResourceMetadata`** — concrete implementation carrying InvenioRDM-specific fields (title, PID, DOI, creators, community, version, resource type, description).
7. **`AssociatedDatasetId`** — value object wrapping a UUID.
8. **`AssociatedDataset`** — aggregate root:
   - Fields: `id`, `projectId`, `sourceType`, `externalHandle`, `connectionState`, `accessLevel`, `resourceMetadata`, `connectedBy` (user), `connectedOn` (timestamp), `optionalExperimentId`, `lastSyncedAt` (nullable).
   - Methods: `connect(...)` factory, `remove()` (soft delete → `state = REMOVED`).
   - Emits `AssociatedDatasetConnectedEvent` on successful connect.
9. **Repository interface** (`AssociatedDatasetRepository`):
   - `save(AssociatedDataset)`
   - `findByProjectId(ProjectId)` → `List<AssociatedDataset>` (excluding REMOVED state)
   - `findById(AssociatedDatasetId)` → `Optional<AssociatedDataset>`

### Task 2: Application service + DatasetSource port
**Module:** `project-management`

1. **`DatasetSource`** (port interface):
   - `SearchResult search(String query, InstanceConfig config)`
   - `ResourceMetadata resolveMetadata(String recordId, InstanceConfig config)`
   - `List<InvenioRdmInstanceDescriptor> availableInstances()` (or read from config)

2. **`InstanceConfig`** — record: `id`, `displayName`, `baseUrl`, `Optional<AccessToken>`.

3. **`SearchResult`** — record: `List<SearchHit> hits`, `int total`, `int page`, `int pageSize`.

4. **`SearchHit`** — record: `id`, `title`, `pid`, `accessLevel`, `version`, `accessLink`, `publicationDate`, `creator`, `resourceType`, `community`, `description`.

5. **`AssociatedDatasetService`** — `@Service` class:
   - `List<SearchHit> searchDatasets(String query, String instanceId)`
   - `Result<AssociatedDatasetId, Error> connectDataset(ProjectId projectId, String recordId, String instanceId, Optional<ExperimentId> experimentId)`
   - `List<AssociatedDatasetInfo> listConnectedDatasets(ProjectId projectId)`
   - Depends on: `AssociatedDatasetRepository`, `DatasetSource`, ACL check.

### Task 3: Infrastructure — InvenioRDM REST client
**Module:** `project-management-infrastructure`

1. **`InvenioRdmClient`** — stateless HTTP client using Spring `RestClient`:
   - `GET {baseUrl}/api/records?q={query}&page={page}&size={size}` — search
   - `GET {baseUrl}/api/records/{id}` — resolve single record metadata
   - Handles pagination via InvenioRDM's `links.next` convention.
   - Bounded synchronous retry (3 attempts, exponential backoff, 5s ceiling) per ADR-0002.
   - Error mapping: 4xx → user-facing error; 5xx/timeout → retry → then user-facing error.

2. **`InvenioRdmDatasetSource`** — implements `DatasetSource` port:
   - Delegates to `InvenioRdmClient`.
   - For this iteration: no token auth (open datasets only). Token support deferred to FEAT-DATSET-05/14.
   - Maps InvenioRDM JSON response → `SearchResult` / `ResourceMetadata`.

3. **`InvenioRdmInstanceProperties`** — `@ConfigurationProperties` for instance config:
   ```properties
   qbic.external-service.invenio-rdm.instances[0].name=Zenodo
   qbic.external-service.invenio-rdm.instances[0].url=https://zenodo.org
   qbic.external-service.invenio-rdm.instances[1].name=FDAT
   qbic.external-service.invenio-rdm.instances[1].url=https://fdat.uni-tuebingen.de
   ```

### Task 4: Infrastructure — JPA persistence
**Module:** `project-management-infrastructure`

1. **`AssociatedDatasetEntity`** — JPA entity mapped to `associated_dataset` table:
   - `id` (PK), `project_id`, `source_type`, `external_handle`, `connection_state`, `access_level`
   - `resource_metadata` (MariaDB `JSON` column) — source-specific metadata
   - `connected_by`, `connected_on`, `experiment_id` (nullable), `last_synced_at`
   - Universal columns for sort/filter: `title`, `pid`, `version`, `publication_date`

2. **`AssociatedDatasetJpaRepository`** — Spring Data JPA repository.

3. **`AssociatedDatasetRepositoryImpl`** — implements domain repository, translates between aggregate ↔ entity.

### Task 5: UI — Route view, navigation, connected resources grid
**Module:** `datamanager-app`

1. **Route constant** in `AppRoutes.ProjectRoutes`:
   ```java
   public static final String DATASETS = "projects/%s/datasets";
   ```

2. **`AssociatedDatasetsMain`** — `@Route(value = "projects/:projectId?/datasets", layout = ProjectMainLayout.class)`:
   - Extends `Main` (following the `ProjectInformationMain` pattern).
   - Contains: "Connected Resources" section + "Connect Datasets" button.
   - Connected resources grid (simplified for this iteration — shows public datasets with basic properties: Title, PID/DOI, Access Status, Version, Access Link, Published Date).
   - Follows the layout from `AssociatedDatasetsDemoV2.java`.
   - **Empty state** — when no datasets are connected, the grid is replaced by an empty state component (see `ConnectedResourcesComponent` below).

3. **`ConnectedResourcesComponent`** — the grid + filter bar. Uses Vaadin Grid's lazy loading (`setItems(Query)`) against the local DB for consistent behaviour with the search sidebar. No access filter needed for public-only (this iteration), but include the component structure for future expansion.

   **Empty state:** When `connectedResources.isEmpty()`, replace the filter bar and grid with an empty-state component. Contents:
   - Large muted icon (`VaadinIcon.DATABASE` or `VaadinIcon.GLOBE`)
   - Heading: "No datasets connected"
   - Explanatory text (1–2 sentences) — what this section does and what a connected dataset means
   - Primary CTA: "Connect Datasets" button (`LUMO_PRIMARY`) — same action as the ActionBar button, opens the connect sidebar
   - Optional secondary line: a one-liner describing the flow ("Search InvenioRDM repositories, select datasets, and connect them to this project.")

   The empty state disappears immediately after the first successful connect (grid refresh replaces it). The ActionBar "Connect Datasets" button is always visible, so the entry point exists even before the empty-state CTA.

   **Empty state:** When `connectedResources.isEmpty()`, the filter bar and grid are replaced by an empty-state component containing:
   - A large muted icon (`VaadinIcon.DATABASE` or `VaadinIcon.GLOBE`)
   - Heading: *"No datasets connected"*
   - Explanatory text (1–2 sentences): what this section is for (connecting external InvenioRDM datasets to a project)
   - Primary CTA: *"Connect Datasets"* button (`LUMO_PRIMARY`) — same action as the ActionBar button (opens the connect sidebar)
   - Optional secondary text: a one-liner describing the sidebar flow ("Search InvenioRDM repositories, select datasets, and connect them to this project.")

   The empty state disappears immediately after the first successful connect (grid refresh replaces it).

4. **`ConnectDatasetSidebar`** — sliding sidebar panel:
   - Instance selector (ComboBox populated from `InvenioRdmInstanceProperties`).
   - Search field (TextField + Enter key listener).
   - Search results as a lazily-loaded Vaadin Grid (single column with card-style rendering per the prototype). Uses InvenioRDM's offset-based pagination (`page` + `size` query params, mapped from Vaadin's `Query.getOffset()` / `getLimit()`) — no visible pagination controls in the UI. Zenodo enforces max 25 results/page for anonymous, 100 for authenticated.
   - Multi-selection + "Connect Selected" button.
   - Optional experiment association: a ComboBox (populated from the project's experiments) in the sidebar footer area, visible before "Connect Selected". The user can select an experiment to link the connected dataset to. Selection is optional — if left empty, the dataset is connected without a specific experiment link.

5. **Navigation entry** in `ProjectSideNavigationComponent`:
   - Add a `SideNavItem("DATASETS", datasetsPath, VaadinIcon.DATABASE.create())` below the Summary link.
   - Visible to all users with project access (READ permission on project, per ADR-0003).

### Task 6: Notification on connect
**Module:** `project-management` (domain event + policy directive) + `datamanager-app` (toast)

Per ADR-0003, notification model is:
- **Actor** (the user who connected) → sees a Vaadin toast immediately (UI layer).
- **Other project members** → receive email, dispatched asynchronously via `JobScheduler`.

**Both actor toast and project-member email are in scope for this iteration.**

The codebase already has the complete prerequisite chain:
- `ProjectAccessService.listCollaborators(projectId)` → `List<ProjectCollaborator>`
- `UserInformationService.findById(userId)` → user info with email address
- `EmailService.send(Subject, Recipient, Content)` → email delivery
- `JobScheduler.enqueue(...)` → async dispatch (JobRunr)

**Implementation:**

1. **Domain event:** `AssociatedDatasetConnectedEvent` (emitted by aggregate in Task 1). Carries `projectId`, `actorUserId`, `datasetTitle`, `datasetPid`.

2. **New policy directive:** `InformProjectMembersAboutDatasetConnection implements DomainEventSubscriber<AssociatedDatasetConnectedEvent>`:
   - Resolves collaborators: `ProjectAccessService.listCollaborators(projectId)`
   - Filters out the actor (ADR-0003: actor already saw the toast)
   - For each remaining collaborator: resolves email via `UserInformationService.findById()`
   - Dispatches each email asynchronously: `JobScheduler.enqueue(() -> emailService.send(...))`
   - Email content via `Messages` pattern (i18n-ready): subject "New dataset connected to project", body with dataset title, PID, and link to datasets view

3. **Actor toast** in `AssociatedDatasetsMain`: success toast shown after the connect call returns.

**Reference pattern:** `InformUsersAboutBatchRegistration` — same event-subscribe → collaborator-lookup → async-email flow.

### Task 7: Tests

- **Unit tests** (Spock):
  - `AssociatedDatasetSpec` — aggregate behavior (connect, remove, state transitions).
  - `AssociatedDatasetServiceSpec` — use-case orchestration, port interaction.
  - `InvenioRdmClientSpec` — HTTP response parsing, retry logic, error mapping.
- **Integration tests**:
  - `AssociatedDatasetIT` — JPA persistence round-trip.
  - WireMock-based test for InvenioRDM API interaction (if feasible).

---

## 5. Database Schema Changes

New table `associated_dataset` in the data-management datasource:

```sql
CREATE TABLE associated_dataset (
  id                VARCHAR(36)   NOT NULL PRIMARY KEY,
  project_id        VARCHAR(36)   NOT NULL,
  source_type       VARCHAR(32)   NOT NULL,              -- 'INVENIO_RDM'
  external_handle   VARCHAR(255)  NOT NULL,              -- record ID on the source
  connection_state  VARCHAR(16)   NOT NULL DEFAULT 'CONNECTED',
  access_level      VARCHAR(16)   NOT NULL,              -- 'PUBLIC' | 'RESTRICTED'

  -- Universal columns for sort/filter
  title             VARCHAR(1024),
  pid               VARCHAR(255),
  version           VARCHAR(32),
  publication_date  DATE,

  -- Source-specific metadata (opaque JSON)
  resource_metadata JSON,

  -- Connection metadata
  connected_by      VARCHAR(255)  NOT NULL,
  connected_on      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  experiment_id     VARCHAR(36),                         -- nullable
  last_synced_at    DATETIME,

  INDEX idx_assoc_ds_project (project_id),
  INDEX idx_assoc_ds_state (connection_state),
  INDEX idx_assoc_ds_source (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

Notes per ADR-0001:
- Universal columns (`title`, `pid`, `version`, `publication_date`) are regular columns for SQL-sortable fields.
- Source-specific metadata is stored in a `JSON` column (MariaDB, not JSONB).
- Soft delete: `connection_state = 'REMOVED'` is the tombstone.

---

## 6. Configuration Changes

Add to `application.properties`:

```properties
# InvenioRDM instances (admin-configured, per ADR-0002 I2)
qbic.external-service.invenio-rdm.instances[0].name=Zenodo
qbic.external-service.invenio-rdm.instances[0].url=https://zenodo.org
qbic.external-service.invenio-rdm.instances[1].name=FDAT
qbic.external-service.invenio-rdm.instances[1].url=https://fdat.uni-tuebingen.de
```

For `development` profile: provide a mock adapter for the `DatasetSource` port so local dev does not require external network access.

---

## 7. UI Design Decisions (derived from prototype + ADRs)

| Decision | Source |
|---|---|
| Sidebar approach for connect (not modal) | Prototype V2 feedback |
| Connected Resources grid with expandable detail rows | Prototype V2 |
| "DATASETS" nav link below "SUMMARY" | User request |
| Instance selector as ComboBox in sidebar | Prototype + ADR-0002 (I2) |
| Card-style search results (single-column grid) | Prototype V2 |
| Toast on connect success/failure | ADR-0003 (N1 — actor toast) |
| No credentials/token section in UI (deferred to Stories 14/15) | Pragmatic scope decision |
| No access filter initially (public-only searchable in this iteration) | Pragmatic scope decision |
| Experiment association as optional ComboBox in connect sidebar | AC9 |
| Empty state in Connected Resources when no datasets connected | UX / onboarding decision |

---

## 8. Order of Implementation

1. **Domain model** (Task 1) — foundation, no external dependencies
2. **Application service + port** (Task 2) — depends on domain
3. **Unit tests for domain + application** (spoke alongside 1, 2)
4. **Infrastructure: InvenioRDM client** (Task 3) — depends on port
5. **Infrastructure: JPA persistence** (Task 4) — depends on domain
6. **UI: Route + navigation + grid + sidebar** (Task 5) — depends on application service + infrastructure
7. **Integration tests** (Task 7) — depends on everything wired
8. **Notification: email directive** (Task 6) — domain event + policy directive following existing `InformUsersAboutBatchRegistration` pattern

---

## 9. Risk Items and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| InvenioRDM API rate limits (Zenodo: 2 req/s for anonymous) | Search failures | Implement retry with backoff; show user-facing error; consider caching. |
| Project-member email lookup missing | ~~Not a risk~~ — `ProjectAccessService` + `UserInformationService` already provide the lookup. | Already available in codebase. |
| MariaDB JSON column query performance | <100 rows/project — acceptable per ADR | Accept risk; index optimization deferred. |
| No mock for InvenioRDM in `development` profile | Local dev requires external access | Build a mock adapter in the `development` profile. |
| Existing `dataset` package in project-management-infrastructure | Name collision risk | Use `dataset/associated` sub-package per ADR-0001 guidance to distinguish from OpenBIS raw data. |

---

## 10. Assumptions

1. The "DATASETS" navigation link is visible to all users with READ access to a project (per ADR-0003 ACL table).
2. Only public datasets are searched/connected in this iteration (no token needed).
3. The InvenioRDM instances are hardcoded in configuration (Zenodo + FDAT) per ADR-0002 (I2).
4. Project-member email notifications are **in scope** — the `ProjectAccessService` + `UserInformationService` + `EmailService` + `JobScheduler` chain already exists and is proven by `InformUsersAboutBatchRegistration`.
5. The UI follows the prototype V2 layout; the sidebar is the chosen approach for connecting datasets.
6. Experiment association is optional and implemented as a dropdown in the connect sidebar.
7. Requirement ID management is a Product Owner concern — not part of this development task.
