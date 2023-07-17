package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.dataview.ComboBoxListDataView;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.Container;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput.ExperimentalGroupBean;
import life.qbic.projectmanagement.application.ExperimentValueFormatter;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.project.experiment.Condition;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.VariableName;

/**
 * <b>ExperimentalGroupInput Field</b>
 * ExperimentalGroupInput is a {@link CustomField} which enables the user to create an
 * {@link ExperimentalGroup} by defining the {@link Condition} and number of
 * {@link BiologicalReplicate} associated with the {@link ExperimentalGroup}
 */
public class ExperimentalGroupInput extends CustomField<ExperimentalGroupBean> {

  private static final Comparator<VariableLevel> VARIABLE_LEVEL_COMPARATOR = Comparator
      .<VariableLevel, String>comparing(level -> level.variableName().value())
      .thenComparing(level -> level.experimentalValue().value());
  private static final ItemLabelGenerator<VariableLevel> VARIABLE_LEVEL_ITEM_LABEL_GENERATOR = it -> String.format(
      "%s: %s", it.variableName().value(),
      ExperimentValueFormatter.format(it.experimentalValue()));

  private final MultiSelectComboBox<VariableLevel> variableLevelSelect;
  private final NumberField replicateCountField;
  int variableCount;

  /**
   * ExperimentalGroupInput is a {@link CustomField} which contains a {@link MultiSelectComboBox}
   * allowing the user to define the {@link Condition} and a {@link NumberField} enabling the user
   * to define the number of {@link BiologicalReplicate} within an {@link ExperimentalGroup}
   *
   * @param availableLevels Collection of {@link VariableLevel} defined for an {@link Experiment}
   */
  public ExperimentalGroupInput(Collection<VariableLevel> availableLevels) {
    addClassName("group-input");

    variableLevelSelect = generateVariableLevelSelect();
    replicateCountField = generateSampleSizeField();

    Span layout = new Span(variableLevelSelect, replicateCountField);
    layout.addClassName("layout");
    add(layout);
    setLevels(availableLevels);
    addValidationForVariableCount();
    variableLevelSelect.addValueChangeListener(
        event -> setInvalid(variableLevelSelect.isInvalid() || replicateCountField.isInvalid()));
    replicateCountField.addValueChangeListener(
        event -> setInvalid(variableLevelSelect.isInvalid() || replicateCountField.isInvalid()));
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
    var levels = variableLevelSelect.getValue().stream().sorted(VARIABLE_LEVEL_COMPARATOR)
        .toList();
    var sampleSize = Optional
        .ofNullable(replicateCountField.getValue()).map(Double::intValue)
        .orElse(0);
    return new ExperimentalGroupBean(sampleSize, levels);
  }

  @Override
  protected void setPresentationValue(ExperimentalGroupBean newPresentationValue) {
    variableLevelSelect.setValue(newPresentationValue.levels);
    replicateCountField.setValue((double) newPresentationValue.sampleSize);
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
    overwriteSelectionOfSameVariable(selectComboBox);
    return selectComboBox;
  }

  private NumberField generateSampleSizeField() {
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
    boolean levelValueContainsFilterString = ExperimentValueFormatter.format(
            level.experimentalValue()).toLowerCase()
        .contains(filterString.toLowerCase());
    boolean variableNameContainsFilterString = level.variableName().value().toLowerCase()
        .contains(filterString.toLowerCase());
    return variableNameContainsFilterString || levelValueContainsFilterString;
  }

  private static void overwriteSelectionOfSameVariable(
      MultiSelectComboBox<VariableLevel> selectComboBox) {
    selectComboBox.addSelectionListener(event -> {
      Set<VariableName> variableNamesInAddedSelection = event.getAddedSelection().stream()
          .map(VariableLevel::variableName).collect(Collectors.toSet());
      List<VariableLevel> previousSelectionOfSelectedVariable = event.getOldSelection().stream()
          .filter(previouslySelected -> variableNamesInAddedSelection.contains(
              previouslySelected.variableName())).toList();
      selectComboBox.deselect(previousSelectionOfSelectedVariable);
    });
  }

  public static class ExperimentalGroupBean {

    private final List<VariableLevel> levels = new ArrayList<>();
    @Min(1)
    private final int sampleSize;

    public ExperimentalGroupBean(int sampleSize, List<VariableLevel> levels) {
      this.sampleSize = sampleSize;
      this.levels.addAll(levels);
    }

    public List<VariableLevel> getLevels() {
      return Collections.unmodifiableList(levels);
    }

    public int getSampleSize() {
      return sampleSize;
    }
  }
}
