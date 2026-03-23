# Test Plan for Issue #1401 - Sample Metadata Validation Fix

## Overview

This document outlines the testing strategy for verifying the fix for GitHub Issue #1401: 
"Partial editing of sample metadata possible although invalid information present"

## Summary of Changes

### Code Changes Made

1. **EditSampleBatchDialog.java** - Made `uploadWithDisplay` an instance field (was local variable)
2. **EditSampleBatchDialog.java** - Added validation checks in `onConfirmClicked()` method
3. **EditSampleBatchDialog.java** - Added test helper methods:
   - `getValidatedSampleMetadata()`
   - `getUploadWithDisplay()`
   - `setValidatedSampleMetadataForTest()`

### Files Modified

- `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/samples/registration/batch/EditSampleBatchDialog.java`

### Files Created

- `datamanager-app/src/test/groovy/life/qbic/datamanager/views/projects/project/samples/registration/batch/EditSampleBatchDialogSpec.groovy`

---

## Unit Tests

### Test File Location
`datamanager-app/src/test/groovy/life/qbic/datamanager/views/projects/project/samples/registration/batch/EditSampleBatchDialogSpec.groovy`

### Test Coverage

#### 1. Initialization Tests
- ✓ Dialog is instantiated correctly
- ✓ `getValidatedSampleMetadata()` returns empty list initially
- ✓ `getUploadWithDisplay()` returns the upload component

#### 2. Metadata Management Tests
- ✓ `setValidatedSampleMetadataForTest()` populates the list correctly
- ✓ `setValidatedSampleMetadataForTest()` with empty list clears metadata
- ✓ `setValidatedSampleMetadataForTest()` replaces old metadata with new
- ✓ Multiple samples can be stored and retrieved
- ✓ `getValidatedSampleMetadata()` returns a copy, not reference to internal list

#### 3. Issue #1401 Fix Verification Tests
- ✓ Dialog can handle adding/removing listeners multiple times
- ✓ Listener registration succeeds
- ✓ getValidatedSampleMetadata returns immutable view of internal list

### Running Unit Tests

```bash
# Run only the EditSampleBatchDialog tests
./mvnw test -Dtest=EditSampleBatchDialogSpec

# Run all tests in the project
./mvnw clean test

# Run with specific Maven profile
./mvnw test -Pdevelopment
```

---

## Integration Testing

### Prerequisites

1. Running Data Manager application in development mode
2. Valid user account with project/experiment access
3. Existing project with experimental groups and sample batches
4. Test XLSX files (valid and invalid examples from issue #1401)

### Manual Test Cases

#### Test Case 1: No File Uploaded - Click Confirm
**Objective:** Verify that confirm button doesn't proceed when no file is uploaded

1. Open Data Manager and navigate to a project's sample batch
2. Click "Edit" on an existing batch
3. **DO NOT** upload any file
4. Enter a valid batch name (e.g., "Test Batch")
5. Click "Edit Batch" button
6. **Expected Result:** Dialog should not close; error message displayed
7. **Actual Result:** _________ (mark as PASS/FAIL)

#### Test Case 2: Invalid File (Validation Failed) - Click Confirm
**Objective:** Verify that confirm button doesn't proceed when validation fails

1. Open Data Manager and navigate to a project's sample batch
2. Click "Edit" on an existing batch
3. Download the metadata template
4. Open the XLSX file and delete values from required columns:
   - Delete all "Species" values
   - Delete all "Specimen" values
   - Delete all "Analyte" values
5. Save the modified file
6. Upload the invalid XLSX file
7. Wait for validation to complete (see "Validating file..." progress)
8. Observe "Invalid sample metadata" error message with specific errors shown
9. Enter a valid batch name
10. Click "Edit Batch" button
11. **Expected Result:** 
    - Dialog should NOT close
    - Error message should remain visible
    - Button should be unresponsive
12. **Actual Result:** _________ (mark as PASS/FAIL)

#### Test Case 3: Valid File (Validation Passed) - Click Confirm
**Objective:** Verify that confirm button DOES proceed when validation passes

1. Open Data Manager and navigate to a project's sample batch
2. Click "Edit" on an existing batch
3. Download the metadata template
4. Keep the file as-is (or make minor non-breaking changes)
5. Upload the valid XLSX file
6. Wait for validation to complete
7. Observe "Your data has been approved" success message
8. Enter a valid batch name
9. Click "Edit Batch" button
10. **Expected Result:**
    - Dialog should close
    - Success toast should appear: "Sample batch updated successfully"
    - Sample data should be updated in the system
11. **Actual Result:** _________ (mark as PASS/FAIL)

#### Test Case 4: Upload Invalid, Then Upload Valid
**Objective:** Verify that state is correctly managed when uploading multiple files

1. Open Data Manager and navigate to a project's sample batch
2. Click "Edit" on an existing batch
3. Download the metadata template
4. Create invalid version (delete required fields) and upload
5. Wait for validation to fail
6. Observe error message: "Invalid sample metadata"
7. Click the X button to remove the invalid file
8. Download the template again
9. Upload a valid file
10. Wait for validation to succeed
11. Observe "Your data has been approved" message
12. Enter batch name and click "Edit Batch"
13. **Expected Result:**
    - Dialog should close and registration should proceed
    - Success toast should appear
14. **Actual Result:** _________ (mark as PASS/FAIL)

#### Test Case 5: Empty Batch Name with Valid File
**Objective:** Verify that validation still enforces batch name requirement

1. Open Data Manager and navigate to a project's sample batch
2. Click "Edit" on an existing batch
3. Upload a valid file
4. **DO NOT** enter a batch name (leave it empty)
5. Click "Edit Batch" button
6. **Expected Result:**
    - Dialog should NOT close
    - Batch name field should show error
    - Batch name field should receive focus
7. **Actual Result:** _________ (mark as PASS/FAIL)

#### Test Case 6: Register New Sample Batch (Verify Consistency)
**Objective:** Verify that RegisterSampleBatchDialog still works correctly

1. Open Data Manager and navigate to a project's sample batch section
2. Click "Register batch" (or click the register button in the disclaimer)
3. Download the sample registration template
4. Upload a valid file
5. Enter a batch name
6. Click "Register" button
7. **Expected Result:**
    - Dialog should close
    - Success toast should appear: "Sample batch is successfully registered"
8. **Actual Result:** _________ (mark as PASS/FAIL)

#### Test Case 7: Cross-Browser Testing
**Objective:** Verify fix works across all supported browsers

Repeat Test Cases 2-5 in each browser:
- [ ] Google Chrome (latest)
- [ ] Mozilla Firefox (latest)
- [ ] Microsoft Edge (latest)
- [ ] Safari (latest)

---

## Regression Testing Checklist

- [ ] Sample registration workflow still works (RegisterSampleBatchDialog)
- [ ] Sample editing workflow still works (EditSampleBatchDialog)
- [ ] Batch deletion still works
- [ ] File upload still validates correctly
- [ ] Validation error messages still display correctly
- [ ] Success notifications still appear when registration completes
- [ ] Cancel button still works
- [ ] Dialog close button (X) still works
- [ ] Keyboard shortcuts (Escape key) still work
- [ ] Multiple samples can be registered in a batch
- [ ] Batch name field validation still works
- [ ] Download template functionality still works

---

## Performance Testing

### Test Case: No Performance Degradation
**Objective:** Verify that the fix does not impact dialog performance

1. Upload a valid file with 100+ samples
2. Wait for validation to complete
3. Measure time to validation completion
4. **Expected Result:** Validation should complete in < 10 seconds
5. **Actual Result:** _________ seconds

---

## Edge Cases

### Test Case 1: File with Mixed Valid/Invalid Rows
**Objective:** Verify validation correctly identifies which rows are invalid

1. Create XLSX file with:
   - Rows 1-5: Valid data (all required fields present)
   - Rows 6-8: Missing species
   - Rows 9-10: Valid data
2. Upload the file
3. **Expected Result:**
   - Validation should fail
   - Error should show "Missing species for 3 samples"
   - NOT allow confirmation
4. **Actual Result:** _________ (mark as PASS/FAIL)

### Test Case 2: File with Empty Content (No Samples)
**Objective:** Verify validation rejects empty/headerless files

1. Create an empty XLSX file (no samples)
2. Upload it
3. **Expected Result:**
   - Validation should fail
   - Error message should display
   - Dialog should not allow confirmation
4. **Actual Result:** _________ (mark as PASS/FAIL)

### Test Case 3: Very Large File (Edge Case)
**Objective:** Verify large files are handled correctly

1. Create XLSX file with 1000+ samples (all valid)
2. Upload the file
3. **Expected Result:**
   - Validation should complete (may take longer)
   - If valid, should allow confirmation
   - If hits size limit, should show appropriate error
4. **Actual Result:** _________ (mark as PASS/FAIL)

---

## Acceptance Criteria Verification

### Requirement: User cannot register invalid sample metadata

- **Test:** Upload invalid file and click confirm
- **Expected:** Confirm button does not trigger registration
- **Status:** ✓ PASS / ✗ FAIL

### Requirement: User can register valid sample metadata

- **Test:** Upload valid file and click confirm
- **Expected:** Dialog closes and registration proceeds
- **Status:** ✓ PASS / ✗ FAIL

### Requirement: Error messages are shown when validation fails

- **Test:** Upload invalid file
- **Expected:** Error messages visible describing what is invalid
- **Status:** ✓ PASS / ✗ FAIL

### Requirement: Dialog behavior is consistent with RegisterSampleBatchDialog

- **Test:** Compare behavior between register and edit dialogs
- **Expected:** Same validation logic and behavior
- **Status:** ✓ PASS / ✗ FAIL

---

## Test Results Summary

| Test Case | Status | Notes |
|-----------|--------|-------|
| No File Uploaded | | |
| Invalid File - Validation Failed | | |
| Valid File - Validation Passed | | |
| Upload Invalid, Then Valid | | |
| Empty Batch Name with Valid File | | |
| Register New Sample Batch | | |
| Chrome | | |
| Firefox | | |
| Edge | | |
| Safari | | |
| File with Mixed Rows | | |
| Empty Content File | | |
| Large File (1000+ samples) | | |

---

## Known Issues / Limitations

### N/A at this time

---

## Sign-Off

- **Developer:** _________________________
- **Date:** _________________________
- **Reviewer:** _________________________
- **Date:** _________________________

---

## Appendix: Test Data

### Valid XLSX Template Structure

Required columns:
- Sample Code
- Sample Name
- Biological Replicate
- Condition
- **Species** (required - do NOT delete)
- **Specimen** (required - do NOT delete)
- **Analyte** (required - do NOT delete)
- Analysis Method
- Comment
- Confounding Variables

### Invalid Test File Creation

1. Download valid template
2. Open in Excel/LibreOffice
3. Select the entire "Species" column (or Specimen, Analyte)
4. Press DELETE to clear all values
5. Save the file
6. Upload and verify validation fails

---

## Additional Notes

### Issue #1401 Context
- **Bug:** Users could register sample batches with invalid/incomplete data by clicking confirm while errors were displayed
- **Root Cause:** Missing validation checks in EditSampleBatchDialog.onConfirmClicked()
- **Fix:** Added checks to verify validatedSampleMetadata is non-empty before allowing registration

### Related Components
- RegisterSampleBatchDialog (should have consistent behavior - already fixed)
- SampleInformationMain (event handler for both dialogs)
- SampleValidationService (performs actual validation)
- UploadWithDisplay (upload component)

