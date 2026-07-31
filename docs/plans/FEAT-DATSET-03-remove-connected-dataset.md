# Implementation Plan: FEAT-DATSET-03 — Remove a Connected Open, Published Dataset

> **Story:** [#1469](https://github.com/qbicsoftware/data-manager-app/issues/1469)
> **Parent Feature:** [#1466](https://github.com/qbicsoftware/data-manager-app/issues/1466) (FEAT-DATASET-CONNECTION)
> **Requirement:** DATA-R-01 (proposed for this feature — see note below)
> **ADR references:** [ADR-0001](../docs/adr/0001-associated-datasets-domain-model.md), [ADR-0003](../docs/adr/0003-connection-lifecycle-stewardship.md)

---

## Requirement ID Note

The parent feature (#1466) proposes new requirement IDs (`DATA-R-01`, `DATA-R-02`, `DATA-R-03`) that are
**pending formal addition to `docs/requirements.md`**. The existing `DATA-R-01` in that file already
covers *Immunopeptidomics Raw Dataset View and Filtering* — an unrelated domain. Before implementation
proceeds, the PO must confirm the correct IDs for the dataset-connection feature and update
`docs/requirements.md`. This is a **prerequisite**, not a blocker for the technical work below, since
the connect feature (#1467) already uses the same proposed IDs consistently.

---

## Acceptance Criteria (from the story)

| # | Scenario |
|---|---|
| AC-1 | Given a user has write access to a project with a connected dataset, When they click the **Remove** button, Then the connection between the dataset and the project is removed. |
| AC-2 | Given the removal is successful, When the operation completes, Then the user receives a confirmation notification and the view refreshes to reflect the updated list of connected datasets. |
| AC-3 | Given the removal fails, When the operation cannot complete, Then the user receives an error notification with further instructions. |

---

## Current State of the Codebase

The following infrastructure **already exists** and can be reused directly:

| Component | Location | Status |
|---|---|---|
| `AssociatedDataset` aggregate | `project-management/…/domain/model/associated_dataset/` | `remove()` exists but does **not** emit a domain event |
| `AssociatedDatasetRemovedEvent` | `project-management/…/domain/model/associated_dataset/event/` | ✅ Complete |
| `RemoveDatasetError` enum | `project-management/…/application/associated_dataset/` | ✅ Complete (`DATASET_NOT_FOUND`, `DATASET_ALREADY_REMOVED`, `REMOVAL_FAILED`) |
| `AssociatedDatasetRepository` | `project-management/…/repository/` | ✅ `findById()` + `save()` available |
| `AssociatedDatasetJpaRepository` | `project-management-infrastructure/…/` | ✅ `save()` (JPA merge) handles state transitions |
| Test specs for `removeDataset()` | `AssociatedDatasetServiceRemoveSpec.groovy` | ✅ Written, awaiting implementation |
| Test spec for removed event | `AssociatedDatasetRemovedEventSpec.groovy` | ✅ Written |
| `ConnectedResourcesComponent` | `datamanager-app/…/views/projects/project/datasets/` | Card list renders, but has **no Remove button** per design |
| `ConnectedDatasetsMain` | `datamanager-app/…/views/projects/project/datasets/` | View exists, sidebar wired |
| `AssociatedDatasetConnectedPolicy` | `project-management/…/application/policy/` | Pattern to replicate for removal |
| `InformProjectCollaboratorsAboutDatasetConnection` | `project-management/…/policy/directive/` | Pattern to replicate for removal |
| `AppConfig` bean wiring | `datamanager-app/…/AppConfig.java` | Policy bean registration |
| Toast notification properties | `datamanager-app/src/main/resources/messages/toast-notifications.properties` | `dataset.connected.*` entries exist, `dataset.removed.*` missing |
| `Messages` class | `project-management/…/application/Messages.java` | `datasetConnectedToProject()` exists, removal template missing |

---

## Task Breakdown

### Task 0 — UI Foundation: Build centralized `AlertDialog` factory

**Layer:** UI (shared, consumed by all views)
**Module:** `datamanager-app`
**Rationale:** The app currently has no proper alert dialog. The UX/UI designer has specified an Alert Dialog pattern (see screenshot in GitHub issue #1469) with these characteristics:
- Minimal content (icon + title + short description)
- Intent-driven styling — icon color and confirm button color reflect severity (danger, warning, error, info)
- 1–2 actions maximum (cancel + confirm, or confirm-only)
- No complex layouts or scrolling
- Use cases: confirm destructive actions, show warnings or errors

The existing `AppDialog` has the right building blocks (`AppDialog.small()`, `DialogHeader.withIcon()`, `DialogBody.withoutUserInput()`, `DialogFooter.withDangerousConfirm()`) but no centralized factory. The private `AppDialog.createConfirmDialog()` is hardcoded for "Discard changes?" and not reusable. Various views construct confirmation dialogs ad-hoc (e.g. `ExperimentDetailsComponent.createConfoundingVarsDeleteConfirmDialog()`).

**Files to create:**
- `datamanager-app/src/main/java/life/qbic/datamanager/views/general/dialog/AlertDialog.java`

**Design:** A static factory with a fluent builder API that uses `AppDialog.small()` internally. Provides two entry points:

```java
// Entry point A: one-shot static factory methods for common patterns
AlertDialog.alert(parentComponent)
    .danger()
    .title("Remove dataset?")
    .message("This will disconnect the dataset from the project.")
    .confirmButton("Remove", onRemove)
    .cancelButton("Cancel", onCancel)
    .open();

// Entry point B: pre-configured danger-intent shortcut
AlertDialog.danger(parentComponent,
    "Remove dataset?",
    "This will disconnect the dataset from the project.",
    onRemove)
    .open();
```

**Builder API:**
```java
public class AlertDialog {

    public enum Intent { DANGER, WARNING, ERROR, INFO }

    public static Builder alert(Component parent) { ... }
    public static AlertDialog danger(Component parent, String title,
        String message, DialogAction onConfirm) { ... }

    public static class Builder {
        public Builder intent(Intent intent)
        public Builder danger()   // shorthand for intent(DANGER)
        public Builder warning()  // shorthand for intent(WARNING)
        public Builder error()    // shorthand for intent(ERROR)
        public Builder info()     // shorthand for intent(INFO)
        public Builder title(String title)
        public Builder message(String message)
        public Builder confirmButton(String label, DialogAction action)
        public Builder cancelButton(String label, DialogAction action)  // optional
        public AlertDialog build()
    }

    public void open()
    public void close()
}
```

**How it works internally:**
1. Creates `AppDialog.small()`
2. Depending on intent, configures the icon via `IconFactory` (warning/error/success/info)
3. Sets the header via `DialogHeader.withIcon()`
4. Sets the body via `DialogBody.withoutUserInput()` with the description text
5. Sets the footer via `DialogFooter.withDangerousConfirm()` for DANGER intent, or `DialogFooter.with()` for other intents
6. Registers confirm/cancel actions via `AppDialog.registerConfirmAction()` / `registerCancelAction()`

**Intent → icon/button mapping:**

| Intent | Icon | Confirm button style | Use case |
|---|---|---|---|
| `DANGER` | `warningIcon()` (orange exclamation) | Danger (red, `.button-danger`) | Destructive actions: remove, delete, discard |
| `WARNING` | `warningIcon()` | Primary | Important non-destructive confirmation |
| `ERROR` | `VaadinIcon.CLOSE_CIRCLE` (red) | Primary | Inform: something went wrong |
| `INFO` | `VaadinIcon.INFO_CIRCLE` (blue) | Primary | Inform: informational message |

**CSS classes already available (no new CSS needed):**
- `.button-danger` — red fill (existing, used by `ButtonFactory.createDangerButton()`)
- `.icon-color-warning` — orange icon color (existing, used by `IconFactory.warningIcon()`)
- `DialogFooter.withDangerousConfirm()` already creates the correct button pair

**When to use `AlertDialog` vs `MessageSourceNotificationFactory.dialog()`:**
- **`AlertDialog`** for *action confirmation* (destructive ops, irreversible actions) — uses `AppDialog` which has the `BeforeLeaveObserver` pattern for unsaved changes.
- **`MessageSourceNotificationFactory.dialog()`** (uses Vaadin `ConfirmDialog`) for *informational/notification* dialogs (warnings, errors, info notices that don't require destructive action).
- Keep the split: confirmation = `AlertDialog`, notification = existing `NotificationDialog`.

**Tests to add:**
- Unit: `AlertDialog.danger()` creates a dialog with warning icon, danger button, registers correct actions
- Unit: builder `.danger().title(...).build().open()` produces correctly configured inner `AppDialog`
- Edge case: `.confirmButton()` without `.cancelButton()` → confirm-only dialog

---

### Task 1 — Domain: Wire event emission into `AssociatedDataset.remove()`

**Layer:** Domain
**Module:** `project-management`
**Files to change:**
- `project-management/src/main/java/life/qbic/projectmanagement/domain/model/associated_dataset/AssociatedDataset.java`

**What to do:**
The current `remove()` method sets `connectionState = REMOVED` and throws if already removed, but does **not** dispatch a domain event. Add a private `emitRemovedEvent()` method (mirroring the existing `emitConnectedEvent()`) and call it from `remove()`.

```java
public void remove() {
  if (this.connectionState == ConnectionState.REMOVED) {
    throw new IllegalStateException("Dataset connection is already removed");
  }
  this.connectionState = ConnectionState.REMOVED;
  emitRemovedEvent();  // ← NEW
}

private void emitRemovedEvent() {
  var event = AssociatedDatasetRemovedEvent.create(
      this.id, this.projectId, this.connectedBy, this.title, this.pid);
  LocalDomainEventDispatcher.instance().dispatch(event);
}
```

**Note on actor identity:** The aggregate uses `connectedBy` as the actor field in the removed event. However, the removal actor may differ from the connection actor (e.g., a different project member removes the connection). The service layer will need to pass the actual removing user's ID. Two options:

- **Option A (simpler):** Pass `removedByUserId` to `remove(String removedByUserId)` and use it in the event. The aggregate's `connectedBy` stays as the original connector, while the event captures who performed the removal.
- **Option B:** Keep the aggregate method signature-free and have the service replace the event's `actorUserId` after emission (fragile, not recommended).

**→ Choose Option A.** Change the signature to `remove(String removedByUserId)` so the event accurately reflects who performed the removal.

**Tests to add / verify:**
- Domain-level test: `AssociatedDataset.remove(userId)` transitions state to REMOVED and emits `AssociatedDatasetRemovedEvent` via `LocalDomainEventDispatcher`
- Edge case: calling `remove()` on an already-removed aggregate throws `IllegalStateException` without emitting an event

---

### Task 2 — Application: Add `removeDataset()` to `AssociatedDatasetService`

**Layer:** Application
**Module:** `project-management`
**Files to change:**
- `project-management/src/main/java/life/qbic/projectmanagement/application/associated_dataset/AssociatedDatasetService.java`

**What to do:**
Add a `removeDataset(String associatedDatasetIdStr, String removedByUserId)` method that:

1. Validates arguments (null checks → `NullPointerException`).
2. Parses `associatedDatasetIdStr` into `AssociatedDatasetId`.
3. Finds the dataset via `associatedDatasetRepository.findById(id)`.
   - If not found → `Result.fromError(RemoveDatasetError.DATASET_NOT_FOUND)`.
4. Sets up a `LocalDomainEventDispatcher` subscriber to collect `AssociatedDatasetRemovedEvent` (collect-during pattern, same as connect).
5. Calls `dataset.remove(removedByUserId)`.
   - If `IllegalStateException` (already removed) → `Result.fromError(RemoveDatasetError.DATASET_ALREADY_REMOVED)`.
6. Persists via `associatedDatasetRepository.save(dataset)`.
   - If persistence throws → `Result.fromError(RemoveDatasetError.REMOVAL_FAILED)`.
7. Forwards cached events to the global `DomainEventDispatcher`.
8. Returns `Result.fromValue(dataset.id())`.

**Method signature:**
```java
@PreAuthorize("hasPermission(#associatedDatasetId, " +
    "'life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDataset', 'WRITE')")
public Result<AssociatedDatasetId, RemoveDatasetError> removeDataset(
    String associatedDatasetId, String removedByUserId)
```

> **Note on ACL:** The `@PreAuthorize` expression above uses the aggregate ID as the resource,
> but the aggregate does not have its own ACL entry — per ADR-0003 §5, connections inherit
> project ACL. The actual permission check needs to resolve the `projectId` from the dataset
> and check `WRITE` on the project. Two options:
>
> - **Option A:** Resolve the dataset first (outside `@PreAuthorize`), then do a manual `@PreAuthorize` on the `projectId` or check via `UserPermissions.editProject()`.
> - **Option B:** Use a custom `PermissionEvaluator` that resolves the dataset → projectId → ACL check.
>
> The connect operation sidesteps this by receiving `projectId` as an explicit parameter in
> `@PreAuthorize`. For remove, the simplest approach is to drop `@PreAuthorize` from the method
> and perform the project-level permission check programmatically inside the method body using
> the `ProjectAccessService` or `SecurityContextHolder`, or by looking up the dataset's
> `projectId()` first and delegating to Spring Security.
>
> **→ Recommend Option A:** Load the aggregate, extract `projectId`, then check WRITE permission
> manually (e.g., via `UserPermissions.editProject(projectId)` or equivalent). If unauthorized,
> return `DATASET_NOT_FOUND` (to avoid leaking existence). This keeps the API simple and avoids
> a new `PermissionEvaluator`.

**Tests:**
The test spec `AssociatedDatasetServiceRemoveSpec.groovy` already covers the happy path, not-found,
already-removed, persistence failure, and null argument cases.

---

### Task 3 — Policy: Create `AssociatedDatasetRemovedPolicy` + Removal Notification Directive

**Layer:** Application (policy)
**Module:** `project-management`
**Files to create:**
- `project-management/src/main/java/life/qbic/projectmanagement/application/policy/AssociatedDatasetRemovedPolicy.java`
- `project-management/src/main/java/life/qbic/projectmanagement/application/policy/directive/InformProjectCollaboratorsAboutDatasetRemoval.java`

**What `AssociatedDatasetRemovedPolicy` does:**
Mirrors `AssociatedDatasetConnectedPolicy`:
```java
public class AssociatedDatasetRemovedPolicy {
  public AssociatedDatasetRemovedPolicy(
      InformProjectCollaboratorsAboutDatasetRemoval informCollaborators) {
    DomainEventDispatcher.instance().subscribe(requireNonNull(informCollaborators));
  }
}
```

**What `InformProjectCollaboratorsAboutDatasetRemoval` does:**
Mirrors `InformProjectCollaboratorsAboutDatasetConnection`. Subscribes to `AssociatedDatasetRemovedEvent`, resolves collaborators excluding the actor, and enqueues a JobRunr background email job for each.

Key differences from the connect directive:
- Class name / event type: `AssociatedDatasetRemovedEvent`
- Subject: "Dataset connection removed from project"
- Uses a new `Messages.datasetRemovedFromProject(...)` template

---

### Task 4 — Messages: Add Removal Email Template

**Layer:** Application
**Module:** `project-management`
**Files to change:**
- `project-management/src/main/java/life/qbic/projectmanagement/application/Messages.java`

**What to add:**
```java
/**
 * A pre-formatted message that informs a project collaborator about
 * a dataset connection that has been removed by a teammate.
 */
public static String datasetRemovedFromProject(String fullNameUser, String projectTitle,
    String datasetTitle, String datasetPid, String projectUri) {
  return String.format("""
      Dear %s,

      a dataset connection has been removed from the project '%s':

        Title: %s
        PID:   %s

      Please open the project to see the current list of connected datasets:

      %s
      """, fullNameUser, projectTitle, datasetTitle, datasetPid, projectUri);
}
```

---

### Task 5 — Toast Notifications: Add Removal Toast Properties

**Layer:** UI (i18n)
**Module:** `datamanager-app`
**Files to change:**
- `datamanager-app/src/main/resources/messages/toast-notifications.properties`

**What to add:**
```properties
dataset.removed.success.message.type=html
dataset.removed.success.message.text=Dataset <strong>{0}</strong> removed from project.
dataset.removed.success.level=success
dataset.removed.failure.message.type=html
dataset.removed.failure.message.text=Failed to remove dataset. Please contact <a href="mailto:support@qbic.zendesk.com">QBiC support</a>.
dataset.removed.failure.level=error
```

---

### Task 6 — UI: Add Remove Button + Confirmation + Service Call

**Layer:** UI
**Module:** `datamanager-app`
**Files to change:**
- `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/datasets/ConnectedResourcesComponent.java`
- `datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/datasets/ConnectedDatasetsMain.java`

#### 6a. `ConnectedResourcesComponent` — Add per-card Remove button

In the `buildCard(ConnectedDatasetView view)` method, add a "Remove" button (with `VaadinIcon.TRASH`) to the card header row, placed after the spacer. The button:
- Only renders when the current user has WRITE permission on the project (`userPermissions.editProject()`)
- Fires a custom `RemoveDatasetClickEvent` carrying the `ConnectedDatasetView.id()` (the aggregate ID)

Add a new event class and listener registration method:
```java
public Registration addRemoveDatasetClickListener(
    ComponentEventListener<RemoveDatasetClickEvent> listener) {
  return addListener(RemoveDatasetClickEvent.class, listener);
}

public static class RemoveDatasetClickEvent
    extends ComponentEvent<ConnectedResourcesComponent> {
  private final String datasetId;
  // constructor + getter
}
```

The component needs write-permission awareness. Add a `setWriteAllowed(boolean)` method,
or pass the permission check result through `setContext()`. The `ConnectedDatasetsMain` view
already has `userPermissions` injected and can propagate it.

#### 6b. `ConnectedDatasetsMain` — Wire the `AlertDialog` confirmation + service call

When a `RemoveDatasetClickEvent` fires, open an `AlertDialog` using the centralized factory from Task 0:

```java
AlertDialog.danger(this,
    "Remove dataset connection?",
    "This will disconnect the dataset '<b>" + view.title()
        + "</b>' from the project. The connection can be re-established later.",
    () -> performRemove(view.id()))
    .open();
```

On confirm (`performRemove`):
   - Resolve current user ID via `authenticationToUserIdTranslator`
   - Call `associatedDatasetService.removeDataset(datasetId, userId)`
   - On `Result.Value` → show success toast, call `connectedResourcesComponent.refresh()`
   - On `Result.Error(DATASET_NOT_FOUND)` → show error toast "Dataset not found"
   - On `Result.Error(DATASET_ALREADY_REMOVED)` → show info toast "Already removed"
   - On `Result.Error(REMOVAL_FAILED)` → show error toast with support link

---

### Task 7 — Wire Removal Policy Bean

**Layer:** Composition root
**Module:** `datamanager-app`
**Files to change:**
- `datamanager-app/src/main/java/life/qbic/datamanager/AppConfig.java`

**What to add:**
A new `@Bean` method mirroring the `associatedDatasetConnectedPolicy` bean:
```java
@Bean
public AssociatedDatasetRemovedPolicy associatedDatasetRemovedPolicy(
    EmailService emailService,
    ProjectAccessService projectAccessService,
    UserInformationService userInformationService,
    ProjectInformationService projectInformationService,
    AppContextProvider appContextProvider,
    JobScheduler jobScheduler) {
  var informCollaborators = new InformProjectCollaboratorsAboutDatasetRemoval(
      emailService, projectAccessService, userInformationService,
      projectInformationService, appContextProvider, jobScheduler);
  return new AssociatedDatasetRemovedPolicy(informCollaborators);
}
```

---

## Execution Order (dependency graph)

```
Task 0 (AlertDialog factory)     Task 1 (domain event emission)
  ↓                                ↓
                                  Task 2 (service removeDataset)
                                    ↓
Task 3 (policy + directive)      Task 5 (toast properties)
  ↓                                ↓
Task 4 (email template)          Task 7 (wire policy bean)
  ↓                                ↓
  └──────────── Task 6 (UI) ──────┘
```

Task 0 (`AlertDialog`) is independent of all domain work and can be done in parallel.
Tasks 1–2 must be sequential (domain first, then service).
Tasks 3, 4, 5, 7 can be parallelized after Task 2.
Task 6 depends on Tasks 0, 2, 5 (needs the service API, the i18n keys, and the alert dialog API).

Recommended order for a single developer:
0 → 1 → 2 → 3 → 4 → 5 → 7 → 6

## UX refinements applied post-implementation

These tweaks were applied based on stakeholder feedback after initial implementation:

| Change | Before | After |
|---|---|---|
| Dialog body message | `'<b>{title}</b>'` (HTML literal since `Span` escapes) | Plain text `'{title}'` |
| Remove button position | Left of spacer (left-aligned) | After spacer + published date (right-aligned, matches app convention) |
| Deletion UX flow | Blocking call on UI thread | `CompletableFuture.supplyAsync` on `ForkJoinPool.commonPool` |
| UI feedback | Success/error toast only (after blocking wait) | `pendingTaskToast("dataset.removing.in-progress")` shown immediately; replaced with success/error toast on completion |
| Error toasts for non-critical failures | Toast shown for `DATASET_NOT_FOUND` / `DATASET_ALREADY_REMOVED` | Silent card-list refresh only |

## File Summary

### New files (4)
| File | Module |
|---|---|
| `…/views/general/dialog/AlertDialog.java` | `datamanager-app` |
| `…/application/policy/AssociatedDatasetRemovedPolicy.java` | `project-management` |
| `…/application/policy/directive/InformProjectCollaboratorsAboutDatasetRemoval.java` | `project-management` |
| (none for tests — specs already exist) | |

### Modified files (7)
| File | Module | Change |
|---|---|---|
| `AssociatedDataset.java` | `project-management` | Wire `emitRemovedEvent()` in `remove()`; change signature to accept `removedByUserId` |
| `AssociatedDatasetService.java` | `project-management` | Add `removeDataset()` method |
| `Messages.java` | `project-management` | Add `datasetRemovedFromProject()` template |
| `AppConfig.java` | `datamanager-app` | Wire `AssociatedDatasetRemovedPolicy` bean |
| `ConnectedResourcesComponent.java` | `datamanager-app` | Per-card Remove button (WRITE-only, right-aligned) + event; `setWriteAllowed(boolean)` |
| `ConnectedDatasetsMain.java` | `datamanager-app` | `AlertDialog.danger()` + `CompletableFuture`-driven `removeDataset` with pending/success/error toasts; lifecycle hooks for `UiHandle` |
| `toast-notifications.properties` | `datamanager-app` | Add `dataset.removed.*` + `dataset.removing.in-progress` entries |

---

## Testing Checklist

| Test | Type | Module | Status |
|---|---|---|---|
| `AssociatedDatasetServiceRemoveSpec.groovy` | Unit (Spock) | `project-management` | ✅ 6 passing (pre-existing) |
| `AssociatedDatasetRemovedEventSpec.groovy` | Unit (Spock) | `project-management` | ✅ 7 passing (pre-existing) |
| `AssociatedDatasetRemoveSpec.groovy` (NEW) | Unit (Spock) | `project-management` | ✅ 4 passing — domain layer `remove(userId)` + event emission |
| `AlertDialogTest.java` (NEW) | Unit (JUnit) | `datamanager-app` | ✅ 12 passing |
| UI: Remove button visible for WRITE users, hidden for READ users | Manual / integration | `datamanager-app` | 🟡 To add (manual verification) |
| UI: `AlertDialog.danger()` + pending-toast flow matches designer spec | Manual | `datamanager-app` |  To add |
| Email: Removal notification sent to collaborators (not actor) | Integration | `project-management` | 🟡 To add |

---

## Risks and Open Questions

1. **Requirement ID collision:** `DATA-R-01` in `docs/requirements.md` already refers to a different feature. The PO must clarify which IDs to use before merging. This does not block development, only the final commit.

2. **ACL strategy for remove:** The aggregate does not carry its own ACL entry (ADR-0003). The service method must check `WRITE` on the parent project. The implementation approach (programmatic check vs. `@PreAuthorize`) should be consistent with how the connect flow will evolve — currently connect receives `projectId` as a parameter, but remove does not.

3. **Remove button visibility:** Only users with `WRITE` permission should see the Remove button. The `ConnectedResourcesComponent` currently has no permission awareness — it needs to receive it from `ConnectedDatasetsMain` (which injects `UserPermissions`).

4. **Event actor identity:** The `remove()` method currently uses `connectedBy` as the implicit actor. If the removing user differs from the connecting user, the event should reflect who actually performed the removal. Changing `remove()` to accept `removedByUserId` is the clean solution.

---

## Definition of Done

- [x] All acceptance criteria pass
- [ ] Domain layer: `remove(userId)` transitions state, emits event, tested
- [ ] Application layer: `removeDataset()` returns correct `Result` for all paths, tested
- [ ] Policy wired: removal sends email to collaborators (excluding actor)
- [ ] UI: `AlertDialog` factory available and produces correctly styled dialogs for danger intent
- [ ] UI: Remove button visible per ACL, `AlertDialog` confirmation required, success/error toast shown
- [ ] View refreshes after successful removal
- [ ] No stack traces leak to the user (per `ExceptionHandling.md`)
- [ ] All new code follows Google Java Style
- [ ] No Vaadin imports in domain/application layers
- [ ] Existing tests still pass (`./mvnw clean verify`)
