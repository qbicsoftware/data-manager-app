# Implementation Plan: Delete Immunopeptidomics Measurements

> **Issue:** [#1430](https://github.com/qbicsoftware/data-manager-app/issues/1430)  
> **Parent Feature:** [#1412](https://github.com/qbicsoftware/data-manager-app/issues/1412) (FEAT-IMMUNOPEPTIDOMICS-MEASUREMENT)  
> **Task:** [#1415](https://github.com/qbicsoftware/data-manager-app/issues/1415)  
> **Requirement IDs:** `MEASUREMENT-R-03`  
> **Constraint:** `LAB-C-01` (OpenBIS synchronisation required)  

---

## 0. Critical Prerequisite: Merge `origin/development`

**DO NOT IMPLEMENT FROM SCRATCH.** The `origin/development` branch already contains the bulk of the immunopeptidomics scaffolding (Domain model, JPA repositories, Service methods, UI tabs). 

Before making any changes, merge the latest development state into this branch to avoid massive merge conflicts and duplicate work:

```bash
git checkout development
git pull origin development
git checkout issue-1430--story--delete-immunopeptidomics-measurements
git merge development
```
*Resolve any merge conflicts by prioritizing the `development` branch's scaffolding, while preserving your local additions to `MeasurementDataRepo` and `OpenbisConnector`.*

---

## 1. Scope & Acceptance Criteria

| # | Acceptance Criteria | Implementation Mapping |
|---|---|---|
| 1 | Given a user with management rights selects immunopeptidomics measurements for deletion, when they confirm, then measurements with no attached dataset are deleted successfully. | Ensure `deleteAllIP` checks `hasDataAttached` → deletes from JPA → deletes from OpenBIS → dispatches `ProjectChanged`. |
| 2 | Given a user with management rights deletes an immunopeptidomics measurement with an attached dataset, the deletion must fail and the user shall be informed to delete the raw datasets first. | Throw `MeasurementDeletionException(DeletionErrorCode.DATA_ATTACHED)` before any mutations. UI already handles this generically. |
| 3 | Given a user with view-only rights views the measurement page, they shall not be able to delete measurements. | Already enforced by `@PreAuthorize("WRITE")` on `MeasurementService.deleteIpMeasurements`. |

---

## 2. Current State Analysis (Post-Merge)

After merging `origin/development`, the following will **already exist** and require **no changes**:
- ✅ `MeasurementCode.MEASUREMENT_PREFIX.IP` + `createIP()` + `isIPDomain()`
- ✅ `ImmunopeptidomicsMeasurement` domain aggregate
- ✅ `ImmunopeptidomicsMeasurementJpaRepo` + JPA entities
- ✅ `MeasurementRepository.deleteAllIP()` signature
- ✅ `MeasurementDomainService.deleteIpById()`
- ✅ `MeasurementService.deleteIpMeasurements()` with `@PreAuthorize(WRITE)`
- ✅ UI: IP tab in `MeasurementDetailsComponent`, deletion event wiring in `MeasurementMain`
- ✅ DB Schema: `ip_measurements` tables

### 🚨 The Gap to Fix
Inspection of `origin/development` reveals that `MeasurementRepositoryImplementation.deleteAllIP` currently **skips the dataset guard** and **does not sync with OpenBIS**:
```java
// Current broken state in development:
// IP measurements don't have data in OpenBIS, so skip the hasDataAttached check
ipMeasurementJpaRepo.deleteAll(matchingMeasurements);
```
This violates Acceptance Criteria #1 and #2, and Constraint `LAB-C-01`. **This is the sole focus of this story.**

---

## 3. Implementation Steps (Post-Merge)

### Step 1: Verify/Update `MeasurementDataRepo`
**File:** `project-management-infrastructure/.../experiment/measurement/MeasurementDataRepo.java`

Ensure this method exists (you may have already added it locally):
```java
void deleteImmunopeptidomicsMeasurements(List<ImmunopeptidomicsMeasurement> measurements);
```
*(Note: `hasDataAttached(List<MeasurementCode>)` requires **no changes**; it is already measurement-type-agnostic.)*

### Step 2: Implement OpenBIS Deletion Sync
**File 1:** `project-management-infrastructure/.../sample/openbis/OpenbisConnector.java`

Ensure this method exists and delegates to the generic helper:
```java
@Override
public void deleteImmunopeptidomicsMeasurements(List<ImmunopeptidomicsMeasurement> measurements) {
  deleteMeasurements(measurements.stream().map(ImmunopeptidomicsMeasurement::measurementCode).toList());
}
```

**File 2:** `project-management-infrastructure/.../sample/openbis/MockConnector.java`

Ensure the no-op stub exists for the development profile:
```java
@Override
public void deleteImmunopeptidomicsMeasurements(List<ImmunopeptidomicsMeasurement> measurements) {
  // No-op for development profile
}
```

### Step 3: Fix `MeasurementRepositoryImplementation.deleteAllIP` (CRITICAL)
**File:** `project-management-infrastructure/.../experiment/measurement/MeasurementRepositoryImplementation.java`

**Replace** the current broken implementation with the guarded pattern that mirrors `deleteAllNgs` / `deleteAllProteomics`:

```java
@Override
public void deleteAllIP(Set<String> measurementIds) {
  if (measurementIds.isEmpty()) {
    return;
  }
  List<ImmunopeptidomicsMeasurement> matchingMeasurements = ipMeasurementJpaRepo.findAllById(
      measurementIds.stream().map(MeasurementId::parse).collect(Collectors.toSet()));
  
  // FIX: Enforce dataset guard per Story #1430 Acceptance Criteria
  if (measurementDataRepo.hasDataAttached(
      matchingMeasurements.stream().map(ImmunopeptidomicsMeasurement::measurementCode).toList())) {
    throw new MeasurementDeletionException(DeletionErrorCode.DATA_ATTACHED);
  }
  
  try {
    deleteAllIP(matchingMeasurements);
  } catch (Exception e) {
    log.error("IP Measurement deletion failed due to " + e.getMessage());
    throw new MeasurementDeletionException(DeletionErrorCode.FAILED);
  }
}

private void deleteAllIP(List<ImmunopeptidomicsMeasurement> measurements) {
  ipMeasurementJpaRepo.deleteAll(measurements);
  // FIX: Ensure OpenBIS synchronisation (LAB-C-01)
  measurementDataRepo.deleteImmunopeptidomicsMeasurements(measurements);
}
```

### Step 4: Verify UI Error Handling (No Code Changes Required)
**File:** `datamanager-app/.../views/projects/project/measurements/MeasurementMain.java`

The existing `handleDeletionError` method already handles this generically:
```java
private void handleDeletionError(MeasurementDeletionException error) {
  String errorMessage = switch (error.reason()) {
    case FAILED -> "Deletion failed. Please try again.";
    case DATA_ATTACHED -> "Data is attached to one or more measurements."; // Matches AC
  };
  showErrorNotification("Deletion failed", errorMessage);
}
```
✅ **Action:** Just verify via manual testing that this toast appears when attempting to delete an IP measurement with attached data.

---

## 4. File Inventory (Post-Merge)

### Files to Modify
| # | Path | Change |
|---|---|---|
| 1 | `MeasurementDataRepo.java` | Ensure `deleteImmunopeptidomicsMeasurements` is declared. |
| 2 | `OpenbisConnector.java` | Ensure `deleteImmunopeptidomicsMeasurements` delegates to `deleteMeasurements`. |
| 3 | `MockConnector.java` | Ensure no-op stub exists. |
| 4 | `MeasurementRepositoryImplementation.java` | **CRITICAL:** Add `hasDataAttached` guard and OpenBIS deletion call. |

### Files Already Complete (No Action Required Post-Merge)
- ✅ `MeasurementCode.java`
- ✅ `ImmunopeptidomicsMeasurement.java`
- ✅ `ImmunopeptidomicsMeasurementJpaRepo.java`
- ✅ `MeasurementRepository.java`
- ✅ `MeasurementDomainService.java`
- ✅ `MeasurementService.java`
- ✅ `MeasurementMain.java` / `MeasurementDetailsComponent.java`
- ✅ `sql/complete-schema.sql`

---

## 5. Testing Plan

### Unit Tests (Spock)
- **`MeasurementRepositoryImplementationSpec`**:
  - `def "deleteAllIP throws DATA_ATTACHED when measurements have attached data"` → Mock `measurementDataRepo.hasDataAttached` to return `true`, assert exception thrown, assert **no** JPA/OpenBIS deletion calls are made.
  - `def "deleteAllIP deletes from JPA and OpenBIS when no data attached"` → Mock `hasDataAttached` to return `false`, verify `ipMeasurementJpaRepo.deleteAll` and `measurementDataRepo.deleteImmunopeptidomicsMeasurements` are invoked exactly once.

### Manual / UI Verification
1. Log in as a project manager.
2. Navigate to an experiment with an immunopeptidomics measurement.
3. Ensure Raw Data exists for that measurement.
4. Select the measurement, click "Delete", and confirm.
5. **Expected:** Red error toast: *"Deletion failed: Data is attached to one or more measurements."*
6. Delete the raw data via the Raw Data tab.
7. Repeat deletion.
8. **Expected:** Green success toast, measurement disappears from the grid.

---

## 6. Constraints & Guardrails

| Constraint | How Addressed |
|---|---|
| **AC: Deletion must fail if dataset attached** | `MeasurementRepositoryImplementation.deleteAllIP` now explicitly calls `hasDataAttached(...)` **before** any mutations. |
| **LAB-C-01: OpenBIS synchronisation** | `deleteAllIP` now calls `measurementDataRepo.deleteImmunopeptidomicsMeasurements(...)`, triggering `OpenbisConnector`. |
| **Security: WRITE permission required** | Already enforced by `@PreAuthorize` on `MeasurementService.deleteIpMeasurements`. |
| **Exception Handling: No silent swallowing** | Existing pattern maintained: catch block logs error and wraps in `MeasurementDeletionException(FAILED)`. |

---

## 7. Traceability & PR Checklist

```
PRD §<immunopeptidomics lifecycle>
  └── Feature #1412 (FEAT-IMMUNOPEPTIDOMICS-MEASUREMENT)
        └── Story #1430 (Delete Immunopeptidomics Measurements)
              └── Task #1415 (Implement deletion with dataset guard)
                    └── Implementation (this plan)
```

**PR Checklist:**
- [ ] **Merge `origin/development` into this branch before starting.**
- [ ] Reference Issue #1430 and Task #1415 in PR description.
- [ ] Explicitly note in PR description: *"Fixes regression in development where IP deletion skipped the dataset guard and OpenBIS sync."*
- [ ] List requirement IDs addressed: `MEASUREMENT-R-03`, `LAB-C-01`.
- [ ] Run `./mvnw clean verify` (unit + integration tests).
- [ ] Ensure SonarCloud quality gate passes.
