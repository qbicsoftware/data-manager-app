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
- Constraints (`*-C-*`) influence architecture and must not be referenced in Stories. Reference them only in Task Technical Notes and ADRs.
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
<Link to PRD section, FAIR/CARE principle, regulatory document, stakeholder request, or ADR>
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

#### AUTH-R-01: InvenioRDM OAuth2 Authentication

The system shall support OAuth2 authentication with external InvenioRDM instances (e.g., Zenodo, FDAT), including authorization code flow with refresh tokens. Users shall be able to authenticate with one or more configured instances to access private datasets and establish authorized linkages.

**Rationale:**
Users need to access their own datasets on InvenioRDM platforms. OAuth2 with refresh tokens provides a seamless experience without requiring repeated authentication. This follows the same pattern as the existing ORCID OAuth2 integration.

**Source:**
PRD §5 Constraints — Authentication; Feature `FEAT-EXTERNAL-DATASET-LINKAGE`

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## PROJECT — Project Management and Experimental Design

### Features

_No features defined yet._

### Functional Requirements

#### PROJECT-R-01: Experiment-Level External Dataset Association

The system shall support associating linked external datasets at the experiment level within a project, with inheritance to the parent project view. Users shall be able to browse datasets by experiment or see all project-associated datasets in a consolidated view.

**Rationale:**
Experiment-level granularity allows researchers to organize associated datasets by the specific experimental context that generated or used them, while project-level inheritance maintains the central access point.

**Source:**
PRD §3 Scope — Project lifecycle management; Feature `FEAT-EXTERNAL-DATASET-LINKAGE`

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## SAMPLE — Sample Registration and Batch Management

### Features

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

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

#### DATA-R-04: External Dataset Discovery and Linkage

The system shall enable users to search public InvenioRDM instances and link discovered datasets to a DataManager project. The system shall display linked dataset metadata (title, DOI, authors, publication date, license) within the project context. Users shall be able to remove a linkage without affecting the source dataset.

**Rationale:**
Researchers manage many types of associated data (figures, protocols, supplementary datasets) on external platforms. Providing a unified view within DataManager reduces context switching and supports good scientific practice for data publication.

**Source:**
PRD §3 Scope — File management; Feature `FEAT-EXTERNAL-DATASET-LINKAGE`

#### DATA-R-05: Linked Dataset Metadata Synchronization

The system shall maintain synchronized metadata for linked InvenioRDM datasets, refreshing automatically at configurable intervals and on-demand via user action. If a source dataset becomes unavailable or its metadata changes significantly, the system shall reflect the current state and notify the user.

**Rationale:**
External dataset metadata evolves (new versions, updated descriptions). Live linkage ensures the DataManager view remains accurate without manual maintenance.

**Source:**
PRD §3 Scope — File management; Feature `FEAT-EXTERNAL-DATASET-LINKAGE`

### Non-Functional Requirements

_No requirements defined yet._

### Constraints

_No requirements defined yet._

---

## FAIR — FAIR Data Principles and Export

### Features

_No features defined yet._

### Functional Requirements

#### FAIR-R-01: FAIR Signposting Bidirectional Linkage

The system shall establish machine-actionable bidirectional relationships between DataManager projects and linked InvenioRDM datasets. The DataManager project URL shall be recorded as a related identifier within the InvenioRDM dataset metadata, discoverable via FAIR Signposting HTTP Link headers and embedded metadata.

**Rationale:**
True FAIR integration requires machine-readable, discoverable relationships rather than manual bookmarks. Bidirectional linkage enables external tools and services to navigate between the project management context and the published dataset.

**Source:**
PRD §3 Scope — FAIR data export; Feature `FEAT-EXTERNAL-DATASET-LINKAGE`

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

_No features defined yet._

### Functional Requirements

_No requirements defined yet._

### Non-Functional Requirements

_No requirements defined yet._

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