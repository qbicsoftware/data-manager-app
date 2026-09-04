# Implementation Plan: FEAT-DATSET-04 + FEAT-DATSET-08 — Sync Connected Datasets

> **Stories:** [#1470](https://github.com/qbicsoftware/data-manager-app/issues/1470) (FEAT-DATSET-04, open) · [#1474](https://github.com/qbicsoftware/data-manager-app/issues/1474) (FEAT-DATSET-08, restricted)
> **Parent Feature:** [#1466](https://github.com/qbicsoftware/data-manager-app/issues/1466) (FEAT-DATASET-CONNECTION)
> **Requirement:** `DATA-R-05` (Connected-Dataset Synchronisation) — see Requirement ID Note below
> **ADR references:** [ADR-0001](../adr/0001-associated-datasets-domain-model.md), [ADR-0002](../adr/0002-invenio-rdm-api-client-credentials.md), [ADR-0003](../adr/0003-connection-lifecycle-stewardship.md), [ADR-0005 (draft)](../adr/0005-associated-datasets-sync-semantics.md)

---

## Requirement ID Note

The two story issues cite `DATA-R-02`, which was the **proposed** ID in the parent feature brief
(#1466 §Proposed new requirements). The registry (`docs/requirements.md`) now contains the
authoritative requirement for this work:

> **DATA-R-05 — Connected-Dataset Synchronisation:** The system shall support synchronising connected
> dataset metadata with the source InvenioRDM instance, so that locally stored connection records stay
> consistent with upstream changes (e.g. new versions, embargoes lifted, titles corrected).

Issues should be updated to reference `DATA-R-05` (governance cleanup). Additionally, the notification
behavior shipped by this feature (per-trigger summary email to all project members) is not yet covered by
a formal requirement — the parent feature proposed `COMM-R-01` (Project Change Notifications), which is
**missing from the registry**. Adding it requires human approval (AGENTS.md §12, requirements changes) and
is tracked as a governance item, not a blocker.

---

## Acceptance Criteria (merged — the stories are identical except for the access level)

| # | DATSET-04 (open) | DATSET-08 (restricted) |
|---|---|---|
| AC-1 | User with **write** access, connected dataset, click **Sync** → sync with the host instance triggered for that dataset | Same, for a restricted dataset |
| AC-2 | User with **write** access, several connected datasets, click **Sync All** → sync triggered for all connected datasets | Same, for all restricted datasets |
| AC-3 | Synchronised dataset has a new version on source → after sync, local entry updated to the latest version and users notified | Same |
| AC-4 | Sync successful → users informed about the number of updated datasets | Same |
| AC-5 | Sync fails → users informed about the failure with guidance on next steps | Same |

---

## Locked Design Decisions (from the architecture interview)

1. **Version-following (ADR-0005 §Decision 1):** sync does not just re-fetch the stored record. It
   resolves the *latest published version* via the InvenioRDM concept (parent) recid:
   - Adapter: `GET /api/records/{handle}` → if `versions.is_latest == true`, return as-is; otherwise
     `GET /api/records/{parent.id}` — the client already follows the `302` (`Redirect.NORMAL`), so this
     returns the latest record's metadata.
   - On version bump the snapshot **and** `external_handle` **and** PID move to the latest record; the
     concept recid (`parentHandle`) is persisted on `InvenioRdmResourceMetadata` for stable future syncs.
   - Verified against the live Zenodo API: `GET /api/records/{conceptRecid}` → `302 → /api/records/{latestRecid}`.
2. **Permission — WRITE on the project** for Sync and Sync All (per the story ACs). This *amends*
   ADR-0003 §5, which listed sync as `READ`. Rationale (decider): in Data Manager, sync mutates the
   project's linked snapshot — a write operation. Recorded in ADR-0005.
3. **Never-borrow-credentials (ADR-0002/0003):** every sync uses the **invoking user's own** token.
   Public-metadata records sync without a token (verified: anonymous `GET` on public-metadata/restricted-files
   records returns 200). A metadata-restricted record without a configured credential is short-circuited
   locally ("provider must be configured first"); with a token, the record GET is authoritative.
4. **Access-link integrity on restricted version bumps (hard failure):** a version bump on a restricted
   dataset commits **only if** a new shareable access link could be created on the latest record. Order:
   create new link (must succeed) → update snapshot → persist → revoke the **old** link afterwards
   (best-effort, logged). If persistence fails after link creation, the new link is revoked (rollback,
   mirroring the existing `revokeAccessLinkIfCreated` connect pattern). No partial local state.
5. **Notifications — one combined email per trigger:** a single summary domain event per sync trigger,
   emitted **only when ≥1 record actually changed** (version bump or metadata/access change). A directive
   sends **one** email per project member (all roles, excluding the actor via JobRunr) listing all updated
   records (title, PID, old→new version, access-status flips). **No emails** for no-op ("already up to
   date") syncs and **no emails on failure**. Supersedes the ADR-0003 audience table for sync events
   (which limited metadata updates to `connectedBy`). Failures surface to the invoking user in the sidecar.
6. **Per-record attempt is the source of truth for access:** no per-record pre-flight. The sidecar rows
   flip live as the per-dataset `Flux` responses arrive (requestId-correlated, bounded parallelism 3 —
   same as the connect flow). Token liveness / per-record grants are only determined by the HTTP call.
7. **Result reporting via a dedicated sidecar** (`SyncResultsSidebar`, modeled on `ConnectDatasetSidebar`)
   instead of toast text: live per-row status, summary header, inline guidance, "Done" action.

---

## Current State of the Codebase

Already present and reusable:

| Component | Location | Status |
|---|---|---|
| `AssociatedDataset` aggregate | `project-management/…/domain/model/associated_dataset/` | `updateMetadata()` + `lastSyncedAt` exist; emits no sync event yet |
| `DatasetSource` port | `project-management/…/application/associated_dataset/` | `resolveMetadata`, `hasValidCredential`, `createAccessLink`, `revokeAccessLink` |
| `InvenioRdmDatasetSource` | `project-management-infrastructure/…/external/invenio/` | Stateless adapter; token `char[]` zeroing in `finally` (ADR-0002 D1) |
| `InvenioRdmClient` | same package | `RecordResponse` already parses `versions.is_latest`/`index`; `parent` block lacks `id`; HTTP client follows 302 (`Redirect.NORMAL`); bounded retry per ADR-0002 §7 |
| `AssociatedDatasetService` | `project-management/…/application/associated_dataset/` | Connect/remove/list with `Result`, reactive `Flux` + `requestId` correlation, bounded parallelism 3 |
| Error enums (`ConnectDatasetError`, `RemoveDatasetError`) | same package | Pattern to replicate for `SyncDatasetError` |
| Policies + directives | `project-management/…/application/policy/{,directive}` | `AssociatedDatasetConnectedPolicy`, `InformProjectCollaboratorsAboutDatasetConnection` (JobRunr email) |
| `ConnectedResourcesComponent` | `datamanager-app/…/views/projects/project/datasets/` | Cards with per-card actions (Remove); action bar with "Connect Datasets" — needs Sync + Sync All |
| `ConnectDatasetSidebar` | same package | Sliding-panel pattern to model `SyncResultsSidebar` on |
| Credential management UI | `datamanager-app/…/views/account/ExternalProvidersMain`, `VerificationSidebar` | FEAT-DATSET-14 shipped; sidecar guidance links here |
| Message properties | `datamanager-app/src/main/resources/messages/toast-notifications.properties` | `dataset.connected.*` entries; `dataset.sync.*` missing |
| `Messages` templates | `project-management/…/application/Messages.java` | `datasetConnectedToProject()` exists; combined-sync template missing |
| Spock test conventions | `project-management/src/test/groovy/…/associated_dataset/` | `AssociatedDatasetServiceConnectSpec`, `AssociatedDatasetServiceRemoveSpec`, `AssociatedDatasetRemoveSpec` |

---

## Task Breakdown

### Task 1 — Domain: version metadata plumbing

**Module:** `project-management` (+ `project-management-infrastructure` for the client model)

- `InvenioRdmClient.Parent`: add `@JsonProperty("id") String id` (concept recid) — single field; raw JSON already carries it.
- `InvenioRdmResourceMetadata`: add nullable `String parentHandle` (concept recid). Backward compatible:
  old persisted rows deserialize to `null`; `@JsonIgnoreProperties(ignoreUnknown = true)` protects future
  reads. Keep the backward-compatible convenience constructor delegating with `parentHandle = null`.
- `AssociatedDataset`: add change-diff support for sync — a method that reports what changed when applying
  new metadata (version moved? access status changed? unchanged?) so the service can produce per-record
  outcomes and the summary event payload. `updateMetadata(...)` is extended/kept for the commit step.

### Task 2 — Domain: sync summary event

**Module:** `project-management`

- New event `AssociatedDatasetsSyncedEvent(projectId, actorUserId, List<UpdatedRecord>)` under
  `…/domain/model/associated_dataset/event/` where
  `UpdatedRecord(datasetId, title, pid, previousVersion, newVersion, accessStatusChanged)`.
- One event per **sync trigger**, emitted by the application service after the batch completes — not by
  the aggregate per dataset (aggregation lives where the batch outcome is known).

### Task 3 — Port: latest-version resolution

**Module:** `project-management` (port) + `project-management-infrastructure` (adapter)

- `DatasetSource.resolveLatest(String externalHandle, InstanceConfig config, String actingUserId)`
  → `Optional<ResourceMetadata>` / `throws DatasetResolveException`. Source-agnostic contract:
  "current/latest state of the record identified by this handle".
- `InvenioRdmDatasetSource` implementation:
  - `GET /records/{handle}` (token rules per ADR-0002: public-metadata → no token required; restricted →
    actor's token, decrypted/zeroed in `finally`).
  - If `versions.is_latest == true` → return mapped metadata.
  - Else → read `parent.id` → `GET /records/{parent.id}` (client follows the 302) → return mapped metadata.
  - 404 → `Optional.empty()`; 401/403 → permanent `InvenioRdmPermanentException` (surfaced as
    CREDENTIAL_REQUIRED / CREDENTIAL_INSUFFICIENT downstream); 5xx/429 → existing bounded retry.
  - Preserve token handling: first call with token, second (parent) call reuses the same decrypted
    `char[]` if a token was configured; zeroed once in the outer `finally`.

### Task 4 — Application: sync orchestration

**Module:** `project-management`

- `SyncDatasetError` enum: `DATASET_NOT_FOUND`, `RECORD_NOT_FOUND`, `CREDENTIAL_REQUIRED`,
  `CREDENTIAL_INSUFFICIENT`, `ACCESS_LINK_REFRESH_FAILED`, `SYNC_FAILED`.
- Request/response records mirroring the connect flow: `SyncDatasetRequest(requestId, datasetId, userId)`,
  `SyncDatasetResponse(requestId, SyncOutcome)` where `SyncOutcome` is a per-dataset status carrying the
  outcome category (UPDATED, UP_TO_DATE, WARN_ACCESS, FAILED) + new version + user-facing message.
- `syncDataset(datasetId, userId)` → `Result<SyncOutcome, SyncDatasetError>` (blocking core):
  1. Load aggregate (DATASET_NOT_FOUND if absent/REMOVED).
  2. WRITE-permission enforced via `@PreAuthorize` on the project (same ACL pattern as connect/remove).
  3. Short-circuit: metadata-restricted snapshot (`recordAccess != PUBLIC`) + no valid credential for the
     dataset's instance (`hasValidCredential`) → `CREDENTIAL_REQUIRED`.
  4. `resolveLatest(handle, config, userId)` → map errors (404 → RECORD_NOT_FOUND; 401/403 relative to
     credential presence → CREDENTIAL_REQUIRED / CREDENTIAL_INSUFFICIENT; transient → SYNC_FAILED).
  5. Diff against stored snapshot (via Task 1 change-diff). Unchanged → UP_TO_DATE (no event row change).
  6. Version changed **and** restricted → `createAccessLink` on the newest record (hard gate;
     `ACCESS_LINK_REFRESH_FAILED` on exception; do **not** reach the snapshot write).
  7. Commit: `updateMetadata(latest)` (+ handle + parentHandle) → save. On save failure → roll back the
     newly created link (`revokeAccessLinkIfCreated` pattern), return SYNC_FAILED.
  8. After successful commit: revoke the **old** link best-effort (log-only on failure).
  9. Guard: if another active connection in the project already carries the target PID (new version's
     DOI), do **not** duplicate — skip the update and surface a distinct outcome (edge case, see below).
- Reactive wrappers: `syncDatasetAsync(...)` and `syncAllDatasets(projectId, userId)` — `Flux`, bounded
  parallelism 3, per-request `requestId`, security-context propagation via
  `ReactiveSecurityContextUtils` (copy of the connect flow).
- After a trigger completes: if ≥1 UPDATED → collect and dispatch **one** `AssociatedDatasetsSyncedEvent`
  (collect-during/forward-after pattern; dispatch failure logged, does not fail the sync).
- `ConnectedDatasetView`: add `Instant lastSyncedAt` (populate from `AssociatedDataset.lastSyncedAt()`).

### Task 5 — Infrastructure: access-link refresh orchestration

**Module:** `project-management-infrastructure`

- Wire Task 4 step 6–8 through the existing port methods (`createAccessLink`,
  `revokeAccessLink`). No new port surface needed. Ensure the newest record's `instanceId` + link info
  are carried into the new snapshot (`InvenioRdmResourceMetadata` fields already exist).

### Task 6 — Notifications: combined summary email

**Module:** `project-management`

- `AssociatedDatasetsSyncedPolicy` (subscribes `AssociatedDatasetsSyncedEvent`).
- `InformProjectCollaboratorsAboutDatasetSync` directive: resolve collaborators excluding the actor
  (`projectAccessService.listCollaborators` — all roles), enqueue **one** JobRunr job per member that
  renders a single email listing all `UpdatedRecord`s (title, PID, old→new version, access flips) + link
  to the project (reuse `appContextProvider.urlToProject`).
- `Messages`: add `datasetsSyncedToProject(addressee, projectTitle, List<UpdatedRecord>, projectUrl)`
  template (mirror `datasetConnectedToProject`).

### Task 7 — UI: buttons and write-gating

**Module:** `datamanager-app`

- `ConnectedResourcesComponent`: per-card **Sync** button (next to Remove; enabled for WRITE users only —
  `userPermissions.editProject` already drives `setWriteAllowed`), **Sync All** button in the action bar
  (WRITE-gated, hidden/disabled otherwise).

### Task 8 — UI: `SyncResultsSidebar`

**Module:** `datamanager-app`

- New sliding-panel component modeled on `ConnectDatasetSidebar`:
  - Opens as soon as a sync starts (single Sync or Sync All).
  - One row per dataset; status flips live as `Flux` responses arrive (pending → UPDATED / UP_TO_DATE /
    WARN_ACCESS / FAILED) with inline guidance text:
    - CREDENTIAL_REQUIRED: "Provider must be configured first — add a *[instance]* token in External Providers."
    - CREDENTIAL_INSUFFICIENT: "Cannot be synced — your token does not grant access to this restricted record."
    - ACCESS_LINK_REFRESH_FAILED: "Cannot be synced — only the record owner can refresh the access link."
    - RECORD_NOT_FOUND: "Record no longer exists on the source. Connection kept."
    - SYNC_FAILED: generic transient failure (details logged).
  - Updated rows show the new version; access-flip rows show the access change.
  - Summary header: "X updated · Y up to date · Z not synced".
  - Footer: "Done" (closes); a hint linking to account → External Providers for credential guidance.
  - Fires `DatasetsSyncedEvent` on completion so `ConnectedDatasetsMain` refreshes the card list.
- Toast properties `dataset.sync.*` in `toast-notifications.properties` (minimal — the sidecar carries
  the detail; a toast only announces the sidecar outcome, e.g. failure to start).

### Task 9 — Parameterization / properties

- No new `application.properties` keys anticipated (instances already configured via
  `qbic.external-service.invenio-rdm.instances`). Verify none needed at implementation time.

### Task 10 — Tests (Spock, following existing specs)

**Modules:** `project-management`, `project-management-infrastructure`, `datamanager-app`

- `InvenioRdmDatasetSourceResolveLatestSpec.groovy` — 302/follow, is_latest true/false, 404 empty,
  401/403 mapping, token zeroing, no-token public-metadata success.
- `AssociatedDatasetServiceSyncSpec.groovy` — happy path, version bump restricted (link create/commit/
  revoke-old), link-create failure (hard fail, nothing written), persist-failure rollback of new link,
  no-token short-circuit, dup-PID guard, UP_TO_DATE no-op, error mapping.
- `AssociatedDatasetsSyncedEventSpec` / directive spec — event emitted only when ≥1 changed; one email
  per collaborator excluding actor; combined listing.
- `AssociatedDatasetSyncSpec.groovy` (aggregate) — change-diff semantics.
- UI: component test for SyncResultsSidebar row state transitions (per existing Vaadin test conventions).

---

## Edge Cases

| Case | Handling |
|---|---|
| Record deleted on source (404) | Per-row failure "record no longer exists"; **connection kept** — user decides manually (no auto-removal) |
| Version bump collides with a separately-connected version (dup PID) | Guard before commit: if another active connection carries the target PID, skip + distinct outcome (do not create duplicate connections) |
| Concurrent syncs by two users (same dataset) | Last-write-wins; no `@Version` optimistic locking in v1 — acceptable at project scale (<100 datasets). Revisit if needed |
| Legacy rows without `instanceId` / `parentHandle` / `accessLinkId` | First sync resolves `parent.id` from the stored record and persists `parentHandle`; link revocation skips null link ids (existing behavior) |
| Embargo lifted / access-status flip | Counts as UPDATED; email notes the access change (AC-3 "notifies users about the update") |
| Public-metadata record with restricted files | Syncs token-less (verified 200 anonymously); files remain gated by the access link on the source — out of scope for metadata sync |
| Rate limiting (429 / 5xx) | Bounded retry per ADR-0002 §7; row only turns FAILED after retries exhaust |
| UI session ends mid-sync | Reactive subscription cancelled via existing `UiHandle` detach pattern; sync itself runs on the server and the email may still be sent |
| Actor == connectedBy | Actor excluded from the email recipient list (existing convention); they see the sidecar |

---

## Governance Items (require human decision/approval)

1. **ADR-0005 (draft):** approve/amend — records version-following, WRITE permission amendment to
   ADR-0003 §5, access-link hard-fail integrity, combined per-trigger summary email (supersedes the
   ADR-0003 sync audience mapping), per-record attempt semantics.
2. **Requirements registry:** stories cite `DATA-R-02` (stale proposal ID) → update issues to `DATA-R-05`.
   Decide whether to add `COMM-R-01` (Project Change Notifications) for the shipped email behavior
   (requires human approval + dedicated PR per AGENTS.md §12 / Requirement Edits).
3. **ADR index (`docs/adr/README.md`):** add the approved ADR-0005 row (only after approval — this plan
   references it as draft).
4. **Story issue updates:** add implementation notes / link tasks to #1470 and #1474 by their stable IDs.