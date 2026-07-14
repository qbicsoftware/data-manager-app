# 0002 — InvenioRDM API client design and external credential security

* Status: proposed
* Deciders: project team (interviewed via [`interview-feat-dataset-connection.md`](interview-feat-dataset-connection.md))
* Date: 2026-07-13

Technical Story: [Connect associated InvenioRDM datasets with Data Manager projects](https://github.com/qbicsoftware/data-manager-app/issues/1466) (FEAT-DATASET-CONNECTION)

## Context and Problem Statement

Data Manager needs to talk to external InvenioRDM instances (Zenodo, FDAT) over HTTPS to
**search** records, **resolve** record metadata, and **validate** user-provided Personal
Access Tokens. The InvenioRDM REST API uses per-request token authentication; there is no
session concept. Tokens are user-scoped: each user provides their own tokens for each
instance they want to access.

This introduces a design problem with three intertwined axes:

1. **Port design** — how does the integration fit the codebase's existing patterns? The
   codebase has two reference patterns for external integration: stateful session connectors
   (OpenBIS, with session + keepalive + vault-backed credentials loaded at startup) and
   stateless HTTP ports (TIB Terminology, ROR API). The InvenioRDM integration is neither
   (there is no session with the external service), but it must be instance-parameterised
   because we talk to multiple InvenioRDM instances.
2. **Credential lifecycle** — tokens are per-user + per-instance, added by users at runtime.
   This is neither a pre-wired service account (which the PKCS12 vault is set up for) nor
   a one-way-hashed credential (which Data Manager's own PATs are). They must be **readable
   at runtime**, not just verifiable.
3. **Security boundary** — the production environment is HA (multiple application nodes).
   Credential storage must be safe under concurrent writes on any node, and the plaintext
   token value must never leak across architectural boundaries (into logs, persisted events,
   UI state, cross-layer DTOs, or other users' invocations).

## Decision Drivers

* The InvenioRDM API is stateless; no session concept means no long-lived connector.
* Multiple external instances (Zenodo, FDAT, and future ones) mean the client must be
  parameterised by instance, not a singleton.
* Tokens are user-scoped (not service-account scoped) — each user brings their own.
* HA production environment — shared PKCS12 keystore on a local filesystem is not safe
  under many concurrent runtime writes from multiple users.
* The `DataManagerVault` writes to a local file on every `add()`; HA-safe only for secrets
  that are set at deploy time (like OpenBIS credentials).
* Tokens represent user access to external platforms; loss of a token is a security incident
  for the affected researcher and possibly for the hosted external platform.
* The architecture must make it structurally impossible to accidentally log, serialise, or
  pass the plaintext token through higher layers.
* Existing error-mapping infrastructure (`ErrorMessageTranslationService` +
  `ApplicationException` + `UiExceptionHandler`) should be used for user-visible errors.
* The port design must be source-agnostic at the domain boundary, consistent with the
  generalised `associated-dataset` aggregate in [ADR-0001](0001-associated-datasets-domain-model.md).

## Considered Options

### Port design

* [P1] OpenBIS-style session connector (login → session → HTTP calls on session)
* [P2] Stateless port, instance-parameterised (recommended)
* [P3] Hardcoded single-instance client (Zenodo-only)

### Instance discovery

* [I1] Hardcoded instances in code (only Zenodo + FDAT)
* [I2] Configurable via `application.properties` (admin-controlled)
* [I3] UI-managed instance list (users add arbitrary URLs)

### Token scoping

* [T1] Per-user-per-instance tokens (user brings their own)
* [T2] Platform-wide service-account tokens (institutional token for everyone)
* [T3] Hybrid (per-user default + optional service-account override)

### Credential storage

* [S1] Share the existing PKCS12 vault at runtime (writes go to local file, synced via FS)
* [S2] Master AES-256 key in vault, encrypted token blobs in DB (HA-safe)
* [S3] External secret manager (HashiCorp Vault, AWS Secrets Manager)
* [S4] Plaintext in DB (unacceptable — ruled out)

### Result type strategy

* [R1] Single unified type for search hit and persisted metadata
* [R2] Separate `SearchResult` (transient, paginated) and `ResourceMetadata` (persisted,
  canonical snapshot)

### Decryption boundary

* [D1] Decryption at infrastructure boundary; plaintext never escapes method scope
* [D2] Decrypted token passed as DTO in application layer

## Decision Outcome

**Chosen option: P2 + I2 + T1 + S2 + R2 + D1, with bounded sync retry, existing error
mapping, and deferred strict-vs-lenient parsing policy.**

Concretely:

1. **Port design (P2):** A stateless `DatasetSource` port, instance-parameterised. The caller
   provides `(query, InstanceConfig)` where `InstanceConfig` bundles `baseUrl` and optional
   `AccessToken`. The port is responsible for the HTTP call and returns a result.

2. **Instance discovery (I2):** Instances are configured via `application.properties`:
   ```properties
   # Example config, evaluate other extendable options for external instances
   # No decision on array usage yet
   qbic.external-service.invenio-rdm.instances[0].name=Zenodo
   qbic.external-service.invenio-rdm.instances[0].url=https://zenodo.org
   qbic.external-service.invenio-rdm.instances[1].name=FDAT
   qbic.external-service.invenio-rdm.instances[1].url=https://fdat.uni-tuebingen.de
   ```
   Adding a new instance is an admin config change + redeploy. Never a UI entry (keeps the
   security surface small — no arbitrary URLs).

3. **Token scoping (T1):** Tokens are strictly per-user + per-instance. Each user owns their
   tokens; the system never uses one user's token to fulfil another user's action
   (**never-borrow-credentials principle**). This rule has forensic (InvenioRDM logs truthfully
   represent who initiated each call) and privacy (one user never transparently gains access
   to data their own token wouldn't grant them) implications.

4. **Credential storage (S2):** The existing PKCS12 keystore vault holds **only the master
   AES-256 encryption key** (distributed to each node at deploy time — already HA-safe
   because the existing OpenBIS flow already uses this deployment pattern). Each user's
   InvenioRDM PAT is individually AES-GCM-encrypted (`nonce + ciphertext + tag`) and stored
   as a `VARBINARY` blob in the DB. Any node can decrypt (same key, shared DB). No local-file
   concurrency problem. This is analogous to the existing `PersonalAccessTokenEncoder` pattern
   (which uses PBKDF2 one-way hashing for DM PATs), upgraded to symmetric (reversible)
   encryption because we *need* the plaintext.

5. **Result types (R2):** Two distinct types — `SearchResult` (transient, paginated, drives the
   UI search display via `List<SearchHit>`) and `ResourceMetadata` (sealed hierarchy, persisted
   as the canonical metadata snapshot on the aggregate). `SearchResult` and `ResourceMetadata` overlap heavily in fields because
   InvenioRDM search hits carry most metadata inline, but they differ in purpose: one is
   "what the API returned right now" (transient), the other is "what we persist as the
   snapshot of truth."

6. **Decryption boundary (D1):** The plaintext token value **never crosses an architectural
   layer boundary**. It exists only within a `char[]` local variable in the infrastructure
   adapter method that performs the HTTP call, and is zeroed after use (`Arrays.fill(token, '\0')`
   in a `finally` block). The method returns only the result of the HTTP call
   (`SearchResult` or `ResourceMetadata`), never the token.

7. **Retry policy:** Bounded synchronous retry (3 attempts, exponential backoff, 5-second
   ceiling) for **transient errors only**: HTTP 5xx, network timeouts, HTTP 429
   (honouring the `Retry-After` header if present). No retry on 401/403/404 — these are
   access/credential semantics, not transient failures.

8. **Error surface:** User-visible errors use the existing
   `ErrorMessageTranslationService` + `ApplicationException` + `UiExceptionHandler` pipeline.
   Raw upstream errors go to server logs only. No correlation IDs are carried to the user
   (no existing concept for them; users don't copy from toasts).

9. **Credential status updates:** The `status` field of a stored credential (`VALID`,
   `INVALIDATED`, `UNKNOWN`) is updated **only on explicit verification** (user clicks "Add"
   or "Validate" in the UI). A failed sync due to 401/403 does **not** silently update the
   credential status; it surfaces only an error to the user.

10. **Parsing strictness (deferred):** The strict-vs-lenient JSON parsing question "for sync"
    is **deferred** — it must be resolved before the sync logic is implemented, but is not
    architectural in nature. Suggested policy (to be confirmed at implementation time):
    lenient on search (preview, transient), strict on sync (preserve existing metadata if
    the InvenioRDM response is not parsable in an expected format — don't silently corrupt
    the snapshot).

### Positive Consequences

* Stateless port fits the codebase's established stateless-HTTP-adapter pattern (TIB, ROR).
* No new external integration (no HashiCorp Vault); leverages the existing PKCS12 vault and
  its HA distribution pattern.
* The `DatasetSource` port is source-agnostic, consistent with the generalised aggregate
  design — adding a LIMS source is an adapter-level change.
* HA-safe: every node can read and decrypt tokens using the shared master key in the DB.
* Decryption boundary rule makes accidental token bleed a structural impossibility: the
  plaintext type (`char[]`) and scope (single method local) are the enforcement mechanism.
* The bounded retry policy limits runaway load on InvenioRDM (especially important for rate
  limits on anonymous endpoints).
* Separation of `SearchResult` and `ResourceMetadata` keeps the persisted snapshot decoupled
  from whatever InvenioRDM's search response happens to include today.

### Negative Consequences

* Users must maintain their own tokens per instance, and must re-add them when tokens expire
  or are revoked on the external platform. UX must surface this clearly.
* The master-key-from-vault pattern requires ops to deploy the same keystore to every HA node
  — a dependency on deployment discipline.
* Decryption-boundary discipline is a **convention**, not a language-level guarantee. It must
  be reinforced in code review. There is no compiler-enforced barrier.
* Separation of search result from persisted metadata means more types to maintain, though
  the overlap is large and the benefit is clear.
* Retry-and-fail on 401/403 gives a "transient-looking" transient failure for a credential
  problem. The error message must clearly distinguish between "transient (try again)" and
  "credential invalid (re-enter token)".

## Pros and Cons of the Options

### P1 — OpenBIS-style session connector

* Good, because it is a familiar pattern in this codebase.
* Bad, because InvenioRDM is not session-based; a session abstraction would be fictitious and
  add operational complexity.
* Bad, because it is inherently per-instance singleton; doesn't map to per-user token scoping.

### P2 — Stateless `DatasetSource` port, instance-parameterised

* Good, because it matches the actual API contract (per-request stateless HTTP).
* Good, because it is source-agnostic at the port level.
* Good, because it accommodates multiple instances trivially.
* Bad, because it is a new pattern relative to the OpenBIS connector (slight familiarity cost
  for maintainers).

### I2 — Instances via `application.properties`

* Good, because it is admin-controlled (no arbitrary URL injection via UI).
* Good, because it fits the existing external-integration configuration pattern
  (`qbic.external-service.*.*`).
* Bad, because adding a new instance requires redeploy. For the known instances (Zenodo, FDAT)
  this is fine.
* Bad, because ops cannot update instances from within the running application.

### T1 — Per-user-per-instance tokens only

* Good, because it is the simplest mental model and eliminates credential-borrowing risk.
* Good, because it aligns InvenioRDM audit logs ("user X performed action Y") with DM's
  logical model.
* Bad, because each user must maintain their own token; no shared institutional token path.
* Bad, because a user leaving the project can orphan a connected dataset's sync path (mitigated
  by the soft-delete + historical-snapshot retention).

### S2 — Master key in vault + AES-GCM-encrypted blobs in DB

* Good, because it eliminates the local-file concurrency problem of the existing PKCS12 vault
  for runtime-written entries.
* Good, because any HA node can decrypt using the shared master key + the shared DB.
* Good, because the PKCS12 vault remains consistent with the existing OpenBIS secret-loading
  pattern (same deployment discipline).
* Bad, because it introduces a symmetric-encryption layer on top of the vault rather than
  using the vault directly for token storage.

### R2 — Separate `SearchResult` and `ResourceMetadata`

* Good, because search (transient, paginated, UI-driving) and persistence (canonical snapshot)
  are fundamentally different concerns with different lifetimes and ownership.
* Good, because the persisted type is source-typed (sealed hierarchy) while search hits are
  source-agnostic previews.
* Bad, because overlapping field sets across two types adds maintenance surface.

### D1 — Decryption boundary at infrastructure, plaintext never escapes scope

* Good, because token bleed into logs, UI state, domain events, and application-layer DTOs
  is made structurally impossible (the plaintext simply doesn't exist at those layers).
* Good, because the enforcement mechanism is a local `char[]` variable + `finally`-block
  zeroing — deterministic and auditable.
* Good, because it reinforces the same invariant across the entire codebase.
* Bad, because it is a **convention**, not a language-enforced barrier. Requires code review.
* Bad, because `char[]` is slightly more awkward than `String` for Java developers accustomed
  to `String` APIs.

## Links

* Depends on [ADR-0001](0001-associated-datasets-domain-model.md) — the `DatasetSource`
  port is the external-facing half of the aggregate defined there.
* Refined by [ADR-0003](0003-connection-lifecycle-stewardship.md) — lifecycle semantics
  (sync, notifications, ACL) build on this port and credential storage.
* Related to [ADR-0004](0004-fair-signposting-deferred.md) — InvenioRDM Signposting is
  deferred; this ADR's REST-based client is the current integration mechanism.
