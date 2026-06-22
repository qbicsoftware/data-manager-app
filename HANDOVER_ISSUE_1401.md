# HANDOVER DOCUMENT: Issue #1401

## Issue Summary

**Title:** [Bug] Partial editing of sample metadata possible although invalid information present  
**Issue ID:** #1401  
**Severity:** High  
**Status:** Open (In Progress)  
**Component:** Sample Management - Edit Sample Batch Dialog

### One-Sentence Summary
Users can proceed with sample batch registration/editing despite validation errors being displayed, allowing invalid metadata to be processed.

---

## Issue Details

### Full Description
When editing a sample batch using the "Edit Sample Batch" workflow, users are able to click the confirmation button even after the validation process has detected errors. The dialog displays validation error messages (e.g., "missing species", "missing specimen"), but these errors do not prevent the user from confirming and proceeding with the registration operation.

### Steps to Reproduce

1. Navigate to a project with experiments and sample batches
2. On the Register/Edit Sample Batch page, click on **Edit** for an existing batch
3. Click **Download the metadata template** to get the XLSX file
4. Open the downloaded XLSX file and **remove required fields** (e.g., delete species, specimen, or analyte by pressing DEL)
5. Save and **upload the modified XLSX file** back to the dialog
6. Observe the validation results displayed in the upload section
7. **The validation display shows errors** (e.g., "Invalid sample metadata", "Missing species for 1 sample")
8. Click the **"Edit Batch" confirmation button** despite the errors being visible
9. **Expected:** Button should do nothing or be disabled  
   **Actual:** Dialog closes, registration is triggered, and a success toast is shown

### Environment Details

- **Affected Browsers:** Microsoft Edge, Safari, Firefox, Chrome (all major browsers)
- **Affected Workflows:** 
  - Edit Sample Batch (primary manifestation)
  - Likely also Register Sample Batch (same code structure)
- **Application Version:** 1.11.0

### Error Messages / Logs

**In Dialog Display:**
- "Invalid sample metadata"
- "Missing species for X samples"
- "Missing specimen for X samples"
- "Missing analyte for X samples"
- Other validation-related messages

**User Observation:**
- Success toast appears: "Your data has been approved" or similar
- Registration completes despite validation failures

### Provided Files

- Valid example: `2026-03-20_Q2RGU6_sample-metadata-update-template.xlsx`
- Invalid example: `2026-03-20_Q2RGU6_sample-metadata-update-template_invalid.xlsx`

---

## Behavior Analysis

### Expected Behavior

1. **Given:** A file has been uploaded and validation has completed with errors  
   **When:** User clicks the confirm button  
   **Then:** The button click should have no effect (remain in edit state, show error indicator on button)

2. **Given:** A file has been uploaded and validation has completed successfully  
   **When:** User clicks the confirm button  
   **Then:** Dialog should proceed with registration and display progress

3. **Given:** Validation is displaying errors  
   **When:** User attempts to proceed  
   **Then:** The confirm button should be visually disabled or the action should be blocked

### Observed Behavior

1. **Actual:** Even when validation errors are displayed (showing "Invalid sample metadata" with specific error counts), the confirm button remains active and clickable

2. **Actual:** Clicking the confirm button while validation errors are displayed:
   - Closes the dialog immediately
   - Triggers the confirmation event to the parent component
   - Proceeds with sample registration/update despite invalid data
   - Shows success notification

3. **Actual:** The `validatedSampleMetadata` list becomes populated with data even when validation has failed, because:
   - When validation fails, the UI display is updated with error information
   - The `validatedSampleMetadata` list is NOT cleared when validation shows failures
   - OR the state tracking doesn't prevent the confirm action from proceeding

### Impact

- **Severity:** High
- **User Impact:** Users can register invalid sample metadata that violates data quality requirements, compromising data integrity in the system
- **System Impact:** Invalid or incomplete sample metadata may propagate through the system, affecting:
  - Project data completeness
  - Subsequent operations dependent on sample metadata
  - FAIR data export compliance
  - Reporting and analysis accuracy

---

## Root Cause Analysis

### Probable Root Cause

The issue appears to be in the `onConfirmClicked()` method of both `EditSampleBatchDialog` and `RegisterSampleBatchDialog`. These dialogs perform validation checks on the batch name field, but **they do NOT validate that valid sample metadata has been uploaded and passed validation**.

**The gap:** When validation fails for the uploaded sample file:
1. The error display is updated in the UI (lines 216-221 in EditSampleBatchDialog)
2. `setValidatedSampleMetadata(List.of())` is called to clear the validated list
3. **However**, in the `onConfirmClicked()` method, there is no check to ensure `validatedSampleMetadata` is non-empty or that validation actually succeeded

**In EditSampleBatchDialog.onConfirmClicked() (lines 339-354):**
```java
@Override
protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
  if (batchNameField.isInvalid()) {
    batchNameField.focus();
    return;
  }
  if (batchNameField.isEmpty()) {
    batchNameField.setInvalid(true);
    batchNameField.focus();
    return;
  }
  // ❌ MISSING CHECK: if (validatedSampleMetadata.isEmpty()) { return; }
  fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), batchNameField.getValue(),
      Collections.unmodifiableList(validatedSampleMetadata)));
}
```

The method only validates the batch name, but never checks whether:
1. A file was actually uploaded
2. The validation for that file succeeded
3. The `validatedSampleMetadata` list is non-empty

**In RegisterSampleBatchDialog.onConfirmClicked() (lines 399-424):**
The register dialog has better logic (lines 411-421) that checks if data exists, but this check logic is only present in RegisterSampleBatchDialog, NOT in EditSampleBatchDialog.

### File Locations

**Primary Affected Files:**

1. **EditSampleBatchDialog.java** (lines 339-354)
   - Path: `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/samples/registration/batch/EditSampleBatchDialog.java`
   - Issue: Missing validation check in `onConfirmClicked()` to verify `validatedSampleMetadata` is non-empty

2. **RegisterSampleBatchDialog.java** (lines 399-424)
   - Path: `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/samples/registration/batch/RegisterSampleBatchDialog.java`
   - Status: Partially correct (has some checks, but inconsistent with edit dialog)
   - Issue: Logic differs between register and edit; should be consistent

**Supporting Files:**

3. **SampleInformationMain.java** (lines 415-481)
   - Path: `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/samples/SampleInformationMain.java`
   - Role: Handles confirm event from dialogs; calls registration service
   - Issue: Receives empty `validatedSampleMetadata` but still proceeds

---

## Affected Codebase Locations

### Primary Issue Locations

#### 1. EditSampleBatchDialog - onConfirmClicked Method
**File:** `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/samples/registration/batch/EditSampleBatchDialog.java`

**Lines 339-354:**
```java
@Override
protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
  if (batchNameField.isInvalid()) {
    // once the user focused the batch name field at least once, the setRequired(true) validation is applied.
    batchNameField.focus();
    return;
  }
  if (batchNameField.isEmpty()) {
    // if the user never focused the name field, no validation took place. Thus, the need to double-check here.
    batchNameField.setInvalid(true);
    batchNameField.focus();
    return;
  }
  fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), batchNameField.getValue(),
      Collections.unmodifiableList(validatedSampleMetadata)));
  // ❌ NO VALIDATION THAT validatedSampleMetadata IS NON-EMPTY
}
```

**Problem:** The method doesn't verify that:
- A file was uploaded
- Validation succeeded
- `validatedSampleMetadata` list is non-empty

#### 2. EditSampleBatchDialog - validatedSampleMetadata Field
**File:** Same as above  
**Lines 75:**
```java
private final transient List<SampleMetadata> validatedSampleMetadata;
```

**Current State:** Initialized as `new ArrayList<>()` (line 118)  
**Tracking:** Updated via `setValidatedSampleMetadata()` method (lines 244-247)

#### 3. EditSampleBatchDialog - onUploadSucceeded Validation Handling
**File:** Same as above  
**Lines 206-241:**
When validation completes with failures:
```java
if (!failedValidations.isEmpty()) {
  ui.access(() -> component.setDisplay(invalidDisplay(uploadedData.fileName(),
      failedValidations.stream().map(ValidationResultWithPayload::validationResult)
          .toList())));
  setValidatedSampleMetadata(List.of());  // ← Clears list on failure
  return;
}
```

The list IS cleared (line 220), but the `onConfirmClicked()` method doesn't check if it's empty.

#### 4. RegisterSampleBatchDialog - onConfirmClicked Method (Reference)
**File:** `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/samples/registration/batch/RegisterSampleBatchDialog.java`

**Lines 399-424:**
```java
@Override
protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
  if (batchNameField.isInvalid()) {
    batchNameField.focus();
    return;
  }
  if (batchNameField.isEmpty() || batchNameField.getValue().isBlank()) {
    batchNameField.setInvalid(true);
    batchNameField.focus();
    return;
  }
  // ✓ BETTER: Has validation for validatedSampleMetadata
  if (validatedSampleMetadata.isEmpty() && uploadWithDisplay.getUploadedData().isEmpty()) {
    var uploadProgressDisplay = new InvalidUploadDisplay(
        "Nothing was uploaded. Please upload the sample metadata and try again.");
    uploadWithDisplay.setDisplay(uploadProgressDisplay);
    return;
  } else if (validatedSampleMetadata.isEmpty() && uploadWithDisplay.getUploadedData()
      .isPresent()) {
    // the uploaded data is not valid
    return;
  }
  fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(),
      batchNameField.getValue(), validatedSampleMetadata));
}
```

**Note:** RegisterSampleBatchDialog has better validation (lines 411-421) but EditSampleBatchDialog does NOT.

#### 5. EditSampleBatchDialog - Upload State Management
**File:** Same  
**Lines 120-134:**
```java
UploadWithDisplay uploadWithDisplay = new UploadWithDisplay(...);
uploadWithDisplay.addSuccessListener(
    uploadSucceeded -> onUploadSucceeded(sampleValidationService, experimentId, projectId,
        uploadSucceeded)
);
uploadWithDisplay.addRemovedListener(it -> setValidatedSampleMetadata(List.of()));
```

**Issue:** There is an `uploadWithDisplay` component, but it's a local variable and not stored as an instance field. This makes it impossible for `onConfirmClicked()` to check if a file is currently uploaded.

#### 6. SampleInformationMain - Confirm Event Handler
**File:** `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/samples/SampleInformationMain.java`

**Lines 434-438 (EditSampleBatchDialog handler):**
```java
editSampleBatchDialog.addConfirmListener(event -> {
  var sampleMetadata = new ArrayList<>(event.validatedSampleMetadata());
  event.getSource().close();
  // ... registration proceeds even if sampleMetadata is empty
```

**Lines 301-306 (RegisterSampleBatchDialog handler):**
```java
registerSampleBatchDialog.addConfirmListener(event -> {
  var sampleMetadata = new ArrayList<>(event.validatedSampleMetadata());
  event.getSource().close();
  // ... registration proceeds even if sampleMetadata is empty
```

---

## Reproduction Steps

### Detailed Step-by-Step

1. **Open the Data Manager application** and log in with a valid user account
2. **Navigate to a project** and select an experiment that has:
   - Existing experimental groups
   - Existing sample batch(es)
3. **Click on the Samples section** (or navigate to the samples view)
4. **Click the "Edit" button** on an existing batch
5. In the Edit Sample Batch dialog:
   - Click **"Download metadata template"**
   - Save the XLSX file locally
6. **Open the XLSX file** in a spreadsheet application (Excel, LibreOffice, etc.)
7. **Locate required columns:** Look for columns like:
   - Species
   - Specimen
   - Analyte
   - Or other required metadata fields
8. **Delete content from required cells:**
   - Select a cell in the "Species" column for one or more rows
   - Press DELETE to remove the value
   - Repeat for other required fields (Specimen, Analyte)
   - Save the file
9. **Return to the browser** and in the Edit Sample Batch dialog:
   - Click in the upload area to select the modified file
   - Upload the XLSX file with missing data
10. **Wait for validation** to complete (progress bar will show "Validating file...")
11. **Observe the validation result:**
    - The dialog should display "Invalid sample metadata"
    - Error messages should show "Missing species for X sample(s)"
    - Multiple error messages for each missing field
12. **Click the "Edit Batch" button** (the confirm button)
13. **Expected behavior:** Button should be disabled or unresponsive  
    **Actual behavior:** Dialog closes, toast shows "Sample batch updated successfully"

---

## Proposed Fix Approach

### Solution Overview

Add validation checks in `EditSampleBatchDialog.onConfirmClicked()` to prevent proceeding when:
1. No file has been uploaded, OR
2. File was uploaded but validation failed (validatedSampleMetadata is empty)

This will bring `EditSampleBatchDialog` in line with the logic already present in `RegisterSampleBatchDialog`.

### Detailed Fix Strategy

#### Step 1: Store Reference to UploadWithDisplay Component

**File:** `EditSampleBatchDialog.java`

**Current (Line 120):**
```java
UploadWithDisplay uploadWithDisplay = new UploadWithDisplay(MAX_FILE_SIZE, new FileType[]{...});
```

**Change to:**
```java
// Make it an instance field (add at line ~76 with other fields)
private final UploadWithDisplay uploadWithDisplay;

// Then initialize in constructor
uploadWithDisplay = new UploadWithDisplay(MAX_FILE_SIZE, new FileType[]{...});
```

**Rationale:** Allows `onConfirmClicked()` to check the upload state.

#### Step 2: Add Validation Check to onConfirmClicked()

**File:** `EditSampleBatchDialog.java`  
**Method:** `onConfirmClicked()` (lines 339-354)

**Current code:**
```java
@Override
protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
  if (batchNameField.isInvalid()) {
    batchNameField.focus();
    return;
  }
  if (batchNameField.isEmpty()) {
    batchNameField.setInvalid(true);
    batchNameField.focus();
    return;
  }
  fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), batchNameField.getValue(),
      Collections.unmodifiableList(validatedSampleMetadata)));
}
```

**Proposed code:**
```java
@Override
protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
  if (batchNameField.isInvalid()) {
    batchNameField.focus();
    return;
  }
  if (batchNameField.isEmpty()) {
    batchNameField.setInvalid(true);
    batchNameField.focus();
    return;
  }
  
  // NEW VALIDATION: Check if validated sample metadata is available
  if (validatedSampleMetadata.isEmpty() && uploadWithDisplay.getUploadedData().isEmpty()) {
    // No file uploaded
    var uploadProgressDisplay = new InvalidUploadDisplay(
        "No file uploaded. Please upload the sample metadata template and try again.");
    uploadWithDisplay.setDisplay(uploadProgressDisplay);
    return;
  } else if (validatedSampleMetadata.isEmpty() && uploadWithDisplay.getUploadedData().isPresent()) {
    // File was uploaded but validation failed
    return;
  }
  
  fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), batchNameField.getValue(),
      Collections.unmodifiableList(validatedSampleMetadata)));
}
```

**Rationale:**
- If `validatedSampleMetadata` is empty AND no file is uploaded → show error and return
- If `validatedSampleMetadata` is empty BUT a file WAS uploaded → validation must have failed; return silently (error is already displayed)
- Only proceed if `validatedSampleMetadata` is non-empty (validation succeeded)

#### Step 3: Consider Disabling Button Proactively (Optional Enhancement)

**Alternative/Additional approach:** Instead of just blocking in `onConfirmClicked()`, also disable the confirm button while validation is in progress or when validation fails.

This could be done by:
1. Adding a method to update button state based on validation state
2. Calling this method from `onUploadSucceeded()` after validation completes
3. Disabling button when validation fails, enabling when it succeeds

**Pseudocode:**
```java
private void updateConfirmButtonState(boolean validationSucceeded) {
  if (validationSucceeded && !validatedSampleMetadata.isEmpty()) {
    confirmButton.setEnabled(true);
  } else {
    confirmButton.setEnabled(false);
  }
}

// In onUploadSucceeded(), after validation:
if (!failedValidations.isEmpty()) {
  // ... existing error display code ...
  updateConfirmButtonState(false);  // ← NEW
} else if (!succeededValidations.isEmpty()) {
  // ... existing success display code ...
  updateConfirmButtonState(true);   // ← NEW
}
```

---

## Testing Checklist

### Unit Tests

- [ ] Test `EditSampleBatchDialog.onConfirmClicked()` with empty `validatedSampleMetadata`
- [ ] Test `EditSampleBatchDialog.onConfirmClicked()` with non-empty `validatedSampleMetadata`
- [ ] Test `EditSampleBatchDialog.onConfirmClicked()` with no file uploaded
- [ ] Test state consistency between `uploadWithDisplay` and `validatedSampleMetadata`
- [ ] Test that `setValidatedSampleMetadata(List.of())` properly clears the list on validation failure

### Integration Tests

- [ ] Upload invalid XLSX file → observe error display → click confirm → verify button is unresponsive
- [ ] Upload valid XLSX file → observe success display → click confirm → verify event is fired
- [ ] Upload valid file → click remove → verify no file uploaded state is correctly detected
- [ ] Edit batch with valid file → proceed normally
- [ ] Edit batch with invalid file → verify error persists until file is corrected

### Manual Testing

- [ ] Edit sample batch with valid file (species, specimen, analyte all present)
  - Expected: Can complete registration
  - Result: ___________

- [ ] Edit sample batch with invalid file (missing species)
  - Expected: Cannot proceed; button unresponsive or disabled
  - Result: ___________

- [ ] Edit sample batch, remove file after uploading valid file
  - Expected: Cannot proceed
  - Result: ___________

- [ ] Edit sample batch with invalid file, then upload corrected file
  - Expected: Can now proceed
  - Result: ___________

### Regression Testing

- [ ] Register new sample batch (RegisterSampleBatchDialog) still works as before
- [ ] Edit sample batch with partial edits works correctly
- [ ] Cancel operations work correctly
- [ ] Batch name validation still works
- [ ] File upload validation messages still display correctly
- [ ] Success/failure notifications display correctly after registration

### Cross-browser Testing

- [ ] Chrome
- [ ] Firefox
- [ ] Safari
- [ ] Microsoft Edge

---

## Related Requirements

### From docs/requirements.md

Based on the Data Manager architecture, the following requirements likely relate to this issue:

- **SAMPLE-R-01** through **SAMPLE-R-XX** (Sample Registration requirements)
- **SAMPLE-NFR-01** (Data Quality / Validation requirements)
- **SAMPLE-C-01** (Sample constraints, if any)

**Note:** Actual requirement IDs should be verified against `docs/requirements.md`.

---

## Assumptions and Constraints

### Assumptions Made During Analysis

1. **Assumption:** The `validatedSampleMetadata` list is properly cleared when validation fails
   - Evidence: Line 220 in `EditSampleBatchDialog`: `setValidatedSampleMetadata(List.of());`
   - Status: ✓ Confirmed

2. **Assumption:** The `InvalidUploadDisplay` component is sufficient for showing errors
   - Evidence: Multiple uses of this component in both dialogs
   - Status: ✓ Confirmed

3. **Assumption:** The issue only affects `EditSampleBatchDialog`, but `RegisterSampleBatchDialog` may have it too
   - Evidence: RegisterSampleBatchDialog has some checks but EditSampleBatchDialog does not
   - Status: ⚠ Needs verification - RegisterSampleBatchDialog should be checked as well

4. **Assumption:** Making `uploadWithDisplay` an instance field won't break anything
   - Evidence: It's only used for upload handling and display
   - Status: ✓ Safe to implement

5. **Assumption:** The fix should be consistent between EditSampleBatchDialog and RegisterSampleBatchDialog
   - Evidence: Both dialogs perform similar workflows
   - Status: ✓ Recommended

### Known Unknowns / Context Gaps

1. **Unknown:** Why is `uploadWithDisplay` a local variable instead of an instance field?
   - **Impact:** Makes the fix slightly more complex
   - **Resolution:** Should be converted to instance field

2. **Unknown:** Are there performance implications of the validation checks?
   - **Impact:** Minimal - just checking list emptiness
   - **Resolution:** None expected

3. **Unknown:** Should there be a visual indicator (e.g., disabled button) during/after validation?
   - **Impact:** Could improve UX
   - **Resolution:** Optional enhancement; blocking the action is sufficient for the bug fix

4. **Unknown:** Are there any special cases where empty `validatedSampleMetadata` is acceptable?
   - **Impact:** Might require nuanced logic
   - **Resolution:** Unlikely; always require at least one valid sample

### Constraints

1. **Constraint:** Must maintain backward compatibility with event listeners
   - Dialog should still fire ConfirmEvent only when validation succeeds
   - Event listeners in SampleInformationMain depend on receiving validated metadata

2. **Constraint:** Error messages must be user-friendly
   - Should not expose technical details
   - Should guide user to correct action (upload file, fix validation errors)

3. **Constraint:** Fix must apply to both create and edit scenarios
   - RegisterSampleBatchDialog and EditSampleBatchDialog should have consistent behavior

4. **Constraint:** No changes to API contracts
   - The ConfirmEvent signature should remain unchanged
   - The dialog should only fire the event when appropriate

---

## Recommendations for Developer

### Priority Actions

1. **Immediate Fix (Critical)**
   - Add validation check in `EditSampleBatchDialog.onConfirmClicked()` to prevent event firing when `validatedSampleMetadata` is empty
   - Make `uploadWithDisplay` an instance field to enable state checking
   - Test with the invalid file provided in the issue

2. **Consistency Check (Important)**
   - Review `RegisterSampleBatchDialog.onConfirmClicked()` for similar issues
   - Ensure both dialogs have identical validation logic
   - Consider extracting common validation logic to a shared method

3. **Enhancement (Nice-to-have)**
   - Disable confirm button proactively when validation fails
   - Add visual feedback during validation process
   - Consider showing upload state indicators

### Code Review Focus Areas

1. State consistency between UI display and internal state
2. All paths through `onConfirmClicked()` return appropriately
3. Error messages are clear and actionable
4. No edge cases allow empty metadata to be processed

### Related Files to Review

While fixing this issue, also review:
- `SampleInformationMain.java` - ensure it doesn't assume valid metadata
- `SampleValidationService.java` - understand validation flow
- `SampleRegistrationServiceV2.java` - verify it handles empty input gracefully

---

## Issue Links and References

- **Original Issue:** https://github.com/qbicsoftware/data-manager-app/issues/1401
- **Provided Files:** 
  - Valid example: `2026-03-20_Q2RGU6_sample-metadata-update-template.xlsx`
  - Invalid example: `2026-03-20_Q2RGU6_sample-metadata-update-template_invalid.xlsx`

---

## Summary

This bug allows users to bypass sample metadata validation by clicking the confirm button while validation errors are displayed. The root cause is missing validation checks in `EditSampleBatchDialog.onConfirmClicked()` that should prevent the dialog from firing a confirm event when the sample metadata is invalid. The fix involves adding a check to ensure `validatedSampleMetadata` is non-empty before proceeding, similar to logic already present in `RegisterSampleBatchDialog`. The impact is high as it compromises data quality by allowing invalid metadata to be registered.

