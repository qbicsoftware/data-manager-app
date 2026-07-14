# 0001 — Domain model and bounded context for associated datasets

* Status: approved
* Deciders: project team (interviewed via [`interview-feat-dataset-connection.md`](interview-feat-dataset-connection.md))
* Date: 2026-07-13

Technical Story: [Connect associated InvenioRDM datasets with Data Manager projects](https://github.com/qbicsoftware/data-manager-app/issues/1466) (FEAT-DATASET-CONNECTION)

## Context and Problem Statement

Data Manager operates as a hub for project data. Beyond first-class data types (raw data via OpenBIS,
measurement metadata), researchers need to **associate** additional external data resources with
their projects — data they cannot or do not want to maintain inside Data Manager as first-class
datasets. The first concrete source is InvenioRDM (Zenodo, FDAT), but future sources (LIMS,
other remote resources) are anticipated.

Two pre-existing design pressures complicate the introduction of this concept:

1. The `project-management` bounded context already contains a `dataset` package (under
   `project-management-infrastructure/dataset/`) that handles **OpenBIS raw measurement data**
   (`LocalRawDataset*`, `RemoteRawData*`). That package is conceptually unrelated to the new
   concept of "associated datasets" and must not be conflated.
2. The project uses a source-agnostic domain event dispatch infrastructure (`DomainEvent` +
   `DomainEventDispatcher`) and a sealed, source-typed extension pattern for value objects is
   desirable to avoid InvenioRDM-specific leakage into the aggregate.

The question is: how do we model **AssociatedDataset** so it (a) fits naturally in
`project-management`, (b) avoids the package name collision, (c) is source-agnostic at the
domain boundary, (d) supports a clean persistence strategy, and (e) can scale if assumptions
grow.

## Decision Drivers

* Bounded context ownership — the concept is inherently project-scoped; `project-management` is
  the only candidate context among the three.
* Product framing — the stakeholders use the term "associated datasets" deliberately, signalling
  a generalised, open-ended concept.
* Avoid naming collision with existing OpenBIS `dataset` package.
* Source-agnostic aggregate design — aggregate must not embed InvenioRDM-specific concepts
  (DOIs, PIDs, communities) at the domain boundary.
* Storage ergonomics — universal fields need straightforward SQL for sorting/filtering
  (stories #1476/#1477); source-specific fields are stored opaquely.
* Scale assumption — a project is expected to have fewer than ~100 associated datasets; at this
  scale, JSON-path extraction is fast enough that index optimisation is deferred.
* Evolution path — the storage design must have a documented path forward when assumptions
  change (e.g., cross-project API endpoints emerge).
* Data stewardship — soft delete (not hard delete) preserves the connection as a tombstone in
  the `REMOVED` state for audit purposes; there is currently no user story exposing removed
  connections in the UI.
* No event sourcing / no snapshot history — stakeholder confirmed this is not required.

## Considered Options

* [Option A] New bounded context (e.g., `dataset-connection`) dedicated to this concept
* [Option B] Package `associated-dataset` within `project-management`, source-agnostic aggregate
  + sealed-metadata hierarchy, universal columns + MariaDB `JSON` column for source-specific
  fields
* [Option C] Package `associated-dataset` within `project-management`, InvenioRDM-specific
  aggregate (no generalisation) — simplest for v1, risky for long-term evolution
* [Option D] Full event sourcing for lifecycle — events as source of truth, projection-only
  aggregate
* [Option E] Aggregate + append-only event log (O2) — dual-write to current state + event log
  table

## Decision Outcome

**Chosen option: B with soft delete.** Package `associated-dataset`
within the `project-management` bounded context; source-agnostic aggregate root carrying
`source_type`, `external_handle`, connection state, and universal fields as regular columns;
source-specific metadata encapsulated in a sealed `ResourceMetadata` hierarchy stored in a
MariaDB `JSON` column; connection removal is a soft-delete transitioning to `state = REMOVED` (tombstone retained for audit, no UI exposure in v1). No event sourcing; no event log table.

This decision resolves five sub-decisions:

1. **Bounded context** → `project-management`
2. **Package name** → `associated-dataset` (distinct from existing OpenBIS `dataset` package)
3. **Generalisation posture** → source-agnostic aggregate root + sealed per-source metadata
   hierarchy
4. **Storage strategy** → universal fields as regular columns (for SQL ergonomics: `ORDER BY`,
   `WHERE`, joins); source-specific fields in a MariaDB `JSON` column (not PostgreSQL `JSONB`,
   which MariaDB does not support)
5. **Connection removal semantics** → soft delete; tombstone retained; audit-only in v1

### Positive Consequences

* Package `associated-dataset` is self-describing and cleanly isolated from the OpenBIS raw-data
  code, removing any risk of conflation.
* Adding a new external source (e.g., LIMS) is a straightforward extension: add a
  `SourceType` enum value, implement the corresponding `ResourceMetadata` subtype, add an
  adapter. The aggregate root shape does not change.
* Universal columns give clean SQL for sort/filter without JSON-path gymnastics.
* Explicit scale assumption (`~100 datasets per project`) makes the performance evolution path
  a deliberate choice when revisited, not a surprise refactor.
* Soft-delete tombstone preserves audit provenance without adding a separate history table.
* No event sourcing avoids CQRS, snapshot strategies, and event-versioning discipline that
  the project has no foundation to support today.

### Negative Consequences

* Column names (`source_type`, `external_handle`) are more generic than InvenioRDM-specific
  names (`doi`), making individual-row queries slightly less direct (filter on `source_type`
  is required to isolate InvenioRDM rows).
* JSON-path queries on source-specific fields (e.g., "find all InvenioRDM datasets where
  community = X") rely on MariaDB's `JSON_EXTRACT` and are not indexed — acceptable at
  <100 rows per project, not acceptable at larger scale without the evolution path.
* Soft-delete adds a `state != 'REMOVED'` filter to the default query path; if this is
  forgotten in one place, dead connections could surface.
* Future evolution (generated columns, CQRS) is available but requires active maintenance when
  assumptions are violated.

## Pros and Cons of the Options

### Option A — New bounded context (e.g., `dataset-connection`)

* Good, because it gives the concept a clearly bounded home with its own identity.
* Bad, because `project-management` is already the natural home — creating a new context adds
  cross-context integration complexity that is not justified.
* Bad, because it fragments the project-level aggregate (`Project` + its associated datasets
  live in separate contexts).

### Option B — `associated-dataset` package, source-agnostic aggregate, universal columns + JSON

* Good, because it isolates the concept from the existing `dataset` package (OpenBIS) without
  conflation.
* Good, because the aggregate is source-agnostic — adding LIMS or another source is an
  extension, not a refactor.
* Good, because universal columns + JSON split matches the sort/filter/display needs at the
  stakeholder-specified scale.
* Good, because soft delete preserves audit provenance without a separate history table.
* Bad, because the aggregate and DB schema carry generic field names (`external_handle`) instead
  of source-specific ones (`doi`, `pid`), which is slightly less intuitive for InvenioRDM-only
  readers.
* Bad, because JSON-path indexing must be explicitly added (via generated columns) when scale
  assumptions are violated.

### Option C — InvenioRDM-specific aggregate within `project-management`

* Good, because it is the simplest v1 design — no generalisation to pay for upfront.
* Bad, because the product framing explicitly anticipates future sources; this option requires
  a painful aggregate-level refactor whenever a second source type is added.
* Bad, because InvenioRDM-specific concepts (DOIs, PIDs, communities) leak into the aggregate
  root, violating the product's generalised framing.

### Option D — Full event sourcing for lifecycle

* Good, because it provides a complete audit trail and rebuild-any-point-in-time capability.
* Good, because the project has half-built event-sourcing infrastructure (`DomainEvent`,
  `DomainEventDispatcher`).
* Bad, because this project has no tooling for the write-side/read-side split, snapshots, or
  event-versioning discipline that full event sourcing requires.
* Bad, because stakeholders explicitly rejected this need as unnecessary.
* Bad, because it is over-investment for a feature with no audit-trail requirement.

### Option E — Aggregate + append-only event log (O2)

* Good, because it delivers audit history without CQRS or snapshot complexity.
* Good, because it keeps the upgrade path to full event sourcing (Option D) open if the need
  ever materialises.
* Bad, because it introduces dual-write consistency risk (aggregate + log must stay in sync).
* Bad, because the stakeholder confirmed audit history is not required.
* Bad, because it adds a storage table and operational complexity that delivers no v1 value.

## Links

* Related by [ADR-0002](0002-invenio-rdm-api-client-credentials.md) — the `DatasetSource`
  port contract defined there is the external-facing half of this aggregate's persistence.
* Related by [ADR-0003](0003-connection-lifecycle-stewardship.md) — lifecycle semantics
  (soft-delete, sync, notifications) build on this aggregate.
* Superseded-in-part by [ADR-0004](0004-fair-signposting-deferred.md) — InvenioRDM
  Signposting integration is deferred; this ADR's REST-based aggregate is the v1 decision.
