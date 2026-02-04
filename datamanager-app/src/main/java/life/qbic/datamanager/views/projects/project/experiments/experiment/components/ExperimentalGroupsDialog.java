package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput.VariableLevel;

/**
 * Dialog for creating and updating experimental groups in the experiment view.
 *
 * <h2>Scope / responsibility</h2>
 * <p>This dialog is responsible for collecting a list of user-defined experimental groups and
 * validating user input before it is passed to the service layer.</p>
 *
 * <h2>Use cases</h2>
 * <ul>
 *   <li><b>Create</b>: Add one or more new experimental groups (implicit when {@code editMode=false}).</li>
 *   <li><b>Edit</b>: Modify existing groups (when {@code editMode=true}).</li>
 *   <li><b>Mixed</b>: In non-editable mode, existing groups are displayed read-only while new groups
 *   can be added.</li>
 * </ul>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>The dialog will block confirmation if any contained {@link ExperimentalGroupInput} is invalid.</li>
 *   <li>The dialog performs cross-row validation for duplicate conditions (same variable-level
 *   combination across multiple groups).</li>
 *   <li>Duplicate condition validation is expressed using Vaadin field validation UX on the
 *   condition input fields.</li>
 * </ul>
 *
 * <p><b>Important:</b> Cross-row validation (like duplicate conditions) cannot live in
 * {@link ExperimentalGroupInput} alone. Therefore, the dialog subscribes to input change events from
 * each group entry and re-validates the collection.</p>
 *
 * @since 1.0.0
 */
public class ExperimentalGroupsDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 1657697182040756406L;

  /**
   * User-facing message shown on group entries that duplicate another group's condition.
   */
  private static final String DUPLICATE_CONDITION_ERROR_MESSAGE =
      "Duplicate condition: another experimental group already uses the same variable levels.";

  private final Collection<VariableLevel> experimentalVariableLevels;
  private final Div experimentalGroupsCollection = new Div();
  private final Div content = new Div();
  private final Div addNewGroupContainer = new Div();
  private final boolean editMode;
  private final List<Integer> groupsToDelete = new ArrayList<>();

  private ExperimentalGroupsDialog(Collection<VariableLevel> experimentalVariableLevels,
      boolean editMode) {
    super();
    this.editMode = editMode;
    this.experimentalVariableLevels = Objects.requireNonNull(experimentalVariableLevels);
    layoutComponent();
  }

  private ExperimentalGroupsDialog(Collection<VariableLevel> experimentalVariableLevels,
      Collection<ExperimentalGroupContent> experimentalGroupContents, boolean editMode) {
    this(experimentalVariableLevels, editMode);
    this.experimentalGroupsCollection.removeAll();
    addEntries(experimentalVariableLevels, experimentalGroupContents);
  }

  /**
   * Adds prefilled group entries to the dialog.
   *
   * <p>Each created {@link ExperimentalGroupInput} is wired to re-run cross-row validation whenever
   * the user changes the entry.</p>
   *
   * @param experimentalVariableLevels available variable levels that can be selected in the condition field
   * @param experimentalGroupContents group data used to prefill the UI
   */
  private void addEntries(Collection<VariableLevel> experimentalVariableLevels,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    experimentalGroupContents.stream().map(group -> {
      var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels, editMode);
      groupEntry.setGroupName(group.name());
      groupEntry.setGroupNumber(group.groupNumber());
      groupEntry.setGroupId(group.id());
      groupEntry.setCondition(group.variableLevels());
      groupEntry.setReplicateCount(group.size());
      groupEntry.setEnabled(editMode);
      groupEntry.addRemoveEventListener(
          listener -> removeExperimentalGroupEntry(groupEntry));

      groupEntry.addValueChangeListener(e -> validateNoDuplicateConditions());

      return groupEntry;
    }).forEach(experimentalGroupsCollection::add);
  }

  /**
   * Creates an empty instance of the dialog for creating new experimental groups.
   *
   * @param experimentalVariables the variable levels to choose from
   * @return a new dialog instance
   */
  public static ExperimentalGroupsDialog empty(Collection<VariableLevel> experimentalVariables) {
    return new ExperimentalGroupsDialog(experimentalVariables, false);
  }

  /**
   * Creates a dialog prefilled with editable experimental groups.
   *
   * @param experimentalVariables available variable levels
   * @param experimentalGroupContents existing groups used to prefill the UI
   * @return a prefilled dialog in edit mode
   */
  public static ExperimentalGroupsDialog editable(Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    return new ExperimentalGroupsDialog(experimentalVariables, experimentalGroupContents, true);
  }

  /**
   * Creates a dialog where existing groups are shown read-only, but new groups can be added.
   *
   * <p>This mode is typically used when existing groups must not be modified due to domain rules
   * (e.g., samples already registered), but users may still add additional groups.</p>
   *
   * @param experimentalVariables available variable levels
   * @param experimentalGroupContents existing groups used to prefill the UI
   * @return a prefilled dialog with non-editable existing groups and an additional empty entry
   */
  public static ExperimentalGroupsDialog nonEditable(
      Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    ExperimentalGroupsDialog dialog = new ExperimentalGroupsDialog(experimentalVariables,
        experimentalGroupContents, false);
    dialog.addNewGroupEntry();
    return dialog;
  }

  /**
   * Converts one UI entry into a serializable dialog result record.
   *
   * <p>Contract: The returned record reflects the current state of the UI entry at the time of the
   * call. The dialog does not perform additional normalization besides the ordering already applied
   * by {@link ExperimentalGroupInput#getCondition()}.</p>
   */
  private static ExperimentalGroupContent convert(ExperimentalGroupInput experimentalGroupInput) {
    return new ExperimentalGroupContent(experimentalGroupInput.getGroupId(),
        experimentalGroupInput.groupNumber(), experimentalGroupInput.getName(),
        experimentalGroupInput.getReplicateCount(),
        experimentalGroupInput.getCondition());
  }

  /**
   * Handles confirm button clicks.
   *
   * <p>Contract: Confirmation is blocked if the dialog is invalid. In particular, duplicate
   * conditions are detected and surfaced to the user before emitting a {@link ConfirmEvent}.</p>
   */
  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    validateNoDuplicateConditions();
    if (!isValid()) {
      return;
    }
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient()));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    close();
  }

  /**
   * Adds a listener for cancellation.
   *
   * @param listener listener invoked when the user cancels the dialog
   */
  public void addCancelEventListener(
      ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  /**
   * Adds a listener for confirmation.
   *
   * <p>Contract: The dialog only fires {@link ConfirmEvent} if {@link #isValid()} is {@code true} at
   * the time the confirm button is clicked.</p>
   *
   * @param listener listener invoked when the user confirms the dialog
   */
  public void addConfirmEventListener(
      ComponentEventListener<ConfirmEvent> listener) {
    addListener(ConfirmEvent.class, listener);
  }

  private void layoutComponent() {
    layoutHeaderAndFooter();

    content.addClassName("content");
    content.add(experimentalGroupsCollection);
    content.add(addNewGroupContainer);
    add(content);

    experimentalGroupsCollection.addClassName("group-collection");
    addNewGroupContainer.addClassName("add-new-group-action");
    addNewGroupEntry();

    var addNewGroupIcon = new Icon(VaadinIcon.PLUS);
    addNewGroupIcon.addClickListener(listener -> addNewGroupEntry());
    Span addGroupHelperText = new Span("Add Experimental Group");
    addGroupHelperText.addClickListener(listener -> addNewGroupEntry());
    addNewGroupContainer.add(addNewGroupIcon, addGroupHelperText);
  }

  private void layoutHeaderAndFooter() {
    setHeaderTitle(editMode ? "Edit Experimental Groups" : "Add Experimental Groups");
    addClassName("experiment-group-dialog");
    setConfirmButtonLabel(editMode ? "Save" : "Add");
    setCancelButtonLabel("Cancel");
    getFooter().add(cancelButton, confirmButton);
  }

  /**
   * Adds a new empty group entry for user input and wires it to cross-row validation.
   *
   * <p>Contract: The added entry is always deletable by the user and is enabled for input.</p>
   */
  private void addNewGroupEntry() {
    var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels, true);
    experimentalGroupsCollection.add(groupEntry);
    groupEntry.addRemoveEventListener(event -> {
      removeExperimentalGroupEntry(event.getSource());
    });

    groupEntry.addValueChangeListener(e -> validateNoDuplicateConditions());

    validateNoDuplicateConditions();
  }

  /**
   * Removes a group entry from the dialog.
   *
   * <p>Contract: The entry is removed from the UI, its group number is recorded in
   * {@link #groupsToDelete()}, and the dialog ensures that at least one empty entry remains.</p>
   *
   * @param entry the entry to remove
   */
  void removeExperimentalGroupEntry(ExperimentalGroupInput entry) {
    experimentalGroupsCollection.getChildren().filter(entry::equals)
        .findAny().ifPresent(experimentalGroupsCollection::remove);
    groupsToDelete.add(entry.groupNumber());
    refreshGroupEntries();
    validateNoDuplicateConditions();
  }

  private void refreshGroupEntries() {
    if (experimentalGroupsCollection.getChildren().toList().isEmpty()) {
      var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels, true);
      experimentalGroupsCollection.add(groupEntry);
      groupEntry.addRemoveEventListener(event -> removeExperimentalGroupEntry(event.getSource()));
      groupEntry.addValueChangeListener(e -> validateNoDuplicateConditions());
    }
  }

  /**
   * Returns experimental groups defined by the user.
   *
   * <h2>Contract</h2>
   * <ul>
   *   <li>Only entries with a non-empty condition are returned.</li>
   *   <li>The returned collection reflects the state of the UI at call time.</li>
   *   <li>Callers should check {@link #isValid()} before using the returned groups.</li>
   * </ul>
   *
   * @return a collection of {@link ExperimentalGroupContent} describing the user input
   * @since 1.10.0
   */
  public Collection<ExperimentalGroupContent> experimentalGroups() {
    return this.experimentalGroupsCollection.getChildren()
        .filter(component -> component.getClass().equals(ExperimentalGroupInput.class))
        .map(groupInput -> (ExperimentalGroupInput) groupInput)
        // the order of the filter statement matters, since we do not want to throw an exception due to type mismatch
        .filter(groupInput -> !groupInput.getCondition().isEmpty())
        .map(ExperimentalGroupsDialog::convert)
        .toList();
  }

  /**
   * Validates the dialog's current state.
   *
   * <p>This method includes both per-entry field validation and cross-row validation (duplicate
   * conditions).</p>
   *
   * @return {@code true} if all group entries are valid and no duplicate conditions exist
   */
  public boolean isValid() {
    validateNoDuplicateConditions();
    return this.experimentalGroupsCollection.getChildren()
        .filter(component -> component.getClass().equals(ExperimentalGroupInput.class))
        .noneMatch(g -> ((ExperimentalGroupInput) g).isInvalid());
  }

  /**
   * Returns the group numbers of the groups that have been removed by the user.
   *
   * <p>Use this in edit mode to instruct the service layer to delete groups that were removed in the
   * UI.</p>
   *
   * @return a list of group numbers that are supposed to be deleted
   * @since 1.10.0
   */
  public List<Integer> groupsToDelete() {
    return this.groupsToDelete.stream().toList();
  }

  /**
   * Returns all {@link ExperimentalGroupInput} components currently rendered in the dialog.
   *
   * <p>Contract: The returned list is a snapshot. Subsequent UI changes (add/remove) require a new
   * call.</p>
   */
  private List<ExperimentalGroupInput> groupInputs() {
    return this.experimentalGroupsCollection.getChildren()
        .filter(component -> component.getClass().equals(ExperimentalGroupInput.class))
        .map(ExperimentalGroupInput.class::cast)
        .toList();
  }

  /**
   * Validates that each condition occurs at most once across all group entries.
   *
   * <h2>Contract</h2>
   * <ul>
   *   <li>Entries with an empty condition are ignored (incomplete input should not block).</li>
   *   <li>Condition equality is order-insensitive with respect to selected variable levels.</li>
   *   <li>For any duplicate condition, the condition field is marked invalid and displays an error
   *   message.</li>
   *   <li>When the user fixes the input, the error is removed immediately.</li>
   * </ul>
   */
  private void validateNoDuplicateConditions() {
    List<ExperimentalGroupInput> inputs = groupInputs();

    // Clear previous duplicate errors first, so they disappear immediately when the user fixes input.
    inputs.forEach(this::clearDuplicateConditionError);

    // Build a normalized key per condition (order-insensitive).
    // Ignore empty conditions (incomplete row should not block).
    var conditionToInputs = inputs.stream()
        .filter(in -> !in.getCondition().isEmpty())
        .collect(Collectors.groupingBy(this::conditionKey));

    // Mark all inputs that are part of a duplicate condition.
    conditionToInputs.values().stream()
        .filter(list -> list.size() > 1)
        .flatMap(List::stream)
        .forEach(this::markDuplicateConditionError);
  }

  /**
   * Creates a stable, order-insensitive key for the condition of one entry.
   *
   * <p>This key is used exclusively for UI-side duplicate detection. It intentionally avoids relying
   * on equals/hashCode implementations outside the dialog and instead normalizes the selected levels
   * into a sorted, textual representation.</p>
   *
   * @param input the group input entry
   * @return a stable key representing the condition selection
   */
  private String conditionKey(ExperimentalGroupInput input) {
    Set<VariableLevel> unique = new HashSet<>(input.getCondition());
    return unique.stream()
        .map(this::variableLevelKey)
        .sorted()
        .collect(Collectors.joining("|"));
  }

  /**
   * Creates a stable textual key for one selected variable level.
   *
   * <p>Contract: Two variable levels are considered equal for duplicate detection if all components
   * of the key match (variable name, value, unit).</p>
   *
   * @param level a variable level (UI representation)
   * @return stable key
   */
  private String variableLevelKey(VariableLevel level) {
    return new StringJoiner("::")
        .add(Objects.toString(level.variableName(), ""))
        .add(Objects.toString(level.value(), ""))
        .add(Objects.toString(level.unit(), ""))
        .toString();
  }

  /**
   * Marks the provided input as invalid due to a duplicate condition.
   *
   * <p>Contract: This method only affects the condition field validation state. It should not
   * override other field-level validation errors.</p>
   *
   * @param input the input to mark as duplicate
   */
  private void markDuplicateConditionError(ExperimentalGroupInput input) {
    input.setConditionInvalid(true);
    input.setConditionErrorMessage(DUPLICATE_CONDITION_ERROR_MESSAGE);
  }

  /**
   * Clears the duplicate-condition error state for the given input (if present).
   *
   * <p>Contract: This method only clears the error if it matches the dialog's duplicate-condition
   * error message. This prevents overriding unrelated validation errors.</p>
   *
   * @param input the input to clear
   */
  private void clearDuplicateConditionError(ExperimentalGroupInput input) {
    if (DUPLICATE_CONDITION_ERROR_MESSAGE.equals(input.getConditionErrorMessage())) {
      input.setConditionErrorMessage(null);
      input.setConditionInvalid(false);
    }
  }

  /**
   * Immutable result record representing the group information entered in one dialog row.
   *
   * <p>This record is used as a DTO between the dialog (UI layer) and the view/service integration.
   * The domain layer may apply additional validation rules.</p>
   */
  public record ExperimentalGroupContent(long id, int groupNumber, String name, int size,
                                         List<VariableLevel> variableLevels) {

  }

  /**
   * Emitted when the user confirms the dialog (Save/Add).
   */
  public static class ConfirmEvent extends
      life.qbic.datamanager.views.general.ConfirmEvent<ExperimentalGroupsDialog> {

    public ConfirmEvent(ExperimentalGroupsDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * Emitted when the user cancels the dialog.
   */
  public static class CancelEvent extends
      life.qbic.datamanager.views.general.CancelEvent<ExperimentalGroupsDialog> {

    public CancelEvent(ExperimentalGroupsDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
