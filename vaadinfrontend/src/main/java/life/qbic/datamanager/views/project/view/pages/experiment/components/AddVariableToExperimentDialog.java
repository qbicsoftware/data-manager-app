package life.qbic.datamanager.views.project.view.pages.experiment.components;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * <b>AddVariableToExperimentDialog</b>
 *
 * <p>Dialog Component to define and add
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable} to an
 * {@link life.qbic.projectmanagement.domain.project.experiment.Experiment}. It's employed within
 * the {@link ExperimentalVariableCard} which handles the actual service logic.
 */

@SpringComponent
@UIScope
public class AddVariableToExperimentDialog extends Dialog {

  private final Handler handler;
  private final VerticalLayout dialogueContentLayout = new VerticalLayout();
  private final VerticalLayout experimentalVariableRowsContainerLayout = new VerticalLayout();
  public final List<ExperimentalVariableRowLayout> experimentalVariablesLayoutRows = new ArrayList<>();
  private final HorizontalLayout addExperimentalVariableLayoutRow = new HorizontalLayout();
  public final Button addVariablesButton = new Button("Add");
  public final Button cancelButton = new Button("Cancel");

  public AddVariableToExperimentDialog() {
    configureDialogLayout();
    initDialogueContent();
    handler = new Handler();
    handler.handle();
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

    private Handler() {
    }

    private void handle() {
      closeDialogViaCancelButton();
      resetDialogUponClosure();
    }

    private void closeDialogViaCancelButton() {
      cancelButton.addClickListener(clickEvent -> resetandClose());
    }

    private void resetDialogUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> resetandClose());
    }

    public void reset() {
      experimentalVariablesLayoutRows.clear();
      experimentalVariableRowsContainerLayout.removeAll();
      initDefineExperimentalVariableLayout();
    }

    public void resetandClose() {
      close();
      reset();
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

    public List<String> getValues() {
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
      //ToDo updating the list should automatically update the parentLayout (virtualList?)
      add(experimentalVariableFieldsLayout, deleteIcon);
      setAlignItems(Alignment.CENTER);
    }

  }
}
