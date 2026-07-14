# Interview: Architectural Decisions for FEAT-DATASET-CONNECTION

> **Feature:** [Connect associated InvenioRDM datasets with Data Manager projects](https://github.com/qbicsoftware/data-manager-app/issues/1466)
> **Purpose:** Aligned understanding of architectural decisions before implementation, to be captured in ADRs.
> **Started:** 2026-07-13
> **Status:** 🟡 In Progress

6a## Interview Timeline

| # | Topic / Question | Status | Decision / Notes |
|---|---|---|---|
| 1 | Bounded context ownership | ✅ Resolved | **`project-management`** bounded context. Associated-dataset concept is inherently tied to a project. |
| 2 | Package / domain naming | ✅ Resolved | **`associated-dataset`** — deliberately generalised, distinct from existing OpenBIS `dataset` package. Product language: users connect "associated data" beyond first-class raw data. |
| 3 | Domain model generalisation strategy | ✅ Resolved | Source-agnostic aggregate (identity + lifecycle) + sealed per-source metadata hierarchy. Aggregate columns carry generic identity fields (`source_type`, `external_handle`, connection state). Source-specific metadata lives in a sealed-value-object type hierarchy (`ResourceMetadata`). |
| 3a | Storage strategy for source-specific metadata | ✅ Resolved | **Option 2:** Universal fields as regular columns (`title`, `publishedAt`, `accessLevel`, `accessLink`) — needed for querying/sorting per stories #1476/#1477. Source-specific fields in a **JSONB column**. |
| 4 | Integration mechanism — InvenioRDM API client design | ✅ Resolved | Stateless `DatasetSource` port, instance-parameterised. **Per-user-per-instance** token scoping (no service-account tokens). Instances configurable via `application.properties` (admin-controlled, no free-form UI entry = small security surface). Separate `SearchResult` (transient, paginated, UI display) and `ResourceMetadata` (persisted value object). |
| 5 | Credential management — where and how are tokens stored? | ✅ Resolved (pending confirmation of 5a–5c) | **Three-layer strategy:** At-rest = AES-GCM-encrypted in DB column. In-transit = HTTPS only. In-memory = `char[]` confined to the infrastructure adapter method performing the HTTP call, zeroed after use. **Decryption boundary rule:** plaintext token value never crosses an architectural layer boundary. See ADR candidate 0003. Open sub-q: 5a (background sync fit), 5b (JobRunr fit), 5c (master key source). |
| 6 | Synchronisation model | ✅ Resolved | **User-initiated only for v1** (no background JobRunr sync for now — future enhancement path). Sync uses the invoking user's own token (**never-borrow-credentials rule** — see ADR 0003). Public records sync without token. Restricted records: invoking user must have their own token; if absent or insufficient, user is informed. Historical metadata + `connectedBy` + `lastSyncedAt` preserved permanently. Sync updates: metadata + access status + notify project members. |
| 7 | Notification mechanism | ✅ Resolved | **Actor sees Vaadin toast** (synchronous, no infrastructure). **Other project members receive email** via `subscription-provider` (SMTP), driven by domain events emitted from application layer. `NotificationService` + `Exchange` NOT used (dead scaffolding, never reaches users). Cross-context Artemis deferred (no v1 need). Caveats: project-member email lookup needs to be built; email delivery not guaranteed (existing risk); actor excluded from recipient list by design. |
| 8 | Database schema | ✅ Resolved | **Soft delete** on connection removal (`state = REMOVED`). Two new tables in `data-management` datasource: `associated_dataset` (source_type + external_handle, state, universal columns for title/access_level/access_link/published_at, `metadata_json` typed **`JSON` (MariaDB)** for source-specific fields, connected_by_user_id as soft FK to identity, linked_experiment_id as hard FK, connected_at, last_synced_at) + `user_invenio_rdm_credential` (user_id, instance_key plaintext, encrypted_token VARBINARY AES-GCM, status, timestamps; unique on (user_id, instance_key)). Instances NOT stored in DB (application.properties config). **Schema migration pattern**: incremental script `sql/migrations/...` added alongside `complete-schema.sql` (establishes pattern for the project; first entry of this type). **No event sourcing / no event log / no snapshot history** (stakeholder call). **Explicit design assumption:** ~100 associated datasets per project (stated in ADR). **Performance evolution path** (for when assumptions change — e.g., cross-project API endpoints): (a) generated virtual column + index on JSON path, (b) promote field to regular column, (c) CQRS read model, (d) EHCache application cache. MariaDB views do NOT improve JSON-path performance (no materialized views); views are for query ergonomics only. |
| 9 | Security & ACL implications | ✅ Resolved | (1) Dataset connections **inherit project ACL** — no per-connection ACL entries. (2) Role mapping: VIEW=READ on project, CONNECT/REMOVE=WRITE, SYNC=READ, own-credential-management=authenticated. (3) Principle for ADR (reframed): "Visibility of a connected dataset follows project ACL. Operations requiring external authentication (e.g., sync of restricted records) succeed if the invoking user has configured the instance with a valid token granting sufficient access to the external record; otherwise they fail." (4) Soft-deleted rows retained **for audit purposes**; there is **no user story** exposing removed-connection visibility in the UI — that is a future decision if it arises. |
| 10 | UI placement | 🚫 Out of scope — design concern, not architectural | UI placement (tab / sidebar / page) is a design-layer decision delegated to the design system / UX, not a structural architectural decision. The `AssociatedDatasetsDemoV2` prototype is a UX artifact for discussing user stories with users, not an architectural reference. This falls outside ADR scope. Principle: ADRs capture structural invariants; UI placement does not constrain architectural evolution. |
| 11 | Error handling & resilience | ✅ Resolved | (11a) Bounded sync retry: 3 attempts, exponential backoff, 5s max. For transient errors only (5xx, timeout, 429). Honour `Retry-After` header. No retry on 401/403/404. (11b) No correlation ID. Use existing `ErrorMessageTranslationService` + `ApplicationException` + `UiExceptionHandler` pipeline. Raw upstream errors in server logs only. (11c) **Deferred** — must be resolved before sync logic implementation. Open q: strict vs. lenient parsing on sync, lenient on search. (11d) Credential `status` updates only on explicit verification calls (token add/validate from UI), never from a failed sync call. |
| 12 | FAIR Signposting future-proofing | ✅ Resolved | Dedicated ADR: (a) decision to defer; (b) reason = upstream serialization bug in InvenioRDM; (c) horizon ≈ 1-2 years. Minimal commitment: existing port design naturally accommodates future adapter. ADR is decision-only, no structural-commitment chapter. |

---

## ADR Plan (to be drafted after interview)

| # | Title | Scope | Status |
|---|---|---|---|
| 0001 | Bounded context, packaging, and domain model for associated datasets | Questions 1, 2, 3, 3a — the domain-modelling cluster | 📋 Ready to draft (pending final sign-off on storage choice, question 3a) |
| 0002 | InvenioRDM API client — stateless port, instance-parameterised | Question 4 | ✅ Ready to draft (pending Q5 — where tokens live) |
| 0003 | InvenioRDM credential storage strategy | Question 5 | 📋 Pending interview |
| 0004 | Dataset connection synchronisation model | Question 6 | 📋 Pending interview |
| 0005 | Project change notification for dataset connection events | Question 7 | 📋 Pending interview |
| ... | *(additional ADRs as discovered)* | | 📋 |

---

## Key Context (gathered from codebase)

- **Feature issue:** #1466 — 13 child stories (#1467–#1479), all 🔴 Open
- **Target platforms:** Zenodo, FDAT (both InvenioRDM instances; future: LIMS, other remote resources)
- **Integration approach:** Conventional InvenioRDM OpenAPI (FAIR Signposting dropped due to upstream bug)
- **Existing demo:** `AssociatedDatasetsDemoV2.java` (sidebar prototype; uses "Associated Datasets" terminology)
- **Existing `dataset` package (`project-management-infrastructure/dataset/`):** OpenBIS raw data only — `LocalRawDataset*`, `RemoteRawData*`. Distinct concept. Must NOT be conflated.
- **Domain event pattern:** `DomainEvent` subclasses + `DomainEventDispatcher` (in-process)
- **External integration reference patterns:** OpenBIS (`infrastructure/sample/openbis/`) uses session + connector + vault + mock for `development` profile; TIB/ROR are stateless HTTP clients
- **Notification pattern:** `NotificationService` via `MessageBusSubmission` (Artemis/JMS pub-sub)
- **Secret storage:** PKCS12 keystore vault (used for OpenBIS + personal access tokens)
- **Existing data sources pattern:** Two datasources (`data-management`, `finance`) with separate entity managers

---

## InvenioRDM Client Design (Q4, recorded for ADR 0002)

```java
interface DatasetSource {
    SearchResult search(String query, InstanceConfig instance);
    Optional<ResourceMetadata> resolve(String externalHandle, InstanceConfig instance);
    boolean validateToken(InstanceConfig instance);
}

record SearchResult(List<SearchHit> hits, int totalCount) {}
record InstanceConfig(String baseUrl, Optional<AccessToken> token) {}

// Transient — from search hits
record SearchHit(
    String externalHandle, String title, String creator, String resourceType,
    String community, AccessStatus accessStatus, String accessLink, Instant publishedAt
) {}

// Persisted — sealed value object on AssociatedDataset aggregate (see ADR 0001)
record InvenioRdmResourceMetadata(...) implements ResourceMetadata {}
```

**Instance list** is configured via `application.properties`:
```properties
qbic.external-service.invenio-rdm.instances[0].name=Zenodo
qbic.external-service.invenio-rdm.instances[0].url=https://zenodo.org
qbic.external-service.invenio-rdm.instances[1].name=FDAT
qbic.external-service.invenio-rdm.instances[1].url=https://fdat.uni-tuebingen.de
```

---

## Key Product Insights (recorded from interview)

1. "Associated dataset" is **deliberately generalised**. Data Manager is a hub; beyond first-class data types (raw data via OpenBIS, measurement metadata), users associate other external data they cannot maintain inside Data Manager. InvenioRDM is the first concrete source; LIMS and other remote resources are plausible future sources.
2. The aggregate design must not narrow the concept to InvenioRDM-specific structures (DOIs, PIDs) at the domain boundary.
3. Source-agnostic aggregate root + sealed per-source metadata hierarchy is acceptable: minimal investment now (two things: `source_type + external_handle` instead of `doi`; `DatasetSource` port interface) avoids aggregate-level refactoring later.
4. For DB storage: universal fields as regular columns (query/sort/filter needs), source-specific fields in JSONB.

---

## Concrete Domain Example (recorded for ADR 0001 context)

```java
// ── Aggregate: identity + lifecycle (generic) ──
@Entity
class AssociatedDataset {
    AssociatedDatasetId id;
    ProjectId projectId;
    SourceType sourceType;              // enum: INVENIO_RDM
    String externalHandle;              // DOI / LIMS ID / URL — opaque to root
    DatasetConnectionState state;       // CONNECTED, SYNCING, ERROR, REMOVED
    UserId connectedBy;
    ExperimentId linkedExperiment;      // optional
    Instant connectedAt;
    Instant lastSyncedAt;
    ResourceMetadata resourceMetadata;  // sealed, source-typed
}

// ── Source-specific metadata: sealed hierarchy ──
sealed interface ResourceMetadata
    permits InvenioRdmResourceMetadata /*, LimsResourceMetadata */ {
    String title();
    String accessLevel();
    String accessLink();
    Instant publishedAt();
}

record InvenioRdmResourceMetadata(
    String title, String pid, String version,
    String accessLevel, String accessLink, Instant publishedAt,
    String resourceProvider, String creator, String resourceType, String community
) implements ResourceMetadata {}

// ── DB: associated_dataset ──
// PK + project_id + source_type + external_handle + state + connected_by
//   + linked_experiment + connected_at + last_synced_at
//   + title + published_at + access_level + access_link     ← universal columns
//   + metadata_json   (JSONB)                               ← source-specific
```

---

*This file is updated incrementally as the interview progresses.*

## Progress bar

- [x] Q1–Q3, Q3a: Domain modelling cluster → ADR 0001
- [x] Q4: Integration mechanism → ADR 0002
- [x] Q5: Credential storage → ADR 0003
- [x] Q6: Synchronisation model
- [x] Q7: Notification mechanism
- [x] Q8: Database schema
- [x] Q9: Security & ACL
- [ ] Q10: UI placement
- [x] Q11: Error handling & resilience
- [x] Q12: FAIR Signposting future-proofing
