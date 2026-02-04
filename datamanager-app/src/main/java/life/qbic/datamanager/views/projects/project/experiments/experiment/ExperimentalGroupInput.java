package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.dataview.ComboBoxListDataView;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import jakarta.validation.constraints.Min;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.Container;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput.ExperimentalGroupBean;
import life.qbic.projectmanagement.application.VariableValueFormatter;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.VariableName;
import org.springframework.lang.Nullable;

/**
 * UI input component for defining (or editing) a single {@link ExperimentalGroup}.
 *
 * <h2>Intent</h2>
 * <p>
 * In the application, an experimental group captures the notion of a <em>condition</em> plus an
 * expected number of biological replicates. This class provides a compact UI representation of that
 * concept as a Vaadin {@link CustomField} with a strongly typed model value
 * ({@link ExperimentalGroupBean}).
 * </p>
 *
 * <h2>What the user can enter</h2>
 * <ul>
 *   <li><b>Group name</b> (optional): a human-readable label used for display purposes.</li>
 *   <li><b>Condition</b>: a selection of {@link VariableLevel}s representing one level per
 *       experimental variable.</li>
 *   <li><b>Biological replicates</b>: the expected sample size for the group.</li>
 * </ul>
 *
 * <h2>Contract / behavior</h2>
 * <ul>
 *   <li>
 *     <b>One level per variable:</b> this component enforces a "single level per variable" rule.
 *     If the user selects a second level for the same variable, the previously selected one is
 *     automatically deselected (see {@link #overwriteSelectionOfSameVariable(MultiSelectComboBox)}).
 *   </li>
 *   <li>
 *     <b>Completeness validation:</b> The component derives the number of distinct variables from
 *     {@code availableLevels} ({@link #variableCount}) and validates that the selection size equals
 *     that count. This expresses the requirement "please select one level for each variable".
 *   </li>
 *   <li>
 *     <b>Model synchronization:</b> Any change to the name, condition selection, or replicate count
 *     emits an {@link InputChangedEvent}. This makes it easy for
 *     parent components to react to changes without listening to each sub-field individually.
 *   </li>
 *   <li>
 *     <b>Edit support:</b> The component can hold an existing group's database id
 *     ({@link #setGroupId(long)}) and an experiment-unique group number
 *     ({@link #setGroupNumber(int)}). These values are included in the model value so callers can
 *     map UI rows back to persisted groups.
 *   </li>
 *   <li>
 *     <b>Optional deletion:</b> If {@code allowDeletion} is {@code true}, a close icon is rendered
 *     and clicking it triggers a {@link RemoveEvent} via the dedicated listener list.
 *   </li>
 * </ul>
 *
 * <h2>Scope</h2>
 * <p>
 * This is a UI component. It does not create or persist domain objects; it only collects and
 * validates user input required to later construct or update a domain {@link ExperimentalGroup}
 * (typically in presenter/service/application-layer code).
 * </p>
 *
 * @since 1.0.0
 */
@Tag(Tag.DIV)
public class ExperimentalGroupInput extends CustomField<ExperimentalGroupBean> {

  private static final Comparator<VariableLevel> VARIABLE_LEVEL_COMPARATOR = Comparator
      .comparing(VariableLevel::variableName)
      .thenComparing(VariableLevel::value);
  private static final ItemLabelGenerator<VariableLevel> VARIABLE_LEVEL_ITEM_LABEL_GENERATOR = it -> String.format(
      "%s: %s", it.variableName(),
      VariableValueFormatter.format(it.value(), it.unit()));

  private final List<ComponentEventListener<RemoveEvent>> removeEventListeners;
  private final TextField nameField;

  /**
   * Database id of an existing group (when editing).
   * <p>
   * Defaults to {@code -1} to indicate "not assigned / new group".
   * </p>
   */
  private long id = -1;

  /**
   * Experiment-unique group number (domain-level identifier used in parts of the UI/back end).
   * <p>
   * Defaults to {@code -1} to indicate "not assigned".
   * </p>
   */
  private int groupNumber = -1;

  private final MultiSelectComboBox<VariableLevel> variableLevelSelect;
  private final NumberField replicateCountField;

  /**
   * Number of distinct experimental variables inferred from the provided available levels.
   * <p>
   * Used to validate that the user selected exactly one level per variable.
   * </p>
   */
  private int variableCount = 0;

  /**
   * Keep created binders reachable (primarily relevant for validation wiring).
   * <p>
   * Note: currently this list is not populated, but retained as a field to allow future extensions.
   * </p>
   */
  private final List<Binder<?>> binders = new ArrayList<>();

  /**
   * Creates an {@link ExperimentalGroupInput} UI row.
   *
   * <p>
   * The component consists of:
   * </p>
   * <ul>
   *   <li>a {@link TextField} for an optional group name</li>
   *   <li>a {@link MultiSelectComboBox} for selecting the condition levels</li>
   *   <li>a {@link NumberField} for selecting the number of biological replicates</li>
   * </ul>
   *
   * <h3>Required caller input</h3>
   * <p>
   * The {@code availableLevels} collection should contain all selectable {@link VariableLevel}s for
   * the experiment design. The component computes {@link #variableCount} from it (counting distinct
   * {@link VariableLevel#variableName()}) and validates that exactly that many levels are selected.
   * </p>
   *
   * <h3>Deletion</h3>
   * <p>
   * If {@code allowDeletion} is {@code true}, a close icon is shown. Clicking it emits a
   * {@link RemoveEvent} to listeners registered via {@link #addRemoveEventListener(ComponentEventListener)}.
   * </p>
   *
   * @param availableLevels collection of {@link VariableLevel}s defined for an {@link Experiment}
   * @param allowDeletion whether this row can be removed via a UI action
   */
  public ExperimentalGroupInput(Collection<VariableLevel> availableLevels, boolean allowDeletion) {
    addClassName("experimental-group-entry");
    removeEventListeners = new ArrayList<>();

    nameField = generateGroupNameField();
    variableLevelSelect = generateVariableLevelSelect();
    replicateCountField = generateBiologicalReplicateField();

    var deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);
    deleteIcon.addClickListener(
        event -> fireRemoveEvent(new RemoveEvent(this, event.isFromClient())));
    add(nameField, variableLevelSelect, replicateCountField);
    if (allowDeletion) {
      add(deleteIcon);
    }
    setLevels(availableLevels);
    addValidationForVariableCount();

    // Keep the CustomField model value in sync and emit a typed event for parent components.
    nameField.addValueChangeListener(event -> onAnyInputChanged(event.isFromClient()));
    variableLevelSelect.addValueChangeListener(event -> onAnyInputChanged(event.isFromClient()));
    replicateCountField.addValueChangeListener(event -> onAnyInputChanged(event.isFromClient()));

    variableLevelSelect.addValueChangeListener(
        event -> setInvalid(variableLevelSelect.isInvalid() || replicateCountField.isInvalid()));
    replicateCountField.addValueChangeListener(
        event -> setInvalid(variableLevelSelect.isInvalid() || replicateCountField.isInvalid()));
  }

  /**
   * Called when any user-relevant sub-field changes.
   *
   * <p>
   * Updates the {@link CustomField}'s model value and emits an {@link InputChangedEvent}.
   * </p>
   *
   * @param fromClient whether the change originated from the client/browser
   */
  private void onAnyInputChanged(boolean fromClient) {
    updateValue();
    fireEvent(new InputChangedEvent(this, fromClient));
  }

  /**
   * Fired whenever the user changes any relevant part of this input (name, condition, replicate
   * count).
   *
   * <p>
   * This event is intended for parent components to keep derived state in sync (e.g. enabling/disabling
   * "Save", recomputing summaries, or performing cross-row validations).
   * </p>
   *
   * @since 1.12.0
   */
  public static class InputChangedEvent extends ComponentEvent<ExperimentalGroupInput> {

    @Serial
    private static final long serialVersionUID = -2689783643228164804L;

    public InputChangedEvent(ExperimentalGroupInput source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * Registers a listener that is notified whenever this {@link ExperimentalGroupInput} changes.
   *
   * @param listener the listener to add
   * @return a registration for removing the listener
   * @since 1.12.0
   */
  public Registration addInputChangedListener(ComponentEventListener<InputChangedEvent> listener) {
    return addListener(InputChangedEvent.class, listener);
  }

  /**
   * Sets the currently selected condition levels (programmatic API).
   *
   * <p>
   * This method will also refresh the field's model value and emit an {@link InputChangedEvent}
   * with {@code fromClient=false}.
   * </p>
   *
   * @param levels the levels to select
   */
  public void setCondition(Collection<VariableLevel> levels) {
    this.variableLevelSelect.setValue(levels);
    onAnyInputChanged(false);
  }

  /**
   * Sets the group name (programmatic API).
   *
   * <p>
   * This method will also refresh the field's model value and emit an {@link InputChangedEvent}
   * with {@code fromClient=false}.
   * </p>
   *
   * @param groupName the new group name (may be empty)
   */
  public void setGroupName(String groupName) {
    this.nameField.setValue(groupName);
    onAnyInputChanged(false);
  }

  /**
   * Sets the replicate count (programmatic API).
   *
   * <p>
   * This method will also refresh the field's model value and emit an {@link InputChangedEvent}
   * with {@code fromClient=false}.
   * </p>
   *
   * @param numberOfReplicates replicate count to set (expected &gt;= 1)
   */
  public void setReplicateCount(int numberOfReplicates) {
    this.replicateCountField.setValue((double) numberOfReplicates);
    onAnyInputChanged(false);
  }

  /**
   * Notifies all registered remove listeners.
   *
   * @param event the remove event to dispatch
   */
  private void fireRemoveEvent(RemoveEvent event) {
    removeEventListeners.forEach(listener -> listener.onComponentEvent(event));
  }

  /**
   * Adds a listener that is notified when the user requests to remove this input row.
   *
   * <p>
   * The remove action is only available if the component was created with {@code allowDeletion=true}.
   * </p>
   *
   * @param listener listener to register
   */
  public void addRemoveEventListener(ComponentEventListener<RemoveEvent> listener) {
    removeEventListeners.add(listener);
  }

  /**
   * Enforces the rule "only one level per variable" on the given {@link MultiSelectComboBox}.
   *
   * <p>
   * When the user adds selection(s), any previously selected level(s) belonging to the same variable
   * name are automatically deselected. This effectively turns the control into a per-variable single
   * select while still allowing a multi-variable condition to be represented.
   * </p>
   *
   * @param selectComboBox the condition selector to apply this behavior to
   */
  private static void overwriteSelectionOfSameVariable(
      MultiSelectComboBox<VariableLevel> selectComboBox) {
    selectComboBox.addSelectionListener(event -> {
      Set<VariableName> variableNamesInAddedSelection = event
          .getAddedSelection().stream()
          .map(VariableLevel::variableName)
          .map(VariableName::new)
          .collect(Collectors.toSet());
      List<VariableLevel> previousSelectionOfSelectedVariable = event
          .getOldSelection().stream()
          .filter(previouslySelected -> variableNamesInAddedSelection.contains(
              new VariableName(previouslySelected.variableName())))
          .toList();
      selectComboBox.deselect(previousSelectionOfSelectedVariable);
    });
  }

  /**
   * Adds a validator that ensures the user selected one level for each distinct variable.
   *
   * <p>
   * The expected number of selections is {@link #variableCount}, derived from the provided available
   * levels (see {@link #setLevels(Collection)}).
   * </p>
   */
  private void addValidationForVariableCount() {
    Binder<Container<Set<VariableLevel>>> variableLevelSelectBinder = new Binder<>();
    variableLevelSelectBinder.forField(variableLevelSelect)
        .withValidator(levels -> levels.size() == variableCount,
            "Please select one level for each variable.")
        .bind(Container::get, Container::set);
  }

  /**
   * Initializes the condition selector with available levels and computes {@link #variableCount}.
   *
   * <p>
   * In addition to setting items, this registers a selection listener that updates the visible
   * entries so the user is not offered levels that would violate "one level per variable".
   * </p>
   *
   * @param availableLevels available {@link VariableLevel}s for the current experiment design
   */
  private void setLevels(Collection<VariableLevel> availableLevels) {
    variableLevelSelect.setItems(this::filterLevel, availableLevels);
    variableLevelSelect.addSelectionListener(event -> filterShownLevels());
    this.variableCount = (int) availableLevels.stream().map(VariableLevel::variableName).distinct()
        .count();
  }

  /**
   * Builds the field's model value from the current UI state.
   *
   * <p>
   * The returned bean is a UI-level representation containing:
   * </p>
   * <ul>
   *   <li>{@link #id} (useful for updates)</li>
   *   <li>group name</li>
   *   <li>replicate count</li>
   *   <li>selected levels (condition), in stable sorted order</li>
   * </ul>
   *
   * @return current model value
   */
  @Override
  protected ExperimentalGroupBean generateModelValue() {
    var name = getName();
    var levels = getCondition();
    var sampleSize = getReplicateCount();
    return new ExperimentalGroupBean(id, name, sampleSize, levels);
  }

  /**
   * @return the currently entered group name (may be empty)
   */
  public String getName() {
    return nameField.getValue();
  }

  /**
   * Returns the persisted group id that this input represents.
   *
   * <p>
   * For new groups this is typically {@code -1}.
   * </p>
   *
   * @return persisted group id or {@code -1}
   */
  public long getGroupId() {
    return id;
  }

  /**
   * Returns the experiment-unique group number (if assigned).
   *
   * @return group number or {@code -1}
   */
  public int groupNumber() {
    return this.groupNumber;
  }

  /**
   * Sets the experiment-unique group number for this input.
   *
   * @param groupNumber the group number
   */
  public void setGroupNumber(int groupNumber) {
    this.groupNumber = groupNumber;
  }

  /**
   * Returns the replicate count (biological replicates).
   *
   * <p>
   * If the field has no value, returns {@code 0}. The UI defaults to {@code 1}.
   * </p>
   *
   * @return replicate count or {@code 0} if no value is present
   */
  public int getReplicateCount() {
    return replicateCountField.getOptionalValue().map(Double::intValue).orElse(0);
  }

  /**
   * Returns the selected condition levels, sorted for stable downstream processing.
   *
   * @return sorted list of selected variable levels
   */
  public List<VariableLevel> getCondition() {
    return variableLevelSelect.getValue().stream()
        .sorted(VARIABLE_LEVEL_COMPARATOR)
        .toList();
  }

  @Override
  public ExperimentalGroupBean getEmptyValue() {
    return generateModelValue();
  }

  /**
   * Updates the UI representation to match the given model value.
   *
   * <p>
   * Note: The group name is intentionally not set here. Callers that use
   * {@link #setPresentationValue(ExperimentalGroupBean)} to populate the field should set the name
   * separately via {@link #setGroupName(String)} if needed.
   * </p>
   *
   * @param newPresentationValue new model value to present
   */
  @Override
  protected void setPresentationValue(ExperimentalGroupBean newPresentationValue) {
    variableLevelSelect.setValue(newPresentationValue.levels);
    replicateCountField.setValue((double) newPresentationValue.replicateCount);
  }

  /**
   * Propagates required state to the relevant inner fields.
   *
   * @param requiredIndicatorVisible whether required indicator should be shown
   */
  @Override
  public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
    super.setRequiredIndicatorVisible(requiredIndicatorVisible);
    variableLevelSelect.setRequired(requiredIndicatorVisible);
    variableLevelSelect.setRequiredIndicatorVisible(requiredIndicatorVisible);
    replicateCountField.setRequiredIndicatorVisible(requiredIndicatorVisible);
  }

  /**
   * A field is considered invalid if any of its relevant inner fields are invalid.
   *
   * @return {@code true} if invalid, {@code false} otherwise
   */
  @Override
  public boolean isInvalid() {
    super.setInvalid(variableLevelSelect.isInvalid() || replicateCountField.isInvalid());
    return super.isInvalid();
  }

  /**
   * Creates the condition selector component.
   *
   * <p>
   * The selector:
   * </p>
   * <ul>
   *   <li>shows formatted labels (variable name + value + optional unit)</li>
   *   <li>does not allow custom values</li>
   *   <li>enforces "one level per variable"</li>
   * </ul>
   *
   * @return configured multi-select combo box
   */
  private MultiSelectComboBox<VariableLevel> generateVariableLevelSelect() {
    MultiSelectComboBox<VariableLevel> selectComboBox = new MultiSelectComboBox<>();
    selectComboBox.setLabel("Condition");
    selectComboBox.addClassName("combo-box");
    selectComboBox.addClassName("chip-badge");
    selectComboBox.setAllowCustomValue(false);
    selectComboBox.setItemLabelGenerator(VARIABLE_LEVEL_ITEM_LABEL_GENERATOR);
    selectComboBox.setWidthFull();
    overwriteSelectionOfSameVariable(selectComboBox);
    return selectComboBox;
  }

  /**
   * Creates the input for selecting the number of biological replicates.
   *
   * <p>
   * Configured as:
   * </p>
   * <ul>
   *   <li>step size: 1</li>
   *   <li>minimum: 1</li>
   *   <li>default value: 1</li>
   * </ul>
   *
   * @return configured number field
   */
  private NumberField generateBiologicalReplicateField() {
    NumberField numberField = new NumberField();
    numberField.addClassName("number-field");
    numberField.setLabel("Biological Replicates");
    numberField.setStepButtonsVisible(true);
    numberField.setStep(1);
    numberField.setMin(1);
    numberField.setValue(1.0);
    numberField.setErrorMessage("Please specify a valid number of replicates");
    return numberField;
  }

  /**
   * Creates the optional group name input.
   *
   * @return configured text field
   */
  private TextField generateGroupNameField() {
    TextField textField = new TextField();
    textField.addClassName("text-field");
    textField.setLabel("Group Name");
    textField.setPlaceholder("optional");
    return textField;
  }

  /**
   * Filters the visible levels in the dropdown based on current selection.
   *
   * <p>
   * Keeps already selected items visible, but hides levels belonging to variable names that already
   * have a selected level. This complements {@link #overwriteSelectionOfSameVariable(MultiSelectComboBox)}
   * by guiding the user through a reduced set of choices.
   * </p>
   */
  private void filterShownLevels() {
    ComboBoxListDataView<VariableLevel> listDataView = variableLevelSelect.getListDataView();
    listDataView.setFilter(
        level -> variableLevelSelect.getSelectedItems().stream()
            .allMatch(selectedLevel ->
                level.equals(selectedLevel)
                    || !level.variableName().equals(selectedLevel.variableName())));
  }

  /**
   * Filters items based on the user's typed filter string.
   *
   * <p>
   * A {@link VariableLevel} matches if the filter text is contained in the variable name or in the
   * formatted level value (including unit, if present).
   * </p>
   *
   * @param level the level candidate
   * @param filterString the user-entered filter string
   * @return {@code true} if the level should be shown
   */
  private boolean filterLevel(VariableLevel level, String filterString) {
    boolean levelValueContainsFilterString = VariableValueFormatter.format(level.value(),
            level.unit()).toLowerCase()
        .contains(filterString.toLowerCase());
    boolean variableNameContainsFilterString = level.variableName().toLowerCase()
        .contains(filterString.toLowerCase());
    return variableNameContainsFilterString || levelValueContainsFilterString;
  }

  /**
   * Sets the persisted group id represented by this input.
   *
   * <p>
   * Used by edit flows so {@link #generateModelValue()} includes the id for update operations.
   * </p>
   *
   * @param id group id
   */
  public void setGroupId(long id) {
    this.id = id;
  }

  /**
   * Sets the error message shown on the condition selector.
   *
   * @param message the message to show
   */
  public void setConditionErrorMessage(String message) {
    variableLevelSelect.setErrorMessage(message);
  }

  /**
   * Sets the invalid state of the condition selector.
   *
   * @param value invalid state
   */
  public void setConditionInvalid(boolean value) {
    variableLevelSelect.setInvalid(value);
  }

  /**
   * @return the current condition selector error message
   */
  public String getConditionErrorMessage() {
    return variableLevelSelect.getErrorMessage();
  }

  /**
   * Model value of {@link ExperimentalGroupInput}.
   *
   * <p>
   * This bean represents the user-entered data for an experimental group in a UI-friendly form.
   * It is the value type used by this {@link CustomField}.
   * </p>
   *
   * <h2>Semantics</h2>
   * <ul>
   *   <li>{@code id}: persisted id (if editing), otherwise typically {@code -1}</li>
   *   <li>{@code name}: optional display name</li>
   *   <li>{@code replicateCount}: biological replicate count (validated to be at least 1)</li>
   *   <li>{@code levels}: the chosen condition levels</li>
   * </ul>
   */
  public static class ExperimentalGroupBean {

    private final List<VariableLevel> levels = new ArrayList<>();

    private final long id;
    private final String name;

    @Min(1)
    private final int replicateCount;

    public ExperimentalGroupBean(long id, String name, int replicateCount, List<VariableLevel> levels) {
      this.id = id;
      this.name = name;
      this.replicateCount = replicateCount;
      this.levels.addAll(levels);
    }

    /**
     * Returns the selected variable levels.
     *
     * @return unmodifiable list of levels
     */
    public List<VariableLevel> getLevels() {
      return Collections.unmodifiableList(levels);
    }

    /**
     * @return replicate count (biological replicates)
     */
    public int getReplicateCount() {
      return replicateCount;
    }
  }

  /**
   * Event emitted when the user requests removal of this component (e.g. clicking the close icon).
   *
   * <p>
   * This event is dispatched via {@link #addRemoveEventListener(ComponentEventListener)} rather than
   * Vaadin's typed event bus, because removal is treated as a local component concern.
   * </p>
   */
  public static class RemoveEvent extends ComponentEvent<ExperimentalGroupInput> {

    @Serial
    private static final long serialVersionUID = 2934203596212238997L;

    public RemoveEvent(ExperimentalGroupInput source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * UI-level representation of a single selectable variable level.
   *
   * <p>
   * This record is used as the item type for the condition selector. It is intentionally simple and
   * decoupled from domain representations to keep the view layer lightweight.
   * </p>
   *
   * @param variableName the variable name (e.g. "genotype")
   * @param value the selected level value (e.g. "wildtype")
   * @param unit optional unit (e.g. "mmol/L"); may be {@code null}
   */
  public record VariableLevel(String variableName, String value, @Nullable String unit) {

  }
}
