package life.qbic.datamanager.views.project.experiment;

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
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.project.ProjectId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@SpringComponent
@UIScope
public class ExperimentCreationDialog extends Dialog {

  private final Handler handler;
  private final VerticalLayout dialogueContentLayout = new VerticalLayout();
  private final HorizontalLayout navHeaderLayout = new HorizontalLayout();
  private final VerticalLayout experimentalVariableRowsContainerLayout = new VerticalLayout();
  private final List<HorizontalLayout> experimentalVariablesLayoutRows = new ArrayList<>();
  private final HorizontalLayout addExperimentalVariableLayoutRow = new HorizontalLayout();
  private final Button nextButton = new Button("Next");
  private final Button cancelButton = new Button("Cancel");

  public ExperimentCreationDialog() {
    configureDialogLayout();
    initDialogueContent();
    handler = new Handler();
    handler.handle();
  }

  private void configureDialogLayout() {
    nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    setHeaderTitle("Experimental Design");
    getFooter().add(cancelButton, nextButton);
  }

  private void initDialogueContent() {
    initNavHeader();
    initDefineExperimentalVariableLayout();
    initDesignVariableTemplate();
    dialogueContentLayout.add(navHeaderLayout);
    dialogueContentLayout.add(experimentalVariableRowsContainerLayout);
    dialogueContentLayout.add(addExperimentalVariableLayoutRow);
    add(dialogueContentLayout);
  }

  private void initNavHeader() {
    //ToDo Init Dialogue Navbar content
  }

  private void initDefineExperimentalVariableLayout() {
    Span experimentalDesignHeader = new Span("Define Experimental Variable");
    experimentalDesignHeader.addClassName("font-bold");
    experimentalVariableRowsContainerLayout.add(experimentalDesignHeader);
    experimentalVariablesLayoutRows.add(createExperimentalVariableLayout());
    experimentalVariablesLayoutRows.add(createExperimentalVariableLayout());
    experimentalVariablesLayoutRows.forEach(experimentalVariableRowsContainerLayout::add);
  }

  private void initDesignVariableTemplate() {
    TextField experimentalVariableField = new TextField("Experimental Variable");
    TextField unitField = new TextField("Unit");
    TextArea levelField = new TextArea("Levels");
    experimentalVariableField.setEnabled(false);
    unitField.setEnabled(false);
    levelField.setEnabled(false);
    Icon plusIcon = new Icon(VaadinIcon.PLUS);
    //ToDo updating the list should automatically update the parentLayout (observable list?)
    plusIcon.addClickListener(iconClickEvent -> {
      HorizontalLayout generatedExperimentalVariableLayout = createExperimentalVariableLayout();
      experimentalVariablesLayoutRows.add(generatedExperimentalVariableLayout);
      experimentalVariableRowsContainerLayout.add(generatedExperimentalVariableLayout);
    });
    FormLayout experimentalVariableFieldsLayout = new FormLayout();
    experimentalVariableFieldsLayout.add(experimentalVariableField, unitField, levelField);
    experimentalVariableFieldsLayout.setResponsiveSteps(new ResponsiveStep("0", 3));
    addExperimentalVariableLayoutRow.add(plusIcon, experimentalVariableFieldsLayout);
    addExperimentalVariableLayoutRow.setAlignItems(Alignment.CENTER);
  }

  private HorizontalLayout createExperimentalVariableLayout() {
    TextField experimentalVariableField = new TextField("Experimental Variable");
    TextField unitField = new TextField("Unit");
    TextArea levelField = new TextArea("Levels");
    FormLayout experimentalVariableFieldsLayout = new FormLayout();
    experimentalVariableFieldsLayout.add(experimentalVariableField, unitField, levelField);
    experimentalVariableFieldsLayout.setResponsiveSteps(new ResponsiveStep("0", 3));
    Icon removeIcon = new Icon(VaadinIcon.CLOSE_SMALL);
    //ToDo updating the list should automatically update the parentLayout (observable list?)
    HorizontalLayout rowLayout = new HorizontalLayout();
    removeIcon.addClickListener(iconClickEvent -> {
      experimentalVariablesLayoutRows.remove(rowLayout);
      experimentalVariableRowsContainerLayout.remove(rowLayout);
    });
    rowLayout.add(experimentalVariableFieldsLayout, removeIcon);
    rowLayout.setAlignItems(Alignment.CENTER);
    return rowLayout;
  }

  private class Handler {

    Optional<ProjectId> projectId;

    private void handle() {
      this.projectId = Optional.empty();
      closeDialogListener();
      resetDialogUponClosure();
    }

    private void closeDialogListener() {
      cancelButton.addClickListener(clickEvent -> closeAndReset());
    }

    private void resetDialogUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> closeAndReset());
    }

    public void reset() {
      experimentalVariablesLayoutRows.clear();
      experimentalVariableRowsContainerLayout.removeAll();
      initDefineExperimentalVariableLayout();
    }

    public void closeAndReset() {
      close();
      reset();
    }
  }
}
