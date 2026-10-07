# Implementation Plan — FEAT-USER-GROUPS-06 (#1564): Share a Group onto a Project

**Status:** 🔵 Planned (ready for review)
**Parent story:** [#1564](https://github.com/qbicsoftware/data-manager-app/issues/1564) — `FEAT-USER-GROUPS-06: Share a Group onto a Project`
**Parent feature:** [#1558](https://github.com/qbicsoftware/data-manager-app/issues/1558) — `FEAT-USER-GROUPS` (EPIC 3 — Sharing onto projects & effective access)
**Strategy / architecture source:** [`docs/user-groups-strategy.md`](../user-groups-strategy.md) — §1a (two role worlds, effective access), §3 (decisions), §4.2 (SID representation), §4.3 (ACL plumbing — recommended path), §4.5 (UI surface), §4.6 (revocation NFR), §5.1 (leakage guard)
**Branch:** `development` (predecessors merged: user-groups backend #1574, My Groups view #1581)
**PR reference:** `Related to #1564` — **NOT** `Closes #1564` (story stays open until EPIC-3 siblings #1565/#1566 land)

---

## 0. Why this story is NOT simple (reality check)

The story reads as "add a group tab to the share dialog". In fact it is the **first story that wires
user groups into the Spring Security ACL decision path** — every predecessor touched group-domain
data that nobody reads for authorization. The hidden-dependency map (each → failing AC if missed):

| # | Hidden dependency | Where | Fails AC if missed |
|---|---|---|---|
| 1 | **No `SidRetrievalStrategy` exists.** `QbicPermissionEvaluator` extends Spring `AclPermissionEvaluator` and never calls `setSidRetrievalStrategy(...)`; groups are deliberately **not** in the `Authentication`, so a group member's `hasPermission` is *always false* — the share writes ACEs that grant nobody. | `QbicPermissionEvaluator.java`; `AclSecurityConfiguration.permissionEvaluator(...)` (bean ~L116) | **AC1** |
| 2 | **OWNER invariant unenforced.** `addAuthorityAccess` + `changeAuthorityAccess` both reach `setOwner(authoritySid)` for `ProjectRole.OWNER`. A group could become ACL owner today. The guard must live at the **service boundary** (application layer), authority-agnostic, because `ProjectRepositoryImpl.add()` calls the service directly — UI-only enforcement is insufficient. | `ProjectAccessServiceImpl` (addAuthorityAccess ~L228, changeAuthorityAccess ~L285) | **AC3** |
| 3 | **Project listing is group-blind.** `retrieveAccessibleProjectIdsForUser()` unions only `authentication.getAuthorities()`; groups are never there. Group-granted projects pass `hasPermission(READ)` but never appear in the project overview. | `ProjectInformationService` (~L118-129) | **AC1** (usable access) |
| 4 | **Duplicate-grant semantics** rely on `addAuthorityAccess`'s existing duplicate check (throws — correct for AC2), but its error message has an **empty `%s` format bug** (no args passed) that will surface to users in the dialog. | `ProjectAccessServiceImpl` ~L260-262 | **AC2** (UX) |
| 5 | **Multi-instance `acl_cache` staleness.** `acl_cache` is process-local EHCache, TTL 600s; `JdbcMutableAclService.updateAcl` evicts only on the writing node. Grant/revoke decisions can lag on other nodes. **The ≤60s revocation NFR is PO-confirmed** (strategy §4.6), so this must be solved, not documented away. | `ehcache3.xml`, strategy §4.6 | Revocation NFR / story 08 |
| 6 | **`listCollaborators()` filters to `PrincipalSid`** — a group branch (or new `listSharedGroups`) is required or the Project Access Groups section has no backend. | `ProjectAccessServiceImpl` (~L341-350) | **AC1** (Groups section) |
| 7 | **SID-string exactness.** `"GROUP_<id>"` must match exactly between (a) the persisted ACE SID and (b) the retrieval-strategy output. Key on the stable group *id*, never the display name; single source of truth = `GroupSidProviderImpl`. | `GroupSidProvider` / `GroupSidProviderImpl` | **AC1** (silent deny on mismatch) |
| 8 | **AC5 gate becomes group-aware for free** — once the retrieval strategy is attached to the shared `QbicPermissionEvaluator`, `UserPermissionsImpl.changeProjectAccess()` (READ && ADMINISTRATION) and the service's `@PreAuthorize(ADMINISTRATION)` both see group-granted ADMIN. Consequence: a *newly-granted group-ADMIN* immediately gets the share UI — intended per AC1 ("at the next authorization check"). | `UserPermissionsImpl` (~L36-40) | **AC5** |
| 9 | **Genuine lockout risk avoided by #1/#8:** without the strategy, a project shared *only* via groups could fail ADMIN checks for every member (nobody can administer/revoke). | — | AC1/AC5 |

**Net:** this story does ~60% of EPIC 3's architectural foundation (authorization read path + group
listing seam + OWNER invariant + multi-instance cache policy) while exposing only a fraction of it
in the UI. It is a security-primitive story wearing a UI-story costume.

---

## 1. Governance & traceability (do this first — but it is NOT blocking)

**Precedent already set by the PO:** `docs/requirements.md` contains **zero** `GROUP-*` entries, and
feature #1558 says "implementation must not start before the requirements PR is merged" — **however**
the PO (code owner) directed implementation to proceed regardless for this exact feature. PR #1574
(story 03 backend) carried the verbatim justification: *requirements entries will be added by the PO
in a dedicated docs PR*; PRs #1580/#1581 followed the same path. This plan follows suit:

- ✅ May start **without** a merged requirements PR.
- ✅ This PR must **not** modify `docs/requirements.md` (PO-owned docs PR, tracked separately).
- ✅ PR must reference **requirement IDs `GROUP-R-06`, `GROUP-R-08`** and carry the same
  PO-direction justification block as #1574 (name the PO decision + date).
- ✅ PR uses `Related to #1564` (story stays open); tasks reference **stable story id
  `FEAT-USER-GROUPS-06`**, never the issue number.
- ✅ `docs/features.md` remains untouched (stories are external per repo policy; story file
  `docs/features/FEAT-USER-GROUPS-stories.md` cited in the issue does not exist in-repo).

**Open governance item (does NOT block start, needs reviewer sign-off):**
- **ADR:** `docs/adr/` has 0001–0009 only. ADR 0010 ("user-groups bounded context") is referenced as
  *pending human approval* in `sql/migrations/create-user-groups.sql`. **No ADR exists** for the
  `GrantedAuthoritySid` sharing approach. Per AGENTS.md §12, ADR creation requires human sign-off.
  **Recommendation:** this PR records the chosen approach (D1 below) and the cache mechanism (D5)
  in its description; the ADR is created as a follow-up docs PR with explicit approval — do **not**
  create the ADR inside this implementation PR without approval.

---

## 2. Goal & scope

Implement **group sharing onto a project** end-to-end (read path + write path + listing + UI
surface), reusing the existing authority-based ACL methods. Delivers:

- **AC1** — share a group at READ/WRITE/ADMIN; SID `GROUP_<id>`; members gain the role at the next
  authorization check; group appears in the Project Access page Groups section.
- **AC2** — no duplicate grants; role change reflected (via existing change/remove semantics).
- **AC3** — OWNER rejected for groups, enforced at the **service boundary** (not just UI).
- **AC4** — sharing stays ADMINISTRATION-only (`@PreAuthorize(ADMINISTRATION)` gate + UI gate via
  `changeProjectAccess`, both group-aware after the retrieval strategy).

**Explicitly OUT of scope (guardrails):**
- ❌ **Notifications** (EPIC 4 / `FEAT-USER-GROUPS-09`). AC1 says "gains the granted project role at
  the next authorization check" — nothing about email. **No** `GroupSharedWithProject` /
  `GroupMembershipChanged` events, no email directives in this story.
- ❌ **Effective-access query engine** (story 07 / #1565, "maximal of direct + group + system role").
  This story only needs *testable evidence* a member gets the granted role via the existing evaluator.
- ❌ **Membership visibility / member counts** (story 08 / #1566 and story 14 / #1572). Groups
  section renders name + description + role only.
- ❌ **Role edit / revoke UI** for groups (story 08). Groups section is **read-only** this story.
- ❌ **Project-card group display** — related SQL views are principal-only (see §8 follow-up).
- ❌ **Public group directory / discoverability page** (#1563). The share dialog uses
  `GroupInformationService` directly with a client-filtered combo; no new directory page.
- ❌ `docs/requirements.md` edits, new ADR creation, story renumbering.

---

## 3. Architecture & key decisions

| # | Decision | Choice | Why |
|---|---|---|---|
| D1 | SID representation | **`GrantedAuthoritySid("GROUP_<id>")`** (recommended path, strategy §4.3) | Fully supported Spring ACL extension points; persistence/lookup unchanged; only check-time SID derivation customized. The alternative (custom `GroupSid`) requires re-implementing the read-path `LookupStrategy` — `BasicLookupStrategy.readAclsById` is `final`, so the read path is **not overridable**; 4 coordinated customizations, real cost/risk. Consequence: `GROUP_` prefix is a convention (type-identical to `ROLE_*`) → OWNER guard (D3) is authority-agnostic and `AuthorityService` must never emit `GROUP_*`. |
| D2 | Check-time SID derivation | New **`GroupAwareSidRetrievalStrategy`** (implements `SidRetrievalStrategy`), wired via `QbicPermissionEvaluator.setSidRetrievalStrategy(...)` | The *only* Spring ACL extension point required (§4.3). Produces default SIDs (principal + one per authority) **plus** one `GrantedAuthoritySid("GROUP_<id>")` per live membership via the existing `GroupSidProvider` bean. Single source of the SID string ⇒ "must match exactly" caveat (hidden dep #7) holds. |
| D3 | OWNER-invariant placement | **Service boundary** (`addAuthorityAccess` + `changeAuthorityAccess` reject `ProjectRole.OWNER` for any non-principal/authority SID) | `ProjectRepositoryImpl.add()` calls the service directly; ACL is the integrity boundary; UI-only enforcement insufficient (AC3 is a security invariant). Production impact nil — current authority call sites grant ADMIN only. |
| D4 | Listing merge | `ProjectInformationService.retrieveAccessibleProjectIdsForUser()` additionally unions `GroupSidProvider.listGroupSidsForUser(userId)` | Companion change mandated by §4.3.3; without it group-granted projects never appear (AC1 usability). |
| D5 | Multi-instance cache | **Mechanism must land ≤60s** (PO-confirmed NFR, §4.6); recommended: **broadcast eviction** via Artemis/JMS topic on group-ACE writes (each instance evicts the affected project OID from `acl_cache`). Fallback for reviewer: eviction-on-write + documented bounded staleness — **but only if** the propagation window is asserted ≤60s by an integration test; plain "accept 600s staleness" violates the confirmed NFR and must be rejected. | §4.6: `acl_cache` is process-local, TTL 600s, `updateAcl` evicts only the writing node. Revocation NFR is in the requirements PR (not optional prose) and must be asserted by P2 integration tests + recorded in the ADR. |
| D6 | Groups section surface | Read-only for this story: `ProjectAccessService.listSharedGroups(ProjectId)` (new, `GROUP_`-prefix branch over `acl.getEntries()`, resolve names via `GroupInformationService`); `ProjectAccessComponent` gets a second grid/section. | AC1 needs the group visible; edit/revoke = story 08. §5.1: share/revoke/role-edit stays ADMINISTRATION, non-members see name+description only. |
| D7 | AC5 semantics | No new gate code strictly required — reuses existing `changeProjectAccess` (READ && ADMINISTRATION) + `@PreAuthorize(ADMINISTRATION)`; both see group SIDs automatically once D2 is wired (same evaluator bean). | Hidden dep #8/#9. A dedicated UI check is defense-in-depth only. |

> **Cache-eviction note (read hygiene):** never pass real SID filters to `readAclById` in the group
> paths — the read is full-set under Spring's "no SID filtering" contract; a manually cached
> filtered Acl trips the `Error: SID-filtered element detected…` assertion. (Oracle verified this
> against Spring Security ACL 6.3.5 bytecode: `updateAcl` = `deleteEntries + createEntries`.)

---

## 4. File changes

### 4.1 CREATE

| Path | Responsibility |
|---|---|
| `datamanager-app/src/main/java/life/qbic/datamanager/security/GroupAwareSidRetrievalStrategy.java` | Implements `SidRetrievalStrategy`: returns default SIDs + `GrantedAuthoritySid("GROUP_<id>")` per active membership via `GroupSidProvider.listGroupSidsForUser(userId)`; userId via `AuthenticationToUserIdTranslator` (empty → no group SIDs); groups resolved lazily (never at bean construction). |
| `datamanager-app/src/test/groovy/life/qbic/datamanager/security/GroupAwareSidRetrievalStrategySpec.groovy` | Spock: principal present; authorities → `GrantedAuthoritySid`; group sids appended (mocked `GroupSidProvider`); empty membership → none; unknown auth handled. |
| `project-management/src/test/groovy/life/qbic/projectmanagement/application/authorization/acl/ProjectAccessServiceSpec.groovy` | Spock: OWNER rejected for authority SID (AC3); OWNER still allowed via `addCollaborator` (regression); ADMIN authority grant OK (protects `ProjectRepositoryImpl`); duplicate authority grant throws (AC2); `listSharedGroups` filtering. |
| `datamanager-app/src/test/groovy/life/qbic/datamanager/views/projects/project/access/AddCollaboratorToProjectDialogSpec.groovy` | Spock: group tab renders; role selection excludes OWNER; confirm fires group event with `GROUP_<id>`; duplicate → friendly message slot. |
| `datamanager-app/src/test/groovy/life/qbic/datamanager/views/projects/project/access/ProjectAccessComponentSpec.groovy` (group section) | Spock: shared groups render read-only for project ADMIN; not rendered/read-only for READ-only users (AC5); refresh on share. |
| `datamanager-app/src/test/groovy/life/qbic/datamanager/security/GroupShareAuthorizationIT.groovy` (or `**/*IT.groovy`, `it` profile) | End-to-end proof: share group as ADMIN → member `hasPermission(READ/WRITE/ADMIN)`; `retrieveAccessibleProjectIdsForUser` includes it; duplicate throws; OWNER rejected; READ-only user denied `addAuthorityAccess` (AC4). |

### 4.2 MODIFY

| Path | Change |
|---|---|
| `project-management/src/main/java/life/qbic/projectmanagement/application/authorization/acl/ProjectAccessServiceImpl.java` | **OWNER guard**: reject `ProjectRole.OWNER` in `addAuthorityAccess` and `changeAuthorityAccess` for non-principal SIDs, before `setOwner`. **Fix format bug**: duplicate-authority `ApplicationException` message (empty `%s`, no args). `changeAuthorityAccess` delete-stale-insert-new logic already yields correct AC2 role-change semantics — verify + keep. |
| `project-management/src/main/java/life/qbic/projectmanagement/application/authorization/acl/ProjectAccessService.java` (interface) | Add `List<SharedProjectGroup> listSharedGroups(ProjectId)` (DTO carrying groupId + resolved name/description + `ProjectRole`). |
| `project-management/src/main/java/life/qbic/projectmanagement/application/ProjectInformationService.java` | New ctor param `GroupSidProvider`; union `listGroupSidsForUser(userId)` into `retrieveAccessibleProjectIdsForUser()` (loop `getAccessibleProjectsForSid` per group sid, dedupe). |
| `project-management/src/test/groovy/.../ProjectInformationServiceSpec.groovy` | Extend existing accessible-projects spec with group SID case (mirror the `ROLE_EXAMPLE` mock pattern). |
| `datamanager-app/src/main/java/life/qbic/datamanager/security/AclSecurityConfiguration.java` | Wire `permissionEvaluator(...)` → `setSidRetrievalStrategy(new GroupAwareSidRetrievalStrategy(...))` on the `QbicPermissionEvaluator` bean after construction. |
| `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/access/AddCollaboratorToProjectDialog.java` | Add a group picker section: client-filtered combo over `GroupInformationService` (public group names + descriptions), filtered against already-shared groups (`listSharedGroups`); role selector READ/WRITE/ADMIN (**no OWNER** — plus service rejects OWNER, defense-in-depth); emits a group-share event (testable seam). |
| `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/access/ProjectAccessComponent.java` | Add **Groups** section (grid: name, description, role badge — read-only, no edit/remove); after group share → `projectAccessService.addAuthorityAccess(projectId, "GROUP_"+groupId, role)`; catch duplicate → friendly alert; refresh. Javadoc already promises "users and user groups" (L53/56). |
| `project-management/src/main/java/life/qbic/projectmanagement/application/authorization/authorities/AuthorityService.java` | No emit-path change; add **collision guard test** asserting no `ROLE_*`/`ProjectRole` name starts with `GROUP_` (strategy §4.3 collision guard, hidden dep #7). |
| Cache/eviction (per D5) | If broadcast: new integration event on group-ACE writes (`addAuthorityAccess`/`changeAuthorityAccess`/`removeAuthorityAccess` for `GROUP_*` sids) → Artemis/JMS topic (existing `broadcasting` module precedent) → listener evicts `acl_cache` for the affected project OID on each instance. If eviction-on-write: evict directly after the write + integration test asserting ≤60s propagation (or document the exact window for reviewer acceptance). |
| `frontend/themes/datamanager/components/…` (CSS) | Groups-section styling following the app's component CSS convention. |

> **DTO placement note:** `SharedProjectGroup` should live in the `project-management` access
> package (application layer, like the existing collaborator DTOs) — resolves through the
> `user-groups-api` facade, never directly against the `user-groups` domain.

---

## 5. Key design details

### 5.1 OWNER guard (AC3)

```java
// ProjectAccessServiceImpl — both addAuthorityAccess and changeAuthorityAccess, before setOwner
if (ProjectRole.OWNER.equals(projectRole) && authoritySid instanceof GrantedAuthoritySid) {
  throw new ApplicationException("Groups and roles can never become project OWNER.");
}
```
- Applies to **all** non-principal SIDs (groups ride as `GrantedAuthoritySid`; the `GROUP_` prefix
  is a convention, not a type) — matches strategy §4.3 "no non-principal SID may ever become the ACL
  owner" and covers both paths that currently call `setOwner`.
- `removeAuthorityAccess`/`changeAuthorityAccess` are additionally checked for operations that would
  degrade the owner's effective access (strategy §4.3) — verify current behaviour handles this;
  principal-SID ownership via `addCollaborator`/`changeRole` stays untouched.

### 5.2 Retrieval strategy + wiring (AC1/AC5 — the linchpin)

```java
// GroupAwareSidRetrievalStrategy.main
Sid[] sidRetrievalStrategy(Authentication authentication, Object target) {
  List<Sid> sids = defaultSids();                 // principal + one GrantedAuthoritySid per authority
  userIdTranslator.translateToUserId(authentication)
      .ifPresent(userId -> groupSidProvider.listGroupSidsForUser(userId)
          .forEach(sid -> sids.add(new GrantedAuthoritySid(sid))));
  return sids.toArray(new Sid[0]);
}
```
- The `"GROUP_<id>"` string comes from exactly one place (`GroupSidProviderImpl`) → persists
  round-trip identical (hidden dep #7). Never derive from group name.
- Wiring must hit the **same evaluator instance** used by method security, `UserPermissionsImpl`,
  and the UI. Verify bean wiring order in review; the IT in §4.1 proves it.
- AC5 for free: `UserPermissionsImpl.changeProjectAccess()` and the `@PreAuthorize(ADMINISTRATION)`
  annotations both resolve group SIDs through the same evaluator.

### 5.3 Listing merge (AC1)

```java
// ProjectInformationService.retrieveAccessibleProjectIdsForUser(userId, authentication)
Set<ProjectId> accessible = getAccessibleProjectsForSid(new PrincipalSid(userId));
for (String authority : authentication.getAuthorities()) accessible.addAll(getAccessibleProjectsForSid(new GrantedAuthoritySid(authority)));
groupSidProvider.listGroupSidsForUser(userId)
    .forEach(sid -> accessible.addAll(getAccessibleProjectsForSid(new GrantedAuthoritySid(sid))));
```
- N+1 over `getAccessibleProjectsForSid` per group sid — acceptable (few memberships per user);
  note in review. Constructor change ripples into bean creation + `ProjectInformationServiceSpec` —
  verify no other construction sites.

### 5.4 Groups section + share dialog (AC1/AC2/AC5)

- `listSharedGroups`: scan `acl.getEntries()`, collect `GrantedAuthoritySid`s whose authority starts
  with `GROUP_`, map to `(groupId, ProjectRole.fromPermissions(...))`, resolve name/description via
  `user-groups-api` (`GroupInformationService.findGroupById`), hide member data entirely (story 08/14).
- Share dialog: group combo filtered against already-shared group ids (AC2 pre-filter) **and** the
  service's duplicate check (AC2 enforcement) → duplicate maps to a friendly alert (fixes the message
  bug surfacing there).
- `ProjectAccessComponent` group section renders read-only (name, description, role badge) —
  §5.1-safe (no member roster, no counts) on both ADMIN and READ-graded views.
- **Build the group section inside `AddCollaboratorToProjectDialog` as a single reusable dialog**
  (it is a plain `DialogWindow` taking `(userInformationService, projectId, collaborators)` —
  constructor-injectable, not `@UIScope`/`@SpringComponent` — so it is mountable anywhere a
  `ProjectId` is known). Iteration 2 (§9) reuses this exact dialog for quick-share from the
  project list without forking it.

---

## 6. Test strategy

| Level | Spec / IT | Proves |
|---|---|---|
| Unit (Spock) | `ProjectAccessServiceSpec` | AC2 (duplicate throws, role change reflects), AC3 (OWNER rejected for authority, allowed for principal), regression for existing ADMIN authority grants |
| Unit (Spock) | `GroupAwareSidRetrievalStrategySpec` | AC1 check-time SID derivation (mocked provider) |
| Unit (Spock) | `ProjectInformationServiceSpec` (extended) | AC1 listing merge |
| Unit (Spock) | `AddCollaboratorToProjectDialogSpec` | AC1/AC2/AC3 UI: group tab, no OWNER option, group event with `GROUP_<id>`, duplicate message |
| Unit (Spock) | `ProjectAccessComponentSpec` | AC1/AC5 UI: Groups section read-only for ADMIN; hidden/read-only for READ-only |
| Unit (Spock) | `AuthorityService` collision-guard test | `GROUP_` prefix never collides with roles (strategy §4.3) |
| Integration (`it`) | `GroupShareAuthorizationIT` | **AC1** member `hasPermission(READ/WRITE/ADMIN)` + project appears in listing; **AC2** duplicate; **AC3** OWNER; **AC4** READ-only user denied `addAuthorityAccess`; **revocation NFR** propagation ≤60s (per D5 mechanism) |

Follow existing conventions: Spock `*Spec.groovy` matching `**/*Spec.class` (surefire), component
specs with injectable seams (no Spring context — `PinnedProjectsComponentSpec` precedent), `*IT.groovy`
under the `it` profile. No `@SpringBootTest` in unit specs.

---

## 7. Suggested tasks (traceability)

- **T1 — Authorization backend:** OWNER guard + duplicate-message fix (`ProjectAccessServiceImpl`),
  `GroupAwareSidRetrievalStrategy` + wiring, `ProjectInformationService` listing merge.
- **T2 — Listing + groups surface:** `listSharedGroups` (interface + impl + DTO), collision-guard
  test, cache/eviction mechanism (D5).
- **T3 — UI:** share-dialog group tab + `ProjectAccessComponent` Groups section + CSS + UI specs.

PR body: `Related to #1564`, requirement IDs `GROUP-R-06`, `GROUP-R-08`, PO-direction justification
block (mirror #1574), NFR-trace note (≤60s revocation interplay with stories 07/08), ADR-follow-up
note (D1/D5 recorded; ADR 0010 pending human approval).

---

## 8. Risks & open questions (for the human reviewer)

1. **Requirements-PR governance** — precedent set (#1574); confirm this PR carries the same
   documented justification and tracks the PO's dedicated requirements PR separately. **[Confirm]**
2. **ADR / approach sign-off** — `GrantedAuthoritySid` + retrieval strategy (D1) vs custom `GroupSid`;
   recommend D1; reviewer signs off; ADR creation itself requires human approval (§12) and is
   a **follow-up docs PR**, not part of this implementation PR. **[Reviewer decision]**
3. **Cache-eviction mechanism (D5)** — broadcast (recommended, meets ≤60s on all instances) vs
   eviction-on-write + asserted ≤60s window vs single-node confirmation. The **≤60s NFR is
   PO-confirmed** — "document 600s staleness" is **not** an acceptable option. Mechanism must be
   recorded in the ADR + asserted by P2 integration tests (strategy §4.6). **[Reviewer decision]**
4. **SID-string exactness** — single-source rule (`GroupSidProviderImpl`); add a round-trip test
   (persist ACE → retrieval strategy output matches); mismatch = silent deny. Strategy §4.3 caveat.
5. **`ProjectAccessComponent` `@UIScope` + `setContext`** — Groups section must render after
   `setContext`, reusing `showControls(changeProjectAccess)`; refresh group rows on share (mirror the
   existing user-grid refresh). Risk: UI stale rows.
6. **Group-ADMIN immediately gets share UI** — intended (AC1, "at the next authorization check");
   confirm PO wants newly-granted admin to administer immediately. **[Confirm]**
7. **`ProjectInformationService` ctor change** — ripple into bean wiring + spec mocks; verify no
   other construction sites.
8. **`acl_sid` semantics** — group SIDs must be written with `principal=0` (default auto-create via
   `JdbcMutableAclService`; duplicate key is `(sid, principal)`). Verify nothing writes groups with
   `principal=1`.
9. **GrantedAuthoritySid N+1 in listing** — acceptable at current scale; watch in review.

**Follow-up dependencies (tracked, NOT done here):**
- **SQL views `project_userinfo` / `project_overview` are principal-only** (strategy §2.2) — group
  grants invisible on project cards. Rework belongs with the project-card group display (story 12);
  record as a follow-up so it is not forgotten.
- Notifications, effective-access query engine, membership visibility, group role-edit/revoke UI,
  group directory page — see §2 OUT-of-scope.

---

## 8a. Quick-share from the project list — second iteration (follow-up, decision for reviewer)

**Context:** the PO plans to let users share a project (people + groups) directly from the
**projects list page** (a "Share" affordance), not only from within the project's access page.

**Decision recorded: fast-follow iteration 2, NOT part of this story's PR.** Rationale:

- This story is the *first* wiring of user groups into the ACL decision path. Its security
  plumbing (D2 retrieval strategy, OWNER guard, listing merge, `listSharedGroups`) currently lives
  entirely under `ProjectAccessMain`'s ADMIN gate (drawer entry + page gate + dialog + service).
  The overview card is **not** an ADMIN-gated page — it is the landing page for all roles. Mounting
  quick-share in the same PR couples two UI surfaces to unproven security plumbing and doubles the
  test matrix (a card visible to READ-only users on the wrong data path = a leak candidate).
- Sharing stays **ADMINISTRATION-only** (AC4). The card gate for quick-share is the *same* check
  the drawer and access page use: `userPermissions.changeProjectAccess(projectId)` — which becomes
  group-aware automatically once D2 lands (group-admin users see the affordance; READ/WRITE-only
  users never do).

**What iteration 2 actually is (small, precedent-shaped — see FEAT-USER-GROUPS-10):**

1. **Mount point already exists:** each project card already has a kebab `ContextMenu` in the
   top-right (`ProjectOverviewItem.buildTopRightControl`, `ProjectCollectionComponent`) with
   "Pin project"/"Unpin project" (per-card menu, sibling of the card RouterLink so it does not
   fire navigation). Add a **"Share project…"** menu item there — per-card, ADMIN-gated.
2. **Reuse, don't fork:** open the *existing* `AddCollaboratorToProjectDialog` (already containing
   the group section from §5.4) with the card's `ProjectId`. No new dialog, no new backend.
3. **Refresh:** on confirm, reuse the overview's existing `applyExternalState`/`refresh` re-render.

**Caveats to flag in the iteration-2 PR:**

- The overview avatar row is **principal-only today** (`projectOverview.collaboratorUserInfos()`,
  backed by the principal-only `project_userinfo` view — strategy §2.2 / §8 follow-up). A
  group-shared project card would show **no avatars** for group members even though access works.
  This is the deferred view-rework follow-up, **not** a quick-share blocker — but reviewers will
  notice the discrepancy; call it out explicitly.
- "Share project…" grants only; **edit/revoke stays on the `/access` page** until story 08
  (role-edit/revoke UI). If the PO wants the *full* manage surface from the overview, defer
  quick-share until after story 08 instead — reviewer decision.
- Sidebar label "USERS" → consider "ACCESS"/"Share" rename (cosmetic, out of scope for this
  story) so the overview affordance and the drawer agree on wording.

---

## 9. Build & verify

```bash
./mvnw -pl project-management -am clean verify       # OWNER guard + listing + listSharedGroups specs
./mvnw -pl datamanager-app -am clean verify          # security strategy specs + UI component specs
./mvnw -pl datamanager-app -am verify -Pit           # GroupShareAuthorizationIT (AC1–AC4 + revocation window)
./mvnw spring-boot:run -pl datamanager-app -Pdevelopment   # dev-mode smoke (share a group, verify member access)
```

Format with Google style (`GoogleStyle.xml` / project formatter) before committing.

---

## 10. Commit sequencing (single PR, incremental)

1. **Write-path hardening:** OWNER guard + duplicate-message fix + `ProjectAccessServiceSpec`. Compile + verify.
2. **Read path:** `GroupAwareSidRetrievalStrategy` + `AclSecurityConfiguration` wiring + `GroupAwareSidRetrievalStrategySpec`. Verify.
3. **Listing merge:** `ProjectInformationService` ctor param + union + spec extension. Verify.
4. **Groups surface backend:** `listSharedGroups` + DTO + interface. Spec.
5. **UI:** share-dialog group tab + Groups section + collision-guard test + CSS. Component specs.
6. **Cache/eviction mechanism** (D5) + **revocation-window IT**.
7. **End-to-end IT** (`GroupShareAuthorizationIT`), polish, format, full `verify`.

---

## 11. Traceability

- Story: **`FEAT-USER-GROUPS-06`** (#1564) — EPIC 3, parent feature #1558.
- Requirement IDs: **`GROUP-R-06`, `GROUP-R-08`** (draft proposals pending the PO's dedicated
  requirements PR; same documented precedent as PR #1574 — see §1).
- Related follow-up requirements context: `GROUP-NFR-01` (≤60s revocation) drives D5.
- PR: `Related to #1564` (never `Closes #1564`); tasks reference the stable story id.