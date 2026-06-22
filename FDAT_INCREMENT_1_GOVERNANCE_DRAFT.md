# FDAT Increment 1 Governance Draft

This document is a working draft for the newly introduced governance framework.
It proposes a requirement > feature > user story structure for the first increment of FDAT integration.
Another agent can later normalize the content into the exact repository governance scheme.

## Scope of Increment 1

- Supported remote repository: FDAT only
- Supported records: public and private FDAT records
- Supported entry point: access is brokered through Data Manager project context
- Supported permissions: project-level `view` and `edit`, mapped to FDAT access-link permissions
- Out of scope: dataset draft creation in FDAT, publication submission workflows, non-FDAT repositories

## Proposed Requirements

### PROJECT-R-01: Link FDAT records to projects

The system shall allow authorized users to link an FDAT record to a Data Manager project by using its DOI.

**Rationale:**
Researchers need project-level visibility of result datasets that are already managed in FDAT, including private results before publication.

**Source:**
Stakeholder workshops on linked remote data resources and project transparency.

### PROJECT-R-02: Link FDAT records to experiments

The system shall allow authorized users to associate a linked FDAT record with one or more experiments inside the same project.

**Rationale:**
Experiment-level linkage improves traceability between project structure and remote result records without introducing experiment-level permission handling.

**Source:**
Stakeholder workshops on project and experiment linkage.

### FAIR-R-01: Resolve FDAT metadata from persistent identifiers

The system shall resolve metadata for linked FDAT records by using the DOI and machine-readable repository metadata exposed through FAIR signposting and FDAT APIs.

**Rationale:**
Standards-based metadata resolution reduces bespoke integration logic and supports FAIR-aligned interoperability.

**Source:**
Stakeholder discussion on FAIR signposting and InvenioRDM-based integration.

### FAIR-R-02: Display provenance metadata for linked FDAT records

The system shall display provenance-relevant metadata for linked FDAT records, such as software used, software versions, and referenced source data, when that information is exposed by FDAT.

**Rationale:**
Project members need to understand how result datasets were produced in order to improve transparency, trust, and reuse.

**Source:**
Stakeholder stories on software, software versioning, and source data visibility.

### USER-R-01: Broker access to linked FDAT records through Data Manager

The system shall broker access to linked FDAT records through Data Manager so that collaborators enter remote records from project context instead of handling raw repository access links directly.

**Rationale:**
Data Manager should remain the primary collaboration workspace and reduce operational overhead for researchers.

**Source:**
Stakeholder discussion on central access handling and reduced manual coordination.

### USER-R-02: Align remote access level with project role

The system shall map Data Manager project roles to the corresponding FDAT access-link permission for all FDAT records linked to that project, such that collaborators with project view rights receive view access and collaborators with project edit rights receive edit access.

**Rationale:**
Project collaborators expect remote access to align with the permissions already granted in the primary application.

**Source:**
Stakeholder decision that permissions are handled on project level for all linked records.

### USER-R-03: Support managed access-link lifecycle actions

The system shall support creation, replacement, revocation, and rotation of managed FDAT access links for linked records.

**Rationale:**
Tokenized repository access requires lifecycle control to support collaboration, incident response, and permission changes.

**Source:**
Stakeholder discussion on link rotation, downgrade handling, and exposure response.

### USER-R-04: Support role-change synchronization for linked FDAT access

The system shall update project-facing FDAT access for linked records when a collaborator's Data Manager project role changes, including downgrade from edit to view where applicable.

**Rationale:**
Manual remote permission maintenance would create unacceptable overhead and undermine trust in the integration.

**Source:**
Stakeholder requirement to avoid manual access reconciliation after role changes.

### QUALITY-NFR-01: Preserve access transparency for linked records

The system shall show collaborators the current access state of each linked FDAT record in project context, including whether the record is public or private and whether the collaborator has brokered view or edit access.

**Rationale:**
Users need clear feedback about visibility, remote access expectations, and why a record may or may not be editable.

**Source:**
Stakeholder discussion on transparency, overview, and security perception.

### QUALITY-NFR-02: Minimize repeated authentication friction

The system should minimize repeated credential entry during FDAT integration workflows by reusing compatible authentication flows, including ORCID-based login where supported by FDAT.

**Rationale:**
Reduced login friction improves adoption and keeps Data Manager as the preferred entry point for integrated workflows.

**Source:**
Stakeholder discussion on ORCID-based login reuse across Data Manager and FDAT.

### LAB-C-01: Increment 1 is limited to FDAT

Increment 1 shall support FDAT only. Other InvenioRDM-based repositories, including Zenodo, are candidates for future evaluation and are out of scope for this increment.

**Rationale:**
Restricting the first increment to FDAT prevents unsupported repository growth and avoids implementation mud.

**Source:**
Product decision from stakeholder discussion.

### FAIR-C-01: Standards-based metadata is required for integration

Remote metadata integration in this capability shall rely on DOI resolution plus machine-readable metadata exposed through FAIR signposting and compatible FDAT APIs.

**Rationale:**
This boundary preserves maintainability and keeps the integration aligned with FAIR-oriented repository capabilities.

**Source:**
Stakeholder discussion on using FAIR signposting as the integration contract.

### USER-C-01: Data Manager brokers access, but FDAT remains the authorization authority

Data Manager shall manage project-facing access routes to linked FDAT records, but the remote authorization model and supported permission scopes remain constrained by FDAT capabilities.

**Rationale:**
This clarifies system boundaries and prevents overpromising full cross-system RBAC synchronization.

**Source:**
Stakeholder discussion on repository-backed share links and delegated responsibility.

### USER-C-02: Raw managed FDAT access links are not user-facing artifacts

Managed FDAT access links shall be handled internally by Data Manager and shall not be intentionally exposed to collaborators as reusable raw URLs.

**Rationale:**
Hiding raw token links makes role-change enforcement and incident-driven link rotation feasible.

**Source:**
Stakeholder discussion on brokered access and link rotation.

## Proposed Features

### FEAT-FDAT-DATASET-INTEGRATION

Integrate FDAT records into Data Manager project context so that researchers can link private or public remote datasets by DOI, navigate them from the project workspace, and maintain traceable project-level connections to experiments.

**Requirement IDs:**
PROJECT-R-01, PROJECT-R-02, FAIR-R-01, LAB-C-01, FAIR-C-01

### FEAT-FDAT-PROVENANCE-VISIBILITY

Show provenance-relevant metadata from linked FDAT records, including software, software versions, and referenced source data when available from FDAT.

**Requirement IDs:**
FAIR-R-01, FAIR-R-02, QUALITY-NFR-01, FAIR-C-01

### FEAT-FDAT-BROKERED-ACCESS

Broker project-level access to linked FDAT records through Data Manager by managing repository-backed access links, aligning access level with project role, and supporting access-link lifecycle changes.

**Requirement IDs:**
USER-R-01, USER-R-02, USER-R-03, USER-R-04, QUALITY-NFR-01, QUALITY-NFR-02, USER-C-01, USER-C-02, LAB-C-01

## Proposed User Stories

### Story 1: Link an FDAT record to a project

**Parent Feature:**
FEAT-FDAT-DATASET-INTEGRATION

**Requirement IDs:**
PROJECT-R-01, FAIR-R-01

**User Story:**
As a data provider, I want to link an FDAT record to a QBiC project via its DOI, so that project members can find the result dataset from the project workspace.

**Acceptance Criteria:**
- Given a user with edit rights in a project and a valid FDAT DOI, when the user links the DOI to the project, then the linked FDAT record is stored and shown in the project's linked records overview.
- Given an invalid or unsupported DOI, when the user submits the DOI, then the system rejects the request and shows a validation message.
- Given an FDAT record already linked to the same project, when the user submits the DOI again, then the system prevents duplicate creation and informs the user.

### Story 2: Link an FDAT record to experiments

**Parent Feature:**
FEAT-FDAT-DATASET-INTEGRATION

**Requirement IDs:**
PROJECT-R-02

**User Story:**
As a data provider, I want to associate a linked FDAT record with one or more experiments in the same project, so that the remote result remains traceable to the relevant experimental context.

**Acceptance Criteria:**
- Given a linked FDAT record and a project containing experiments, when a user with project edit rights assigns the record to selected experiments, then the system stores those experiment associations.
- Given an experiment from a different project, when the user attempts to associate it with the linked FDAT record, then the system rejects the association.
- Given an FDAT record linked to experiments, when a project member views the record in Data Manager, then the linked experiments are shown in the project context.

### Story 3: View provenance metadata of a linked FDAT record

**Parent Feature:**
FEAT-FDAT-PROVENANCE-VISIBILITY

**Requirement IDs:**
FAIR-R-01, FAIR-R-02, QUALITY-NFR-01

**User Story:**
As a project manager, I want to see provenance metadata of a linked FDAT record, so that I can understand how the result dataset was produced.

**Acceptance Criteria:**
- Given a linked FDAT record whose metadata exposes software information, when a project member opens the record details in Data Manager, then the software used is shown.
- Given a linked FDAT record whose metadata exposes software version information, when a project member opens the record details in Data Manager, then the software versions are shown.
- Given a linked FDAT record whose metadata exposes referenced source data, when a project member opens the record details in Data Manager, then the referenced source data is shown.
- Given provenance information is not exposed by FDAT for a linked record, when a project member opens the record details in Data Manager, then the system indicates that the information is unavailable instead of showing fabricated or empty values.

### Story 4: Open a linked FDAT record through Data Manager

**Parent Feature:**
FEAT-FDAT-BROKERED-ACCESS

**Requirement IDs:**
USER-R-01, USER-R-02, QUALITY-NFR-01, USER-C-02

**User Story:**
As a project collaborator, I want to open a linked FDAT record through Data Manager, so that I can access the remote record with the permission level that matches my project role.

**Acceptance Criteria:**
- Given a project collaborator with view rights and a linked FDAT record, when the collaborator opens the record through Data Manager, then the system brokers view access to the remote record.
- Given a project collaborator with edit rights and a linked FDAT record, when the collaborator opens the record through Data Manager, then the system brokers edit access to the remote record.
- Given a user without access to the project, when the user attempts to open the linked FDAT record through Data Manager, then the system denies access.
- Given a linked FDAT record is private, when an authorized collaborator opens it through Data Manager, then the collaborator can still reach the private record according to the brokered access level.

### Story 5: Synchronize remote access with project role changes

**Parent Feature:**
FEAT-FDAT-BROKERED-ACCESS

**Requirement IDs:**
USER-R-02, USER-R-03, USER-R-04, USER-C-01, USER-C-02

**User Story:**
As a project manager, I want FDAT access to linked records to stay aligned with Data Manager project roles, so that collaborators always retain the correct remote permission level without manual reconciliation.

**Acceptance Criteria:**
- Given a collaborator's project role changes from edit to view, when the role change is applied in Data Manager, then the system updates the project-facing access path so the collaborator only receives view access when re-entering through Data Manager.
- Given a collaborator's project role changes from view to edit, when the role change is applied in Data Manager, then the system updates the project-facing access path so the collaborator receives edit access when re-entering through Data Manager.
- Given a collaborator loses project access completely, when the role change is applied in Data Manager, then the system no longer brokers access to linked FDAT records for that collaborator.
- Given stronger access links may have been exposed previously, when a downgrade or removal requires access hardening, then the system rotates or replaces the managed FDAT access links needed to enforce the new access state.

### Story 6: Rotate managed access links after suspected exposure

**Parent Feature:**
FEAT-FDAT-BROKERED-ACCESS

**Requirement IDs:**
USER-R-03, QUALITY-NFR-01, USER-C-02

**User Story:**
As a project manager, I want to rotate managed FDAT access links when I suspect a link was exposed to an unauthorized party, so that the old access path becomes invalid and collaborators regain valid access by returning through Data Manager.

**Acceptance Criteria:**
- Given an authorized user suspects a managed FDAT access link is exposed, when the user triggers link rotation for a linked record and access level, then the previously managed access link is replaced.
- Given collaborators still have legitimate access after rotation, when they re-enter the linked FDAT record through Data Manager, then the system uses the current valid managed access path for their role.
- Given a link rotation impacts a shared access tier, when the authorized user initiates the rotation, then the system warns that existing project-facing access for that tier will be refreshed.

### Story 7: See access and visibility status for a linked FDAT record

**Parent Feature:**
FEAT-FDAT-BROKERED-ACCESS

**Requirement IDs:**
QUALITY-NFR-01

**User Story:**
As a project collaborator, I want to see the access and visibility state of a linked FDAT record in Data Manager, so that I understand whether the record is public or private and what level of access I currently have.

**Acceptance Criteria:**
- Given a linked FDAT record, when a project collaborator views it in Data Manager, then the system shows whether the record is public or private.
- Given a linked FDAT record and a current collaborator role, when a project collaborator views it in Data Manager, then the system shows whether the collaborator currently has brokered view access or edit access.
- Given the collaborator cannot currently access the linked FDAT record, when the collaborator views it in Data Manager, then the system shows that access is unavailable.

### Story 8: Reuse compatible authentication flows for FDAT integration

**Parent Feature:**
FEAT-FDAT-BROKERED-ACCESS

**Requirement IDs:**
QUALITY-NFR-02, USER-C-01

**User Story:**
As a data provider, I want Data Manager to reuse compatible authentication flows for FDAT where possible, so that I do not need to re-enter credentials unnecessarily during linked-record workflows.

**Acceptance Criteria:**
- Given Data Manager and FDAT both support a compatible ORCID-based authentication flow, when the user starts an FDAT integration action from Data Manager, then the system uses that compatible flow to reduce repeated credential entry.
- Given FDAT requires additional authentication state, when the user enters the remote record through Data Manager, then the system redirects the user through the required authentication step without breaking the Data Manager-driven workflow.
- Given a compatible authentication reuse path is unavailable, when the user starts the workflow, then the system continues the FDAT entry flow without claiming silent authentication reuse.

## Notes for Later Governance Normalization

- The requirement IDs above are proposed IDs only and should be checked against the authoritative numbering in `docs/requirements.md` before adoption.
- Constraints are intentionally separated from user stories and should later appear in task technical notes rather than story requirement references.
- The stories assume project-level permission handling for all records linked to a project.
- The stories assume brokered access through Data Manager and no user-facing raw managed FDAT token links.
