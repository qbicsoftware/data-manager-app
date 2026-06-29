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

### FEAT-EXTERNAL-DATASET-LINKAGE

| Field | Value |
|---|---|
| **Description** | Users can search public and private InvenioRDM repositories, link discovered datasets to their DataManager projects and experiments, and maintain live bidirectional metadata synchronization via FAIR Signposting. DataManager becomes the central hub; InvenioRDM remains the publishing platform. |
| **PRD Section** | §3 Scope — FAIR data export; §5 Constraints — FAIR principles; File management |
| **Requirements** | `AUTH-R-01`, `DATA-R-04`, `DATA-R-05`, `FAIR-R-01`, `PROJECT-R-01` |
| **GitHub Feature** | TBD |
| **Status** | 🔴 Open |

---

### Stories

_No stories approved yet. Draft stories below for refinement._

| Story ID | Title | Requirement(s) | Status |
|---|---|---|---|
| `FEAT-EXT-DATA-01` | Authenticate with InvenioRDM via OAuth2 | `AUTH-R-01` | 🔴 Draft |
| `FEAT-EXT-DATA-02` | Search and discover InvenioRDM datasets | `DATA-R-04` | 🔴 Draft |
| `FEAT-EXT-DATA-03` | Link discovered datasets to a project/experiment | `DATA-R-04`, `PROJECT-R-01` | 🔴 Draft |
| `FEAT-EXT-DATA-04` | View and manage linked datasets in project context | `DATA-R-04`, `PROJECT-R-01` | 🔴 Draft |
| `FEAT-EXT-DATA-05` | Synchronize linked dataset metadata | `DATA-R-05` | 🔴 Draft |
| `FEAT-EXT-DATA-06` | Establish FAIR Signposting bidirectional linkage | `FAIR-R-01` | 🔴 Draft |

---

*Last updated: 2026-06-16*
