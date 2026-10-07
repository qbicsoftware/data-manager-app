# 0007 — Paginated lists with URL-restorable list state

* Status: accepted
* Deciders: project team (via story refinement on FEAT-PAGINATED-LISTS, #1536 / #1537)
* Date: 2026-09-14

Technical Story: [FEAT-PAG-LIST-01 — Paginated Project Overview with Responsive Card Layout](https://github.com/qbicsoftware/data-manager-app/issues/1537) · FEAT-PAG-LIST-02..04 (projected: #1538, #1539, #1540) — parent feature [FEAT-PAGINATED-LISTS](https://github.com/qbicsoftware/data-manager-app/issues/1536)

## Context and Problem Statement

The application displays large entity collections (projects, samples, measurements, raw datasets) in
scroll-loaded (endless-scrolling) Vaadin Grids whose embedded scroll container replaces the native page
scroll. This breaks native browser behaviour — find-in-page, scroll position, back/forward navigation,
shareable URLs — and behaves unreliably on small screens. Requirements
[USER-R-01](../requirements.md) (explicit pagination with bounded page size, page navigation, total
count), [USER-R-03](../requirements.md) (list state reflected in the browser URL and restorable via
history) and [USER-NFR-01](../requirements.md) (no embedded scroll container; layout adapts to viewport
width) mandate replacing this pattern for all large entity collections.

This ADR pins the architecture for all four stories of FEAT-PAGINATED-LISTS, so the four views share one
mechanism instead of diverging. The cross-page **selection** model of USER-R-02 is deliberately **out of
scope** here: FEAT-PAG-LIST-01 has no bulk actions, and the selection model will be refined with the
stories that need it (a follow-up ADR).

## Decision Drivers

* USER-R-01: bounded page size, page-navigation controls, and a visible total count; continuous
  scroll-loading must not be used for these lists.
* USER-R-03: current page, page size, filter, and sort order reflected in the browser URL; back/forward
  restores prior list states; lists are shareable as links.
* USER-NFR-01: no embedded scroll container that traps the native page scroll; layout must reflow to the
  viewport width on tablets/phones.
* Data-steward scale: stewards can hold access to several hundred projects; search and sorting are the
  primary discovery mechanisms and the pager must always communicate location and total (e.g.
  "Page 3 of 21 — 500 projects").
* The pinned platform (Vaadin 25.2.5) ships **no `Pagination` component** — the pager must be built
  in-house.
* The backend already supports offset/limit pagination (`queryOverview(filter, offset, limit,
  sortOrders)` via `ProjectOverviewLookup` + JPA `Specification`); the feature boundary forbids
  service-API changes — a matching **count query is the only missing backend piece**.
* Default ordering stays **`lastModified` descending** — the behaviour stewards know today — **until the
  application supports per-user list preferences that survive a re-login** (stakeholder decision on
  #1537). After that, the default may become a persisted per-user setting.
* Offset/limit pagination requires a **deterministic total order**: without it, ties in the sort key
  drift between queries and items are duplicated or dropped across page boundaries.

## Considered Options

### Page source

* [P1] Keep the offset/limit query path; add a matching `count(filter, projectIds)` to
  `ProjectOverviewLookup`, served through a new `ProjectInformationService.countOverview(filter)`
* [P2] Switch the overview queries to Spring Data `Page<T>` (`findAll(spec, pageable)` returning
  `Page` with total)
* [P3] Keyset (seek) pagination on `lastModified` — no count, but no total and no arbitrary offsets

### URL ↔ list-state synchronisation

* [U1] History API with the URL as single source of truth: interactions write the URL via
  `getHistory().pushState`/`replaceState`, one `HistoryStateChangeHandler` re-applies state read from
  the URL (covers back/forward without a server-side view re-init), `BeforeEnterEvent` seeds state on
  load/reload/shared links
* [U2] `UI.navigate(...)` with `QueryParameters` on every state change — router re-enters the view
  (`BeforeEnter`) for every interaction, including each debounced keystroke
* [U3] Component-local state only, no URL synchronisation

### Pager component

* [G1] Custom, reusable `PaginationBar` component (prev/next, numbered window with ellipsis,
  "Page X of Y — N items", page-size selector)
* [G2] Platform `Pagination` component (prev/next + numeric input only)
* [G3] Minimal prev/next buttons only

### Card rendering

* [C1] Plain DOM container (`Div`) with a responsive CSS grid
  (`repeat(auto-fill, minmax(280px, 1fr))`); only the current page is rendered
* [C2] Keep a Vaadin `Grid` with a single component column as the card container (status quo)

## Decision Outcome

Chosen: **P1 + U1 + G1 + C1.**

1. **Page source (P1):** extend `ProjectOverviewLookup` with
   `long count(String filter, Collection<ProjectId> projectIds)` and implement it via
   `projectOverviewRepository.count(specification)`, reusing the existing filter + access-rights
   `Specification` minus the `distinct` predicate (the `project_overview` SQL view yields one row per
   project, so plain `count` equals `count(distinct)` and JPA count-`distinct` quirks are avoided).
   `ProjectInformationService.countOverview(filter)` resolves the caller's accessible project ids the
   same way as `queryOverview` (empty authentication → 0). No other service API changes.
2. **Ordering:** the default ordering remains **`lastModified` descending** — unchanged from today,
   so stewards still land on their most relevant projects first. To guarantee the deterministic total
   order required by offset/limit pagination, the service appends `projectCode` ascending as an
   internal **tie-break key only** (consulted solely when the primary sort attributes are exactly
   equal; not user-visible in the normal case and never the primary ordering). When per-user list
   preferences that survive a re-login are introduced, the hardcoded default may be replaced by the
   persisted user default; the tie-break mechanism remains.
3. **URL as single source of truth (U1):** route URLs carry `page`, `size`, `q`, `sort` query
   parameters. Interactions write the URL: `History.pushState` for page/page-size/sort changes
   (back/forward then steps through each list state), `History.replaceState` for debounced search
   input (avoids history spam). A single `HistoryStateChangeHandler` re-applies the state parsed from
   the URL and skips redundant application when the canonicalised query string is unchanged;
   `BeforeEnterEvent` seeds the initial state (direct load, reload, shared links). Back/forward
   restores a previous list state **without** a server-side view re-init, so the browser restores the
   document scroll position natively; only the current page's data is fetched and rendered.
4. **Pager (G1):** a custom, reusable `PaginationBar` (platform `Pagination` is unavailable):
   prev/next buttons, numbered page window with ellipsis, "Page X of Y — N items" label, and a
   page-size selector. Page size is bounded; concrete options are product decisions tracked in the
   story (FEAT-PAG-LIST-01: default 24, options 12/24/48/96). Page changes, search, and sort changes
   reset/clamp the page (filter/sort → page 1; out-of-range page → clamped to the last valid page).
5. **Card rendering (C1):** scroll-loaded Grid card containers are replaced by plain DOM containers
   styled with a responsive CSS grid so cards reflow into multiple columns on wide screens and a
   single column on small screens; the page scrolls natively. The `grid-templates.css` stopgap rows
   (`.main.project-overview` `100vh` floor, `.no-welcome`) are removed.
6. **List state as a shared value:** a small `ListState` record + query-parameter codec
   (parse/serialize, defaults, clamping, sort-option → `SortOrder` mapping) lives in the app module so
   all four stories reuse one mechanism. Selection (USER-R-02) is intentionally not part of this
   decision.

### Positive Consequences

* Native web behaviour is restored on all large lists: find-in-page, native page scroll, back/forward,
  bookmarkable/shared list states (USER-R-01, USER-R-03, USER-NFR-01).
* One shared mechanism (`ListState` codec + `PaginationBar`) covers all four stories; no duplication.
* Minimal backend surface: the count method is the only service change; the existing query path is
  reused unchanged.
* History-driven re-application guarantees that the URL is always the complete source of truth — no
  client/server state divergence on reload or navigation.

### Negative Consequences

* Each browser-history event triggers a new data fetch and a re-render of the current page; acceptable
  because page sizes are bounded (≤ 96 items) and the project overview is not a high-frequency surface.
* Each route view must register its history handler and seed from `BeforeEnter` (repeated per list
  view, mitigated by the shared codec/helper).
* Equal `lastModified` values are now disambiguated by `projectCode` instead of the previous
  (unstable) DB-dependent tie order — only observable when sort keys tie exactly and otherwise
  invisible.
* The hardcoded default ordering is a stopgap until persisted per-user preferences exist.

## Pros and Cons of the Options

### P1 — Offset/limit + count

* Good, because it reuses the proven query path and needs no service-API redesign.
* Good, because the count reuses the exact filter/access specification, so the total always matches the
  filter.
* Good, because it provides a true total for the pager label ("Page X of Y — N items").
* Bad, because deep offsets are O(offset) for the DB — irrelevant at stewards' realistic scales
  (hundreds of projects, not millions).

### P2 — Spring Data `Page`

* Good, because totals come free from the repository.
* Bad, because it changes the service API signature, which the feature boundary forbids.

### P3 — Keyset pagination

* Good, because it is O(1) per page for the DB.
* Bad, because it cannot provide a total count (USER-R-01) without a separate count anyway, and it
  complicates arbitrary offset/sort changes.

### U1 — History API, URL as source of truth

* Good, because back/forward and shareable links work with the browser's own semantics.
* Good, because interactions only update the URL; the view instance survives, preserving native scroll.
* Bad, because state re-application must be written carefully (parse once, guard against redundant
  application).

### U2 — Router navigation per change

* Good, because routing does the state bookkeeping.
* Bad, because every debounced keystroke re-enters the view (full server round-trip + re-render), and
  scroll restoration after back/forward is not guaranteed for server-side views.

### U3 — No URL state

* Good, because it is the least work.
* Bad, because it fails USER-R-03 outright.

### G1 — Custom PaginationBar

* Good, because it is the only path to a rich pager on the pinned platform.
* Good, because it is styled with the app design system and reused across stories.
* Bad, because it is in-house code that must be maintained and tested.

### G2 — Platform Pagination

* Good, because it would be zero in-house code.
* Bad, because the component is not available in Vaadin 25.2.5 (dropped from the platform).

### C1 — Plain container + responsive CSS grid

* Good, because CSS grid reflows cards to any viewport (USER-NFR-01) and the page scrolls natively.
* Good, because it renders only the current page's cards (bounded DOM, no virtualizer needed).
* Bad, because it drops the Grid's lazy fetching — irrelevant at bounded page sizes (≤ 96).

### C2 — Grid as card container (status quo)

* Good, because it is already implemented.
* Bad, because the Grid prevents responsive multi-column reflow and traps the page scroll — the exact
  problems USER-NFR-01 and USER-R-01 exist to fix.

## Links

* Refines [FEAT-PAGINATED-LISTS #1536](https://github.com/qbicsoftware/data-manager-app/issues/1536) and implements [FEAT-PAG-LIST-01 #1537](https://github.com/qbicsoftware/data-manager-app/issues/1537)
* Implements [USER-R-01](../requirements.md), [USER-R-03](../requirements.md), [USER-NFR-01](../requirements.md)
* Supersedes the `grid-templates.css` scroll stopgap for `.main.project-overview` ("REMOVE once
  pagination replaces endless scrolling in data grids")
* Follow-up: ADR for the identifier-based cross-page selection model (USER-R-02) with FEAT-PAG-LIST-02..04