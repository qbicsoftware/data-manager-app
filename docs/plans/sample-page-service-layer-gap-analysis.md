I have the complete evidence base. One correction to my earlier suspicion, verified: `findMeasuredSampleIds` is **correct** (the `retainAll` at `MeasurementLookupService.java:104` repairs the dropped filter) but **wasteful** — the DB-level `includingSamples` is never wired through `MeasurementLookup.mapToDatabaseFilter`, so it fetches every measurement in the experiment. That distinction matters for the recommendation.

I have no write-capable tool in my toolset, so per my instructions I'm returning the complete artifact for the runtime to persist.

---

# Sample Page Rework — Service & Persistence Layer Gap Analysis

> **Scope:** Feasibility of the planned master-detail sample page against today's view model, service endpoints and schema. Read-only assessment. Evidence is cited as `file:line`. Gap classes: **(A)** already supported · **(B)** new API method that is a thin wrapper over existing queries · **(C)** new persistence/query work · **(D)** new domain-model change (new data captured).

## Executive verdict

**Partly — the identity grid and the side panel ship today with zero service changes; measurement status, structured quick filters and (if the page becomes project-scoped) the cross-experiment list need work, but none of it touches the `Sample` aggregate.**

`SamplePreview` (`project-management/src/main/java/life/qbic/projectmanagement/application/sample/SamplePreview.java:30-61`) already carries every field the reworked page needs: sample code, name, biological replicate, batch, condition (as a full `ExperimentalGroup` with its `Condition`), species, specimen, analyte, analysis method, comment, registration and modification time. The side panel's five prototype groups therefore require **no extra fetch** — a page of previews is self-sufficient. Pagination, count, cross-page selection, bulk export/edit/delete and URL-restorable list state all exist and are independent of this rework. The milestone/unlocked band is pure view-state — the exact predicate already drives the current disclaimers (`datamanager-app/.../samples/SampleInformationMain.java:155`, used via `setBatchAndSampleInformation()`). Three genuine gaps remain. (1) **Measurement status** is absent from `SamplePreview` and cannot be expressed by the current preview query; it is derivable from the indexed `sample_id` columns of the three measurement-join tables (`sql/complete-schema.sql:602-633`), so it is class **C**, not **D** — no new data must be captured. (2) **Structured quick filters** are impossible today because `SamplePreviewFilter` carries only a single free-text string (`AsyncProjectService.java:1437`); condition and ontology filtering exist only as `LIKE` predicates, and the underlying `experimental_group_variableLevels` table has **no primary key and no index** (`sql/complete-schema.sql:159-172`), while the ontology columns are `@Converter(autoApply=true)` Jackson JSON blobs (`OntologyClassAttributeConverter`) reached only via non-indexable `JSON_EXTRACT`. (3) **Project-scoped listing** — the prototype's `projectSamplesPage()` shows "every sample in the project, across all experiments", but the only preview query is experiment-scoped; the `sample` table already has `project_id` (`sql/complete-schema.sql:527`) but `SamplePreview` does not map it. Recommended posture: ship the identity grid + side panel + milestone band first (no backend work), then land measurement status and filters as separate, independently reviewable backend slices.

## Capability matrix

| # | Capability | Planned UI need | Current support | Class | Work required | Est. | Key files |
|---|---|---|---|---|---|---|---|
| 1 | Identity grid | Sample ID, name, condition, species columns | All on `SamplePreview`; free-text filter + sort already keyed for these | **A** | None (drop columns from view) | S | `SamplePreview.java:30-61`; `SampleSort.java`; `SamplePreviewJpaRepository.java:88-127` |
| 2 | Side panel payload | Full record grouped in 5 groups + timestamps | Every group's fields present on the preview page object | **A** | None — reuse the loaded page item | S | `SamplePreview.java:129-179` |
| 3 | Milestone / "unlocked" band | Visible once experimental groups exist | Predicate already implemented for the current disclaimers | **A** | View-state only | S | `SampleInformationMain.java:155`, `setBatchAndSampleInformation()` |
| 4 | Experiment column | Show experiment name/identity | Only `experimentId` on the preview (`SamplePreview.java:32`) | **A** (exp-scoped) / **C** (project-scoped) | Exp-scoped: resolve the single name once. Project-scoped: join/projection | S / M | `ExperimentInformationService.java:116-124`; `Experiment.java:116` |
| 5 | **Measurement status per row** | "Measured / Not measured" column | Not on `SamplePreview`; only an ID-set lookup exists | **C** | Add status to the preview query via join/subquery; must stay N+1-free | M | `MeasurementLookupService.java:80-105`; `sql/complete-schema.sql:602-633` |
| 6 | **Measurement status as filter** | Filter "measured / not measured" | No preview filter dimension exists | **C** | Positive join is easy; **"not measured" is an anti-join** — needs `NOT EXISTS` | M | `SamplePreviewJpaRepository.java:129-155` |
| 7 | **Status facet counts** | Chip counts e.g. "Measured (42)" | No facet/count endpoint at all | **C** | Group-by / conditional-sum count query | M | `SamplePreviewRepository.java` (add `@Query`); `AsyncProjectService.java:1485` |
| 8 | Filter: analysis method | Multi-select analysis method | `analysisMethodContains` LIKE → enum abbreviation set | **B** | Add exact/`IN` predicate over the enum; filter record field | S | `SamplePreviewJpaRepository.java` `analysisMethodContains` |
| 9 | Filter: batch | Multi-select batch label | `batchLabelContains` LIKE only | **B** | Add equality/`IN` predicate + distinct-values query for the options | S | `SamplePreviewJpaRepository.java` `batchLabelContains` |
| 10 | **Filter: condition / variable level** | Filter by experimental condition | Only `LIKE` across joined `variableLevels`; join table un-indexed | **C** | New exact-match join spec; index `experimental_group_variableLevels` | M | `SamplePreviewJpaRepository.java:201-215`; `sql/complete-schema.sql:159-172` |
| 11 | **Filter: sample origin (ontology)** | Filter species / specimen / analyte | Only `JSON_EXTRACT($.label) LIKE` | **C** | Equality/`IN` on JSON is possible but **non-indexable** — decide generated columns or accept scan | M/L | `SamplePreviewJpaRepository.java:217-243`; `OntologyClassAttributeConverter` |
| 12 | Filter option lists | Populate quick-filter chips | Variable levels + species/specimen/analyte getters exist | **A/B** | Reuse 3 getters; batch needs a new distinct query | S | `ExperimentInformationService.java:485,496,507,558` |
| 13 | URL state for filters | Restorable/shareable filter state | `ListStateCodec` serialises exactly one `q` | **C (view)** | Extend codec to multi-valued, validated filter params | S/M | `ListState.java:20`; `ListStateCodec.java:29-32` |
| 14 | Cross-page selection + "select all matching" | Bulk actions over filtered set | Implemented, resolves all matching IDs | **A** ⚠ | Works; loads *all* IDs into memory — review at scale | S | `SampleDetailsComponent.java:353-362` |
| 15 | Pagination + total count | Pager, honest totals | `countSamples(filter)` implemented | **A** | None | S | `AsyncProjectServiceImpl.java:636-654` |
| 16 | **Project-scoped sample list** | Samples across all experiments | Only experiment-scoped preview query; `sample.project_id` exists but is unmapped | **C** | Map `project_id` on `SamplePreview` + project spec + new API method | M | `SamplePreview.java:30-37`; `sql/complete-schema.sql:527` |
| 17 | Bulk export / edit / delete | Toolbar actions | `sampleInformationTemplate`, `SampleRegistrationServiceV2.updateSamples`, `DeletionService.deleteSamplesAsync` | **A** | None | S | `SampleInformationMain.java` (`submitSampleUpdate`, `deleteSamples`) |

⚠ Row 14 hides a real defect — see Risks.

## Recommended approach per gap

### Gap 5/6/7 — Measurement status (column, filter, facet)

**Do not denormalise onto `sample` as a first move, and do not call `findMeasuredSampleIds` per page.**

Today's lookup is **correct but O(all measurements in the experiment)**: `MeasurementLookupService.findMeasuredSampleIds` (`MeasurementLookupService.java:80-105`) builds a filter with `.includingSamples(sampleIds)` (`:86,93,100`) and queries with `offset=0, limit=Integer.MAX_VALUE`; the final `measuredSampleIds.retainAll(sampleIds)` (`:104`) repairs the result. It is correct only *because* of that `retainAll` — the requested set never reaches SQL. The DB-layer capability exists (`NgsMeasurementJpaRepository.java:123-129` `includingSamples`, honoured in `measuresSamples()` at `:145-171`), but `MeasurementLookup.mapToDatabaseFilter` (`MeasurementLookup.java:133-157`) **never forwards `includedSamples`/`excludedSamples`**. For a page of 12 that is tolerable; for the filter and facet cases it is not.

Recommended, in order:
1. **Fix the drop-through** in `MeasurementLookup.mapToDatabaseFilter` (`:133-157`) to chain `.includingSamples(...)`/`.excludingSamples(...)` onto the DB filter for all three domains. This is a genuine pre-existing bug (the API contract in `NgsMeasurementLookup.java` documents the semantics the infrastructure silently ignores) and it makes the *existing* status lookup scale. Independent, small, and worth its own change.
2. **Derive status in the preview query** rather than as a second round-trip: add a projection/subquery on the three join tables (`specific_measurement_metadata_ngs|pxp|ip`, `sql/complete-schema.sql:602-633`, each with an indexed `sample_id`) so `SamplePreview` exposes a boolean/enum status with no N+1. Introduce `SamplePreviewSortKey.MEASUREMENT_STATUS` so the column is sortable and consistent with the rest of the grid.
3. **"Not measured" requires an anti-join** (`NOT EXISTS`), not a `LIKE`/`IN`. Budget for it explicitly; do not attempt to model it as a negated status value.
4. **Facets:** add a dedicated count query on `SamplePreviewRepository` (which is currently a bare `JpaRepository` with no custom queries) returning counts per dimension under the *active* filter set. Cheapest correct shape is one grouped/conditional-sum query, not N count calls.

### Gap 8/9 — Analysis method and batch filters (cheap wins)

Both predicates already exist as `LIKE` (`analysisMethodContains`, `batchLabelContains`) and `analysisMethod` is a Java enum, so exact `IN` filtering is a small addition to the spec class plus a field on `SamplePreviewFilter`. Do these first — they are the lowest-risk way to prove the multi-filter plumbing (API record → spec composition → URL codec) before tackling condition and ontology.

### Gap 10 — Condition filtering (the sharp edge)

The only existing support is `conditionContains` (`SamplePreviewJpaRepository.java:201-215`), which joins `experimentalGroup → condition → variableLevels` and `LIKE`s across `variableName`, `experimentalValue.value` and `variableName` — fine for search, wrong for a precise chip. The join target `experimental_group_variableLevels` (`sql/complete-schema.sql:159-172`) has **no primary key and no index on its FK**, so an exact-match filter will table-scan per query. Recommendation: add an exact-match specification keyed on `(variableName, value, unit)`, and add an index on `experimental_group_experimentalGroupId` as part of the same change. **Schema changes require human approval per AGENTS.md §12** — flag this as its own migration, and note that `Condition.variableLevels` is `FetchType.EAGER` (`Condition.java:42-43`), so any preview query touching conditions pays a join-with-eager-load cost today.

### Gap 11 — Ontology filtering (accept a cost, or change the model)

`species`/`specimen`/`analyte` are `text` columns holding Jackson JSON, mapped by `OntologyClassAttributeConverter` (`@Converter(autoApply = true)`), and filtered only through `JSON_EXTRACT(col, '$.label') LIKE ...` (`SamplePreviewJpaRepository.java:217-243`). Equality/`IN` is expressible, but **no functional index exists and MariaDB cannot index `JSON_EXTRACT` on a `text` column directly**. For "dozens to hundreds of samples per experiment" a scan is genuinely acceptable — recommend starting with `JSON_EXTRACT ... IN (...)` and measuring, rather than changing the schema. If the page later becomes project-scoped across thousands of samples, the correct fix is generated/stored columns for `species_label` etc. Do not conflate the two.

### Gap 16 — Project-scoped listing

`sample.project_id` already exists (`sql/complete-schema.sql:527`) but is unmapped on `SamplePreview` (`SamplePreview.java:30-37` maps only `experiment_id`, `code`, `sample_id`, and the payload fields). Adding the mapping plus a project specification, and exposing `getSamplePreviewsForProject(...)`/`countSamplesForProject(...)`, is a mechanical extension of the existing pattern — **but only the route decision is cheap; the experiment column and the filter facets get materially harder project-wide**. This is a **product decision** (see below), not a technical one.

### View-layer prerequisite (gap 13)

`ListState` is a record of `(page, pageSize, filter, sort)` with a single `String filter` (`ListState.java:20`), serialised by `ListStateCodec` into exactly `page`, `size`, `q`, `sort` (`ListStateCodec.java:29-32`). Quick filters need a **structured, multi-valued and validated** state (unknown/foreign values must degrade gracefully — the codec's existing allowlist pattern for `sort` is the right precedent). Treat this as part of the frontend slice, but decide the URL contract early, because it constrains the `SamplePreviewFilter` shape.

## Risks and unknowns

1. **Pre-existing bug — available-samples filter is dropped in infrastructure.** `MeasurementLookup.mapToDatabaseFilter` (`:133-157`) ignores `includedSamples`/`excludedSamples`. Result correctness survives only via `retainAll` at `MeasurementLookupService.java:104`; cost does not. This is squarely on the path of "measurement status at scale" and is currently untested — there is **no test referencing `findMeasuredSampleIds` anywhere**. Highest-value independent fix in this analysis.
2. **`selectAllMatching` loads every matching ID into memory** (`SampleDetailsComponent.java:353-362`: `countSamples` then `getSamplePreviews(..., 0, total, ...)`). Correct today; at "thousands of samples" it materialises the whole filtered set into the UI session. With measurement-status filters this gets likelier. Consider a server-side "select all matching = filter predicate" token instead of an ID set.
3. **Specification composition pitfalls.** The existing pattern is fragile: `isBlank` returns `null` when the filter is *not* blank (`SamplePreviewJpaRepository.java:168-176`) and relies on Spring Data's null-as-no-op composition, while `.and(...)` chains carry a `distinct` spec (`:160-166`) because condition joins fan out rows. Adding OR-ed multi-select filters on top of a `distinct` + fanned-out join is a well-known source of wrong counts (`COUNT(DISTINCT ...)` vs `COUNT(*)`). Any facet count must be validated against a hand-computed fixture.
4. **Schema/index reality.** No index on `sample.experiment_id`, `sample.project_id`, `sample.batch` or `sample.code` (`sql/complete-schema.sql:522-543`); `experimental_group_variableLevels` has no PK and no index (`:159-172`). Non-indexable `JSON_EXTRACT` on ontology. All three are latent at hundreds of samples and load-bearing at project scale. Schema changes need human approval.
5. **`SamplePreview` is a mutable `@Entity` over the `sample` table, not a view** — it maps `@OneToOne(cascade = CascadeType.ALL)` to `ExperimentalGroup` (`SamplePreview.java:56-58`). Adding more joins increases the blast radius of accidental writes through a "read" projection. Keep new status data **derived/read-only** (projection or `@Formula`-style) rather than adding associations.
6. **`countSamples(projectId, experimentId, filter)` only applies `filter.sampleName()`** (`AsyncProjectServiceImpl.java:636-654`) and the filter's `sortOrders` are ignored for counting. Consistent today; must be updated in lockstep when the filter record gains dimensions, or counts and pages will disagree.
7. **Unknowns I could not verify:** (a) whether MariaDB's version in production supports functional indexes on `JSON_EXTRACT` for a generated-column workaround; (b) actual cardinalities (samples per experiment, measurement rows) needed to right-size the facet query; (c) whether `EXPLAIN` plans on `experimental_group_variableLevels` confirm a scan at realistic volume — this needs a populated database, which is outside this read-only assessment.

## Recommended slicing

Each slice below is independently reviewable and shippable; the ordering reflects risk, and slices 1–2 need **no backend change**.

- **Slice 0 — Governance prerequisite (blocking).** No implementation without a Feature and Story; the prototype uses no requirement IDs, and AGENTS.md §0 forbids implementation without a stable `FEAT-<SLUG>-<NN>` in `docs/features.md`. The sample-page rework plausibly needs a **new** capability requirement (identity-first presentation, quick filtering) rather than reuse of `SAMPLE-R-01…04`, which cover batch removal and Excel registration/edit. Requirement additions require a **dedicated PR plus human approval** (AGENTS.md §0, §12). Resolve this before any code.
- **Slice 1 — Identity grid + side panel + milestone band.** Pure frontend; no backend, no schema, no API change. Delivers the prototype's headline value and validates the master-detail interaction (inline column vs overlay) with real data. **Highest value-to-risk ratio.**
- **Slice 2 — Measurement-status bug fix** (`mapToDatabaseFilter` forwarding sample filters) with tests for `findMeasuredSampleIds`. Independent of the UI, fixes latent cost, and is a prerequisite for status at scale.
- **Slice 3 — Multi-filter plumbing** via the cheap dimensions (analysis method, batch): extend `SamplePreviewFilter`, add `IN` predicates, extend `ListStateCodec` to a validated multi-filter URL contract. Proves the end-to-end pattern on low-risk predicates.
- **Slice 4 — Measurement status column + filter + facets.** Depends on Slice 2. Includes the anti-join for "not measured" and the grouped count query. Validate counts against fixtures.
- **Slice 5 — Condition and ontology filters.** Condition needs exact-match joins + an index (schema approval); ontology starts as a scan and is revisited only if the page goes project-scoped.
- **Slice 6 — Project-scoped list** (only if the product decides the page should span experiments). Needs `project_id` mapping, new API methods, and materially harder facets.

---

## Parent verification — corrections to the above (added after review)

This section was added by the orchestrating session after independently checking the
highest-impact claims. The assessment above was produced from a deliberately narrow evidence base
(the **experiment-scoped** `SamplePreview` / `AsyncProjectService` path). It did not account for the
**project-level read model** introduced by the `feature/project-page-rework` branch, which already
delivers several capabilities the matrix marks as missing. Where the two disagree, the corrections
below supersede the matrix rows named.

### Confirmed findings (kept as-is)

1. **`MeasurementLookup.mapToDatabaseFilter` drops the sample filter.** Verified at
   `project-management-infrastructure/src/main/java/life/qbic/projectmanagement/infrastructure/experiment/measurement/MeasurementLookup.java:133-155`:
   all three overloads chain only `forExperiment(...)`, `anyContaining(...)` and
   `atClientTimeOffset(...)`. `includedSamples`/`excludedSamples` never reach the DB filter.
2. **`findMeasuredSampleIds` is correct but wasteful.** Verified at
   `project-management/.../application/measurement/MeasurementLookupService.java:78-107`: it passes
   `includingSamples(sampleIds)` yet queries `offset=0, limit=Integer.MAX_VALUE`, then repairs the
   result with `retainAll(sampleIds)`. Correct only because of the `retainAll`.
3. **`SamplePreviewFilter` carries no filter dimensions.** Verified at
   `project-management/.../application/api/AsyncProjectService.java:1437` — `(String sampleName, List<SortOrder<SamplePreviewSortKey>> sortOrders)`.
4. **`countSamples(projectId, experimentId, filter)` must be kept in lock-step with the page query**, per
   `AsyncProjectServiceImpl.java:636-654`. Confirmed and load-bearing for any facet work.
5. **`ListState`/`ListStateCodec` carry a single flat filter** — `ListState.java:20`,
   `ListStateCodec.java:29-32`. Confirmed.

### Refuted findings — the project-level read model already exists

The branch already ships a project-scoped, paginated, measured-filterable sample read model over a
dedicated SQL view. This invalidates matrix rows 4 (project-scoped variant), 5 (partially), 6, 7,
11 (partially) and 16.

| Claim above | Verified reality | Evidence |
|---|---|---|
| Project-scoped list needs new persistence work (**C**) | A complete project-scoped read model exists: `getProjectSamples(projectId, offset, limit, ProjectSampleFilter)`, `countProjectSamples(projectId, filter)`, `getRecentSamples(...)` | `project-management/.../application/api/ProjectQueryService.java:59,70,92` |
| Measurement status must be added to `SamplePreview` (**C**) | Per-sample `measured` already exists as a **flat read-only projection**: `@Immutable @Entity` over `v_project_sample_overview`, with `sampleCode`, `sampleName`, `batch`, `experimentName`, `experimentalGroupName`, `analysisMethod`, `species`, `specimen`, `analyte`, `registrationTime`, `lastModified`, `measured` | `project-management/.../application/query/ProjectSampleOverview.java` |
| Experiment column needs a join/projection (**C**) | `ProjectSampleOverview.experimentName` and `experimentId` are already on the projection | same file |
| "Not measured" needs an anti-join in the preview query (**C**) | The view computes `measured` with `EXISTS` over `specific_measurement_metadata_ngs|pxp|ip`; the repository filters `MEASURED` → `isTrue(measured)`, `NOT_MEASURED` → `isFalse(measured)` — **in SQL, not in memory** | `sql/migrations/create-project-page-read-models.sql:241-278`; `project-management-infrastructure/.../query/ProjectPageReadRepositoryImpl.java:127-139` |
| Status-as-filter is absent | `ProjectSampleFilter(sampleName, experimentId, MeasuredFilter measured, sortOrders)` with `MeasuredFilter{ALL, MEASURED, NOT_MEASURED}` | `ProjectQueryService.java:119,136` |
| Index support is inadequate | The migration adds `idx_sample_experiment` on `sample(experiment_id)` and notes `sample.project_id` is already FK-indexed | `create-project-page-read-models.sql:282-300` |

**Consequence:** milestone, identity grid, side panel, status column, status filter and experiment
attribution are all reachable **without new persistence work**; what remains genuinely open is
**facet counts** (no grouped count query exists) and the **rich** facets (condition variable level,
ontology origin, batch, analysis method), which `ProjectSampleFilter` does not model. The view's
columns can be extended additively, so those facets are cheaper than the matrix implies — but the
view is consumed by `ProjectSampleOverview`, whose mapping must be kept in sync.

### The unresolved question this raises (decision, not assessment)

There are now **two** sample read models on this branch:

- **experiment-scoped** — `AsyncProjectService.getSamplePreviews` + `SamplePreview` (the entity the
  current `SampleInformationMain` route uses, and what `img_14.png` shows);
- **project-scoped** — `ProjectQueryService.getProjectSamples` + `v_project_sample_overview` (the
  prototype's "every sample in the project, across all experiments" page).

The prototype's bluff is a *project-level* Samples page, but the page the product owner screenshotted
and complained about is the *experiment-scoped* one. Whether the reworked "final sample page" is
experiment-scoped (rework in place) or project-scoped (promote the read model) determines the entire
backend slice list:

- **Experiment-scoped** → Slice 3/4/5 of the slicing plan are real work (extend `SamplePreviewFilter`,
  per-experiment status derivation, facet counts).
- **Project-scoped** → most of Slice 4 already exists; the work collapses to facet counts, the rich
  filters, and the UI. And `ProjectSampleOverview` is `@Immutable` and flat **by contract**, so facet
  work belongs in the view + a new grouped count query, never as JPA associations.

~~This should be settled before any implementation slice is cut.~~

> **DEFERRED (product owner decision).** The project-wide sample view is explicitly deferred. The
> reworked sample page is **experiment-scoped**: rework `SampleInformationMain` in place at
> `projects/:projectId/experiments/:experimentId/samples`, and treat `ProjectQueryService` /
> `v_project_sample_overview` as an unused but available read model. Do **not** extend the view,
> revise `ProjectSampleSortKey`, or unify the two filter records as part of this rework. Nothing in
> the slice plan should assume a project-wide scope, and no work should be taken on the
> "experiment as a filter chip" question. Revisit only if the product decides to build the
> project-level page.

---

## Acceptance report