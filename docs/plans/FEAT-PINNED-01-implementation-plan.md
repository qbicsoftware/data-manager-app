# Implementation Plan — FEAT-PINNED-01: Pin Projects for Quick Access on the Project Overview

> **Story:** [FEAT-PINNED-01](../features.md#feat-pinned-01--pin-projects-for-quick-access-on-the-project-overview) — tracked in `docs/features.md`, no GitHub issue (see §6)
> **Parent Feature:** `FEAT-PINNED-PROJECTS`
> **Requirement:** [`USER-R-04`](../requirements.md) — added in PR #1544
> **ADR:** [ADR-0008 — Pinned projects are user preferences, not project state](../adr/0008-pinned-projects-as-user-preferences.md)
> **Schema:** [`sql/migrations/create-pinned-projects.sql`](../../sql/migrations/create-pinned-projects.sql) · operator guide [`docs/migrations/NEXT.md`](../migrations/NEXT.md)
> **Date:** 2026-09-14

This file is the task breakdown for the story. It exists because the story is tracked in
`docs/features.md` only, without a GitHub issue to hold tasks.

---

## 1. Scope

A user can pin up to five of the projects they have access to. The pins render as a personal
quick-access row at the top of the project overview, above the title and the search controls, and
survive reloads and sessions. Pinning never touches project data; unpinning works even when the
pinned project became unreadable.

Out of scope for this story: pinning from the project detail view, a global pinned-project switcher
in the navigation, an automatically derived "recently visited" list, bulk pinning through cross-page
selection, and making the cap of five configurable.

---

## 2. Design decisions already taken (do not re-litigate in review)

| Decision | Outcome | Recorded in |
|---|---|---|
| Curated vs derived shortlist | user-curated; recency and membership cannot express personal relevance | `USER-R-04` rationale |
| Where the row renders | above the overview title and list controls, i.e. a toolbar, not part of the result set | story AC |
| Row size and order | at most 5 pins, newest pin first, no manual reordering | story AC, `PinnedProjectService.MAX_PINNED_PROJECTS` |
| Storage | `pinned_projects` table, application-layer preference; **not** `Project` aggregate state | ADR-0008 |
| Unreadable pins | rendered as a placeholder using a write-once label snapshot; still consume one of the five places; still removable without project access | ADR-0008 |
| Interaction with search/sort/paging | row is independent of the list state and never enters the URL | story AC; keeps `ADR-0007` semantics intact |
| Duplicates | a pinned project may appear both in the row and in the list below | story AC |
| Overflow | reject with a message; never evict a pin the user created | story AC |

---

## 3. Tasks

### T1 — Preference storage (`project-management`, infrastructure)

- [x] `PinnedProject` + `PinnedProjectId` (composite key `userId` + `projectId`), with `pinnedAt` and
  the write-once `projectCodeSnapshot` / `projectTitleSnapshot`.
- [x] `PinnedProjectStore` port in `application/api` and `PinnedProjectStoreImplementation` +
  `PinnedProjectRepository` in `project-management-infrastructure`.
- [x] Writes require an enclosing transaction (`Propagation.MANDATORY`); the service owns it.
- DoD: unit tests for the duplicate guard and the blank-user-id guards.

### T2 — Application service (`project-management`)

- [x] `PinnedProjectService` with `findPinnedProjects()`, `pin(ProjectId)`, `unpin(ProjectId)`.
- [x] Cap of `MAX_PINNED_PROJECTS = 5` enforced on **stored rows**, so an unreadable pin cannot strand
  a place.
- [x] Pinning resolves the label through the access-restricted overview lookup and rejects projects the
  user cannot read; unpinning performs no project access check (ADR-0008).
- [x] Expected failures returned as `PinOutcome`, never thrown.
- [x] `ProjectInformationService.findAccessibleProjectIds()` exposed so pin visibility and list
  visibility resolve through one rule.
- DoD: `PinnedProjectServiceSpec` covers live vs placeholder rendering, cap rejection, pinning without
  access, idempotent pin, unpin without access, and the no-authentication case.

### T3 — Schema

- [x] `pinned_projects` in `sql/complete-schema.sql`.
- [x] `sql/migrations/create-pinned-projects.sql` (idempotent, with rollback notes).
- [x] Index rows in `sql/migrations/README.md` and a Tier-2 entry in `docs/migrations/NEXT.md`.
- DoD: FK to `projects_datamanager` with `ON DELETE CASCADE`; secondary index
  (`userId`, `pinnedAt`) for the read path. **Schema changes need explicit human approval in review.**

### T4 — UI

- [x] `PinnedProjectsComponent` — compact pin cards in a responsive `auto-fit` grid; hidden when the
  user has no pins; fed by a `Supplier<List<PinnedProjectView>>` so the row holds no service logic.
- [x] Placeholder card for unreadable pins: no `RouterLink`, snapshot label only, "No access" tag, still
  removable.
- [x] Star toggle on every overview card, as a **sibling** of the card-body `RouterLink` inside
  `.project-card-wrapper`, so clicking the star cannot also navigate.
- [x] Both toggles share `ProjectCollectionComponent.handlePinToggle`, which refreshes the row and
  re-renders the current page's cards from the cached overviews (no extra list query).
- [x] Limit message via `MessageSourceNotificationFactory`, key `project.pinned.limit`.
- [x] CSS in `page-area.css`: reserved right gutter on cards for the toggle, `minmax(11rem, 1fr)` grid,
  truncated titles with a full-label tooltip.
- DoD: `PinnedProjectsComponentSpec` covers the empty row, the placeholder card, and unpin delegation.

### T5 — Verification

- [x] `./run-tests.sh` env, then
  `./mvnw -Pdevelopment -pl project-management,project-management-infrastructure,datamanager-app -am test`
  → BUILD SUCCESS (project-management, infrastructure and app modules green).
- [ ] Manual pass on a running instance (`./mvnw spring-boot:run -pl datamanager-app -Pdevelopment`):
  pin 5, try a 6th, unpin one, reload, resize to a phone viewport, and verify a pin whose access was
  revoked renders as a placeholder that can still be removed. Requires a database with
  `pinned_projects` applied.

---

## 4. Known limitations

- The cap is enforced read-then-write; two simultaneous pin requests from the same user could exceed it
  by one. The composite primary key prevents duplicates, which is the invariant that actually matters.
- Pin state is cached for the lifetime of the rendered overview page. A pin created in a second browser
  tab appears after the next refresh of the list, not instantly.
- The label snapshot is never refreshed. For an accessible pin it is not displayed at all, so staleness
  is only visible on placeholders — which is the intended, self-describing case.

---

## 5. Follow-up candidates

1. Global pinned-project switcher in the app navigation; reuses `PinnedProjectService` unchanged.
2. Pinning from the project detail header.
3. Making the cap a per-user list preference; `ADR-0007` already anticipates persisted per-user list
   settings for the overview default sort, so both could share one preference store.
4. A "recently visited" derivation, if usage shows curation is too much effort for users.

---

## 6. Governance note

`AGENTS.md` §0 expects Features, Stories and Tasks to live in GitHub issues. This story is tracked in
`docs/features.md` and in this file instead, so the `**GitHub Feature**` / `**GitHub**` table fields
carry `—` and the implementation PR references `FEAT-PINNED-01` and `USER-R-04` rather than an issue
number. `AGENTS.md` was deliberately not amended; the deviation is stated in the PR description.
