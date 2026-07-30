# Frontend components 
Some visual aid of our custom view components structure.

## App dialog

```mermaid
---
title: Dialog window
---

classDiagram
    note for Component "Vaadin Component"
    note for Dialog "Vaadin Component"
    AppDialog <-- DialogHeader
    AppDialog <-- DialogBody
    AppDialog <-- DialogFooter
    DialogBody ..|> UserInput
    UserInput --> InputValidation
    AppDialog --> Component
    AppDialog --> Action
    AppDialog --|> Dialog
    AppDialog --> UserInput
    DialogBody *-- DialogSection
    
    class Dialog {
        
    }
    
    class DialogSection {
        
    }

    class Component {
    }

    class AppDialog {
        +setHeader(Component component)
        +setBody(Component component)
        +setFooter(Component component)
        +registerConfirmAction(Action action)
        +registerCancelAction(Action action)
        +registerUserInput(UserInput input)
        
        +confirm()
        +cancel()
    }

    class DialogHeader {
    }

    class DialogBody {
    }

    class Action {
        <<interface>>
        +execute()
    }

    class DialogFooter {
    }

    class InputValidation {
        + passed() boolean
        + failed() boolean
    }

    class UserInput {
        <<interface>>
        + validate() InputValidation
        + hasChanges() boolean
    }

```

## Stepper dialog

```mermaid

classDiagram
    
    StepperDialogFooter ..|> NavigationListener
    StepperDialogFooter --> StepperDialog
    StepperDialog --> NavigationListener
    DialogStep ..|> Step
    StepperDialog --> AppDialog
    StepperDialog --> Step
    StepDisplay ..|> NavigationListener
    StepDisplay --> StepperDialog
    
    
    class Step {
        <<interface>>
        + name() String
        + content() Component
        + userInput() UserInput
    }
    
    class AppDialog {
        
    }
    
    class DialogStep {
        
    }
    
    class StepperDialog {
        AppDialog dialog
        Step[] steps
        + registerCancelAction(Action action)
        + registerConfirmAction(Action action)
        + registerNavigationListener(NavigationListener listener)
        + setFooter(Component component)
        + setHeader(Component component)
        + setStepDisplay(Component component)
        + cancel()
        + confirm()
        + next()
        + previous()
        
    }
    
    class NavigationListener {
        <<interface>>
        + onNavigationUpdate(NavigationInfo info)
    }
    
    class StepDisplay {
        StepperDialog dialog
    }
    
    class StepperDialogFooter {
        DialogFooter currentState
        StepperDialog dialog
        
    }

```

## Alert dialog

```mermaid
classDiagram
    note for Component "Vaadin Component"
    AlertDialog <-- AppDialog
    AppDialog --> DialogHeader
    AppDialog --> DialogBody
    AppDialog --> DialogFooter
    AppDialog --|> Dialog
    
    class AlertDialog {
        <<builder>>
        +alert(Component parent) Builder
        +danger(Component parent, String title, String message, DialogAction onConfirm) AlertDialog
        +open()
        +close()
        +dialog() AppDialog
    }
    
    class Builder {
        +intent(Intent intent) Builder
        +danger() Builder
        +warning() Builder
        +error() Builder
        +info() Builder
        +title(String title) Builder
        +message(String message) Builder
        +confirmButton(String label, DialogAction action) Builder
        +cancelButton(String label, DialogAction action) Builder
        +build() AlertDialog
    }
    
    class AppDialog {
        +small() AppDialog
        +registerConfirmAction(DialogAction action)
        +registerCancelAction(DialogAction action)
        +open()
        +close()
    }
    
    class DialogHeader {
        +withIcon(AppDialog dialog, String title, Icon icon)
    }
    
    class DialogBody {
        +withoutUserInput(AppDialog dialog, Component component)
    }
    
    class DialogFooter {
        +withDangerousConfirm(AppDialog dialog, String cancelText, String confirmText)
        +with(AppDialog dialog, String cancelText, String confirmText)
        +withConfirmOnly(AppDialog dialog, String confirmText)
    }
    
    class DialogAction {
        <<interface>>
        +execute()
    }
```

### When to use

| Need | Pattern |
|---|---|
| Destructive action confirmation | `AlertDialog.danger(parent, title, message, onConfirm).open()` |
| Error requiring acknowledgment | `AlertDialog.alert(parent).error().title(...).message(...).confirmButton("OK", () -> {}).build().open()` |
| Error with redirect + cancel | `AlertDialog.alert(parent).error().confirmButton("Go...", nav).cancelButton("Cancel", close).build().open()` |
| Cancel / discard changes | `CancelConfirmationDialogFactory.cancelConfirmationDialog(action, key, locale).open()` |
| Success / info / transient feedback | `MessageSourceNotificationFactory.toast(...)` (unchanged) |
| Pending task with progress bar | `MessageSourceNotificationFactory.pendingTaskToast(...)` (unchanged) |

### Code examples

**Destructive confirmation (most common):**
```java
AlertDialog.danger(this,
    "Samples within batch will be deleted",
    "Deleting this Batch will also delete the samples contained within. Proceed?",
    () -> deleteBatch(batchId)).open();
```

**Error dialog with single button:**
```java
AlertDialog.alert(this)
    .error()
    .title("Cannot edit variables")
    .message("Editing experimental variables is only possible if samples are not registered.")
    .confirmButton("Okay", () -> {})
    .build()
    .open();
```

**Warning with cancel:**
```java
AlertDialog.alert(this)
    .warning()
    .title("Discard changes?")
    .message("By aborting, you will lose all entered information.")
    .confirmButton("Discard", () -> discard())
    .cancelButton("Continue Editing", () -> {})  // close dialog
    .build()
    .open();
```

### Deprecated: NotificationDialog

`NotificationDialog` has been removed and replaced with `AlertDialog`. The following classes were deleted:

- `AccessTokenDeletionConfirmationNotification`
- `BatchDeletionConfirmationNotification`
- `MeasurementDeletionConfirmationNotification`
- `QCItemDeletionConfirmationNotification`
- `PurchaseItemDeletionConfirmationNotification`
- `ProjectUserRemovalConfirmationNotification`
- `ExistingGroupsPreventVariableEdit`
- `ExistingSamplesPreventVariableEdit`
- `ExistingSamplesPreventSampleOriginEdit`
- `ExistingSamplesPreventGroupEdit`

If any code still references `NotificationDialog`, it will throw `UnsupportedOperationException` at runtime. Use the patterns above instead.



