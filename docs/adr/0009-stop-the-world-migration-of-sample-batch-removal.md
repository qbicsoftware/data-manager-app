# 0009 — Stop-the-world migration for the sample batch removal

* Status: proposed
* Deciders: project team (via refinement on FEAT-SAMPLE-BATCH-REMOVAL, #1548 / #1551)
* Date: 2026-09-15

Technical Story: [FEAT-SAMBAT-03 #1551](https://github.com/qbicsoftware/data-manager-app/issues/1551) — parent feature [FEAT-SAMPLE-BATCH-REMOVAL #1548](https://github.com/qbicsoftware/data-manager-app/issues/1548)

## Context and Problem Statement

[ADR-0008](0008-remove-explicit-sample-batch-from-data-model.md) removes the explicit sample batch and
adds a free-text `batch` property plus a `project_id` association to the `sample` table. This changes
the database schema and requires existing `sample_batches` data to be carried into the new shape.

The Data Manager runs on 3 nodes sharing a single database. An earlier assumption required a rolling
deployment where nodes were not all replaced at once, which would have forced a dual-write/dual-read
transition window. It is now **acceptable to shut down all 3 nodes at once**, apply the migration, and
bring them back up together. This ADR pins the migration strategy for that stop-the-world window and
records why no rolling/dual-write strategy is needed.

## Decision Drivers

* **SAMPLE-R-03:** existing batch names and creation/modification dates are preserved on each sample
  after migration, with no data loss.
* **SAMPLE-R-02:** each sample receives a `project_id` backfilled from its experiment's owning project.
* A stop-the-world window is acceptable (all 3 nodes down together), so old nodes never need to read or
  write the legacy tables after the cutover.
* The migration must be **atomic and idempotent** where practical, and must run exactly once on the
  shared database (a single operator-controlled step).

## Considered Options

* [S1] **Stop-the-world, single migration:** shut down all nodes, run one backfill + schema change,
  then start all nodes together. No dual-write; old code is never run against the new schema.
* [S2] **Rolling with dual-write:** keep old nodes reading/writing the legacy `sample_batches` tables
  while new nodes use the new `batch` column; maintain both during a transition window, then backfill
  and drop the legacy tables.
* [S3] **Keep legacy tables indefinitely:** add the new columns but never drop `sample_batches` /
  `sample_batches_sampleid`, always writing both.

## Decision Outcome

Chosen: **S1 — stop-the-world, single migration.**

1. **Maintenance window.** All 3 nodes sharing the database are shut down at once. No application code
   runs against the database during the window, so no dual-write or dual-read compatibility is required.
2. **Single migration step.** Within the window, run one backfill + schema change that, for each sample:
   * copies `sample_batches.batchLabel` → `sample.batch` (via the existing `assigned_batch_id`);
   * copies `sample_batches.createdOn` → `sample.registrationTime`;
   * copies `sample_batches.lastModified` → `sample.lastModified`;
   * backfills `sample.project_id` from the sample's experiment (`experiment → project`).
   The pilot flag (`sample_batches.isPilot`) is dropped. The step then adds the `project_id` foreign key
   to `projects_datamanager` and drops the `sample_batches` and `sample_batches_sampleid` tables.
3. **Execution and idempotency.** The migration is a single operator-controlled step run once on the
   shared database; the app startup `DATAMANAGEMENT_DB_DDL_AUTO` remains `none` (no Hibernate schema
   management). The migration is written to be safe to re-run against already-migrated data (e.g.
   guarded by the presence of the legacy tables) to avoid partial-apply races.
4. **No rolling/dual-write code.** Because old nodes are never up against the new schema, no code in the
   application maintains the legacy tables, and no legacy-tolerant read paths are required. This keeps
   the implementation of ADR-0008 clean.
5. **UI and application release together.** The code implementing ADR-0008 (model + UI) ships in the same
   release as this migration, so the app never runs against a mismatched schema.

### Positive Consequences

* No dual-write or dual-read compatibility code — the implementation is simpler (SAMPLE-R-01/R-03/R-04).
* A single atomic migration is easier to reason about and validate than a rolling transition.
* No drift between the legacy and new representations, since both never coexist for app traffic.

### Negative Consequences

* Requires a coordinated downtime window for all 3 nodes (acceptable per the decision drivers).
* The migration is a one-shot, operator-run step; it must be prepared, tested, and executed carefully.

## Pros and Cons of the Options

### S1 — Stop-the-world, single migration

* Good, because it needs no compatibility code in the application.
* Good, because the migration is a single, testable, atomic step with a clear cutover.
* Bad, because it requires a full downtime window for all nodes (accepted).

### S2 — Rolling with dual-write

* Good, because it avoids a full downtime window.
* Bad, because it requires dual-write and dual-read logic in the application, a compatibility layer, and
  a prolonged transition where the two representations can drift.
* Bad, because it is significantly more complex than needed given the accepted stop-the-world window.

### S3 — Keep legacy tables indefinitely

* Good, because it avoids any destructive migration.
* Bad, because it keeps redundant tables and dual-maintenance forever, and leaves the legacy batch
  concept in the schema that SAMPLE-R-04 aims to remove.

## Links

* Implements [SAMPLE-R-03](../requirements.md) (data preservation)
* Refines [ADR-0008](0008-remove-explicit-sample-batch-from-data-model.md) (data model)
* Refines [FEAT-SAMPLE-BATCH-REMOVAL #1548](https://github.com/qbicsoftware/data-manager-app/issues/1548) and [FEAT-SAMBAT-03 #1551](https://github.com/qbicsoftware/data-manager-app/issues/1551)