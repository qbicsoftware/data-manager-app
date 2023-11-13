package life.qbic.datamanager.views.projects.create;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import java.io.Serial;
import java.util.ArrayList;
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
public class ProjectCreationStepper extends Tabs {

  private static final Logger log = getLogger(ProjectCreationStepper.class);
  Step designProject = createStep("Project Design", ProjectCreationSteps.DESIGN_PROJECT);
  Step fundingInformation = createStep("Funding Information",
      ProjectCreationSteps.DEFINE_FUNDING);
  Step projectCollaborators = createStep("Project Collaborators",
      ProjectCreationSteps.SET_PROJECT_COLLABORATORS);
  Step experimentalInformation = createStep("Experimental Information",
      ProjectCreationSteps.SET_EXPERIMENT_INFORMATION);
  private final List<ComponentEventListener<ProjectCreationStepSelectedEvent>> projectCreationStepSelectionListeners = new ArrayList<>();

  public ProjectCreationStepper() {
    initLayout();
    addTabSelectionListeners();
    addClassName("project-creation-stepper");
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
    projectCreationStepSelectionListeners.add(listener);
  }

  public ProjectCreationSteps getCurrentProjectCreationLayout() {
    Step currentStep = (Step) getSelectedTab();
    return currentStep.getProjectCreationSteps();
  }

  private void initLayout() {
    add(designProject, createArrowTab(), fundingInformation, createArrowTab(), projectCollaborators,
        createArrowTab(), experimentalInformation);
    addThemeVariants(TabsVariant.LUMO_MINIMAL);
  }

  //ToDo can this be automatized?
  private Tab createArrowTab() {
    Icon arrowIcon = VaadinIcon.ARROW_RIGHT.create();
    Tab arrow = new Tab(arrowIcon);
    arrow.addClassName("arrow-tab");
    arrow.setEnabled(false);
    return arrow;
  }

  private Step createStep(String label, ProjectCreationSteps projectCreationSteps) {
    String stepNumber = String.valueOf(projectCreationSteps.ordinal() + 1);
    Avatar stepAvatar = new Avatar(stepNumber);
    stepAvatar.addClassName("avatar");
    stepAvatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
    Step step = new Step(stepAvatar, new Span(label), projectCreationSteps);
    step.addClassName("step");
    return step;
  }

  private void addTabSelectionListeners() {
    addSelectedChangeListener(
        event -> fireProjectCreationStepSelected(this, (Step) event.getSelectedTab(),
            (Step) event.getPreviousTab(), event.isFromClient()));
  }

  private void fireProjectCreationStepSelected(Tabs source, Step selectedStep, Step previousStep,
      boolean fromClient) {
    var projectCreationStepSelectedEvent = new ProjectCreationStepSelectedEvent(source,
        selectedStep, previousStep, fromClient);
    projectCreationStepSelectionListeners.forEach(
        listener -> listener.onComponentEvent(projectCreationStepSelectedEvent));
  }

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

  public static class Step extends Tab {

    private final ProjectCreationSteps projectCreationSteps;
    private final Avatar avatar;

    public Step(Avatar avatar, Component label, ProjectCreationSteps projectCreationSteps) {
      this.avatar = avatar;
      this.projectCreationSteps = projectCreationSteps;
      this.add(avatar);
      this.add(label);
      this.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
    }

    public Avatar getAvatar() {
      return avatar;
    }

    public ProjectCreationSteps getProjectCreationSteps() {
      return projectCreationSteps;
    }
  }

  public static class ProjectCreationStepSelectedEvent extends
      ComponentEvent<Tabs> {

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

    public ProjectCreationStepSelectedEvent(Tabs source, Step selectedStep, Step previousStep,
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
