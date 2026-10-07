# Requirements Registry

## Intent and Scope

This file is the **authoritative requirements registry** for the Data Manager Application. It contains all functional requirements (R), non-functional requirements (NFR), and constraints (C) that govern the system's capabilities and quality attributes.

### Relationship to Other Documents

- **PRD (`docs/prd.md`):** The Product Requirements Document contains the product vision, user personas, and business objectives. It is the upstream input to this registry.
- **This Registry:** Formalises PRD objectives into traceable, ID-tagged requirements. Each requirement is derived from and traces back to the PRD, stakeholder requests, regulatory drivers, or architectural decisions.
- **GitHub Features, Stories, and Tasks:** Requirements sit above Features in the governance hierarchy. A Feature groups related Stories under a named, user-visible capability and references one or more requirement IDs. A Story references a parent Feature and one or more requirement IDs, describing a user-facing workflow. A Task references a Story and implements concrete work towards that Story's acceptance criteria.

### Workflow

1. **PRD → Requirements → Features → Stories → Tasks → Implementation**
2. Before making any change to the system, check this file to confirm the change is covered by an existing requirement. If the change introduces new system capability not covered by any existing requirement, update this file first. Consult `docs/requirements-guide.md` for full authoring conventions.
3. Before creating a Story, confirm a parent Feature issue exists in the GitHub repository. If no Feature exists for the area of work, create the Feature first using `.github/ISSUE_TEMPLATE/feature.yml`.
4. **Agents:** You must read this file and `AGENTS.md` Section 11 step 0 before making any code change.

---

## Feature Layer

Features are the bridge between requirements and Stories. A Feature represents a named, user-visible capability — a coherent set of functionality that a user can understand and benefit from as a whole.

### Feature ID Schema

Features are identified using a short, human-readable slug:

```
FEAT-<SLUG>
```

- **SLUG** — A concise, uppercase, hyphen-separated label describing the user-visible capability.
  - Valid: `FEAT-SAMPLE-REGISTRATION`, `FEAT-FAIR-EXPORT`, `FEAT-USER-AUTH`
  - Invalid: `FEAT-01`, `feat-sample`, `FEATURE_REGISTRATION`

### Feature Rules

- A Feature slug must be **unique and stable** — never rename it once Stories reference it.
- A Feature must reference at least one `R-<NN>` or `NFR-<NN>` requirement ID from this registry.
- Constraints (`*-C-*`) must not be the sole requirement references for a Feature. They may appear in Feature notes but at least one `R` or `NFR` ID must be cited.
- One Feature may span multiple domains if the user-visible capability is coherent and the Stories it groups share a unified purpose.
- A Feature must be created in GitHub (using `.github/ISSUE_TEMPLATE/feature.yml`) before any Story references it.

### Feature Format

Features are tracked as GitHub issues using the Feature issue template. Each Feature issue must include:

- **Feature ID** — The `FEAT-<SLUG>` identifier
- **Requirement IDs** — One or more `R-<NN>` or `NFR-<NN>` references
- **Description** — What the user can do once this Feature is complete
- **Scope / Boundaries** — What is in scope and explicitly out of scope

---

## Requirement ID Schema

All requirements follow a domain-based ID structure:

```
<DOMAIN>-<TYPE>-<NN>
```

- **DOMAIN** — Functional area. Allowed values:
  - `AUTH` — Authentication, user identity, login, ORCID
  - `PROJECT` — Project management, experiments, experimental design
  - `SAMPLE` — Sample registration, batch management
  - `MEASUREMENT` — Measurement metadata, data tracking
  - `DATA` — Raw data handling, file management, downloads
  - `FAIR` — FAIR principles, RO-Crate export, data discoverability
  - `CARE` — CARE principles, governance, indigenous data rights
  - `QUALITY` — Data quality, validation, integrity
  - `LAB` — Laboratory operations, OpenBIS integration
  - `API` — Programmatic access, API design, tokens
  - `USER` — User management, roles, permissions, UI/UX
  - `COMM` — Communication, notifications, announcements, email

- **TYPE** — Requirement classification:
  - `R` — Functional requirement (system capability: "the system shall…")
  - `NFR` — Non-functional requirement (quality attribute: performance, scalability, security, usability, etc.)
  - `C` — Constraint (solution boundary: "must use…", "must not…", architectural decision)

- **NN** — Sequential number per (DOMAIN, TYPE) pair, zero-padded to exactly two digits:
  - Valid: `01`, `02`, `10`, `99`
  - Invalid: `1`, `2`, `010` (no leading zeros beyond two digits)

### Examples

- `AUTH-R-01` — Functional requirement in the Authentication domain
- `PROJECT-NFR-02` — Non-functional requirement in the Project Management domain
- `SAMPLE-C-01` — Constraint in the Sample Management domain
- `FAIR-R-03` — Functional requirement in the FAIR domain

### Rules

- IDs are **stable and must never be renumbered or reused.**
- Constraints (`*-C-*`) influence architecture and must not be referenced in Stories. Reference them only in Task Technical Notes and [ADRs](../adr/README.md).
- One requirement may be referenced by multiple Stories.
- A Story may reference multiple requirement IDs when a single user workflow spans multiple domains. If a Story spans more than two domains, consider splitting it.

---

## Requirement Format

Each requirement must contain:

```
### <ID>: <Short Title>

<Statement>

**Rationale:**
<Why this requirement exists — strategic, regulatory, stakeholder-driven, or architectural>

**Source (optional but recommended):**
<Link to PRD section, FAIR/CARE principle, regulatory document, stakeholder request, or [ADR](../adr/README.md)>
```

---

## Example (illustrative only)

### PROJECT-R-01: Project Creation by Authorised Users (illustrative only)

The system shall allow authorised users to create new research projects with metadata including title, description, and principal investigator.

**Rationale:**
Projects are the primary organisational unit in the Data Manager. Users need the ability to establish new research initiatives and provide basic context. This is a foundational capability enabling all downstream sample and measurement registration workflows.

**Source:**
PRD §3.1 Core Data Management; User story: "As a researcher, I want to create a project so that I can organise my research data."

---

## AUTH — Authentication and User Identity

### Features

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## PROJECT — Project Management and Experimental Design

### Features

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## SAMPLE — Sample Registration and Batch Management

### Features

_No features defined yet._

### Functional Requirements

#### SAMPLE-R-01: Direct Sample Registration within an Experiment

The system shall allow authorised users to register samples directly within an experiment of a project without first creating an explicit sample batch. A sample shall always belong to exactly one experiment. Each sample shall carry a mandatory free-text `batch` label provided as a distinct column in the registration spreadsheet.

**Rationale:**
Removing the explicit batch as a required registration step simplifies the sample registration workflow: users no longer need to define a batch entity before registering samples. Samples remain grouped within an experiment for contextual organisation. The mandatory batch label (per Product Owner) preserves grouping and backwards compatibility with existing data and downstream tooling, and is provided directly in the registration spreadsheet.

**Source:**
PRD §3 — Sample registration; Issue [FEAT-SAMBAT-01 #1549](https://github.com/qbicsoftware/data-manager-app/issues/1549); Stakeholder request [Incorporate Batch definition during Sample Sheet upload/edit template #1330](https://github.com/qbicsoftware/data-manager-app/issues/1330)

#### SAMPLE-R-02: Sample–Project and Sample–Experiment Association

The system shall associate every sample with both the project and the experiment it belongs to. The sample shall store a direct reference to its owning project (`project_id`) in addition to the existing experiment association, so that queries fetching all samples for a project do not require a transitive lookup through the experiment hierarchy.

**Rationale:**
Directly associating samples with their project and experiment makes project-wide sample queries efficient and keeps the association explicit in the data model. The `project_id` reference is denormalised for query optimisation; samples are still registered within an experiment.

**Source:**
PRD §3 — Sample registration; Issue [FEAT-SAMBAT-02 #1550](https://github.com/qbicsoftware/data-manager-app/issues/1550)

#### SAMPLE-R-03: Backwards-Compatible Batch Property and Data Preservation

The system shall preserve existing sample batch information during the removal of the explicit batch entity. Each existing sample shall retain its batch name as a free-text `batch` property, and batch-related metadata (creation/modification dates) shall be carried forward onto the sample. The samples view shall display the registration and modification date for each sample. The registration and modification dates shall be set by the system and shall not be editable via the Excel registration or edit templates. The migration shall transfer existing data to the new schema without loss.

**Rationale:**
Removing the explicit batch must not lose historical sample grouping or metadata. Preserving the batch name as a property on each sample keeps the information available to users and downstream consumers, and carrying the batch dates forward maintains data fidelity for existing samples. Displaying the registration and modification date per sample gives users visibility into when each sample was created and last changed. These dates are system-managed audit values rather than user-supplied sample metadata, so they are set by the system and excluded from the editable template columns. The pilot flag is no longer needed and is dropped.

**Source:**
Issue [FEAT-SAMBAT-03 #1551](https://github.com/qbicsoftware/data-manager-app/issues/1551); Stakeholder request [Incorporate Batch definition during Sample Sheet upload/edit template #1330](https://github.com/qbicsoftware/data-manager-app/issues/1330)

#### SAMPLE-R-04: Removal of the Explicit Batch and Samples-Only Management

The system shall no longer expose the explicit sample batch as a first-class entity in the sample management workflow. Samples shall be registered directly within an experiment via an Excel spreadsheet, with the batch name as a mandatory column. Editing shall also be Excel-based: the user shall select samples in the grid, download a pre-filled template containing the selected samples' current values, modify editable fields, and re-upload to apply changes. Deletion shall operate on a multi-selection: the user selects the corresponding samples in the grid and deletes them in one action. The samples view shall present samples directly, showing the batch as a sample property, and shall not offer batch-specific actions such as creating, editing, or deleting a batch.

**Rationale:**
Once the batch is no longer a first-class entity, exposing batch grids and batch dialogs would be confusing and inconsistent with the simplified data model. Presenting samples directly, with batch as a per-sample property, gives users a coherent, samples-only experience. Registration and editing via Excel mirror the established measurement workflows (see MEASUREMENT-R-01 and MEASUREMENT-R-02), ensuring a consistent bulk-data entry experience, while select-then-edit/delete gives users precise control over which samples are affected.

**Source:**
Issue [FEAT-SAMBAT-04 #1552](https://github.com/qbicsoftware/data-manager-app/issues/1552); Stakeholder request [Incorporate Batch definition during Sample Sheet upload/edit template #1330](https://github.com/qbicsoftware/data-manager-app/issues/1330)

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## MEASUREMENT — Measurement Metadata and Data Tracking

### Features

_No features defined yet._

### Functional Requirements

#### MEASUREMENT-R-01: Immunopeptidomics Measurement Registration

The system shall support registration of immunopeptidomics measurements via a domain-specific Excel template. Each successfully registered measurement shall be assigned a unique measurement code with an `IP-` domain prefix. The system shall validate all mandatory fields per the template specification and reject the entire batch if any row contains invalid or missing mandatory data.

**Rationale:**
The immunopeptidomics partner facility requires a dedicated measurement template with domain-specific metadata fields (e.g., MHC antibody, enrichment method, LC column) that differ from existing proteomics and genomics templates. Structured bulk registration via Excel ensures consistency and reduces manual entry errors.

**Source:**
PRD §3 Scope — Measurement integration; Issue #1412  
Stakeholder artifact: `docs/stakeholder-artifacts/measurement-metadata/immunopeptidomics-registration-spec-v1.0-2026-05-11.xlsx`

#### MEASUREMENT-R-02: Immunopeptidomics Measurement Editing

The system shall support editing of existing immunopeptidomics measurements via a pre-filled Excel template. Users shall download a template containing current values for selected measurements, modify editable fields, and re-upload to apply changes. The system shall validate the uploaded sheet and reject the batch if any editable mandatory field is missing or invalid.

**Rationale:**
Measurement metadata frequently requires corrections or updates after initial registration (e.g., instrument changes, comment additions). Providing a pre-filled edit template mirrors the existing proteomics and genomics workflows and ensures a consistent user experience across measurement domains.

**Source:**
PRD §3 Scope — Measurement integration; Issue #1412  
Stakeholder artifact: `docs/stakeholder-artifacts/measurement-metadata/immunopeptidomics-registration-spec-v1.0-2026-05-11.xlsx`

#### MEASUREMENT-R-03: Immunopeptidomics Measurement Deletion

The system shall allow authorised users to delete immunopeptidomics measurements. Measurements with an attached raw dataset shall not be deletable; the system shall inform the user that the attached dataset must be removed first. Deletion shall be synchronised between the Data Manager and the OpenBIS repository.

**Rationale:**
Users need the ability to remove erroneously registered measurements. Preventing deletion when raw data is attached protects data integrity and prevents orphaned datasets. OpenBIS synchronisation ensures consistency across the integrated data ecosystem.

**Source:**
PRD §3 Scope — Measurement integration; Issue #1412

#### MEASUREMENT-R-04: Immunopeptidomics Measurement View and Filtering

The system shall display registered immunopeptidomics measurements in the measurement view on a dedicated tab, distinctly from NGS and proteomics measurements. The view shall show domain-specific columns as defined in the immunopeptidomics template (e.g., MHC Antibody, Enrichment Method, Mass Range) and provide a search filter across all visible properties. The count of immunopeptidomics measurements shall be shown.

**Rationale:**
Project members need visibility into which immunopeptidomics measurements have been registered for an experiment. A dedicated tab with domain-specific columns and filtering enables quick discovery and review without mixing disparate metadata models.

**Source:**
PRD §3 Scope — Measurement integration; Issue #1412  
Stakeholder artifact: `docs/stakeholder-artifacts/measurement-metadata/immunopeptidomics-registration-spec-v1.0-2026-05-11.xlsx`

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## DATA — Raw Data File Handling

### Features

_No features defined yet._

### Functional Requirements

#### DATA-R-01: Immunopeptidomics Raw Dataset View and Filtering

The system shall display uploaded immunopeptidomics raw datasets in the raw data view on a dedicated tab, distinctly from NGS and proteomics datasets. The displayed information shall include Measurement ID, Sample Name, Upload Date, Number of Files, File Size, and File Suffixes. The view shall provide a search filter across all visible properties and show the count of immunopeptidomics datasets.

**Rationale:**
Project members need to verify that raw data has been successfully uploaded and associated with the correct immunopeptidomics measurements. A dedicated, filterable view keeps the user experience consistent with existing NGS and proteomics raw data workflows.

**Source:**
PRD §3 Scope — File management; Issue #1412

#### DATA-R-02: Immunopeptidomics Raw Data Upload via SFTP

The system shall support upload of immunopeptidomics raw datasets via SFTP to the data scanner application. Datasets shall be associated with the correct immunopeptidomics measurement ID using the `IP-` prefix during the upload and registration process. The uploaded dataset shall be discoverable in the Data Manager raw data view.

**Rationale:**
Partner facilities generate large immunopeptidomics raw data files that must be transferred efficiently via SFTP. Recognising the `IP-` measurement prefix ensures the data scanner routes datasets to the correct domain and associates them with the proper metadata record.

**Source:**
PRD §3 Scope — File management; Issue #1412

#### DATA-R-03: Immunopeptidomics Raw Data Download

The system shall support download of immunopeptidomics raw datasets via standard protocols (e.g., wget, cURL) from the data download server. Project members with appropriate access rights shall be able to specify an immunopeptidomics measurement ID and retrieve the associated dataset in the same manner as proteomics and genomics datasets.

**Rationale:**
Data scientists and bioinformaticians need programmatic access to raw immunopeptidomics data for downstream analysis. Consistent download behaviour across all measurement domains reduces friction and enables reproducible analysis pipelines.

**Source:**
PRD §3 Scope — File management; Issue #1412

#### DATA-R-04: Connected-Dataset Project-Listing Visibility

The system shall, for each project accessible to the logged-in user in the project collection view, display (a) the number of datasets connected to that project, (b) the per-access-level breakdown (open / restricted), and (c) the most-recent connection date across those datasets. The connected-dataset indicator shall be a distinct click target within the project card that navigates directly to the project's connected-datasets view.

**Rationale:**
Researchers need to assess dataset connectivity at a glance from their project listing, without opening each project individually. Surface-level visibility of both quantity and access status (including restricted data awaiting credential unlock) supports triage and prioritisation of project work.

**Source:**
Feature #1466 § FEAT-DATASET-CONNECTION; Story #1475 (FEAT-DATSET-09)

#### DATA-R-05: Connected-Dataset Synchronisation

The system shall support synchronising connected dataset metadata with the source InvenioRDM instance, so that locally stored connection records stay consistent with upstream changes (e.g. new versions, embargoes lifted, titles corrected).

**Rationale:**
Connected-dataset metadata can change on the source platform after connexion. Stale local metadata misleads researchers about access status and data provenance.

**Source:**
Feature #1466 § FEAT-DATASET-CONNECTION; Stories #1470, #1474

#### DATA-R-06: InvenioRDM Credential Management

The system shall allow individual users to register, list, and remove personal access tokens for InvenioRDM instances. Registered tokens shall be encrypted at rest and used only to search and connect access-restricted datasets on behalf of the user.

**Rationale:**
Access-restricted datasets require a valid InvenioRDM personal access token. Without a per-user credential management surface, researchers cannot connect restricted datasets or unlock embargoes their institution has access to.

**Source:**
Feature #1466 § FEAT-DATASET-CONNECTION; Stories #1471–#1479

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## FAIR — FAIR Data Principles and Export

### Features

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## CARE — CARE Principles and Data Governance

### Features

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## QUALITY — Data Quality and Validation

### Features

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## LAB — Laboratory Operations and External Integrations

### Features

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## API — Programmatic Access and API Design

### Features

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## USER — User Management, Roles, and Permissions

### Features

- `FEAT-PAGINATED-LISTS` — Explicit, paginated display of large entity collections (projects, samples, measurements, raw datasets) with cross-page selection for bulk actions and URL-restorable list state.
- `FEAT-PINNED-PROJECTS` — User-curated pinned projects that give quick access to the projects a user is actively working on, without searching.

### Functional Requirements

#### USER-R-01: Paginated List Display

The system shall display large entity collections (projects, samples, measurements, raw datasets) as explicitly paginated lists with a bounded page size, page navigation controls, and the total number of matching items. Continuous scroll-loading (endless scrolling) shall not be used for these lists.

**Rationale:**
Scroll-loaded lists replace the native page scroll with an embedded scroll container, which breaks native browser behaviour (find-in-page, scroll position, back/forward navigation) and behaves unreliably on small screens. Explicit pagination restores predictable, location-aware navigation. Power users such as data stewards can have access to several hundred projects and need reliable orientation when working with large collections.

**Source:**
Stakeholder request (UX review of list behaviour); PRD user personas (data steward).

#### USER-R-02: Cross-Page Selection for Bulk Actions

The system shall preserve item selections in paginated lists when the user navigates between pages, and bulk actions (export, edit, deletion) shall be applicable to the full selection across pages. The number of currently selected items shall be visible regardless of the page displayed.

**Rationale:**
Core workflows depend on targeting specific items scattered across a large collection: users download selected items' metadata for offline editing and perform targeted deletions. Without cross-page selection, pagination would force users to process one page at a time, breaking these workflows for collections of hundreds of items.

**Source:**
Stakeholder request; established bulk-action workflows for samples, measurements, and raw datasets.

#### USER-R-03: List View State in the URL

The system shall reflect the current page, page size, filter, and sort order of paginated lists in the browser URL, so that list views are restorable via browser history (back/forward) and shareable as links.

**Rationale:**
Native web behaviour allows users to bookmark, share, and navigate back to a previously seen list state. Scroll-loaded lists cannot provide this; explicit pagination makes it achievable at negligible additional cost and is a primary motivation for the change.

**Source:**
Stakeholder request (UX review of list behaviour).

#### USER-R-04: Pinned-Project Quick Access

The system shall let an authenticated user mark a small, self-selected subset of the projects they have access to as their pinned projects, and shall present that subset to the user as their entry point to project work. Pinned projects are private to and controlled by that user alone, and pinning a project shall never grant or reveal access to project data the user is not entitled to see.

**Rationale:**
Users who maintain many projects pay a search cost every time they return to the platform, even though they repeatedly work on only a handful of them. A user-curated short list removes that recurring cost. Curation is required because "the projects I am active in" is a judgement the user makes — it reflects current personal relevance that no system-derived signal, such as recent modification or membership, can express: a project that has been untouched for months may still be central to a user's work. This supports the project-manager need for quick overviews of important projects and the researcher need to avoid manual tasks that slow down scientific work.

**Source:**
PRD §2 Users & primary use cases — Persona 1 (Anna Becker, Project Manager: quick overviews of important project updates) and Persona 2 (Dr. Jonas Weber, Researcher: frustrated when excessive manual tasks slow down actual scientific progress); stakeholder request.

### Non-Functional Requirements

#### USER-NFR-01: Responsive List Rendering

Paginated lists shall remain fully usable on small screens (tablets, phones) without trapping the native page scroll inside an embedded scroll container, and shall adapt their layout to the available viewport width.

**Rationale:**
Embedded scroll containers in scroll-loaded lists have proven unreliable on smaller screens. Users increasingly access the application on mobile devices; list views are the primary navigation surface and must degrade gracefully.

**Source:**
Stakeholder request (UX review of list behaviour).

### Constraints

_No requirements defined yet._

---

## COMM — Communication, Notifications, and Announcements

### Features

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._