# Project page rework — implementation strategy (analysis + proposal)

> **Provenance.** AI-authored analysis, 2026-10-14, produced by a code read of the current
> production views, services and schema plus the existing analysis in
> [`Project-management-refactor.md`](Project-management-refactor.md) and the development-only
> prototype (`views/demo/ProjectPagePrototypeDemo`). It is a **proposal**, not an approved decision.
> It does not discharge the governance gate in the parent plan.
>
> **Declared scope of this document.** The parent plan answers *what the page should be*. This
> document answers *how to build it against the code we have*, and — the part the prototype could
> not see — *what has to change below the UI layer*. It deliberately treats the prototype as
> evidence, not as a starting codebase (parent plan: "the prototype is evidence, not
> specification").
>
> **Scope clarification (confirmed by the project owner, 2026-10-14):** the current production
> project page is **replaced, not refactored in place**. `ProjectMainLayout`'s project chrome, the
> `ProjectSideNavigationComponent` drawer, `ProjectInformationMain`, `ProjectSummaryComponent` and
> the three right-rail panels (`ExperimentListComponent`, `OfferListComponent`,
> `QualityControlListComponent`) are **retired**. The prototype is the **orientation for the new
> layout**, not a codebase to port. What is retained is the infrastructure *below* the page: the
> project list, the paginated grid / list-state / selection primitives, and the application and
> service layers.

---

## 1. Fit with what already exists (what we inherit, what we must not rebuild)

Stakeholder constraint: the project **list** is already engineered; the **grid** with filter and
selection is already refactored for samples and measurements; the sidecar is only needed for the
**detail of a selected resource**. This strategy is built around not rebuilding any of it.

### 1.1 Reuse — project list (no change)

| Reused | Where | Evidence |
|---|---|---|
| Paginated project overview | `ProjectOverviewMain` + `ProjectCollectionComponent` | list state, history handler, `PaginationBar`, card grid already wired |
| List-state codec | `ListState`, `ListStateCodec` (ADR-0007) | `page/size/q/sort` in URL |
| Count query for pager | `ProjectInformationService.countOverview` | exists |

### 1.2 Reuse — grid/filter/selection (no change, extend at the edge)

| Reused | Where |
|---|---|
| Generic paginated grid | `PaginatedGrid<T>` (`PageLoader`, `Page`, `IdExtractor`, `select`/`deselect`, `listState()`) |
| Immutable list state + URL codec | `ListState`, `ListStateCodec` |
| Identifier-based cross-page selection | `Selection` (ADR-0009) |
| Pager | `PaginationBar` |
| Reference implementations | `SampleDetailsComponent`, `MeasurementDetailsComponent`, `RawDataMain`/`RawDataTabPagination` |

**Decision:** the project-level `Samples` and `Datasets` surfaces are *new instantiations of the
existing `PaginatedGrid` pattern*, not new grid code. The only grid work is a **preview/facet
toolbar** for filters the current `ListState` cannot express (see §3.3 and §5.4).

### 1.3 Reuse — sidecar

Production has **bespoke** sidebars (`ConnectDatasetSidebar`, `SyncResultsSidebar`,
`VerificationSidebar`, `ProjectSharingDrawer`) but no reusable "detail of the selected record"
panel. The prototype's `.pt-overlay` / `.pt-panel` is the first generic one. **Decision:** build
**one** reusable detail panel primitive and use it for sample and measurement/dataset detail; do
not grow another bespoke sidebar per surface.

### 1.4 Do not rebuild

- The current project page itself is **removed**, not reused: its layout, rails and `@UIScope`
  state model are not carried into the new page.
- No new grid, pager, selection model or list-state codec.
- No new project list.
- No production import of `views.demo` (`PrototypeIsolationSpec` enforces this).

---

## 2. Target surfaces → routes → read model → API

The prototype navigates one dev route with `?view=…&experiment=…`. Production needs one real route
per surface (parent plan §"Routing and URL-addressable state"). Mapping against the routes that
exist in `AppRoutes.ProjectRoutes`:

| Prototype surface | Existing route | Action | Scope |
|---|---|---|---|
| Home / projects list | `projects/list` | reuse | project-independent |
| Project `Summary` | `projects/%s/info` (`ProjectInformationMain`) | **replace** (page retired) | project |
| Project `Samples` | **none** (only experiment `.../samples`) | **new route** `projects/%s/samples` | project |
| Project `Datasets` | `projects/%s/datasets` (connected datasets only) | **extend/replace** with produced + connected | project |
| Project `Access` | `projects/%s/access` | reuse | project |
| Project `Settings` | **none** | **new route** `projects/%s/settings` | project |
| Experiment `Overview` | `projects/%s/experiments/%s` (`ExperimentInformationMain`) | **replace** with overview landing | experiment |
| Experiment `Samples` | `projects/%s/experiments/%s/samples` | reuse | experiment |
| Experiment `Dataset` (merged Measurements + Raw data) | `.../measurements` + `.../rawdata` | **merge** (one route, likely `.../dataset`) | experiment |

Route collisions to resolve deliberately: `projects/%s/datasets` currently means *connected*
datasets; the plan's `Datasets` page absorbs it. `.../measurements` and `.../rawdata` remain as
back-compatible redirects while the merged `Dataset` view is introduced.

---

## 3. Service-layer analysis

### 3.1 The two access styles (the root architectural pain)

`AsyncProjectService` documents itself as "the Service API layer the user interface code shall
interact with", and `service_api.md` prescribes the request/response + `requestId` shape. In
practice the UI also injects application services **directly**:

| View | Direct dependency (not via `AsyncProjectService`) |
|---|---|
| `ProjectInformationMain` | `ProjectPurchaseService`, `QualityControlService` |
| `ProjectSummaryComponent` | `ProjectInformationService`, `PersonLookupService`, `AsyncProjectService` (mixed) |
| `ProjectInformationMain` / `ProjectSideNavigationComponent` | `ExperimentInformationService`, `AddExperimentToProjectService` |
| `SampleInformationMain` | `MeasurementService`, `SampleInformationService`, `DeletionService` |
| `ProjectInformationService` | `ProjectOverview` (JPA view entity) |

The rework multiplies **read-only projections**. Without a decision, it will add more ad-hoc
service calls from views. This is the single most important below-UI decision (§4).

### 3.2 What the read side already provides

| Capability | Service / method | Scope | Notes |
|---|---|---|---|
| Project identity + totals | `ProjectInformationService.find(ProjectId)` / `findOverview` → `ProjectOverview` | project | JPA entity over SQL view `project_overview` |
| Project measurement totals | `ProjectOverview.ngs/pxp/ipMeasurementCount` | project | from `project_measurements` view |
| Connected-dataset rollup + `lastConnectedOn` | `ProjectOverview.connected/open/restrictedDatasetCount`, `lastConnectedOn` | project | from `associated_dataset` |
| Experiments | `ExperimentInformationService.findAllForProject`, `AsyncProjectService.getExperiments` | project | full aggregates / descriptions |
| Samples (entities) | `SampleInformationService.retrieveSamplesForProject`, `AsyncProjectService.getSamplesForProject` | project | **no pagination** |
| Sample previews (paginated) | `SampleInformationService.querySamplePreview`, `AsyncProjectService.getSamplePreviews` + `countSamples` | **experiment** | `SamplePreviewLookup` is experiment-keyed only |
| Sample per-experiment count | `SampleRepository.countSamplesWithExperimentId` | experiment | one query per experiment |
| Measurement count | `MeasurementLookupService.countMeasurements(project, experiment)` | experiment | runs 3 type counts |
| Measurement metadata (paginated) | `NgsMeasurementLookup` / `PxpMeasurementLookup` / `IpMeasurementLookup` | experiment | `MeasurementFilter.experimentId` is `@NonNull` |
| Primary data (paginated + count) | `LocalRawDatasetLookupService.findAll*` / `countAll*` | experiment | `RawDatasetFilter` |
| Primary data presence | `RemoteRawDataService.hasRawData(project, experiment)` | experiment | **boolean only** |
| Connected datasets (list) | `AssociatedDatasetService.listConnectedDatasetViews` | project | |

### 3.3 Gap register (what the new surfaces need and where it is missing)

| # | New capability | Exists today | Gap | Proposed endpoint / artifact |
|---|---|---|---|---|
| G1 | **Per-experiment progress** (`experiment → samples → measurements → primary data`) for the summary pipeline and attention rollup | only per-experiment counts, one query each; 3 measurement types + 3 primary-data counts + 1 sample count ≈ **7 queries/experiment** | no batched, per-experiment progress read model | new query method returning `List<ExperimentProgress>`; backed by a new SQL view (e.g. `experiment_progress`) |
| G2 | **Project-level paginated sample list** with Experiment column + quick-filter per experiment | `SamplePreviewLookup` is experiment-keyed | no project-scoped paginated preview query + count | new project-scoped lookup method (+count); reuse `PaginatedGrid` |
| G3 | **Sample status** (`Measured`/`Not measured`) as a list column and stat | `MeasurementService.findMeasuredSampleIds` only for a supplied ID set | no bulk status projection | add measured-status to the project/experiment sample preview projection |
| G4 | **Project Datasets page**: one row per experiment (`primary` kind, measurement count, primary-data completeness, files, size, status) | scattered per-experiment counts | no per-experiment dataset aggregation | new query method; same view as G1 or a sibling (`experiment_dataset_rollup`) |
| G5 | **Merged experiment `Dataset` view** (measurement metadata + primary data together) | measurement lists and raw-data lists exist separately | no joined measurement+primary-data row | extend measurement lookups with primary-data fields, or a new merged projection |
| G6 | **Project-wide measurement filter** | `MeasurementFilter.experimentId` mandatory | cannot query across experiments | make `experimentId` optional, or add project-scoped lookup methods |
| G7 | **Primary-data completeness excluding `deleted`** | `hasRawData` boolean; `countAll*` count raw-data entries | need "measurements without primary data" as an integer, `deleted=false` | define and implement the count semantics in one place |
| G8 | **Recency previews** (last 3 registered/modified) for inline snapshots | `SamplePreviewSortKey.REGISTRATION_TIME`/`MODIFICATION_TIME` | measurements have `registrationDate` but **no last-modified** | reuse sample sort keys; measurement preview ranks by registration; last-modified for measurements = schema change (defer) |
| G9 | **Connected assets "new since you last looked"** | `lastConnectedOn` (project-wide) | no per-user last-seen | new persisted user preference + service (cf. ADR-0008 user preferences) |
| G10 | **Multi-facet URL state** (experiment quick-filter, measurement-type tab, power mode, selected record) | `ListStateCodec` covers only `page/size/q/sort` | no codec for view-specific params | extend the codec or add a `ProjectViewParams` codec; requirement gap (USER-R-03 covers list state only) |

### 3.4 Explicit answers to "do we need new services / endpoints?"

- **Yes — a dedicated read/query service for the project page**, with a reactive contract but a
  blocking implementation over JPA. Not an extension of `AsyncProjectService` (see §4).
- **Yes — at least one new SQL projection/view** for per-experiment progress (G1/G4). This is a
  **schema change → human approval (AGENTS §12)** and an ADR (progress read model).
- **Yes — one project-scoped sample-preview query** (G2/G3) and one merged measurement+primary-data
  projection (G5).
- **Yes — one reusable UI-side detail panel** (no service change; reuses existing single-record
  reads such as `SampleInformationService.findSample`, `findNGSMeasurementById`, etc.).
- **No** — no new grid, no new pager, no new selection model, no new project list.
- **No** — the project `Summary` identity/metadata section can keep using `ProjectInformationService`
  and the existing `AsyncProjectService` update endpoints.

---

## 4. Recommended read-side architecture

**Do not extend `AsyncProjectService`.** It is already 2 870 lines with ~40 nested records and mixes
project CRUD, experiment CRUD, sample registration, measurement registration/validation, ontology
lookup, template generation and raw-data queries. Adding progress/preview/aggregation to it grows a
god interface and conflates **commands** (write, reactive, `requestId`-oriented) with **queries**
(projection reads for rendering).

**Proposal:** introduce a dedicated **application-layer query service** for the project page
(working name `ProjectPageQueryService`, or a `projectmanagement.application.query` package),
returning explicit **read-model records** (not JPA entities, not domain aggregates):

```
record ExperimentProgress(String experimentId, String name, Set<String> domains,
                          int sampleCount, int measurementCount,
                          int measurementsWithPrimaryData, long primaryDataFiles,
                          long primaryDataBytes, ProgressState state) {}

record ProjectSamplesPage(...);   // stats + page of sample rows
record ExperimentDatasetRow(...); // per-experiment dataset aggregation
record MeasurementDataRow(...);   // measurement + primary-data fields
record RecentRow(...);            // recency preview
```

Rules it must follow (so it stays honest):

1. **Bounded context:** it lives in `project-management/application`, talks to domain repositories
   and lookups, and never reaches into `datamanager-app` or JPA directly.
2. **Security:** every method is `@PreAuthorize`-guarded on `Project` `READ`, exactly like
   `SampleInformationService`. A batch read model must not become an authorization bypass.
3. **One query per surface, not N+1.** The progress/dataset rollups are served by a single SQL
   projection rather than composing 7 per-experiment calls. For a landing page this is a
   correctness (consistency) *and* performance requirement.
4. **Read models are flat, and may be view-backed entities.** A read model may be a managed
   `@Immutable @Entity` bound to a SQL view — the established `ProjectOverview` pattern — **only
   if it carries no lazy associations**, so it is fully hydrated before any thread hop. A shape
   that is not flat must be a detached record/DTO. Never add an association to a view-backed read
   entity. See *4.2 Resolved decisions* for the chosen mapping mechanism.
5. **`AsyncProjectService` stays the command boundary.** Writes (create experiment, register
   samples, edit metadata, project update/delete) keep their current shape. If a write is reused by
   the new page, call the existing endpoint — do not duplicate it.

**Open decision (ADR):** query service vs. extending the existing read services
(`ProjectInformationService`, `SampleInformationService`). The strategy recommends a new service;
the ADR must record why, and how it relates to `AsyncProjectService` as the documented UI API.

### 4.1 Reactive boundary policy (over a blocking JPA stack)

Agree with a reactive contract, but the async claim needs to be precise. Facts from the current
code:

- `PaginatedGrid.PageLoader<T>` is **synchronous** (`Page<T> load(ListState)`); Vaadin's data
  provider cannot consume a `Mono`.
- `SampleDetailsComponent.loadPage` already bridges with `.blockOptional()` /
  `.collectList().blockOptional()` — there are **8 `.block*()` sites in the views**.
- `AsyncProjectServiceImpl` carries **0 `@PreAuthorize`**, uses `subscribeOn(elasticScheduler)`, and
  delegates to **21 application-service files** that carry `@PreAuthorize`; it bridges the security
  context with `ReactiveSecurityContextUtils.applySecurityContext`.

Consequences for the query service:

1. **JPA is blocking — Reactor here is thread-offload and fan-out, not a non-blocking stack.**
   Without R2DBC the DB call blocks a worker thread. That is acceptable; call it what it is.
2. **The pager cannot be end-to-end async.** A reactive query is `.block()`ed in the page loader
   exactly as today unless `PageLoader` changes. Reactor pays off for fan-out aggregation (the
   progress band), streaming (exports) and external I/O — not for a single paginated read.
3. **Security context is thread-bound.** Every query method must propagate the security context
   (reuse `ReactiveSecurityContextUtils`) or perform its ACL check at the query boundary; otherwise
   `@PreAuthorize`/ACL evaluates against no principal after a scheduler hop. This is a silent
   authorization bug, not a compile error. **Hard rule.**
4. **Read models must be fully hydrated before the thread hop.** A managed entity is not
   forbidden — `ProjectOverview` is one — but it must be **flat** (no lazy associations), because a
   scheduler hop with an uninitialised proxy causes `LazyInitializationException`. This is the same
   requirement as §4 clause 4. **Hard rule.**
5. **One shared scheduler.** Reuse `elasticScheduler`; do not create per-service schedulers, and be
   deliberate about `subscribeOn` vs `publishOn`.
6. **Do not mirror the command ceremony.** `requestId`, sealed request/response and `Retry` are for
   writes; a query takes a typed input and returns a read model.
7. **A thin Vaadin adapter.** `Mono` → `CompletableFuture` + `UI.access`, in one shared place, so
   views stop hand-rolling `.block()` (and the 8 existing sites can migrate onto it).

**Accepted trade-off — defer the async pager.** The synchronous `PaginatedGrid.PageLoader` is kept
as-is. Blocking one page query on the UI thread is the status quo (8 sites already do it) and is not
a regression; a reactive query service does not make it worse. Deferring is safe **only while two
cheap disciplines hold now**:

- **One bridge, at the edge.** All reactive→synchronous conversion happens in exactly one shared
  adapter, and services never call `.block()`. If the blocking scatters across views, "refactor
  later" becomes many refactors.
- **Read models stay fully hydrated and flat** (hard rule 4), so the JPA call is safe on whatever
  thread executes it.

Because the progress band is a single projection rather than a fan-out (clause 3), the blocked query
stays one fast read. **Revisit the async loader when** UI-thread time becomes measurable: slow list
loads, request/heartbeat timeouts, or a list query that performs external I/O. At that point only
the adapter changes, not the query service.

**Verdict:** the architecture is *a query service whose **contract** is reactive*, not *an async
query service*. Over this stack the async is thread-offload plus parallel composition; any claim
that paginated reads are non-blocking is contradicted by the code.

### 4.2 Resolved decisions (2026-10-14)

- **D4 — Query service shape:** a new application-layer query service (working name
  `ProjectQueryService`), not an extension of `ProjectInformationService` or
  `SampleInformationService`. To be recorded as an ADR.
- **D5 — `AsyncProjectService` read ownership:** keep it as the command boundary; freeze new
  project-level reads onto the query service and do **not** churn the existing experiment-scoped
  reads (`getSamplePreviews`, `getRawDatasetInformation*`, `countRawData*`) in this effort; migrate
  them opportunistically later.
- **D6 — View mapping mechanism:** follow the `ProjectOverview` pattern — a flat
  `@Immutable @Entity` (Spring Data repository + `Specification`) per view. Records / `JdbcClient`
  remain the fallback for non-flat shapes, not the default. No second persistence idiom is
  introduced. (Corrects the earlier over-strict "entity-free" wording: the real rule is *flat, no
  lazy associations*. `ProjectOverview` is itself a managed entity and is safe for exactly that
  reason.)
- **D7 — Reactive→sync bridge:** one shared adapter in `datamanager-app`; the existing eight
  `.block()` sites migrate onto it over time.
- **D8 — Cross-experiment measurement counting:** a measurement is counted in **every** experiment
  it measures samples of (per-experiment attribution). Consequence: project totals may double-count
  a cross-experiment measurement. **Finding for the PO:** `MeasurementService.isSampleFromProject`
  checks *project* membership only, not a single experiment, so the plan's "a dataset is 1:1 with
  its experiment" is product intent, not a rule — decide whether to accept cross-experiment
  measurements or add a registration guard.
- **D9 — Primary-data counting:** `remote_measurement_data` is 1:1 with a measurement
  (`LocalRawDatasetRepositoryImpl.saveAll` upserts one row per measurement, keyed by
  measurementCode, and re-sync resets `deleted = false`). `measurements_with_primary_data` = the
  number of measurements with a non-deleted data row.
- **D10 — Read-model source of truth:** SQL views over the same database (strongly consistent; no
  separate read store, no event-driven projections). Composing in the application layer was
  rejected (N+1 and inconsistent numbers between bands).
- **D11 — Indexing:** keep the single `idx_sample_experiment`; add a composite
  `sample(project_id, experiment_id)` only if `EXPLAIN` during validation shows a scan. **Closed
  (2026-10-14):** validation `EXPLAIN` showed the index is used, so no composite index is added.

### 4.3 Schema validation (2026-10-14)

The read-model migration was applied to the central test schema and the checks in *Verify*
(`NEXT.md`) plus the read-model cross-checks were run. All results clean:

- all three views created;
- `experiment_progress` agrees with the existing `project_measurements` view per project
  (measurement-count cross-check returned no mismatches);
- `sample_count` matches the raw `sample` table per experiment (no mismatches);
- `measured + not_measured = total` in `v_project_sample_overview`, no inconsistent rows;
- `measurements_with_primary_data <= measurement_count` everywhere;
- `remote_measurement_data` contains `deleted = true` rows, confirming the filter is load-bearing;
- `EXPLAIN` showed index usage → **D11 closed**.

**Provision note:** validation was executed by the project owner against the central test DB; the
agent has no database credentials. The migration remains additive and idempotent, so re-running it
is safe.

---

## 5. Below-UI pain points the prototype could not see

Severity: **H** blocks delivery · **M** costly if ignored · **L** hygiene.

| # | Pain point | Sev | Consequence if ignored | Action |
|---|---|---|---|---|
| P1 | UI reads through two different service styles; no read-model convention | H | every new band adds another ad-hoc service call; no testable seam | ADR + `ProjectPageQueryService` (§4) |
| P2 | `AsyncProjectService` god interface (2 870 lines) | H | unbounded growth; commands and queries entangled | keep as command boundary; freeze new reads onto the query service and migrate existing ones (§4) |
| P3 | `ProjectOverview` is a JPA entity over a SQL view; progress needs new columns/views | H | landing page becomes an unbounded view; schema change needs approval | new projection view + approval (AGENTS §12) |
| P4 | No per-experiment progress projection → N+1 (≈7 queries/experiment) | H | slow, inconsistent numbers between bands | single SQL projection (§3.3 G1) |
| P5 | `remote_measurement_data` joins measurements to attribute to experiment; `deleted` must be excluded | H | completeness claims are wrong | define the count semantics once (G7) |
| P6 | "dataset" vocabulary collision: `AssociatedDataset` vs `LocalRawDatasetEntry` / `remote_measurement_data` | M | UI names and code disagree; renames leak into API | execute the plan's decided vocabulary; class rename is code; **table rename is schema → approval**; phase it |
| P7 | Route collisions (`datasets`, measurement vs dataset, no project `samples`/`settings`) | H | broken deep links / back-compat | explicit route map + redirects (§2) |
| P8 | `ListState` has a single free-text filter; new surfaces need facets | M | facets become lossy client state, not shareable | extend codec (G10); requirement gap |
| P9 | Two layouts build different chrome (`ProjectMainLayout` opens drawer, `ExperimentMainLayout` not; different navbar DOM) | H | chrome shifts on navigation even before the drawer is removed | converge to one shell; ADR for navigation model |
| P10 | Removing the drawer touches shared shell (`DataManagerLayout`, `TopLevelNavigationComponent`, `DataManagerMenu`, `HomeLink`, footer, test-instance banner) | H | broad blast radius, regressions on every page | shell-level ADR + isolated rollout |
| P11 | `ProjectSummaryComponent` is `@UIScope @SpringComponent` with mutable section/detail-box fields mutated by async callbacks | M | fragile reloads inherited by the rework | move to route-scoped state + explicit read models |
| P12 | Duplicated add-experiment orchestration in `ProjectInformationMain` and `ProjectSideNavigationComponent` | M | two divergent flows | extract one shared component/service |
| P13 | Cross-context calls from the summary (`ProjectPurchaseService` from `finances`) | M | the settings/Offers move is a context move | move Offers to Settings; keep the API boundary |
| P14 | Measurement recency needs a last-modified timestamp that does not exist | L | preview ranks by registration only | defer; document the divergence (G8) |
| P15 | Per-user "last looked" does not exist | M | "new since you last looked" cannot be built | new preference + service (G9); ADR/requirement |
| P16 | Governance: zero `PROJECT` requirements, no `FEAT-PROJECT` Feature/Stories, no ADRs | H | implementation may not start (AGENTS §0/§12) | author requirements + Feature/Stories + ADRs first |
| P17 | Prototype boundary must hold during production work | M | prototype code copied into production | keep `PrototypeIsolationSpec`; never import `views.demo` |

---

## 6. The sidecar (detail of a selected resource)

**Decision:** one reusable detail panel, opened from the grid row, never a bespoke sidebar.

- **Interaction:** row click opens the panel; the list (page, filter, selection) is preserved.
  Deep-link the selected record via query parameter so a record is shareable (ties into G10;
  new URL-addressable state beyond USER-R-03 — requirement decision required).
- **Content:** grouped by the stakeholder spec categories (Sample Identity / Properties /
  Provenance; Measurement Provenance / Instrument / Run Parameters / Notes), matching the
  prototype `Detail`/`DetailGroup` shape.
- **Data:** single-record reads that already exist (`SampleInformationService.findSample`,
  `MeasurementLookupService.find*ById`). No list-wide join needed.
- **Accessibility (carried from the plan):** operability by pointer **and** Enter/Space; state
  never carried by colour alone; a card containing a real button is not itself `role="button"`.
- **Not** a Vaadin `Dialog` and **not** a route: it must keep list context, which is the whole
  point. It is not the settings page.

---

## 7. Phased delivery plan

Each phase ends with something reviewable and keeps the app shippable. Phases 0–1 are prerequisites
for everything else.

### Phase 0 — Governance and decisions (no code)

1. Author `PROJECT-R-*` / `PROJECT-NFR-*` requirements (dedicated requirement-only PR, human
   approval, mandatory changelog — AGENTS §0/§12).
2. Create parent Feature + Stories (`FEAT-PROJECT-*`) in `docs/features.md` and GitHub, each with
   acceptance criteria.
3. ADRs: (a) navigation model / shell, (b) project-page read model + query service, (c) dataset
   vocabulary, (d) whether to extend USER-R-03 for facet/selected-record URL state.
4. Get schema-approval for the progress projection (P3/G1) and decide the per-user last-seen model
   (G9).

### Phase 1 — Shell and navigation (no data change)

- Converge `ProjectMainLayout` and `ExperimentMainLayout` on one navbar (P9).
- Introduce the scope-contextual navigation row; retire the drawer (P10).
- Keep all existing routes working; add redirects only after the new routes exist.

### Phase 2 — Project summary read model

- Add `ProjectPageQueryService` + `ExperimentProgress` (G1/G4) and the SQL projection.
- Build the replacement for `ProjectSummaryComponent`: an identity band + progress band + inline
  experiment snapshots, consuming the read model (P11). The old component is retired, not edited.
- Attention rollup per experiment derived from the same read model.

### Phase 3 — Project-level lists

- New route `projects/%s/samples` (G2/G3), reusing `PaginatedGrid`/`ListState`.
- Rework `projects/%s/datasets` into produced + connected (G4/G5).
- Merge experiment Measurements + Raw data into the `Dataset` view (G5/G6).
- Extract the reusable detail panel and wire row selection (§6).
- Extend URL state for facets (G10) once the requirement is decided.

### Phase 4 — Settings, creation, vocabulary

- New `projects/%s/settings` route; move funding/contacts/Offers/delete (P13).
- Dedicated `projects/create` page.
- Execute the dataset vocabulary rename in code; schedule the table/database rename separately
  behind schema approval (P6).

---

## 8. Approval and sequencing checklist

| Item | Approver | Blocks |
|---|---|---|
| New `PROJECT-*` requirements | human reviewer | all implementation (AGENTS §0/§12) |
| Feature + Stories in `docs/features.md` | PO | all implementation |
| Navigation/shell ADR | human reviewer | Phase 1 |
| Read-model / query-service ADR | human reviewer | Phase 2 |
| Reactive boundary / read-API ADR (query contract, security propagation, scheduler, hydration) | human reviewer | Phase 2 |
| Dataset vocabulary ADR | human reviewer | Phase 3/4 |
| New SQL view(s) / columns | human reviewer (schema) | Phase 2 (G1/G4), G9 |
| URL-state requirement extension | PO | Phase 3 facets |
| Table rename | human reviewer (schema) | Phase 4 |
| Reusable detail panel | design-system repo review | Phase 3 |

---

## 9. Risks and open questions

1. ~~Read-model source of truth~~ — **resolved (D10):** SQL views, consumed as flat `@Immutable`
   read entities. Composing in the application layer was rejected (N+1 and inconsistent numbers).
2. ~~Primary-data count semantics~~ — **resolved (D9):** `remote_measurement_data` is 1:1 with a
   measurement; `measurements_with_primary_data` counts measurements with a non-deleted data row.
3. **Measurement last-modified:** recency preview for datasets ranks by registration only until a
   modification timestamp exists. Acceptable, or a schema change?
4. **Per-user last-seen:** new persistence; where does it live (user preference vs project state)?
   ADR-0008 establishes that user preferences are not project state — this belongs on the user side.
5. **Query service vs. existing services:** the ADR must decide whether `ProjectInformationService`
   is extended or a new service is introduced, and how this reconciles with `AsyncProjectService`
   as the documented UI API (`service_api.md`). Reads currently on `AsyncProjectService`
   (`getSamplePreviews`, `getRawDatasetInformation*`, `countRawData*`, `getExperiments`,
   `getProject`) need a single owner — freeze new reads on the query service and migrate the
   existing ones deliberately, or the two will diverge.
6. **Route back-compat:** how long do `.../measurements` and `.../rawdata` redirect to the merged
   `Dataset` view, and do external bookmarks matter?
7. **Prototype retirement:** once the decided behaviours are ported to Stories, the prototype and
   its stylesheet should be deleted (parent plan rule 2). The durable fix — moving the prototype to
   its own module so production cannot import it — remains an open module-boundary decision.
