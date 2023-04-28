package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * <b>AddVariablesDialog</b>
 *
 * <p>Dialog Component to define and add
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable} to an
 * {@link life.qbic.projectmanagement.domain.project.experiment.Experiment}. It's employed within
 * the {@link ExperimentalVariableCard} which handles the actual service logic.
 */

@SpringComponent
@UIScope
public class AddVariablesDialog extends Dialog {

  private final Handler handler;

  private final VerticalLayout dialogueContentLayout = new VerticalLayout();
  private final VerticalLayout experimentalVariableRowsContainerLayout = new VerticalLayout();
  public final List<ExperimentalVariableRowLayout> experimentalVariablesLayoutRows = new ArrayList<>();
  private final HorizontalLayout addExperimentalVariableLayoutRow = new HorizontalLayout();
  public final Button addVariablesButton = new Button("Add");
  public final Button cancelButton = new Button("Cancel");

  public AddVariablesDialog(
      @Autowired ExperimentInformationService experimentInformationService) {
    configureDialogLayout();
    initDialogueContent();
    handler = new Handler(experimentInformationService);
  }

  public void experimentId(ExperimentId experimentId) {
    handler.setExperimentId(experimentId);
  }

  private void configureDialogLayout() {
    addVariablesButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    setHeaderTitle("Experimental Design");
    getFooter().add(cancelButton, addVariablesButton);
  }

  private void initDialogueContent() {
    initDefineExperimentalVariableLayout();
    initDesignVariableTemplate();
    dialogueContentLayout.add(experimentalVariableRowsContainerLayout);
    dialogueContentLayout.add(addExperimentalVariableLayoutRow);
    add(dialogueContentLayout);
  }

  private void initDefineExperimentalVariableLayout() {
    Span experimentalDesignHeader = new Span("Define Experimental Variable");
    experimentalDesignHeader.addClassName("font-bold");
    experimentalVariableRowsContainerLayout.add(experimentalDesignHeader);
    appendEmptyRow();
  }

  private void appendEmptyRow() {
    appendRow(new ExperimentalVariableRowLayout());
  }

  private void appendRow(ExperimentalVariableRowLayout component) {
    component.setCloseListener(it -> removeRow(it.origin()));
    this.experimentalVariablesLayoutRows.add(component);
    experimentalVariableRowsContainerLayout.add(component);
  }

  private void removeRow(ExperimentalVariableRowLayout component) {
    boolean wasRemoved = this.experimentalVariablesLayoutRows.remove(component);
    if (wasRemoved) {
      experimentalVariableRowsContainerLayout.remove(component);
    }
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
    addExperimentalVariableLayoutRow.add(plusIcon, experimentalVariableFieldsLayout);
    addExperimentalVariableLayoutRow.setAlignItems(Alignment.CENTER);
  }

  private class Handler {

    ExperimentInformationService experimentInformationService;
    ExperimentId experimentId;

    private Handler(ExperimentInformationService experimentInformationService) {
      this.experimentInformationService = experimentInformationService;
      closeDialogViaCancelButton();
      addExperimentViaAddButton();
      resetDialogUponClosure();
    }

    public void setExperimentId(ExperimentId experimentId) {
      this.experimentId = experimentId;
    }

    private void closeDialogViaCancelButton() {
      cancelButton.addClickListener(clickEvent -> resetAndClose());
    }

    private void addExperimentViaAddButton() {
      addVariablesButton.addClickListener(event -> {
        dropEmptyRows();
        addExperimentalVariableToExperiment(experimentId);
        closeDialogueIfValid();
      });
    }

    private void dropEmptyRows() {
      experimentalVariablesLayoutRows.removeIf(ExperimentalVariableRowLayout::isEmpty);
    }

    private void closeDialogueIfValid() {
      if (experimentalVariablesLayoutRows.stream()
          .allMatch(ExperimentalVariableRowLayout::isValid)) {
        resetAndClose();
      }
      //ToDo what should happen if invalid information is provided in rows
    }

    private void resetDialogUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> resetAndClose());
    }

    public void reset() {
      experimentalVariablesLayoutRows.clear();
      experimentalVariableRowsContainerLayout.removeAll();
      initDefineExperimentalVariableLayout();
    }

    private void resetAndClose() {
      close();
      reset();
    }

    private void addExperimentalVariableToExperiment(ExperimentId experimentId) {
      for (ExperimentalVariableRowLayout row : experimentalVariablesLayoutRows) {
        if (!row.isEmpty() && row.isValid()) {
          experimentInformationService.addVariableToExperiment(experimentId, row.getVariableName(),
              row.getUnit(), row.getLevels());
        }
      }
    }
  }

  static class ExperimentalVariableRowLayout extends HorizontalLayout {

    private final TextField nameField = new TextField("Experimental Variable");
    private final TextField unitField = new TextField("Unit");
    private final TextArea levelArea = new TextArea("Levels");
    private final Icon deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);
    private Registration clickListener;

    private ExperimentalVariableRowLayout() {
      init();
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

    private record CloseEvent(ExperimentalVariableRowLayout origin) {

    }

    public void setCloseListener(Consumer<CloseEvent> closeListener) {
      if (Objects.nonNull(clickListener)) {
        clickListener.remove();
      }
      clickListener = deleteIcon.addClickListener(it -> closeListener.accept(new CloseEvent(this)));
    }

    private void init() {
      FormLayout experimentalVariableFieldsLayout = new FormLayout();
      experimentalVariableFieldsLayout.add(nameField, unitField, levelArea);
      experimentalVariableFieldsLayout.setResponsiveSteps(new ResponsiveStep("0", 3));
      nameField.setRequired(true);
      levelArea.setRequired(true);
      add(experimentalVariableFieldsLayout, deleteIcon);
      setAlignItems(Alignment.CENTER);
    }

    //We need to make sure that the service is only called if valid input is provided

    public boolean isValid() {
      boolean isNameFieldValid = !nameField.isInvalid() && !nameField.isEmpty();
      boolean isLevelFieldValid = !levelArea.isInvalid() && !levelArea.isEmpty();
      return isNameFieldValid && isLevelFieldValid;
    }

    public boolean isEmpty() {
      return nameField.isEmpty() && unitField.isEmpty() && levelArea.isEmpty();
    }

  }


}
