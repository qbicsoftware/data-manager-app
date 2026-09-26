# FEAT-USER-GROUPS — User Stories (Draft, pending PO approval)

> **Status:** 🔴 Draft — stories are **not** yet approved; they are intentionally **not** hosted on
> GitHub (user stories are hosted externally per current policy). They will be recorded in
> `docs/features.md` only once approved and assigned final stable IDs.
>
> **Companion:** [docs/user-groups-strategy.md](../user-groups-strategy.md) and
> [docs/auth-glossary.md](../auth-glossary.md).
>
> **Traceability:** every story traces to proposed requirement IDs in the new `GROUP-*` domain
> (`docs/requirements.md`); these requirements are **draft proposals only** and require a
> human-approved requirements PR (`AGENTS.md` §12) before implementation. See the
> `docs/user-groups-strategy.md` §6 governance steps.

---

## EPIC 1 — Org Groups (admin-managed; QBiC admin acts as owner-equivalent)

**Definition of done for this epic:** A QBiC administrator can create org groups, assign managers,
and manage membership; org groups have no OWNER row and governance is app-level via `ROLE_ADMIN`.

---

### FEAT-USER-GROUPS-01 — Create Org Groups (Admin)

**User Story**

> As a QBiC administrator, I want to create organisational groups with a unique name and
> description, so that recurring teams (e.g. NGS labs) can be shared onto projects as one unit
> instead of re-adding members individually.

**Acceptance Criteria**

- Given I am logged in as a QBiC admin, When I create an org group with a unique case-insensitive
  name and a description, Then the group is created in status active, appears in the group
  directory, and is visible to all users by name and description.
- Given I create an org group with a duplicate name (case-insensitive), When I submit, Then the
  creation is rejected with a clear message that the name is not unique.
- Given I attempt to create an org group without being a QBiC admin, When I submit, Then the
  operation is denied and no group is created.

**Requirement IDs:** `GROUP-R-01`, `GROUP-NFR-02`

**Notes & Context**

- Org groups have **no OWNER membership row**; the QBiC admin acts as owner-equivalent at the
  application layer (system role `ROLE_ADMIN`). See strategy §3, §8.

---

### FEAT-USER-GROUPS-02 — Manage Org Group Membership and Managers (Admin)

**User Story**

> As a QBiC administrator, I want to assign managers and manage the membership of org groups, so
> that the right people represent and maintain each lab/team group.

**Acceptance Criteria**

- Given an org group exists, When I assign a user as a manager, Then that user gets the MANAGER
  role and can add/remove regular members and rename/describe the group.
- Given an org group exists with members, When I add or remove a member, Then the membership list
  updates, newly granted members receive the newly-gained-access email (deduplicated), and project
  owners/admins of projects the group is shared with are informed of the membership change.
- Given I remove a member from an org group, When the change takes effect, Then the member loses
  group-derived project access at the next authorization check (not later than the 60s revocation
  NFR).
- Given an org group has no remaining managers, When I review it, Then the QBiC admin remains the
  owner-equivalent — the group stays governed and no OWNER membership is created.

**Requirement IDs:** `GROUP-R-01`, `GROUP-R-03`, `GROUP-R-10`, `GROUP-NFR-01`

**Notes & Context**

- Manager demotion path is an open item (§8); this story covers assignment and the no-manager
  fallback only.

---

## EPIC 2 — Ad-hoc Groups (self-service)

**Definition of done:** A regular user can create ad-hoc groups without admin involvement, manage
membership and profile as owner/manager, and leave safely (transfer-or-dissolve guard). Empty
ad-hoc groups auto-dissolve.

---

### FEAT-USER-GROUPS-03 — Create Ad-hoc Groups Self-Service

**User Story**

> As a researcher, I want to create an ad-hoc group myself and become its owner, so that I can
> assemble a collaboration team without involving an administrator.

**Acceptance Criteria**

- Given I am a regular authenticated user, When I create an ad-hoc group with a unique name and a
  description, Then the group is created, I become its OWNER, and it appears in the public
  directory and in my groups.
- Given I create an ad-hoc group with a duplicate name (case-insensitive), When I submit, Then the
  creation is rejected with a clear uniqueness message.
- Given an ad-hoc group I own becomes empty (all members including myself removed), When the last
  membership is removed, Then the group is auto-dissolved and removed from the directory.

**Requirement IDs:** `GROUP-R-02`, `GROUP-NFR-02`

---

### FEAT-USER-GROUPS-04 — Manage Ad-hoc Group Membership and Profile (owner/manager)

**User Story**

> As a group owner or manager, I want to add or remove members, rename the group, and change its
> description, so that the group stays accurate as team membership changes.

**Acceptance Criteria**

- Given I own an ad-hoc group, When I appoint a manager, Then the appointed user gets the MANAGER
  role and can add/remove members and rename/describe the group.
- Given I am a manager (or owner), When I add or remove a regular member, Then the membership
  updates and newly granted members receive the deduplicated newly-gained-access email (only those
  who newly gain effective access).
- Given I am a member but not a manager/owner, When I attempt to add, remove, rename or describe
  the group, Then the operation is denied.
- Given I am a regular member, When I remove myself from a group, Then I leave the group and, if I
  was the last member, the group auto-dissolves.

**Requirement IDs:** `GROUP-R-03`, `GROUP-R-10`

---

### FEAT-USER-GROUPS-05 — Transfer or Dissolve When a Manager Leaves

**User Story**

> As a manager of an ad-hoc group, when I leave or am removed such that the group would be left
> without governance, I want a guarded transfer-or-dissolve choice, so that an unmanaged group
> never lingers.

**Acceptance Criteria**

- Given I am the only manager of an ad-hoc group and attempt to leave it, When I confirm leaving,
  Then I am offered an explicit, guarded choice: transfer ownership/managership to another member
  or dissolve the group, and the group is not left without governance.
- Given I choose to dissolve, When I confirm dissolution (with affected projects shown), Then the
  group is dissolved, its memberships removed, all its ACEs on projects removed (no orphaned
  grants), and former members plus project owners/admins of affected projects receive the
  dissolution email listing affected projects.
- Given I choose to transfer, When I confirm, Then the new owner/manager receives the role and the
  group remains active with its grants intact.

**Requirement IDs:** `GROUP-R-04`, `GROUP-R-10`, `GROUP-NFR-03`

---

## EPIC 3 — Sharing groups onto projects & effective access

**Definition of done:** Project access-administration holders can share a group onto a project at
READ/WRITE/ADMIN (never OWNER); members gain effective access at the next authorization check;
effective access is the maximal of direct grants, group grants, and system roles.

---

### FEAT-USER-GROUPS-06 — Share a Group onto a Project

**User Story**

> As a project collaborator with access administration, I want to share a group onto my project at
> READ/WRITE/ADMIN, so that I can grant the whole team access in one step instead of adding each
> member individually.

**Acceptance Criteria**

- Given I have project ADMIN on a project, When I open the share dialog and select a group with a
  role (READ/WRITE/ADMIN), Then the group grant is created (SID `GROUP_<id>`), every member of the
  group gains the granted project role at the next authorization check, and the group appears in
  the Project Access page Groups section.
- Given a group is already shared on a project, When I attempt to grant the group again or change
  its role, Then the system does not create duplicate grants and reflects the changed role.
- Given I try to grant a group the OWNER role, When I submit, Then the grant is rejected — groups
  can never become project OWNER.
- Given I have READ-only or WRITE-only access on a project, When I attempt to share a group, Then
  the operation is denied (sharing stays ADMINISTRATION-only).

**Requirement IDs:** `GROUP-R-06`, `GROUP-R-08`

---

### FEAT-USER-GROUPS-07 — Effective Access via Groups (composition)

**User Story**

> As a group member, I want to automatically receive the project access granted to my groups, so
> that I can work on all projects shared with my team without being added by name.

**Acceptance Criteria**

- Given I am a member of a group shared on a project at WRITE, When I open the project, Then I can
  read and write as the granted role, exactly as if I were added by name.
- Given I have both a direct grant (e.g. READ) and a group grant (e.g. WRITE) on a project, When my
  effective access is evaluated, Then the maximal of the two applies (WRITE).
- Given a system role (e.g. `ROLE_ADMIN`) grants me a higher access than my group grant, When
  effective access is evaluated, Then the maximal applies.
- Given a group is shared on a project, When a project listing or permission check runs, Then
  group-derived projects appear in my project overview (listing merges group SIDs) and
  `hasPermission(READ)` passes.

**Requirement IDs:** `GROUP-R-07`, `GROUP-NFR-01`

---

### FEAT-USER-GROUPS-08 — View and Manage Groups on the Project Access Page

**User Story**

> As a project access-administration holder, I want to see which groups are shared on my project,
> change their role, and remove them, so that I can manage team access without per-person edits.

**Acceptance Criteria**

- Given I have project ADMIN, When I open the Project Access page, Then I see the People section
  (unchanged) and a Groups section listing shared groups with their grant roles and the group
  name/description.
- Given the Groups section, When I select a shared group, Then I can change its role
  (READ/WRITE/ADMIN) or revoke the grant; revocation takes effect at the next authorization check
  (≤60s NFR).
- Given I view a shared group and I am not a member of it, When I expand it, Then I see only the
  group name and description — no member list or counts.
- Given a user with plain READ access views the Project Access page, When they inspect a shared
  group, Then they see group names + descriptions only (no members) — the surface is not widened
  beyond today's READ-graded collaborator list.

**Requirement IDs:** `GROUP-R-08`, `GROUP-R-09`, `GROUP-R-06`, `GROUP-NFR-01`

---

## EPIC 4 — Notifications

**Definition of done:** Users are informed only of newly gained project access (deduplicated), get
a digest when joining a group with existing grants, receive revocation emails on removal, and
project owners/admins are informed of membership changes on shared groups. Role-level changes are
audit-log-only.

---

### FEAT-USER-GROUPS-09 — Notifications on Newly Gained Group Access

**User Story**

> As a user, I want to be notified only about project access I newly gain through a group, so that
> I know I can now work on those projects without receiving spam when nothing changes for me.

**Acceptance Criteria**

- Given I am newly added to a group shared on a project, When the group or project grant is
  created, Then I receive a single email per project I newly gain access to (deduplicated across
  multiple groups and roles).
- Given I join a group that is already shared on several projects I previously had no access to,
  When the join happens, Then I receive a digest email listing exactly those projects.
- Given a group grant changes my role on a project but I already had access, When the role changes,
  Then I do not receive a newly-gained-access email (role-level changes stay audit-log-only), and
  project owners/admins are informed of the membership change per policy.

**Requirement IDs:** `GROUP-R-10`

---

## EPIC 5 — UI surfaces

**Definition of done:** My Groups view (account area), Admin Groups view, Project Access page
Groups section, share-dialog group tab, and project cards showing group names all exist and
enforce the visibility policy.

---

### FEAT-USER-GROUPS-10 — My Groups View (account area)

**User Story**

> As a user, I want a "My Groups" view in my account area showing the groups I belong to and my
> role, with manager/owner controls, so that I can manage my ad-hoc groups and see my group
> memberships in one place.

**Acceptance Criteria**

- Given I open My Groups, When my memberships are listed, Then each group shows name, description,
  and my internal role; groups without membership do not appear.
- Given I am an owner of an ad-hoc group, When I use My Groups, Then I can appoint managers,
  add/remove members, rename, and dissolve (with confirmation).
- Given I am a manager, When I use My Groups, Then I can add/remove members and rename/describe the
  group; I cannot appoint managers or dissolve.
- Given I am a plain member, When I use My Groups, Then I can self-remove but perform no management
  actions.
- Given the group is an org group I belong to, When I view it in My Groups, Then I see
  membership-only actions appropriate to my role (no owner-equivalent admin controls).

**Requirement IDs:** `GROUP-R-05`, `GROUP-R-03`, `GROUP-R-02`, `GROUP-R-04`

---

### FEAT-USER-GROUPS-11 — Admin Groups View

**User Story**

> As a QBiC administrator, I want a dedicated Admin Groups view, so that I can create and manage
> org groups, oversee all groups (incl. ad-hoc read-only), and act as owner-equivalent for org
> groups.

**Acceptance Criteria**

- Given I am a QBiC admin, When I open Admin Groups, Then I can create, rename, and dissolve org
  groups; assign managers; and manage org-group membership.
- Given I am a QBiC admin, When I select an ad-hoc group, Then I see it read-only (name,
  description, members) and cannot modify it.
- Given I am a QBiC admin acting as owner-equivalent for an org group, When a governance action is
  needed (dissolve/oversight), Then it is performed at the application layer via `ROLE_ADMIN`
  without an OWNER membership row.
- Given I am not a QBiC admin, When I attempt to open Admin Groups, Then access is denied.

**Requirement IDs:** `GROUP-R-01`, `GROUP-R-12`

---

### FEAT-USER-GROUPS-12 — Show Group Names on Project Cards

**User Story**

> As a project collaborator, I want to see on a project card which groups the project is shared
> with, so that I can understand team-based access at a glance.

**Acceptance Criteria**

- Given a project is shared with one or more groups, When I view its project card in the project
  overview, Then the group names are displayed (compact).
- Given a project is shared with a group but I am not a member of it, When I view the card, Then
  only the group name is shown — no member list or counts.
- Given a project is not shared with any group, When I view its card, Then no group section is
  rendered.
- Given the share grant is revoked, When the project card is next rendered, Then the group name no
  longer appears for that project.

**Requirement IDs:** `GROUP-R-11`

---

## EPIC 6 — Revocation & security

**Definition of done:** Revocation (membership or group grant) takes effect at the next
authorization check with ≤60s propagation across instances; dissolution removes all ACEs and
membership rows (no orphaned grants); membership data is never exposed to non-members.

---

### FEAT-USER-GROUPS-13 — Revoke Group Access and Understand Revocation Semantics

**User Story**

> As a project access-administration holder or group manager, I want revocation (removing a member
> from a group, or removing a group grant from a project) to take effect quickly and predictably,
> so that a removed user or team loses access without waiting for a session or cache to expire.

**Acceptance Criteria**

- Given a group grant is revoked from a project or a member is removed from a shared group, When
  the next authorization decision for that user/project runs, Then access is denied — with a
  maximum observed propagation delay of 60s across all deployed instances (asserted by integration
  tests).
- Given a member is removed from a group, When the removal completes, Then they immediately lose
  group-derived access to all projects the group is shared on and receive the revocation email.
- Given a group is dissolved, When the dissolution completes, Then all its ACEs are removed from
  `acl_entry` (no orphaned `GROUP_<id>` SID remains) and all its `group_membership` rows are
  deleted.
- Given a member removed from a group still has a direct grant on a project, When the next
  authorization check runs, Then their access reverts to the maximal of their remaining direct and
  system-role grants — not the removed group grant.

**Requirement IDs:** `GROUP-R-07`, `GROUP-NFR-01`, `GROUP-NFR-03`

---

### FEAT-USER-GROUPS-14 — Visibility Baseline (membership not exposed to non-members)

**User Story**

> As a user, I want to be sure that group membership information is never visible to people outside
> the group, so that I keep control over who can see who is in which team.

**Acceptance Criteria**

- Given a group exists, When a non-member (no project access-administration, no admin role) views
  the group directory, project cards, Project Access page, or any other surface, Then they never
  see the member list, member counts, or any signal of a specific user's membership.
- Given a project access-administration holder or QBiC admin views a shared group, When they are
  not members themselves, Then they see the member list only in the context governed by
  admin/access-administration (per strategy §5.1); on READ-only surfaces they see group
  name+description only.
- Given I am a member of a group, When I view that group, Then I see the member list and counts;
  when I view a different group I am not a member of, I do not.
- Given the DPA (data processing agreement), When a user is informed beforehand, Then
  ORCID/username/full-name visibility to other users is disclosed as required; group names remain
  public.

**Requirement IDs:** `GROUP-R-09`, `GROUP-NFR-03`

---

## Proposed requirement IDs (draft, for the requirements PR)

These IDs are **proposals** — they must be written into `docs/requirements.md` under a new `GROUP`
domain in a dedicated, human-approved requirements PR (AGENTS.md §0/§12, strategy §6). Classify
constraints (`GROUP-C-*`) separately; constraint IDs must not be referenced by stories.

### Functional

| ID | Statement |
|---|---|
| `GROUP-R-01` | QBiC admins can create and manage org groups (unique name case-insensitive, description, membership, manager assignment). |
| `GROUP-R-02` | Users can create ad-hoc groups self-service; creator becomes OWNER; empty ad-hoc groups auto-dissolve. |
| `GROUP-R-03` | Group OWNER/MANAGER can add/remove MEMBERs; MANAGER can rename/describe; MEMBER can self-remove. |
| `GROUP-R-04` | When a manager leaves an ad-hoc group such that it would be left without governance, the group offers a guarded transfer-or-dissolve choice. |
| `GROUP-R-05` | Users can discover groups by public name/description and see their own membership and role. |
| `GROUP-R-06` | Users with project change-access can share a group onto a project at READ/WRITE/ADMIN (never OWNER); members gain access at the next authorization check. |
| `GROUP-R-07` | Effective project access is the maximal of direct grants, group grants, and system roles. |
| `GROUP-R-08` | Project access-administration holders can list, change, and revoke group grants on a project. |
| `GROUP-R-09` | Group names/descriptions are public; member lists and counts are visible only to group members, project access-administration holders, and QBiC admins; membership is never exposed to non-members. |
| `GROUP-R-10` | Notification profile: newly-gained-access emails (deduplicated), join digest, revocation email, dissolution email, project owner/admin information on membership changes; role-level changes are audit-log-only. |
| `GROUP-R-11` | Project overview cards show the group names a project is shared with. |
| `GROUP-R-12` | QBiC admins can administer org groups (CRUD, membership, oversight) and have read-only oversight of ad-hoc groups. |

### Non-functional

| ID | Statement |
|---|---|
| `GROUP-NFR-01` | When a user's group membership or a group's project grant is revoked, the change must take effect at the next authorization decision for the affected user, with a maximum observed propagation delay of 60 seconds across all deployed instances. (PO-confirmed, strategy §4.6.) |
| `GROUP-NFR-02` | Group names must be unique case-insensitively. |
| `GROUP-NFR-03` | Revoking/dissolving a group must not leave orphaned `GROUP_<id>` ACE entries or membership rows. |

---

## Pending decisions / open items (do not create stories for these yet)

- Group size limits / group count limits per user (strategy §8).
- Manager demotion path (§8) — story 02 and 04 cover assignment; explicit demotion is deferred.
- Whether org groups pre-seed with any project grants at rollout (§8).
- Audit-trail: strategy §3/§5.2 says "audit events for every group operation" — a dedicated
  `FEAT-USER-GROUPS-15 — Audit Group Operations` story should be created if the PO wants explicit
  user-visible audit coverage (not created here to avoid inventing scope).

---

_Last updated: 2026-09-21_