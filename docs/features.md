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

---

## Active Features

### FEAT-IMMUNOPEPTIDOMICS-MEASUREMENT

| Field | Value |
|---|---|
| **Description** | Full lifecycle support for immunopeptidomics measurements — registration, editing, deletion, raw data upload/download |
| **PRD Section** | §3 Scope — Measurement integration; File management |
| **Requirements** | `MEASUREMENT-R-01`, `MEASUREMENT-R-02`, `MEASUREMENT-R-03`, `MEASUREMENT-R-04`, `DATA-R-01`, `DATA-R-02`, `DATA-R-03` |
| **GitHub Feature** | [#1412](https://github.com/qbicsoftware/data-manager-app/issues/1412) |
| **Status** | 🟡 In Progress |

#### Stories

| # | Title | Requirement IDs | Status | GitHub |
|---|---|---|---|---|
| #1428 | Register Immunopeptidomics Measurements via Excel Template | `MEASUREMENT-R-01` | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1428) |
| #1429 | Edit Immunopeptidomics Measurements via Pre-filled Excel Template | `MEASUREMENT-R-02` | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1429) |
| #1430 | Delete Immunopeptidomics Measurements | `MEASUREMENT-R-03` | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1430) |
| #1431 | View Immunopeptidomics Measurements in Measurement View | `MEASUREMENT-R-04` | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1431) |
| #1432 | View and Access Immunopeptidomics Raw Datasets | `DATA-R-01`, `DATA-R-02`, `DATA-R-03` | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1432) |

#### Tasks

| # | Title | Parent Story | Status | GitHub |
|---|---|---|---|---|
| #1413 | Registration of Immunopeptidomics Measurements | #1428 | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1413) |
| #1414 | Editing of Immunopeptidomics Measurements | #1429 | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1414) |
| #1415 | Deletion of Immunopeptidomics Measurements | #1430 | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1415) |
| #1417 | Show the Immunopeptidomics Measurements in the Measurement View | #1431 | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1417) |
| #1416 | Show Immunopeptidomic datasets within the raw data view | #1432 | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1416) |
| #1434 | Integration Test — End-to-End Verification | #1432 | 🔴 Open | [Link](https://github.com/qbicsoftware/data-manager-app/issues/1434) |

---

*Last updated: 2026-05-06*
