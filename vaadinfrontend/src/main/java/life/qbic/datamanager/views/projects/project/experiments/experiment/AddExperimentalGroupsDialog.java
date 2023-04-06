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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class AddExperimentalGroupsDialog extends Dialog {

  private final VerticalLayout rows;
  private final HorizontalLayout templateRow;
  private Collection<VariableLevel> levels;

  private static class ExperimentalGroupLayout extends HorizontalLayout {

    private final ConditionComboBox variableLevelsInput = new ConditionComboBox("Condition");
    private final NumberField sampleSize = new NumberField("Number of Samples");


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
      variableLevelsInput.addClassName("chip-badge");
      variableLevelsInput.setRequiredIndicatorVisible(true);
      variableLevelsInput.setItems(levels);
      Button removeRowButton = new Button(VaadinIcon.CLOSE_SMALL.create());
      removeRowButton.setIconAfterText(true);
      removeRowButton.addClickListener(it -> this.getElement().removeFromParent());
      removeRowButton.setVisible(removeButtonVisible);
      removeRowButton.setWidthFull();
      add(variableLevelsInput, sampleSize, removeRowButton);
      setWidthFull();
    }

  }

  record ExperimentalGroupInformation(Set<VariableLevel> levels, int sampleSize) {

  }


  public AddExperimentalGroupsDialog() {
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
    levels = Collections.emptySet();
  }

  public boolean setLevels(Collection<VariableLevel> levels) {
    if (isOpened()) {
      return false;
    }
    this.levels = levels;
    return true;
  }

  private void reset() {
    rows.removeAll();
    templateRow.removeAll();
  }

  @Override
  public void open() {
    reset();
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
            new ExperimentalGroupInformation(it.variableLevelsInput.getSelectedItems(),
                Optional.ofNullable(it.sampleSize.getValue())
                    .orElse(0.0).intValue()))
        .toList();
  }
}
