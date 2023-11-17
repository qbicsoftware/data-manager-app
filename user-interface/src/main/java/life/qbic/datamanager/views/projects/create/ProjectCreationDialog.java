package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;
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
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.projects.create.AddProjectDialog.ProjectAddEvent;
import life.qbic.datamanager.views.projects.create.CollaboratorsLayout.ProjectCollaborators;
import life.qbic.datamanager.views.projects.create.ExperimentalInformationLayout.ExperimentalInformation;
import life.qbic.datamanager.views.projects.create.ProjectCreationStepper.ProjectCreationSteps;
import life.qbic.datamanager.views.projects.create.ProjectDesignLayout.ProjectDesign;
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
  private final Div layoutContainer = new Div();
  private final String TITLE = "Create Project";
  private final ProjectCreationStepper projectCreationStepper = new ProjectCreationStepper();
  private final ProjectDesignLayout projectDesignLayout;
  private final FundingInformationLayout fundingInformationLayout;
  private final CollaboratorsLayout collaboratorsLayout;
  private final ExperimentalInformationLayout experimentalInformationLayout;
  private Component currentLayout;
  private final Button confirmButton = new Button("Confirm");
  private final Button cancelButton = new Button("Cancel");
  private final Button backButton = new Button("Back");
  private final Button nextButton = new Button("Next");

  public ProjectCreationDialog(FinanceService financeService,
      ExperimentalDesignSearchService experimentalDesignSearchService) {
    super();
    //ToDo Communicate with finance service via events instead of propagating it to the layout
    this.projectDesignLayout = new ProjectDesignLayout(financeService);
    this.fundingInformationLayout = new FundingInformationLayout();
    this.collaboratorsLayout = new CollaboratorsLayout();
    this.experimentalInformationLayout = new ExperimentalInformationLayout(
        experimentalDesignSearchService);
    this.financeService = requireNonNull(financeService,
        " must not be null");
    initDialog();
    initListeners();
    addClassName("project-creation-dialog");
  }

  private void initDialog() {
    setHeaderTitle(TITLE);
    add(generateSectionDivider(), projectCreationStepper, generateSectionDivider(), layoutContainer,
        generateSectionDivider());
    layoutContainer.addClassName("layout-container");
    nextButton.addClassName("primary");
    confirmButton.addClassName("primary");
    setDialogContent(ProjectCreationSteps.DESIGN_PROJECT);
    adaptFooterButtons(ProjectCreationSteps.DESIGN_PROJECT);
  }

  private void initListeners() {
    projectCreationStepper.addListener(
        event -> {
          if (event.isFromClient()) {
            if (isCurrentLayoutValid()) {
              setDialogContent(event.getSelectedStep().getProjectCreationSteps());
              adaptFooterButtons(event.getSelectedStep().getProjectCreationSteps());
            } else {
              projectCreationStepper.setSelectedStep(event.getPreviousStep(), false);
            }
          }
        });
    cancelButton.addClickListener(event -> close());
    backButton.addClickListener(
        event -> projectCreationStepper.selectPreviousStep(event.isFromClient()));
    nextButton.addClickListener(event -> {
      if (isCurrentLayoutValid()) {
        projectCreationStepper.selectNextStep(event.isFromClient());
      }
    });
    confirmButton.addClickListener(event -> {
      //Todo can this be simplified?
      if (projectDesignLayout.isInvalid()) {
        throw new ApplicationException(projectDesignLayout.getErrorMessage());
      } else if (fundingInformationLayout.isInvalid()) {
        throw new ApplicationException(fundingInformationLayout.getErrorMessage());
      } else if (collaboratorsLayout.isInvalid()) {
        throw new ApplicationException(collaboratorsLayout.getErrorMessage());
      } else if (experimentalInformationLayout.isInvalid()) {
        throw new ApplicationException(experimentalInformationLayout.getErrorMessage());
      } else {
        fireEvent(new ProjectCreationEvent(this, projectDesignLayout.getProjectDesign(),
            fundingInformationLayout.getFundingInformation(),
            collaboratorsLayout.getCollaboratorInformation(),
            experimentalInformationLayout.getExperimentalInformation(), true));
      }
    });
  }

  /**
   * Add a listener that is called, when a new {@link ProjectAddEvent event} is emitted.
   *
   * @param listener a listener that should be called
   * @since 1.0.0
   */
  public void addListener(ComponentEventListener<ProjectCreationEvent> listener) {
    Objects.requireNonNull(listener);
    addListener(ProjectCreationEvent.class, listener);
  }

  private boolean isCurrentLayoutValid() {
    return !((HasValidation) currentLayout).isInvalid();
  }

  private void adaptFooterButtons(ProjectCreationSteps projectCreationSteps) {
    DialogFooter footer = getFooter();
    footer.removeAll();
    Span rightButtonsContainer = new Span();
    rightButtonsContainer.addClassNames("footer-right-buttons-container");
    switch (projectCreationSteps) {
      case DESIGN_PROJECT -> {
        rightButtonsContainer.add(cancelButton, nextButton);
        footer.add(new Span(), rightButtonsContainer);
      }
      case SET_EXPERIMENT_INFORMATION -> {
        rightButtonsContainer.add(cancelButton, confirmButton);
        footer.add(backButton, rightButtonsContainer);
      }
      default -> {
        rightButtonsContainer.add(cancelButton, nextButton);
        footer.add(backButton, rightButtonsContainer);
      }
    }
  }

  private static Span generateSectionDivider() {
    Span sectionDivider = new Span(new Hr());
    sectionDivider.addClassName("section-divider");
    return sectionDivider;
  }

  private void setDialogContent(ProjectCreationSteps projectCreationSteps) {
    layoutContainer.removeAll();
    List<Component> dialogLayouts = List.of(projectDesignLayout, fundingInformationLayout,
        collaboratorsLayout, experimentalInformationLayout);
    Component selectedComponent = dialogLayouts.stream().filter(
            component -> component.getClass().equals(projectCreationSteps.getProjectCreationLayout()))
        .findFirst()
        .orElseThrow();
    currentLayout = selectedComponent;
    layoutContainer.add(selectedComponent);
  }

  /**
   * <b>Project Add Event</b>
   *
   * <p>Indicates that a user submitted a project addition request</p>
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
