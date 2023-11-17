package life.qbic.datamanager.views.projects.create;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class ProjectCreationStepper extends Div {

  private static final Logger log = getLogger(ProjectCreationStepper.class);
  private final Step designProject = createStep("Project Design",
      ProjectCreationSteps.DESIGN_PROJECT);
  private final Step fundingInformation = createStep("Funding Information",
      ProjectCreationSteps.DEFINE_FUNDING);
  private final Step projectCollaborators = createStep("Project Collaborators",
      ProjectCreationSteps.SET_PROJECT_COLLABORATORS);
  private final Step experimentalInformation = createStep("Experimental Information",
      ProjectCreationSteps.SET_EXPERIMENT_INFORMATION);
  private final List<Step> projectCreationStepList = List.of(designProject, fundingInformation,
      projectCollaborators, experimentalInformation);
  private Step currentStep = designProject;

  public ProjectCreationStepper() {
    initLayout();
    addClassName("project-creation-stepper");
    setStepAsActive(currentStep);
    log.debug(
        String.format("New instance for %s(#%s) created",
            this.getClass().getSimpleName(), System.identityHashCode(this)));
  }

  /**
   * Add a listener that is called, when a new {@link ProjectCreationStepSelectedEvent event} is
   * emitted.
   *
   * @param listener a listener that should be called
   * @since 1.0.0
   */
  public void addListener(ComponentEventListener<ProjectCreationStepSelectedEvent> listener) {
    Objects.requireNonNull(listener);
    addListener(ProjectCreationStepSelectedEvent.class, listener);
  }

  public void setSelectedStep(Step step, boolean fromClient) {
    Step originalStep = getCurrentStep();
    if (projectCreationStepList.contains(step)) {
      setCurrentStep(step);
      fireStepSelected(this, getCurrentStep(), originalStep, fromClient);
    }
  }

  public void selectNextStep(boolean fromClient) {
    Step originalStep = getCurrentStep();
    int originalIndex = projectCreationStepList.indexOf(originalStep);
    if (originalIndex < projectCreationStepList.size() - 1) {
      setCurrentStep(projectCreationStepList.get(originalIndex + 1));
      fireStepSelected(this, getCurrentStep(), originalStep, fromClient);
    }
  }

  public void selectPreviousStep(boolean fromClient) {
    Step originalStep = getCurrentStep();
    int currentIndex = projectCreationStepList.indexOf(originalStep);
    if (currentIndex > 0) {
      setCurrentStep(projectCreationStepList.get(currentIndex - 1));
      fireStepSelected(this, getCurrentStep(), originalStep, fromClient);
    }
  }

  private void setCurrentStep(Step step) {
    setStepAsActive(step);
    currentStep = step;
  }

  public Step getCurrentStep() {
    return currentStep;
  }

  private void setStepAsActive(Step activatableStep) {
    currentStep.getElement().setAttribute("selected", false);
    activatableStep.getElement().setAttribute("selected", true);
  }

  private void initLayout() {
    add(designProject, createArrowSpan(), fundingInformation, createArrowSpan(),
        projectCollaborators,
        createArrowSpan(), experimentalInformation);
  }

  //ToDo can this be automatized?
  private Span createArrowSpan() {
    Icon arrowIcon = VaadinIcon.ARROW_RIGHT.create();
    Span arrow = new Span(arrowIcon);
    arrow.addClassName("arrow");
    return arrow;
  }


  private Step createStep(String label, ProjectCreationSteps projectCreationSteps) {
    String stepNumber = String.valueOf(projectCreationSteps.ordinal() + 1);
    Avatar stepAvatar = new Avatar(stepNumber);
    stepAvatar.addClassName("avatar");
    stepAvatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
    Step step = new Step(stepAvatar, new Span(label), projectCreationSteps);
    step.addClassName("step");
    step.setEnabled(false);
    return step;
  }

  private void fireStepSelected(Div source, Step selectedStep, Step previousStep,
      boolean fromClient) {
    var projectCreationStepSelectedEvent = new ProjectCreationStepSelectedEvent(source,
        selectedStep, previousStep, fromClient);
    fireEvent(projectCreationStepSelectedEvent);
  }

  //ToDo Replace this with method to allow user to define steps?
  public enum ProjectCreationSteps {
    DESIGN_PROJECT(ProjectDesignLayout.class), DEFINE_FUNDING(
        FundingInformationLayout.class), SET_PROJECT_COLLABORATORS(
        CollaboratorsLayout.class), SET_EXPERIMENT_INFORMATION(ExperimentalInformationLayout.class);

    private final Class<?> projectCreationLayout;

    ProjectCreationSteps(Class<?> projectCreationLayout) {
      this.projectCreationLayout = projectCreationLayout;
    }

    public Class<?> getProjectCreationLayout() {
      return projectCreationLayout;
    }
  }

  public static class Step extends Div {

    private final ProjectCreationSteps projectCreationSteps;
    private final Avatar avatar;

    public Step(Avatar avatar, Component label, ProjectCreationSteps projectCreationSteps) {
      this.avatar = avatar;
      this.projectCreationSteps = projectCreationSteps;
      this.add(avatar);
      this.add(label);
    }

    public Avatar getAvatar() {
      return avatar;
    }

    public ProjectCreationSteps getProjectCreationSteps() {
      return projectCreationSteps;
    }
  }

  public static class ProjectCreationStepSelectedEvent extends
      ComponentEvent<Div> {

    @Serial
    private static final long serialVersionUID = -8239112805330234097L;
    private final Step selectedStep;
    private final Step previousStep;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param selectedStep the Step which was selected when this event was triggered
     * @param previousStep the previously selected Step
     * @param fromClient   <code>true</code> if the event originated from the client
     *                     side, <code>false</code> otherwise
     */

    public ProjectCreationStepSelectedEvent(Div source, Step selectedStep, Step previousStep,
        boolean fromClient) {
      super(source, fromClient);
      this.selectedStep = selectedStep;
      this.previousStep = previousStep;
    }

    public Step getSelectedStep() {
      return selectedStep;
    }

    public Step getPreviousStep() {
      return previousStep;
    }
  }
}
