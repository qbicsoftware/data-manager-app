# Implementation Plan — FEAT-DATSET-09: Access available datasets after login (project-listing visibility)

> **Story:** [#1475](https://github.com/qbicsoftware/data-manager-app/issues/1475)  
> **Parent Feature:** [#1466 — FEAT-DATASET-CONNECTION](https://github.com/qbicsoftware/data-manager-app/issues/1466)  
> **UI Prototype:** `ProjectListingDatasetsDemo.java` at `datamanager-app/.../views/demo/project-listing-datasets` (`@Profile("development")`)  
> **Prototype screenshots:** `img_2.png` → `img_7.png` in project root (iteration history)  
> **Production target:** `ProjectCollectionComponent.ProjectOverviewItem` at `datamanager-app/.../views/projects/overview/components/ProjectCollectionComponent.java`  
> **Destination route (already registered):** `ConnectedDatasetsMain` at `projects/:projectId?/datasets`  
> **Date:** 2026-07-29

---

## 1. Scope — What This Iteration Covers

FEAT-DATSET-09 ensures researchers see connected-dataset information **directly on the project collection view** after login, without opening a project first.

The acceptance criteria from Issue #1475 are:

- **AC1 (existing):** When a user views their project collection, they can see from each project entry **whether** that project has connected resources.
- **AC2 (existing):** From each project entry they can see the **quantity** of connected resources.
- **AC3 (existing):** From each project entry they can see the resources' **access status** (open vs. restricted).
- **AC4 (existing):** Clicking a project's dataset indicator navigates **directly** to the project's connected resources section.
- **AC5 (added per stakeholder iteration):** Users can see **when** the last dataset was connected to each project (recent activity signal).

**Out of scope for this story** (covered by separate stories or future work):

- Connecting / disconnecting / syncing datasets from the collection view (Stories 01–08) — still done inside the per-project dataset view.
- Filtering or sorting the project listing by connected-dataset criteria (e.g. "show only projects with restricted datasets") — follow-up task if requested.
- InvenioRDM credential management (Stories 14/15) — no change here; credentials are managed from within the per-project view.

---

## 2. Governance pre-requisites (blocking, must be resolved before implementation PR lands)

### 2.1 Requirement-ID conflict (blocking)

Issue #1466 (parent Feature) references proposed `DATA-R-01` / `DATA-R-02` / `DATA-R-03` for dataset connexion. Those IDs already exist in `docs/requirements.md` for immunopeptidomics raw data and cannot be reused without breaking traceability.

**Decision required from PO:**

| Option | Action |
|---|---|
| (a) Allocate fresh IDs | Add `DATA-R-04` (Dataset Connection Management), `DATA-R-05` (Dataset Synchronisation), `DATA-R-06` (InvenioRDM Credentials) to `docs/requirements.md`. |
| (b) Rename existing | Only if the existing `DATA-R-01/02/03` are no longer used (unlikely). Retire them first, reassign. |

**This requires a dedicated PR** against `docs/requirements.md` — per AGENTS.md §11 requirement changes require human approval and must not be bundled with implementation.

### 2.2 Schema migration (blocking)

Phase 1 requires extending the **`project_overview` DB view** with columns aggregated from `associated_dataset`. Schema changes require human review per AGENTS.md §12.

**Action:** separate PR against `sql/complete-schema.sql` plus a migration script for existing installations.

---

## 3. Prototyping record

The prototype lives at `datamanager-app/src/main/java/life/qbic/datamanager/views/demo/ProjectListingDatasetsDemo.java` (route `test-view/project-listing-datasets`, profile `development`). Iterations produced a sequence of validated UI choices that the production code inherits verbatim:

| Iteration | Key decision | Evidence |
|---|---|---|
| Initial (img_2 → img_3 fix) | Card layout rendered without a Vaadin Grid — replaced with plain flex container; duplicate header removed. | img_3.png |
| Indicator style 1 | Full-row coloured bar (green/amber) reads as a warning — rejected by stakeholder. Restricted is a status, not a problem. | img_5.png |
| Indicator style 2 | Indicator neutral, access status expressed as `Tag` pills (`SUCCESS`/`WARNING`), "Last connected" date added. | — |
| Interaction 1 | Whole indicator row as `RouterLink` — "looks super weird", full row blue link overloads visual weight. | feedback |
| Interaction 2 | Chevron icon only as `RouterLink` — too small a click target for users with motor disabilities. | feedback |
| Interaction 3 (final) | Full-width footer region as a `RouterLink`, visually differentiated with top border + background, containing count + Open/Restricted `Tag`s + last-connected + spacer + chevron. | img_6.png, img_7.png |
| Hover style | Text underline on hover noisy across tags/separators; dropped in favour of subtle background + border brightening only. | img_6.png |
| Alignment | Footer left-right padding removed; top-bottom padding kept — keeps content aligned with card body. | img_7.png |

The production implementation inherits all of the above. Do not revisit these choices without a stakeholder sign-off.

---

## 4. Architectural constraints

### 4.1 Bounded context

Everything lives in `project-management`. Connected-dataset aggregate (`associated_dataset` table, `AssociatedDatasetService`, `ConnectedDatasetView`) is in this module, as is `ProjectOverview`, `ProjectInformationService`, and the `project_overview` DB view. No cross-context communication required.

### 4.2 Persistence model

- `ProjectOverview` is `@Immutable @Entity` backed by the `project_overview` DB **view**, not a table.
- The view aggregates `projects_datamanager` + `project_measurements` + `project_userinfo` via `LEFT JOIN`s; query uses Spring JPA Specifications (`ProjectOverviewLookupImplementation`).
- The `associated_dataset` table lives on the same datasource, indexed on `project_id` with a partial unique constraint on `(project_id, pid)` (excluding `REMOVED` rows).
- `ConnectedDatasetView` DTO already contains `isPublic`, `connectedOn`, `accessDetail` and related fields — but the *listing* needs per-project aggregates, not per-dataset.

### 4.3 Approach: extend the DB view (vs. service-layer augmentation)

| Path | Mechanism | Trade-off |
|---|---|---|
| **(A) Extend `project_overview` view (recommended)** | Add `LEFT JOIN (SELECT project_id, COUNT, SUM(public), SUM(restricted), MAX(connected_on) FROM associated_dataset WHERE state='CONNECTED' GROUP BY project_id)` | Self-contained projection; existing `Specification` query pattern preserved; no N+1. |
| (B) Query counts in service layer, augment `ProjectOverview` before return | Batch query per project id list | Requires per-page merge step; drift risk over time. |

**Decision:** Path A. View is queried one page at a time; per-project dataset counts are small.

### 4.4 UI: click-routing mechanism

- `ProjectCollectionComponent` renders `ProjectOverviewItem` — a private inner `Div` with `addClickListener(...)` that currently navigates the whole card to `ProjectInformationMain`.
- Footer must be a `RouterLink` (native `<a>`) so browser-anchor navigation short-circuits Vaadin's server-side `ClickEvent` routing — otherwise clicking the footer also fires the card-body handler. *Validated in prototype.*
- `ConnectedDatasetsMain` is registered at `projects/:projectId?/datasets`; the route is live and stable.

---

## 5. Phase 1 — Data layer

### 5.1 Extend `project_overview` DB view

Files: `sql/complete-schema.sql` plus migration `sql/migration/V1.12.0__extend_project_overview_datasets.sql`.

Append derived `LEFT JOIN`:

```sql
LEFT JOIN (
    SELECT
        `project_id`,
        COUNT(*)                                                    AS connectedDatasetCount,
        SUM(CASE WHEN `access_level` = 'PUBLIC'    THEN 1 ELSE 0 END) AS openDatasetCount,
        SUM(CASE WHEN `access_level` = 'RESTRICTED' THEN 1 ELSE 0 END) AS restrictedDatasetCount,
        MAX(`connected_on`)                                         AS lastConnectedOn
    FROM `associated_dataset`
    WHERE `connection_state` = 'CONNECTED'
    GROUP BY `project_id`
) AS connected_datasets ON connected_datasets.project_id = pd.projectId
```

Add to `SELECT` list:

```sql
COALESCE(connected_datasets.connectedDatasetCount,   0) AS connectedDatasetCount,
COALESCE(connected_datasets.openDatasetCount,        0) AS openDatasetCount,
COALESCE(connected_datasets.restrictedDatasetCount,  0) AS restrictedDatasetCount,
connected_datasets.lastConnectedOn                                   AS lastConnectedOn
```

### 5.2 Extend `ProjectOverview` JPA entity

File: `project-management/src/main/java/life/qbic/projectmanagement/application/ProjectOverview.java`.

```java
@Column(name = "connectedDatasetCount",  nullable = false)
private int connectedDatasetCount;

@Column(name = "openDatasetCount",       nullable = false)
private int openDatasetCount;

@Column(name = "restrictedDatasetCount", nullable = false)
private int restrictedDatasetCount;

@Column(name = "lastConnectedOn")
private Instant lastConnectedOn;   // nullable: projects with no datasets
```

Add public accessors. Entity is `@Immutable`, Hibernate fields set via reflection — no constructor change.

### 5.3 Service layer unchanged

`ProjectInformationService.queryOverview(...)` returns `List<ProjectOverview>`; the new columns come along for free via JPA projection. Existing `Specification` predicates and lazy pagination (`OffsetBasedRequest`) are untouched. Skip exposing the new fields in `ProjectOverviewSpec` predicates (filter/search) — follow-up task if requested.

---

## 6. Phase 2 — UI composition

### 6.1 Where the footer styling lives

Production target file: `datamanager-app/frontend/themes/datamanager/components/page-area.css` (this is where `project-overview-item`, `project-grid`, `project-collection-component` already live).

```css
/* region project-dataset footer */

.project-dataset-footer,
.project-dataset-footer *,
.project-dataset-footer a {
    text-decoration: none !important;
    box-sizing: border-box;
}

.project-dataset-footer {
    display: block;
    color: var(--lumo-body-text-color);
    border-top: 1px solid var(--lumo-contrast-10pct);
    background-color: var(--lumo-base-color);
    padding-top:    var(--spacing-04);
    padding-bottom: var(--spacing-04);
    padding-left: 0;
    padding-right: 0;
    margin: 0;
    transition:
        background-color var(--lumo-shade-50ms),
        border-top-color var(--lumo-shade-50ms);
}

.project-dataset-footer:hover,
.project-dataset-footer:focus-within {
    background-color: var(--shade-color-5pct);
    border-top-color: var(--shade-color-20pct);
}

.project-dataset-footer vaadin-icon {
    color: var(--primary-text-color);
}

.project-dataset-footer:focus-visible {
    outline: 2px solid var(--primary-color);
    outline-offset: -2px;
}

/* endregion */
```

Note spacing variables use the app's `--spacing-XX` (already mapped `--spacing-04 = --lumo-space-m`, `--spacing-05 = --lumo-space-l`), consistent with `page-area.css`.

Delete prototype's `addDatasetFooterStylesheet()` (injects via `executeJs`) — no longer needed.

### 6.2 Extend `ProjectOverviewItem` (inline-style audit)

File: `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/overview/components/ProjectCollectionComponent.java` — inner `ProjectOverviewItem` class.

Append dataset-footer rendering below the avatar group:

```java
if (projectOverview.connectedDatasetCount() > 0) {
    add(buildDatasetFooter(projectOverview));
}
```

`buildDatasetFooter(...)` builds the same content the prototype does (icon + count + Tag pills + last-connected + spacer + chevron) **but exclusively via `addClassName(...)`** — no `getStyle().set(...)` calls. Use the following mapping:

| Prototype inline style | Existing class (in `all.css` / `page-area.css`) | Replacement |
|---|---|---|
| `display: flex;` / `flex-direction: column` | `.flex-vertical` (all.css) | `addClassName("flex-vertical")` |
| `display: flex; flex-direction: row` | `.flex-horizontal` (all.css) | `addClassName("flex-horizontal")` |
| `align-items: center` | `.flex-align-items-center` (all.css) | `addClassName("flex-align-items-center")` |
| `gap: var(--lumo-space-s)` = `var(--spacing-03)` | `.gap-03` (all.css) | `addClassName("gap-03")` |
| `gap: var(--lumo-space-l)` = `var(--spacing-05)` | `.gap-05` (all.css) | `addClassName("gap-05")` |
| `margin-top: var(--lumo-space-xs)` = `var(--spacing-02)` | `.margin-top-02` (all.css) | `addClassName("margin-top-02")` |
| `margin-top: var(--lumo-space-m)` = `var(--spacing-04)` | `.margin-top-04` (all.css) | `addClassName("margin-top-04")` |
| `color: var(--lumo-primary-text-color)` | `.color-primary-text` (all.css) | `addClassName("color-primary-text")` |
| `color: var(--lumo-body-text-color)` | body-text-color already implicit | no-op |
| `font-weight: 700` | `.bold` (all.css) | `addClassName("bold")` |
| `flex-grow: 1` | `.flex-grow-1` (all.css) | `addClassName("flex-grow-1")` |
| `flex-shrink: 0` | — | add `.flex-shrink-0 { flex-shrink: 0; }` to all.css |
| `border-top: 1px solid var(--lumo-contrast-10pct)` | — | already set by theme rule |

**Avatar-circle inline styling exception.** The prototype's 28×28 circle with -8px overlap and `border-radius: 50%` is a single component-level primitive with no existing utility class. Keep inline for these 4 properties only:

- `width: 28px; height: 28px; border-radius: 50%;` (shape)
- `border: 2px solid var(--lumo-base-color);` (edge)
- `margin-left: -8px;` (overlap)
- `color: white; font-size: 10px; font-weight: 600; display: flex; align-items: center; justify-content: center;` (content)

If other views already render collaborator circles, factor these into a reusable `.collaborator-avatar` utility class. Otherwise leave the avatar primitive as-is.

**Tag usage** — existing `life.qbic.datamanager.views.general.Tag` with `TagColor.SUCCESS` (open) / `TagColor.WARNING` (restricted). Consistent with measurement-type tags on the same card and with V3/V4 associated-dataset demos.

**Date formatting** — use `life.qbic.application.commons.time.DateTimeFormat.SIMPLE_DATE_TIME` with `Locale.ENGLISH`. Same formatter used elsewhere in the project listing (last-modified display).

**RouterLink and aria-label** — `new RouterLink("", ConnectedDatasetsMain.class, new RouteParameters("projectId", projectOverview.projectId().value()))`, class `"project-dataset-footer"`, `aria-label` = `"Open datasets for Q2KX4B: 3 connected, 2 open, 1 restricted, last updated 20 July 2026"` (format: last connected date rendered with full month name, locale English).

### 6.3 CSS-class migration rule

> Any `getStyle().set(...)` in the production `ProjectOverviewItem` footer implementation must correspond to an existing class in `all.css` or `page-area.css`, or a newly-added utility class. Inline styles are restricted to the avatar-circle literal (shape, overlap, border-radius) — everything else uses classes.

---

## 7. Phase 3 — Prototype deprecation

Keep `ProjectListingDatasetsDemo.java` under `@Profile("development")`. Update its class javadoc:

```
Prototype — superseded by real integration in ProjectCollectionComponent.
Retain as visual reference and UI-iteration sandbox.
Delete once feature validated against production-shaped data.
```

Delete `addDatasetFooterStylesheet()` — no longer needed (theme CSS holds the rules now).

---

## 8. Test coverage

| Test | Location | Type |
|---|---|---|
| `ProjectOverview` exposes the four new fields (mapping check) | `project-management/src/test/groovy/.../ProjectOverviewSpec.groovy` | Spock unit — no DB |
| `ProjectOverviewLookup.query(...)` returns rows with non-zero `connectedDatasetCount` for projects that have `associated_dataset` rows | `project-management-infrastructure/src/integrationTest/groovy/...` | Spring Boot slice; seeded DB |
| `ProjectOverviewItem` renders a footer when `connectedDatasetCount() > 0` and omits it otherwise | `datamanager-app/src/test/groovy/.../ProjectOverviewItemSpec.groovy` (create if missing) | Component-level via Vaadin testing API |

The third test cannot be a pixel-perfect visual comparison from a Groovy unit test, but it can assert the presence/absence of the `RouterLink` component by its class name.

---

## 9. Change matrix

| File / path | Change | Risk | Human-review gate? |
|---|---|---|---|
| `docs/requirements.md` | Add / resolve `DATA-R-*` IDs for dataset connexion | Governance | **Yes** |
| `sql/complete-schema.sql` + migration script | Extend `project_overview` view | Schema migration | **Yes** |
| `ProjectOverview.java` | +4 fields + accessors | Entity evolution | No (follows schema) |
| `page-area.css` | + `.project-dataset-footer` rules | Theme CSS | No |
| `all.css` | + `.flex-shrink-0` utility (small add) | Theme utilities | No |
| `ProjectCollectionComponent.java` | + `buildDatasetFooter(...)` + conditional render | UI composition | No |
| `ProjectListingDatasetsDemo.java` | Deprecation note; delete `addDatasetFooterStylesheet()` | No functional change | No |
| 3 × `*Spec.groovy` | + new field & rendering tests | Test addition | No |

---

## 10. Sequencing

```
Week 1
  PR #1   docs/requirements.md                    — DATA-R-* ID reconciliation
        ↓ gate: PO sign-off
  PR #2   sql/complete-schema.sql + migration     — view extension
        ↓ gate: DBA / infra review

Week 2
  PR #3   ProjectOverview.java + spec             — +4 fields
        ↓ depends on PR #2 merged

Week 3
  PR #4   ProjectCollectionComponent + theme CSS  — footer render
        ↓ depends on PR #3 merged
        ↓ sign-off: visual match against img_7.png

Week 4
  PR #5   Prototype deprecation + smoke test against production-shaped data
```

Drafts of PR #3 and PR #4 may be prepared ahead of time but **must not be merged** until their upstream dependency lands.

---

## 11. Open questions

| # | Question | Impact if unresolved |
|---|---|---|
| 1 | `DATA-R-01/02/03` ID conflict — rename immunopeptidomics reqs or allocate new IDs? | **Blocking.** |
| 2 | `access_level` in `associated_dataset`: populated for pre-prototype data? | Legacy rows with NULL `access_level` yield incorrect counts. Mitigate with data-fix migration deriving from `resource_metadata`. |
| 3 | Projects with zero datasets: render "No datasets connected yet" footer, or omit footer entirely? | Prototype uses `> 0` guard. Keeping this (less visual noise), but it's a UX decision. |
| 4 | `int` vs `long` for the count columns in `ProjectOverview`? | SQL `COUNT()` is `BIGINT`. JPA `int` OK for realistic scale; confirm if a project-SLA requires `long`. |
| 5 | Filter projects by connected-dataset criteria? | Out of scope. Determines whether to index new columns. |
| 6 | Performance of the `LEFT JOIN` per-project aggregate on large instances? | Worth `EXPLAIN` on prod-like data. Pattern precedent (existing view already joins `project_userinfo` via subquery + `GROUP_CONCAT`) suggests acceptable. |
| 7 | Collaborator-avatar circle reusable? | If similar circles exist in other views, factor into shared `.collaborator-avatar` utility. Audit before finalising. |
