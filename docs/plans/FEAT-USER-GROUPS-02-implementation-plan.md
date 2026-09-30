# Implementation Plan — FEAT-USER-GROUPS-02 (#1560): Manage Org Group Membership and Managers (Admin)

**Status:** 🔵 Planned (ready for implementation) — *corrected revision, verified against `development` code (2026-09-29)*
**Parent story:** [#1560](https://github.com/qbicsoftware/data-manager-app/issues/1560) — `FEAT-USER-GROUPS-02: Manage Org Group Membership and Managers (Admin)`
**Parent feature:** [#1558](https://github.com/qbicsoftware/data-manager-app/issues/1558) — `FEAT-USER-GROUPS` (EPIC 1 — Org Groups, admin-managed)
**Requirement IDs:** `GROUP-R-01`, `GROUP-R-03`, `GROUP-R-10`, `GROUP-NFR-01` *(pending PO requirements PR; implementation proceeds under recorded PO direction — see §1)*
**Strategy / architecture source:** [`docs/user-groups-strategy.md`](../user-groups-strategy.md) — §1a (two role worlds), §3 (governance/visibility/notifications decisions), §4.2 (data model, no OWNER row for org groups), §4.3 (SID representation + live per-check derivation), §4.5 (Admin Groups view), §4.6 (revocation NFR ≤60s)
**Branch:** `development` (predecessors merged: user-groups backend #1574, My Groups + group detail #1580/#1581, ad-hoc management #1577 (+af217aadb), sharing #1585, groups nav #1586, project-access groups #1587, org-group creation #1589)
**PR reference:** `Related to #1560` — **NOT** `Closes #1560` (EPIC-1 closes when story 01 **and** story 02 both land)

---

> **Correction (2026-09-30) — project-administrator informing withdrawn from this story.**
>
> The PO confirmed that the official, approved user-story document is authoritative, and that
> `FEAT-USER-GROUPS-02` AC2 does **not** require project owners/admins to be informed of a
> membership change — that clause was added to GitHub issue #1560 in error. The
> project-owner/admin informing belongs to the notifications story (`FEAT-USER-GROUPS-09`,
> EPIC 4, proposed `GROUP-R-10`) and is **not** approved for implementation yet.
>
> Consequently, design decision **D4**, D4b, file-list items 10–13, the directive tests and the
> AC2 second-half mapping below are **withdrawn from this PR**. No
> `InformProjectAdministratorsAboutMembershipChange` directive/policy and no
> `ProjectAccessService.listProjectAdministrators` seam are shipped here; the pre-existing
> member-facing emails (`MemberAccessPolicy` → `InformAddedGroupMember`/`InformRemovedGroupMember`)
> are unchanged. Treat the D4-related sections below as historical planning context only.

## 0. What this story actually is (verified reality check)

The story reads as "let an admin manage an org group's members and managers". **The code already implements most of it** — the only missing piece is a small, precisely-scoped admin gate. Verified on `development`:

| # | Verified fact | Evidence |
|---|---|---|
| 1 | **Org MANAGER members already get full manager capabilities through the existing, type-agnostic role gates.** `UserGroup.addMember`, `removeMember` (other regular members), `rename`, `updateDescription` all gate on OWNER/MANAGER *membership role* — **never on `GroupType`**. An org MANAGER can add/remove regular members, rename, describe **today, zero new code**. | `UserGroup.java` (`requireRole` + each method) |
| 2 | **The one gap: there is no path to appoint the *first* manager.** `UserGroup.appointManager` requires `requireRole(..., OWNER)`; org groups have **no OWNER row** (by design, strategy §3); the QBiC admin holds no membership at all. Nobody can become a manager. | `UserGroup.appointManager`, `UserGroup.createOrg` (empty roster) |
| 3 | **The QBiC admin also cannot remove a manager.** `UserGroup.removeMember` forbids a non-OWNER from removing a MANAGER ("Only the group owner may remove a manager"). Admin has no OWNER row → cannot remove a manager (AC4's manager-replacement path). | `UserGroup.removeMember` |
| 4 | **AC4's no-manager fallback already works structurally.** Org groups **never auto-dissolve** — `removeMembership`/`removeMember` only dissolve for `GroupType.ADHOC`. An empty org stays ACTIVE, admin-governed; `createOrg` guarantees no OWNER row ever. | `UserGroup.removeMembership/removeMember`, `UserGroup.createOrg` |
| 5 | **AC3 revocation needs no ACL/cache work.** `GroupSidProviderImpl` derives `GROUP_<id>` SIDs **live from the DB per check** (fresh `findActiveGroupsByUserId` read; no `@Cacheable` in `user-groups*`). Removing a membership removes the SID at the next check on every node. The project-grant (ACE) axis ≤60s propagation is already solved by story 08's broadcast eviction (verified `AclEvictionPublisher`). | `GroupSidProviderImpl`, `GroupAwareSidRetrievalStrategy`, `ProjectAccessServiceImpl.publishAclEviction` |
| 6 | **Member add/remove notification directives already exist and need no code for the member email.** `MemberAccessPolicy` subscribes `InformAddedGroupMember` (`MemberAddedToGroup`) + `InformRemovedGroupMember` (`MemberRemovedFromGroup`), wired in `AppConfig` (lines ~256–284). Once org ops dispatch these events, the **"newly-gained-access" member email works for free** (per-membership → trivially deduplicated). | `MemberAccessPolicy`, `InformAddedGroupMember`, `InformRemovedGroupMember`, `AppConfig` |
| 7 | **What does NOT exist anywhere: informing project owners/admins of shared projects about membership changes** (AC2's second half). `ProjectAccessService.getAccessibleProjectsForSid(String)` exists and is **not** `@PreAuthorize`-gated (verified, line 452), which is the reverse-lookup seam a JobRunr directive needs. | `ProjectAccessServiceImpl.getAccessibleProjectsForSid` (line 452, no annotation) |
| 8 | **The admin UI for story 02 does not yet exist.** `AdminGroupsMain` is a **read-only directory** (rows: name/description/type), with only a "New Organisational Group" action to `AdminGroupCreationMain`. No per-row manage action, no manager list. | `AdminGroupsMain.java` |
| 9 | **Manager demotion (MANAGER→MEMBER staying in group) is an open §8 item** and is **explicitly out of scope**. `appointManager`/`demoteManager` (ad-hoc, OWNER-gated) must **not** be exposed for org groups. The admin path only ever *removes* a manager's membership (that's removal, not demotion — allowed, AC4). | issue Notes (§8), AC4 wording |

**Net:** this story is **not** a governance-seam rewrite. It is:
1. **Two new admin-gated application/domain methods** — `appointOrgManager` and `removeOrgManager` (the only missing manager lifecycle), plus
2. **A small admin UI** — "manage managers" affordance on the existing `AdminGroupsMain` (no members-block, no profile editing — managers do that themselves), plus
3. **One new notification directive** — informing project owners/admins of shared projects (AC2 second half), and
4. **Tests locking the invariants** (no OWNER row, no org auto-dissolve, admin oversight, AC3 revocation proof).

No schema change, no ACL/SID change, no new Maven modules, no changes to manager/member operations.

---

## 1. Governance & traceability

Precedent set by the PO for this feature family (recorded 2026-09-28): `docs/requirements.md` contains **zero** `GROUP-*` entries; implementation proceeds under explicit PO direction with the justification carried in the PR description (PRs #1574/#1585/#1589). This plan follows suit:

- ✅ May start **without** a merged requirements PR (PO confirmed for this feature family 2026-09-28).
- ✅ This PR must **not** modify `docs/requirements.md` (PO-owned docs PR, tracked separately).
- ✅ PR must reference requirement IDs **`GROUP-R-01`, `GROUP-R-03`, `GROUP-R-10`, `GROUP-NFR-01`** and carry the PO-direction justification block (name the PO decision + date), mirroring #1589.
- ✅ PR uses `Related to #1560` (story stays open); tasks reference **stable story id `FEAT-USER-GROUPS-02`**, never the issue number.
- ✅ `docs/features.md` untouched (stories external per repo policy; `docs/features/FEAT-USER-GROUPS-stories.md` is not in-repo on `development`).
- ✅ **No schema migration** — `create-user-groups.sql` already covers `user_group` + `group_membership` (role MANAGER/MEMBER; no OWNER row is ever created for ORG). Manager assignment is a role value on the existing `group_membership` table.
- **ADR:** do **not** create an ADR inside this PR (AGENTS.md §12). Record D1–D5 in the PR description; ADR 0010 (user-groups bounded context + SID approach) remains a follow-up docs PR with explicit approval.

---

## 2. Goal & scope

Implement **the missing admin manager-lifecycle for org groups** end-to-end (application paths behind the existing admin port + a small admin UI affordance on the existing Admin Groups directory), reusing the existing aggregate, role gates, events, directives and sharing substrate.

**In scope:**

- **AC1** — QBiC admin appoints a manager to an org group. The user gains a MANAGER *membership* and — through the **already-working** role-gated paths — can add/remove regular members and rename/describe the group, exactly like a manager of an ad-hoc group (this is the PO-confirmed model: *"org managers see org groups and manage them exactly like normal ad-hoc groups"*). Appointment covers both **direct appointment of a non-member** (creates MANAGER membership) and **promotion of an existing MEMBER**.
- **AC2** — Add/remove member updates the roster (existing paths for managers; nothing new). The added member receives the newly-gained-access email via the **existing** `InformAddedGroupMember` directive (fires automatically once org member adds dispatch `MemberAddedToGroup`). **New:** project owners/admins of projects the group is shared with are informed of the membership change (`InformProjectAdministratorsAboutMembershipChange`, D4).
- **AC3** — Removing a member revokes their group-derived project access at the **next authorization check** on every node (live SID derivation — no cache work), proven by a new integration test.
- **AC4** — An org group with no remaining managers stays **ACTIVE** and admin-governed; **no OWNER membership row is ever created**. The admin may remove the last manager (governance intact; admin = owner-equivalent at the application layer).

**Explicitly OUT of scope (guardrails):**
- ❌ **Manager demotion** (MANAGER → MEMBER while staying in the group) — open §8 item; `demoteManager`/`appointManager` must **not** be exposed for org groups.
- ❌ **Admin management of regular members** — org managers do this via the existing My Groups / group-detail UI (PO model). No members-block on the admin page.
- ❌ **Admin rename/description editing** — org managers already can (existing `rename`/`updateDescription`, MANAGER-gated, type-agnostic). No admin profile-editing surface.
- ❌ **Deduped per-project newly-gained-access digest / join digest** — story 09 (#1567) + effective-access engine (story 07/#1565). This story keeps the existing per-membership email + the new project-admin informing; the AC2 "(deduplicated)" wording is satisfied trivially per-membership (one group join → one email).
- ❌ **Admin dissolve of org groups** — blast-radius cleanup (orphaned ACEs/membership rows) belongs to EPIC-6 story 13 (#1571). Org groups never auto-dissolve; admin-initiated dissolve stays out.
- ❌ **ACL/ACE/SID changes, broadcast eviction** — AC3 needs none (live derivation, verified); the ACE axis is already solved by story 08 (#1587, `publishAclEviction`).
- ❌ **Ad-hoc group oversight by admins** — out of scope (org-only in this story). `listMembers` stays member-only for ad-hoc (no change).
- ❌ Group-size/count limits (strategy §8), audit story (FEAT-USER-GROUPS-15), org-group pre-seeding of project grants.
- ❌ `docs/requirements.md`, `docs/features.md`, ADR, migrations, `listPublicDirectory` semantic changes.

---

## 3. Design decisions

### D1 — Admin-gated manager lifecycle on the aggregate (the ONLY missing domain piece)
The existing aggregate is type-agnostic and role-gated; org groups have no OWNER row and the admin holds no membership. Add **two** ORG-only methods that require **no membership role** — the authority comes from the application-layer admin port (exactly the `createOrg` pattern: factory not role-gated, gate in `GroupService`):

```java
// UserGroup
public void appointOrgManager(String actingAdminUserId, String userId, Instant joinedAt)
    // asserts type == ORG && active; creates MANAGER membership if absent
    // (direct appointment), or promotes MEMBER → MANAGER if present; no-op if already
    // MANAGER; NEVER creates an OWNER row.
public void removeOrgManager(String actingAdminUserId, String userId)
    // asserts type == ORG && active; removes a MANAGER's (or MEMBER's) membership.
    // Allowed even when it removes the LAST manager (AC4). Never auto-dissolves an
    // org group. Does not demote (out of scope) — it removes the membership.
```

- Type-assert `ORG` and reject dissolved groups so ad-hoc callers can never reach them.
- `joinedAt` for direct appointment = `Instant.now()` at the application layer.
- Rejected alternative: threading an `isAdmin` predicate into the existing `appointManager`/`removeMember` — mutates story-04-spec'd ad-hoc paths (adds a concept ad-hoc must not have) and breaks their tests.
- Rejected alternative: a separate `OrgGroupAdministration` domain service — more files, identical effect.

### D2 — Application-layer admin-gated methods on `GroupService`
Add to `GroupService` (both call `groupAdministrationPermission.isAdmin(actingAdminUserId)` **before any write**, returning `ACCESS_DENIED`, matching `createOrgGroup` exactly):

```java
public Result<Void, ApplicationException> appointOrgManager(String groupId, String actingAdminUserId, String userId)
public Result<Void, ApplicationException> removeOrgManager(String groupId, String actingAdminUserId, String userId)
```

- Reuse existing helpers: `resolveActiveGroup`, `groupNotFound`, `accessDenied`, `systemFailure`, `userNotFound` (via `UserInformationService.findById` pre-check for appointment — same as `addMember`).
- Errors: dissolved/wrong-type group → `GENERAL`; unknown target user → `userNotFound`; non-admin → `ACCESS_DENIED`; target already MANAGER (appointment) / not a member (removal) → `GENERAL`.
- `GroupManagementServiceImpl` (facade) delegates both (policy-free, `run(...)` → `IllegalArgumentException`, verified pattern).

### D3 — Events: reuse the existing vocabulary, no new event types
`GroupDomainService` gains `appointOrgManager` / `removeOrgManager` that store + dispatch **existing** events so the pre-wired directives fire:

| Operation | Event(s) |
|---|---|
| `appointOrgManager` — non-member **direct appointment** | `MemberAddedToGroup` (membership gained → existing `InformAddedGroupMember` email; previously they had no access → newly-gained semantics are trivially correct) |
| `appointOrgManager` — existing MEMBER **promoted** | `GroupMembershipRoleChanged(MEMBER → MANAGER)` (audit-hook; **no email** — role changes are audit-only per notification profile, and the user already had group membership/access) |
| `removeOrgManager` | `MemberRemovedFromGroup` (→ existing `InformRemovedGroupMember` email). **Never** `GroupDissolved` (org groups never auto-dissolve). |

Rationale: `GroupMembershipRoleChanged` cannot express "from nothing to MANAGER" (non-null `GroupRole` params, verified) — treating direct appointment as *membership gained* sidesteps null-tolerance changes and keeps serialization/back-compat untouched. Member add/remove by an *org manager* already dispatch the same events via the existing `addMember`/`removeMember` domain-service paths — no change. **Scope note for story 09:** the deduped per-project digest builds on `MemberAddedToGroup`/`MemberRemovedFromGroup` (already org-capable) + the story-07 effective-access engine.

### D4 — New notification directive (AC2 second half), in the composition root
Membership events are dispatched in-process (`DomainEventDispatcher.instance()`) in the `user-groups` module; the directive needs `ProjectAccessService` (project-management). Create the directive + policy in **`datamanager-app`** (composition root sees both contexts; `MemberAccessPolicy` is already a composition-root bean in `AppConfig`, verified lines ~256–284):

- `datamanager-app/.../views/groups/notifications/InformProjectAdministratorsAboutMembershipChange.java` — `DomainEventSubscriber<MemberAddedToGroup>` + `DomainEventSubscriber<MemberRemovedFromGroup>`; JobRunr-scheduled `@Job`:
  1. Resolve shared projects: `projectAccessService.getAccessibleProjectsForSid(GROUP_SID_PREFIX + groupId)` — exists, **not** `@PreAuthorize`-gated (verified `ProjectAccessServiceImpl` line 452), safe for JobRunr (no security context).
  2. For each project, resolve owners/admins. `listCollaborators` is `@PreAuthorize(READ)` (verified line 497) — **do not** reuse inside a JobRunr job. Use the existing ACL principal scan used by `listCollaborators`, exposed as a **new non-gated** `ProjectAccessService.listProjectAdministrators(ProjectId)` (D4b) returning OWNER/ADMIN principal SIDs.
  3. Email each owner/admin via the existing `EmailServiceProvider` → `UserGroupsEmailServiceProvider` pattern (verified `AppConfig`). Skip silently when the group is shared on no projects or a project has no owner/admin principals.
- Wire with a new small policy bean in `AppConfig` (alongside `memberAccessPolicy`).

**D4b — non-gated admin-enumeration seam.** Add `ProjectAccessService.listProjectAdministrators(ProjectId)` (interface + impl) returning the principals holding project OWNER/ADMIN grants. Deliberately annotation-free (JobRunr use). Small, deliberate cross-context seam in `project-management` — no dependency-direction change (it already depends on `user-groups-api`). No `@PreAuthorize`.

**Template:** new `Messages.informProjectAdministratorsAboutMembershipChange(...)` in the composition-root directive (cross-context concern lives at the seam). Reuse the group name; keep the text generic ("A member was added to / removed from the group '…' shared with your project").

### D5 — Admin UI: extend the existing read-only directory, no new page
`AdminGroupsMain` (verified: read-only directory, admin-gated, `groups/admin`) gains a **"Manage managers" affordance** per org-group row (navigate to a manager-management view i.e. route `groups/admin/:groupId/managers`, or an in-place dialog — recommend a dedicated lightweight view `AdminGroupManagersMain` to match the existing page-per-surface pattern). The view:
- Lists the org group's MANAGER members (admin oversight via `GroupService.listMembers` + admin branch — see D6) with member count.
- **Assign manager**: user-search (reuse `UserInformationService` filter pattern from `GroupMembersComponent.openAddMemberDialog`) → `appointOrgManager`.
- **Remove manager**: per-row remove → `removeOrgManager`, with an `AlertDialog` confirm when removing the **last** manager: *"No managers remain — QBiC admins retain governance of this group (the group stays active; no owner is created)."* (AC4).
- **No members-block, no profile editing** — org managers manage members/profile through the existing My Groups / group-detail UI (PO model).
- Route gate: `@PermitAll` + `BeforeEnter` admin re-check (`GroupAdministrationPermission`, same as `AdminGroupsMain`/`AdminGroupCreationMain`, verified). The service gate (D2) remains authoritative.
- Rejected alternative: a full `AdminGroupDetailMain` page with members-block + profile editing (the oracle's original D8) — contradicts the PO model (managers handle members/profile) and duplicates the existing `GroupDetailMain` capabilities.

### D6 — Admin oversight branch in `listMembers` (read-only, minimal)
`GroupService.listMembers(groupId, viewerId)` currently returns empty for non-members. Per strategy §3 ("…and QBiC admins for oversight"), let a QBiC admin view **org** rosters:

```java
boolean isMember = group.memberships().stream().anyMatch(m -> m.userId().equals(viewerId));
boolean adminOversight = group.type() == GroupType.ORG
    && groupAdministrationPermission.isAdmin(viewerId);
if (!isMember && !adminOversight) return List.of();
```

Ad-hoc rosters stay member-only (no change). The admin manage view uses this for the manager list. **Do not** leak rosters through `GroupInfo`/`GroupInformationService` (public directory stays name/description/type only).

### D7 — AC4 invariants (no-manager fallback), enforced in three places
1. **Aggregate:** `removeOrgManager` permits removing the last MANAGER; org groups **never** auto-dissolve (verified: only `ADHOC` dissolves); `appointOrgManager` never creates an OWNER row.
2. **Application:** the admin gate makes the admin owner-equivalent for *governance* (appoint/remove managers); `listMyGroups(admin)` stays empty for org groups (admins are not members) — already asserted by the story-01 IT.
3. **UI:** last-manager-removal confirm + (optional) subtle "No managers — QBiC admins govern this group" state on the manage view.

### D8 — Synchronization edge: manager-removed-mid-session
When an org group's manager is removed by the admin, that user's existing UI session may still show group-manage controls. This is **out of scope for a server-rendered Vaadin app** (next request/check re-evaluates membership role at the application boundary; `GroupService.addMember` etc. all re-check `requireRole` against the current roster — no stale session state can mutate). No SID/cache work — membership removal changes the live SID derivation at the next authorization check (AC3).

---

## 4. File-by-file change list

**Domain (`user-groups/src/main/java/life/qbic/usergroups/`)**
1. `domain/model/UserGroup.java` — add `appointOrgManager(...)` and `removeOrgManager(...)` (D1; ORG type-assert, dissolved-reject, no role gates, no auto-dissolve, never OWNER).
2. `domain/service/GroupDomainService.java` — add `appointOrgManager(...)` / `removeOrgManager(...)` (persist + dispatch per D3).

**API (`user-groups-api/src/main/java/life/qbic/usergroups/api/`)**
3. `GroupManagementService.java` — add `appointOrgManager` / `removeOrgManager` (javadoc: admin-gated, org-only). No changes to `GroupAdministrationPermission`, `GroupSidProvider`, `GroupMember`, `GroupRole`, `GroupInfo`.

**Application (`user-groups/src/main/java/life/qbic/usergroups/application/`)**
4. `GroupService.java` — D2 methods (admin gate before any write); D6 `listMembers` oversight branch.
5. `service/GroupManagementServiceImpl.java` — delegate the two new methods (facade stays policy-free).

**Infrastructure (`user-groups-infrastructure/`)** — **no change** (columns/roles exist; no migration).

**Composition root (`datamanager-app/`)**
6. `views/AppRoutes.java` — add `GroupsRoutes.ADMIN_GROUP_MANAGERS = "groups/admin/:groupId/managers"` next to the existing group routes.
7. `views/groups/AdminGroupsMain.java` — add per-row **Manage** affordance navigating to `AdminGroupManagersMain`; optional: show manager/member count per row (admin-visible oversight, fed by `listMembers` admin branch — never the public directory).
8. `views/groups/AdminGroupManagersMain.java` — **new** lightweight admin view (D5): manager list, assign-manager search, remove-manager (last-manager confirm), empty-manager state; `@PermitAll` + `BeforeEnter` admin gate.
9. `views/groups/GroupsNavigationComponent.java` — extend `selectTabFor`: map `AdminGroupManagersMain → AdminGroupsMain` (Admin tab) alongside the existing mappings.
10. `views/groups/notifications/InformProjectAdministratorsAboutMembershipChange.java` — **new** directive (D4, JobRunr `@Job`).
11. `views/groups/notifications/InformProjectAdministratorsPolicy.java` — **new** policy bean subscribing the directive; wire in `AppConfig`.
12. `AppConfig.java` — wire the new directive + policy beans (`informProjectAdministrators...`, `...Policy`), mirroring the `informAddedGroupMember`/`memberAccessPolicy` block (lines ~256–284); no new `GroupService` construction (methods live on existing bean).
13. `project-management/.../authorization/acl/ProjectAccessService.java` + `ProjectAccessServiceImpl.java` — add non-gated `listProjectAdministrators(ProjectId)` (D4b; principal OWNER/ADMIN scan, no `@PreAuthorize`).
14. `datamanager-app/src/main/resources/messages/toast-notifications.properties` — add `user-groups.org.manager.assigned.success/.error`, `user-groups.org.manager.removed.success/.error`; new project-admin informing template key.
15. `frontend/themes/datamanager/components/my-groups.css` (or sibling) — admin manage view styles (manager list, empty-manager state).

---

## 5. Test plan

| Level | Spec/Target | New tests |
|---|---|---|
| Domain | `user-groups/.../domain/model/UserGroupSpec.groovy` | `appointOrgManager`: non-member → MANAGER membership created (no OWNER); MEMBER → promoted MANAGER; already-MANAGER → no-op; ADHOC rejected; dissolved rejected; never OWNER. `removeOrgManager`: removes MANAGER/MEMBER membership; last-manager removal allowed; **no dissolve flag for ORG**; ADHOC rejected; unknown user → false |
| Domain | `.../domain/service/GroupDomainServiceSpec.groovy` | `appointOrgManager`: direct → `MemberAddedToGroup` (no role-changed); promotion → `GroupMembershipRoleChanged(MEMBER→MANAGER)` (no added-member email); `removeOrgManager` → `MemberRemovedFromGroup` and **`0 * GroupDissolved`** |
| Application | `.../application/GroupServiceSpec.groovy` | both methods: admin → success; non-admin → `ACCESS_DENIED` + nothing persisted; unknown target → error; dissolved/ADHOC → error; appointment of already-MANAGER → clean error. **No OWNER row** (`listMyGroups(admin)` empty, membership role assertion). `listMembers`: org roster visible to admin (oversight); empty for non-member non-admin; ad-hoc roster stays member-only (regression) |
| Application | `.../service/GroupManagementServiceImplSpec.groovy` | facade delegates; errors → `IllegalArgumentException` (mirror `run` mapping) |
| Application | `.../service/GroupSidProviderImplSpec.groovy` | regression: org MANAGER membership produces `GROUP_<id>` sid; removed membership stops producing it (next-check contract) |
| UI component | `datamanager-app` `AdminGroupsMainSpec.groovy` (extend) | rows expose the Manage affordance navigating to `AdminGroupManagersMain` |
| UI component | `datamanager-app` `AdminGroupManagersMainSpec.groovy` | admin gate (non-admin rerouted); manager list renders (oversight); assign fires `appointOrgManager`; remove fires `removeOrgManager`; last-manager removal shows AC4 confirm |
| Directive | `datamanager-app` `InformProjectAdministratorsAboutMembershipChangeSpec.groovy` | group shared on project → owners/admins emailed; unshared → no email; empty admin set → no-op; MemberAdded + MemberRemoved variants |
| Integration (`it`) | new `OrgGroupManagementIT.java` (datamanager-app, mirror `OrgGroupCreationIT`) | **AC1**: admin appoints manager → MANAGER membership; manager can `addMember`/`rename` via existing stack (regression across `GroupService`); **AC2**: manager add persists + `MemberAddedToGroup`; project-admin directive targets shared projects (event/subscriber assertion); **AC3**: after manager removes a member, `GroupSidProvider` SIDs for that user exclude `GROUP_<id>` → permission check on a shared project flips at next check; **AC4**: removing the last manager leaves group ACTIVE, admin-governed; JPQL count of OWNER memberships == 0; admin `listMyGroups` empty |

Also: existing `GroupShareAuthorizationTest`/`OrgGroupCreationIT` stay green — org groups with members now produce SIDs; removal of membership is invisible to ACL persistence (only SID derivation changes).

---

## 6. Acceptance-criteria mapping

| AC | Implementation | Test |
|---|---|---|
| **AC1** (assign manager → MANAGER role; manager can add/remove members + rename/describe) | `UserGroup.appointOrgManager` + `GroupDomainService.appointOrgManager` + admin-gated `GroupService.appointOrgManager`; manager capabilities = **existing** type-agnostic role-gated paths (verified — no new code) | `UserGroupSpec`, `GroupServiceSpec`, `OrgGroupManagementIT` |
| **AC2** (roster updates; added member gets newly-gained-access email; project owners/admins informed) | roster = existing persistence; member email = existing `InformAddedGroupMember` (fires via existing `MemberAddedToGroup` on manager adds — no new code); **new** `InformProjectAdministratorsAboutMembershipChange` directive (D4) | `GroupDomainServiceSpec` (events), `InformProjectAdministrators...Spec`, `OrgGroupManagementIT` |
| **AC3** (removed member loses group-derived access at next check, ≤60s) | no code change required — live per-check SID derivation (`GroupSidProviderImpl`, verified no membership caching); **proof** via IT; project-grant axis already covered by story 08's broadcast eviction | `OrgGroupManagementIT` (revocation assertions), `GroupSidProviderImplSpec` regression |
| **AC4** (no remaining managers → admin owner-equivalent, governed, no OWNER row) | `removeOrgManager` allows last-manager removal + org never auto-dissolves + never creates OWNER; application-level admin governance; UI confirm + empty-manager state | `UserGroupSpec`, `GroupServiceSpec` (no OWNER row), `AdminGroupManagersMainSpec`, `OrgGroupManagementIT` |
| **NFR `GROUP-NFR-01`** (≤60s revocation) | membership axis structurally met (live derivation, verified no membership cache); asserted by IT; **no transport work** (project-grant axis already solved by story 08's broadcast eviction) | `OrgGroupManagementIT` (revocation), note in PR description |

---

## 7. Sequencing & dependencies

- Lands **after** story 01 (#1589, merged — `createOrg`, admin port, `AdminGroupsMain`, `AdminGroupCreationMain` all exist) and after stories 03/04 (aggregate, role gates, events, directives) and 06/08 (sharing + broadcast eviction). All verified present on `development`.
- Does **not** bundle story 09 (deduped per-project digest; needs story-07 effective-access engine). Story 02 delivers the project-admin informing half of AC2 and relies on the existing member email. **Handoff note in PR body:** story 09 builds the per-project digest on `MemberAddedToGroup`/`MemberRemovedFromGroup` (already org-capable) + the story-07 effective-access query.
- Does **not** bundle admin dissolve — EPIC-6 story 13 (#1571) owns orphaned-ACE/membership cleanup; record the handoff in the PR body.
- `GroupAwareSidRetrievalStrategy`/`AclSecurityConfiguration` untouched (no ACL/SID changes needed for membership revocation).

---

## 8. Risks / open questions

| Item | Resolution |
|---|---|
| Governance: requirements-PR / PO direction | **Resolved by precedent** (2026-09-28; PRs #1574/#1585/#1589). Carry the justification block + requirement IDs `GROUP-R-01`, `GROUP-R-03`, `GROUP-R-10`, `GROUP-NFR-01`. |
| PO model confirmed: org managers manage org groups "exactly like ad-hoc groups" | **Confirmed by PO during planning (2026-09-29).** This is the governing decision: no admin members-block, no admin profile-editing. Managers use existing role-gated paths. |
| Scope split vs. story 09 (deduped per-project digest) | Story 02 ships project-admin informing + existing member emails; digest/dedupe = story 09 (needs story-07 engine). AC2 "(deduplicated)" satisfied per-membership (one join → one email). **Record the split in the PR body.** |
| Direct manager appointment event semantics (D3) | **Decided:** non-member appointment → `MemberAddedToGroup` (membership gained); MEMBER promotion → `GroupMembershipRoleChanged(MEMBER→MANAGER)` (audit-only, no email). Avoids null-role serialization changes. |
| JobRunr + `@PreAuthorize` (D4/D4b) | `getAccessibleProjectsForSid` is **not** `@PreAuthorize`-gated (verified line 452); `listCollaborators` **is** (verified line 497) → new non-gated `listProjectAdministrators`. Both verified before finalizing. |
| Manager removal vs. demotion (AC4 vs. §8 open item) | **Decided:** admin removes a manager's *membership* (removal — in scope, AC4); MANAGER→MEMBER while staying (demotion — out of scope, §8). UI exposes only removal. |
| UI surface: dedicated view vs. in-place dialog | **Recommend:** lightweight dedicated view `AdminGroupManagersMain` at `groups/admin/:groupId/managers` (matches the page-per-surface pattern; simpler testability). In-place dialog is acceptable if the reviewer prefers fewer routes. |
| `listMembers` admin oversight branch (D6) | **In scope (minimal):** org rosters visible to QBiC admins for the manage view; ad-hoc rosters stay member-only. Aligns with strategy §3 ("…and QBiC admins for oversight"). |
| Admin rename/describe of org groups | **Out of scope by PO model** — org managers already can (existing MANAGER-gated `rename`/`updateDescription`, type-agnostic). |
| Multi-instance revocation proof | IT proves next-check semantics + no membership caching. True two-node proof impractical in CI (same caveat as story 08 — broker/process-level evidence accepted). **Confirm evidence level with reviewer.** |

---

## 9. PR description requirements

- Reference `Related to #1560` (not Closes); list requirement IDs **`GROUP-R-01`, `GROUP-R-03`, `GROUP-R-10`, `GROUP-NFR-01`**.
- Carry the PO-direction justification block for the missing requirements PR (mirror #1589 wording; name PO decision + date 2026-09-28).
- Record the **PO-confirmed model** (2026-09-29): org managers manage org groups exactly like ad-hoc groups; admins only initiate org groups + appoint/remove managers; no admin members-block or profile editing.
- Record D1 (admin-gated manager lifecycle), D3 (event reuse — no new event types), D4 (composition-root directive + non-gated `listProjectAdministrators`), D6 (admin oversight in `listMembers`).
- Note **`GROUP-NFR-01`** (≤60s revocation): membership axis structurally met via live per-check SID derivation (no membership cache; verified), asserted by the new IT; project-grant axis already covered by story 08's broadcast eviction (#1587) — no additional work.
- Handoff notes: (a) story 09 builds the deduped digest on these (already org-capable) events + story-07 engine; (b) EPIC-6 story 13 (#1571) owns admin-dissolve cleanup — deliberately not bundled.
- Explicit out-of-scope list per §2 (demotion, admin member/profile management, dissolve, ad-hoc oversight, digest).

---

## Build & verify

```bash
./mvnw -pl user-groups -am clean verify            # aggregate/domain-service/application specs
./mvnw -pl project-management -am clean verify     # listProjectAdministrators seam
./mvnw -pl datamanager-app -am clean verify        # admin view + directive specs + regression
./mvnw -pl datamanager-app -am verify -Pit         # OrgGroupManagementIT (AC1–AC4 + revocation)
./mvnw spring-boot:run -pl datamanager-app -Pdevelopment   # dev smoke: create org → appoint manager → manager adds/removes members + renames → admin removes last manager → verify governance
```

Format with Google Java Style (`GoogleStyle.xml`) before committing.

---

*Revision note (2026-09-29): this plan supersedes the earlier oracle draft. The earlier draft incorrectly assumed org groups were administered via an admin members-block (D8) and asserted the existing manager paths needed rework. Verified code (`UserGroup`, `GroupService`, `GroupDomainService`, directives, `AdminGroupsMain`, `ProjectAccessService`) shows the manager capabilities already exist type-agnostically; the PO confirmed managers manage org groups exactly like ad-hoc groups. The corrected scope is: two admin-gated manager-lifecycle methods + small admin manage view + one project-admin notification directive + tests.*