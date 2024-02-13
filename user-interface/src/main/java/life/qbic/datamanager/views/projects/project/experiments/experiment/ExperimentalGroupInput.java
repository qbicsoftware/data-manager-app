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
import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.VariableName;

/**
 * <b>ExperimentalGroupInput Field</b>
 * ExperimentalGroupInput is a {@link CustomField} which enables the user to create an
 * {@link ExperimentalGroup} by defining the {@link Condition} and number of
 * {@link BiologicalReplicate} associated with the {@link ExperimentalGroup}
 */
@Tag(Tag.DIV)
public class ExperimentalGroupInput extends CustomField<ExperimentalGroupBean> {

  private static final Comparator<VariableLevel> VARIABLE_LEVEL_COMPARATOR = Comparator
      .<VariableLevel, String>comparing(level -> level.variableName().value())
      .thenComparing(level -> level.experimentalValue().value());
  private static final ItemLabelGenerator<VariableLevel> VARIABLE_LEVEL_ITEM_LABEL_GENERATOR = it -> String.format(
      "%s: %s", it.variableName().value(),
      VariableValueFormatter.format(it.experimentalValue()));

  private final List<ComponentEventListener<RemoveEvent>> removeEventListeners;
  private final TextField nameField;
  private final MultiSelectComboBox<VariableLevel> variableLevelSelect;
  private final NumberField replicateCountField;
  int variableCount = 0;
  private final List<Binder<?>> binders = new ArrayList<>();

  /**
   * ExperimentalGroupInput is a {@link CustomField} which contains a {@link MultiSelectComboBox}
   * allowing the user to define the {@link Condition}, a {@link NumberField} enabling the user
   * to define the number of {@link BiologicalReplicate} within an {@link ExperimentalGroup}, and
   * a {@link TextField} to optionally name the group.
   *
   * @param availableLevels Collection of {@link VariableLevel} defined for an {@link Experiment}
   */
  public ExperimentalGroupInput(Collection<VariableLevel> availableLevels) {
    addClassName("experimental-group-entry");
    removeEventListeners = new ArrayList<>();

    nameField = generateGroupNameField();
    variableLevelSelect = generateVariableLevelSelect();
    replicateCountField = generateBiologicalReplicateField();

    var deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);
    deleteIcon.addClickListener(
        event -> fireRemoveEvent(new RemoveEvent(this, event.isFromClient())));
    add(nameField, variableLevelSelect, replicateCountField, deleteIcon);
    setLevels(availableLevels);
    addValidationForVariableCount();
    variableLevelSelect.addValueChangeListener(
        event -> setInvalid(variableLevelSelect.isInvalid() || replicateCountField.isInvalid()));
    replicateCountField.addValueChangeListener(
        event -> setInvalid(variableLevelSelect.isInvalid() || replicateCountField.isInvalid()));
    nameField.addValueChangeListener(
        event -> setInvalid(variableLevelSelect.isInvalid() || replicateCountField.isInvalid()));
  }

  public void setCondition(Collection<VariableLevel> levels) {
    this.variableLevelSelect.setValue(levels);
  }

  public void setGroupName(String groupName) {
    this.nameField.setValue(groupName);
  }

  public void setReplicateCount(int numberOfReplicates) {
    this.replicateCountField.setValue((double) numberOfReplicates);
  }

  private void fireRemoveEvent(RemoveEvent event) {
    removeEventListeners.forEach(listener -> listener.onComponentEvent(event));
  }

  public void addRemoveEventListener(ComponentEventListener<RemoveEvent> listener) {
    removeEventListeners.add(listener);
  }

  private static void overwriteSelectionOfSameVariable(
      MultiSelectComboBox<VariableLevel> selectComboBox) {
    selectComboBox.addSelectionListener(event -> {
      Set<VariableName> variableNamesInAddedSelection = event
          .getAddedSelection().stream()
          .map(VariableLevel::variableName)
          .collect(Collectors.toSet());
      List<VariableLevel> previousSelectionOfSelectedVariable = event
          .getOldSelection().stream()
          .filter(previouslySelected -> variableNamesInAddedSelection.contains(
              previouslySelected.variableName()))
          .toList();
      selectComboBox.deselect(previousSelectionOfSelectedVariable);
    });
  }

  private void addValidationForVariableCount() {
    Binder<Container<Set<VariableLevel>>> variableLevelSelectBinder = new Binder<>();
    variableLevelSelectBinder.forField(variableLevelSelect)
        .withValidator(levels -> levels.size() == variableCount,
            "Please select one level for each variable.")
        .bind(Container::get, Container::set);
  }

  private void setLevels(Collection<VariableLevel> availableLevels) {
    variableLevelSelect.setItems(this::filterLevel, availableLevels);
    variableLevelSelect.addSelectionListener(event -> filterShownLevels());
    this.variableCount = (int) availableLevels.stream().map(VariableLevel::variableName).distinct()
        .count();
  }

  @Override
  protected ExperimentalGroupBean generateModelValue() {
    var levels = getCondition();
    var sampleSize = getReplicateCount();
    return new ExperimentalGroupBean(sampleSize, levels);
  }

  public int getReplicateCount() {
    return replicateCountField.getOptionalValue().map(Double::intValue).orElse(0);
  }

  public List<VariableLevel> getCondition() {
    return variableLevelSelect.getValue().stream()
        .sorted(VARIABLE_LEVEL_COMPARATOR)
        .toList();
  }

  @Override
  public ExperimentalGroupBean getEmptyValue() {
    return generateModelValue();
  }

  @Override
  protected void setPresentationValue(ExperimentalGroupBean newPresentationValue) {
    variableLevelSelect.setValue(newPresentationValue.levels);
    replicateCountField.setValue((double) newPresentationValue.replicateCount);
  }

  @Override
  public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
    super.setRequiredIndicatorVisible(requiredIndicatorVisible);
    variableLevelSelect.setRequired(requiredIndicatorVisible);
    variableLevelSelect.setRequiredIndicatorVisible(requiredIndicatorVisible);
    replicateCountField.setRequiredIndicatorVisible(requiredIndicatorVisible);
  }

  @Override
  public boolean isInvalid() {
    super.setInvalid(variableLevelSelect.isInvalid() || replicateCountField.isInvalid());
    return super.isInvalid();
  }

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

  private void filterShownLevels() {
    ComboBoxListDataView<VariableLevel> listDataView = variableLevelSelect.getListDataView();
    listDataView.setFilter(
        level -> variableLevelSelect.getSelectedItems().stream()
            .allMatch(selectedLevel ->
                level.equals(selectedLevel)
                    || !level.variableName().equals(selectedLevel.variableName())));
  }

  private boolean filterLevel(VariableLevel level, String filterString) {
    boolean levelValueContainsFilterString = VariableValueFormatter.format(
            level.experimentalValue()).toLowerCase()
        .contains(filterString.toLowerCase());
    boolean variableNameContainsFilterString = level.variableName().value().toLowerCase()
        .contains(filterString.toLowerCase());
    return variableNameContainsFilterString || levelValueContainsFilterString;
  }

  public static class ExperimentalGroupBean {

    private final List<VariableLevel> levels = new ArrayList<>();
    @Min(1)
    private final int replicateCount;

    public ExperimentalGroupBean(int replicateCount, List<VariableLevel> levels) {
      this.replicateCount = replicateCount;
      this.levels.addAll(levels);
    }

    public List<VariableLevel> getLevels() {
      return Collections.unmodifiableList(levels);
    }

    public int getReplicateCount() {
      return replicateCount;
    }
  }

  public static class RemoveEvent extends ComponentEvent<ExperimentalGroupInput> {

    @Serial
    private static final long serialVersionUID = 2934203596212238997L;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public RemoveEvent(ExperimentalGroupInput source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
