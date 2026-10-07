# Authorization Glossary (Data Manager)

> **Status:** Draft — open for review (docs-only, no code changes, no requirement changes)
> **Date:** 2026-09-10
>
> Plain-language definitions of the authorization terms introduced and used in the Data Manager
> access-control design. Terms are grouped by the area they belong to. See the companion strategy
> at [`docs/user-groups-strategy.md`](user-groups-strategy.md).

---

## 1. Group domain terms

These are the application's own domain concepts (managed by the application, not by Spring ACL).
Spring Security ACL only ever sees their SID representation (see §2 and §3).

### Group
A named collection of users that a project can be shared with as a single unit, instead of adding
users one by one.
- **Notes:** Stored in application tables (e.g. `user_group`). Two types exist: **org group** and
  **ad-hoc group**. Groups are keyed by a unique (case-insensitive) name for humans, but by a
  stable numeric **id** for everything technical (the SID string, the ACE, the SID row).

### Org group
A group created and managed by QBiC administrators (e.g. an NGS lab).
- **Notes:** Membership is admin-managed, not self-service. Pre-seeded at rollout. The QBiC admin
  acts as the owner-equivalent.

### Ad-hoc group
A self-service group created by a regular user, with no admin involvement.
- **Notes:** The creator becomes **OWNER**; managers can add/remove regular members; an empty
  ad-hoc group auto-dissolves; a manager leaving is offered a guarded transfer-or-dissolve choice.

### GroupMembership
The application-level record linking a user to a group with an internal role.
- **Notes:** One row per user–group pair, unique on `(group_id, username)`. Stored in an
  application table (e.g. `group_membership`), entirely managed by a `GroupService`.

### Group roles (OWNER / MANAGER / MEMBER)
The *internal* role a user holds *within* a group.
- **Notes:** This is **not** a Spring Security authority and **not** a project role. Used to govern
  group-management actions (appoint managers, add/remove members) in the application layer. Org
  groups treat the QBiC admin as owner-equivalent.

---

## 2. Core ACL / SID concepts

These are Spring Security ACL concepts. The Data Manager uses them, sometimes through a subclass
(`QbicPermissionEvaluator`).

### SID (Security Identifier)
The object that Spring ACL matches against an ACE to decide access. An abstract concept —
concrete implementations carry the actual identity (a user, an authority, or a group).
- **Notes:** In this application, a caller can have multiple SIDs (their own, their authorities',
  and their groups'). Access is granted if **any** of the caller's SIDs matches a granting ACE.
- **Related:** `PrincipalSid`, `GrantedAuthoritySid` (groups reuse this as `GrantedAuthoritySid("GROUP_<id>")`, §3).

### PrincipalSid
The SID representing a specific user, built from the user's principal identifier.
- **Notes:** This is today's manual "per-user" grant path. Keyed on the QBiC user id.
- **OIDC:** resolved correctly. `QbicOidcUser.getName()` is overridden to return the QBiC user id
  (see `QbicOidcUser.java`, "needed for ACL permission checks"), so `authentication.getName()` — and
  therefore the `PrincipalSid` built from it — matches the QBiC user id for both local and OIDC
  logins.

### GrantedAuthoritySid
The SID representing a Spring Security authority/role (e.g. `ROLE_ADMIN`).
- **Notes:** This is today's "per-authority" grant path, used to hardcode `ROLE_ADMIN` /
  `ROLE_PROJECT_MANAGER` grants on new projects. **Groups reuse this type** as
  `GrantedAuthoritySid("GROUP_<id>")` — the recommended approach (§3) — keeping Spring ACL's
  persistence/lookup unchanged. The `GROUP_` prefix is the only thing that distinguishes a group
  from a real role, so `AuthorityService` must never emit a `GROUP_...` authority.

### SidRetrievalStrategy
The Spring Security component that maps an `Authentication` to the caller's set of `Sid`s at
check time.
- **Notes:** The Data Manager replaces it with `GroupAwareSidRetrievalStrategy` (§3) so group
  membership is included. The membership lookup is cached keyed by **user id**; the cache entries
  must be **evicted whenever a user's group assignment changes** so grant/revoke stay effective at
  the next permission check (live rollout).

### AclPermissionEvaluator
Spring Security's `PermissionEvaluator` that turns `hasPermission(...)` into an ACL lookup.
- **Notes:** The Data Manager subclasses it as `QbicPermissionEvaluator` (which only special-cases
  empty `Optional` targets and otherwise delegates).

### QbicPermissionEvaluator
The Data Manager's `AclPermissionEvaluator` subclass, wired as the bean used by method security
and UI permission gating.
- **Notes:** All `@PreAuthorize("hasPermission(...)")` and `UserPermissionsImpl` checks funnel
  through it, so the custom SID retrieval it delegates to is applied everywhere consistently.

---

## 3. Group SIDs — recommended path and alternative

Groups are surfaced to Spring ACL as SIDs. The **recommended** approach reuses
`GrantedAuthoritySid("GROUP_<id>")` (one customization). A dedicated custom `GroupSid` is an
alternative that requires four coordinated customizations.

### Recommended path — `GrantedAuthoritySid("GROUP_<id>")`

Groups ride as a Spring authority-SID with a reserved `GROUP_` prefix and `principal = false`.
Because `GrantedAuthoritySid` is natively understood by Spring ACL, the write path
(`JdbcMutableAclService`) and read path (`BasicLookupStrategy`) need **no** customization. The only
ACL extension is a check-time `SidRetrievalStrategy` that adds the caller's group SIDs.

### GroupAwareSidRetrievalStrategy (check-time)
A `SidRetrievalStrategy` that adds the caller's group SIDs to their SID set.
- **Notes:** Returns the caller's default SIDs (`PrincipalSid` + a `GrantedAuthoritySid` per
  authority) plus a `GrantedAuthoritySid("GROUP_<id>")` per live group membership, resolved via a
  `user-groups-api` facade. This is the **only** Spring ACL extension point in the recommended
  approach. It is wired onto `QbicPermissionEvaluator` via `AclPermissionEvaluator.setSidRetrievalStrategy(...)`.
  Because it derives group SIDs from the database on every check, grant/revoke are effective at the
  next permission check (live rollout).

### Alternative — dedicated custom `GroupSid`

If groups must be kept structurally distinct from authorities, a custom `GroupSid` can be used
instead. Because Spring ACL persistence/lookup only understand `PrincipalSid` and
`GrantedAuthoritySid` by default, this requires **four coordinated customizations**:

### GroupSid
A custom `Sid` whose string identity is `"GROUP_<groupId>"` and whose `equals`/`hashCode` are
based on the group id.
- **Notes:** The *id* is used as the SID string (not the display name) so ACEs survive group
  renames. The `principal` flag is `false` (stored as a non-principal SID). This makes the `GROUP_`
  prefix collision guard unnecessary (the type distinguishes groups structurally).

### GroupAwareJdbcMutableAclService (write path)
A `JdbcMutableAclService` subclass that knows how to *persist* a `GroupSid`.
- **Notes:** Overrides `createOrRetrieveSidPrimaryKey(Sid, boolean)` to store a `GroupSid` as a
  non-principal SID string. Without it, the default throws `IllegalArgumentException: Unsupported
  implementation of Sid`. Must preserve the existing MySQL `SELECT @@IDENTITY` identity queries.

### GroupAwareLookupStrategy (read path)
A custom `LookupStrategy` implementation that *reconstructs* a `GroupSid` when reading it back
from storage.
- **Notes:** Must be a **custom `LookupStrategy` implementation**, not a `BasicLookupStrategy`
  subclass — `BasicLookupStrategy` does not *support* subclassing (its class Javadoc warns the
  class "is likely to change in future releases") and, decisively, its `readAclsById` method is
  `final`, so a subclass cannot override the read path; SID reconstruction (`protected createSid`)
  is thus not reachable for group types through `BasicLookupStrategy`. Without it, a
  stored group SID is read back as a `GrantedAuthoritySid`, which never `.equals()` a `GroupSid`
  → access silently denied. Resolves group SIDs lazily so groups created after startup are
  recognised. (The official ACL reference notes `AclService` delegates retrieval to a
  `LookupStrategy` and supports custom implementations.)

### GroupAwareSidRetrievalStrategy (check-time, custom-Sid variant)
As in the recommended path, but producing `GroupSid`s instead of `GrantedAuthoritySid`s.

---

## 4. ACL data model terms

The persistence layer Spring ACL uses to store object permissions.

### ACL (Access Control List)
The per-object record of who may do what. Each protected object has one ACL.
- **Notes:** In this application ACLs exist only on `Project` objects; groups themselves get no
  ACL (group management is enforced in the application layer).

### ObjectIdentity
The identity of a protected domain object within the ACL store.
- **Notes:** Composed of the class (`life.qbic.projectmanagement.domain.model.project.Project`)
  and the object's id. The ACL cache is keyed on this.

### ACE (Access Control Entry)
A single row within an ACL: one SID, one permission, and a granting flag.
- **Notes:** One group grant = one or more ACEs pairing `GrantedAuthoritySid("GROUP_<id>")` with a
  project permission.

### Permission (READ / WRITE / ADMIN / OWNER)
The project-level rights mapped to Spring `BasePermission`s (READ, WRITE, ADMINISTRATION, CREATE,
DELETE).
- **Notes:** Groups can be granted READ / WRITE / ADMIN — **never OWNER** (a group grant caps at
  ADMIN).

### acl_sid
The table storing distinct SIDs (a `sid` string + a `principal` flag).
- **Notes:** Unique on `(sid, principal)`. The `GROUP_<id>` prefix avoids collisions with `ROLE_*`
  and user ids. Rows are auto-created on ACE insert.

### acl_entry
The table storing ACEs (permission, granting flag, SID, ACL object).
- **Notes:** This is where a group grant is physically written.

### acl_object_identity / acl_class
Tables storing the protected objects and their Java class names, respectively.
- **Notes:** `acl_class` carries one row for `life.qbic.projectmanagement.domain.model.project.Project`.

### principal flag
The boolean on a SID that distinguishes a *user* (`true`) from a *non-user* (`false`, e.g. roles
and groups).
- **Notes:** Used by `ProjectAccessService.listCollaborators()` to filter out non-user SIDs when
  showing the "People" list.

---

## 5. Related behavioral terms

### Live rollout
The property that a membership change takes effect on the **next** permission check, with no login
or session round-trip.
- **Notes:** Achieved because `GroupAwareSidRetrievalStrategy` reads membership from the database
  on every check. Supersedes the login-scoped authority-injection alternative.

### Effective access
The *resolved* access a user has to a project, considering direct ACEs, group ACEs, and role
grants together.
- **Notes:** A query for this is needed for notification de-duplication and member-count display.

---

## References

- [`docs/user-groups-strategy.md`](user-groups-strategy.md) — the strategy this glossary supports.
- Spring Security reference: *Access Control List* (ACL) documentation for `Sid`,
  `PrincipalSid`, `GrantedAuthoritySid`, `SidRetrievalStrategy`, `AclPermissionEvaluator`.