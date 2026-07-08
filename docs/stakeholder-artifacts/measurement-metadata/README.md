# Immunopeptidomics Measurement Metadata Specification

## Current Approved Version

| Document | Version | Date | Approved By | Status |
|---|---|---|---|---|
| `immunopeptidomics-registration-spec-v1.0-2026-05-11.xlsx` | 1.0 | 2026-05-11 | Stakeholder (Immunopeptidomics Partner Facility) | Approved |

This Excel workbook is the **authoritative stakeholder specification** for immunopeptidomics measurement metadata. It contains two sheets:

1. **`Metadata`** — The actual Excel registration template with column headers.
2. **`Property information`** — The formal field specification: property name, position, provision, category, allowed values, and descriptions.

## Field Specification (Extracted from `Property information`)

The following table reproduces the `Property information` sheet exactly, ordered by **Position** (1-based column index) to match the Excel template layout. **The Excel sheet remains the source of truth;** this Markdown is a diffable reference derived from it.

| Pos | Property | Provision | Category | Allowed values | Description |
|---|---|---|---|---|---|
| 1 | QBiC Sample Id* | mandatory | Sample Identity | QBiC sample ids | Each measurement need to be linked to at least on analyte sample. |
| 2 | Sample Name | optional | Sample Identity | free text | Coprocessing Identifier. This is just a visual aid simplify sample navigation for the person managing the metadata. You can e.g. download the sample metadata and copy the sample ID + label column in here. This column gets ignored during measurement registration |
| 3 | Measurement Name | optional | Measurement Identity | free text | Internal Identifier used by the partner facility to enable them to track their measurement |
| 4 | Cycle/Fraction Name | optional | Sample Properties | Free text, e.g. Fraction01, AB | Sometimes a sample is fractionated and all fractions are measured. With this property you can indicate which fraction it is. |
| 5 | Sample Mass (mg)* | mandatory | Sample Properties | decimal | Mass that was harvested from the biological probe (mg) |
| 6 | Sample Volume (decimal)* | mandatory | Sample Properties | decimal | Volume after enrichment that was injected into the mass spectrometer (microliter) |
| 7 | Prep Date | optional | Sample Provenance | YYYY-MM-DD (ISO 8601) | Includes the date of the sample preparation (YYYYMMDD) |
| 8 | Enrichment method* | mandatory | Analyte Properties | free text | Method to enrich HLA peptides in sample volume ( e.g. immunoaffinity purification, immunoaffinity purification (iodoacetamide), mild acid elution, detergent lysis ) |
| 9 | MHC Antibody* | mandatory | Analyte Properties | free text | The MHC Antibody that was used for the measurement |
| 10 | MHC Typing Method | optional | Analyte Properties | free text | Method used to obtain the donors HLA typing (e.g. DNA seq-based with Optitype, RNA-seq-based with HLA-LA, Immunopeptidomics-based with immunotype) |
| 11 | Facility* | mandatory | Organisational Provenance | free text | Ideally the facilites name within the organisation (groupname, etc.) |
| 12 | Organisation URL* | mandatory | Organisational Provenance | URL | For good provenance tracking and enabling FAIR, we need a persistent and unique identifier of the organisation the measurement has been conducted at. We expect a full ROR id as URL (e.g. https://ror.org/03a1kwz48) |
| 13 | MS Run Date | optional | Measurement Provenance | YYYY-MM-DD (ISO 8601) | Includes the date of the sample measurement on the MS (YYYYMMDD) |
| 14 | Data Acquisition* | mandatory | Instrument | Free text | Mass spectrometer acquisition mode (e.g. DDA, DIA, PRM etc.) |
| 15 | Instrument* | mandatory | Instrument | CURIE (ontology) | The instrument model that has been used for the measurement, which needs to be an ontology CURIE that will be resolved to an existing persistent ID. You can use the ontology search in the data manager to get the CURIE for an instrument model. |
| 16 | LCMS Method* | mandatory | Instrument | free text | Laboratory specific methods that have been used for LCMS measurements (e.g., CIDOT, HCDOT, MSV035..). |
| 17 | LC Column* | mandatory | Instrument | Free text, can be a commercial name or brand | The type of column that has been used. |
| 18 | Charge range* | mandatory | Run Parameters | free text | Charge window where the mass spectrometer method was designed to analyze precursors. Units are either m/z or Dalton |
| 19 | Ion mobility range (1/k0) | optional | Run Parameters | free text | Ion mobility window where the mass spectrometer method was designed to analyze precursors. Units are 1/k0 or CCS. |
| 20 | Mass range (m/z)* | mandatory | Run Parameters | free text | Mass window where the mass spectrometer method was designed to analyze precursors. Units are either m/z or Dalton |
| 21 | Retention time range (min)* | mandatory | Run Parameters | integer | Time of chromatogram gradient (min) |
| 22 | Comment | optional | Notes | free text | Any other comments that can be noted (issue at the machines, during isolation or if the sample is excluded from the rest of the analysis) |

## Mandatory / Optional Summary

- **Mandatory (14):** QBiC Sample Id*, Sample Mass (mg)*, Sample Volume (decimal)*, Enrichment method*, MHC Antibody*, Facility*, Organisation URL*, Data Acquisition*, Instrument*, LCMS Method*, LC Column*, Charge range*, Mass range (m/z)*, Retention time range (min)*
- **Optional (8):** Sample Name, Measurement Name, Cycle/Fraction Name, Prep Date, MHC Typing Method, MS Run Date, Ion mobility range (1/k0), Comment

## Category Summary

| Category | Fields | Count |
|---|---|---|
| Sample Identity | QBiC Sample Id*, Sample Name | 2 |
| Measurement Identity | Measurement Name | 1 |
| Sample Properties | Sample Mass (mg)*, Sample Volume (decimal)*, Cycle/Fraction Name | 3 |
| Sample Provenance | Prep Date | 1 |
| Analyte Properties | Enrichment method*, MHC Antibody*, MHC Typing Method | 3 |
| Organisational Provenance | Facility*, Organisation URL* | 2 |
| Measurement Provenance | MS Run Date | 1 |
| Instrument | Data Acquisition*, Instrument*, LCMS Method*, LC Column* | 4 |
| Run Parameters | Charge range*, Ion mobility range (1/k0), Mass range (m/z)*, Retention time range (min)* | 4 |
| Notes | Comment | 1 |

## Architecture Notes

- **Position** (1-based column index) is relevant **only for Excel I/O** — template generation, parsing, and validation of uploaded sheets. It must not leak into the domain model or database schema.
- **Category** is a documentation / UI grouping concept. It may be used for logical section grouping in the measurement detail view or help text, but is not a domain invariant.

## Traceability

| Governance Item | Reference |
|---|---|
| **Requirement** | `MEASUREMENT-R-01` — Immunopeptidomics Measurement Registration |
| **Requirement** | `MEASUREMENT-R-02` — Immunopeptidomics Measurement Editing |
| **Feature** | `FEAT-IMMUNOPEPTIDOMICS-MEASUREMENT` |
| **Story** | #1428 — Register Immunopeptidomics Measurements via Excel Template |
| **Story** | #1429 — Edit Immunopeptidomics Measurements via Pre-filled Excel Template |
| **Task** | #1413 — Registration of Immunopeptidomics Measurements |
| **Task** | #1414 — Editing of Immunopeptidomics Measurements |

## Change Log

| Version | Date | Change Summary | Approved By |
|---|---|---|---|
| 1.0 | 2026-05-11 | Initial field specification for registration template, handed over by immunopeptidomics partner facility. Includes 22 fields (14 mandatory, 8 optional), position, and category metadata. | Stakeholder |
