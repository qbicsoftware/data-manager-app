# 0003 — Dataset connection lifecycle and data stewardship

* Status: approved
* Deciders: project team (interviewed via [`interview-feat-dataset-connection.md`](interview-feat-dataset-connection.md))
* Date: 2026-07-13

Technical Story: [Connect associated InvenioRDM datasets with Data Manager projects](https://github.com/qbicsoftware/data-manager-app/issues/1466) (FEAT-DATASET-CONNECTION)

## Context and Problem Statement

An *associated dataset* connects an external record (hosted on Zenodo, FDAT, or a future
LIMS) to a Data Manager project. Once connected, the dataset has a lifecycle within the
project: it can be viewed, synced, and removed. Each step in that lifecycle raises
questions about:

1. **Who can perform the action?** (ACL interaction — see
   [ADR-0001 §Security](0001-associated-datasets-domain-model.md) for the domain model;
   this ADR focuses on the lifecycle.)
2. **What happens on the external platform?** (Do we use the user's own token, or do we
   borrow someone else's?)
3. **What do other project members see when things change?** (notification model)
4. **What is preserved when the connection is removed?** (data stewardship)
5. **How is sync structured?** (manual vs. background; user-initiated vs. automated)

The project has existing patterns for project ACL (Spring Security ACL tables + permission
model) and for email communication (`subscription-provider` SMTP). The design problem is
which patterns to reuse and which to extend, and how to document the invariants
operationally.

## Decision Drivers

* Project ACL is the natural authority for "who sees what in this project." Dataset
  connections are attached to a project, not independent resources.
* A sync operation against an external platform is an authenticated action on behalf of a
  user. That user's identity and access privileges must flow through transparently. The
  system must not silently use another user's credentials.
* Notification should be user-actionable and not flood inboxes. The granularity should be:
  "actor gets immediate feedback, other members get email" — not "every event goes to
  everyone."
* The existing `NotificationService` + `Exchange` in-memory bus (in the `broadcasting`
  module) is acknowledged scaffolding — its own JavaDoc marks it "development purposes
  only" — and does not actually reach users. Building on it would be cargo-culting.
* Cross-context Artemis pub/sub is used in the project for one specific integration
  (identity events flowing from `identity` into `project-management`). It is not a
  general-purpose notification dispatch system for within-context events, and there is no
  v1 need for another bounded context to observe dataset connection events.
* Stakeholders confirmed that snapshot-sync history (who synced when, what metadata
  snapshot was before) is **not required**. Data stewardship is provided by the soft-delete
  tombstone and the last-known snapshot retention ([ADR-0001](0001-associated-datasets-domain-model.md)).

## Considered Options

### Sync model

* [Y1] User-initiated sync only (v1); background refresh is an explicitly deferred future path
* [Y2] Automatic sync on project page load
* [Y3] Scheduled background sync (recurring JobRunr job)
* [Y4] Per-connection opt-in auto-refresh with consent (`autoRefreshEnabled` flag on aggregate)

### Sync credential policy

* [C1] Invoking user's own token; public records don't need token; no credential borrowing
* [C2] ConnectedBy user's stored token (credential borrowing)
* [C3] Service-account token (institutional)

### Notification delivery

* [N1] Actor sees Vaadin toast synchronously; other project members receive email via
  `subscription-provider`; domain events drive the email dispatch
* [N2] Use `NotificationService` + `Exchange` in-memory bus
* [N3] Use Artemis pub/sub for all notifications (including in-app)
* [N4] Email only (no toast)

### Notification scope (event → audience)

| Event | Audience in recommended mapping |
|---|---|
| Dataset connected | All project members except actor |
| Dataset connection removed | All project members except actor |
| Sync succeeded (metadata updated) | Only `connectedBy` (low-signal) |
| Sync changed access status | All project members except actor |
| Sync failed (credential issue) | Only invoking user (actionable by them) |
| User added / removed their own token | Only that user (personal credential action) |

### Removed-connection visibility

* [V1] Soft-deleted tombstone retained for audit; no UI exposure in v1
* [V2] Soft-deleted tombstone retained; visible to project WRITE users via a "history" toggle

## Decision Outcome

**Chosen option: Y1 + C1 + N1 + V1 (with the event→audience mapping above).**

Concretely:

1. **Sync is user-initiated only** for v1. Background / scheduled sync is an explicitly
   deferred future enhancement path. When it becomes a concern, the credential policy from
   option Y4 (per-connection consent) is the natural evolution.

2. **Never-borrow-credentials principle** in full:
   * Each sync attempt uses the invoking user's own InvenioRDM token.
   * Public records sync without a token.
   * For restricted records: if the invoking user has no token → they are informed ("Add a
     [instance] token to sync this restricted dataset"); if their token is insufficient →
     "Your token does not grant access to this restricted record."
   * The original `connectedBy` user's token is **never reused** to fulfil another user's sync.
   * Historical metadata + `connectedBy` + `lastSyncedAt` are preserved permanently even if
     the original connecting user leaves the project.

3. **Notification delivery** via Vaadin toast + `subscription-provider` email. No use of
   `NotificationService` / `Exchange` (dead scaffolding). No Artemis pub/sub for within-context
   events (no v1 need; deferred if another bounded context ever needs to observe this feature).
   Domain events emitted by the application layer drive the email dispatch to project members.
   Actor is excluded from the project-member recipient list by design (the toast already tells
   them what happened). A project-member email lookup (which does not yet exist as a query in
   the codebase) needs to be built as a prerequisite. Email delivery is not guaranteed —
   SMTP failures silently drop notifications; this is an accepted risk consistent with the
   rest of the application.

4. **Removed-connection rows retained for audit.** No user story currently exposes them;
   exposing removed connections in the UI is a future decision if it arises.

5. **ACL inheritance:** Connections inherit project ACL without separate ACL entries. The
   role mapping from the project ACL tables applies uniformly:

   | Action | Minimum permission |
   |---|---|
   | View connected datasets | `READ` on project |
   | Connect a dataset | `WRITE` on project |
   | Remove a connection | `WRITE` on project |
   | Sync a dataset | `READ` on project |
   | Manage own InvenioRDM tokens | Authenticated (no project permission needed —
     credentials are user-level) |

6. **Cross-user visibility principle** (recorded for ADR reference):

   > Visibility of a connected dataset follows project ACL. Operations requiring external
   > authentication (e.g., sync of a restricted record) succeed if the invoking user has
   > configured the instance with a valid token granting sufficient access to the external
   > record; otherwise they fail. Failure is reported to the invoking user as a user-facing
   > error per the error-mapping pipeline ([ADR-0002](0002-invenio-rdm-api-client-credentials.md)).

### Positive Consequences

* Lifecycle semantics are simple and explicit — fewer failure modes, easier debugging.
* No JobRunr integration for sync reduces the v1 scope significantly.
* Never-borrow-credentials gives forensic and privacy correctness: external platform logs
  and DM logs both accurately reflect who initiated each action.
* Toast + email matches the mental model users expect: "I get immediate feedback when I'm
  looking; my teammates get an email when they aren't."
* No new notification persistence model is built; we rely on email SMTP + the existing
  `subscription-provider`, which is production-grade.
* Soft-delete audit retention leaves the door open for future UI without requiring schema
  migrations.

### Negative Consequences

* No background sync means stale metadata between syncs. Mitigation: the UX makes sync
  state visible (last-synced timestamp) so users know when they might want to re-sync.
* Project-member email lookup needs to be built as a new service — not a major cost, but
  a prerequisite that must be scheduled.
* Email delivery is not guaranteed; users relying on email for notifications could miss
  changes if SMTP has issues. Mitigation (if it becomes a real problem): introduce an
  in-app unread-notification model later.
* Actor is excluded from the "other members" mailing list by convention. This is
  deliberate (they already saw it via toast), but it means actor-initiated events never
  reach their own email inbox, even when the actor is offline. This is a reasonable
  trade-off in v1.

## Pros and Cons of the Options

### Y1 — User-initiated sync only (chosen)

* Good, because it keeps the v1 surface minimal and explicit.
* Good, because no "who is the background user?" design problem arises.
* Good, because credential-borrowing risk cannot exist without automation.
* Bad, because metadata can go stale between explicit syncs.

### Y4 — Per-connection opt-in auto-refresh (deferred future)

* Good, because it preserves the never-borrow-credentials principle via explicit user
  consent.
* Good, because it's the natural evolution path if v1 stakeholders later want background
  refresh.
* Bad, because it adds aggregate state and JobRunr integration that isn't needed in v1.

### C1 — Invoking user's own token; no borrowing (chosen)

* Good, because it preserves forensic non-repudiation and privacy boundaries.
* Good, because a user who lacks access to a record cannot silently gain visibility.
* Bad, because a user who has no token cannot sync a restricted dataset they can otherwise
  "see" in DM. The UX must explain this clearly.

### N1 — Toast + email via domain events + SMTP (chosen)

* Good, because it uses production-grade paths (`subscription-provider`).
* Good, because it doesn't depend on dead scaffolding (`NotificationService`).
* Good, because it keeps in-app and out-of-band notifications aligned (actor sees immediate
  feedback; others get email).
* Bad, because email delivery is not guaranteed. If SMTP fails, a silent drop occurs.

### V1 — Soft-delete tombstone, no UI exposure (chosen)

* Good, because it preserves audit provenance without adding UI complexity that has no
  user story driving it.
* Good, because it is non-irreversible: exposing the history is a UI-layer decision that
  doesn't require schema changes later.
* Bad, because audit rows in the DB are invisible to end users by default; the audit
  capability is latent until a story is raised.

## Links

* Depends on [ADR-0001](0001-associated-datasets-domain-model.md) — the aggregate and
  soft-delete semantics are defined there.
* Depends on [ADR-0002](0002-invenio-rdm-api-client-credentials.md) — the
  never-borrow-credentials principle and credential storage shape the sync + token flows here.
* Related to [ADR-0004](0004-fair-signposting-deferred.md) — if InvenioRDM Signposting
  becomes the primary integration in 1-2 years, the lifecycle semantics here remain intact;
  only the client layer changes ([ADR-0002](0002-invenio-rdm-api-client-credentials.md)).
