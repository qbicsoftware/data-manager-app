package life.qbic.datamanager.views.projects.create;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.general.Stepper;
import life.qbic.datamanager.views.general.Stepper.Step;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.projects.create.CollaboratorsLayout.ProjectCollaborators;
import life.qbic.datamanager.views.projects.create.ExperimentalInformationLayout.ExperimentalInformation;
import life.qbic.datamanager.views.projects.create.ProjectDesignLayout.ProjectDesign;
import life.qbic.finances.api.FinanceService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.domain.model.project.Project;

/**
 * Project Creation Dialog
 *
 * <p>Vaadin dialog component which enables the user to trigger the {@link Project}
 * creation process</p>
 */

@SpringComponent
@UIScope
public class ProjectCreationDialog extends Dialog {

  @Serial
  private static final long serialVersionUID = 7643754818237178416L;
  private static final Logger log = logger(ProjectCreationDialog.class);
  private final Div dialogContent = new Div();
  private final String TITLE = "Create Project";
  private final Stepper stepper = new Stepper();
  private final ProjectDesignLayout projectDesignLayout;
  private final FundingInformationLayout fundingInformationLayout;
  private final CollaboratorsLayout collaboratorsLayout;
  private final ExperimentalInformationLayout experimentalInformationLayout;
  private final Button confirmButton = new Button("Confirm");
  private final Button cancelButton = new Button("Cancel");
  private final Button backButton = new Button("Back");
  private final Button nextButton = new Button("Next");
  private Step projectDesignStep;
  private Step fundingInformationStep;
  private Step projectCollaboratorsStep;
  private Step experimentalInformationStep;

  public ProjectCreationDialog(FinanceService financeService,
      ExperimentalDesignSearchService experimentalDesignSearchService) {
    super();
    Objects.requireNonNull(financeService,
        financeService.getClass().getSimpleName() + " must not be null");
    Objects.requireNonNull(experimentalDesignSearchService,
        experimentalDesignSearchService.getClass().getSimpleName() + " must not be null");
    this.projectDesignLayout = new ProjectDesignLayout(financeService);
    this.fundingInformationLayout = new FundingInformationLayout();
    this.collaboratorsLayout = new CollaboratorsLayout();
    this.experimentalInformationLayout = new ExperimentalInformationLayout(
        experimentalDesignSearchService);
    initDialog();
    initListeners();
    addClassName("project-creation-dialog");
  }

  private void initDialog() {
    setHeaderTitle(TITLE);
    add(generateSectionDivider(), stepper, generateSectionDivider(), dialogContent,
        generateSectionDivider());
    initStepper();
    dialogContent.addClassName("layout-container");
    nextButton.addClassName("primary");
    confirmButton.addClassName("primary");
    setDialogContent(stepper.getFirstStep());
    adaptFooterButtons(stepper.getFirstStep());
  }

  private void initStepper() {
    projectDesignStep = stepper.addStep("Project Design");
    stepper.addComponent(createArrowSpan());
    fundingInformationStep = stepper.addStep("Funding Information");
    stepper.addComponent(createArrowSpan());
    projectCollaboratorsStep = stepper.addStep("Project Collaborators");
    stepper.addComponent(createArrowSpan());
    experimentalInformationStep = stepper.addStep("Experimental Information");
  }

  private Span createArrowSpan() {
    Icon arrowIcon = VaadinIcon.ARROW_RIGHT.create();
    Span arrow = new Span(arrowIcon);
    arrow.addClassName("project-creation-stepper-arrow");
    return arrow;
  }

  private void initListeners() {
    stepper.addListener(
        event -> {
          //We only want to update the view if the user triggered the step selection
          if (event.isFromClient()) {
            setDialogContent(event.getSelectedStep());
            adaptFooterButtons(event.getSelectedStep());
          } else {
            stepper.setSelectedStep(event.getPreviousStep(), false);
          }
        });
    cancelButton.addClickListener(event -> close());
    backButton.addClickListener(
        event -> stepper.selectPreviousStep(event.isFromClient()));
    nextButton.addClickListener(event -> {
      if (isDialogContentValid()) {
        stepper.selectNextStep(event.isFromClient());
      }
    });
    confirmButton.addClickListener(event -> {
      if (isDialogContentValid()) {
        fireEvent(new ProjectCreationEvent(this, projectDesignLayout.getProjectDesign(),
            fundingInformationLayout.getFundingInformation(),
            collaboratorsLayout.getCollaboratorInformation(),
            experimentalInformationLayout.getExperimentalInformation(), true));
      }
    });
  }

  /**
   * Add a listener that is called, when a new {@link ProjectCreationEvent event} is emitted.
   *
   * @param listener a listener that should be called
   * @since 1.0.0
   */
  public void addListener(ComponentEventListener<ProjectCreationEvent> listener) {
    Objects.requireNonNull(listener);
    addListener(ProjectCreationEvent.class, listener);
  }

  private boolean isDialogContentValid() {
    return dialogContent.getChildren().filter(component -> component instanceof HasValidation)
        .map(component -> ((HasValidation) component).isInvalid()).anyMatch(aBoolean -> !aBoolean);
  }

  private void adaptFooterButtons(Step step) {
    DialogFooter footer = getFooter();
    footer.removeAll();
    Span rightButtonsContainer = new Span();
    rightButtonsContainer.addClassNames("footer-right-buttons-container");
    if (stepper.getFirstStep().equals(step)) {
      //First Step --> no back button
      rightButtonsContainer.add(cancelButton, nextButton);
      footer.add(new Span(), rightButtonsContainer);
    } else if (stepper.getLastStep().equals(step)) {
      //Last Step --> Confirm Button instead of Next button
      rightButtonsContainer.add(cancelButton, confirmButton);
      footer.add(backButton, rightButtonsContainer);
    } else {
      rightButtonsContainer.add(cancelButton, nextButton);
      footer.add(backButton, rightButtonsContainer);
    }
  }

  private static Span generateSectionDivider() {
    Span sectionDivider = new Span(new Hr());
    sectionDivider.addClassName("section-divider");
    return sectionDivider;
  }

  private void setDialogContent(Step step) {
    dialogContent.removeAll();
    Component selectedComponent = null;
    if (step.equals(projectDesignStep)) {
      selectedComponent = projectDesignLayout;
    } else if (step.equals(fundingInformationStep)) {
      selectedComponent = fundingInformationLayout;
    } else if (step.equals(projectCollaboratorsStep)) {
      selectedComponent = collaboratorsLayout;
    } else if (step.equals(experimentalInformationStep)) {
      selectedComponent = experimentalInformationLayout;
    }
    dialogContent.add(selectedComponent);
  }

  /**
   * <b>Project Creation Event</b>
   *
   * <p>Indicates that a user submitted a project creation request</p>
   *
   * @since 1.0.0
   */
  public static class ProjectCreationEvent extends ComponentEvent<ProjectCreationDialog> {

    @Serial
    private static final long serialVersionUID = 3629446840913968906L;
    private final ProjectDesign projectDesign;
    private final FundingEntry fundingEntry;
    private final ProjectCollaborators projectCollaborators;
    private final ExperimentalInformation experimentalInformation;

    public ProjectCreationEvent(ProjectCreationDialog source,
        ProjectDesign projectDesign, FundingEntry fundingEntry,
        ProjectCollaborators projectCollaborators,
        ExperimentalInformation experimentalInformation, boolean fromClient) {
      super(source, fromClient);
      this.projectDesign = projectDesign;
      this.fundingEntry = fundingEntry;
      this.projectCollaborators = projectCollaborators;
      this.experimentalInformation = experimentalInformation;
    }

    public ProjectDesign getProjectDesign() {
      return projectDesign;
    }

    public FundingEntry getFundingEntry() {
      return fundingEntry;
    }

    public ProjectCollaborators getProjectCollaborators() {
      return projectCollaborators;
    }

    public ExperimentalInformation getExperimentalInformation() {
      return experimentalInformation;
    }

  }
}
