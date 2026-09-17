# UX Findings — Paginated Measurement View (FEAT-PAG-LIST-03)

> **Context:** Combined code + screenshot review of the paginated measurement lists
> (branch `feature/feat-pag-list-03-measurement-pagination`, ADR-0007/0009,
> requirements USER-R-01/-R-02/-R-03, USER-NFR-01).
> Review method: structural code analysis plus a visual pass on `img_1.png`
> (screenshot of the Genomics tab, 15 measurements, all selected).
> This file tracks remediation status. Update statuses as fixes land on the feature branch.

## Status legend

- 🔴 Open — not yet addressed
- 🟡 In Progress — partially implemented
- 🟢 Done — implemented on the feature branch
- ⚪ Deferred — consciously postponed (reason recorded)

---

## Findings

### F1 — Selection-scope ambiguity (highest priority)
The screenshot simultaneously shows: grid content "12 per page", pager "Page 1 of 2 — 15
measurements", and the selection bar "15 measurements are selected". Three conflicting numbers
in one frame; a user cannot reliably predict what Export/Edit/Delete will act on.

**Fix:** Gmail-style scope disambiguation. When the selection equals the filtered total, the
selection bar must say "All N measurements matching the filter are selected". A page-level
selection additionally offers the "Select all N matching filter" refinement (toolbar button).

**Status:** 🟢 Done (selection bar now renders the "all matching" message when the selection
covers the filtered total).

---

### F2 — Selection bar / pager below the fold at large page sizes
Grids render full height (`setAllRowsVisible(true)`, native page scroll). At page sizes 50/100
both the selection bar and the pager are pushed out of the viewport — violating USER-R-02 in
practice ("number of selected items must always be visible").

**Fix:** Make the selection bar sticky to the viewport bottom so it remains visible while
scrolling the grid.

**Status:** 🟢 Done (CSS `position: sticky; bottom: 0` on `.measurement-selection-bar`).

---

### F3 — No scroll-to-top on page/sort/tab change
After paging via the bottom pager, the viewport stays at the bottom of the list.

**Fix:** After every grid re-render, scroll the grid's top into view.

**Status:** 🟢 Done (`scrollIntoView` executed on grid re-render in
`MeasurementDetailsComponent#renderPage`).

---

### F4 — Bulk actions enabled at zero selection (error-after-the-fact)
Export/Edit/Delete are always enabled; clicking with an empty selection shows a "missing
selection" note. Users must fail once to learn the rule.

**Fix:** Buttons are disabled until the selection is non-empty; enabled state follows
selection changes reactively.

**Status:** 🟢 Done (`MeasurementDetailsComponent#updateSelectionBar` toggles action buttons;
initial state disabled at construction).

---

### F5 — No button hierarchy; destructive action undifferentiated
Export/Edit/Delete/Show-Hide-Columns all render with identical styling and no icons. The
Delete risk is invisible.

**Fix:** Export = primary theme, Delete = error theme, icons added; Show/Hide Columns gets a
chevron affordance signalling "opens a menu".

**Status:** 🟢 Done.

---

### F6 — Search field accessibility / scope hint
Placeholder-only input, no label → a11y anti-pattern; search scope is unknowable to users.

**Fix:** `aria-label` added; placeholder kept generic (lookup search scope is not limited
to named fields, so no misleading hint text was added).

**Status:** 🟢 Done (aria-label). Scope-hint text intentionally omitted —
backend `withSearch` covers multiple columns; a partial hint would mislead.

---

### F7 — Pagination info label verbosity
"Page 1 of 2 — 15 measurements" is wordy; for small result sets the "of N" framing is noise.

**Fix:** Reordered to emphasise the count: "15 measurements · Page 1 of 2".

**Status:** 🟢 Done (`PaginationBar#render`).

---

### F8 — Empty states missing
Filter → 0 results: pager hides, grid renders a blank area; no "clear search" affordance.
Tab with no measurements: blank grid, no onboarding hint.

**Status:** 🟢 Done (per-tab empty-state Div in `MeasurementDetailsComponent`; distinguishes
"No measurements registered yet" from "No measurements match '<filter>'"; grid hidden while
empty).

---

### F9 — Tab count badges
Tab labels ("Genomics" etc.) carry no per-tab totals; cross-tab situational awareness only
via switching.

**Status:** 🟢 Done (`MeasurementTabPagination#setTabLabel`; counts loaded via the existing
unfiltered `count{...}Measurements` lookups on every refresh in
`MeasurementDetailsComponent#updateTabCounts`; labels render as "Genomics (15)").

---

### F10 — Page-size selector affordance
"12 per page" is a button + ContextMenu without a visible dropdown cue.

**Status:** 🟢 Done (`PaginationBar` now uses `Select<Integer>` with label "Items per page";
shared component — the project overview pager benefits as well).

---

### F11 — Title/nav mismatch
Workflow step says "View Measurements"; page heading says "Register Measurements".

**Status:** 🟢 Done (page title renamed to "View Measurements", aligning with the workflow
step label).

---

### F12 — Vertical budget
Test-instance banner + workflow step bar + page title + toolbar consume significant vertical
space before the grid starts.

**Status:** ⚪ Deferred (banner is environment-level chrome; out of scope for
FEAT-PAG-LIST-03).

### F13 — Mutation actions offered to read-only users
Users with read-project scope (ACL `READ` only) were offered Edit/Delete.

**Fix:** `MeasurementMain` evaluates `UserPermissions.editProject(projectId)` in
`showMeasurements()` and passes the result to the component; `MeasurementDetailsComponent`
hides (not just disables) Edit/Delete when the scope is missing. Defaults to fail-closed
(hidden) until write access is confirmed.

**Status:** 🟢 Done, extended: registration is gated too — `openRegistrationDialog()` checks
`editProject` (fail-closed notification) and the "Register Measurements" button is hidden for
read-only scope in `updateComponentVisibility()`.

---

## Parent references

- Story: `FEAT-PAG-LIST-03` — Paginated Measurement Lists with Cross-Page Selection
- Feature: `FEAT-PAGINATED-LISTS`
- Requirements refined: USER-R-01 (Paginated List Display), USER-R-02 (Cross-Page Selection),
  USER-R-03 (List View State in URL), USER-NFR-01 (Responsive List Rendering)
- ADRs: 0007 (paginated lists), 0009 (measurement pagination + selection)
