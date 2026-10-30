# Measurement Dataset — UI rework (facts, gap register, plan)

> **Provenance.** AI-authored analysis, 2026-10-26, from a code read of the two current experiment
> surfaces (`MeasurementMain`, `RawDataMain`), their components, the read models they consume, the
> schema, and the two prior slices (project page #1641, samples page #1648). Decisions A–H were
> taken with the project owner in the same session and are recorded in §2.
>
> **Status:** proposal. No production code modified.
>
> **Governance (AGENTS.md §0/§12).** `docs/requirements.md` contains no requirement for a merged
> experiment dataset surface, none for data status on a measurement list, and `MEASUREMENT-R-03`
> currently pins copy this rework changes; `docs/features.md` has no Feature/Story for it, and
> `FEAT-PAG-LIST-04` is superseded by it. **Per project-owner direction, this slice proceeds with
> the governance gate skipped**, recorded the same way as the project-page slice
> (`docs/migrations/NEXT.md`, migration #2: "governance gate skipped at project-owner direction").
> This document creates no IDs.

---

## 1. The subject: two process steps, one system boundary

A measurement is the metadata that describes how a result was produced. The data is the result.
They belong together *for the user*, but they are two ordered steps — and step 1 is a
**precondition** for step 2, because step 2 is keyed by the identifier step 1 mints.

| | Step 1 — Measurement (metadata) | Step 2 — Measurement data (the result) |
|---|---|---|
| Owner | **This application** | **A separate system**: facility → SFTP → `data-scanner` → OpenBIS |
| Written by | The user, via Excel registration | Nobody in this application |
| Stored in | `ngs_measurements`, `proteomics_measurements`, `immunopeptidomics_measurements` | OpenBIS; this app holds a **synced summary** in `remote_measurement_data` |
| Identity | Mints `MeasurementCode` (`NGS-…`, `MS-…`, `IP-…`) | **Consumes it**: `remote_measurement_data.measurement_id = measurementCode` |
| Timing | Immediate, user-driven | Asynchronous — `RawDataSyncService` polls every ~2 min (ShedLock + watermark) |

That is the whole design constraint. Three consequences follow, and they are the reason the current
two-page split is the wrong shape rather than merely verbose:

1. **"No data yet" is the normal state, not an error.** After registration a measurement has no
   data *by design* until the facility uploads. Neither current page can say "step 1 done, step 2
   pending": the measurements page hides itself, the raw-data page hides itself.
2. **"Which measurements are still waiting for data?" is the question the two-step model creates**,
   and no per-measurement surface can answer it today (§4, G1).
3. **Only one side is writable by us.** Step 2 is readable here, never writable (§9, follow-ups).

### 1.1 Vocabulary (decided, project owner 2026-10-26)

```
Experiment
 └── Measurement Dataset     one per measurement method; an experiment has one or more
      └── Measurement Data   one entry per measurement — strictly 1:1
```

- **Measurement Dataset** — the experiment's data for one measurement method (genomics,
  proteomics, immunopeptidomics). Several per experiment.
- **Measurement Data** — one entry: the registered measurement together with its datum. The entry
  *is* the combined unit, which is what "they belong together subjectively" means operationally.
- **"Raw data" is retired as a UI term.** It was a synthetic construct: the data is the result of
  the measurement, and "raw" is relative (base-called reads are themselves processed signal).
- **Connected dataset** keeps its existing meaning: an external record linked to the project
  (`AssociatedDataset`), relationship deliberately unclassified. Unchanged by this work.

**This corrects the written plan.** `docs/plans/Project-management-refactor.md` states "a dataset is
1:1 with its experiment, kind `primary`". That is stale. The code already implements the decided
model — `PrimaryDatasetsComponent` javadoc: *"A dataset is one experiment and one assay type. Not
one per experiment."* — and the doc must be corrected rather than the code.

---

## 2. Decisions taken (project owner, 2026-10-26)

| # | Decision |
|---|---|
| **A** | Data status values are **Waiting** / **Available** (not "arrived", not "attached"). |
| **A'** | **The download-URL export stays as-is in this slice.** The download service refactor (see §9.1) ships in a later increment. |
| **B** | Vocabulary as §1.1. The page is **Measurement Dataset**; an entry is **Measurement Data**. |
| **C** | The relationship is **stable 1:1**: one registered datum per measurement. |
| **D** | Data without a registered measurement is an **escalation**: data management must be made aware and act immediately. **Not** a state on the experiment page (§4, D-gap). |
| **E** | A data-removal path is required but **out of scope** for now. |
| **F** | Data status is **eventually consistent**, not live. No freshness guarantee is promised. |
| **G** | The governance gate is skipped (see status header). |
| **H** | **Complete refactor in one slice** — no extract-then-adapt, no triplication left behind. |

---

## 3. What exists today

### 3.1 Chrome and routes

| | |
|---|---|
| Experiment nav (`AppNavigationComponent.forExperiment`) | `Overview · Samples · **Measurements** · **Raw data**` |
| Routes | `…/experiments/:id/measurements` → `MeasurementMain`; `…/experiments/:id/rawdata` → `RawDataMain` |
| Dead constants | `ProjectRoutes.MEASUREMENT` (`projects/%s/measurements/%s`) and `ProjectRoutes.SAMPLE` have **no `@Route`** |
| Layout CSS | `.main.measurement` = unstyled single column with bordered `.page-area`; `.main.raw-data` = 80/20 grid with a nested scroll |

The two pages are already a navigation-level duplication of one subject: the user must know which
step their data is in before they know which link to click.

### 3.2 `MeasurementMain` + `MeasurementDetailsComponent` (1 228 + 1 628 lines)

- Three tabs (Genomics / Proteomics / Immunopeptidomics) over `MeasurementTabPagination`; each tab
  a paginated in-memory `Grid` wrapped in the shared `PaginatedGrid` in externally-controlled mode.
- **Flat, type-specific, all-visible columns: NGS 12, PxP 15, IP 23** (`setHeader` counts), plus a
  per-tab `Show/Hide Columns` menu.
- Already carries the ADR-0007/0009 machinery: `ListState` + codec, `PaginationBar`, per-tab state,
  identifier-based cross-page `Selection`, "Select all N matching", tab counts, empty-state
  distinction, sticky selection bar, write-access gating. 12 of 13 findings in
  `docs/ux-review/measurement-pagination-ux-findings.md` are 🟢.
- Registration / edit = `AppDialog` + Excel template download/upload; edit offers "all of this
  domain" **and** "selected (N)".
- Bulk actions: Export metadata (xlsx) · Edit · Delete (`AlertDialog.danger`, blocked by
  `DATA_ATTACHED`).
- **Only detail affordance:** clickable pooled-sample cell → `AppDialog` of `SampleInfo` rows.
  **No record detail panel.**
- Blocked/empty states **hide the whole page** behind `Disclaimer`s ("Register your samples first",
  "Manage your measurement metadata") — the anti-pattern the samples slice already retired.
- A dismissible `InfoBox` hand-off: "Raw data results … available now → Go to Download Raw data".

### 3.3 `RawDataMain` + `RawDataDetailsComponent` (301 + 763 lines)

- The same three tabs, via `RawDataTabPagination` — a **copy-paste twin** of
  `MeasurementTabPagination` (354-line diff, almost entirely renames).
- Grid per domain: **4 columns** (Measurement Id, Measurement Name, Sample Name, Upload Date) plus
  an expander row (`GridDetailsItem`) carrying Number of Files / File Size / File Suffixes.
- **Only bulk action:** "Export Dataset URLs" (TSV, `RawDataUrlFile` over the cross-page selection).
  The page is a download runbook, not an editor.
- `RawDataDownloadInformationComponent` occupies a permanent right rail: PAT step, URL-export step,
  `curl`/`wget` code tabs.
- Tab visibility from **three blocking count queries**; blocked/empty states again hide the page.
- `.main.raw-data` is an 80/20 grid; `#1640` was a hotfix just to make the Proteomics tab appear.

### 3.4 Where the two pages' data comes from (the crux)

| | Source |
|---|---|
| Measurement metadata | `MeasurementLookup` → JPA entities over **`ngs_measurements`** / proteomics / IP tables (`MeasurementLookup.java`, `NgsMeasurementJpaRepository:379`) |
| Measurement data | `LocalRawDataset*JpaRepository` → JPA entities over the **views** `v_ngs_measurement_sample_json` / `v_pxp_…` / `v_ip_…` (`LocalRawDatasetNgsEntry`) |
| The views | `INNER JOIN remote_measurement_data rmd ON rmd.measurement_id = m.measurementCode` (`sql/complete-schema.sql:853, 924, 1008`) |
| Completeness, both sides | **`experiment_progress`** → `ExperimentProgress` (via `ProjectQueryService`) — already computes `measurementCount`, `measurementsWithPrimaryData`, `measurementsWithoutPrimaryData()`, per assay type, plus `primaryDataBytes` |
| Recency | `RecentMeasurement` (`experiment_recent_measurement`) |

**The structural finding:** because the views use an `INNER JOIN`, a measurement with no data **is
not a row**. The raw-data page therefore cannot represent "waiting" at all, and the "18 of 24" figure
exists only as a project-level aggregate. Meanwhile the measurement page reads a *different* source
and has no data field whatsoever.

### 3.5 The two pages' only consumers in the app

`ExperimentOverviewComponent.datasetCard()` and `PrimaryDatasetsComponent.openExperiment()` both
navigate to **`MeasurementMain`**, while describing the merged surface ("Dataset", "measurements and
primary data"). The rest of the app is already built against the model this slice delivers.

---

## 4. Gap register

Legend: **B** blocked on a decision · **C** new persistence/query work · **V** view-layer only.

| # | Gap | Class | Evidence | Notes |
|---|---|---|---|---|
| **G1** | No per-measurement **data status**. The only per-measurement data facts live behind an inner-join view, so a waiting measurement has no row anywhere | **C** | `complete-schema.sql:853,924,1008`; `LocalRawDataset*Entry` | The single most valuable addition; the technical crux of the slice |
| **G2** | No merged row: measurement metadata and its datum never meet in one projection | **C** | §3.4 | Needs an **outer** join keyed on `measurementCode` |
| **G3** | `rmd_deleted` is exposed but **never filtered**, and is not mapped on the JPA view entity — tombstones render as live data | **C** | `LocalRawDatasetNgsEntry` fields; `ExperimentProgressQuery` filters it, these views do not | Must be fixed by the merge |
| **G4** | `MeasurementLookup.mapToDatabaseFilter` **drops** `includedSamples`/`excludedSamples` | **C** | `MeasurementLookup.java:133-152` | Pre-existing, untested (`findMeasuredSampleIds` survives only via `retainAll`). Blocks status filtering |
| **G5** | Measurement recency has **no modification timestamp** | **C** | `RecentMeasurement.changed()` javadoc | Ranking is by registration only; a schema change, deliberately deferred |
| **G6** | No project-wide measurement query (`MeasurementFilter.experimentId` is `@NonNull`) | **C** | `NgsMeasurementLookup.MeasurementFilter` | Not needed by this slice (project data is read-only aggregate); do not build it here |
| **G7** | Orphan data (datum whose measurement is gone) cannot be attributed to project or experiment | **C/B** | `remote_measurement_data` keys on `measurement_id` only; `OpenbisConnector:510-512` tolerates it | **Decision D:** escalation, not an experiment state → separate follow-up surface (§9.2) |
| **G8** | `RemoteRawDataService.hasRawData` runs ~6 queries and materialises every measurement code to return a **boolean**, and drives page visibility | **C** | `RemoteRawDataService.java:58-96` | Replaced by `ExperimentProgress` |
| **G9** | Tab visibility is derived from *data presence* on one page and *measurement counts* on the other | **V** | `RawDataDetailsComponent.refreshTabVisibility` | Merged: visibility must follow **measurements**, status inside the tab. The class of bug `#1640` hotfixed |
| **G10** | Blocked/empty states hide the entire page | **V** | `MeasurementMain.show*Disclaimer`, `RawDataMain.show*Disclaimer` | Samples already replaced this with derived-state bands |
| **G11** | No record detail panel; the only "detail" is a pooled-sample dialog | **V** | §3.2 | Reuse the `SampleDetailPanel` primitive |
| **G12** | `MeasurementTabPagination` ↔ `RawDataTabPagination` and both details components are near-duplicates | **V** | 354-line rename-only diff | Decision **H**: delete, don't extract-then-adapt |
| **G13** | The "how to download" runbook occupies a permanent 20 % rail | **V** | `RawDataDownloadInformationComponent` | Decision **A'**: export stays; the rail does not (§5.3) |
| **G14** | Delete error sends users to an action that does not exist | **V** | `DATA_ATTACHED` at `MeasurementRepositoryImplementation:123,143,309`; `MEASUREMENT-R-03` | Decision **E**: copy states removal is handled by data management; full path is a follow-up |

---

## 5. Target design

### 5.1 One surface, staged

Route: **`projects/:projectId/experiments/:experimentId/dataset`** → one `MeasurementDatasetMain`.
`…/measurements` and `…/rawdata` redirect into it (§6, phase 6).

```
Measurement Dataset                                     [Register measurement data]
 12 measurement entries · 7 with data available · 5 waiting         ← page status line
 ────────────────────────────────────────────────────────────────────────────────────
 1  Measurement metadata   ✓ 12 registered
 2  Data                  ⏳ 5 waiting                             ← stage, not a second list
 ────────────────────────────────────────────────────────────────────────────────────
 [Search…] [Data: All ▾] [Genomics] Proteomics  Immunopeptidomics     Show/Hide columns
 ────────────────────────────────────────────────────────────────────────────────────
 ☐  Measurement ID     Name      Samples   Instrument   Data                          ›
 ☐  NGS-…              NGS run   2         NovaSeq      Available · 3 files · 4.2 GB  ›
 ☐  NGS-…              NGS run   1         NovaSeq      Waiting — not uploaded yet     ›
 ────────────────────────────────────────────────────────────────────────────────────
 [2 pending]  Export metadata   Export dataset locations   Edit   Delete
 12 measurement entries · Page 1 of 1                                            12 ▾
```

- **Identity-first columns**, type-specific fields in the panel: the rule already established for
  samples and stated in `Project-management-refactor.md` ("Scan → Inspect → Extract", "status is the
  only attribute-like thing that earns a list column"). NGS 12 / PxP 15 / IP 23 flat columns is the
  defect this fixes; the retired columns remain reachable via `Show/Hide Columns`.
- **`Data` is the one status column** — Waiting / `Available · n files · size`. It is what drives
  attention, and it is the answer to the question the two-step model creates.
- **Stage band states the process, once**, at page level: metadata count and how many entries are
  waiting. It never becomes a second list.

### 5.2 The detail panel

Reuse the proven `SampleDetailPanel` primitive (`?record=<measurementId>`, inline column ≥1100 px,
drawer below, focus management, read-only in v1). Groups follow the stakeholder spec categories
already drafted in the prototype (`ProjectPagePrototypeDemo.measurementDetailGroups`, `:2263`):

`Measurement identity · Sample · Instrument · Run parameters · Organisational provenance · Notes`
then, as its own group, **`Data`**: state, number of files, total size, file suffixes, upload date,
`last sync` (silent unless present — decision **F**).

Type-specific groups differ per method (IP carries MHC antibody, enrichment, mass/charge/ion-mobility
ranges). This is what makes 23 columns survivable.

### 5.3 The download handoff (deferred, but not left dishonest)

Decision **A'** keeps the URL export. The rail does **not** stay:

- The export action moves into the merged toolbar (`Export dataset locations`), unchanged in
  behaviour (`RawDataUrlFile`, `server.download.api.measurement.url`, the TSV).
- `RawDataDownloadInformationComponent` is **not** carried over as a permanent 20 % rail. The PAT +
  `curl`/`wget` guidance becomes a link at the handoff point (docs + Settings → API tokens).
- Rationale recorded in §9.1: the app currently *mints* URLs by concatenating a configured prefix
  with a measurement code, asserting an endpoint contract it neither owns nor can verify.

---

## 6. Phased plan (one slice, sequentially reviewable commits)

> **Progress (2026-10-26):** phases 1 and 2 are implemented — see §6.1.

| # | Phase | Delivers | Backend |
|---|---|---|---|
| **1** | **Read model** | `MeasurementDataRow` projection: measurement metadata + its datum, **outer-joined** on `measurementCode`, `deleted = FALSE` excluded (G1–G3). One query per tab page + one count. New `ProjectQueryService` method (strategy D4 — not `AsyncProjectService`). | **C** |
| **2** | **Query fixes** | `MeasurementLookup.mapToDatabaseFilter` forwards `includedSamples`/`excludedSamples` (G4) — with the missing test. Retire `RemoteRawDataService.hasRawData` in favour of `ExperimentProgress` (G8). | **C** |
| **3** | **Shell + stage band** | `MeasurementDatasetMain`: capped centred shell like `.main.sample`, borderless bands, `H1`, status line, staged band (G9, G10). Disclaimers replaced by derived-state bands. | **V** |
| **4** | **Grid + panel** | One tab container (new, on `PaginatedGrid`/`ListState`/`Selection`/`PaginationBar`), identity columns, `Data` status column, `Data` filter, `MeasurementDataDetailPanel`. | **V** |
| **5** | **Actions** | Registration / edit / delete / export metadata / export dataset locations, `DATA_ATTACHED` copy (G14), PAT handoff link (G13). | **V** |
| **6** | **Delete the old** | Remove `MeasurementMain`, `RawDataMain`, `MeasurementDetailsComponent`, `RawDataDetailsComponent`, `MeasurementTabPagination`, `RawDataTabPagination`, the `rawdata`/`measurements` list-state codecs, `RawDataDownloadInformationComponent`; redirect both old routes; repoint `AppNavigationComponent.forExperiment`, `ExperimentOverviewComponent`, `PrimaryDatasetsComponent`; CSS (`measurements-pagination.css`, `rawdata-pagination.css`, `.main.raw-data`/`.main.measurement` blocks). | **V** |
| **7** | **Tests** | Status projection (incl. `deleted = TRUE` exclusion and the waiting case), outer-join completeness, filter→spec mapping, codec round-trip, tab visibility from measurements not data, panel field coverage, redirects. | — |

**Naming policy for phase 1–2:** the persistence names `LocalRawDataset*` and the table
`remote_measurement_data` **stay**. The table rename is a schema change (AGENTS §12) and must not
ride along in a UI slice; only the read-model records introduced here use the decided vocabulary.

### 6.1 Phase 1–2 as implemented

**Read model (new).**

| Artifact | Module | Purpose |
|---|---|---|
| `application/query/MeasurementDataRow` | `project-management` | Flat record: measurement metadata + data status + data facts. Carries the `DataStatus` enum (`WAITING` / `AVAILABLE`). |
| `ProjectQueryService.getMeasurementData` / `countMeasurementData` + `MeasurementDataFilter` + `MeasurementDataSortKey` | `project-management` | Reactive read contract, per strategy D4. |
| `ProjectPageReadRepository.findMeasurementData` / `countMeasurementData` | `project-management` | Blocking read port. |
| `infrastructure/query/MeasurementDataQuery` | `project-management-infrastructure` | The SQL. `ENTRIES_CTE` and `FILTER` are shared constants so page and count cannot drift apart. |
| `ProjectPageReadRepositoryImpl` mapping | `project-management-infrastructure` | `LEFT JOIN` mapping; `0` bytes preserved as "not recorded", absent columns as `null`. |

Decisions baked into the SQL:

- **`LEFT JOIN` on `remote_measurement_data`**, never the inner-join views — otherwise a waiting
  entry is not a row.
- **`deleted = TRUE` reports `WAITING`**, never `AVAILABLE`.
- **`sample_count` is scoped to the experiment**, so a cross-experiment measurement does not
  overstate either experiment.
- **Sort columns come from a closed enum**, never from a caller-supplied string (ADR-0009).

**Pre-existing defects fixed on the way (phase 2).**

1. `MeasurementLookup.mapToDatabaseFilter` silently dropped `includedSamples`/`excludedSamples` for
   all three domains. `findMeasuredSampleIds` stayed correct only because it re-applied
   `retainAll`, so every such lookup fetched *all* measurements of the experiment. Now forwarded.
2. `measuresSamples()` in all three JPA filters built `IN ()` from an empty included set — which
   matches nothing — so a filter that only *excluded* samples would have returned no rows. An empty
   set now contributes no predicate.

**Tests added:** `MeasurementDataRowSpec` (7), `MeasurementDataQuerySpec` (12 — pins the outer join, the tombstone rule, shared page/count
filtering, the tie-break and collation pinning),
`MeasurementLookupSampleFilterSpec` (12 — the regression guard the dropped filter never had), plus
two cases in `ProjectQueryServiceImplTest`.

> **Environment note.** `ProjectQueryServiceImplTest` and `AsyncProjectServiceImplTest` cannot run
> here: Mockito fails with "Could not initialize plugin: MockMaker" on this JDK. Verified on a clean
> tree (5/5 errors before and after this work), so it is an environment defect, not a regression.
> The new cases in that class are therefore unexecuted locally and must be run in CI.

---

## 7. Risks

1. **The outer join is the whole slice.** If it is built over the existing inner-join views, "waiting"
   remains unrepresentable and the status column silently always says `Available`. Must join the
   measurement tables to `remote_measurement_data` directly.
2. **Count and page must share one filter**, or the pager lies — the pre-existing trap from the
   sample-page analysis, repeated here.
3. **Status is eventually consistent** (decision **F**). A measurement can read `Waiting` for ~2 min
   after upload. Do not add copy promising otherwise; do not add a blocking refresh.
4. **`deleted = TRUE` exclusion** is load-bearing (`ExperimentProgressQuery` already documents this);
   omitting it makes completeness claims wrong.
5. **Tab visibility** must follow measurements, not data (G9) — otherwise a method whose data has not
   arrived loses its tab, which is exactly `#1640`.
6. **Column set churn** changes what `Show/Hide Columns` and any screenshot-based check expect; the
   existing `MeasurementDetailsGridColumnsSpec` comparator pins must be carried over or rewritten
   deliberately, not deleted.
7. **Route redirects**: external bookmarks to `…/measurements` and `…/rawdata` (also the target of
   `PrimaryDatasetsComponent` and `ExperimentOverviewComponent`) must keep working.

---

## 8. Out of scope, recorded deliberately

- **Project-wide measurement list** (G6). Project-level data stays read-only aggregate
  (`PrimaryDatasetsComponent`); registration stays experiment-scoped.
- **Measurement modification timestamp** (G5) — schema change.
- **Table/class rename** of `remote_measurement_data` / `LocalRawDataset*` — schema change.
- **Data removal path** (decision **E**).
- **Orphan-data steward surface** (decision **D**, §9.2).
- **Download-service handoff redesign** (decision **A'**, §9.1).

---

## 9. Follow-up increments

### 9.1 Download service handoff

**Problem.** The app holds no URL. `remote_measurement_data` stores count/types/bytes/date only, so
every URL is *invented* at export time: `new RawDataURL(prefix, measurementCode)` →
`"<DOWNLOAD_MEASUREMENT_API_URL>/<measurementCode>"` (`RawDataUrlFile.RawDataURL`). We assert an
endpoint contract owned by another service and cannot detect it breaking; the export would emit dead
links with no error. The guidance rail then teaches a `curl`/`wget` workflow that also belongs to
that service and goes stale with it.

**Options for the follow-up:** (A1) discovery not construction — show data facts (ours) and link to
the download service; export **measurement codes**, not URLs, keeping the endpoint contract with its
owner. (A2) as A1 plus a deep-link handoff (`?measurements=NGS-1,MS-2`), which is the best
convenience and needs a versioned contract with that service. (A3) proxying bytes through this app —
rejected: it makes us a data plane, a bottleneck, and responsible for delivery we cannot honour.

**This slice** keeps the export as-is and only relocates it (§5.3).

### 9.2 Orphan-data escalation surface (decision D)

A datum whose measurement was deleted cannot be attributed to a project or experiment: the cache
keys on `measurement_id` alone. Tolerated by design (`OpenbisConnector:510-512`: *"We don't fail, but
we keep data in openbis that might have been registered between the check and deletion"*). Because
it is unattributable, it needs a **steward-global** surface ("data without a registered
measurement"), which does not exist today. Data management must be able to see and act on it.

### 9.3 Data removal path (decision E)

`MEASUREMENT-R-03` promises that attached data "must be removed first", and `DATA_ATTACHED` blocks
deletion — but no in-app action removes data. Needs a request path or an explicit handoff.

---

## 10. Open decisions

| # | Decision | Proposed default |
|---|---|---|
| 1 | Column set per method (which identity columns survive in the default view) | Measurement ID (frozen) · Name · Samples · Instrument · Registration date · **Data** |
| 2 | Does the `Data` filter belong in v1 | Yes — All / Available / Waiting; it is the reason the status exists |
| 3 | Row activation | Row click opens the panel; a per-row button is the keyboard path (the `autoSelect` question already resolved on the samples slice) |
| 4 | Stage band vs. status line only | Stage band while anything is waiting; collapse to the status line when none is |
| 5 | `last sync` in the panel | Show it in the `Data` group (cheap, and it is the only staleness hedge) |
| 6 | Redirect lifetime for `…/measurements` and `…/rawdata` | Keep indefinitely; they are cheap and external bookmarks exist |
