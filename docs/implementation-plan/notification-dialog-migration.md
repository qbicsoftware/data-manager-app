# Implementation Plan: Replace NotificationDialog with AlertDialog and Toast

**GitHub Issue:** [#1487](https://github.com/qbicsoftware/data-manager-app/issues/1487)  
**Branch:** `refactor/replace-notification-dialog-with-alertdialog`  
**Date:** 2025-06-28

---

## Executive Summary

The frontend uses two overlapping notification patterns:

| Pattern | Class | Status |
|---|---|---|
| **Legacy** | `NotificationDialog` (extends `ConfirmDialog`) | ❌ To be removed |
| **Modern** | `AlertDialog` (built on `AppDialog.small()`) | ✅ Reference implementation in `ConnectedDatasetsMain` |

**Goal:** Delete all 10 `NotificationDialog` subclasses, migrate all call-sites to `AlertDialog`, deprecate `NotificationDialog.java`, and update `front-end-components.md`.

### Why delete the subclasses?

Every subclass is a dead-simple factory with zero reusable logic:

```java
// Anti-pattern — pure indirection:
var dialog = new BatchDeletionConfirmationNotification();
dialog.addConfirmListener(event -> { doDelete(); dialog.close(); });
dialog.addCancelListener(event -> dialog.close());
dialog.open();
```

`AlertDialog` already handles dialog lifecycle internally, so callers can write:

```java
// One-liner, no indirection:
AlertDialog.danger(this, "Samples will be deleted", "Proceed?", () -> doDelete()).open();
```

---

## Decision Tree

```
Need a notification?
├── Destructive action confirmation?
│   └── → AlertDialog.danger(parent, title, message, onConfirm).open()
├── Error requiring acknowledgment?
│   └── → AlertDialog.alert(parent).error().title().message()
│                .confirmButton("OK", () -> {}).build().open()
├── Error with redirect + cancel?
│   └── → AlertDialog.alert(parent).error().confirmButton("Go...", nav)
│                .cancelButton("Cancel", close).build().open()
├── Cancel / discard changes?
│   └── → CancelConfirmationDialogFactory (returns AlertDialog after migration)
├── Success / info / transient feedback?
│   └── → MessageSourceNotificationFactory.toast() (unchanged)
└── Pending task with progress bar?
    └── → MessageSourceNotificationFactory.pendingTaskToast() (unchanged)
```

---

## Migration Phases

### Phase 1: Inline All Call-Sites (Delete 10 Subclasses)

Each `NotificationDialog` subclass is replaced by inlining a single `AlertDialog` call at every call-site.

#### 1A. Deletion Confirmation Notifications (6 files)

These all follow the pattern: `WARNING` intent, cancelable, red confirm button.

| # | Delete | Callers (edit) | Inlined replacement |
|---|---|---|---|
| 1 | `AccessTokenDeletionConfirmationNotification.java` | `PersonalAccessTokenMain.java` | `AlertDialog.danger(this, "Personal Access Token will be deleted", "Deleting this Personal Access Token will make it unusable. Proceed?", () -> deleteToken()).open()` |
| 2 | `BatchDeletionConfirmationNotification.java` | `SampleInformationMain.java` | `AlertDialog.danger(this, "Samples within batch will be deleted", "Deleting this Batch will also delete the samples contained within. Proceed?", () -> doDelete()).open()` |
| 3 | `MeasurementDeletionConfirmationNotification.java` | `MeasurementMain.java` (3 sites) | `AlertDialog.danger(this, title, "Are you sure you want to delete %d measurements?".formatted(count), () -> delete()).open()` |
| 4 | `QCItemDeletionConfirmationNotification.java` | `ProjectInformationMain.java` | `AlertDialog.danger(this, "Quality control will be deleted", "Are you sure you want to delete this file?", () -> delete()).open()` |
| 5 | `PurchaseItemDeletionConfirmationNotification.java` | `ProjectInformationMain.java` | `AlertDialog.danger(this, "Offer will be deleted", "Are you sure you want to delete this offer?", () -> delete()).open()` |
| 6 | `ProjectUserRemovalConfirmationNotification.java` | `ProjectAccessComponent.java` | `AlertDialog.danger(this, "Remove user from project", "Are you sure you want to remove the user %s from the project?".formatted(user), () -> remove()).open()` |

#### 1B. Error Notifications (4 files)

These are `ERROR` intent — not destructive, but require user acknowledgment.

| # | Delete | Callers (edit) | Inlined replacement |
|---|---|---|---|
| 7 | `ExistingGroupsPreventVariableEdit.java` | 1 caller | `AlertDialog.alert(this).error().title("Cannot edit variables").message("Editing experimental variables requires all experimental groups to be deleted...").confirmButton("Go to Experimental Groups", () -> navigate()).cancelButton("Cancel", () -> {}).build().open()` |
| 8 | `ExistingSamplesPreventVariableEdit.java` | 1 caller | `AlertDialog.alert(this).error().title("Cannot edit variables").message("Editing experimental variables is only possible if samples are not registered...").confirmButton("Okay", () -> {}).build().open()` |
| 9 | `ExistingSamplesPreventSampleOriginEdit.java` | `EditExperimentDialog.java` | `AlertDialog.alert(this).error().title("Cannot remove sample origin").message("'<label>' cannot be deleted, as it is referenced in samples...").confirmButton("Okay", () -> {}).build().open()` |
| 10 | `ExistingSamplesPreventGroupEdit.java` | `ExperimentDetailsComponent.java` | `AlertDialog.alert(this).error().title("Cannot edit experimental groups").message("Editing experimental groups is only possible if samples are not registered...").confirmButton("Okay", () -> {}).build().open()` |

---

### Phase 2: Migrate Factory Classes

#### 2A. `CancelConfirmationDialogFactory`

**File:** `datamanager-app/src/main/java/life/qbic/datamanager/views/notifications/CancelConfirmationDialogFactory.java`

| Change | Detail |
|---|---|
| Return type | `NotificationDialog` → `AlertDialog` (both overloads) |
| Build | Replace `NotificationDialog.warningDialog()` + `setCancelable`/`setCancelText`/`setConfirmButton` with `AlertDialog.alert(parent).warning().cancelButton(...).confirmButton(...).build()` |
| Listeners | Replace `ConfirmEvent` listeners with `DialogAction` lambdas |
| Import | Remove `ConfirmEvent`; add `AlertDialog`, `AppDialog` references |

**Affected call-sites (auto-migrated):**
- `AddProjectDialog.java:150`
- `ProjectSideNavigationComponent.java:343`

#### 2B. `MessageSourceNotificationFactory.dialog()`

**File:** `datamanager-app/src/main/java/life/qbic/datamanager/views/notifications/MessageSourceNotificationFactory.java`

| Change | Detail |
|---|---|
| Single call-site | `SampleInformationMain.java:405` calls `dialog("sample-batch.update.failure", ...)` |
| Action | Inline the dialog construction in `SampleInformationMain.displayUpdateFailure()` using `AlertDialog.alert(...).error().title("Didn't update sample batch.").message("We are sorry! The sample batch update failed. Please try again.").confirmButton("Okay", () -> {}).build().open()` |
| Deprecate | Mark `MessageSourceNotificationFactory.dialog()` as `@Deprecated(since="1.12.0", forRemoval=true)` |

**Note:** `toast()`, `routingToast()`, and `pendingTaskToast()` are used ~25 times and are **unchanged** — they already produce non-blocking toast notifications, which is the correct pattern for success/info.

---

### Phase 3: Migrate UiExceptionHandler

**File:** `datamanager-app/src/main/java/life/qbic/datamanager/exceptionhandling/UiExceptionHandler.java`

| Change | Detail |
|---|---|
| `showErrorDialog()` | Replace `NotificationDialog.errorDialog().withTitle(...).withContent(...)` with `AlertDialog.alert(ui).error().title(title).message(message).confirmButton("Okay", () -> {}).build().open()` |
| Import | Remove `NotificationDialog`; add `AlertDialog` |

---

### Phase 4: Delete / Deprecate Legacy Classes

| File | Action |
|---|---|
| `NotificationDialog.java` | **Delete** (or `@Deprecated` if any external consumers exist) |
| `NotificationLevel.java` | **Delete** if no remaining usages after all migrations |
| `StyledNotification.java` | Check for references to `NotificationDialog`; delete if affected |
| `Toast.java` | **No changes** — already uses the modern pattern |

**Supporting classes that stay untouched:**
- `AppDialog.java` — infrastructure
- `DialogHeader.java` — infrastructure
- `DialogBody.java` — infrastructure
- `DialogFooter.java` — infrastructure
- `DialogAction.java` — interface
- `IconFactory.java` — provides `warningIcon()` used by `AlertDialog`

---

### Phase 5: Update Documentation

**File:** `datamanager-app/front-end-components.md`

Add a new **AlertDialog** section with:
1. Mermaid class diagram showing `AlertDialog → AppDialog → Dialog`
2. The decision tree (see section above)
3. Code examples for each variant:
   - `AlertDialog.danger()` — destructive confirmations
   - `AlertDialog.alert().error()` — error dialogs
   - Toast via `MessageSourceNotificationFactory.toast()` — non-blocking feedback
4. Note about `NotificationDialog` deprecation

---

### Phase 6: Tests

| Action | Details |
|---|---|
| Update | `AlertDialogTest.java` — already exists, covers factory contract |
| Create | 10 new unit tests — one per deleted subclass, verifying `AlertDialog` construction with correct parameters |
| Create | `CancelConfirmationDialogFactoryTest` — verify `AlertDialog` return type and action wiring |
| Create | `UiExceptionHandlerTest` — verify `AlertDialog` displayed for errors |
| Check | `SampleInformationMain` tests — update if any test verifies `notificationFactory.dialog()` behavior |

---

## Files Changed Summary

### Deleted (10 + 1)

| # | File | Reason |
|---|---|---|
| 1 | `views/account/AccessTokenDeletionConfirmationNotification.java` | Inlined into caller |
| 2 | `views/projects/project/samples/BatchDeletionConfirmationNotification.java` | Inlined into caller |
| 3 | `views/projects/project/measurements/MeasurementDeletionConfirmationNotification.java` | Inlined into caller |
| 4 | `views/projects/qualityControl/QCItemDeletionConfirmationNotification.java` | Inlined into caller |
| 5 | `views/projects/purchase/PurchaseItemDeletionConfirmationNotification.java` | Inlined into caller |
| 6 | `views/projects/project/access/ProjectUserRemovalConfirmationNotification.java` | Inlined into caller |
| 7 | `views/projects/project/experiments/experiment/components/ExistingGroupsPreventVariableEdit.java` | Inlined into caller |
| 8 | `views/projects/project/experiments/experiment/components/ExistingSamplesPreventVariableEdit.java` | Inlined into caller |
| 9 | `views/projects/project/experiments/experiment/components/ExistingSamplesPreventSampleOriginEdit.java` | Inlined into caller |
| 10 | `views/projects/project/experiments/experiment/ExistingSamplesPreventGroupEdit.java` | Inlined into caller |
| 11 | `views/notifications/NotificationDialog.java` | Deprecated or deleted |

### Edited (10)

| # | File | What changes |
|---|---|---|
| 1 | `views/account/PersonalAccessTokenMain.java` | Replace `new AccessTokenDeletionConfirmationNotification()` → `AlertDialog.danger()` |
| 2 | `views/projects/project/samples/SampleInformationMain.java` | Replace 2 call-sites (batch deletion + dialog factory) → `AlertDialog.danger()` + inline error dialog |
| 3 | `views/projects/project/measurements/MeasurementMain.java` | Replace 3 call-sites → `AlertDialog.danger()` |
| 4 | `views/projects/project/info/ProjectInformationMain.java` | Replace 2 call-sites (offer + QC deletion) → `AlertDialog.danger()` |
| 5 | `views/projects/project/access/ProjectAccessComponent.java` | Replace 1 call-site → `AlertDialog.danger()` |
| 6 | `views/projects/project/experiments/experiment/components/ExistingGroupsPreventVariableEdit.java` | **DELETED** — inlined into caller |
| 7 | `views/projects/project/experiments/experiment/components/ExistingSamplesPreventVariableEdit.java` | **DELETED** — inlined into caller |
| 8 | `views/projects/project/experiments/experiment/components/ExistingSamplesPreventSampleOriginEdit.java` | **DELETED** — inlined into caller |
| 9 | `views/projects/project/experiments/experiment/ExistingSamplesPreventGroupEdit.java` | **DELETED** — inlined into caller |
| 10 | `views/projects/project/experiments/experiment/update/EditExperimentDialog.java` | Replace 1 call-site → `AlertDialog.alert().error()` |

### Edited (4 factories / infrastructure)

| # | File | What changes |
|---|---|---|
| 11 | `views/notifications/CancelConfirmationDialogFactory.java` | Return `AlertDialog`; replace `ConfirmEvent` with `DialogAction` |
| 12 | `views/notifications/MessageSourceNotificationFactory.java` | Deprecate `dialog()` method |
| 13 | `exceptionhandling/UiExceptionHandler.java` | Use `AlertDialog` instead of `NotificationDialog.errorDialog()` |
| 14 | `views/navigation/ProjectSideNavigationComponent.java` | Auto-migrated by `CancelConfirmationDialogFactory` change |
| 15 | `views/projects/create/AddProjectDialog.java` | Auto-migrated by `CancelConfirmationDialogFactory` change |

### Edited (1 documentation)

| # | File | What changes |
|---|---|---|
| 16 | `front-end-components.md` | Add AlertDialog section with decision tree |

---

## Risk Assessment

| Risk | Impact | Mitigation |
|---|---|---|
| CSS class mismatches | Styling regression | `AlertDialog` uses `.button-danger` via `DialogFooter.withDangerousConfirm()` — verify in Dev mode |
| `BeforeLeaveObserver` behavior | Unsaved-changes protection may differ | `AppDialog.small()` already implements `BeforeLeaveObserver` — actually **better** than `ConfirmDialog` |
| Vaadin API mismatch | Compile errors from `addConfirmListener(ConfirmEvent)` → `DialogAction` | Straightforward substitution; no complex event handling in any call-site |
| HTML content in existing dialogs | Some dialogs use HTML formatting | Replace with `Div` + `Span` compositions using `DialogBody.withoutUserInput()` |
| `MessageSourceNotificationFactory.dialog()` remaining usage | 1 call-site only | Inline directly in `SampleInformationMain.java` |

---

## Verification Checklist

- [ ] `grep -rn "NotificationDialog" datamanager-app/src/main/java --include="*.java"` returns only the deprecated file itself
- [ ] All deletion confirmations use `AlertDialog.danger()`
- [ ] All error dialogs use `AlertDialog.alert().error()`
- [ ] `CancelConfirmationDialogFactory` returns `AlertDialog`
- [ ] `UiExceptionHandler` uses `AlertDialog`
- [ ] `front-end-components.md` documents the AlertDialog pattern
- [ ] `mvn clean verify` passes
- [ ] Manual test in Dev mode (`./mvnw spring-boot:run -Pdevelopment`) — verify all notification dialogs render correctly