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
| **Status** | 🔴 Open |
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

*Last updated: 2026-09-14*
