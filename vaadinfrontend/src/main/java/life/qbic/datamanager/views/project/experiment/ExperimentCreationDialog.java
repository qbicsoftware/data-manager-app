package life.qbic.datamanager.views.project.experiment;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
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

  private final VerticalLayout dialogueContentLayout = new VerticalLayout();
  private final HorizontalLayout navHeaderLayout = new HorizontalLayout();
  private final VerticalLayout defineExperimentalVariableLayout = new VerticalLayout();
  private final List<FormLayout> experimentalVariablesLayouts = new ArrayList<>();
  private final FormLayout designVariableTemplate = new FormLayout();
  private final Button nextButton = new Button("Next");
  private final Button cancelButton = new Button("Cancel");

  public ExperimentCreationDialog() {
    //ToDo Handler should be moved to dedicated ExperimentalDesignPage and Component similar to ProjectOverviewComponent
    configureDialogLayout();
    initDialogueContent();
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
    dialogueContentLayout.add(defineExperimentalVariableLayout);
    dialogueContentLayout.add(designVariableTemplate);
    add(dialogueContentLayout);
  }

  private void initNavHeader() {
    //ToDo Init Dialogue Navbar content
  }

  private void initDefineExperimentalVariableLayout() {
    Span experimentalDesignHeader = new Span("Define Experimental Variable");
    experimentalDesignHeader.addClassName("font-bold");
    defineExperimentalVariableLayout.add(experimentalDesignHeader);
    experimentalVariablesLayouts.add(createExperimentalVariableLayout());
    experimentalVariablesLayouts.add(createExperimentalVariableLayout());
    experimentalVariablesLayouts.forEach(defineExperimentalVariableLayout::add);
  }

  private void initDesignVariableTemplate() {
    Icon plusIcon = new Icon(VaadinIcon.PLUS);
    ComboBox<?> experimentalVariableCombobox = new ComboBox<>("Experimental Variable");
    ComboBox<?> unitCombobox = new ComboBox<>("Unit");
    ComboBox<?> levelCombobox = new ComboBox<>("Levels");
    experimentalVariableCombobox.setEnabled(false);
    unitCombobox.setEnabled(false);
    levelCombobox.setEnabled(false);
    designVariableTemplate.setResponsiveSteps(new ResponsiveStep("0", 4));
    designVariableTemplate.add(plusIcon, experimentalVariableCombobox, unitCombobox, levelCombobox);
  }

  private FormLayout createExperimentalVariableLayout() {
    ComboBox<?> experimentalVariableCombobox = new ComboBox<>("Experimental Variable");
    ComboBox<?> unitCombobox = new ComboBox<>("Unit");
    ComboBox<?> levelCombobox = new ComboBox<>("Levels");
    Icon removeIcon = new Icon(VaadinIcon.CLOSE_SMALL);
    experimentalVariableCombobox.setAllowCustomValue(true);
    unitCombobox.setAllowCustomValue(true);
    levelCombobox.setAllowCustomValue(true);
    FormLayout experimentalVariableLayout = new FormLayout();
    experimentalVariableLayout.setResponsiveSteps(new ResponsiveStep("0", 4));
    experimentalVariableLayout.add(experimentalVariableCombobox, unitCombobox, levelCombobox,
        removeIcon);
    return experimentalVariableLayout;
  }

  public void setProjectContext(ProjectId projectId) {
  }

  private class Handler {

    Optional<ProjectId> projectId;

    Handler() {
      this.projectId = Optional.empty();

    }

    void setProjectContext(ProjectId projectId) {
      if (projectId == null) {
        warnAboutMissingProjectContext();
      }
      this.projectId = Optional.of(projectId);
    }

    private void warnAboutMissingProjectContext() {
      throw new ApplicationException() {
        @Override
        public ErrorCode errorCode() {
          return ErrorCode.MISSING_PROJECT_CONTEXT;
        }
      };
    }

  }


}
