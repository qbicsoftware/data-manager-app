package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Collections.emptySet;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox.ItemFilter;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.dataview.ComboBoxListDataView;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.provider.ListDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.ExperimentValueFormatter;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ConditionComboBox extends MultiSelectComboBox<VariableLevel> implements
    HasValidators<Set<VariableLevel>> {

  private final List<Validator<Set<VariableLevel>>> validators = new ArrayList<>();


  private static final ItemFilter<VariableLevel> VARIABLE_LEVEL_ITEM_FILTER = (level, filterString) ->
      level.variableName().value().toLowerCase().contains(filterString.toLowerCase())
          || ExperimentValueFormatter.format(level.experimentalValue()).toLowerCase()
          .contains(filterString.toLowerCase());
  private static final ItemLabelGenerator<VariableLevel> VARIABLE_LEVEL_ITEM_LABEL_GENERATOR = it -> String.format(
      "%s: %s", it.variableName().value(),
      ExperimentValueFormatter.format(it.experimentalValue()));

  public ConditionComboBox(String label) {
    super(label);
    setMinWidth(20, Unit.REM);
    addClassName("chip-badge");
    setAllowCustomValue(false);
    setItemLabelGenerator(VARIABLE_LEVEL_ITEM_LABEL_GENERATOR);
    overwritePreviousLevelOfVariable();
    addValidator(getDefaultValidator());
    addValidator((value, context) -> {
      if (isRequired() && isEmpty()) {
        return ValidationResult.error("Please make a selection");
      } else {
        return ValidationResult.ok();
      }
    });
    addValidator((value, context) -> {
      if (value.stream().map(VariableLevel::variableName).distinct().count()
          == distinctVariableCount()) {
        return ValidationResult.ok();
      } else {
        return ValidationResult.error("A condition must contain a level for every variable.");
      }
    });
  }


  @Override
  public ComboBoxListDataView<VariableLevel> setItems(Collection<VariableLevel> variableLevels) {
    return super.setItems(VARIABLE_LEVEL_ITEM_FILTER, variableLevels);
  }

  @Override
  public ComboBoxListDataView<VariableLevel> setItems(VariableLevel... variableLevels) {
    return super.setItems(VARIABLE_LEVEL_ITEM_FILTER, variableLevels);
  }

  @Override
  public ComboBoxListDataView<VariableLevel> setItems(
      ListDataProvider<VariableLevel> dataProvider) {
    return super.setItems(VARIABLE_LEVEL_ITEM_FILTER, dataProvider);
  }

  private long distinctVariableCount() {
    return getListDataView().getItems().map(VariableLevel::variableName).distinct().count();
  }

  private void overwritePreviousLevelOfVariable() {
    addSelectionListener(event -> updateSelection(emptySet(),
        getSelectedItems().stream()
            .filter(item -> !event.getAddedSelection().contains(item))
            .filter(item -> event.getAddedSelection().stream()
                .map(VariableLevel::variableName)
                .anyMatch(variableName -> variableName.equals(item.variableName())))
            .collect(Collectors.toSet())));
  }

  @Override
  protected void validate() {
    applyValidators();
  }

  @Override
  public void addValidator(Validator<Set<VariableLevel>> validator) {
    validators.add(validator);
  }

  @Override
  public void removeValidator(Validator<Set<VariableLevel>> validator) {
    validators.remove(validator);
  }

  @Override
  public ValidationResult applyValidators() {
    Set<VariableLevel> value = getValue();
    List<ValidationResult> results = validators.stream()
        .map(it -> it.apply(value, new ValueContext()))
        .toList();
    results.stream().filter(ValidationResult::isError).findFirst().ifPresentOrElse(
        error -> {
          setErrorMessage(error.getErrorMessage());
          setInvalid(true);
        },
        () -> setInvalid(false)
    );
    return results.stream().filter(ValidationResult::isError).findFirst()
        .orElse(ValidationResult.ok());
  }
}
