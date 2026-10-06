# UX/UI Improvement Plan — Project Access Management

> **Context:** Visual review of the Project Access Management page (`img_6.png`, test instance).
> This document tracks concrete UI/UX improvements to make access management more accessible,
> scannable, and well-structured for users.
>
> **Related strategy:** [`docs/project-sharing-ux-strategy.md`](../project-sharing-ux-strategy.md)
> covers the broader dialog-free sharing architecture (composer, drawer, inline confirm). This
> document focuses on **visual design, information architecture, and micro-interactions** that
> complement that strategy.
>
> **Date:** 2026-01-XX
> **Review method:** Screenshot analysis of the current access page layout.

---

## Status legend

- 🔴 Open — not yet addressed
- 🟡 In Progress — partially implemented
- 🟢 Done — implemented
- ⚪ Deferred — consciously postponed (reason recorded)

---

## Improvement areas

### I1 — Role visual hierarchy (🔴 High priority)

**Problem:** Role badges (`owner`, `editor`, `member`) all use the same pill style with similar
muted colors. The most privileged role (`owner`) doesn't stand out visually, making it hard to
quickly identify who has elevated access.

**Current state:** All roles render as small gray/blue pills with no iconography.

**Proposed fix:**
- **Semantic colors:** `owner` = amber/gold (`#f59e0b`), `editor` = blue (`#3b82f6`),
  `member` = neutral gray (`#6b7280`).
- **Role icons:** Add a small icon per role (crown for owner, pencil for editor, person for
  member) to aid quick scanning and support colorblind users.
- **Owner row highlight:** Subtle background tint (`bg-amber-50`) on the owner row to draw
  attention without being distracting.

**Acceptance criteria:**
- Given I view the access list, When I scan the Role column, Then I can distinguish owner from
  editor from member within 2 seconds without reading the text.
- Given I am a colorblind user, When I view the roles, Then I can still distinguish them via
  iconography.

**Status:** 🔴 Open

---

### I2 — Table information density & grouping (🔴 High priority)

**Problem:** The principal table is long and visually dense. Users and groups are mixed together
with no visual separation, making it hard to scan for specific entries or understand the
permission structure at a glance.

**Current state:** Flat list of all principals, sorted alphabetically, with no grouping.

**Proposed fix:**
- **Group by role:** Cluster entries under collapsible sections: "Owners (1)", "Editors (2)",
  "Members (9)". Each section header shows the count and can be collapsed.
- **Sticky section headers:** When scrolling, the current section header sticks to the top so
  users always know which group they're viewing.
- **Default expansion:** All sections expanded by default; users can collapse sections they
  don't need.

**Acceptance criteria:**
- Given I open the access page, When I view the list, Then I see three sections (Owners,
  Editors, Members) with counts.
- Given a section is expanded, When I click the section header, Then it collapses and the count
  remains visible.
- Given I scroll a long member list, When I reach the bottom, Then the "Members" header is
  still visible (sticky).

**Status:** 🔴 Open

---

### I3 — Bulk action UX (🟡 Medium priority)

**Problem:** The "Change role" and "Remove" buttons are placed above the table but are only
meaningful after selecting rows. Their enabled/disabled state isn't visually clear, and the
workflow (select → click button) is disconnected from the selection.

**Current state:** Two buttons above the table; "Change role" opens a dropdown, "Remove" deletes
selected rows.

**Proposed fix:**
- **Contextual toolbar:** Move bulk actions to a toolbar that appears **only when rows are
  selected** (like Gmail's selection bar). The toolbar shows: "[N] selected · [Change role ]
  [Remove] [Cancel selection]".
- **Select all checkbox:** Add a checkbox in the table header to select all visible rows.
- **Inline role dropdown (alternative):** For single-user changes, add a role dropdown directly
  on each row so users don't need to select + click a button.

**Acceptance criteria:**
- Given no rows are selected, When I view the table, Then no bulk action toolbar is visible.
- Given I select one or more rows, When I view the table, Then a toolbar appears showing the
  count and available actions.
- Given I click "Select all" in the header, When all visible rows are selected, Then the toolbar
  shows the total count.

**Status:**  Open

---

### I4 — Search & filter clarity (🟡 Medium priority)

**Problem:** The search bar says "Search people and groups" but the adjacent "All" dropdown is
vague. It's unclear what "All" filters — roles? types? The filter lacks explicit labeling.

**Current state:** Search input + "All" dropdown (unlabeled) + no result count.

**Proposed fix:**
- **Rename the filter:** Change "All" to "Filter by role" with options: All, Owners, Editors,
  Members.
- **Add type filter:** Second dropdown: "Type" with options: All, Users, Groups.
- **Show result count:** After searching/filtering, display "N results" below the search bar.
- **Live filtering:** Remove any "Apply" button; filter results as the user types/selects.

**Acceptance criteria:**
- Given I view the search area, When I look at the filter dropdown, Then it says "Filter by
  role" with clear options.
- Given I type in the search box, When I enter "test", Then results update immediately and show
  "3 results" (or similar).
- Given I select "Groups" in the type filter, When I view the list, Then only group entries are
  shown.

**Status:** 🔴 Open

---

### I5 — Share panel integration ( Medium priority)

**Problem:** The right-hand "Share this project" panel feels disconnected from the main table.
The flow (search → choose role → grant) isn't visually guided, and the "Grant access" button is
buried at the bottom.

**Current state:** Two-column layout: table on left, share composer on right. "Add a person" and
"Add a group" sections have inconsistent styling.

**Proposed fix:**
- **Visual flow indicators:** Add numbered steps or arrows showing the flow: ① Search → ②
  Choose role → ③ Grant access.
- **Consistent styling:** Make "Add a person" and "Add a group" use the same visual treatment
  (both as headings or both as links).
- **Prominent grant button:** Make "Grant access" more visually prominent (larger, primary
  color) and ensure it's always visible (sticky at bottom if the panel scrolls).
- **Alternative — modal/drawer:** Consider making the share panel a slide-over drawer triggered
  by a "Share project" button, keeping the main table as the single source of truth (see
  `project-sharing-ux-strategy.md` §7.1).

**Acceptance criteria:**
- Given I view the share panel, When I look at the layout, Then the flow from search to grant
  is visually clear.
- Given I add a person and a group, When I view the staged items, Then they use consistent
  styling.
- Given the panel has many staged items, When I scroll, Then the "Grant access" button remains
  visible.

**Status:** 🔴 Open

---

### I6 — Action feedback & confirmation (🟡 Medium priority)

**Problem:** There's no visible confirmation mechanism for actions. When a user clicks "Grant
access" or "Remove", there's no indication of success/failure.

**Current state:** Actions execute silently; no toast or inline feedback visible in the
screenshot.

**Proposed fix:**
- **Toast notifications:** Show a non-modal toast for actions: "Access granted to Test Group",
  "User removed", "Role changed to editor".
- **Confirmation dialog:** Show a confirmation dialog before removing users, especially for
  destructive actions (already covered by `project-sharing-ux-strategy.md` §8.2 inline confirm).
- **Undo support:** Consider an "Undo" option in the toast for recent changes (like Gmail's
  undo).

**Acceptance criteria:**
- Given I grant access to a group, When the action completes, Then a toast appears saying
  "Access granted to [group name]".
- Given I remove a user, When I confirm, Then a toast appears saying "[user] removed from
  project".
- Given I see a toast, When I click "Undo" (if implemented), Then the action is reversed.

**Status:** 🔴 Open (inline confirm already specified in strategy doc; toast feedback needs
implementation)

---

### I7 — Empty state & no-results handling (🟢 Low priority)

**Problem:** If the table were empty or a search returned no results, the current layout would
look broken with no guidance.

**Current state:** No empty state visible; likely renders a blank table.

**Proposed fix:**
- **Empty table state:** Show an illustration with a call-to-action: "No members yet. Share this
  project to get started."
- **No results state:** After a search/filter returns nothing, show: "No results for '[query]'.
  Try broadening your search." with a "Clear filters" button.

**Acceptance criteria:**
- Given a project has no shared users/groups, When I view the access page, Then I see an empty
  state with a "Share this project" button.
- Given I search for "nonexistent", When no results match, Then I see a "No results" message
  with a "Clear filters" button.

**Status:** 🔴 Open

---

### I8 — Accessibility improvements (🟢 Low priority)

**Problem:** Several accessibility concerns: role badges rely solely on color, checkbox hit
targets are small, "Change role" button doesn't indicate what role to change to.

**Current state:** Color-only role differentiation, small checkboxes, ambiguous button labels.

**Proposed fix:**
- **Role badges:** Add text labels or icons (see I1) so colorblind users can distinguish roles.
- **Checkbox size:** Increase checkbox hit targets to at least 44×44px per WCAG 2.1.
- **Button clarity:** Change "Change role" to "Change role for selected" or make it a dropdown
  that shows the target role.
- **Keyboard navigation:** Ensure all actions are reachable via keyboard (Tab, Enter, Space).
- **Screen reader labels:** Add `aria-label` attributes to icon-only buttons and role badges.

**Acceptance criteria:**
- Given I use a screen reader, When I navigate the access list, Then each role is announced
  (e.g., "owner role", "editor role").
- Given I use only a keyboard, When I Tab through the page, Then I can reach all actions and
  activate them with Enter/Space.
- Given I am a colorblind user, When I view the roles, Then I can distinguish them without
  relying on color alone.

**Status:** 🔴 Open

---

### I9 — Group details truncation ( Low priority)

**Problem:** Group descriptions like "All QBIC member labs that offer NGS services via QBIC." are
shown inline, making rows uneven and hard to scan.

**Current state:** Full group description visible in the table row, causing variable row heights.

**Proposed fix:**
- **Tooltip on hover:** Show the full description in a tooltip when hovering over the group
  name.
- **Truncate with ellipsis:** Keep the table row to: avatar + name + type badge + role. Show
  only the first 30-40 characters of the description with "..." if longer.
- **Expandable row detail:** Add a chevron to expand the row and show the full description.

**Acceptance criteria:**
- Given I view a group with a long description, When I look at the row, Then the description is
  truncated with "..." and the row height is consistent.
- Given I hover over the truncated description, When I wait 1 second, Then a tooltip shows the
  full description.

**Status:** 🔴 Open

---

### I11 — Grant confirmation quality (🟢 Done)

**Problem:** After a successful grant, the only feedback was "Access granted to 1 principal." — an
anonymous count that names neither the principal nor the role. Errors were *more* informative than
successes (they name the principal and the reason), feedback was silent to screen readers, the
"just granted" row highlight was indistinguishable from the row-selection tint, and nothing pointed
the user at the effect in the roster.

**Current state (before):** Persistent inline banner with a bare count; `::part(recently-granted)`
row background equal to `--lumo-primary-color-10pct` (visually identical to Lumo's selection tint on
an already-tinted row); no `aria-live` anywhere in the access view; no dismiss control.

**Implemented fix:**
- **Named confirmation.** The success banner lists one line per granted principal with its role
  (`jdoe · editor`, `NGS Lab (group) · member`), mirroring the error-path formatting so success and
  failure carry the same evidence weight.
- **Accessibility.** The grant result is delivered through a permanently attached, visually hidden
  ARIA live region (`role="status"` / `aria-live="polite"`, `aria-atomic="true"`, switched to
  `role="alert"` / `assertive` for errors), so the asynchronously delivered grant result is
  announced. The live region is a separate, always-rendered element rather than the visible banner:
  a region that becomes visible together with its content in one update is frequently not announced
  at all, and the visible banner is hidden while empty.
- **Unambiguous "just granted" marker.** The highlight moved out of the grid's shadow boundary into
  the Principal cell: a success-coloured leading edge plus a `New` tag. It no longer competes with
  the (blue) row-selection tint and it does not rely on colour alone.
- **Actionable confirmation.** A "Show in the roster" follow-up selects and scrolls to the newly
  granted rows; a dismiss control clears the banner instead of it lingering indefinitely.
- **Already-granted model.** Principals that already have access stay searchable in the pickers and
  are marked `already has access` instead of being silently filtered out. Selecting one explains
  that the role must be changed instead. This removes the three-line disclaimer about the hidden
  filter that the rail previously had to carry.
- **Disabled-button affordance.** The disabled primary `Grant access` button is now rendered with
  neutral colours instead of a washed-out primary fill, so it reads as "nothing staged yet" rather
  than "broken / loading".

**Acceptance criteria:**
- Given I grant a principal, When the grant succeeds, Then the confirmation names the principal(s)
  and the role each received.
- Given I use a screen reader, When the grant result arrives asynchronously, Then the confirmation
  is announced without me moving focus.
- Given a principal was just granted, When I look at the roster, Then I can distinguish its marker
  from a row I have merely selected, without relying on colour.
- Given a confirmation is shown, When I click "Show in the roster", Then the newly granted rows are
  selected and scrolled into view.
- Given a person already has access, When I search for them in the picker, Then they appear marked as
  already having access and I am told to change the role instead.

**Status:** 🟢 Done (inline confirmation surface; a toast-based variant remains an option, see I6)

**Traceability:** parent `FEAT-USER-GROUPS` (#1558), story `FEAT-USER-GROUPS-08` (#1566). *Governance
check pending human review:* the already-granted picker behaviour changes externally observable
behaviour of an existing capability. Confirm whether an existing `USER-R-*` / `GROUP-R-*`
requirement already covers it or whether `docs/requirements.md` needs a dedicated, human-approved
requirements PR before this part is merged.

---

### I12 — Picker marker execution (🟢 Done)

**Problem:** the "already has access" marker (I11) was correct in intent but its execution introduced a
contrast regression, placed the same status in two different positions, and collided with the
dropdown's own scrollbar.

**Measured defects (from `img_5.png` / `img_6.png`):**
- The granted option's *name* was washed with `--lumo-secondary-text-color`, not just the marker:
  `rgb(108,117,130)` at **4.66:1** on white, dropping to **4.48:1** on the hovered row background
  (`--lumo-primary-color-10pct`) — below WCAG AA. The full name measured **3.38:1**. The row the user
  is actively pointing at was the least legible.
- Marker position differed between pickers: trailing on the person option, but appended as a fourth
  *stacked line* on the group option, where it read as a body line of the option.
- The marker was a neutral pill, visually identical to the group **type** badge
  (`organisational` / `User Group`); two different semantics shared one visual language, and the
  group list showed three stacked pills of the same shape.
- The overlay scroller reserves no space for its own scrollbar, so a trailing marker ran under it
  (`img_5.png`: pill right edge at x=1259 vs scrollbar at x=1252–1266), and the group description's
  **rendered ellipsis was hidden by the scrollbar** (text ended in `sed diam non`, no `…` visible).
- The option never said *which* role the principal holds, so the reason the row was inert was only
  revealed after selecting it.

**Implemented fix:**
- **Contrast restored.** The option is no longer de-emphasised as a whole; the name keeps normal
  contrast and only the marker carries the state. Removed the wrapper-level muted colour.
- **One position, both pickers.** The marker trails the identity in the person picker and is appended
  to the group option's *name row* — never as an extra stacked line.
- **Distinct from the type badge.** The marker now uses the success tint plus a check icon and reads
  `Has access · <role>`, with a tooltip naming the next step. State and group type no longer compete,
  and it survives colour-blindness via the icon and the text.
- **Scrollbar reserve.** Padding is added to the option content (the scroller's `overflow` lives in a
  shadow root and is not reachable from the theme), so the marker and the description ellipsis are no
  longer clipped.
- **Role surfaced in the option**, so the user does not have to click an inert row to learn why.
- **Ordering:** selectable principals are listed before already-granted ones, clustered at the top.

**Acceptance criteria:**
- Given a principal already has access, When I open the picker, Then its name is as legible as any
  other option and the marker names the role it holds.
- Given I look at a person and a group option, When both already have access, Then the marker appears
  in the same relative position in both.
- Given the dropdown has a scrollbar, When a marker or a long description reaches the trailing edge,
  Then neither is hidden beneath the scrollbar.

**Status:** 🟢 Done

**Traceability:** parent `FEAT-USER-GROUPS` (#1558), story `FEAT-USER-GROUPS-08` (#1566). The contrast
fix is an accessibility defect repair; the marker copy/position change is a change to externally
observable behaviour and carries the same pending governance check as I11.

---

### I10 — Two-tab layout consideration (⚪ Deferred)

**Problem:** Showing the full table + share panel simultaneously creates cognitive load. Users
may not know whether to focus on managing existing access or adding new access.

**Proposed fix:**
- **Tab-based layout:** Two tabs — "Members" (current table with search/filter/bulk actions) and
  "Share" (focused composer workflow). This reduces cognitive load and makes each task
  self-contained.
- **Rationale for deferral:** The existing strategy document (`project-sharing-ux-strategy.md`)
  proposes a drawer-based approach for the landing page and an inline composer for the access
  page. A tab-based layout would be a larger restructuring and should be evaluated after the
  current strategy is implemented.

**Status:**  Deferred (pending evaluation after P1/P2 of the sharing strategy land)

---

## Priority matrix

| Priority | Improvement | Impact | Effort | Dependencies |
|----------|-------------|--------|--------|--------------|
| 🔴 High | I1 — Role visual hierarchy | High (scanability) | Low (CSS + icons) | None |
| 🔴 High | I2 — Table grouping | High (information architecture) | Medium (component refactor) | None |
| 🔴 High | I3 — Bulk action UX | High (workflow clarity) | Medium (toolbar component) | None |
| 🟡 Medium | I4 — Search & filter clarity | Medium (findability) | Low (label + dropdown) | None |
| 🟡 Medium | I5 — Share panel integration | Medium (workflow guidance) | Medium (layout refactor) | Strategy doc P1 |
| 🟡 Medium | I6 — Action feedback | Medium (user confidence) | Low (toast component) | Strategy doc inline confirm |
| 🟢 Done | I11 — Grant confirmation quality | High (confidence + a11y) | Low (component + CSS) | I6 (partially supersedes) |
| 🟢 Done | I12 — Picker marker execution | High (a11y + clarity) | Low (component + CSS) | I11 |
| 🟢 Low | I7 — Empty state | Low (edge case) | Low (empty state component) | None |
| 🟢 Low | I8 — Accessibility | Low (compliance) | Medium (audit + fixes) | I1 (role icons) |
| 🟢 Low | I9 — Group details truncation | Low (visual consistency) | Low (CSS + tooltip) | None |
| ⚪ Deferred | I10 — Two-tab layout | Medium (cognitive load) | High (layout restructure) | Strategy doc P1/P2 |

---

## Implementation sequence

### Phase 1 — Quick wins (low effort, high impact)
1. **I1** — Role visual hierarchy (colors + icons)
2. **I4** — Search & filter clarity (rename dropdown, add result count)
3. **I9** — Group details truncation (tooltip + ellipsis)

### Phase 2 — Structural improvements (medium effort)
4. **I2** — Table grouping by role (collapsible sections)
5. **I3** — Bulk action UX (contextual toolbar)
6. **I6** — Action feedback (toast notifications)

### Phase 3 — Polish & accessibility (lower priority)
7. **I7** — Empty state & no-results handling
8. **I8** — Accessibility improvements (keyboard nav, screen reader labels)
9. **I5** — Share panel integration (visual flow indicators)

### Phase 4 — Evaluation (deferred)
10. **I10** — Two-tab layout (evaluate after strategy P1/P2)

---

## Parent references

- **Related strategy:** [`docs/project-sharing-ux-strategy.md`](../project-sharing-ux-strategy.md)
- **Feature:** `FEAT-USER-GROUPS` (parent feature `#1558`)
- **Story:** `FEAT-USER-GROUPS-08` — View and Manage Groups on the Project Access Page (`#1566`)
- **Requirements:** USER-R-01, USER-R-02, USER-R-03 (access management)
- **Screenshot:** `img_6.png` (test instance, QVAMP project)

---

## Notes

- This document focuses on **visual design and micro-interactions**. The broader architectural
  changes (dialog-free composer, drawer, inline confirm) are tracked in
  `project-sharing-ux-strategy.md`.
- Improvements I1-I9 can be implemented incrementally without blocking the strategy document's
  P1/P2 phases.
- Accessibility improvements (I8) should be integrated throughout implementation, not deferred
  to the end.