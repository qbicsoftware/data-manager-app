package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable;

/**
 * A layout containing rows for experimental variable input
 */
final class ExperimentalVariableRowLayout extends Span {

  @Serial
  private static final long serialVersionUID = -1126299161780107501L;
  private final TextField nameField = new TextField("Experimental Variable");
  private final TextField unitField = new TextField("Unit");
  private final TextArea levelArea = new TextArea("Levels");
  private final Icon deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);
  private Registration clickListener;

  ExperimentalVariableRowLayout() {
    init();
  }

  static ExperimentalVariableRowLayout from(
      final ExperimentalVariable experimentalVariable) {
    final ExperimentalVariableRowLayout rowLayout = new ExperimentalVariableRowLayout();
    rowLayout.nameField.setValue(experimentalVariable.name().value());
    rowLayout.unitField.setValue(
        experimentalVariable.levels().get(0).experimentalValue().unit().orElse(""));
    rowLayout.levelArea.setValue(
        experimentalVariable.levels().stream().map(it -> it.experimentalValue().value())
            .collect(Collectors.joining("\n")));
    return rowLayout;
  }

  private void init() {
    addClassName("row");
    FormLayout experimentalVariableFieldsLayout = new FormLayout();
    experimentalVariableFieldsLayout.add(nameField, unitField, levelArea);
    experimentalVariableFieldsLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
    nameField.setRequired(true);
    nameField.setPlaceholder("e.g. age");
    unitField.setPlaceholder("e.g. years");
    levelArea.setRequired(true);
    levelArea.setHelperText(
        "Please enter each level on a new line. Comma separated values are treated as a single level.");
    levelArea.setPlaceholder("""
        32
        42
        68
        """);
    add(experimentalVariableFieldsLayout, deleteIcon);
  }

  public String getVariableName() {
    return nameField.getValue();
  }

  public String getUnit() {
    return unitField.getValue();
  }

  public List<String> getLevels() {
    return levelArea.getValue().lines().filter(it -> !it.isBlank()).toList();
  }

  public void setCloseListener(
      Consumer<CloseEvent> closeListener) {
    if (Objects.nonNull(clickListener)) {
      clickListener.remove();
    }
    clickListener = deleteIcon.addClickListener(it -> closeListener.accept(
        new CloseEvent(this)));
  }

  public boolean isValid() {
    boolean isNameFieldValid = !nameField.isInvalid() && !nameField.isEmpty();
    boolean isLevelFieldValid = !levelArea.isInvalid() && !levelArea.isEmpty();
    return isNameFieldValid && isLevelFieldValid;
  }

  public boolean isEmpty() {
    return nameField.isEmpty() && unitField.isEmpty() && levelArea.isEmpty();
  }

  public record CloseEvent(ExperimentalVariableRowLayout origin) {

  }

}
