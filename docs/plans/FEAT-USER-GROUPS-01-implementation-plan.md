# Implementation Plan — FEAT-USER-GROUPS-01 (#1559): Create Org Groups (Admin)

**Status:** 🔵 Planned (ready for implementation)
**Parent story:** [#1559](https://github.com/qbicsoftware/data-manager-app/issues/1559) — `FEAT-USER-GROUPS-01: Create Org Groups (Admin)`
**Parent feature:** [#1558](https://github.com/qbicsoftware/data-manager-app/issues/1558) — `FEAT-USER-GROUPS` (EPIC 1 — Org Groups, admin-managed)
**Requirement IDs:** `GROUP-R-01`, `GROUP-NFR-02` *(pending PO requirements PR; implementation proceeds under recorded PO direction — see §1)*
**Strategy / architecture source:** [`docs/user-groups-strategy.md`](../user-groups-strategy.md) — §1a (two role worlds), §3 (decisions), §4.2 (data model, no OWNER row for org groups), §4.3 (SID representation — not touched by this story), §4.5 (Admin Groups view), §4.6 (revocation NFR — out of scope)
**Branch:** `development` (predecessors merged: user-groups backend #1574, My Groups view #1580/#1581, sharing #1585, project-access groups #1587, nav #1586)
**PR reference:** `Related to #1559` — **NOT** `Closes #1559` (story stays open until sibling story 02 `FEAT-USER-GROUPS-02` lands)
**Reviewer approvals (2026-09-28, PO/code owner):**
- ✅ PO direction to proceed without merged requirements PR (same as PRs #1574/#1585)
- ✅ Admin gate = permission port in `user-groups-api` (no Spring Security in `user-groups` module)
- ✅ Admin UI = admin-only tab in the Groups hub at `groups/admin/new` (not Settings, not a new top-level area)
- ✅ No admin directory/oversight page in this story (deferred to story 02)
- ✅ No ADR in this PR (recorded in PR description; ADR is a follow-up docs PR)
- ✅ Shared duplicate-name message reused verbatim; landing page = `MyGroupsMain`

---

## 0. Why this story is NOT as simple as it looks (reality check)

The story reads as "add an admin-gated create form". The backend substrate (DB, case-insensitive uniqueness, public directory, events) already exists from stories 03/06/08 — but the org *create path* is genuinely absent, and the admin gate has no clean seam today. Hidden dependencies (each → failing AC if missed):

| # | Hidden dependency | Where | Fails AC if missed |
|---|---|---|---|
| 1 | **No admin gate exists anywhere in `user-groups`; no shared `isAdmin` helper app-wide.** `GroupService` cannot check `ROLE_ADMIN`; the `user-groups` module has **no Spring Security dependency** (only `user-groups-api` pulls `spring-security-core`). The gate must be a **port** (D1). | `GroupService`; `user-groups/pom.xml`; app-wide: 3 ad-hoc `ROLE_ADMIN` patterns (`AclSecurityConfiguration`, `ProjectOverviewMain`, `finances-infrastructure` `@PreAuthorize`) | **AC3** |
| 2 | **No `createOrg` factory.** `UserGroup.createAdHoc(...)` is the only factory; `GroupDomainService.createAdHocGroup(...)` the only create path. Reusing the ad-hoc factory would create an **OWNER** membership row — violating the strategy's "org groups have NO OWNER row" invariant. | `UserGroup.java`, `GroupDomainService.java` | **AC1 + strategy §3** |
| 3 | **No admin UI surface exists.** Story 02 needs the same surface for membership/manager management. Building this story as a throwaway settings page would force a rework. | `views/groups/*`, `GroupsNavigationComponent`, `SettingsMainLayout` | AC1 (fitness), sequencing |
| 4 | **`NewGroupForm` is hardwired to `createAdHocGroup`** — submit calls it unconditionally. Reused for orgs without a seam → OWNER rows appear. | `NewGroupForm.createGroup(...)` (~L250) | **AC1 (owner row), strategy** |
| 5 | **Duplicate-name uniqueness is global (case-insensitive), not type-scoped.** `findByNameIgnoreCase` + DB unique index `uk_user_group_name` cover all groups. Any "optimisation" to ORG-only breaks AC2 semantics. | `GroupRepository.findByNameIgnoreCase`, `uk_user_group_name` | **AC2** |
| 6 | **"Appears in the group directory" is already implemented** — `listPublicDirectory()` lists ALL active groups incl. `GroupType.ORG`. AC1 needs no directory change; do **not** add an org-filter. | `GroupService.listPublicDirectory`, `GroupInformationServiceImpl` | false-positive scope creep |
| 7 | **`GroupCreated` event already carries `GroupType`** and is serialisable (`@JsonProperty`, used by JobRunr/broadcasting). Reuse; do not add a new event or directive (emails = story 09). | `GroupCreated.java` | consistency |
| 8 | **Org groups store `created_by` = acting admin id even with empty roster.** Traceability (audit, dissolve-cleanup per strategy §4.2) depends on it. | `UserGroup.createOrg` factory | strategy §4.2 trace |

---

## 1. Governance & traceability

Precedent set by the PO for this feature family: `docs/requirements.md` contains **zero** `GROUP-*` entries; implementation proceeds under explicit PO direction, with the verbatim justification carried in the PR description (as in PRs #1574/#1585/#1587). This plan follows suit:

- ✅ May start **without** a merged requirements PR (PO confirmed 2026-09-28).
- ✅ This PR must **not** modify `docs/requirements.md` (PO-owned docs PR, tracked separately).
- ✅ PR must reference requirement IDs **`GROUP-R-01`, `GROUP-NFR-02`** and carry the PO-direction justification block (name the PO decision + date), mirroring #1574.
- ✅ PR uses `Related to #1559` (story stays open); tasks reference **stable story id `FEAT-USER-GROUPS-01`**, never the issue number.
- ✅ `docs/features.md` remains untouched (stories external per repo policy; no in-repo edits).
- **ADR:** do **not** create an ADR inside this PR (AGENTS.md §12). Record D1/D3/D5 in the PR description; ADR 0010 (user-groups bounded context + SID approach) is a follow-up docs PR with explicit approval. `sql/migrations/create-user-groups.sql` already references ADR 0010 as pending — do not touch.

---

## 2. Goal & scope

Implement **org-group creation by QBiC admins** end-to-end (application path + admin gate + admin UI surface), reusing the existing backend substrate. Delivers:

- **AC1** — QBiC admin creates org group with unique case-insensitive name + description → status ACTIVE, appears in the public directory, visible to all by name+description.
- **AC2** — duplicate case-insensitive name rejected with the existing clear message (`DUPLICATE_GROUP_NAME`).
- **AC3** — non-admin denied at the application boundary; no group created, nothing persisted.

**Explicitly OUT of scope (guardrails):**
- ❌ **Org-group membership/manager management** (story 02) — `createOrg` produces an empty roster; no add/remove/appoint/demote for org groups.
- ❌ **Notifications/emails** — `GroupCreated` fires but no directive/policy is added (story 09).
- ❌ **ACL/ACE/SID changes** — no sharing (story 06), no `GROUP_<id>` SID writes, no revocation NFR work (story 08).
- ❌ **Dissolve/rename/description-edit UI** for org groups (story 02 / strategy §4.5).
- ❌ **Admin directory/oversight page** (story 02) — only the create surface here.
- ❌ `docs/requirements.md`, `docs/features.md`, ADR, or `listPublicDirectory` semantic changes.
- ❌ Group-size/count limits (strategy §8), audit story (FEAT-USER-GROUPS-15), admin pre-seeding of project grants.

---

## 3. Design decisions

### D1 — Admin gate as a port (application-layer enforced)
Add narrow authorization port in `user-groups-api` (already depends on `spring-security-core` for types; module itself stays security-free):

```java
public interface GroupAdministrationPermission {
    boolean isAdmin(String userId);
}
```

Implemented in the composition root (`datamanager-app`): `QbicGroupAdministrationPermission` resolves the user's authorities via the existing `UserAuthorityProvider`/`AuthorityService` and checks `ROLE_ADMIN`. Rationale:
- `user-groups` module must stay infrastructure/security-free (current posture: `GroupManagementServiceImpl` docs state the facade "contains no policy of its own").
- AC3 is enforced **at the application boundary** (`GroupService.createOrgGroup`), not by hiding a button — `@PreAuthorize`/method security does **not** protect Vaadin `@Route` views, so the service gate is the only enforceable application-layer option.
- The gate is testable via a mock port without a Spring context; mirrors the existing `UserInformationService` port pattern in `GroupService`.
- Becomes the single seam for all future admin-group surfaces (story 02/10) — no more ad-hoc `ROLE_ADMIN` strings.

Security model detail: the check runs on the **same userId the UI derives** (`AuthenticationToUserIdTranslationService`, same as `createAdHocGroup`'s `currentUserId` seam) — no spoofable discrepancy.

### D2 — Application method
Add to `GroupService`:

```java
public Result<GroupInfoProjection, ApplicationException> createOrgGroup(
        String actingAdminUserId, GroupName name, GroupDescription description)
```

- Guard: `if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) return ACCESS_DENIED` (before any write).
- Reuse the **exact** duplicate flow from `createAdHocGroup`: pre-check `findByNameIgnoreCase` → duplicate error; DB race → `DataIntegrityViolationException` → `duplicateNameError`. **No new error code/message** — existing `ErrorCode.DUPLICATE_GROUP_NAME` and message `"A group with the name '%s' already exists. Group names must be unique (case-insensitive)."` satisfy AC2 verbatim.
- Delegate to `GroupDomainService.createOrgGroup(...)`.
- **No OWNER membership row** created (D4).

### D3 — Domain factory
Add to `UserGroup`:

```java
public static UserGroup createOrg(GroupId id, GroupName name, GroupDescription description,
                                  String createdByUserId, Instant createdAt)
```

- `type=ORG`, `status=ACTIVE`, **empty roster**, `createdBy` = acting admin id (non-blank enforced like `createAdHoc`).
- Matching `GroupDomainService.createOrgGroup(...)`: store + dispatch existing `GroupCreated` with `type=ORG`.

### D4 — Ownership model (no OWNER row)
Per strategy §3 + issue Notes: enforced by the factory **not creating a membership**. `GroupSidProvider`/`listMyGroups` naturally return nothing for an admin who merely created an org group — correct (admins are not members). QBiC admin is owner-equivalent only at the application layer.

### D5 — Admin UI surface in the Groups hub
New `@Route` `AdminGroupCreationMain` at `groups/admin/new`, hosted **inside** `GroupsMainLayout` (top-level Groups nav stays highlighted), registered as an admin-only tab in `GroupsNavigationComponent` shown only for admins. Reuses `NewGroupForm` with the D6 seam. Route gate (`BeforeEnter`) re-checks admin (defense in depth) — the service gate (D1) remains authoritative.

Rejected alternative: a separate Admin Groups section under `SettingsMainLayout` (splits org-group management from the Groups hub where story 02 needs it; strategy §4.5 calls it the "Admin Groups view").

### D6 — `NewGroupForm` reuse (seam, not a fork)
Add org-mode toggle: `boolean createOrg` (default `false`).
- `createOrg=false` (existing My Groups path): unchanged behaviour.
- `createOrg=true`: submit calls `createOrgGroup` instead of `createAdHocGroup`; labels "New Org Group" / "Create an organisational group for recurring teams".
- Name-availability field + duplicate-error display (`markNameError`) identical.
- Preserve the existing package-private test seams (`runNameAvailabilityCheck`, `setValues`); keep binary compatibility via overload/static factories (e.g. `forAdHocGroup(...)` / `forOrgGroup(...)`).

### D7 — Directory: no change
`listPublicDirectory`/`GroupInfo` already surface all active groups (incl. `GroupType`). AC1's directory requirement is satisfied by `createOrgGroup` producing an ACTIVE org group + the existing listing. Regression test locks this.

### D8 — Events: no fan-out for creation
Only the existing `GroupCreated` event (already `type=ORG`-capable). No new directives, no `MemberAccessPolicy` subscription — membership/notification semantics are story 02/09.

---

## 4. File-by-file change list

**Domain (`user-groups/src/main/java/life/qbic/usergroups/`)**
1. `domain/model/UserGroup.java` — add `createOrg(...)` factory (empty roster, ORG, ACTIVE; non-blank creator).
2. `domain/service/GroupDomainService.java` — add `createOrgGroup(...)` (store + dispatch `GroupCreated` with type ORG).

**API (`user-groups-api/src/main/java/life/qbic/usergroups/api/`)**
3. `GroupAdministrationPermission.java` — **new** port: `boolean isAdmin(String userId)`.

**Application (`user-groups/src/main/java/life/qbic/usergroups/application/`)**
4. `GroupService.java` — inject `GroupAdministrationPermission` (constructor param → update `AppConfig` wiring); add `createOrgGroup(...)` with admin guard + duplicate flow (reuse existing private helpers).
5. `GroupInfoProjection.java` — no change (already carries `groupType`).

**Infrastructure (`user-groups-infrastructure/`)**
6. No change (DB + JPA already support `type`).

**Composition root (`datamanager-app/`)**
7. `AppConfig.java` — wire `GroupAdministrationPermission` → `QbicGroupAdministrationPermission`; extend `groupService(...)` bean construction; register new UI beans.
8. `security/QbicGroupAdministrationPermission.java` — **new**: resolve authorities via `UserAuthorityProvider` (`AuthorityService`) + `AuthenticationToUserIdTranslationService`; check `ROLE_ADMIN`.
9. `views/AppRoutes.java` — add `GroupsRoutes.NEW_ORG_GROUP = "groups/admin/new"`.
10. `views/groups/NewGroupForm.java` — add `createOrg` seam; route submit to `createOrgGroup` when set.
11. `views/groups/AdminGroupCreationMain.java` — **new**: `@Route(value=NEW_ORG_GROUP, layout=GroupsMainLayout.class)`; `@PermitAll` + `BeforeEnter` admin check; hosts org-mode `NewGroupForm`; success toast + navigate to `MyGroupsMain`.
12. `views/groups/GroupsNavigationComponent.java` — admin-only "Admin" tab (shown only when `isAdmin`); keep `selectTabFor` mapping for the new route.

**Messages/resources**
13. `datamanager-app/src/main/resources/messages/toast-notifications.properties` — add `user-groups.created.org.success` (e.g. "Created org group &lt;strong&gt;{0}&lt;/strong&gt;."); reuse existing duplicate-name message (no org-specific error key).

---

## 5. Test plan

| Level | Spec/Target | New tests |
|---|---|---|
| Domain | `user-groups/.../domain/model/UserGroupSpec.groovy` | `createOrg`: `type=ORG`, `ACTIVE`, **empty memberships**, blank creator rejected |
| Domain | `.../domain/service/GroupDomainServiceSpec.groovy` | `createOrgGroup` stores + dispatches `GroupCreated` with `type=ORG`; roster empty |
| Application | `.../application/GroupServiceSpec.groovy` | `createOrgGroup` as admin → projection `groupType=ORG`; non-admin → `ACCESS_DENIED`; duplicate (exact + case-insensitive) → `DUPLICATE_GROUP_NAME` + exact message; DB race → same message; `listPublicDirectory` includes org group; **no OWNER membership** (`listMyGroups(admin)` empty); blank admin id → error |
| Application | `.../service/GroupInformationServiceImplSpec.groovy` | org group surfaces via `listPublicDirectory().type == ORG` (regression) |
| UI component | `datamanager-app` `NewGroupForm` component spec (extend) | org mode submits to `createOrgGroup`; duplicate-error inline display unchanged; ad-hoc mode unchanged (regression) |
| Security unit | `QbicGroupAdministrationPermissionSpec` | admin true / non-admin false / unknown user false |
| Integration (`it`) | new `OrgGroupCreationIT` (datamanager-app) | AC1/AC2/AC3 end-to-end: admin creates org → ACTIVE in DB, in `listPublicDirectory` with `type=ORG`, **zero** `group_membership` rows; non-admin → `ACCESS_DENIED`, nothing persisted |

Also: existing `GroupShareAuthorizationTest` must stay green (org groups with no members produce no SIDs — confirms no ACL regression).

---

## 6. Acceptance-criteria mapping

| AC | Implementation | Test |
|---|---|---|
| **AC1** (admin creates org w/ unique ci-name + description → ACTIVE, in directory, visible to all) | `UserGroup.createOrg` + `GroupDomainService.createOrgGroup` + `GroupService.createOrgGroup` + admin-gated `AdminGroupCreationMain`; directory = existing `listPublicDirectory` | `UserGroupSpec`, `GroupServiceSpec`, `OrgGroupCreationIT` |
| **AC2** (duplicate ci-name rejected with clear message) | Reuse global ci pre-check + DB-index race handling + existing `DUPLICATE_GROUP_NAME` message; **global, type-agnostic** | `GroupServiceSpec` (exact + ci + race), `NewGroupForm` component spec inline error |
| **AC3** (non-admin denied, nothing created) | `GroupAdministrationPermission.isAdmin` port enforced in `GroupService.createOrgGroup` **before any write**; UI hides surface + route re-checks | `GroupServiceSpec` non-admin → `ACCESS_DENIED` + nothing persisted; `OrgGroupCreationIT` non-admin flow; permission impl spec |
| **NFR `GROUP-NFR-02`** (referenced in issue) | N/A for create-only (revocation/≤60s propagation is story 06/08 territory) — note in PR description | — |

---

## 7. Sequencing & dependencies

- Lands **alone** (create-only) and **before** story 02 (which builds membership/manager management on the same admin surface + `createOrgGroup` factory substrate).
- Does **not** bundle story 02: its scope includes `admin`-aware listing (e.g. `listMembers` for admins) — keep out.
- No conflict with merged ACL/SID work: org groups with no members produce no SIDs (`GroupAwareSidRetrievalStrategy`, `GroupSidProviderImpl` untouched).
- `QbicGroupAdministrationPermission` becomes the single admin-gate seam for story 08/10 surfaces.

---

## 8. Risks / open questions (resolved 2026-09-28)

| Item | Resolution |
|---|---|
| Admin-gate consistency (port vs `@PreAuthorize`) | **Resolved:** port (D1) — only enforceable application-layer option for Vaadin `@Route` views; keeps `user-groups` security-free |
| Admin UI location | **Resolved:** Groups hub admin tab `groups/admin/new` (story 02 extends it) |
| Admin directory page in story 01? | **Resolved:** no — AC1 satisfied by existing public directory; oversight page deferred to story 02 |
| Multi-instance ACL staleness / ≤60s NFR | Out of scope (no ACL writes in story 01); note in PR description that NFR remains open until story 08 broadcast eviction |
| Duplicate-name wording | Reuse shared message verbatim (single source of truth; org-specific variant if PO requests = one-line change) |
| `created_by` semantics | Org groups store admin user id with empty roster; no ownership row ever created — deletion traceability (strategy §4.2) future-proofed |

---

## 9. PR description requirements

- Reference `Related to #1559` (not Closes); list requirement IDs `GROUP-R-01`, `GROUP-NFR-02`.
- Carry the PO-direction justification block for missing requirements PR (mirror #1574 wording; name PO decision + date 2026-09-28).
- Record D1 (permission port), D3 (org factory), D5 (admin surface) — note ADR deferred to a follow-up docs PR.
- Note NFR `GROUP-NFR-02` (≤60s revocation) explicitly out of scope for create-only.