# Features and User Stories Tracker

This document maps active Features and their constituent Stories to the
[Product Requirements Document](prd.md) and the
[Requirements Registry](requirements.md).
It is updated as work progresses and serves as the stakeholder-visible
view of the implementation roadmap.

## Status Legend

| Symbol | Meaning |
|--------|---------|
| 🔴 | Open / Not started |
| 🟡 | In Progress |
| 🟢 | Done |
| ⚫ | Closed / Cancelled |

## Story ID Schema

Stories are identified using a short, human-readable slug derived from their parent Feature:

    FEAT-<SLUG>-<NN>

Where:

- **SLUG** — a concise, abbreviated identifier derived from the parent Feature slug (e.g., `IP-MEAS` for `FEAT-IMMUNOPEPTIDOMICS-MEASUREMENT`)
- **NN** — sequential number, zero-padded to two digits (e.g., `01`, `02`, `05`)

### Example

    FEAT-IP-MEAS-01
    FEAT-IP-MEAS-05

### Rules

- Story IDs are assigned when a story moves from draft (refinement) to approved (ready for implementation).
- Story IDs must be stable and must never be renumbered.
- Tasks reference stories by their stable ID, not by GitHub issue number.
- GitHub issues for stories are updated to carry the stable story ID in the title and body.

## Active Features

### FEAT-IMMUNOPEPTIDOMICS-MEASUREMENT

| Field | Value |
|---|---|
| **Description** | Full lifecycle support for immunopeptidomics measurements — registration, editing, deletion, raw data upload/download |
| **PRD Section** | §3 Scope — Measurement integration; File management |
| **Requirements** | `MEASUREMENT-R-01`, `MEASUREMENT-R-02`, `MEASUREMENT-R-03`, `MEASUREMENT-R-04`, `DATA-R-01`, `DATA-R-02`, `DATA-R-03` |
| **GitHub Feature** | [#1412](https://github.com/qbicsoftware/data-manager-app/issues/1412) |
| **Status** | 🟡 In Progress |

---

### Stories

#### FEAT-IP-MEAS-01 — Register Immunopeptidomics Measurements via Excel Template

| Field | Value |
|---|---|
| **Requirement IDs** | `MEASUREMENT-R-01` |
| **Status** | 🟢 Done |
| **GitHub** | [#1428](https://github.com/qbicsoftware/data-manager-app/issues/1428) |

**User Story**

> As a data provider, I want to register immunopeptidomics measurements via a domain-specific Excel template, so that I can capture domain-specific metadata (MHC antibody, enrichment method, LC column, etc.) for my experiment.

**Acceptance Criteria**

- Given a user is on the measurement registration page, When they select the immunopeptidomics domain, Then a downloadable registration template with the correct columns and Property Information sheet is provided.
- Given a user uploads a filled immunopeptidomics registration template with all mandatory fields valid, When the system validates the sheet, Then all measurements are registered successfully with unique `IP-` prefixed measurement codes.
- Given a user uploads a sheet with a missing mandatory field (e.g., blank `MHC Antibody`), When the system validates the sheet, Then an error identifying the column and row is returned and no records are created.
- Given a user uploads a sheet with an invalid `Organisation URL` or `Prep Date` format, When validation runs, Then a domain-appropriate error is returned and no records are created.
- Given a user uploads a sheet with a non-existent or cross-experiment `QBiC Sample Id`, When validation runs, Then a missing/unknown-id error is returned and no records are created.
- Given a user uploads a sheet with all optional fields blank and all mandatory fields valid, When the system processes the sheet, Then measurements are created successfully.

**Notes & Context**

- The registration template columns must match the immunopeptidomics partner facility specification.
- OpenBIS must be extended with a dedicated object type for immunopeptidomics measurements.

**Tasks**

| # | Title | Status | GitHub |
|---|---|---|---|
| — | Registration of Immunopeptidomics Measurements | 🟢 Done | [#1413](https://github.com/qbicsoftware/data-manager-app/issues/1413) |
| — | Integration Test — Register Immunopeptidomics Measurements | 🟢 Done | [#1437](https://github.com/qbicsoftware/data-manager-app/issues/1437) |

---

#### FEAT-IP-MEAS-02 — Edit Immunopeptidomics Measurements via Pre-filled Excel Template

| Field | Value |
|---|---|
| **Requirement IDs** | `MEASUREMENT-R-02` |
| **Status** | 🟢 Done |
| **GitHub** | [#1429](https://github.com/qbicsoftware/data-manager-app/issues/1429) |

**User Story**

> As a data provider, I want to edit existing immunopeptidomics measurements via a pre-filled Excel template, so that I can update metadata (e.g., comments, instrument changes) without re-registering measurements.

**Acceptance Criteria**

1. **Pre-filled edit template**
   Given a user has selected existing immunopeptidomics measurements to edit, when they choose to download the edit template, then a pre-filled Excel file is produced containing the current metadata values for those measurements, with properties logically grouped in the same familiar structure as the registration template.
2. **Persist valid modifications**
   Given a user has modified modifiable fields in the downloaded template and re-uploads it, when the system processes the file, then the corresponding measurement records are updated with the new values.
3. **Ignore read-only changes**
   Given a user has modified read-only fields in the downloaded template and re-uploads it, when the system processes the file, then those changes are silently ignored without causing errors, and any valid modifications to modifiable fields are still applied.
4. **Require measurement identifier**
   Given a user uploads an edit template containing rows without a measurement identifier, when the system validates the file, then the upload is rejected with a clear error indicating that the identifier is required.
5. **Scope edits to the experiment**
   Given a user uploads an edit template containing measurement identifiers that do not belong to the current experiment, when the system validates the file, then the upload is rejected with a clear error.
6. **Validate mandatory modifiable fields**
   Given a user clears a mandatory modifiable field in the template and re-uploads it, when the system validates the file, then the upload is rejected with clear guidance indicating which required information is missing.
7. **Template reference accuracy**
   Given a user downloads the edit template, when they review the included property information, then it accurately reflects the measurement specification, including property categories, provisioning guidance, and allowed values.

**Notes & Context**

- Editable and read-only fields are defined in the measurement specification; non-editable fields are ignored on re-upload.

**Tasks**

| # | Title | Status | GitHub |
|---|---|---|---|
| — | Editing of Immunopeptidomics Measurements | 🟢 Done | [#1414](https://github.com/qbicsoftware/data-manager-app/issues/1414) |

---

#### FEAT-IP-MEAS-03 — Delete Immunopeptidomics Measurements

| Field | Value |
|---|---|
| **Requirement IDs** | `MEASUREMENT-R-03` |
| **Status** | 🟢 Done |
| **GitHub** | [#1430](https://github.com/qbicsoftware/data-manager-app/issues/1430) |

**User Story**

> As a project member with management rights, I want to delete immunopeptidomics measurements, so that I can remove erroneously registered entries to increase metadata quality of the project.

**Acceptance Criteria**

- Given a user with management rights selects immunopeptidomics measurements for deletion, When they confirm deletion, Then measurements with no attached dataset are deleted successfully.
- Given a user with management rights deletes an immunopeptidomics measurement with an attached dataset, the deletion must fail and the user shall be informed to delete the raw datasets first.
- Given a user with view-only rights views the measurement page, they shall not be able to delete measurements.
- The user shall see a confirmation notification about the amount of successfully deleted measurements.

**Notes & Context**

- Dataset deletion is out of scope; users must remove datasets via the raw data view first.

**Tasks**

| # | Title | Status | GitHub |
|---|---|---|---|
| — | Deletion of Immunopeptidomics Measurements | 🟢 Done | [#1415](https://github.com/qbicsoftware/data-manager-app/issues/1415) |

---

#### FEAT-IP-MEAS-04 — View Immunopeptidomics Measurements in Measurement View

| Field | Value |
|---|---|
| **Requirement IDs** | `MEASUREMENT-R-04` |
| **Status** | 🟢 Done |
| **GitHub** | [#1431](https://github.com/qbicsoftware/data-manager-app/issues/1431) |

**User Story**

> As a project member, I want to see registered immunopeptidomics measurements in the measurement view, so that I can track which measurements exist for my experiment and review their metadata.

**Acceptance Criteria**

- Given a user navigates to the measurement view, When the page loads, Then a dedicated immunopeptidomics tab is visible alongside NGS and proteomics tabs.
- Given the immunopeptidomics tab is active, When measurements exist, Then they are displayed with domain-specific columns: QBiC Sample Id, Sample Name, Measurement Name, Organisation URL, Facility, Sample Mass (mg), Sample Volume, Cycle/Fraction Name, MHC Antibody, MHC Typing Method, Enrichment Method, Instrument, Prep Date, MS Run Date, LCMS Method, LC Column, Data Acquisition, Mass range (m/z), Retention time range (min), Charge range, Ion mobility range (1/k0), Registration Date, Comment.
- Given the immunopeptidomics tab is active, When the page loads, Then the current count of immunopeptidomics measurements is shown.
- Given a user enters a search term in the filter box, When they type, Then the displayed measurements are filtered to show rows where any visible property contains the search term.
- Given no immunopeptidomics measurements exist for the experiment, When the page loads, Then the tab shows an empty state or zero count.

**Notes & Context**

- The measurement view must keep immunopeptidomics data visually distinct from NGS and proteomics data.
- The grid component pattern should mirror the existing flexible grid used for proteomics and genomics.

**Tasks**

| # | Title | Status | GitHub |
|---|---|---|---|
| — | Show the Immunopeptidomics Measurements in the Measurement View | 🟢 Done | [#1417](https://github.com/qbicsoftware/data-manager-app/issues/1417) |

---

#### FEAT-IP-MEAS-05 — View and Access Immunopeptidomics Raw Datasets

| Field | Value |
|---|---|
| **Requirement IDs** | `DATA-R-01`, `DATA-R-02`, `DATA-R-03` |
| **Status** | 🟢 Done |
| **GitHub** | [#1432](https://github.com/qbicsoftware/data-manager-app/issues/1432) |

**User Story**

> As a user with project access, I want to see available immunopeptidomics raw datasets for measured samples and instructions how to access them for detailed investigation and processing.

**Acceptance Criteria**

- Given a user with project access rights accesses the available raw datasets for measured samples, the user is able to see contextual metadata and properties about each raw dataset.
- Given a user with project access rights accesses the available raw datasets for measured samples, the user is able to filter datasets with generic text input.
- Given a user with project access rights accesses the available raw datasets for measured samples, the user is able to see the total number of available raw datasets.
- Given no raw datasets for the selected experiment exist, the user is informed about how to register datasets and that there are no raw datasets registered yet.

**Notes & Context**

- Raw data upload/download backend implementations live in separate repositories (`data-scanner`, `data-download-server`). The Data Manager only consumes the scanned metadata and provides download URLs.

**Tasks**

| # | Title | Status | GitHub |
|---|---|---|---|
| — | Show Immunopeptidomic datasets within the raw data view | 🟢 Done | [#1416](https://github.com/qbicsoftware/data-manager-app/issues/1416) |

---

### FEAT-PAGINATED-LISTS

| Field | Value |
|---|---|
| **Description** | Explicit, paginated display of all large entity collections (projects, samples, measurements, raw datasets), replacing scroll-loaded (endless scrolling) lists, with cross-page selection for bulk actions and URL-restorable list state |
| **PRD Section** | — (UX-driven change; no dedicated PRD section yet) |
| **Requirements** | `USER-R-01`, `USER-R-02`, `USER-R-03`, `USER-NFR-01` |
| **GitHub Feature** | [#1536](https://github.com/qbicsoftware/data-manager-app/issues/1536) |
| **Status** | 🔴 Open |

**Persona Constraint — Data Steward Scale**

Data stewards are power users with access to potentially hundreds of projects. Design decisions for this Feature must hold at that scale: search and sorting (not page-by-page browsing) are the primary discovery mechanisms; a page-size selector serves power users; and the pager must always communicate location and total (e.g. "Page 3 of 21 — 500 projects").

---

### Stories

#### FEAT-PAG-LIST-01 — Paginated Project Overview with Responsive Card Layout

| Field | Value |
|---|---|
| **Requirement IDs** | `USER-R-01`, `USER-R-03`, `USER-NFR-01` |
| **Status** | 🟡 In Progress |
| **GitHub** | [#1537](https://github.com/qbicsoftware/data-manager-app/issues/1537) |

**User Story**

> As a data steward with access to several hundred projects, I want to browse the project overview as a paginated, responsive card layout, so that I can navigate my projects reliably with native browser behaviour on any screen size.

**Acceptance Criteria**

- Given a user on the project overview with more projects than the page size, When the list is displayed, Then page navigation controls and the total project count are shown and only the current page is rendered.
- Given a user navigates to another page, changes the page size, searches, or changes the sort order, When the list updates, Then the URL reflects the list state and the browser back/forward buttons restore previous states.
- Given a user on a small screen, When viewing the project overview, Then the card layout adapts to the viewport width and the page scrolls natively without an embedded scroll container.
- Given a user enters a search term or changes the sort order, When the list updates, Then paging resets to the first page and the total count reflects the active filter.
- Given a steward with access to hundreds of projects, When they choose a larger page size (e.g. 24/48/96), Then the overview renders that many project cards per page.
- Given a user sorts by a project attribute (e.g. title, project code, last modified), When the sort is applied, Then the project cards are ordered accordingly across pages.

**Notes & Context**

- The current implementation uses a Vaadin `Grid` with a single component column purely as a lazy-loading card container (`ProjectCollectionComponent`). The Grid provides no used table features here and prevents responsive multi-column card reflow; the story explicitly allows replacing it with a non-Grid card layout.
- Default sort remains `lastModified` descending so stewards land on their most relevant projects.
- A count query for the project overview does not exist yet and must be added for the pager total.

---

#### FEAT-PAG-LIST-02 — Paginated Sample List with Cross-Page Selection

| Field | Value |
|---|---|
| **Requirement IDs** | `USER-R-01`, `USER-R-02`, `USER-R-03` |
| **Status** | 🔴 Open |
| **GitHub** | [#1540](https://github.com/qbicsoftware/data-manager-app/issues/1540) |

**User Story**

> As a project member, I want to browse samples in a paginated list and keep my selection while navigating pages, so that I can export exactly the samples I need for offline metadata editing.

**Acceptance Criteria**

- Given a user on the sample list of an experiment, When the samples exceed the page size, Then page navigation controls and the total sample count are shown and only the current page is rendered.
- Given a user has selected samples on one page, When they navigate to another page, Then the selection is preserved and the number of selected samples remains visible.
- Given a user has samples selected across multiple pages, When they trigger the metadata export, Then the export contains exactly the selected samples.
- Given a user changes page, filter, or sort, When the list updates, Then the URL reflects the list state and browser back/forward restores previous states.

**Notes & Context**

- Selection must be modelled as a set of sample identifiers owned by the view component, not via the Vaadin Grid selection model, so it survives page switches without holding full item object graphs in the session.
- The header select-all checkbox applies to the current page and must communicate this honestly; whether a "select all N matching the filter" action is added is an open product decision.
- Whether selection survives a search/filter change (current behaviour: yes) is an open product decision to resolve during refinement.

---

#### FEAT-PAG-LIST-03 — Paginated Measurement Lists with Cross-Page Selection

| Field | Value |
|---|---|
| **Requirement IDs** | `USER-R-01`, `USER-R-02`, `USER-R-03` |
| **Status** | 🟡 In Progress |
| **GitHub** | [#1539](https://github.com/qbicsoftware/data-manager-app/issues/1539) |

**User Story**

> As a project member, I want to browse proteomics, genomics, and immunopeptidomics measurements in paginated lists and keep my selection while navigating pages, so that I can export, edit, or delete exactly the measurements I targeted.

**Acceptance Criteria**

- Given a user on any measurement tab (NGS, PxP, IP), When the measurements exceed the page size, Then page navigation controls and the total measurement count are shown and only the current page is rendered.
- Given a user has selected measurements on one page, When they navigate to another page, Then the selection is preserved and the number of selected measurements remains visible.
- Given a user has measurements selected across multiple pages, When they trigger export, edit, or deletion, Then the action applies to exactly the selected measurements.
- Given a user triggers a deletion, When the confirmation is shown, Then the number of affected measurements across all pages is communicated before the deletion is executed.

**Notes & Context**

- All bulk actions already reduce the selection to measurement IDs before calling the service layer, so an identifier-based selection model requires no service API changes.
- A review step or "show selected only" option before destructive actions is a candidate UX improvement to evaluate during refinement.
- **Product decision (recorded on #1536 during refinement):** "select all" means **all measurements matching the active filter in backend storage**, so a full metadata export across pages always works. The selection snapshot **survives page, filter, and sort changes**; a "Clear selection" affordance accompanies the count label. Paginated measurement grids are single-sort (the URL carries one sort parameter). See [ADR-0009](adr/0009-paginated-measurement-lists-and-selection.md).

---

#### FEAT-PAG-LIST-04 — Paginated Raw Dataset Lists with Cross-Page Selection

| Field | Value |
|---|---|
| **Requirement IDs** | `USER-R-01`, `USER-R-02`, `USER-R-03` |
| **Status** | 🔴 Open |
| **GitHub** | [#1538](https://github.com/qbicsoftware/data-manager-app/issues/1538) |

**User Story**

> As a project member, I want to browse raw datasets in paginated lists and keep my selection while navigating pages, so that I can export dataset download URLs for exactly the datasets I need.

**Acceptance Criteria**

- Given a user on any raw data tab (NGS, PxP, IP), When the datasets exceed the page size, Then page navigation controls and the total dataset count are shown and only the current page is rendered.
- Given a user has selected datasets on one page, When they navigate to another page, Then the selection is preserved and the number of selected datasets remains visible.
- Given a user has datasets selected across multiple pages, When they trigger the dataset URL export, Then the export contains exactly the selected datasets.

**Notes & Context**

- The URL export already reduces the selection to measurement IDs before generating the file, so an identifier-based selection model requires no service API changes.

---

### FEAT-SAMPLE-BATCH-REMOVAL

| Field | Value |
|---|---|
| **Description** | Remove the explicit sample batch entity: samples are registered directly within an experiment, carry a mandatory free-text `batch` label and a denormalised `project_id`, and are managed via a samples-only UI (Excel registration/edit, multi-select delete) |
| **PRD Section** | §3 Scope — Sample registration |
| **Requirements** | `SAMPLE-R-01`, `SAMPLE-R-02`, `SAMPLE-R-03`, `SAMPLE-R-04` |
| **GitHub Feature** | [#1548](https://github.com/qbicsoftware/data-manager-app/issues/1548) |

#### FEAT-SAMBAT-01 — Register Samples Directly in a Project

| Field | Value |
|---|---|
| **Requirement IDs** | `SAMPLE-R-01` |
| **Status** | 🟢 Done |
| **GitHub** | [#1549](https://github.com/qbicsoftware/data-manager-app/issues/1549) |

**User Story**

> As a project owner, I want to register samples directly in a project (grouped under an experiment) without first creating an explicit sample batch, so that sample registration no longer requires an intermediate batch step.

**Acceptance Criteria**

- Given a user has write access to a project with an experiment, When they register samples in that experiment, Then they upload sample metadata via an Excel spreadsheet directly and do NOT need to create an explicit sample batch first.
- Given samples are registered in an experiment, When the registration completes, Then the samples are associated with the project and the experiment.
- Given a sample is registered, When the sample is persisted, Then it carries a mandatory free-text `batch` label (a distinct column in the registration spreadsheet).
- Given a registration spreadsheet is submitted without a batch value for a row, When the validation runs, Then the sample is rejected with a validation error.

**Notes & Context**

- Registration template column order: Sample Name → Analysis to be performed → Biological Replicate → **Batch** → Condition → Species → Specimen → Analyte → Comment. The `Batch` column sits after *Biological Replicate* and before *Condition*. The registration template does NOT include registration/modification time columns (they are system-set).

**Tasks**

| # | Title | Status | GitHub |
|---|---|---|---|
| — | Register samples directly in a project (remove batch-creation step) | 🟢 Done | [#1549](https://github.com/qbicsoftware/data-manager-app/issues/1549) |

---

#### FEAT-SAMBAT-02 — Associate Samples with Project and Experiment

| Field | Value |
|---|---|
| **Requirement IDs** | `SAMPLE-R-02` |
| **Status** | 🟢 Done |
| **GitHub** | [#1550](https://github.com/qbicsoftware/data-manager-app/issues/1550) |

**User Story**

> As a system/API consumer, I want every sample to be directly associated with its project and experiment, so that queries fetching all samples for a project avoid an extra transitive lookup through the experiment.

**Acceptance Criteria**

- Given a sample is registered, When it is persisted, Then it is directly associated with both the project and the experiment.
- Given a project with multiple experiments, When all samples for that project are requested, Then the query resolves them without a transitive lookup through the experiment hierarchy.
- Given existing samples in the database, When the association is introduced, Then they are backfilled with the correct project association derived from their experiment.
- Given the schema is updated, When the migration runs, Then the sample table carries a `project_id` column referencing the project.

**Notes & Context**

- The `project_id` association is a denormalised column on `sample` for query optimisation; it is backfilled from the sample's experiment by the migration. `Sample` exposes `projectId()`, and project-wide sample queries use `findSamplesByProjectId`.

**Tasks**

| # | Title | Status | GitHub |
|---|---|---|---|
| — | Add denormalised `project_id` association and backfill | 🟢 Done | [#1550](https://github.com/qbicsoftware/data-manager-app/issues/1550) |

---

#### FEAT-SAMBAT-03 — Backwards-Compatible Batch Property and Data Migration

| Field | Value |
|---|---|
| **Requirement IDs** | `SAMPLE-R-03` |
| **Status** | 🟢 Done |
| **GitHub** | [#1551](https://github.com/qbicsoftware/data-manager-app/issues/1551) |

**User Story**

> As a system operator, I want existing sample data and downstream consumers to keep working after the removal of the explicit batch, so that no data is lost during the migration.

**Acceptance Criteria**

- Given existing samples with an assigned batch, When the migration is complete, Then each sample retains its batch name as a free-text `batch` property.
- Given the batch table is removed, When the batch is referenced, Then the batch name is still available from the sample via the `batch` property.
- Given a sample grid is displayed, When the grid renders, Then the registration and modification date are shown for each sample.
- Given the registration and modification dates, When they are displayed, Then they are shown in the sample grid (not in the Excel registration/edit templates), are system-set, and are not editable by the user.
- Given a sample is edited, When the edit is applied, Then the last-modified time is updated by the system (not by the user).

**Notes & Context**

- The registration and modification times are system-managed audit values shown in the sample grid (registration and modification date columns). They are NOT present in the Excel registration or edit templates (the Product Owner confirmed they are unnecessary in the spreadsheets because they are system-managed).
- Edit and information template column order: QBiC Sample Id → Sample Name → Analysis to be performed → Biological Replicate → **Batch** → Condition → Species → Specimen → Analyte → Comment. The `Batch` column sits after *Biological Replicate* and before *Condition*; the templates do NOT include registration/modification time columns.
- Stop-the-world migration: the additive migration (`add-sample-batch-property-and-project-association.sql`) preserves data and is applied; the production migration (`finalize-sample-batch-removal.sql`) drops the legacy `sample_batches`/`sample_batches_sampleid` tables.

**Tasks**

| # | Title | Status | GitHub |
|---|---|---|---|
| — | Preserve batch property and dates; apply migration | 🟢 Done | [#1551](https://github.com/qbicsoftware/data-manager-app/issues/1551) |

---

#### FEAT-SAMBAT-04 — Samples-Only UI (Remove Batch Grid and Batch Dialogs)

| Field | Value |
|---|---|
| **Requirement IDs** | `SAMPLE-R-04` |
| **Status** | 🟢 Done |
| **GitHub** | [#1552](https://github.com/qbicsoftware/data-manager-app/issues/1552) |

**User Story**

> As a user managing samples in a project, I want a samples-only view where samples can be registered, edited, and deleted directly (without the explicit batch concept), so that I manage samples in the same bulk, Excel-based way I manage measurements.

**Acceptance Criteria**

- Given a user navigates to the samples section of a project/experiment, When the view loads, Then no batch grid is displayed.
- Given a user registers samples, When they start registration, Then they upload sample metadata via an Excel spreadsheet directly (no separate batch-name dialog).
- Given a sample grid is displayed, When the grid renders, Then the `batch` value is shown as a column on the sample.
- Given a user edits samples, When they trigger edit, Then they download a pre-filled Excel template containing the current sample values, modify editable fields, and re-upload to apply changes.
- Given a user deletes samples, When they select the corresponding samples in the grid, Then the selected samples are deleted in one action.
- Given the batch concept is removed from the UI, When the view is shown, Then no batch-specific actions (create/edit/delete batch) are available.

**Notes & Context**

- The samples-only view mirrors the measurement workflow: Register (primary action), Export (feature action), and Edit/Delete (secondary actions) operate on the sample grid. Deletion uses `deletionService.deleteSamples`.
- Sample templates use the column order described under FEAT-SAMBAT-01 (Batch after Biological Replicate). Registration and modification times are shown in the sample grid, not in the Excel templates.

**Tasks**

| # | Title | Status | GitHub |
|---|---|---|---|
| — | Samples-only UI (remove batch grid and batch dialogs) | 🟢 Done | [#1552](https://github.com/qbicsoftware/data-manager-app/issues/1552) |

---

---

### FEAT-PINNED-PROJECTS

| Field | Value |
|---|---|
| **Description** | User-curated pinned projects: a small personal shortlist of the projects a user is actively working on, presented as the entry point to project work, so that returning to frequently used projects no longer requires a search |
| **PRD Section** | §2 Users & primary use cases — Persona 1 (Anna Becker, Project Manager) and Persona 2 (Dr. Jonas Weber, Researcher) |
| **Requirements** | `USER-R-04` |
| **GitHub Feature** | — (tracked in this document only; see Notes & Context of FEAT-PINNED-01) |
| **Status** | 🟡 In Progress |

**Why curation, not derivation**

"The projects I am active in" is a judgement only the user can make. A project that has had no
recorded change for months can still be central to a user's work, while a project that was edited
yesterday may be finished for them. Recency, membership and role-based signals therefore produce a
list that is right most of the time and wrong exactly when it matters. The Feature accepts a small
curation cost (one click per project, once) in exchange for a shortlist that is correct by
definition, and keeps the set deliberately small so it stays a shortlist rather than a second copy
of the project list.

---

### Stories

#### FEAT-PINNED-01 — Pin Projects for Quick Access on the Project Overview

| Field | Value |
|---|---|
| **Requirement IDs** | `USER-R-04` |
| **Status** | 🟡 In Progress |
| **GitHub** | — |

**User Story**

> As a researcher or project manager who maintains many projects, I want to pin the few projects I
> am currently working on, so that I can open them straight away from the project overview instead
> of searching for them every time.

**Acceptance Criteria**

- Given a project card in the project overview, When the user activates the pin control on that card, Then the project is added to that user's pinned projects and appears in the pinned-project row above the list controls.
- Given a project is already pinned by the user, When they view the project overview or a project card, Then the pin control indicates the pinned state, and activating it removes the pin and the project leaves the pinned-project row.
- Given a user who has pinned six projects, When they attempt to pin a seventh, Then no pin is created and the user is told that six is the limit and that an existing pin must be removed first.
- Given a user with several pinned projects, When the pinned-project row is rendered, Then the projects appear most recently pinned first.
- Given a user's pins, When any of them is rendered, Then the displayed project code, title and measurement types for accessible projects come from the same live data used by the project list, and no pin is ever visible to anyone but its owner.
- Given a pinned project the user can no longer access, When the pinned-project row is rendered, Then a placeholder is shown carrying only the project code and title recorded when the project was pinned, together with an indication that access is no longer available, and no other project data is displayed.
- Given a placeholder for a project the user cannot access, When the user unpins it, Then the pin is removed without requiring project access and the freed place can be used for another pin.
- Given a pinned project the user has never been able to access or has lost access to, When any pinned-project data is read, Then no project title, contact, sample, measurement or dataset information is disclosed beyond the label recorded at pin time by that same user.
- Given the pinned-project row is shown, When the user searches, changes the sort order or moves to another page of the project list, Then the row stays in place and unchanged; when the user has no pins the row is not shown at all.
- Given a pinned project that also appears in the current page of the project list, When the overview is rendered, Then it is displayed in both places and the pager total still matches the number of projects matching the current filter.
- Given a narrow viewport, When the pinned-project row is rendered, Then the pinned items reflow to fewer columns without trapping the native page scroll in an embedded scroll container (USER-NFR-01).
- Given a user who has pinned projects, When they return to the overview in a later session or from another device, Then the same projects are still pinned for that user.

**Notes & Context**

- The limit of six pinned projects is a fixed product parameter for this story: the limit keeps
  the set a shortlist, and six allows a balanced 3×2 grid on desktop so five pins wrap into an
  even 3+2 layout instead of a cramped single row. It is not part of `USER-R-04` and may become a
  per-user preference later.
- A pinned project is intentionally allowed to appear twice (in the row and in the list below).
  Suppressing it in the list would change the meaning of the overview count and the page-boundary
  behaviour established in `ADR-0007`, which is a worse trade than a duplicated card.
- Pins are stored as an application-layer user preference, not as project state: pinning must not
  alter `Project#lastModified`, because that value is the default sort key of the very list this
  story speeds up. See [ADR-0008](adr/0008-pinned-projects-as-user-preferences.md).
- Deferred, not part of this story: pinning from the project detail view; a global pinned-project
  switcher in the application navigation (the natural next step, reusing the same service); an
  automatically derived "recently visited" list; bulk pinning through cross-page selection
  (`USER-R-02`).
- Traceability deviation: this Feature and Story are tracked in this document only, without GitHub
  Feature/Story issues, so the `**GitHub Feature**` and `**GitHub**` fields carry `—`. The task
  breakdown lives in [`docs/plans/FEAT-PINNED-01-implementation-plan.md`](plans/FEAT-PINNED-01-implementation-plan.md).
  `AGENTS.md` §0/§11 were deliberately left unchanged; the deviation is recorded in the
  implementation pull request description.

---

### FEAT-USER-GROUPS

| Field | Value |
|---|---|
| **Description** | User groups for project sharing: org groups (admin-managed, e.g. NGS labs) and ad-hoc groups (self-service) that can be shared onto projects at READ/WRITE/ADMIN (never OWNER), replacing per-person access assignment for recurring teams |
| **PRD Section** | — (new capability; see (docs) [`user-groups-strategy.md`](user-groups-strategy.md)) |
| **Requirements** | `GROUP-R-01` … `GROUP-R-12`, `GROUP-NFR-01` … `GROUP-NFR-03` *(draft — new `GROUP` domain, pending requirements PR)* |
| **GitHub Feature** | — (tracked in this document + external stories; see Notes & Context) |
| **Status** | 🔴 Open |

**Strategy reference:** [`docs/user-groups-strategy.md`](user-groups-strategy.md) defines the full
strategy (two group types, internal OWNER/MANAGER/MEMBER roles vs. project roles, visibility policy,
notification profile, revocation NFR = ≤60s). The full user stories (EPIC-grouped, with acceptance
criteria) are hosted **externally** in
[`docs/features/FEAT-USER-GROUPS-stories.md`](features/FEAT-USER-GROUPS-stories.md); this entry
carries only the Feature-level summary until stories are approved.

**Governance status & blockers (per `AGENTS.md` §0/§12 and strategy §6):**

- New `GROUP-*` / `ACCESS-*` requirements must be added to `docs/requirements.md` in a **dedicated,
  human-approved PR** before any story is implemented.
- A new `user-groups` bounded context requires an **ADR + Maven module approval** (human) before
  implementation can begin.
- Stories follow the **draft → approved lifecycle**: they are recorded here with stable IDs only
  once approved; tasks reference the stable story ID, never a GitHub issue number.
- **Traceability deviation (mirrors FEAT-PINNED-PROJECTS):** user stories are hosted externally
  (per current policy, not on GitHub), so the **GitHub Feature** / **GitHub** fields carry `—`.
  Story IDs follow the `FEAT-USER-GROUPS-<NN>` schema.

### Stories

_Draft EPIC-grouped stories live in_ [`docs/features/FEAT-USER-GROUPS-stories.md`](features/FEAT-USER-GROUPS-stories.md).
_Once approved, stories will be recorded here individually with their final stable IDs._

---

*Last updated: 2026-09-21*
