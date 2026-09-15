# 0008 — Remove the explicit sample batch from the data model

* Status: proposed
* Deciders: project team (via refinement on FEAT-SAMPLE-BATCH-REMOVAL, #1548 / #1549 / #1550 / #1551 / #1552)
* Date: 2026-09-15

Technical Story: [FEAT-SAMBAT-01 #1549](https://github.com/qbicsoftware/data-manager-app/issues/1549) · [FEAT-SAMBAT-02 #1550](https://github.com/qbicsoftware/data-manager-app/issues/1550) · [FEAT-SAMBAT-03 #1551](https://github.com/qbicsoftware/data-manager-app/issues/1551) · [FEAT-SAMBAT-04 #1552](https://github.com/qbicsoftware/data-manager-app/issues/1552) — parent feature [FEAT-SAMPLE-BATCH-REMOVAL #1548](https://github.com/qbicsoftware/data-manager-app/issues/1548)

## Context and Problem Statement

The Data Manager models an explicit **sample batch** as a first-class entity: a `Batch` aggregate
(`sample_batches` table) with a many-to-many join table (`sample_batches_sampleid`), and each `Sample`
carries an `assigned_batch_id`. The batch has no direct project/experiment link in persistence — the
experiment→batch association is derived by querying samples. This makes batches a heavyweight concept
whose only real purpose is a grouping label, and it forces the registration workflow to create a batch
before registering samples.

We want to remove the explicit batch as a first-class entity. Samples should be registered directly
within an experiment of a project, and each sample should carry a free-text `batch` label purely for
backwards compatibility with existing data and downstream tooling. This ADR pins the target data model;
the strategy for migrating existing data is captured separately in [ADR-0009](0009-stop-the-world-migration-of-sample-batch-removal.md).

## Decision Drivers

* **SAMPLE-R-01:** samples are registered directly within an experiment; the batch is optional and not a
  registration prerequisite.
* **SAMPLE-R-02:** every sample is directly associated with both its project and its experiment, so
  project-wide sample queries avoid a transitive lookup through the experiment hierarchy.
* **SAMPLE-R-03:** existing batch information is preserved — the batch name and the creation/modification
  dates carry forward onto the sample. The pilot flag is not needed and is dropped.
* **SAMPLE-R-04:** the explicit batch is no longer exposed in the registration workflow; the UI presents
  samples only, with batch as an optional property.
* The batch currently provides no domain behaviour beyond grouping labels and a pilot marker (which is no
  longer needed); removing the entity reduces schema and orchestration complexity.
* Existing downstream tooling and historical data reference the batch name; a free-text `batch` property
  preserves that without keeping the batch tables.

## Considered Options

### Batch-to-sample mapping

* [M1] Collapse the batch into a free-text `batch` property on the sample; move the batch creation and
  modification dates onto the sample; drop the `Batch` aggregate, `sample_batches`, and
  `sample_batches_sampleid`.
* [M2] Keep the `Batch` aggregate but relink it directly to project/experiment with real FKs; samples
  keep referencing a batch by id.
* [M3] Keep the `Batch` aggregate and tables unchanged; only stop exposing it in the UI.

### Sample–project association

* [P1] Add a `project_id` column on the `sample` table with a real foreign key to `projects_datamanager`,
  coexisting with the existing `experiment_id` (kept as a soft reference).
* [P2] Add `project_id` without a foreign key constraint (matching the existing `experiment_id` /
  `assigned_batch_id` soft-reference pattern).
* [P3] Keep no project column on the sample; derive the project transitively via the experiment.

## Decision Outcome

Chosen: **M1 + P1.**

1. **Batch becomes a sample property (M1).** Remove the `Batch` aggregate and its domain services/events
   (`BatchRegistered`, `BatchUpdated`, `BatchDeleted`). Add a free-text `batch` column on `sample`.
   Move the batch metadata onto the sample:
   * `sample_batches.createdOn` → `sample.registrationTime`
   * `sample_batches.lastModified` → `sample.lastModified`
   The pilot flag (`sample_batches.isPilot`) is not needed and is dropped. Drop the `sample_batches` and
   `sample_batches_sampleid` tables. The existing `assigned_batch_id` column on `sample` is replaced by
   the `batch` free-text column.
2. **Direct project association (P1).** Add `project_id` on the `sample` table as a real foreign key to
   `projects_datamanager(projectId)`. Keep the existing `experiment_id` association (soft reference)
   unchanged, so a sample belongs to exactly one project and exactly one experiment. The `project_id`
   is backfilled from each sample's experiment during migration (see ADR-0009). This denormalised
   association lets project-wide sample queries resolve directly instead of joining through
   `experiment → project`.
3. **Registration within an experiment.** Samples are still registered within an experiment
   (unchanged); only the explicit batch-creation step is removed from the registration flow
   (`SampleRegistrationServiceV2` currently creates a batch before samples and adds samples to it —
   that pre-step and the batch-scoped edit/delete operations are removed).
4. **UI becomes samples-only.** Remove `BatchDetailsComponent`, `RegisterSampleBatchDialog`,
   `EditSampleBatchDialog`, and the batch event listeners in `SampleInformationMain`. The sample grid
   shows `batch` as a column (sortable/filterable) when present.

### Positive Consequences

* Simpler data model: one fewer aggregate, two fewer tables, no batch orchestration services, events,
  or UI dialogs.
* Project-wide sample queries become direct (SAMPLE-R-02) instead of a transitive join.
* Registration no longer requires a batch name as a prerequisite (SAMPLE-R-01).
* Historical batch grouping and metadata remain available on each sample (SAMPLE-R-03).

### Negative Consequences

* Batch grouping is now per-sample free text; there is no longer a single batch record to update or
  delete atomically (a batch rename must touch every sample carrying that label).
* The denormalised `project_id` must be kept consistent with the experiment's project; it is set at
  registration and backfilled once during migration.
* Downstream tooling that relied on the `sample_batches` tables must switch to the `batch` property.

## Pros and Cons of the Options

### M1 — Collapse batch into a sample property

* Good, because it removes a whole aggregate and its tables, simplifying schema and orchestration.
* Good, because the free-text `batch` property preserves grouping and historical names on each sample.
* Good, because it directly satisfies SAMPLE-R-01, SAMPLE-R-03, and SAMPLE-R-04.
* Bad, because renaming a batch is no longer a single entity update (must update all samples with that
  label).

### M2 — Keep Batch aggregate, relink with FKs

* Good, because batch metadata stays centralised and batch rename/delete remains atomic.
* Bad, because it keeps the batch entity, tables, and orchestration that SAMPLE-R-04 removes — the
  explicit batch would still exist in the model and UI.

### M3 — Keep tables, hide from UI only

* Good, because it is the least code change.
* Bad, because it leaves the redundant batch aggregate and join table in the model and violates the
  intent of SAMPLE-R-01/R-03 to simplify the model.

### P1 — project_id real FK

* Good, because referential integrity is enforced and the association is explicit.
* Good, because it matches the existing real-FK pattern used by `experimentalGroupId`.
* Bad, because it diverges from the soft-reference pattern of `experiment_id`/`assigned_batch_id`.

### P2 — project_id without FK

* Good, because it matches the existing soft-reference columns.
* Bad, because the association is not enforced and can drift, which is riskier for the primary
  project-wide query path.

### P3 — No project column

* Good, because it changes the schema least.
* Bad, because project-wide sample queries keep doing the transitive join through experiment, which is
  exactly the inefficiency SAMPLE-R-02 exists to remove.

## Links

* Implements [SAMPLE-R-01](../requirements.md), [SAMPLE-R-02](../requirements.md),
  [SAMPLE-R-03](../requirements.md), [SAMPLE-R-04](../requirements.md)
* Refines [FEAT-SAMPLE-BATCH-REMOVAL #1548](https://github.com/qbicsoftware/data-manager-app/issues/1548)
* Refined by [ADR-0009](0009-stop-the-world-migration-of-sample-batch-removal.md) (data migration strategy)