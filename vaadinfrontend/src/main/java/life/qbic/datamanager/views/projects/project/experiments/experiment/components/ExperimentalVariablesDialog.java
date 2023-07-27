package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import life.qbic.datamanager.views.general.CancelEvent;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;

/**
 * <b>Add Experimental Variables Dialog</b>
 *
 * <p>Component that provides the user with a dialog to add new experimental variables</p>
 *
 * @since 1.0.0
 */
public class ExperimentalVariablesDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 5296014328282974007L;
  private final List<ExperimentalVariableRowLayout> experimentalVariablesLayoutRows = new ArrayList<>();
  private final List<ComponentEventListener<CancelEvent<ExperimentalVariablesDialog>>> cancelEventListeners = new ArrayList<>();
  private final Div dialogueContentLayout = new Div();
  private final Div experimentalVariableRowsContainerLayout = new Div();
  private final Span addExperimentalVariableLayoutRow = new Span();
  private final List<ComponentEventListener<ConfirmEvent<ExperimentalVariablesDialog>>> confirmEventListeners = new ArrayList<>();
  private final MODE mode;

  public ExperimentalVariablesDialog() {
    this(false);
  }

  private ExperimentalVariablesDialog(boolean editMode) {
    super();
    mode = editMode ? MODE.EDIT : MODE.ADD;
    setConfirmButtonLabel(confirmActionLabel());
    setCancelButtonLabel("Cancel");
    addClassName("experiment-variable-dialog");
    layoutComponent();
    initDialogueContent();
    configureComponent();
  }

  /**
   * Creates a new dialog prefilled with experimental variables.
   *
   * @param experimentalVariables the variables to fill the dialog with
   * @return a new instance of the dialog
   */
  public static ExperimentalVariablesDialog prefilled(
      List<ExperimentalVariable> experimentalVariables) {
    return editDialog(experimentalVariables);
  }

  private static ExperimentalVariablesDialog editDialog(
      final Collection<? extends ExperimentalVariable> experimentalVariables) {
    final ExperimentalVariablesDialog experimentalVariablesDialog = new ExperimentalVariablesDialog(
        true);
    final var rowLayouts = convertVariables(experimentalVariables);
    rowLayouts.forEach(experimentalVariablesDialog::prefill);
    return experimentalVariablesDialog;
  }

  private void prefill(final ExperimentalVariableRowLayout rowLayout) {
    appendRow(rowLayout);
  }

  private static List<ExperimentalVariableRowLayout> convertVariables(
      final Collection<? extends ExperimentalVariable> variables) {
    return variables.stream()
        .map(ExperimentalVariableRowLayout::from)
        .toList();
  }

  private String confirmActionLabel() {
    return isEditing() ? "Save" : "Add";
  }

  private void configureComponent() {
    resetDialogUponClosure();
    configureCancelling();
    configureConfirmation();
  }

  private void configureConfirmation() {
    this.confirmButton.addClickListener(event -> fireConfirmEvent());
  }

  private void configureCancelling() {
    this.cancelButton.addClickListener(cancelListener -> fireCancelEvent());
  }

  private void fireConfirmEvent() {
    this.confirmEventListeners.forEach(
        listener -> listener.onComponentEvent(new ConfirmEvent<>(this, true)));
  }

  private void fireCancelEvent() {
    this.cancelEventListeners.forEach(
        listener -> listener.onComponentEvent(new CancelEvent<>(this, true)));
  }

  private void resetDialogUponClosure() {
    // Calls the reset method for all possible closure methods of the dialogue window:
    addDialogCloseActionListener(closeActionEvent -> resetAndClose());
  }

  /**
   * Adds a listener for {@link CancelEvent}s
   *
   * @param listener the listener to add
   */
  public void addConfirmEventListener(
      final ComponentEventListener<ConfirmEvent<ExperimentalVariablesDialog>> listener) {
    this.confirmEventListeners.add(listener);
  }

  /**
   * Adds a listener for {@link ConfirmEvent}s
   *
   * @param listener the listener to add
   */
  public void addCancelEventListener(
      final ComponentEventListener<CancelEvent<ExperimentalVariablesDialog>> listener) {
    this.cancelEventListeners.add(listener);
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
    this.experimentalVariablesLayoutRows.clear();
    this.experimentalVariableRowsContainerLayout.removeAll();
    initDefineExperimentalVariableLayout();
  }

  private void initDefineExperimentalVariableLayout() {
    final Span experimentalDesignHeader = new Span("Define Experimental Variable");
    experimentalDesignHeader.addClassName("header");
    this.experimentalVariableRowsContainerLayout.add(experimentalDesignHeader);
    if (isAdding()) {
      appendEmptyRow();
    }
  }

  private boolean isAdding() {
    return MODE.ADD == this.mode;
  }

  private boolean isEditing() {
    return MODE.EDIT == this.mode;
  }

  private void appendEmptyRow() {
    appendRow(new ExperimentalVariableRowLayout());
  }

  private void appendRow(final ExperimentalVariableRowLayout experimentalVariableRowLayout) {
    experimentalVariableRowLayout.setCloseListener(it -> removeRow(it.origin()));
    this.experimentalVariablesLayoutRows.add(experimentalVariableRowLayout);
    this.experimentalVariableRowsContainerLayout.add(experimentalVariableRowLayout);
  }

  private void removeRow(final ExperimentalVariableRowLayout experimentalVariableRowLayout) {
    final boolean wasRemoved = this.experimentalVariablesLayoutRows.remove(
        experimentalVariableRowLayout);
    if (wasRemoved) {
      this.experimentalVariableRowsContainerLayout.remove(experimentalVariableRowLayout);
    }
  }

  private void layoutComponent() {
    setHeaderTitle("Experimental Design");
    final DialogFooter footer = getFooter();
    footer.add(this.cancelButton, this.confirmButton);
  }

  private void initDialogueContent() {
    initDefineExperimentalVariableLayout();
    initDesignVariableTemplate();
    this.dialogueContentLayout.addClassName("content");
    this.experimentalVariableRowsContainerLayout.addClassName("variables");
    this.dialogueContentLayout.add(this.experimentalVariableRowsContainerLayout);
    this.dialogueContentLayout.add(this.addExperimentalVariableLayoutRow);
    add(this.dialogueContentLayout);
  }

  private void initDesignVariableTemplate() {
    final TextField experimentalVariableField = new TextField("Experimental Variable");
    final TextField unitField = new TextField("Unit");
    final TextArea levelField = new TextArea("Levels");
    experimentalVariableField.setEnabled(false);
    unitField.setEnabled(false);
    levelField.setEnabled(false);
    final Icon plusIcon = new Icon(VaadinIcon.PLUS);
    plusIcon.addClickListener(iconClickEvent -> appendEmptyRow());
    final FormLayout experimentalVariableFieldsLayout = new FormLayout();
    experimentalVariableFieldsLayout.add(experimentalVariableField, unitField, levelField);
    experimentalVariableFieldsLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
    this.addExperimentalVariableLayoutRow.addClassName("row");
    this.addExperimentalVariableLayoutRow.add(plusIcon, experimentalVariableFieldsLayout);
  }

  /**
   * @return a list of experimental variables defined by this dialog
   */
  public List<ExperimentalVariableContent> definedVariables() {
    return this.experimentalVariablesLayoutRows.stream()
        .map(ExperimentalVariableContent::from)
        .toList();
  }

  private enum MODE {
    ADD, EDIT
  }


}
