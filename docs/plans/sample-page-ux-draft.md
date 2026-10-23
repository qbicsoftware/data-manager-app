I have all the grounding I need. Here is the draft plan.

---

# Sample page rework — UX / frontend design plan (DRAFT for review)

**Status:** draft for product owner / stakeholder review. No production code was modified.
**Scope:** the experiment-scoped Samples view (`SampleInformationMain`, route `projects/:projectId/experiments/:experimentId/samples`).
**Evidence base:** `img_14.png` (current state), `SampleInformationMain.java`, `SampleDetailsComponent.java`, `SampleSort.java`, `views/general/pagination/*`, `views/demo/ProjectPagePrototypeDemo.java` (§ samples page, lines 2433–2712) + `frontend/styles/views/project-prototype/project-page-prototype.css`, `front-end-components.md`, `frontend/themes/datamanager/components/{variables,main,grid-templates,page-area,div,span,info,project-progress,project-experiments}.css`, `docs/plans/Project-management-refactor.md` §"How wide records are presented".

**Governance note (AGENTS.md §0):** this rework is a user-visible capability (milestone state, master-detail, quick filters). A **Feature issue and a Story with stable IDs must exist in `docs/features.md` + GitHub before implementation**, and any `docs/requirements.md` change needs human approval in a dedicated PR. This document creates no IDs.

---

## 1. Subject & audience

**Subject:** the Samples view of one experiment in QBiC's Data Manager — a research data management platform for omics projects (genomics, proteomics, immunopeptidomics). Users are lab/research staff and data stewards registering sample batches from Excel metadata templates, plus PIs reviewing what has been registered.

**One job of this screen:** *show which samples this experiment has and let the user get to a single sample's full record, in a list dense enough to scan 50–500 rows.* Registration itself happens in `RegisterSampleDialog`, not on this page.

**The two failures to fix (from the product owner):** (1) the moment "sample registration just became possible" is invisible; (2) 12 flat columns (screenshot: Sample ID · Sample Name · Biological Replicate · Batch · Condition · Species · Specimen · Analyte · Analysis to Perform · Registration time · Modification time · Comment, horizontally scrolling, most values identical row to row) carry no scan path.

`assumption:` the primary audience of the milestone band is a **first-time user in their first project**; returning users must not be shown the same band as furniture.

---

## 2. Information architecture & flow

### Page bands (top → bottom)

| Band | Content | Visibility |
|---|---|---|
| **A. Page header** | `H1` "Samples", scope/status line, primary `Register samples` | Always (all states) |
| **B. Prerequisite gate / milestone band** | Either the *blocked* gate (no groups) or the *unlocked* milestone (groups, 0 samples) | Exactly one of the two, never both, never both-plus-samples |
| **C. Filter band** | search field, quick-filter chips, active-filter summary, "More filters" disclosure, Show/Hide columns | When ≥1 sample |
| **D. Grid** | identity columns only, checkbox column, frozen Sample ID, per-row *Details* affordance | When ≥1 sample (empty-state text when a filter matches nothing) |
| **E. Pager** | existing `PaginationBar` (owns the only total count on the page) | When total > 0 |
| **F. Detail column** | `SampleDetailPanel`, inline at ≥1100px | Only while a record is open |

### Task path (first-time user, the case the PO is fixing)

1. Enter from experiment **Overview** → Samples. Nothing registered yet, no groups.
2. Band B = **gate**: "Sample registration is blocked" + prerequisite + `Define experimental design` → routes to `ExperimentMainLayout` design surface (`ProjectRoutes.EXPERIMENT`).
3. User defines variables and the first experimental group there; returns to Samples.
4. Band B = **milestone**: "Sample registration is unlocked" + the facts that unlocked it + `Register samples` (primary). Skip point: the band is dismissible; the same action exists in band A, so dismissal never removes the path.
5. `Register samples` → `RegisterSampleDialog` (unchanged: template download + Excel upload). Interruption: dialog cancel is handled by `AppDialog`; the page state is untouched.
6. On success the toast (`sample.registered.success`) appears, the grid reloads via `refresh()` (existing `reloadSampleInformation` path), band B disappears (samples now exist) and the header line reports the count.
7. Scan → filter → open a row's *Details* → read the full record in band F (reversible: `Esc`/Close, or deselect).

### Primary / secondary / absent

- **Primary:** identity + status of a sample; the single next action for the current state.
- **Secondary (band F):** everything else (`SamplePreview` + `Sample` attributes), grouped.
- **Absent from the grid:** Biological replicate, Species, Specimen, Analyte, Analysis to perform, Registration time, Modification time, Comment — all move to band F. They remain reachable in the grid only through the existing Show/Hide Columns menu (power mode).

### Explicitly not adopted

- **`experimentFilter()` from the prototype does not apply.** Scope is fixed by the route (`EXPERIMENT_ID_ROUTE_PARAMETER`, `AppRoutes.ProjectRoutes.SAMPLES`); the experiment column/quick-filter belongs to a *project-level* Samples page, which does not exist in this codebase yet (`ProjectQueryService.getProjectSamples`/`ProjectSampleFilter` already model it). Replacing the filter with a scope line in band A.
- **`sampleOverviewStats()` 4-cell rollup is not adopted** at experiment scope: the pager already owns the total (avoid two counts), and measured coverage is expressed as counts on the Status filter chips.
- **The project-wide Samples page is out of scope and deferred.** A project-level page ("every sample in the project, across all experiments", experiment as a filter chip) is a separate product decision the product owner has explicitly deferred. A read model for it already exists (`ProjectQueryService.getProjectSamples` / `v_project_sample_overview`) and is deliberately left untouched. This plan must not depend on it, and no slice may assume project scope. See `sample-page-service-layer-gap-analysis.md` → "Parent verification".

---

## 3. "Registration unlocked" — the milestone

Three distinct states, three distinct treatments. They must not read as variants of one card.

| State | Trigger (as in `setBatchAndSampleInformation()`) | Component | Tone |
|---|---|---|---|
| **Blocked** | `experiment.getExperimentalGroups().isEmpty()` | **`SamplePrerequisiteGate`** (new; `Div`, `Hyphen style dashed border + `--shade-color-5pct` surface, `VaadinIcon.LOCK`) | neutral — a missing prerequisite is **not** an error and **not** amber (the prototype's rule: locked is neutral/dashed, per `pt-step--locked` / `project-progress-step--locked`) |
| **Unlocked, 0 samples** | groups exist, `getSamples(...)` empty — today's `showRegisterBatchDisclaimer()` | **`RegistrationMilestoneBand`** (new; `Div`, `--primary-color-10pct` surface + `--primary-color-20pct` border, `TagColor.SUCCESS` "Unlocked" tag) — **this is the one loud element on the page** | positive achievement + next action |
| **Populated** | ≥1 sample | nothing. Band B is absent; band A's status line says `100 samples registered · last registered 22 Jan 2026 16:17` | quiet |

**Why this replaces `noSamplesRegisteredDisclaimer`:** today the unlocked-and-empty state hides *the whole page* (`content.setVisible(false)` in `showRegisterBatchDisclaimer()`), so the user sees a centred "Manage your samples in one place / Start your project by registering the first sample batch / Register batch" prompt with **no page title, no experiment name, no evidence of what they just achieved**. The milestone band keeps the page (header + scope + the achievement) and moves the invitation into the empty grid area as text only, so there is exactly **one** call to action on screen.

**Copy (band B, unlocked):**
- Title (`H2`): `Sample registration is unlocked`
- Body: `This experiment has 1 experimental group and 2 variables, so every registered sample can be assigned to a condition. Register your first batch with the metadata template.` (numbers and pluralisation from `Experiment.getExperimentalGroups()`, `ExperimentInformationService.getVariablesOfExperiment(...)`)
- Primary: `Register samples`
- Quiet link in the body: `Review experimental design` → design surface
- Band tag: `Unlocked` (`TagColor.SUCCESS`) — the word carries the state, green only reinforces it.

**Copy (band B, blocked):**
- Title: `Define experimental design first`
- Body: `Every registered sample is assigned to an experimental group, so this experiment needs at least one group before samples can be registered. Groups are built from the variables: variables, then groups.`
- Primary: `Define experimental design`
- Blocked-by evidence line: `0 experimental groups · 0 variables` (from the same services; never a bare "0").

**Dismissal:** the milestone band gets a close control (`VaadinIcon.CLOSE`, `aria-label="Dismiss registration notice"`) that hides it for the rest of the session and per experiment. Mechanism: `VaadinSession` attribute keyed like `DataManagerLayout.DRAWER_STATE_KEY` (`"sampleMilestoneDismissed:" + experimentId`). Not persisted server-side, so a new session/new user sees the achievement again; deleting every sample re-arms it automatically because the state is derived, not stored.
**Never dismissible:** the blocked gate (it is the only path forward) and the header action.

---

## 4. Layout concept

**One sentence:** a capped, centred shell (the app's `.main.project`/`.main.experiment` geometry, `--content-shell-max-width`) holds a left-aligned single column — header, milestone, filter band, grid, pager — which becomes a 2-column master-detail grid only while a record is open; left-aligned throughout, no centred blocks.

**Alignment & rhythm:** left-aligned to the shell gutter `--content-shell-gutter` so the page lines up with the chrome rows above it; vertical rhythm from the existing spacing scale (`--lumo-space-s` inside a band, `--lumo-space-m`/`--lumo-space-l` between bands), matching `div.css`/`page-area.css` conventions. Bands are borderless (main.css comment: "The project page's bands are borderless; separation is whitespace"); only the grid and the milestone band carry a hairline.

**Responsive:** ≥1100px two columns (detail inline); 1100–768px single column, detail as a right drawer with backdrop (below); <768px single column, filter chips horizontally scroll within their row, grid is horizontally scrollable with the Sample ID column frozen (already `setFrozen(true)` today).

### Chosen layout

```
 ┌ Projects / Q256A6 / Proteomics / Proteomics ─── [Samples]  Measurements  Raw data ┐   ← existing chrome
 ├──────────────────────────────────────────────────────────────────────────────────┤
 │ Samples                                          [+ Register samples]            │ A
 │ 100 samples registered · last registered 22 Jan 2026 16:17                       │
 ├──────────────────────────────────────────────────────────────────────────────────┤
 │ [✓ Unlocked]  Sample registration is unlocked                                    │ B
 │ This experiment has 1 experimental group and 2 variables, so every registered     │
 │ sample can be assigned to a condition. Register your first batch with the …       │
 │ [ Register samples ]                     Review experimental design        [✕]    │
 ├──────────────────────────────────────────────────────────────────────────────────┤
 │ [Search samples…]  Status: All ▾ Time: 0 h 100 h Batch: 3  More filters ▾        │ C
 │                    Show/Hide columns ▾                                            │
 │ 3 filters active · Clear all            Batch 3 ✕ · Time: 100 h ✕ · Measured ✕   │
 ├──────────────────────────────────────────────┬───────────────────────────────────┤
 │ ☐  Sample ID    Sample name   Condition  …   │ Panel                            │
 │ ☐  Q256A6001AO  New Sample 14 Time: 0 h  …   │                                  │ D/F
 │ ☑  Q256A6002AX  New Sample 68 Time: 100h …   │                                  │
 ├──────────────────────────────────────────────┴───────────────────────────────────┤
 │ 100 samples · Page 1 of 9        [12 ▾ per page]                                 │ E
 └──────────────────────────────────────────────────────────────────────────────────┘
```

### Rejected alternative A — keep the flat grid, add a column-group header

```
 │ Sample ID │ Sample name │ Replicate │ Batch │ Condition │ Species │ Specimen │ Analyte │ …
 │           │ ◄── identity ──► │ ◄──────── design-level (constant per experiment) ────────► │
```
Rejected because: the user must decide per row which of 12 columns matter (the PO's complaint), horizontal scrolling is mandatory at 1440px (visible in `img_14.png`), and there is still **no single-record view** anywhere — the edit dialog is the only place a record's full metadata can be read. The axis is wrong: a table with 100 rows of one repeated Species value is 100 copies of a constant.

### Rejected alternative B — full-page drill-down (`projects/:projectId/samples/:sampleId`, `ProjectRoutes.SAMPLE` exists)

Rejected: loses list context on every inspection, costs a route round-trip per record, and makes a left/right scan impossible. `Project-management-refactor.md` already settled this ("opened per record in a side panel (keeps the list context)"). Kept as a **deep link** (`?record=Q2…`) produced *from* the panel, not as the navigation model.

### Rejected alternative C — prototype's overlay panel as-is

The prototype's `.pt-overlay`/`.pt-panel` (fixed `inset: 0`, `width: min(440px, 94vw)`, 28% black backdrop) is right for a 6-row demo and wrong here: it covers the list exactly when scanning matters, and it makes a read-only panel modal (focus trap, backdrop dismissal) for no reason. **Adopted instead:** inline column ≥1100px; the app's existing drawer mechanics below (see §7).

---

## 5. Tokens

All values below already exist in `frontend/themes/datamanager/components/variables.css` and are reused; **no new colour is introduced.**

| Role | Token (existing) | Used for |
|---|---|---|
| Page ink | `--shade-color-90pct` (`--body-text-color`) | body copy, grid cells |
| Heading ink | `--heading-text-color` (`--shade-color`) | `H1`/`H2`, band titles |
| Subtle ink | `--shade-color-50pct` (`--secondary-text-color`) | field labels in the panel, scope line |
| Action | `--primary-color`, `--primary-text-color`, `--primary-contrast-color` | exactly one solid button per band |
| Milestone surface | `--primary-color-10pct` + `--primary-color-20pct` | band B (matches `project-progress-step--active`, the app's "the next move" semantic) |
| Unlocked tag | `--lumo-success-color-10pct` + `--success-text-color` (`.tag.success`) via `Tag(TagColor.SUCCESS)` | "Unlocked" |
| Locked / gate | `--shade-color-5pct` surface, `1px dashed --border-color-default` | band B blocked; matches `project-progress-step--locked` |
| "Not measured" | `Tag(TagColor.CONTRAST)` (grey) | normal state, **not** amber — amber is reserved for genuine warnings (`variables.css` info/attention comment) |
| Danger | `--error-text-color`, `--error-color-10pct` | Delete button (`ButtonVariant.LUMO_ERROR`, already used) |
| Grid lines | `--lumo-contrast-10pct` / `-20pct` | grid hairline, filter chip border |
| Identifier | `--font-family-monospace` (`.identifier`) | Sample ID in grid and panel |
| Weights | `--font-weight-medium/semi-bold` | labels, tags |
| Space | `--spacing-01..12` (`--lumo-space-xs/s/m/l`) | all gaps/padding |
| Shell | `--content-shell-max-width`, `--content-shell-gutter`, `--app-density`, `--card-readable-width` | page geometry, panel breakpoint basis |
| Icons | `--icon-size-xs/s/m/l` | lock, close, chevron |

**Type scale** (Lumo, existing): `H1` + `.title` (`--lumo-font-size-xxl`, semi-bold) · `H2` band title (`--lumo-font-size-l`, semi-bold) · body `--lumo-font-size-m` · grid/meta `--lumo-font-size-s` · chips/tags `--lumo-font-size-xs`. Panel group titles: `.heading-6` (`--lumo-font-size-xs`, semi-bold) — **not** uppercase (the app has no uppercase-label convention; the prototype's `.pt-panel__group-title` uses caps and is not adopted).

**New tokens (only these):** `--sample-detail-panel-width: 24rem`, `--sample-detail-panel-min-width: 20rem`, `--sample-detail-breakpoint: 1100px` (as a documented media-query value; CSS has no custom-media in this stack). All in the new `sample-page.css`.

**Chips/tags:** filter chips reuse the pill geometry of `.project-exp-chip` (`border-radius: 999px`, `--lumo-font-size-xs`, `2px var(--lumo-space-s)` padding) with a selected variant (accent border + accent ink, **not** a solid accent fill — the solid accent belongs to the single primary button, per the prototype's own note on `.pt-toggle[aria-pressed=true]`). Status uses `Tag`, not a chip.

---

## 6. Quick filters

All facets are **server-side**; nothing is filtered in memory (rows arrive a page at a time from `AsyncProjectService.getSamplePreviews`).

| # | Facet | Values from | Selection | Tier |
|---|---|---|---|---|
| 1 | **Experimental condition** — one chip group per experimental variable, labelled `Time:`, `Treatment:` | `Experiment.getExperimentalGroups()` → `subject.experimentalDesign.getExperimentalGroups()`; each group is one level per variable | **multi-select** over groups; OR within a group of levels; AND across chip groups (matching `experimentalGroupId IN (...)`) | 1 |
| 2 | **Batch** | distinct batch labels in this experiment (`SamplePreview.batchLabel`; needs a new distinct-value query) | multi-select; OR | 1 |
| 3 | **Measurement status** | per-sample measured flag (below) | **single-select**: `All · Measured · Not measured`, segmented chips, default `All`; mirrors `ProjectQueryService.MeasuredFilter` | 1 |
| 4 | **Sample origin** | `ExperimentInformationService.getSpeciesOfExperiment / getSpecimensOfExperiment / getAnalytesOfExperiment` | three independent facets, multi-select, OR within, AND across | 2 (disclosure) |
| 5 | **Analysis method** | `AnalysisMethod.values()` filtered to those present in the experiment | multi-select; OR | 2 (disclosure) |

**Tier 1 / tier 2:** tier 1 renders inline in band C (Status + up to two condition chip groups); tier 2 lives behind a `Details` disclosure titled `More filters` (the app already uses `Details`, e.g. `SharedWithSummary`). Adaptation rule: further condition variable groups and Batch move to tier 2 only when the row would otherwise wrap at the 1100px breakpoint. Editors: `CheckboxGroup` (vertical, `flex-vertical`) for ≤15 values (species/specimen/analyte/analysis method), `MultiSelectComboBox` for Batch when >15 (an experiment can have hundreds).

**Facet counts:** only the **Status** facet shows counts (`Measured 12`, `Not measured 88`) — one extra count query, and it is the facet that drives attention. Other facets show no counts in v1 to avoid five additional count round-trips (flagged, §11).

**Active filters & clearing:** a summary row under the chips — `3 filters active · Clear all` followed by one removable chip per active value (`Batch 3 ✕`, `Time: 100 h ✕`, `Measured ✕`, each `aria-label="Remove filter Batch 3"`). `Clear all filters` resets facets to empty, `Status` to `All`, and page to 1; it does **not** clear the search field (separate control, separate intent). Result totals are **not** re-stated here — `PaginationBar` already prints `100 samples · Page 1 of 9`.

**Where filters live in the URL:** `ListStateCodec` carries only `page/size/q/sort`. Add `views/projects/project/samples/pagination/SampleQuickFilters.java` (immutable record) and `SampleListStateCodec.java`, following the `MeasurementListStateCodec` precedent: **reuse `ListStateCodec.parse/toQueryParameters` for the flat part** and add repeatable parameters `f.status=measured|not-measured`, `f.batch=…`, `f.group=…`, `f.species=…`, `f.specimen=…`, `f.analyte=…`, `f.method=…`, plus `record=<sampleCode>` for the open detail. Not a second flat codec.

---

## 7. Side panel (band F)

**Component:** `SampleDetailPanel extends Div` (new, `views/projects/project/samples/`), hosting `DetailGroup`/`DetailField` — the prototype's three private records (`ProjectPagePrototypeDemo.java` lines ~136–140) promoted to package-visible types in the panel component, since production has no equivalent.

**Layout component & placement (the asked question):** hosted by a new `.sample-detail-layout` `Div` inside `SampleDetailsComponent`:

```
.sample-detail-layout { display: grid; grid-template-columns: minmax(0,1fr); gap: var(--lumo-space-l); }
@media (min-width: 1100px) {
  .sample-detail-layout--open { grid-template-columns: minmax(0,1fr) var(--sample-detail-panel-width); }
  .sample-detail-panel { position: sticky; top: var(--lumo-space-m); max-height: calc(100vh - 8rem); overflow-y: auto; }
}
```
**Inline column, not overlay** (≥1100px): the list stays visible and scannable while the record is read, the grid does not move under the pointer, and no focus trap is needed for a read-only surface. **Below 1100px** the panel becomes a right-anchored drawer reusing the mechanics already in the codebase (`ConnectDatasetSidebar`'s `.cds-backdrop` + fixed `.cds-panel`, or `ProjectSharingDrawer`): backdrop click / `Esc` closes, focus moves into the panel and returns to the invoking row's *Details* button.

**Field groups** (prototype's grouping kept; improved where the domain demands):

| Group | Fields | Change from prototype |
|---|---|---|
| **Sample identity** | QBiC sample ID (mono, `.identifier`, copy button), Sample name, **Registration time**, **Modification time** (client time zone via `ExtendedClientDetails`, as `SampleDetailsComponent.formatTime` already does) | prototype carried ID + name only; times added (they were grid columns) |
| **Condition** | one row per experimental variable (`Time` → `100 h`), then confounding variables if any | prototype collapsed this to one `Condition` line; an experiment has N variables, so N rows — otherwise multi-variable conditions are unreadable |
| **Sample origin** | Species, Specimen, Analyte (label + CURIE in `title`/tooltip, per `docs/plans/ontology-term-identity-ui-review.md`) | unchanged from prototype's three fields |
| **Laboratory** | **Batch**, **Biological replicate**, Analysis to be performed | prototype split Batch into "Sample Properties" and Analysis into its own group; merged into one lab group |
| **Status** | **Measurement**: `Measured` / `Not measured` `Tag` + `3 measurements` when measured | **new group** — the PO asked for measurement status, and it is the only attribute in the panel that drives attention |
| **Notes** | Comment (`—` when empty) | unchanged |

Panel header: `H2` = sample name, subtitle = `Q256A6001AO · <experiment name>` (mono ID, exactly as the prototype's `.pt-panel__title/.pt-panel__subtitle`), close button (`LUMO_TERTIARY_INLINE`, `aria-label="Close sample details"`). Panel actions, quiet: `Edit sample` (reuses `EditSampleDialog` with a single `SampleId`), `Delete sample` (`AlertDialog.danger` as in `onDeleteSamplesClicked`), `Open as page` (links `ProjectRoutes.SAMPLE`) — the panel itself carries no primary button, because the page's primary remains registration.

**Data source:** the open record is resolved from the already-loaded `SamplePreview` **plus** the full `Sample` via `SampleInformationService.retrieveSamplesByIds(projectId, List.of(sampleId))` (already exists, ACL-guarded) — the preview lacks the variable-level list and confounding variables. Load state: the panel shows a `Loading sample…` status line while that call is in flight (`aria-busy` on the panel).

---

## 8. Component breakdown (concrete classes)

| UI element | Vaadin / project class | Notes |
|---|---|---|
| Page shell + bands orchestration | `SampleInformationMain` (`@Route` owner) | bands A/B; keeps `beforeEnter`/URL-history handling unchanged |
| Page title | `H1` + class `title` (was a `Span`) | heading landmark; nav item is already `Samples` (`AppNavigationComponent.forExperiment`) so "View Samples" is dropped (refactor doc item 10) |
| Blocked gate | **new** `SamplePrerequisiteGate extends Div` | dashed border, `VaadinIcon.LOCK`, one primary |
| Milestone band | **new** `RegistrationMilestoneBand extends Div` | `Tag(TagColor.SUCCESS)` + facts + primary + dismiss; fires `MilestoneDismissedEvent` |
| Filter band host | **new** `SampleQuickFilterBar extends Div` | search (`TextField`, kept from `SampleDetailsComponent`), chip groups, `Details` disclosure, active-filter summary, Show/Hide columns `MenuBar`+`CheckboxGroup` (reused verbatim) |
| Facet editors | `CheckboxGroup` (vertical) / `MultiSelectComboBox` | `CheckboxGroupVariant.LUMO_VERTICAL` |
| Grid | `PaginatedGrid<SamplePreview>` (unchanged, externally-controlled mode) + `Grid`/`Column`/`ComponentRenderer` | per-item `Grid` multi-select retained for bulk |
| Identity columns | `Column`s on `PaginatedGrid.grid()` | Sample ID (`.identifier` renderer, `setFrozen(true)`), Sample name, Condition (`ComponentRenderer` → `Tag` per variable level, reuse `createConditionRenderer`), Batch, Status (`Tag`) |
| Details affordance | `Button` (icon-only, `VaadinIcon.ANGLE_RIGHT`, `LUMO_TERTIARY_INLINE`, `aria-label="Open details for Q2…"`) in a `setFlexGrow(0)` column | see §10 for why not row-click |
| Bulk selection bar | existing `selectionBar` (`selectionDisplay`, `selectAllResultsButton`, `clearSelectionButton`) | unchanged behaviour; add live region |
| Pager | existing `PaginationBar` | page sizes stay `ListStateCodec.ALLOWED_PAGE_SIZES`; default raised to 24 (§11) |
| Detail panel | **new** `SampleDetailPanel extends Div` + `DetailGroup`/`DetailField` | §7 |
| Empty states | `EmptyState` (`views/general/EmptyState.java`) | text-only variant (no action button) inside the grid area; the band carries the action |
| Auxiliary note | `ContextNote` | e.g. "Editing metadata of measured samples is limited." where applicable |
| Dangerous confirms | `AlertDialog.danger(...)` | unchanged; reuse the 6-arg production form |
| Feedback | `MessageSourceNotificationFactory.toast / pendingTaskToast` | unchanged keys: `sample.registered.success`, `sample.deleted.success`, `sample.updated.success`, `task.failed` |

**New CSS only:** `frontend/themes/datamanager/components/sample-page.css` (imported from `custom.css`) — `.sample-header-band`, `.sample-milestone`, `.sample-gate`, `.sample-filter-band`, `.sample-filter-chip`, `.sample-filter-summary`, `.sample-detail-layout`, `.sample-detail-panel`, `.sample-detail-group`. **Edits to existing files:** `grid-templates.css` (`.main.sample` → capped centred shell like `.main.experiment`, single column), `page-area.css` (`.sample-details-component .sample-toolbar` / `.sample-selection-bar` kept; drop the `.page-area` border for this page so bands are borderless like the project page). **Do not** import or reuse the prototype's `pt-*` classes (the file is dev-only and every selector is namespaced under `.pt-app`).

---

## 9. State & edge coverage

| Region | Default | Loading | Empty | Partial | Error | Permission denied |
|---|---|---|---|---|---|---|
| Band B | hidden when samples exist | — | *this is the state*: the milestone (§3) | — | — | gate shown, `Define experimental design` hidden if the user cannot write the project |
| Band C (filters) | chips unselected, `Status: All` | chips disabled while a page loads | — | facet values present in the experiment only; a facet with 1 value still renders (honest) | — | whole band hidden |
| Band D (grid) | page of identity columns | `Loading samples…` status line in the grid area + `aria-busy="true"` on the grid container (today `loadPage` blocks with `blockOptional()` and shows **nothing**, which looks frozen) | **two distinct empties**: (a) no samples at all → text only: `No samples registered yet. Use Register samples to upload the first batch.`; (b) filtered to empty → `No samples match these filters.` + `Clear all filters` action; never the same string | pager total > rendered rows; frozen ID column; horizontal scroll <1100px | error row above the grid: `Samples could not be loaded.` + `Try again` (re-runs the page load) + toast `task.failed`. **Today a backend failure is swallowed** — `blockOptional().orElse(0)` in `loadPage` renders an empty list, so a failure is indistinguishable from "no samples". Must be fixed. | `AccessDeniedException` → `You do not have permission to view samples in this project.` + `Request access` (routed to `ProjectRoutes.ACCESS` when `userPermissions.changeProjectAccess`) |
| Band F (panel) | closed | `Loading sample…` in the panel body while the `Sample` fetch runs | — | a field with no value renders `—`, never blank | `Sample details could not be loaded.` + `Try again`, Close stays enabled | not reachable (rows are not rendered) |

**Selection states:** single (one checkbox) · multi (page) · cross-page (`Selection` id set, survives page/filter/sort — ADR-0009) · "all N matching" via `selectAllMatching()` · cleared by `Clear selection` or by deleting the selected rows (`deselectDeletedSamples`). The panel's open record is **independent** of that selection set and survives paging (the ID stays in the URL).
**Bulk actions:** `Export` (enabled with a selection; downloads via the existing `sampleInformationTemplate` + `DownloadComponent`), `Edit` (always enabled — the dialog offers "all metadata" and "selected"), `Delete` (enabled with a selection; blocked when any selected sample is measured → the existing `showBlockedDeletionDialog` list).
**Filtered-to-empty + selection:** if a filter change leaves selected IDs outside the filter, the count stays and the summary chip row shows the selection count with `Select all N matching` → the existing scope-disambiguation copy in `updateSelectionBar()` is retained.

---

## 10. Interaction & behaviour

- **Row → detail:** the per-row *Details* icon button opens the panel (`aria-label` carries the sample code). **Not** row-body click: the grid's `MULTI` selection model toggles selection on any row click (and `PaginatedGrid.configureGrid()` forces `MULTI`), so making the row body mean "open" would silently add the row to a bulk selection — and a subsequent Delete would then hit a record the user never checked. Same keyboard and mouse path: `Tab` to the button, `Enter`/`Space`.
- **Open/close:** opening sets `?record=Q2…` via `History.pushState` (existing `writeUrl` pattern) and moves focus to the panel `H2` (`tabindex="-1"`); `Esc` or `Close` clears the parameter, closes the panel, returns focus to the invoking *Details* button. In drawer mode (<1100px) `Esc` and backdrop click close with the same focus return.
- **Hover/focus:** row hover = `--shade-color-5pct` background (no change to selection); focus ring = `:focus-visible` 2px `--primary-color`, offset 2px; chips and buttons keep their own visible focus treatment. Transitions only inside `@media (prefers-reduced-motion: no-preference)` (prototype's pattern).
- **Touch targets:** all icon buttons ≥ `--lumo-size-m` min-height/min-width; the chip hit area keeps `2px var(--lumo-space-s)` padding so a chip is not a 20px target.
- **Filter changes:** any facet change resets to page 1 (existing `withPage(1)` convention) and rewrites the URL.
- **Bulk actions:** Export → pending toast → download; Edit → `EditSampleDialog`; Delete → `AlertDialog.danger` **confirm** (irreversible + cascades to connected data — the existing copy already says so). Registration → no confirm, `RegisterSampleDialog` performs template/upload validation and `SampleRegistrationComponent` shows validation errors inside the dialog.
- **Validation errors** appear in the dialog footer/step body (unchanged), never as a page-level banner.
- **Undo:** none offered. Confirm-only for destructive operations (matches `front-end-components.md`).
- **Read-only users:** hide (not disable) `Register samples`, `Edit`, `Delete`, and panel `Edit`/`Delete`; gate on `userPermissions.editProject(projectId)`. **Today the sample page never checks write permission** (unlike `MeasurementMain.openRegistrationDialog`), so a read-only user is offered actions that can only fail.

**Accessibility floor (WCAG 2.1 AA):**
- Landmarks/headings: `H1` "Samples"; band `H2`s (`Registration`, `Samples`/grid title, panel title); panel groups `H3`. `main`/`region` come from the existing shell.
- Side panel: `role="region"` + `aria-labelledby` the panel `H2`; **not** `aria-modal` in inline mode; in drawer mode reuse the modal drawer semantics with a focus trap and `Esc`.
- Selection count (`selectionDisplay`) becomes a live region: `role="status"`, `aria-live="polite"`, `aria-atomic="true"` (today it is a plain `Span`; the count changes without announcement). The active-filter summary reuses the same pattern.
- Grid: Vaadin `Grid` already exposes `role="grid"`/column headers; every icon-only control has an explicit `aria-label`; the Details button's label includes the sample code so a screen-reader row list is meaningful. The `Tag` text carries status, so colour is never the only signal.
- Contrast: body/label ink on the tints used here ≥4.5:1 (`--body-text-color` on `--primary-color-10pct`; check the accent tag at `--lumo-font-size-xs` — `--success-text-color` on `--lumo-success-color-10pct` is the app's existing pair). Verify the chip's selected ink against white; if under 4.5:1, use `--primary-text-color` with the accent border rather than accent ink.
- No content relies on hover: the CURIE is in `title` **and** in the field's accessible name.
- Density unchanged by zoom: `--app-density` remains the single root-size knob (`docs/plans/app-density-without-zoom.md`); this work adds no `zoom` and no fixed `px` type.

---

## 11. Copy (real strings)

| Where | String |
|---|---|
| `H1` | `Samples` |
| Header status line | `100 samples registered · last registered 22 Jan 2026 16:17` / `0 samples registered` |
| Header primary | `Register samples` |
| Milestone tag | `Unlocked` |
| Milestone title | `Sample registration is unlocked` |
| Milestone body | `This experiment has {G} experimental group(s) and {V} variable(s), so every registered sample can be assigned to a condition. Register your first batch with the metadata template.` |
| Milestone link | `Review experimental design` |
| Milestone dismiss | `Dismiss registration notice` (aria-label; ✕) |
| Gate title | `Define experimental design first` |
| Gate body | `Every registered sample is assigned to an experimental group, so this experiment needs at least one group before samples can be registered. Groups are built from the variables: variables, then groups.` |
| Gate primary | `Define experimental design` |
| Gate evidence | `0 experimental groups · 0 variables` |
| Search placeholder | `Search samples` |
| Filter tiers | `More filters` / `Fewer filters` |
| Active filters | `3 filters active` · `Clear all` · `Remove filter {label}` · `No filters active` |
| Grid empty (none) | `No samples registered yet. Use Register samples to upload the first batch.` |
| Grid empty (filtered) | `No samples match these filters.` + `Clear all filters` |
| Grid loading | `Loading samples…` |
| Grid error | `Samples could not be loaded.` + `Try again` |
| Permission denied | `You do not have permission to view samples in this project.` + `Request access` |
| Panel actions | `Edit sample` · `Delete sample` · `Open as page` · `Close sample details` |
| Panel loading/error | `Loading sample…` · `Sample details could not be loaded.` + `Try again` |
| Panel empty value | `—` |
| Panel status | `Measured` · `Not measured` · `3 measurements` |
| Delete confirm (unchanged) | `Samples will be deleted` / `Deleting these samples will also delete the data connected to them. Proceed?` / `Delete samples` / `Keep samples` |
| Toasts (unchanged keys) | `sample.registered.success`, `sample.updated.success`, `sample.deleted.success`, `sample.no-sample-selected`, `task.failed` |

Voice: sentence case, no mood, action names identical from button to toast (`Register samples` → "Samples registered successfully.").

---

## 12. Build handoff

**Order (each step independently reviewable):**
1. **Shell/geometry** — `.main.sample` capped centred shell, bands borderless, `H1`; delete the `no-samples` disclaimer path. No behaviour change beyond layout.
2. **State split** — `SamplePrerequisiteGate` + `RegistrationMilestoneBand` + derived state in `setBatchAndSampleInformation()` (replace the three `show*Disclaimer()` toggles), session-scoped dismissal. *Delivers the PO's problem #1 alone.*
3. **Identity grid + panel** — reduce the column set, add the Details column, `SampleDetailPanel` with the inline-column layout and the drawer fallback. *Delivers problem #2 alone; needs no backend change* (`SamplePreview` + `retrieveSamplesByIds` cover every field).
4. **Backend facets** — extend `AsyncProjectService.SamplePreviewFilter` with a facet record; thread the **whole** filter into `countSamples` (it currently passes only `filter.sampleName()` — with facets added, count and page would disagree and paginate wrong); extend `SamplePreviewSpecs` with `experimentalGroupId IN`, `batch IN`, `analysisMethod IN`, ontology-label equality per facet, and the measured predicate; add a distinct-batch-values query.
5. **Filter band + URL** — `SampleQuickFilterBar`, `SampleQuickFilters`, `SampleListStateCodec` (reusing `ListStateCodec` for page/size/q/sort), `?record=`.
6. **States & a11y pass** — loading/error/permission states, live regions, focus management, `userPermissions.editProject` gating.
7. **Tests (Spock, per AGENTS.md):** milestone state machine (blocked/unlocked/populated incl. the transition after registration and after deleting all samples), facet → `SamplePreviewFilter` mapping, codec round-trip, selection-vs-detail independence, count/page agreement under facets.

**Prerequisites outside this plan (do not redesign here):**
- `SamplePreviewLookup`/`SamplePreviewJpaRepository` have **no facet support** today — only a free-text `sampleName` substring; the sample page cannot filter by origin/batch/condition/measured without step 4.
- Per-sample measured status exists only as `ProjectSampleOverview.measured` (project scope, `v_project_sample_overview`) and as `MeasurementLookupService.findMeasuredSampleIds(...)` (id set). The experiment-scoped equivalent must be added in SQL — **do not** resolve it in memory.
- `docs/plans/Project-management-refactor.md` mentions nav badges for per-experiment counts; `AppNavigationComponent` has no badge support.
- Feature + Story (stable IDs) before code; `docs/requirements.md` change needs a dedicated human-approved PR.

**Risks / uncertainties:**
- **Blocking page loads.** `loadPage` calls `blockOptional()` inside the UI thread; with 96-per-page and facet counts this can visibly stall. Minimum bar is the loading status; a real fix (reactive load + `UI.access`) is a separate change to `PaginatedGrid`.
- **Two count-shaped queries** (page + total) must use the identical filter or the pager lies.
- **Facet values must come from the experiment's design**, not from all samples in the project: after data drift, a filter chip for a value no sample carries would return 0 rows and look broken.
- **Measured-facet performance**: an `EXISTS`-style predicate is required; a per-row lookup would be N+1.
- **Default page size 12 → 24** changes the meaning of every shared/bookmarked URL (`size` is explicitly encoded, so old links keep 12 — acceptable).

---

## 13. Open product decisions (proposed defaults, for a human)

| # | Decision | Proposed default | Why it needs a human |
|---|---|---|---|
| 1 | How does a row open the panel, given bulk selection owns row click? | Per-row *Details* button; bulk selection behaviour untouched | The alternative (row click opens, checkboxes decoupled from the grid's selection model) is better on a large screen but requires extending `PaginatedGrid` (it forces `MULTI`) and reworking `configureSelectionReconciliation` |
| 2 | Is the measurement-status facet in v1? | Show it only if the SQL predicate lands; otherwise omit the facet rather than compute in memory | Backend cost; a facet that silently disappears is a product statement |
| 3 | Milestone dismissal persistence | Session-scoped per user+experiment | Persisting per user needs storage + a "show again" affordance |
| 4 | Default page size for samples | 24 (from 12) | Density vs. load time; affects `ListStateCodec` defaults per route |
| 5 | Batch facet source | Distinct batches of this experiment | Project-wide batches would offer empty results |
| 6 | Condition filter granularity | One multi-select over experimental groups, labelled by condition | Per-variable independent selection is finer but can express combinations no group exists for (guaranteed empty result) |
| 7 | Power mode | Keep the existing per-column Show/Hide Columns menu; hidden-by-default columns = the retired ones | `Project-management-refactor.md` wanted "show all columns" URL-addressable; that adds state not covered by USER-R-03 |
| 8 | Panel width / breakpoint | 24rem inline ≥1100px, drawer below | 20rem gives a wider grid; 24rem keeps label/value on one line for long ontology labels |
| 9 | Panel actions in v1 | Include `Edit sample` + `Delete sample` (quiet) | Editing from the panel makes the panel a mutation surface; scoping it to the grid only is the conservative alternative |
| 10 | Hide vs. disable mutation actions for read-only users | Hide | Changes behaviour beyond the PO's statement; disabling is more discoverable but is a dead control |
| 11 | Row density | Lumo default; no user-facing density toggle | A compact mode would help 500-row scans but is a new global preference |
| 12 | `record=` deep link | Ship it | Additional URL-addressable state beyond USER-R-03 (noted in the refactor doc as needing a requirement/ADR decision) |