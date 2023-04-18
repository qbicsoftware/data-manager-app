package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

  private final FormLayout rows;
  private final HorizontalLayout templateRow;
  private Collection<VariableLevel> levels;

  private final Span errorTextDisplay;

  private final List<Binder<?>> binders = new ArrayList<>();

  private final List<ExperimentalGroupInput> experimentalGroupInputs = new ArrayList<>();

  record ExperimentalGroupInformation(Set<VariableLevel> levels, int sampleSize) {
  }


  public AddExperimentalGroupsDialog() {
    setHeaderTitle("Experimental Groups");
    errorTextDisplay = new Span("");
    errorTextDisplay.getStyle().set("color", "red");
    add(errorTextDisplay);
    rows = new FormLayout();
    rows.setWidthFull();
    rows.setResponsiveSteps(new ResponsiveStep("0", 1));
    templateRow = new HorizontalLayout();
    templateRow.setAlignItems(Alignment.BASELINE);
    add(rows);
    add(templateRow);
    setCloseOnEsc(false);
    setCloseOnOutsideClick(false);
    levels = Collections.emptySet();
  }

  public void setLevels(Collection<VariableLevel> levels) {
    if (isOpened()) {
      return;
    }
    this.levels = levels;
  }

  private void reset() {
    rows.removeAll();
    templateRow.removeAll();
  }

  @Override
  public void open() {
    reset();
    Button addRow = new Button(VaadinIcon.PLUS.create());
    ExperimentalGroupInput templateElement = new ExperimentalGroupInput(levels);
    templateElement.setEnabled(false);
    templateRow.add(addRow, templateElement);
    addRow.addClickListener(it -> addExperimentalGroupRow());
    addExperimentalGroupRow();
    templateElement.setWidthFull();
    setWidthFull();
    super.open();
  }

  private void addExperimentalGroupRow() {
    ExperimentalGroupInput inputComponent = new ExperimentalGroupInput(levels);
    Button removeRowButton = new Button(VaadinIcon.CLOSE_SMALL.create());
    inputComponent.setWidthFull();
    HorizontalLayout row = new HorizontalLayout();
    row.add(inputComponent, removeRowButton);
    row.setPadding(false);
    row.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
    row.setWidthFull();
    removeRowButton.addClickListener(it -> row.getElement().removeFromParent());
    row.addDetachListener(it -> experimentalGroupInputs.remove(inputComponent));
    row.addAttachListener(it -> experimentalGroupInputs.add(inputComponent));
    rows.add(row);
  }

  public ExperimentalGroupInput[] getInputFields() {
    return experimentalGroupInputs.toArray(ExperimentalGroupInput[]::new);
  }

  public boolean isInputValid() {
    boolean experimentalGroupInputValid = experimentalGroupInputs.stream()
        .noneMatch(CustomField::isInvalid);
    //TODO fail validation for duplicate conditions
    //TODO fail validation
    return experimentalGroupInputValid;
  }

}
