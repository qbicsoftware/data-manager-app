# 0009 — Paginated measurement lists with identifier-based cross-page selection

* Status: accepted
* Deciders: project team (via story refinement on FEAT-PAG-LIST-03, #1539; product decisions
  recorded on #1536)
* Date: 2026-09-14

Technical Story: [FEAT-PAG-LIST-03 — Paginated Measurement Lists with Cross-Page Selection](https://github.com/qbicsoftware/data-manager-app/issues/1539) — parent feature [FEAT-PAGINATED-LISTS](https://github.com/qbicsoftware/data-manager-app/issues/1536)

## Context and Problem Statement

[ADR-0007](0007-paginated-lists-and-list-state.md) pinned the architecture for explicit,
URL-restorable pagination of large entity collections and deliberately left the cross-page
**selection model (USER-R-02)** out of scope: *"the selection model will be refined with the
stories that need it (a follow-up ADR)"*.

The measurement lists (NGS / Proteomics / Immunopeptidomics tabs in
`MeasurementDetailsComponent`, hosted by `MeasurementMain`) previously rendered as
**scroll-loaded lazy Vaadin Grids** with the platform's `SelectionPreservationMode.PRESERVE_ALL`:
selected *item objects* were held in the session across lazy fetches. This breaks at
data-steward scale — a cross-page selection of hundreds of measurements pins hundreds of full
`MeasurementInfo` object graphs (each with sample lists, ontology records, organisation records)
into the Vaadin session — and it violated the pagination requirements: no explicit page
navigation, no honest total count except via the lazy item-count, no URL-restorable list state.

The feature boundary (per AGENTS.md / FEAT-PAGINATED-LISTS) forbids service-API changes: all bulk
actions (export, edit, deletion) already reduce the selection to **measurement ID strings** before
calling the service layer, so an identifier-based selection model requires no service API changes.

Additionally, while preparing this ADR the team discovered that the measurement lookup queries
**validate the requested `Sort` but never apply it**: `MeasurementLookup.lookup{Ngs,Pxp,Ip}
Measurements(...)` built the Spring Data `Pageable` with a hardcoded
`Sort.by(ASC, "measurementCode")`. The grids only reordered the fetched window client-side.
For explicit offset/limit pagination this is fatal — page boundaries would drift if the database
order disagreed with the visible sort. The pagination work therefore includes a small
infrastructure fix that applies the user's sort plus a deterministic tie-break
(`measurementCode ASC`), mirroring the ADR-0007 tie-break decision for projects.

## Decision Drivers

* USER-R-02: selections must survive page navigation and bulk actions must apply to the full
  cross-page selection; the number of selected items must always be visible.
* USER-R-01: bounded page size, page-navigation controls, visible total; **only the current page
  is rendered** — no continuous scroll-loading.
* USER-R-03: current page, page size, filter, sort order reflected in the URL; restorable via
  back/forward; shareable links.
* Data-steward scale: selections must not hold full item object graphs in the session; the
  selection is a set of identifiers.
* Feature boundary: **no service-API changes**; bulk actions already accept measurement ID lists.
* The measurement grids are real data tables (many sortable columns, pooled-sample popovers,
  ontology/ROR links, client-timezone date rendering) — the pagination must not regress grid
  functionality (contrast: the project overview, where the only Grid role was a lazy card
  container, ADR-0007 C1).
* The pinned platform ships no `Pagination` component; [ADR-0007](0007-paginated-lists-and-list-state.md)
  already committed to the in-house `PaginationBar`, `ListState`, `ListStateCodec`, and the
  History-API URL synchronisation pattern — this ADR reuses them unchanged.
* Product decisions recorded on the parent feature (#1536) during refinement: the "select all"
  action must mean **all measurements matching the active filter in backend storage** (so a full
  metadata export always works), and the selection snapshot **survives page, filter, and sort
  changes**.

## Considered Options

### Page source per tab

* [P1] Reuse the existing offset/limit lookup methods. Each tab keeps a `PaginationBar`; on every
  state change the view calls `lookup{...}(projectId, (page−1)*size, size, appliedSort, filter)` +
  `count{...}` and hands the current page to an in-memory Grid (`setItems`).
* [P2] Keep lazy `CallbackDataProvider` Grids and drive paging through the data provider
  (offset/limit in the callbacks); render only the current page's window.
* [P3] Full ADR-0007 C1 treatment: replace the Grids with plain DOM containers (cards/table),
  abandoning grid features.

### Selection model

* [S1] **Identifier-based selection owned by the view**: a `Set<String>` of measurement IDs per
  measure type (e.g. a `MeasurementSelection` component). Grid row selection is reconciled with
  the set on page renders; the count label reads the set; bulk actions read the set.
* [S2] Keep `SelectionPreservationMode.PRESERVE_ALL` item-object selection (status quo).
* [S3] Session-scoped selection (Vaadin `@SessionScope` bean).

### Select-all semantics

* [A1] **"Select all N measurements matching the active filter"** — resolves all matching
  measurement IDs in backend storage (one additional lookup query, limit = the count we already
  compute for the pager) and adds them to the selection set.
* [A2] Current page only (status quo from the lazy Grid's native select-all), honestly labelled.
* [A3] Server-side "select all" deferred to a follow-up (feature scope listed it as an open
  product decision).

### Drop/keep of multi-sort

* [M1] Single-sort paginated grids; the URL carries one `sort` param; the tie-break stays
  internal.
* [M2] Keep multi-sort; serialise multiple sort orders in the URL.

### Selection survival across filter/sort changes

* [F1] Selection is a snapshot of IDs and **survives** page, filter, and sort changes; a "Clear
  selection" affordance exists next to the count label.
* [F2] Selection clears on filter/sort change.

## Decision Outcome

Chosen: **P1 + S1 + A1 + M1 + F1**, reusing the ADR-0007 machinery (`ListState`, `ListStateCodec`,
`PaginationBar`, History-API URL synchronisation).

1. **Page source (P1):** each of the three tabs renders an in-memory Grid whose items are the
   current page, fetched via the existing offset/limit lookup methods with the active sort (see 4.)
   and filter. Only the current page is fetched and rendered; a single shared `PaginationBar`
   under the tab sheet reports "Page X of Y — N measurements" and drives page/page-size changes.
   Grid features (sortable columns, pooled-sample popovers, ROR links, client-timezone date
   rendering) are preserved.
2. **Selection model (S1):** a view-owned `MeasurementSelection` holds a `Set<String>` of
   measurement IDs per measure type (NGS / PxP / IP). On every page render the view reconciles
   the grid's row selection with the set (select rows whose IDs are in the set, deselect rows
   that are not). The selection count label always shows the **full** cross-page selection size.
   Selection changes never trigger a refetch. Bulk actions (export/edit/delete) read the ID set —
   the existing event contracts (`List<String> measurementIds`) and service calls are untouched.
3. **Select-all (A1):** a "Select all N measurements matching the current search" toolbar action
   (replacing the lazy Grid's page-scoped native header checkbox with an honest one). N comes
   from the same count query that feeds the pager, so it is always exact and filter-consistent.
   The action resolves the matching measurement IDs (one lookup query with `limit = N`; the count
   query is already paid for by the pager) and adds them to the selection set. Row checkboxes
   continue to toggle individual measurements. This guarantees a full metadata export of all
   matching measurements always works. If a pathological experiment ever makes the resolution
   query unacceptable, a dedicated `allMeasurementIds(filter)` lookup method is a clean follow-up
   (deliberately parked).
4. **Sort handling (M1) + infrastructure fix:** paginated measurement grids are single-sort; the
   URL carries one `sort=property:asc|desc` param. `MeasurementLookup.lookup{Ngs,Pxp,Ip}
   Measurements(...)` are fixed to (a) **apply** the validated user sort and (b) append
   `measurementCode ASC` as an internal tie-break (consulted only when the primary sort
   attributes tie; never user-visible as the primary order). This is the measurement analogue of
   the ADR-0007 project tie-break and is required for deterministic offset/limit pagination.
5. **URL state scale:** the measurements route gets a `tab=ngs|pxp|ip` query parameter plus the
   flat `page/size/q/sort` parameters of the **active** tab. In-session, each tab keeps its own
   `ListState`, so switching tabs restores each tab's last page/filter/sort even though the URL
   describes only the active tab. History semantics follow ADR-0007: `pushState` for
   page/page-size/sort changes, `replaceState` for debounced search input **and for tab
   switches** (a tab is a view mode, not history granularity — back/forward stays within the
   active tab's browsing flow).
6. **Selection survival (F1):** the selection set survives page, filter, and sort changes. After
   a deletion the deleted IDs are removed from the set; if the current page falls past the end
   (deletion or filter shrink), the page is clamped to the last valid page. A "Clear selection"
   button sits next to the count label.

### Positive Consequences

* Selections are lightweight (a set of strings) and survive arbitrary navigation, satisfying
  USER-R-02 at data-steward scale.
* Bulk actions already take measurement ID lists — zero service-API changes; the only backend
  change is the sort-application + tie-break fix.
* The full metadata export of all measurements matching a filter works out of the box, because
  select-all resolves against backend storage.
* The shared `ListState`/`ListStateCodec`/`PaginationBar`/History-API machinery is reused, so the
  measurement lists get USER-R-01/-R-03 for free, consistent with the project overview.
* The sort fix removes a correctness landmine for the whole pagination feature (measurements,
  raw data later).

### Negative Consequences

* Per-page in-memory Grids fetch one page at a time — no lateral lazy scroll. Bounded page sizes
  (≤ 96) keep this cheap; identical trade-off as the project overview (ADR-0007 C1).
* Multi-sort is dropped for the paginated measurement lists (single-sort UX); re-adding it later
  requires serialising multiple sort orders in the URL.
* "Select all matching filter" resolves IDs with one extra query — negligible at realistic scales
  (hundreds of measurements per experiment), parked follow-up if it ever becomes a bottleneck.
* Each tab maintains its own `ListState` and selection; the URL only restores the active tab's
  state (per-tab state of *inactive* tabs is lost on full page reload — a deliberate trade-off
  for a single URL).

## Pros and Cons of the Options

### P1 — Offset/limit lookup + in-memory current-page Grid

* Good, because it reuses the proven query path and enforces "only the current page is rendered".
* Good, because grid features are preserved.
* Bad, because it drops the lazy windowing — irrelevant at bounded page sizes (≤ 96).

### P2 — Keep lazy Grid, drive paging through callbacks

* Good, because it keeps the existing `FilterGrid` seam.
* Bad, because a lazy Grid renders a windowed "current page" only by lying about the total
  window — the item-count/selection semantics are Grid-native and the pager would fight the
  virtualizer; "only the current page is rendered" is not honestly satisfiable.

### P3 — Plain DOM containers (C1 verbatim)

* Good, because it is the ADR-0007 project-overview pattern.
* Bad, because it forces re-implementing rich table interactions (sortable columns, popovers,
  links) that a Grid already provides.

### S1 — Identifier-based selection owned by the view

* Good, because it holds only IDs in the session and survives navigation trivially.
* Good, because bulk actions already consume IDs.
* Bad, because row-level selection must be reconciled with the grid on each page render (small
  bookkeeping).

### S2 — Status quo (PRESERVE_ALL item objects)

* Good, because it is already implemented.
* Bad, because it pins full item object graphs in the session at scale and does not survive
  page switches without holding every visited page's items.

### S3 — Session-scoped selection bean

* Good, because it centralises state.
* Bad, because it leaks selection state across unrelated views/tabs and complicates lifecycle
  (clearing, per-measure-type scoping); the view-owned model is simpler and testable.

### A1 — Select all N matching filter

* Good, because the full export always works (the product requirement).
* Good, because N and the pager total come from the same count query — always consistent.
* Bad, because it is a decision the parent feature listed as "open" — now resolved in scope.

### A2 — Current page only

* Good, because it is the smallest change.
* Bad, because it breaks full metadata export across pages — the exact requirement USER-R-02
  exists for.

### A3 — Defer select-all

* Good, because it shrinks this story.
* Bad, because the export workflow would regress for large selections compared to today's
  page-scoped checkbox.

### M1 — Single sort

* Good, because URL round-trip is unambiguous and the tie-break stays internal.
* Bad, because users lose multi-column sort composition.

### M2 — Multi sort in URL

* Good, because it preserves today's UX.
* Bad, because the URL/serialisation gets ambiguous and the codec must grow a repeatable-parameter
  or composed-sort format — not justified by usage.

### F1 — Selection survives filter/sort changes

* Good, because it matches the product decision and the user story ("exactly the measurements I
  targeted").
* Good, because a snapshot of IDs costs nothing.
* Bad, because after a filter change the visible rows may not include all selected IDs — mitigated
  by the always-visible count label and the clear-selection affordance.

### F2 — Clear on filter/sort change

* Good, because it is conservative.
* Bad, because it violates the recorded product decision ("keep the selection is crucial").

## Links

* Refines [ADR-0007](0007-paginated-lists-and-list-state.md) (selection model for the paginated
  lists, deferred there) and implements [FEAT-PAG-LIST-03 #1539](https://github.com/qbicsoftware/data-manager-app/issues/1539)
* Implements [USER-R-01](../requirements.md), [USER-R-02](../requirements.md), [USER-R-03](../requirements.md)
* Follow-up: FEAT-PAG-LIST-02 (#1540) and FEAT-PAG-LIST-04 (#1538) reuse the selection model and
  per-tab URL state extracted here; the shared machinery may be generalised at that point.