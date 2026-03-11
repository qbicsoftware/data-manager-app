# Product Requirements Document (PRD)

## 1. Goal
Our vision is to build a research data management platform that enables scientists to plan, document, and manage their research projects in a structured, transparent, and FAIR-compliant manner.

The platform will support the full lifecycle of high-throughput research workflows — from project and experimental design documentation, through sample registration and measurement parameter capture, to raw data registration and quality tracking. It will integrate partner laboratories performing high-throughput analyses (e.g., next-generation sequencing, proteomics, metabolomics) and ensure that all managed data fulfills FAIR and CARE principles.

By combining structured metadata capture, quality assessment, and interoperability, the platform will provide a reliable foundation for reproducible, transparent, and reusable research.

## 2. Users & primary use cases

<!-- PLACEHOLDER: The personas and use cases below are illustrative only. Replace with actual stakeholder research and user feedback before finalising the PRD. -->

**Persona A: Dr. Sarah Meyer – Principal Investigator (Biology Group)**
- Role: Leads a translational cancer biology research group at University of Tübingen
- Primary need: Design and track multi-year, multi-omics studies with 200+ samples from patient cohorts
- Pain point: Tracks experimental metadata in spreadsheets; struggles to ensure consistent sample naming and measurement parameters across internal lab work and outsourced sequencing

**Persona B: Marcus Klein – Lab Manager (QBiC Service Provider)**
- Role: Manages high-throughput analysis services (NGS, proteomics) at QBiC for internal and external researchers
- Primary need: Register samples from multiple projects, batch them for efficiency, upload measurement metadata from partner instruments, and track data transfer workflows
- Pain point: Currently uses OpenBIS and manual scripts; needs a unified UI to manage project-level metadata and track which samples have been measured

**Persona C: Jing Liu – Bioinformatician (Data Analyst)**
- Role: Analyzes omics data for research collaborators; publishes datasets
- Primary need: Download raw measurement data, review sample metadata, and export projects as FAIR-compliant research objects (RO-Crate) for archival and publication
- Pain point: Data discovery across projects is fragmented; wants reproducibility and proper attribution (ORCID integration)

**Top 3 jobs-to-be-done:**
1. **Design and document an experiment** – Create a project, define experimental groups, specify variables and confounding factors, and invite collaborators without manual email coordination
2. **Register and batch samples** – Bulk-upload sample metadata (XLSX), link to external sample identifiers from OpenBIS, and organize samples into measurement batches with consistent naming and QC flags
3. **Access, review, and export research data** – Download raw measurement files by experiment, verify sample metadata integrity, generate FAIR-compliant RO-Crate exports for sharing and publication, and manage access via role-based permissions

<!-- END PLACEHOLDER -->

## 3. Scope

### In scope

<!-- PLACEHOLDER: The in-scope features below are illustrative. Refine based on MVP definition and roadmap priorities. -->

- **Project lifecycle management** – Create projects, invite collaborators, assign roles (PI, Project Manager, Scientist, Lab Manager, Bioinformatician), track project status
- **Experimental design** – Define experiments within a project; document experimental groups, variables (e.g., treatment, timepoint, disease stage), and confounding factors
- **Sample registration and metadata capture** – Bulk register samples via XLSX/TSV upload; capture structured metadata (material type, organism, tissue, extraction method); link to external identifiers (OpenBIS sample codes)
- **Measurement integration** – Register measurement batches (NGS, proteomics, metabolomics); capture measurement parameters (sequencing depth, instrument model, data quality scores); track measurement files
- **File management** – Upload, store, and download raw data files; metadata-driven file organization; checksums for integrity
- **FAIR data export** – Generate RO-Crate bundles for research objects, enable archival and publication workflows, include project metadata, sample metadata, measurement parameters, and data availability statements
- **User authentication and authorization** – Local username/password registration, ORCID OAuth2 login, role-based access control, personal access tokens for programmatic API access
- **Notifications and audit** – Email notifications for project events, announcements, audit logs for data access and modifications

<!-- END PLACEHOLDER -->

### Out of scope

<!-- PLACEHOLDER: The out-of-scope items below clarify feature boundaries. Adjust based on stakeholder feedback and prioritization. -->

- **Raw data processing and analysis** – The platform does not run bioinformatics pipelines; it manages metadata and file references
- **Real-time instrument control** – Integration with lab equipment (sequencers, mass spectrometers) for live parameter push; rely on asynchronous batch imports instead
- **Advanced statistical analysis within the platform** – Users export data and perform analysis in dedicated tools (R, Python)
- **Commercial accounting/billing** – Financial offer tracking is provided, but detailed cost center integration and invoice generation are out of scope
- **Graphical experiment design tools** – Experiment structure is defined via structured forms, not drag-and-drop graph editors
- **Machine learning-based metadata validation** – Validation rules are rule-based and ontology-driven, not ML-based

<!-- END PLACEHOLDER -->

## 4. Success metrics

<!-- PLACEHOLDER: The metrics below are illustrative. Define baseline, targets, and measurement methods based on deployment and user engagement. -->

| Metric | Baseline | Target | Measurement Method |
|--------|----------|--------|-------------------|
| Project creation rate | 0 | 15–20 projects/month within 6 months of launch | Platform analytics / project creation logs |
| Sample registration volume | 0 | 5,000+ samples registered/month within 9 months | Database query on `sample` table |
| Measurement data uploads | 0 | 200+ batches/month by month 12 | File system and database audit logs |
| FAIR export usage | 0 | 20+ RO-Crate exports/month by month 12 | RO-Crate generation event logs |
| User adoption (active monthly users) | 0 | 50+ researchers and staff by month 12 | Login and action tracking via audit logs |
| Data integrity (file checksum match) | N/A | 99.9% file integrity rate | Automated checksum validation on download |
| CARE principle compliance | N/A | 100% of projects include attribution, minimal data collection, community-driven governance documentation | Manual audit of exported metadata |

<!-- END PLACEHOLDER -->

## 5. Constraints

<!-- PLACEHOLDER: The constraints below are illustrative. Validate against institutional policy, funding, and infrastructure reality before finalizing. -->

### Security & Privacy
- **Data classification:** All biomedical research data falls under German data protection law (GDPR, LMU Policy for Research Data); personally identifiable information (PII) in clinical metadata must be encrypted at rest and in transit
- **Access control:** Role-based access control (RBAC) and discretionary access control (DAC) via Spring Security ACL; PI controls project-level permissions
- **Audit logging:** All data access, modifications, and exports must be logged with timestamp, user ID, and action type; logs retained for 7 years per institutional policy

### Regulatory & Governance
- **FAIR principles:** Data must be Findable (discoverable via RO-Crate metadata), Accessible (download with proper authentication), Interoperable (standard ontologies and formats), Reusable (clear licensing and attribution)
- **CARE principles:** Indigenous data governance considerations; community benefit and reciprocity emphasized in data sharing agreements; data sovereignty respected in project setup
- **Institutional compliance:** Align with QBiC service level agreements (SLAs) for partner labs; ensure compatibility with OpenBIS integration for existing workflows

### Technical Constraints
- **Authentication:** ORCID OAuth2 integration for researcher identity; local password-based authentication as fallback
- **Data persistence:** MariaDB 10.6+ as primary data store; schema migration strategy required for updates; no breaking schema changes without migration tooling
- **Integrations:** OpenBIS API (asynchronous batch imports only); TIB Terminology Service for ontology lookup; ROR API for organization validation
- **Scalability:** Support 100+ concurrent users, 10+ GB monthly data uploads; performance SLA: page load < 2 seconds, API response < 500 ms for 95th percentile
- **Performance targets:** Background jobs (sample sync, file upload) complete within 5 minutes for typical batch sizes (1,000 samples)

### Infrastructure
- **Server:** Java 21, Spring Boot 3.5+, Vaadin 24+; containerized deployment (Docker); can run on-premises or cloud (AWS, Azure)
- **Messaging:** Apache ActiveMQ Artemis for async event communication between bounded contexts
- **Caching:** EHCache for session and query result caching to reduce database load
- **Backup & recovery:** Daily automated backups; recovery time objective (RTO) 24 hours; recovery point objective (RPO) < 1 hour

<!-- END PLACEHOLDER -->

## 6. Open questions

<!-- PLACEHOLDER: The questions below are illustrative. Track and resolve with stakeholders during requirements validation and design phases. -->

- [ ] **Data deletion policy:** Should researchers be able to delete projects and associated samples, or only deactivate them? What is the retention window for audit logs after project deletion?
- [ ] **OpenBIS synchronization:** Should measurement data sync from OpenBIS be real-time (pull on project view) or scheduled (nightly batch)? What is acceptable latency?
- [ ] **Multi-site deployments:** Should the platform support federated/distributed deployments across multiple institutional sites, or is a single-instance SaaS model acceptable?
- [ ] **Ontology customization:** Can research groups define custom vocabularies for variables and measurement parameters, or must they use pre-approved ontologies (NCBI, EBI, CHEBI)?
- [ ] **Data archival integration:** Should completed projects be automatically moved to long-term archival storage (e.g., TAPE, institutional repository), and when?
- [ ] **Cost model:** Will projects include cost-per-sample tracking tied to financial offers, or is metadata-only sufficient for MVP?
- [ ] **Mobile/offline access:** Is mobile app or offline data access a priority, or is web-only acceptable for MVP?
- [ ] **Internationalization:** Should the UI support languages other than English (e.g., German for local staff)?

<!-- END PLACEHOLDER -->
