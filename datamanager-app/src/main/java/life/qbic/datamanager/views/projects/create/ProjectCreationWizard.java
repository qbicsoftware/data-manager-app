package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import life.qbic.datamanager.views.general.Stepper;
import life.qbic.datamanager.views.general.Stepper.StepIndicator;
import life.qbic.datamanager.views.general.Stepper.StepState;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.projects.create.CollaboratorsLayout.ProjectCollaborators;
import life.qbic.datamanager.views.projects.create.ExperimentalInformationLayout.ExperimentalInformation;
import life.qbic.datamanager.views.projects.create.ProjectDesignLayout.ProjectDesign;
import life.qbic.finances.api.FinanceService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.contact.PersonLookupService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;

/**
 * The project-creation wizard: a stepper over the project's layouts with Back / Next / Cancel /
 * Create.
 *
 * <p>Extracted from the former {@code AddProjectDialog} so it can be hosted by the
 * {@code projects/create} page instead of a dialog (project owner, 2026-10-19). The
 * <b>Experimental Information</b> step is the last step and is <b>optional</b>: leaving it empty
 * creates the project without an experiment, which can then be created from the project summary.
 * Filling it in validates it like any other step.</p>
 *
 * @since 1.19.0
 */
public class ProjectCreationWizard extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Stepper stepper = new Stepper();
  private final Div content = new Div();
  private final Map<String, Component> stepContent = new HashMap<>();

  private final ProjectDesignLayout projectDesignLayout;
  private final FundingInformationLayout fundingInformationLayout;
  private final CollaboratorsLayout collaboratorsLayout;
  private final ExperimentalInformationLayout experimentalInformationLayout;

  private final StepIndicator projectDesignStep;
  private final StepIndicator fundingStep;
  private final StepIndicator collaboratorsStep;
  private final StepIndicator experimentalStep;

  private final Button backButton = new Button("Previous");
  private final Button nextButton = new Button("Next");
  private final Button cancelButton = new Button("Cancel");
  private final Button createButton = new Button("Create project");

  public ProjectCreationWizard(ProjectInformationService projectInformationService,
      FinanceService financeService, PersonLookupService personLookupService,
      SpeciesLookupService speciesLookupService, TerminologyService terminologyService) {
    addClassName("project-creation-wizard");
    this.projectDesignLayout = new ProjectDesignLayout(projectInformationService, financeService);
    this.fundingInformationLayout = new FundingInformationLayout();
    this.collaboratorsLayout = new CollaboratorsLayout(personLookupService);
    this.experimentalInformationLayout = new ExperimentalInformationLayout(speciesLookupService,
        terminologyService);

    // Required steps first, optional steps last, so a user can reach Create project without being
    // forced through fields they may not have yet. Funding Information is optional too: its
    // validators already accept an empty entry and only block a half-filled one.
    projectDesignStep = addStep("Project Design", projectDesignLayout);
    collaboratorsStep = addStep("Project Collaborators", collaboratorsLayout);

    // The hint is handed to the layout so it lands below the section title instead of above it.
    fundingInformationLayout.setStepHint(
        "Optional — if you skip this, you can add funding information later.");
    fundingStep = addStep("Funding Information (optional)", fundingInformationLayout);

    experimentalInformationLayout.setStepHint(
        "Optional — leave this empty to create the project now and create the experiment later "
            + "from the project summary.");
    experimentalStep = addStep("Experimental Information (optional)",
        experimentalInformationLayout);

    stepper.setSelectedStep(projectDesignStep);
    content.addClassName("project-creation-content");
    setStepContent(projectDesignStep);

    backButton.addClassName("back");
    backButton.addClickListener(event -> stepper.selectPreviousStep());
    nextButton.addClassNames("primary", "next");
    nextButton.addClickListener(event -> onNextClicked());
    cancelButton.addClassName("cancel");
    cancelButton.addClickListener(event -> fireEvent(new CancelEvent(this)));
    createButton.addClassNames("primary", "confirm");
    createButton.addClickListener(event -> onCreateClicked());

    Div actions = new Div(cancelButton, backButton, nextButton, createButton);
    actions.addClassName("project-creation-actions");

    stepper.addStepSelectionListener(stepSelectedEvent -> {
      // Moving off a step, whether by Next or by Previous, records what it turned out to be: a step
      // that was filled in and validated, or an optional one the user moved past empty. That is
      // what lets the bar answer "what is left?" and not just "where am I?".
      StepIndicator previous = stepSelectedEvent.getPreviousStep();
      if (previous != null && previous != stepSelectedEvent.getSelectedStep()) {
        applyOutcomeFor(previous);
      }
      setStepContent(stepSelectedEvent.getSelectedStep());
      adaptButtons(stepSelectedEvent.getSelectedStep());
    });
    adaptButtons(stepper.getFirstStep());

    add(generateSectionDivider(), stepper, generateSectionDivider(), content,
        actions);
  }

  /**
   * Records the outcome of the step the user just left. The two optional steps report
   * {@link StepState#SKIPPED} while empty, which is exactly the "passed but not filled in" state
   * the previous grey-on-grey stepper could not express.
   */
  private void applyOutcomeFor(StepIndicator step) {
    if (step == fundingStep) {
      if (isFundingInformationEmpty()) {
        stepper.markSkipped(step);
      } else {
        stepper.markCompleted(step);
      }
    } else if (step == experimentalStep) {
      if (experimentalInformationLayout.isEmpty()) {
        stepper.markSkipped(step);
      } else {
        stepper.markCompleted(step);
      }
    } else {
      stepper.markCompleted(step);
    }
  }

  private boolean isFundingInformationEmpty() {
    FundingEntry fundingEntry = fundingInformationLayout.getFundingInformation();
    return fundingEntry == null || fundingEntry.isEmpty();
  }

  /** Allows the caller to pre-fill project information from an offer search. */
  public void enableOfferSearch() {
    projectDesignLayout.enableOfferSearch();
  }

  public ProjectCreationInformation projectCreationInformation() {
    return new ProjectCreationInformation(projectDesignLayout.getProjectDesign(),
        fundingInformationLayout.getFundingInformation(),
        collaboratorsLayout.getProjectCollaborators());
  }

  /**
   * The experiment to create, if the optional step was filled in. Empty means "create the project
   * without an experiment".
   */
  public Optional<ExperimentalInformation> experimentalInformation() {
    if (experimentalInformationLayout.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(experimentalInformationLayout.getExperimentalInformation());
  }

  public Registration addCreateListener(ComponentEventListener<CreateEvent> listener) {
    return addListener(CreateEvent.class, listener);
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    return addListener(CancelEvent.class, listener);
  }

  private StepIndicator addStep(String label, Component layout) {
    stepContent.put(label, layout);
    return stepper.addStep(label);
  }

  private void setStepContent(StepIndicator step) {
    content.removeAll();
    content.add(stepContent.getOrDefault(step.getLabel(), new Div()));
  }

  private void adaptButtons(StepIndicator step) {
    backButton.setVisible(!stepper.isFirstStep(step));
    boolean last = stepper.isLastStep(step);
    nextButton.setVisible(!last);
    createButton.setVisible(last);
  }

  private void onNextClicked() {
    if (isCurrentStepValid()) {
      stepper.selectNextStep();
    }
  }
  /** Per-step gate for Next. The optional experiment step passes when it is empty. */
  private boolean isCurrentStepValid() {
    StepIndicator selected = stepper.getSelectedStep();
    if (selected == experimentalStep) {
      return experimentalInformationLayout.isEmpty()
          || !experimentalInformationLayout.validate().isInvalid();
    }
    if (selected == projectDesignStep) {
      return !projectDesignLayout.validate().isInvalid();
    }
    if (selected == fundingStep) {
      return !fundingInformationLayout.validate().isInvalid();
    }
    if (selected == collaboratorsStep) {
      return !collaboratorsLayout.isInvalid();
    }
    return true;
  }

  private void onCreateClicked() {
    StepIndicator invalidStep = firstInvalidStep();
    if (invalidStep != null) {
      // Surface the error rather than blocking silently: jump to the step that needs attention, so
      // its fields show their validation messages. This matters because the stepper labels let a
      // user jump between steps without passing through Next's per-step validation.
      stepper.setSelectedStep(invalidStep);
      setStepContent(invalidStep);
      adaptButtons(invalidStep);
      return;
    }
    fireEvent(new CreateEvent(this, projectCreationInformation(),
        experimentalInformation().orElse(null)));
  }

  /** The first step whose input is invalid, or {@code null} if the wizard is valid. */
  private StepIndicator firstInvalidStep() {
    if (projectDesignLayout.validate().isInvalid()) {
      return projectDesignStep;
    }
    if (collaboratorsLayout.isInvalid()) {
      return collaboratorsStep;
    }
    if (fundingInformationLayout.validate().isInvalid()) {
      return fundingStep;
    }
    // Optional: only invalid when something was entered.
    if (!experimentalInformationLayout.isEmpty()
        && experimentalInformationLayout.validate().isInvalid()) {
      return experimentalStep;
    }
    return null;
  }

  private static Span generateSectionDivider() {
    Span sectionDivider = new Span(new Hr());
    sectionDivider.addClassName("section-divider");
    return sectionDivider;
  }

  /** Emitted when the user asks to create the project, with its optional first experiment. */
  public static class CreateEvent extends ComponentEvent<ProjectCreationWizard> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ProjectCreationInformation projectCreationInformation;
    private final ExperimentalInformation experimentalInformation;

    public CreateEvent(ProjectCreationWizard source,
        ProjectCreationInformation projectCreationInformation,
        ExperimentalInformation experimentalInformation) {
      super(source, true);
      this.projectCreationInformation = projectCreationInformation;
      this.experimentalInformation = experimentalInformation;
    }

    public ProjectCreationInformation projectCreationInformation() {
      return projectCreationInformation;
    }

    public Optional<ExperimentalInformation> experimentalInformation() {
      return Optional.ofNullable(experimentalInformation);
    }
  }

  /** Emitted when the user leaves the wizard without creating a project. */
  public static class CancelEvent extends ComponentEvent<ProjectCreationWizard> {

    @Serial
    private static final long serialVersionUID = 1L;

    public CancelEvent(ProjectCreationWizard source) {
      super(source, true);
    }
  }

  /**
   * <b>Project Creation Information</b>
   *
   * <p>Down to earth project info data container.</p>
   */
  public record ProjectCreationInformation(ProjectDesign projectDesign, FundingEntry fundingEntry,
                                           ProjectCollaborators projectCollaborators) {

  }
}
