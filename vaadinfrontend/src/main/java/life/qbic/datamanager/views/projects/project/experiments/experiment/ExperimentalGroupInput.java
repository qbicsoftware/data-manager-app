package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.dataview.ComboBoxListDataView;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import jakarta.validation.constraints.Min;
import life.qbic.datamanager.views.general.Container;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput.ExperimentalGroupBean;
import life.qbic.projectmanagement.application.ExperimentValueFormatter;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.VariableName;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ExperimentalGroupInput extends CustomField<ExperimentalGroupBean> {

  private static final Comparator<VariableLevel> VARIABLE_LEVEL_COMPARATOR = Comparator
      .<VariableLevel, String>comparing(level -> level.variableName().value())
      .thenComparing(level -> level.experimentalValue().value());
  private static final ItemLabelGenerator<VariableLevel> VARIABLE_LEVEL_ITEM_LABEL_GENERATOR = it -> String.format(
      "%s: %s", it.variableName().value(),
      ExperimentValueFormatter.format(it.experimentalValue()));

  private final MultiSelectComboBox<VariableLevel> variableLevelSelect;
  private final NumberField sampleSizeField;

  private int variableCount = 0;

  private final List<Binder<?>> binders = new ArrayList<>();


  public ExperimentalGroupInput(Collection<VariableLevel> availableLevels) {
    variableLevelSelect = generateVariableLevelSelect();
    sampleSizeField = generateSampleSizeField();

    HorizontalLayout horizontalLayout = new HorizontalLayout(variableLevelSelect, sampleSizeField);
    add(horizontalLayout);
    setLevels(availableLevels);
    horizontalLayout.setWidth(null);
    setWidth(null);
    addValidationForVariableCount();
    variableLevelSelect.addValueChangeListener(
        event -> setInvalid(variableLevelSelect.isInvalid() || sampleSizeField.isInvalid()));
    sampleSizeField.addValueChangeListener(
        event -> setInvalid(variableLevelSelect.isInvalid() || sampleSizeField.isInvalid()));
  }

  private void addValidationForVariableCount() {
    Binder<Container<Set<VariableLevel>>> binder = new Binder<>();
    binder.forField(variableLevelSelect)
        .withValidator(levels -> levels.size() == variableCount,
            "Please select one level for each variable.")
        .bind(Container::get, Container::set);
    binders.add(binder);
  }

  private void validateInput() {
    for (Binder<?> binder : binders) {
      binder.validate();
    }
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
        .ofNullable(sampleSizeField.getValue()).map(Double::intValue)
        .orElse(0);
    return new ExperimentalGroupBean(sampleSize, levels);
  }

  @Override
  protected void setPresentationValue(ExperimentalGroupBean newPresentationValue) {
    variableLevelSelect.setValue(newPresentationValue.levels);
    sampleSizeField.setValue((double) newPresentationValue.sampleSize);
  }

  @Override
  public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
    super.setRequiredIndicatorVisible(requiredIndicatorVisible);
    variableLevelSelect.setRequired(requiredIndicatorVisible);
    variableLevelSelect.setRequiredIndicatorVisible(requiredIndicatorVisible);
    sampleSizeField.setRequiredIndicatorVisible(requiredIndicatorVisible);
  }

  @Override
  public boolean isInvalid() {
    validateInput();
    super.setInvalid(variableLevelSelect.isInvalid() || sampleSizeField.isInvalid());
    return super.isInvalid();
  }

  private MultiSelectComboBox<VariableLevel> generateVariableLevelSelect() {
    MultiSelectComboBox<VariableLevel> selectComboBox = new MultiSelectComboBox<>();
    selectComboBox.setLabel("Condition");
    selectComboBox.addClassName("chip-badge");
    selectComboBox.setAllowCustomValue(false);
    selectComboBox.setItemLabelGenerator(VARIABLE_LEVEL_ITEM_LABEL_GENERATOR);
    overwriteSelectionOfSameVariable(selectComboBox);
    selectComboBox.setWidthFull();
    return selectComboBox;
  }

  private NumberField generateSampleSizeField() {
    NumberField numberField = new NumberField();
    numberField.setLabel("Number of Samples");
    numberField.setStepButtonsVisible(true);
    numberField.setStep(1);
    numberField.setMin(1);
    numberField.setValue(1.0);
    numberField.setWidth(150, Unit.PIXELS);
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
