package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import life.qbic.datamanager.views.general.CancelEvent;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * <b>Add Experimental Variables Dialog</b>
 *
 * <p>Component that provides the user with a dialog to add new experimental variables</p>
 *
 * @since 1.0.0
 */
public class AddExperimentalVariablesDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 5296014328282974007L;
  private final List<AddExperimentalVariablesDialog.ExperimentalVariableRowLayout> experimentalVariablesLayoutRows = new ArrayList<>();
  private final List<ComponentEventListener<CancelEvent<AddExperimentalVariablesDialog>>> listenersCancellation = new ArrayList<>();
  private final Div dialogueContentLayout = new Div();
  private final Div experimentalVariableRowsContainerLayout = new Div();
  private final Span addExperimentalVariableLayoutRow = new Span();
  private final List<ComponentEventListener<ConfirmEvent<AddExperimentalVariablesDialog>>> listenersConfirmation = new ArrayList<>();

  public AddExperimentalVariablesDialog() {
    super();
    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");
    addClassName("experiment-variable-dialog");
    layoutComponent();
    initDialogueContent();
    configureComponent();
  }

  private void configureComponent() {
    resetDialogUponClosure();
    configureCancelling();
    configureConfirmation();
  }

  private void configureConfirmation() {
    ConfirmEvent<AddExperimentalVariablesDialog> confirmEvent = new ConfirmEvent<>(this, true);
    confirmButton.addClickListener(confirmListener -> listenersConfirmation.forEach(
        listener -> listener.onComponentEvent(confirmEvent)));
  }

  private void configureCancelling() {
    CancelEvent<AddExperimentalVariablesDialog> cancelEvent = new CancelEvent<>(this, true);
    cancelButton.addClickListener(cancelListener -> listenersCancellation.forEach(
        listener -> listener.onComponentEvent(cancelEvent)));
  }

  private void resetDialogUponClosure() {
    // Calls the reset method for all possible closure methods of the dialogue window:
    addDialogCloseActionListener(closeActionEvent -> resetAndClose());
  }

  public void subscribeToConfirmEvent(
      ComponentEventListener<ConfirmEvent<AddExperimentalVariablesDialog>> listener) {
    listenersConfirmation.add(listener);
  }

  public void subscribeToCancelEvent(
      ComponentEventListener<CancelEvent<AddExperimentalVariablesDialog>> listener) {
    listenersCancellation.add(listener);
  }

  /**
   * Closes the dialog, all entered information is lost.
   */
  @Override
  public void close() {
    resetAndClose();
  }

  private void resetAndClose() {
    reset();
    super.close();
  }

  private void reset() {
    experimentalVariablesLayoutRows.clear();
    experimentalVariableRowsContainerLayout.removeAll();
    initDefineExperimentalVariableLayout();
  }

  private void initDefineExperimentalVariableLayout() {
    Span experimentalDesignHeader = new Span("Define Experimental Variable");
    experimentalDesignHeader.addClassName("header");
    experimentalVariableRowsContainerLayout.add(experimentalDesignHeader);
    appendEmptyRow();
  }

  private void appendEmptyRow() {
    appendRow(new AddExperimentalVariablesDialog.ExperimentalVariableRowLayout());
  }

  private void appendRow(AddExperimentalVariablesDialog.ExperimentalVariableRowLayout component) {
    component.setCloseListener(it -> removeRow(it.origin()));
    this.experimentalVariablesLayoutRows.add(component);
    experimentalVariableRowsContainerLayout.add(component);
  }

  private void removeRow(AddExperimentalVariablesDialog.ExperimentalVariableRowLayout component) {
    boolean wasRemoved = this.experimentalVariablesLayoutRows.remove(component);
    if (wasRemoved) {
      experimentalVariableRowsContainerLayout.remove(component);
    }
  }

  private void layoutComponent() {
    setHeaderTitle("Experimental Design");
    getFooter().add(cancelButton, confirmButton);
  }

  private void initDialogueContent() {
    initDefineExperimentalVariableLayout();
    initDesignVariableTemplate();
    dialogueContentLayout.addClassName("content");
    experimentalVariableRowsContainerLayout.addClassName("variables");
    dialogueContentLayout.add(experimentalVariableRowsContainerLayout);
    dialogueContentLayout.add(addExperimentalVariableLayoutRow);
    add(dialogueContentLayout);
  }

  private void initDesignVariableTemplate() {
    TextField experimentalVariableField = new TextField("Experimental Variable");
    TextField unitField = new TextField("Unit");
    TextArea levelField = new TextArea("Levels");
    experimentalVariableField.setEnabled(false);
    unitField.setEnabled(false);
    levelField.setEnabled(false);
    Icon plusIcon = new Icon(VaadinIcon.PLUS);
    plusIcon.addClickListener(iconClickEvent -> appendEmptyRow());
    FormLayout experimentalVariableFieldsLayout = new FormLayout();
    experimentalVariableFieldsLayout.add(experimentalVariableField, unitField, levelField);
    experimentalVariableFieldsLayout.setResponsiveSteps(new ResponsiveStep("0", 3));
    addExperimentalVariableLayoutRow.addClassName("row");
    addExperimentalVariableLayoutRow.add(plusIcon, experimentalVariableFieldsLayout);
  }

  private void dropEmptyRows() {
    experimentalVariablesLayoutRows.removeIf(
        AddExperimentalVariablesDialog.ExperimentalVariableRowLayout::isEmpty);
  }

  private void closeDialogueIfValid() {
    if (experimentalVariablesLayoutRows.stream()
        .allMatch(AddExperimentalVariablesDialog.ExperimentalVariableRowLayout::isValid)) {
      resetAndClose();
    }
    //ToDo what should happen if invalid information is provided in rows
  }

  public List<ExperimentalVariableContent> definedVariables() {
    return experimentalVariablesLayoutRows.stream().map(
            experimentalVariableRowLayout -> new ExperimentalVariableContent(
                experimentalVariableRowLayout.getVariableName(),
                experimentalVariableRowLayout.getUnit(), experimentalVariableRowLayout.getLevels()))
        .toList();
  }
  public record ExperimentalVariableContent(String name, String unit, List<String> levels) {

  }


  static class ExperimentalVariableRowLayout extends Span {

    private Registration clickListener;
    @Serial
    private static final long serialVersionUID = -1126299161780107501L;
    private final TextField nameField = new TextField("Experimental Variable");
    private final TextField unitField = new TextField("Unit");
    private final TextArea levelArea = new TextArea("Levels");
    private final Icon deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);

    private ExperimentalVariableRowLayout() {
      init();
    }

    private void init() {
      addClassName("row");
      FormLayout experimentalVariableFieldsLayout = new FormLayout();
      experimentalVariableFieldsLayout.add(nameField, unitField, levelArea);
      experimentalVariableFieldsLayout.setResponsiveSteps(new ResponsiveStep("0", 3));
      nameField.setRequired(true);
      levelArea.setRequired(true);
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
        Consumer<AddExperimentalVariablesDialog.ExperimentalVariableRowLayout.CloseEvent> closeListener) {
      if (Objects.nonNull(clickListener)) {
        clickListener.remove();
      }
      clickListener = deleteIcon.addClickListener(it -> closeListener.accept(
          new AddExperimentalVariablesDialog.ExperimentalVariableRowLayout.CloseEvent(this)));
    }

    public boolean isValid() {
      boolean isNameFieldValid = !nameField.isInvalid() && !nameField.isEmpty();
      boolean isLevelFieldValid = !levelArea.isInvalid() && !levelArea.isEmpty();
      return isNameFieldValid && isLevelFieldValid;
    }

    //We need to make sure that the service is only called if valid input is provided

    public boolean isEmpty() {
      return nameField.isEmpty() && unitField.isEmpty() && levelArea.isEmpty();
    }

    private record CloseEvent(AddExperimentalVariablesDialog.ExperimentalVariableRowLayout origin) {

    }

  }
}
