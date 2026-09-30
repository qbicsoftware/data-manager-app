# Strategy — Dialog-free Project Sharing UX

> **Status:** Draft — decisions locked, open for review (docs-only PR, no code changes, no requirement changes)
> **Date:** 2026-09-30
> **Scope:** New look and UX for sharing projects with users and user groups, on **both** surfaces:
> the project list (the landing page after login) and within a project. The guiding constraint is to
> **remove modal dialogs**, which impose high cognitive load and break the user's context.
>
> **Driver:** [#1566](https://github.com/qbicsoftware/data-manager-app/issues/1566) —
> `FEAT-USER-GROUPS-08: View and Manage Groups on the Project Access Page` (parent feature
> [#1558](https://github.com/qbicsoftware/data-manager-app/issues/1558) `FEAT-USER-GROUPS`).
>
> **Companions:**
> [`docs/user-groups-strategy.md`](user-groups-strategy.md) — group model, ACL plumbing, visibility
> policy (§4.3, §4.5, §4.6, §5.1); [`docs/auth-glossary.md`](auth-glossary.md) — authorization
> terminology.

---

## 0. Implementation status

P1 and the landing-page quick-share part of P2 have landed:

| Item | Status | Notes |
|---|---|---|
| Inline, dialog-free composer (`ProjectSharingComposer`) | ✅ Done | Batch + per-item staging; per-chip role; people and groups |
| Always-visible inline role control (no "Edit" step) | ✅ Done | Direct `Select<ProjectRole>` on the row |
| Inline removal confirmation (no `AlertDialog.danger`) | ✅ Done | One confirm at a time; keyboard-operable |
| Read-only mode for READ collaborators + route gate relaxed to READ | ✅ Done | `ProjectAccessMain` now gates on `readProject` |
| `AddCollaboratorToProjectDialog` removed | ✅ Done | Plus its spec |
| Landing-page `ProjectSharingDrawer` (non-modal) | ✅ Done | Composer + read-only access summary; opened from the card kebab, gated to access-administrators |
| Compact card access summary (group name chips) | ⏸ Deferred | Needs the `project_userinfo` / `project_overview` SQL-view rework (story 12); the card still shows principal avatars |
| Full edit/revoke parity inside the drawer | ⏸ Deferred | Drawer is currently add + summary; full parity reuses the access page |
| Drag-and-drop accelerant (P3) | ⏸ Deferred | Optional, not started |

> The route-gate relaxation (ADMINISTRATION → READ) and the added visibility surface are
> security-relevant and remain subject to the human sign-off and requirements PR described in
> §12 before release.

---

## 1. Motivation

Sharing a project today means opening a modal dialog, choosing exactly one principal (a person **or**
a group), assigning a role, and closing it — then repeating. Removing access adds a *second* modal
confirmation for every row. As the group feature lands, the principal space doubles and the "blast
radius" of a single grant grows, so the friction and the risk both increase.

The dialog is the primary cognitive-load offender:

- It dims the page and traps focus, hiding who else already has access while you compose.
- It discards selections on cancel and grants only one principal per open.
- It forces a second interruption (`AlertDialog.danger`) for each removal.
- It is the wrong surface for a **batch** operation, which is exactly what sharing with groups enables.

This document proposes a **dialog-free** sharing experience, reusing the non-modal patterns already
established in the codebase (right-hand sidebars, inline editing, `PageArea`, card/avatar/tag
components).

---

## 2. Current UX inventory (evidence)

| Surface | Control | File | Friction |
|---|---|---|---|
| Access page | `Add people or groups` → modal | `views/projects/project/access/ProjectAccessComponent.java` + `AddCollaboratorToProjectDialog.java` | One principal per grant; XOR person/group; no batch; selection lost on cancel |
| Access page | People/Groups grid `Edit` → inline `Grid.Editor` | `ProjectAccessComponent.java` | Requires an "Edit" step before the role control appears; two grids duplicate the same control |
| Access page | `Remove` → `AlertDialog.danger(...)` | `ProjectAccessComponent.java` | Second modal; confirm-then-act; no context retained |
| Landing page | Card collaborator avatars (principals only) + kebab menu with "Pin project" | `views/projects/overview/components/ProjectCollectionComponent.java` | Groups invisible; **no sharing entry point**; kebab only pins |
| Both | `/projects/:projectId/access` route | `views/projects/project/access/ProjectAccessMain.java` | Route gated to `changeProjectAccess` (READ **and** ADMINISTRATION) → sharing is separated from the place users spend most time |

The service layer is already group-ready: `ProjectAccessService.changeAuthorityAccess` /
`removeAuthorityAccess` / `listSharedGroups` exist and are `ADMINISTRATION`/`READ`-gated; the
`AddCollaboratorToProjectDialog` already supports groups. The problem is the **interaction model**,
not the backend.

---

## 3. Design principles

1. **In-place over overlay** — the share control appears where the user already is; nothing dims or
   traps focus.
2. **Direct manipulation over steps** — role is a property visible and editable on the row, not hidden
   behind an "Edit" action.
3. **Explicit confirmation over undo** — removal is confirmed *inline in the row* (decision D1); no
   modal, no optimistic-removal race with notifications.
4. **Progressive disclosure** — group names/descriptions are public (discoverability); member rosters
   and counts stay hidden from non-members (`user-groups-strategy.md` §5.1).
5. **One sharing surface, reused** — the same panel powers the landing page and the in-project page,
   so behaviour cannot drift.
6. **State is always visible** — the access list is the source of truth, not a transient modal.
7. **Accessible & keyboard-first** — no focus trap, `Esc`/`Enter` semantics, labelled controls.

> **Terminology:** a **non-modal drawer** (already used by `ConnectDatasetSidebar` /
> `SyncResultsSidebar`) is *not* a dialog — the page stays visible and interactive and there is no
> focus trap. Eliminating **modal dialogs** is the goal.

---

## 4. Target mental model

```
Principal (User | Group)  ──grant──►  ProjectRole (READ | WRITE | ADMIN)
```

A project has an **access list**. Granting = adding an entry; revoking = removing it; changing the
role = editing a property. The composer, the drawer, and the card summary are all views over that one
list.

---

## 5. Options considered

### A — Popover replacing the modal
Swap the dialog for a Vaadin `Popover` anchored to the button.
- **Pros:** smallest delta; reuses content; non-blocking; page context stays.
- **Cons:** still an overlay; cramped for multi-select + search + role; positioning breaks near
  viewport edges; selections lost on dismiss. **Weak fit for the stated goal.**

### B — Right-hand "Sharing" drawer
A sliding panel (`ConnectDatasetSidebar` pattern) hosting the composer + access list.
- **Pros:** large canvas; page stays visible; no focus trap; proven in-repo; reusable on both
  surfaces; URL-addressable.
- **Cons:** still covers part of the page; drawer fatigue; two entry points must stay in sync.

### C — Inline, page-native composer + inline row editing
On the access page: a persistent composer row plus always-visible role controls per row.
- **Pros:** zero overlay, zero context switch; best direct manipulation and accessibility; removes the
  "Edit" step; supports batch naturally.
- **Cons:** not usable on a cramped card; page grows; editable controls on every row (accidental-edit
  risk, mitigated by reversibility and gating).

### D — Direct manipulation (drag chips onto cards)
- **Pros:** delightful for repeat actions; low friction.
- **Cons:** poor accessibility/discoverability; conflicts with card navigation; ambiguous default role;
  poor on touch. **Accelerant only, never the sole path.**

### E — Card affordance + deep link only
- **Pros:** smallest surface; single source of truth; no overlay.
- **Cons:** adds navigation for a frequent task; no quick-share from the list.

### F — Command palette / search-first
- **Pros:** fast for power users.
- **Cons:** heavy investment; discoverability/novice-hostile. **Out of scope.**

### Comparison

| Option | vs. "no dialogs" | Landing fit | In-project fit | Effort | Accessibility |
|---|---|---|---|---|---|
| A Popover | weak | medium | medium | low | medium |
| B Drawer | good | **high** | high | medium | high |
| C Inline page | **best** | none | **best** | medium | **best** |
| D Drag | good | high | low | medium | poor |
| E Deep link | **best** | low | high | low | high |
| F Palette | good | medium | medium | high | medium |

---

## 6. Locked decisions

| # | Decision | Rationale / implication |
|---|---|---|
| **D1** | **Inline confirm** for revoke (no Undo) | No optimistic removal, no notification race. Removal fires only after explicit in-row confirmation. |
| **D2** | **Batch composer that also serves per-item** | One staging area; stage one chip → per-item; stage several → batch. No separate modes. |
| **D3** | **Drawer** on the landing page | Non-modal right panel hosting the full access panel; URL-addressable. |
| **D4** | **Sections + shared rows + filter** | People/Groups stay distinct (privacy, scanability); one `AccessRow`; one filter control. |
| **D5** | **Compact card summary** | Avatars + group name chips + count; no rosters on cards. |
| **D6** | **Mobile** | Drawer → full-height sheet; composer stacks; drag never the sole path. |
| **D7** | **READ collaborators get read-only access to the sharing panel** | Group names/descriptions are public by design; "who a project is shared with" is benign. Member rosters/counts remain hidden from non-members. |

---

## 7. Recommended architecture

### 7.1 Layered hybrid

- **Primary (in-project) — Option C.** Rebuild the access page as a dialog-free access workspace:
  one list, a persistent inline composer, per-row role controls, inline-confirm removal.
- **Secondary (landing) — Option E + B.** A compact card summary plus a `Share project…` action in
  the existing kebab menu that opens the **same composer inside a right drawer** (not a modal).
- **Accelerant (later, optional) — Option D.** Drag-and-drop only as progressive enhancement.
- **Never:** a modal dialog for sharing or for revoking.

### 7.2 Component set (the DRY move)

```text
ProjectAccessPanel              ← the one source of truth UI
├── ProjectSharingComposer      ← batch/per-item staging + "Grant access"
├── AccessFilter                ← search + type filter (All | People | Groups)
├── PeopleSection  → [AccessRow]
└── GroupsSection  → [AccessRow]

Containers:
├── ProjectAccessMain (route)   → embeds ProjectAccessPanel inline
└── ProjectSharingDrawer        → embeds the same ProjectAccessPanel (landing page)

ProjectCardAccessSummary        ← compact avatars + group chips + count
```

`ProjectAccessPanel` is embedded **both inline** on `/access` and **inside the drawer** on the landing
page. The drawer is a container, not a second implementation. `AddCollaboratorToProjectDialog` is
deleted.

### 7.3 Relationship to existing code

- Reuses `PageArea`, `UserAvatar` / `UserAvatarGroupItem`, `Tag`, `Card`, `AlertDialog` (only where a
  genuinely destructive action outside this flow still needs it — e.g. group dissolution).
- Reuses `ProjectRoleRecommendationRenderer` for role descriptions.
- Replaces the `Grid.Editor` "Edit" step and the `AlertDialog.danger` removal in
  `ProjectAccessComponent`.
- Extends `ProjectCollectionComponent.buildTopRightControl` (the existing kebab menu) with a
  `Share project…` item.

---

## 8. Detailed flows

### 8.1 Composer — batch that includes per-item (D2)

A single staging area, no mode switch:

1. **Search** across People (username / full name / ORCID) and Groups (name / description); results are
   **grouped under "People" / "Groups"**.
2. Already-granted principals are filtered out, or shown disabled with *"already has access"*.
3. Selecting a result adds a **chip**: `[avatar/group] Name   [role ▾]   [×]`. Role defaults to
   **READ**; options READ/WRITE/ADMIN (never OWNER). Remove the chip before applying.
4. **Grant access** applies **all staged chips in one call**.
   - Stage one chip → per-item workflow.
   - Stage several → batch.
5. After apply: composer clears, list refreshes, non-modal toast *"Access granted to N principals."*

**Group blast-radius preview.** The composer is only reachable by access-administrators, so a staged
group chip may show its member count (*"N members"*) — the privacy-allowed case from
`user-groups-strategy.md` §5.1. This is the main reason a manager batch-grants.

### 8.2 Inline confirm for revoke (D1)

- The row's **action area** swaps to a compact confirm; the rest of the row (name, type, role) stays
  visible:
  `Remove <name> from this project?  [ Remove ]  [ Cancel ]`
- **One confirm open at a time** — opening another cancels the previous.
- `Esc` cancels; focus moves to the confirm control; `Enter` on the focused `Remove` confirms.
- On confirm: write → row disappears → toast *"<name> removed from project."*
- **Group rows** show a consequence hint: *"N members will lose access at the next authorization
  check (≤60s)."* Member counts are allowed here (panel is `ADMINISTRATION`-gated; project
  ADMIN/OWNER may see counts for groups shared on their project).

**What still gets a confirmation**

| Action | Confirmation |
|---|---|
| Ordinary revoke | Inline confirm (above) |
| Role change | None — audit-log-only, reversible |
| Grant | None — adding access is low-risk and removable |
| Group dissolution / ownership transfer (outside this flow) | Stronger, still dialog-free treatment (inline danger zone in the group view) |

Because removal is explicit, the EPIC-4 notification pipeline stays simple: the revocation directive
fires on confirm, with no Undo window to coordinate.

### 8.3 Sections, shared rows, filter (D4)

- **Sections:** *People* and *Groups* remain separate — the privacy boundary and scanability depend on
  it.
- **Shared `AccessRow`** renders both:
  `avatar/name + username` (or group name + description) · `type indicator` · `role control` ·
  `remove (inline confirm)`.
- **Filter:** one search field (name/description/ORCID) plus a type filter `All | People | Groups`;
  a section hides when the filter selects the other type.
- **Owner / self rules preserved:** the OWNER row shows a static role label and no controls; the
  current user's row has no remove; every edit is gated by
  `userPermissions.changeProjectAccess(...)`.

### 8.4 Landing page — drawer + card summary (D3, D5)

**Card summary (`ProjectCardAccessSummary`)**
- Existing avatar group (principals, max 3) **plus** group name chips (e.g. `NGS Lab`), capped
  (≈2 chips + `+N`), plus an access count affordance.
- No member rosters or counts on the card (the card is not admin-gated).

**Kebab menu** (existing `buildTopRightControl`)
- `Share project…` → opens `ProjectSharingDrawer` with the project as title.
- `Pin / Unpin project` (existing).
- `Open access page` (deep link for users who prefer the full page).

**Drawer**
- Non-modal right panel hosting `ProjectAccessPanel` (full parity: compose, view, edit roles, revoke)
  so a manager never has to enter the project to share.
- The card body remains a `RouterLink`; the kebab stays a sibling so opening the drawer never
  navigates (existing pattern).
- **URL-addressable:** `?share=<projectId>` on the projects route; browser Back closes it, consistent
  with the existing `ListState` URL contract (USER-R-03).

### 8.5 Mobile (D6)

- Drawer becomes a **full-height sheet** on narrow viewports.
- Composer stacks: search full-width, chips wrap, role control full-width; staging row scrolls.
- Inline confirm uses large tap targets; `Esc` is supplemented by an explicit `Cancel`.
- Drag-and-drop (future) is strictly an accelerant — every action remains reachable via search and
  composer.

---

## 9. Visibility and privacy (D7)

`ProjectAccessPanel` renders a first-class **view-only** mode when the viewer lacks
`changeProjectAccess`:

- No composer, no grant affordance.
- `AccessRow` shows the role as a **static label**; no dropdown, no remove, no inline confirm.
- Filter + sections + search remain available (browsing who has access).
- Groups still show **name + description only** — no roster, no member count. `SharedProjectGroup`
  carries no membership data, so this is structural, not CSS-hidden.

| Viewer | People (direct grants) | Group name / description | Group members / count |
|---|---|---|---|
| READ collaborator | ✅ visible | ✅ visible | ❌ hidden |
| Project ADMIN / OWNER | ✅ visible | ✅ visible | ✅ visible (for groups on their project) |
| QBiC admin (system role) | ✅ visible | ✅ visible | ✅ visible (oversight) |

**Route gate change (D7).** `ProjectAccessMain.beforeEnter` currently reroutes to NotFound unless
`changeProjectAccess(projectId)` (READ **and** ADMINISTRATION). It relaxes to a plain READ check:

```text
changeProjectAccess = READ && ADMINISTRATION   →   READ   (read-only panel mode)
```

The service layer is already ready: `listCollaborators` and `listSharedGroups` are `@PreAuthorize(READ)`.

> **Security note:** relaxing an authorization gate is a security-relevant behavior change and
> requires explicit human sign-off in the PR (see §12), even though it is not a
> `SecurityConfiguration.java` change.

---

## 10. Acceptance criteria (draft)

- Given I am a project ADMIN on the landing page, When I open a card's *Share* action, Then a
  non-modal drawer opens without navigating away, and the URL reflects `?share=<projectId>`; Back
  closes it.
- Given the composer, When I select one or more people/groups and assign roles, Then *Grant access*
  applies all staged grants at once and the list refreshes; a single staged chip behaves identically
  (per-item).
- Given a shared group in the list and I am an access-administrator, When I choose *Remove*, Then an
  inline confirm appears in that row (no dialog); on confirm the grant is revoked and a toast is
  shown; no second confirm is open at the same time.
- Given an ordinary role change, When I select a new role, Then it applies immediately without
  confirmation.
- Given I have READ access to a project, When I open its sharing view, Then I see the People list and
  the Groups as name + description only, with no ability to grant, change roles, or revoke.
- Given I am a READ collaborator and I am not a member of a shared group, Then I never see that
  group's member list or member count.
- Given the project OWNER or my own row, Then the role is static and no remove control is rendered.
- Given a narrow viewport, Then the drawer is a full-height sheet and the composer stacks; no action
  requires drag-and-drop.
- Given I am an access-administrator viewing a shared group, Then the composer/row may show the group
  member count before a grant.

---

## 11. Guardrails and edge cases

- **Authorization invariants unchanged:** cannot remove self, cannot remove/edit the project OWNER,
  groups never become OWNER, `ADMINISTRATION` gating for grant/role/revoke
  (`userPermissions.changeProjectAccess`). Reversibility must not weaken these.
- **View-only leakage guard:** `changeProjectAccess=false` → zero add controls, zero role dropdowns,
  zero remove controls; rendering the panel never calls a membership API
  (`0 * groupInformationService.listMyGroups(*)`).
- **Revocation timing:** the ≤60s NFR is asserted after the inline confirm; the list refresh reflects
  the write.
- **Stale races:** concurrent change → refresh, no unhandled exception (non-shared revoke is already a
  no-op at the service).
- **Privacy:** card and read-only surfaces never fetch group membership; only group names/descriptions.
- **Accessibility:** drawer is non-modal (no focus trap); inline confirm is keyboard-operable; role
  controls are labelled.
- **URL/state:** drawer open-state lives in the URL; back closes it.

---

## 12. Governance and phasing

Per `AGENTS.md`:

1. **Feature / Stories with stable IDs** before any Task (extend `FEAT-USER-GROUPS` or a sibling
   `FEAT-PROJECT-SHARING-UX`).
2. **ADR** for the dialog-free panel/drawer pattern — human approval required (§12).
3. **Design-system coordination** — the theme lives in a separate repository.
4. **Requirements review:** D7 relaxes an authorization gate and changes externally observable
   behavior → reflect it in `docs/requirements.md` via a **dedicated PR with human approval** (cite an
   existing `GROUP-*`/`ACCESS-*` requirement or add a new one); do not bundle with implementation.
5. **Security sign-off** for the route-gate relaxation (ADMINISTRATION → READ).

### Phasing

| Phase | Scope |
|---|---|
| **P1** | Extract `ProjectAccessPanel` + `ProjectSharingComposer` + `AccessRow` + `AccessFilter`; ship on `/access`; include the read-only mode (D7) from the start; delete `AddCollaboratorToProjectDialog` and the `AlertDialog.danger` removal on access paths. |
| **P2** | `ProjectSharingDrawer` + `ProjectCardAccessSummary` on the landing page; relax the route gate to READ; URL-addressable `?share=<projectId>`. |
| **P3** | Optional drag-and-drop accelerant and keyboard shortcuts. |
| **P4** | Notification coordination cleanup and full removal of the legacy dialog/confirmation code. |

---

## 13. Open items

- Card group-chip cap (proposed ≈2 chips + `+N`).
- Whether the drawer should allow **full** edit/revoke parity or add-only (recommendation: full
  parity, since it reuses `ProjectAccessPanel`).
- Whether READ collaborators should also be able to open `/access` directly as a route (yes, per D7)
  or only through the panel reached from the list.
- Concrete requirement IDs for the visibility policy (PO-owned requirements PR).
- Scope of the future drag accelerant (P3) vs. dropping it entirely.

---

## 14. References

- `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/access/ProjectAccessComponent.java`
- `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/access/AddCollaboratorToProjectDialog.java`
- `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/access/ProjectAccessMain.java`
- `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/overview/ProjectOverviewMain.java`
- `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/overview/components/ProjectCollectionComponent.java`
- `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/datasets/ConnectDatasetSidebar.java` (drawer pattern)
- `project-management/.../application/authorization/acl/ProjectAccessService.java`
- `datamanager-app/front-end-components.md`
- [`docs/user-groups-strategy.md`](user-groups-strategy.md) — §4.5 (UI surface), §4.6 (revocation NFR), §5.1 (visibility)
- [`docs/auth-glossary.md`](auth-glossary.md)
