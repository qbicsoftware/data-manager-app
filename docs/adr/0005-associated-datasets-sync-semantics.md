# 0005 — Associated dataset synchronisation semantics

* Status: accepted
* Deciders: project team (via architecture interview on #1470 / #1474)
* Date: 2026-07-15

Technical Story: [FEAT-DATSET-04 — Sync a connected open, published dataset](https://github.com/qbicsoftware/data-manager-app/issues/1470) · [FEAT-DATSET-08 — Sync a connected restricted, published dataset](https://github.com/qbicsoftware/data-manager-app/issues/1474) (FEAT-DATASET-CONNECTION, #1466)

## Context and Problem Statement

[ADR-0003](0003-connection-lifecycle-stewardship.md) fixed the sync *model* (user-initiated Y1,
never-borrow-credentials C1, toast+email N1) but deliberately left the concrete sync mechanics open.
Implementing DATA-R-05 requires deciding four things that the earlier ADRs did not pin down:

1. How a "new version on the source platform" (story AC-3) is detected and followed — InvenioRDM
   publishes each version as a *new record with a new DOI*; the stored `external_handle` points at the
   record the user connected at connect time.
2. Which project permission gates a sync — the story ACs require **write** access, while
   [ADR-0003 §5](0003-connection-lifecycle-stewardship.md) listed sync as `READ`.
3. How access-link integrity is preserved when a restricted dataset's version moves — the shareable
   access link created at connect time is tied to a concrete record (version), not the concept.
4. How notifications behave under multi-dataset syncs — per-record emails would flood members; the
   ADR-0003 audience table limited metadata-update emails to `connectedBy`, which contradicts the story
   AC "notifies users about the update".

Additionally, the ADR-0003 audience table must be formally superseded for sync events by the decisions
here.

## Decision Drivers

* The story ACs are authoritative and require **write** access to sync (a sync mutates the project's
  linked snapshot) and notification of users on version updates.
* InvenioRDM's concept (parent) recid always resolves to the latest published version — verified live:
  `GET /api/records/{conceptRecid}` → `302` → `/api/records/{latestRecid}`; the HTTP client follows
  `Redirect.NORMAL` on GET, so no client change is needed to follow the redirect.
* Access links are created per record (`POST /api/records/{recordId}/access/links`), i.e. **tied to the
  version**; collaborators must be able to reach the version shown in the snapshot, and stale links must
  not linger.
* Data integrity on our side: the local snapshot must never silently diverge from what the source
  actually serves to the project (no partial "new version shown, old link" state).
* Users must be able to see *why* a record could not be updated (insufficient rights, missing provider
  configuration) — inline, per record.
* Notification flooding must be avoided when several versions update in one trigger (Sync All).
* Never-borrow-credentials ([ADR-0002](0002-invenio-rdm-api-client-credentials.md),
  [ADR-0003](0003-connection-lifecycle-stewardship.md)) remains inviolable: a sync only ever uses the
  invoking user's own token; public-metadata records need no token.
* Credential status is only updated on explicit user verification (ADR-0002 §9): a failed sync must
  never silently flip a credential to `INVALIDATED`.

## Considered Options

### Version following

* [V1] Parent-recID resolution: keep the concrete record id as `external_handle`; sync resolves the
  latest version via the concept recid (extra `GET` only when `is_latest == false`); persist
  `parentHandle` on the metadata snapshot for stable future syncs
* [V2] Store the concept recid as the handle at connect time so sync is a plain re-fetch
* [V3] Same-record refresh only (no version following)

### Sync permission

* [P1] `WRITE` on the project (per story ACs — a sync is a write to the local snapshot)
* [P2] `READ` on the project (per ADR-0003 §5 status quo)

### Access-link integrity on restricted version bumps

* [L1] Hard failure: if the new version's access link cannot be created, the snapshot update fails
  atomically (create → commit → revoke old after commit; rollback new link on persistence failure)
* [L2] Best-effort: metadata moves to the new version regardless; link refresh attempted, stale link
  kept on 403

### Notification aggregation

* [N1] One summary domain event per sync trigger + one combined email per member (all collaborators
  except the actor), sent only when ≥1 record changed; no emails on no-op or failure
* [N2] Per-record events → per-record emails (flooding)
* [N3] Time-window aggregation inside the notification directive (brittle timing/state)

### Access determination

* [A1] Per-record attempt is the source of truth; the HTTP call decides 401/403 per record; only a
  local short-circuit for "metadata-restricted record with no configured credential"
* [A2] Pre-flight per-instance token liveness check (`GET /api/users`) before per-record calls

## Decision Outcome

Chosen: **V1 + P1 + L1 + N1 + A1.**

1. **Version following (V1):** sync resolves the latest published version via the InvenioRDM concept
   recid. Adapter flow: `GET /records/{handle}`; if `versions.is_latest == true` return the record;
   otherwise `GET /records/{parent.id}` (302 followed by the client) returns the latest record. The
   snapshot, `external_handle`, and PID all move to the latest record; the concept recid is persisted as
   `parentHandle` on `InvenioRdmResourceMetadata` (nullable, backward compatible — legacy rows resolve it
   on first sync).
2. **Sync permission (P1):** Sync and Sync All require `WRITE` on the parent project. This **amends**
   ADR-0003 §5 (sync row: READ → WRITE). The story ACs are authoritative: a sync is a write operation on
   the project's linked snapshot.
3. **Access-link integrity (L1):** for restricted datasets, a version bump commits **only if** the new
   shareable access link was created on the latest record. Order: create new link (must succeed) → update
   snapshot → persist → revoke the old link afterwards (best-effort, failures logged). If persistence
   fails after link creation, revoke the new link (rollback, mirroring the connect flow's
   `revokeAccessLinkIfCreated`). This guarantees the project never shows a version its members cannot
   reach via the snapshot's access link.
4. **Notification aggregation (N1):** the application layer emits **one**
   `AssociatedDatasetsSyncedEvent` per sync trigger, only when ≥1 record changed. A policy directive
   sends **one** email per project collaborator (all roles, actor excluded, via JobRunr) listing all
   updated records (title, PID, old→new version, access-status flips). No emails for no-op syncs and no
   emails for failures (failures surface to the invoking user in the results sidecar). This **supersedes
   the ADR-0003 audience table for sync events** ("sync succeeded (metadata updated) → only connectedBy"
   becomes "all project members except the actor").
5. **Access determination (A1):** per-record HTTP attempts are the source of truth for token validity on
   a record (verified: per-record access is server-enforced; even anonymous search hides fully-restricted
   records). The only free local short-circuit: a metadata-restricted snapshot
   (`recordAccess != PUBLIC`) with no configured credential for the instance is reported as "provider
   must be configured first" without an HTTP call. No per-instance liveness pre-flight in v1; 401/403
   map to `CREDENTIAL_REQUIRED` / `CREDENTIAL_INSUFFICIENT` per-record outcomes. A failed sync never
   mutates credential status (ADR-0002 §9).

### Positive Consequences

* The local snapshot always reflects the latest published version and is reachable by the project via a
  fresh access link — no stale or unreachable "latest" state.
* Version-following via the concept recid requires only a one-field parsing addition (`parent.id`) and
  reuses the client's existing redirect handling; no migration of existing connections (nullable
  `parentHandle`).
* One combined email per trigger prevents floods while still notifying every member of version updates
  (story AC-3).
* Per-record outcomes keep the UI truthful: rows show exactly why a record could or could not be
  updated.
* The plan document
  (`docs/plans/FEAT-DATSET-04-08-sync-connected-datasets.md`) pins the implementation surface.

### Negative Consequences

* Hard-fail link refresh means a restricted version bump is blocked until a user with link-creation
  permission (record owner) syncs — a deliberate trade for integrity; the UI must explain it ("only the
  record owner can refresh the access link").
* Version-following costs one extra `GET` per sync only when the connected record is no longer the
  latest (common after a version bump, rare otherwise).
* The synced `external_handle` mutation requires care: the aggregate's handle is no longer immutable
  after connect (it follows the version); the dup-PID guard (plan §Edge Cases) prevents duplicate
  connections after a bump.
* Amending ADR-0003 via this ADR introduces a supersession relationship that must be documented.

## Pros and Cons of the Options

### V1 — Parent-recID resolution

* Good, because it keeps `external_handle` pointing at a concrete, version-anchored record — required
  for version-tied access links.
* Good, because the concept recid resolution is one redirect the client already follows.
* Good, because V2 would break access-link creation (`POST /records/{conceptRecid}/access/links` would
  hit the 302, and `Redirect.NORMAL` does not follow redirects on POST).
* Bad, because it adds a small `parent.id` parsing surface and a nullable `parentHandle` field.

### V2 — Concept recid as handle

* Good, because sync becomes a plain re-fetch.
* Bad, because it changes connect-time semantics for new connections only (dual semantics), requires a
  migration story for existing rows, and **breaks access-link creation on the concept id** (POST
  redirects are not followed).

### V3 — Same-record refresh only

* Good, because it is the simplest possible sync.
* Bad, because it cannot satisfy story AC-3 (a new version is a new record; the stored handle would
  return the superseded record with `is_latest=false`).

### P1 — WRITE permission

* Good, because it matches the story ACs and the semantics (sync writes the local snapshot).
* Good, because it reuses the existing project ACL (`hasPermission WRITE`), same as connect/remove.
* Bad, because it diverges from the earlier ADR-0003 note (handled via supersession).

### L1 — Hard failure on link refresh

* Good, because it guarantees local/remote consistency for collaborators.
* Good, because it reuses the existing create/rollback link pattern.
* Bad, because syncs of restricted version bumps can be blocked pending an owner — mitigated by explicit
  per-row guidance.

### N1 — Combined summary email

* Good, because it satisfies "notifies users about the update" without flooding (Sync All).
* Good, because aggregation happens where the batch outcome is known (deterministic, testable).
* Bad, because a member receives one email even if they personally don't care — acceptable for version
  updates of project data.

### A1 — Per-record attempt as source of truth

* Good, because per-record grants/ownership are unknowable locally (verified against the live API).
* Good, because no additional API surface in v1.
* Bad, because a sync with a dead token produces N per-record failures rather than one upfront warning —
  accepted; each row still carries precise guidance.

## Links

* Refines [ADR-0002](0002-invenio-rdm-api-client-credentials.md) (credential handling, retry, D1 boundary)
* Supersedes in part [ADR-0003](0003-connection-lifecycle-stewardship.md) (§5 sync permission row; sync
  event audience mapping)
* Depends on [ADR-0001](0001-associated-datasets-domain-model.md) (aggregate, soft delete, universal columns)
* Implementation plan: [FEAT-DATSET-04-08-sync-connected-datasets](../plans/FEAT-DATSET-04-08-sync-connected-datasets.md)