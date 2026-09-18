# 0008 — Pinned projects are user preferences, not project state

* Status: accepted
* Deciders: project team (via story refinement on FEAT-PINNED-PROJECTS)
* Date: 2026-09-14

Technical Story: FEAT-PINNED-01 — Pin Projects for Quick Access on the Project Overview ([docs/features.md](../features.md))

## Context and Problem Statement

`USER-R-04` introduces pinned projects: an authenticated user marks a small, self-selected subset of
the projects they can access, and that subset is presented to them as their entry point to project
work. The requirement is deliberately silent about where this information lives, but two questions
have to be answered before any code is written, and both are hard to reverse later:

1. **Which bounded context and which aggregate own the association between a user and a pinned
   project?** The obvious candidates are the `Project` aggregate, the `User` aggregate in `identity`,
   or a preference owned by `project-management` outside either aggregate.
2. **How is a pin rendered once the user can no longer read the project it points at?** Every project
   read in this application goes through an access-restricted lookup. A pin whose project has become
   inaccessible can no longer be labelled or removed through that path, yet the user must still be
   able to recognise it and free the place it occupies — and the set of pins is bounded, so a pin the
   user cannot act upon is not merely cosmetic, it is a permanently occupied slot.

## Decision Drivers

* Pinning is initiated by one user and is private to that user; it is not a property of the project
  and must not become visible to or modifiable by other project members.
* `Project#lastModified` is the default sort key of the project overview (and of several other
  collections), so any model that writes through the `Project` aggregate makes one user's pinning
  action a mutation of shared project state.
* Spring Security ACL protects `Project` instances. A deletion path that bypasses the ACL is only
  defensible if it is narrow, explicit, and touches no project attribute.
* `USER-R-04` states that pinning shall never grant or reveal access to project data the user is not
  entitled to see; the read path must therefore never fall back to an unrestricted project query.
* The application already stores per-user, non-domain data outside aggregates in the data-management
  datasource (e.g. `personal_access_tokens.userId` is a bare `varchar(255)` reference with no foreign
  key to `users`).
* Project deletion is a real event, and pins of deleted projects must not leave dangling rows.

## Considered Options

### Ownership of the pin

* **[O1] Application-layer preference in `project-management`** — a dedicated table
  (`pinned_projects`), keyed by user id and project id, written and read by an application service;
  no `DomainEvent`, no change to the `Project` aggregate.
* **[O2] State on the `Project` aggregate** — a collection of pinned-by users held by the project and
  persisted with it.
* **[O3] State on the `User` aggregate in `identity`** — a collection of pinned project ids held by the
  user, exposed to other contexts through `identity-api`.

### Label of an inaccessible pin

* **[L1] Write-once label snapshot on the pin row** — the project code and title are copied into the
  pin row when the pin is created and are used only when the live project can no longer be read.
* **[L2] Live project query bypassing the access-restricted lookup** for the user's own pins.
* **[L3] No label at all** — an anonymous "a project you can no longer access" entry with a remove
  action.
* **[L4] Drop the pin row** when it is found to be inaccessible.

## Decision Outcome

**Ownership: [O1].** A pin is modelled as an application-layer preference of
`project-management`, stored in `pinned_projects` with a composite key of (`userId`, `projectId`), a
`pinnedAt` timestamp, and the label snapshot described below. `userId` is a bare `varchar` reference,
following the `personal_access_tokens` precedent, so no dependency on the `identity` schema is
introduced. `projectId` references `projects_datamanager(projectId)` with `ON DELETE CASCADE`, which
keeps deleted projects from leaving orphan rows. No `DomainEvent` is emitted: a pin is not a
significant state change of any aggregate, and publishing it would invite other contexts to treat it
as business data.

**Label: [L1], combined with an explicit non-ACL removal path.** The pin row carries the project code
and title as captured at pin time. For a project the user can still read, the snapshot is ignored and
live data is used; the snapshot is rendered only as the fallback label of an inaccessible pin. A pin
is removed by (`userId`, `projectId`), deliberately **without** an ACL check on the referenced
project, because the row is the user's own data and refusing to delete it would strand them.

**Rejected: [L4].** Auto-purging inaccessible pins destroys the user's expressed intent on a transient
permission state, and access revocation is reversible while the deletion is not.

### Positive Consequences

* Pinning, unpinning and re-ordering never touch `Project`, so `lastModified` keeps meaning "the
  project changed" rather than "somebody's shortlist changed", and no optimistic-lock version bump on
  a shared aggregate is caused by a private action.
* The association stays inside the context that can resolve it: `project-management` already owns the
  access-restricted project lookup, so the pinned set and the readable-project set are intersected in
  one place rather than across a context boundary.
* No cross-context contract change is needed: `identity` is untouched, and `identity-api` gains no
  project-specific vocabulary.
* Inaccessible pins remain visible and remain removable, so a bounded pin set can always be freed by
  the user who owns it.

### Negative Consequences

* Two columns hold data that is authoritative elsewhere and can go stale. The blast radius is bounded
  by only ever rendering the snapshot when the live project is unreadable, and by never refreshing it
  on read — a page load must not become a write.
* A deletion path that does not consult the project ACL is a permanent exception in an access-controlled
  codebase. It is confined to the pin table, keyed by the authenticated owner's own id, and must be
  re-read alongside this ADR before any similar path is added.
* The snapshot replicates a project title outside the project row. It inherits the same protection as
  the pin table (per-user, never joined into another user's result), but it is one more place a project
  name exists, and it must never be exported, indexed, or included in RO-Crate output.
* Rendering has to classify **every** stored pin of the user, so the cap is defined over stored rows
  rather than over readable ones. Applying the access restriction before the row limit would silently
  shrink the shortlist whenever an inaccessible pin exists, and would also free up places that the user
  did not consciously give back.

## Pros and Cons of the Options

### [O1] Application-layer preference in `project-management`

* Good, because a pin belongs to the (user, project) pair and not to either side alone.
* Good, because the write is cheap and cannot contend with project edits.
* Good, because the same service that resolves accessible projects resolves readable pins.
* Bad, because the preference is invisible to the domain model, so domain invariants about pins cannot
  exist — the cap is enforced in the application service instead.

### [O2] State on the `Project` aggregate

* Good, because pins would be reachable from the project without a join.
* Bad, because every pin bumps a shared aggregate: `lastModified` changes, the optimistic-lock version
  changes, and a private preference becomes project history.
* Bad, because it makes the project the wrong owner: other project members neither can nor should see
  who pinned it.

### [O3] State on the `User` aggregate in `identity`

* Good, because the data is genuinely per-user.
* Bad, because `identity` would store and validate `project-management` identifiers, inverting the
  dependency the `identity-api` boundary exists to prevent.
* Bad, because rendering titles requires reading projects, so the join crosses contexts on every page
  load.

### [L2] Live project query bypassing the access-restricted lookup

* Good, because the label would always be current.
* Bad, because it puts an intentionally unprotected project read in the codebase, contradicting the
  pattern in `ProjectInformationService` where accessible ids are resolved before any query, and would
  read as an access-control defect to any reviewer.

### [L3] No label at all

* Good, because it replicates nothing.
* Bad, because with more than one inaccessible pin the user cannot tell which is which, so the row
  becomes recognisable only by trial and error — the opposite of quick access, which is the whole
  point of the Feature.

## Links

* Implements [USER-R-04](../requirements.md) (pinned-project quick access, including "pinning shall
  never grant or reveal access").
* Related to [ADR-0007](0007-paginated-lists-and-list-state.md) — pinned projects are deliberately
  kept out of the paginated list state, so they neither occupy a page nor enter the URL; that ADR's
  deterministic tie-break and count semantics are unchanged by this decision.
* Follow-up candidate: a global pinned-project switcher in the application navigation can reuse this
  preference store without changing the storage decision recorded here.
