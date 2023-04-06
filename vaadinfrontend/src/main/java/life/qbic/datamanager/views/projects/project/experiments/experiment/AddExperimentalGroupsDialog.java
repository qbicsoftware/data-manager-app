package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.VariableName;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class AddExperimentalGroupsDialog extends Dialog {

  final ExperimentInformationService experimentInformationService;
  private final VerticalLayout rows;
  private final HorizontalLayout templateRow;

  private static class ExperimentalGroupLayout extends HorizontalLayout {

    ConditionComboBox variableLevels = new ConditionComboBox("Condition");
    NumberField sampleSize = new NumberField("Number of Samples");
    Button removeRowButton = new Button(VaadinIcon.CLOSE_SMALL.create());

    public ExperimentalGroupLayout(Collection<VariableLevel> levels) {
      this(true, levels);
    }

    public ExperimentalGroupLayout(boolean removeButtonVisible,
        Collection<VariableLevel> levels) {
      requireNonNull(levels, "levels must not be null");
      setAlignItems(Alignment.BASELINE);
      sampleSize.setRequiredIndicatorVisible(true);
      sampleSize.setStepButtonsVisible(true);
      sampleSize.setStep(1);
      sampleSize.setMin(1);
      sampleSize.setWidthFull();
      Label sampleSizeErrorLabel = new Label();
      sampleSizeErrorLabel.getStyle().set("color", "var(--lumo-error-text-color)");
      sampleSizeErrorLabel.setVisible(false);
      sampleSize.addValueChangeListener(event -> {
        ValidationResult validationResult = event.getSource().getDefaultValidator()
            .apply(event.getValue(), new ValueContext());
        if (validationResult.isError()) {
          sampleSizeErrorLabel.setText(validationResult.getErrorMessage());
          sampleSizeErrorLabel.setVisible(true);
        } else {
          sampleSizeErrorLabel.setVisible(false);
        }
      });
      sampleSize.setHelperComponent(sampleSizeErrorLabel);
      variableLevels.addClassName("chip-badge");
      variableLevels.setRequiredIndicatorVisible(true);
      variableLevels.setItems(levels);
      removeRowButton.setIconAfterText(true);
      removeRowButton.addClickListener(it -> this.getElement().removeFromParent());
      removeRowButton.setVisible(removeButtonVisible);
      removeRowButton.setWidthFull();
      add(variableLevels, sampleSize, removeRowButton);
      setWidthFull();
    }

  }

  record ExperimentalGroupInformation(Set<VariableLevel> levels, int sampleSize) {

  }


  public AddExperimentalGroupsDialog(ExperimentInformationService experimentInformationService) {
    this.experimentInformationService = experimentInformationService;
    setHeaderTitle("Experimental Groups");
    rows = new VerticalLayout();
    rows.setPadding(false);
    rows.setSpacing(false);
    templateRow = new HorizontalLayout();
    templateRow.setAlignItems(Alignment.BASELINE);
    add(rows);
    add(templateRow);
    setCloseOnEsc(false);
    setCloseOnOutsideClick(false);
  }

  @Override
  public void close() {
    super.close();
    reset();
  }

  private void reset() {
    rows.removeAll();
    templateRow.removeAll();
  }

  @Override
  public void open() {
    //TODO fetch data from service
    Collection<VariableLevel> levels = List.of(
        VariableLevel.create(VariableName.create("color"), ExperimentalValue.create("red")),
        VariableLevel.create(VariableName.create("color"), ExperimentalValue.create("blue")),
        VariableLevel.create(VariableName.create("color"), ExperimentalValue.create("green")),
        VariableLevel.create(VariableName.create("color"), ExperimentalValue.create("yellow")),
        VariableLevel.create(VariableName.create("width"), ExperimentalValue.create("5", "cm")),
        VariableLevel.create(VariableName.create("width"), ExperimentalValue.create("10", "cm")),
        VariableLevel.create(VariableName.create("height"), ExperimentalValue.create("20", "cm")),
        VariableLevel.create(VariableName.create("height"), ExperimentalValue.create("500", "cm")));

    Button addRow = new Button(VaadinIcon.PLUS.create());
    ExperimentalGroupLayout templateElement = new ExperimentalGroupLayout(false, levels);
    templateElement.setEnabled(false);
    templateRow.add(addRow, templateElement);
    addRow.addClickListener(it -> rows.add(new ExperimentalGroupLayout(levels)));
    rows.add(new ExperimentalGroupLayout(levels));
    super.open();
  }

  public List<ExperimentalGroupInformation> getContent() {
    return rows.getChildren()
        .filter(it -> it instanceof ExperimentalGroupLayout).map(it -> (ExperimentalGroupLayout) it)
        .map(it ->
            new ExperimentalGroupInformation(it.variableLevels.getSelectedItems(),
                Optional.ofNullable(it.sampleSize.getValue())
                    .orElse(0.0).intValue()))
        .toList();
  }
}
