# Strategy — User Groups for Project Sharing

> **Status:** Draft — open for review (docs-only PR, no code changes, no requirement changes)
> **Date:** 2026-09-07
> **Scope:** Investigation of the current user & access management capabilities and a proposed
> strategy to introduce *user groups* so projects can be shared with a group of people instead of
> assigning them manually, one by one.
>
> **Companion:** a plain-language glossary of the authorization terms introduced here lives in
> [`docs/auth-glossary.md`](auth-glossary.md).

---

## 1. Motivation

Today, sharing a project means adding individual users (Project Access Management → "Add people").
This becomes tedious and error-prone as soon as recurring teams are involved:

- QBiC internal labs (e.g. NGS labs) frequently need quick access to projects — for assistance or
  review — without becoming project owners.
- Ad-hoc collaboration teams (e.g. "everyone working on project X") must be re-assembled manually
  per project and re-assembled again whenever the team changes.

This document investigates the existing user & access management capabilities and proposes how to
extend them with **user groups** that can be shared onto projects like a single user.

**Guiding decisions (aligned with the product owner):**
- Two group types: **org groups** (admin-managed, e.g. NGS labs) and **ad-hoc groups** (self-service
  user feature, no admin involvement).
- Groups have an internal role structure (owner / manager / member).
- Anyone with project access-administration rights can share a project with a group.
- No special handling for "sensitive" projects; transparency is achieved through notifications.
- Members who *newly* gain access through a group grant are informed by email (deduplicated).

---

## 2. Current State Analysis

### 2.1 User management (identity context)

- The `identity` bounded context owns the user aggregate
  (`identity/src/main/java/life/qbic/identity/domain/model/User.java`): user id, full name, user
  name, email, password, OIDC entries. **No roles and no groups live on the aggregate.**
- **System roles are an authorization concept in `project-management`**, not in `identity`:
  - `roles`, `user_role`, `role_permission` tables (`sql/insert-default-values.sql` seeds
    `ADMIN`, `USER`, `PROJECT_MANAGER`).
  - `Role` implements `GrantedAuthority`; `getAuthority()` returns `ROLE_<name>`.
  - Membership is resolved per user at login by
    `AuthorityService.getAuthoritiesByUserId()` (in
    `project-management/.../application/authorization/authorities/AuthorityService.java`) and
    injected into `QbicUserDetails` — used by both local login (`UserDetailsServiceImpl`) and OIDC
    login (`OidcUserDetailsService`).
- `identity-api` exposes `UserInformationService` as the cross-context facade (find by email/id,
  query active users, …).

### 2.2 Project access (Spring Security ACL)

- Every project is an ACL object (`acl_class` entry for
  `life.qbic.projectmanagement.domain.model.project.Project`, string id). Access is expressed as
  Access Control Entries (ACEs) in `acl_entry`, with SIDs in `acl_sid` (unique on
  `(sid, principal)` — users vs. roles are distinguished by the `principal` flag).
- `ProjectAccessService` / `ProjectAccessServiceImpl`
  (`project-management/.../application/authorization/acl/`) offers **two parallel grant paths**:
  - **Per-user** (`PrincipalSid`): `addCollaborator`, `removeCollaborator`, `changeRole` — this is
    today's manual assignment.
  - **Per-authority** (`GrantedAuthoritySid`): `addAuthorityAccess`, `removeAuthorityAccess`,
    `changeAuthorityAccess` — currently only used to hardcode
    `ROLE_ADMIN` + `ROLE_PROJECT_MANAGER` = ADMIN grants on every newly created project
    (`project-management-infrastructure/.../project/ProjectRepositoryImpl.java`).
- Project roles: `READ / WRITE / ADMIN / OWNER`, mapped to Spring `BasePermission`s
  (READ, WRITE, ADMINISTRATION, CREATE, DELETE).
- **Project listing already merges per-user and per-authority access:**
  `ProjectInformationService.retrieveAccessibleProjectIdsForUser()` queries the ACL for the user's
  own SID *and* for every authority in `authentication.getAuthorities()`.
- UI: the "Project Access Management" page (`ProjectAccessComponent`) lists only users
  (`listCollaborators()` filters to `PrincipalSid`) and supports add/remove/role-edit. Its Javadoc
  already mentions "users and user groups" — groups were envisioned but never implemented.
- Notifications: `addCollaborator` fires the `ProjectAccessGranted` domain event → email
  (`InformUserAboutGrantedAccess`). Authority grants fire no events.
- The SQL view `project_userinfo` (used by `project_overview`, shown on project cards) resolves
  **principal SIDs only** — group grants would be invisible there and need to be reworked.

### 2.3 Key insight

The ACL layer is already group-ready:

1. `GrantedAuthoritySid`-based grants exist and are exercised (the hardcoded role grants).
2. Project listing already resolves access via the user's authorities.
3. `@PreAuthorize("hasPermission(...)")` evaluation (`QbicPermissionEvaluator`) works generically
   for any authority present in the authentication.

The missing pieces are: a **group concept** (aggregate + membership + role hierarchy), a **way to
surface group SIDs in ACL evaluation** — `GrantedAuthoritySid("GROUP_<id>")` derived live at
evaluation time via a custom `SidRetrievalStrategy` on the permission evaluator (see §4.3), plus
the companion project-listing change — and the **UI surface** for group management and group-based
sharing. Everything else builds on proven paths.

---

## 3. Aligned Product Decisions

| Aspect | Decision |
|---|---|
| Group types | **Org groups** (admin-created and pre-seeded, e.g. NGS labs; membership admin-managed) and **ad-hoc groups** (self-service user feature; no admins involved) |
| Internal group roles | **OWNER** (only the ad-hoc creator; appoints/removes managers), **MANAGER** (adds/removes regular members; org groups: rename/describe), **MEMBER**. For org groups the QBiC admin acts as owner-equivalent |
| Governance | Org groups: admins manage membership. Ad-hoc groups: fully self-service — creator becomes owner; manager can add/remove members; members can self-remove; an empty ad-hoc group is auto-dissolved; a manager leaving is offered an explicit, guarded *transfer-or-dissolve* choice |
| Sharing | Anyone with project *change-access* can share a group onto a project. Grant level: READ / WRITE / ADMIN — **never OWNER** |
| Visibility | Group names, descriptions, and memberships are visible to all users. Group names are **unique** (case-insensitive). As part of the DPA, users are informed beforehand that their ORCID, username, and Full Name are visible to other users |
| Sensitive data | No project flag; transparency via notifications (see §5.3) |
| Notifications | Email only members who **newly gain** access (deduplicated); digest on joining a group with existing grants; revocation email on removal; dissolution email listing affected projects; project admins informed about membership changes on groups shared with their projects; role-level changes are audit-log-only |
| Project cards | Show group *names* on project cards (compact view; the card area is shared real estate) |

---

## 4. Proposed Architecture

### 4.1 Bounded context placement — *needs ADR + module approval*

Recommendation: a **new bounded context `user-groups`** (domain + api + infrastructure modules),
mirroring the `identity` module layout. Rationale:

- Groups are a user-domain concept; keeping `identity` as pure accounts is cleaner.
- Avoids coupling group management into `project-management`'s authorization package.
- Provides a clean `user-groups-api` facade consumed by `AuthorityService` (project-management)
  and the UI (datamanager-app), following the established `identity-api` pattern.

This adds Maven modules → **human approval required per AGENTS.md §12**, and the decision
merits an ADR.

### 4.2 Conceptual data model

- `user_group(id, name unique-ci, description, type [ORG | ADHOC], status, created_by, created_at)`
- `group_membership(group_id, user_id, role [OWNER | MANAGER | MEMBER], joined_at)`
- ACL SID representation: a **`GrantedAuthoritySid("GROUP_" + groupId)`**, stored with
  `principal = false`. The prefix avoids collisions with `ROLE_*` and user ids under the `acl_sid`
  unique key, and keying on the stable group *id* (rather than the display name) keeps ACEs valid
  across group renames. The human-readable group *name* is kept separately for display purposes only
  — it is never used as the SID string. `JdbcMutableAclService` auto-creates sid rows on ACE insert,
  so no special sid seeding is needed.
  *Note:* an alternative is a dedicated custom `GroupSid` (§4.3 Alternative); the recommended
  `GrantedAuthoritySid` representation keeps Spring ACL's persistence/lookup unchanged.
- **No ACL objects for groups themselves** — group *management* permissions (owner/manager actions,
  admin oversight) are enforced in the application layer via membership roles + `ROLE_ADMIN`
  checks. Spring ACL remains exclusively on `Project` objects.

### 4.3 Authorization plumbing

Groups are represented in the ACL by **`GrantedAuthoritySid("GROUP_<id>")`**, i.e. they ride as a
Spring authority-SID with a reserved `GROUP_` prefix. This is the **recommended** approach because
it keeps Spring Security ACL's persistence and lookup completely unchanged — only the check-time SID
derivation is customized. A dedicated custom `GroupSid` is documented as an alternative at the end
of this section.

**Recommended approach — `GrantedAuthoritySid` + a single `SidRetrievalStrategy`**

1. **Check-time retrieval — `GroupAwareSidRetrievalStrategy`** (implements `SidRetrievalStrategy`,
   `datamanager-app/.../security/GroupAwareSidRetrievalStrategy.java`): returns the caller's
   default SIDs (`PrincipalSid` + a `GrantedAuthoritySid` per authority) **plus** one
   `GrantedAuthoritySid("GROUP_<id>")` per live group membership, resolved through a
   `user-groups-api` facade (e.g. `listGroupSidsForUser(userId)`). This is the *only* Spring ACL
   extension point required. `SidRetrievalStrategy` is an interface designed for this; `AclPermissionEvaluator`
   exposes `setSidRetrievalStrategy(...)`.

   Wiring in `AclSecurityConfiguration`: add a `GroupAwareSidRetrievalStrategy` bean and call
   `setSidRetrievalStrategy(...)` on the `QbicPermissionEvaluator` bean. The `LookupStrategy`,
   `MutableAclService`, and `MethodSecurityExpressionHandler` beans stay **unchanged** (plain
   `BasicLookupStrategy` + `JdbcMutableAclService`).

2. **Sharing (write path):** reuse the existing `addAuthorityAccess(projectId, "GROUP_<id>", role)`
   (`ProjectAccessServiceImpl`), which already accepts an authority string and builds a
   `GrantedAuthoritySid`. Add validation blocking `ProjectRole.OWNER` (groups cap at ADMIN).
   `removeAuthorityAccess` / `changeAuthorityAccess` cover revoke and role change with no change.

3. **Project listing — companion change (required):**
   `ProjectInformationService.retrieveAccessibleProjectIdsForUser()` already unions
   `authentication.getAuthorities()` into the accessible-project set, but groups are *not* in the
   `Authentication` (they are derived live, not injected at login). It must additionally union the
   user's group SIDs via the same `user-groups-api` facade — otherwise group-granted projects pass
   `hasPermission(READ)` but never appear in the project overview.

4. **Listing / UI:** extend `listCollaborators()` (or add `listSharedGroups()`) to surface
   `GrantedAuthoritySid`s whose authority starts with `GROUP_` (group name, description, member
   count, member list). `listCollaborators()` currently filters to `PrincipalSid`, so a group
   branch must be added.

5. **Effective-access query (new):** needed for notification dedupe and member-count display —
   "has member X effective access (direct ACE, via group, or via role) to project P?" Implemented
   as a batch query against `acl_entry` / `acl_sid` for the member's sid set (including the
   member's `GROUP_<id>` authorities).

**Check-time behaviour (live rollout):** because `GroupAwareSidRetrievalStrategy` derives the
caller's group SIDs from the database on every `hasPermission(...)` call, grant **and** revocation
are effective at the next permission check — for `@PreAuthorize("hasPermission(...)")` (method
security) and `UserPermissionsImpl` (UI gating) alike. No session machinery, no authority cache to
invalidate; the Spring ACL cache stays valid because it is keyed by `ObjectIdentity`, while
`isGranted` re-evaluates the freshly derived SIDs per call.

- *Freshness / concurrency:* membership is read through a `@Cacheable` facade; every
  `addMember`/`removeMember` must evict the affected user's entries so membership changes are seen
  on the next check. Group SIDs are resolved lazily (not at bean construction), so groups created
  after startup are still recognised.
- *Constraint:* group-aware checks must route through `hasPermission(...)` / the listing helper;
  `hasRole` and view-level authority checks do not see groups (groups are not in the
  `Authentication`).
- *Key caveat:* the `"GROUP_<id>"` string must match exactly between (a) the persisted ACE SID and
  (b) what the retrieval strategy produces for a member — a mismatch silently denies access. Using
  the stable group *id* (not the display name) as the SID string avoids breakage on renames.
- *Collision guard:* since groups ride as `GrantedAuthoritySid`s, the only thing separating them
  from real roles is the `GROUP_` prefix **convention** (no type distinguishes them). `AuthorityService`
  must never emit a `GROUP_...` authority, and no role should be named with the prefix.

**Alternative — dedicated custom `GroupSid`**

If groups must be kept structurally distinct from authorities, a custom `GroupSid implements Sid`
can be used instead. Spring ACL persistence/lookup only understand `PrincipalSid` and
`GrantedAuthoritySid` by default, so this requires **four coordinated customizations**:

a. **`GroupSid`** — a custom `Sid` with `equals`/`hashCode` on the group id and a `toString`
   returning `"GROUP_<id>"`.
b. **Write path — `GroupAwareJdbcMutableAclService`** (extends `JdbcMutableAclService`): overrides
   `createOrRetrieveSidPrimaryKey(Sid, boolean)` to persist a `GroupSid` as a non-principal SID.
   Without it, the default throws `IllegalArgumentException: Unsupported implementation of Sid`.
c. **Read path — custom `LookupStrategy` implementation** (not a `BasicLookupStrategy` subclass):
   `BasicLookupStrategy` explicitly *does not support subclassing* (class Javadoc) and its SID
   reconstruction (`createSid(boolean, String)`) is a `protected` detail outside the `LookupStrategy`
   interface. A custom `LookupStrategy` must run the ACL/ACE SQL and reconstruct each stored SID
   string as a `GroupSid` (resolved lazily) instead of a `GrantedAuthoritySid`; the official ACL
   reference notes `AclService` delegates retrieval to a `LookupStrategy` and supports custom
   implementations. Without this, a stored group SID is read back as a `GrantedAuthoritySid`, which
   never `.equals()` a `GroupSid`, so access is always denied.
d. **Check-time retrieval — `GroupAwareSidRetrievalStrategy`** — as in the recommended approach, but
   producing `GroupSid`s instead of `GrantedAuthoritySid`s.

The custom `GroupSid` buys a *structural* type distinction and makes the `GROUP_` prefix collision
guard unnecessary, but it requires re-implementing the read-path lookup (owning the ACL/ACE SQL and
assembly) — a real cost and risk. **Trade-off:** the `GrantedAuthoritySid` route is materially
simpler and uses fully supported Spring ACL extension points; it only loses the strict conceptual
separation of groups from roles.

### 4.4 Notifications (event → policy → directive)

Follow the existing pattern (`ProjectAccessGranted` → `ProjectAccessGrantedPolicy` →
`InformUserAboutGrantedAccess`, JobRunr-scheduled email). New events, e.g.:

- `GroupSharedWithProject` — email every member who *newly* gains effective access (dedupe computed
  via the effective-access query *before* the ACE is written).
- `GroupMembershipChanged` — digest to the new member ("you can now access: X, Y, Z"); notify
  project admins of affected projects.
- `MemberRemovedFromGroup` — revocation email to the removed user; in-app notice to group managers.
- `GroupDissolved` — email former members + admins of affected projects, listing the projects.

### 4.5 UI surface

1. **Project Access page** (`ProjectAccessComponent`) — two sections: **People** (current grid,
   unchanged) and **Groups** (grid of shared groups + grant role; edit/remove; member list visible
   to all users).
2. **Share dialog** (`AddCollaboratorToProjectDialog`) — "Add people or groups": a group tab with a
   searchable dropdown over public group names + role selection, same flow as adding a person.
3. **My Groups view** (user-facing, in the account area) — groups I belong to with my role;
   manager controls for ad-hoc groups (add/remove members, rename, dissolve with confirmation and
   transfer-or-dissolve guard).
4. **Admin Groups view** (QBiC admins) — org group CRUD: create, assign managers, manage
   membership, rename, dissolve; full directory of all groups; read-only oversight of ad-hoc
   groups.
5. **Project cards** (overview) — show group names the project is shared with. Rebuild the
   `project_userinfo` / `project_overview` SQL views accordingly.

### 4.6 Revocation semantics

Because `GroupAwareSidRetrievalStrategy` derives the caller's group SIDs (`GrantedAuthoritySid("GROUP_<id>")`) on every permission check (§4.3), revocation is effective at the **next `hasPermission(...)` / listing evaluation** — with no session invalidation or per-request freshness filter. This supersedes the login-scoped authority-injection alternative (which would inject group SIDs into the `Authentication` at login, deferring changes to next login / session expiry and requiring session-invalidation or per-request-authority machinery).

Caching the membership lookup is **optional and up to the implementation** (e.g. keyed by user id,
as in the glossary). If caching is added, care must be taken that group-membership changes do not
lead to **stale cache entries** — i.e. every `addMember`/`removeMember` must evict the affected
user's entries so the change is reflected at the next check.

---

## 5. Security Analysis

### 5.1 Leakage

- As part of the DPA, users are informed beforehand that their ORCID, username, and Full Name are
  visible to other users. Group names, descriptions, and memberships are therefore not restricted.
- The share dialog context already requires project `change-access`; `listCollaborators` requires
  ADMINISTRATION — who-has-access stays privileged.

### 5.2 Blast radius

Org groups are a force multiplier: one membership mistake silently grants/revokes access on every
project shared with the group. Mitigations baked into the design:

1. Instant revocation — effective at the next permission check (§4.3, §4.6) — plus loud emails on
   removal.
2. Project admins notified on membership changes of shared groups.
3. Groups can never be OWNER; a group grant caps at ADMIN.
4. Dissolution shows affected projects before confirming; ACEs cleaned up.
5. Audit events for every group operation (existing event → policy → directive pattern).
6. Disabled users lose access at the login gate (no extra work).

### 5.3 Sensitive-data posture (decision a)

No flag/restriction on sensitive projects; transparency through the notification profile above.

---

## 6. Governance Next Steps (per AGENTS.md)

1. **Requirements first:** this is new system capability → new requirement entries in
   `docs/requirements.md` (proposed new domain, e.g. `GROUP-*` or `ACCESS-*`), as a **dedicated PR
   with human approval** — never bundled with implementation.
2. **ADR(s):** bounded-context placement + `GrantedAuthoritySid`-based grant approach — ADR
   creation requires human confirmation (MADR template at `docs/adr/templates/`).
3. **Feature / Stories / Tasks:** `FEAT-<SLUG>` feature first, then stories with stable IDs
   recorded in `docs/features.md` (draft → approved lifecycle), then tasks.
4. **Schema migrations** under `docs/migrations/` + `sql/` per existing convention.

---

## 7. Suggested Implementation Phases

| Phase | Scope |
|---|---|
| **P0 — Governance** | Requirements PR, ADR(s), Feature + Stories |
| **P1 — Domain** | `user-groups` module: aggregate, membership, role hierarchy & policies (auto-dissolve, transfer-or-dissolve guard, unique names), JPA + migrations |
| **P2 — Authorization** | `GroupAwareSidRetrievalStrategy` (adds `GrantedAuthoritySid("GROUP_<id>")` at check time), share/list groups via ACL, effective-access query, OWNER block |
| **P3 — Notifications** | Events + policies + email directives with dedupe/digests |
| **P4 — UI** | My Groups, Admin Groups, access-page Groups section, share-dialog group tab, project-card view rebuild |
| **P5 — Ops** | Org-group seeding, Spock specs + integration tests, audit logging, docs |

---

## 8. Open Items (no decision needed yet)

- Org-group "owner" semantics: admin acts as owner-equivalent (recommended).
- Group size limits / group count limits per user.
- Manager demotion path (owner action only).
- Whether org groups pre-seed with any project grants at rollout.
- Verify the `PrincipalSid` derivation for OIDC logins: `SidRetrievalStrategyImpl` builds the
  user's principal SID from `authentication.getName()` (the OIDC user's name), while ACLs are
  keyed on the QBiC user id — confirm OIDC users actually match on the QBiC user id today before
  groups build on top.

---

## 9. References

- `identity/src/main/java/life/qbic/identity/domain/model/User.java`
- `identity-api/src/main/java/life/qbic/identity/api/UserInformationService.java`
- `project-management/.../application/authorization/acl/ProjectAccessService.java` (and `Impl`)
- `project-management/.../application/authorization/authorities/AuthorityService.java`
- `project-management/.../application/ProjectInformationService.java` (`retrieveAccessibleProjectIdsForUser`)
- `project-management-infrastructure/.../project/ProjectRepositoryImpl.java`
- `datamanager-app/.../security/AclSecurityConfiguration.java`, `SecurityConfiguration.java`,
  `UserPermissionsImpl.java`
- `datamanager-app/.../security/GroupAwareSidRetrievalStrategy.java` (proposed; recommended path)
- `project-management/.../application/authorization/acl/QbicPermissionEvaluator.java`
- `datamanager-app/.../views/projects/project/access/ProjectAccessComponent.java`, `AddCollaboratorToProjectDialog.java`
- `project-management/.../application/policy/directive/InformUserAboutGrantedAccess.java`
- `sql/complete-schema.sql` (ACL tables, `project_userinfo` view)