package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.projects.create.ProjectCreationStepper.ProjectCreationSteps;
import life.qbic.finances.api.FinanceService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

@SpringComponent
@UIScope
public class ProjectCreationDialog extends Dialog {

  @Serial
  private static final long serialVersionUID = 7643754818237178416L;
  private static final Logger log = logger(ProjectCreationDialog.class);
  private final FinanceService financeService;
  private final Div dialogContent = new Div();
  private final String TITLE = "Create Project";
  private final Span dialogFooter = new Span("Insert awesome buttons here");
  private final ProjectCreationStepper projectCreationStepper = new ProjectCreationStepper();
  private final ProjectDesignLayout projectDesignLayout;
  private final FundingInformationLayout fundingInformationLayout;
  private final CollaboratorsLayout collaboratorsLayout;
  private final ExperimentalInformationLayout experimentalInformationLayout;
  private final List<Component> dialogLayouts = new ArrayList<>();

  public ProjectCreationDialog(FinanceService financeService, ExperimentalDesignSearchService experimentalDesignSearchService) {
    super();
    //ToDo Communicate with finance service via events instead of propagating it to the layout
    this.projectDesignLayout = new ProjectDesignLayout(financeService);
    this.fundingInformationLayout = new FundingInformationLayout();
    this.collaboratorsLayout = new CollaboratorsLayout();
    this.experimentalInformationLayout = new ExperimentalInformationLayout(experimentalDesignSearchService);
    this.financeService = requireNonNull(financeService,
        " must not be null");
    initDialog();
    initDialogLayouts();
    addClassName("project-creation-dialog");
    projectCreationStepper.addListener(
        event -> setDialogContent(event.getSelectedStep().getProjectCreationSteps()));
  }

  private void initDialog() {
    setHeaderTitle(TITLE);
    addComponentAsFirst(projectCreationStepper);
    add(dialogContent);
    dialogContent.add(projectDesignLayout);
    getFooter().add(dialogFooter);
  }

  private void initDialogLayouts() {
    dialogLayouts.add(projectDesignLayout);
    dialogLayouts.add(fundingInformationLayout);
    dialogLayouts.add(collaboratorsLayout);
    dialogLayouts.add(experimentalInformationLayout);
  }

  //ToDo setDialogContent dependent on Stepper produced events

  private void setDialogContent(ProjectCreationSteps projectCreationSteps) {
    dialogContent.removeAll();
    Component selectedComponent = dialogLayouts.stream().filter(
            component -> component.getClass().equals(projectCreationSteps.getProjectCreationLayout()))
        .findFirst()
        .orElseThrow();
    dialogContent.add(selectedComponent);
  }

  //ToDo setDialogFooter dependent on Stepper produced events
  // (Since the first step has no back button and the last step has save instead of next button)
  private void setDialogFooter() {
  }

}
