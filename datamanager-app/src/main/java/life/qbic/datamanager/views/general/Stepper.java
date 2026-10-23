package life.qbic.datamanager.views.general;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.logging.api.Logger;

/**
 * Stepper Component
 *
 * <p>Horizontal linear stepper component enabling the indication of progress throughout the
 * defined steps. Every defined step carries a {@link StepState} and the state is what the styling
 * keys off (a green check for completed, solid primary for the current step, an open ring for a
 * step not reached yet, a dashed ring for an optional step that was skipped). A step defaults to
 * {@link StepState#NOT_STARTED} until it becomes the selected step or is explicitly marked.</p>
 *
 * <p>The steps are held apart by the container's own spacing rather than by decorative separator
 * glyphs: an arrow between every pair of steps carries no progress information and occupied a
 * whole band of the page.</p>
 */

public class Stepper extends Div {

  private static final Logger log = logger(Stepper.class);
  private final List<StepIndicator> stepList = new ArrayList<>();
  private StepIndicator selectedStep;

  public Stepper() {
    addClassName("stepper");
    log.debug("New instance for %s %s%s created".formatted(this.getClass().getSimpleName(), "#",
        System.identityHashCode(this)));
  }
  /**
   * Add a listener that is called, when a new {@link StepSelectedEvent event} is emitted.
   *
   * @param listener a listener that should be called
   * @return
   */
  public Registration addStepSelectionListener(ComponentEventListener<StepSelectedEvent> listener) {
    Objects.requireNonNull(listener);
    return addListener(StepSelectedEvent.class, listener);
  }


  public boolean isLastStep(StepIndicator stepIndicator) {
    return stepList.lastIndexOf(stepIndicator) == stepList.size() - 1;
  }

  public boolean isFirstStep(StepIndicator stepIndicator) {
    return stepList.indexOf(stepIndicator) == 0;
  }

  /**
   * Creates and adds a new StepIndicator to the stepper with the provided label
   *
   * @param label the label with which the step should be created
   */
  public StepIndicator addStep(String label) {
    StepIndicator newStep = createStep(label);
    stepList.add(newStep);
    add(newStep);
    return newStep;
  }


  /**
   * Removes a step from the stepper
   *
   * @param step the step to be removed from the stepper
   */
  public void removeStep(StepIndicator step) {
    int stepIndex = stepList.lastIndexOf(step);
    remove(step);
    stepList.remove(step);

    if (getChildren().anyMatch(component -> component.equals(step))) {
      remove(step);
    }
    stepList.remove(step);
  }


  /**
   * Specifies to which step the stepper should be set
   *
   * @param step       the step to which the stepper should be set
   */
  public void setSelectedStep(StepIndicator step) {
    if (step == null) {
      return;
    }
    if (!stepList.contains(step)) {
      return;
    }
    StepIndicator originalStep = getSelectedStep();
    setStepAsActive(step);
    selectedStep = step;
    // Revisiting a step makes it current again, whatever it was marked as before.
    step.setState(StepState.CURRENT);
    fireStepSelected(this, getSelectedStep(), originalStep);
  }

  /**
   * Specifies that the stepper should be set to the next step if possible
   *
   */
  public void selectNextStep() {
    StepIndicator originalStep = getSelectedStep();
    int originalIndex = stepList.indexOf(originalStep);
    if (originalIndex < stepList.size() - 1) {
      setSelectedStep(stepList.get(originalIndex + 1));
    }
  }

  /**
   * Specifies that the stepper should be set to the previous step if possible
   *
   */
  public void selectPreviousStep() {
    StepIndicator originalStep = getSelectedStep();
    int currentIndex = stepList.indexOf(originalStep);
    if (currentIndex > 0) {
      setSelectedStep(stepList.get(currentIndex - 1));
    }
  }

  /**
   * Returns the currently selected step in the Stepper component
   */
  public StepIndicator getSelectedStep() {
    return selectedStep;
  }

  /**
   * Returns a list of defined steps within the Stepper component
   */
  public List<StepIndicator> getDefinedSteps() {
    return stepList;
  }

  /**
   * Returns the first defined step in the Stepper component
   */
  public StepIndicator getFirstStep() {
    return stepList.getFirst();
  }

  /**
   * Returns the last defined step in the Stepper component
   */
  public StepIndicator getLastStep() {
    return stepList.getLast();
  }

  /**
   * Marks a step as successfully filled in. Without this every step that is not the current one
   * stayed {@link StepState#NOT_STARTED}, which made a completed step and a step never reached
   * render identically.
   *
   * @param step the step that has been completed
   */
  public void markCompleted(StepIndicator step) {
    step.setState(StepState.COMPLETED);
  }

  /**
   * Marks a step that was passed by without being filled in, e.g. an optional wizard step that the
   * user chose to leave empty. Only meaningful for steps that allow skipping.
   *
   * @param step the step that has been skipped
   */
  public void markSkipped(StepIndicator step) {
    step.setState(StepState.SKIPPED);
  }

  /**
   * Returns a step to its default state, e.g. when an entry that had completed an optional step is
   * removed again.
   *
   * @param step the step to reset
   */
  public void markNotStarted(StepIndicator step) {
    step.setState(StepState.NOT_STARTED);
  }

  private void setStepAsActive(StepIndicator activatableStep) {
    if (selectedStep != null) {
      selectedStep.getElement().setAttribute("selected", false);
    }
    activatableStep.getElement().setAttribute("selected", true);
  }

  private StepIndicator createStep(String label) {
    int number = stepList.size() + 1;
    StepIndicator step = new StepIndicator(label, number);
    step.addClassName("step");
    step.setEnabled(false);
    return step;
  }

  private void fireStepSelected(Div source, StepIndicator selectedStep,
      StepIndicator previousStep) {
    var stepSelectedEvent = new StepSelectedEvent(source,
        selectedStep, previousStep, false);
    fireEvent(stepSelectedEvent);
  }

  /**
   * Where a step stands. {@code SKIPPED} is for an optional step the user moved past without
   * filling in; a stepper that does not allow skipping never sets it and its steps only move
   * through the other three states.
   */
  public enum StepState {
    NOT_STARTED,
    CURRENT,
    COMPLETED,
    SKIPPED
  }

  public static class StepIndicator extends Div {

    private final Span marker = new Span();
    private final String label;
    private final int number;
    private StepState state = StepState.NOT_STARTED;

    public StepIndicator(String label, int number) {
      this.label = label;
      this.number = number;
      marker.addClassName("step-marker");
      this.add(marker);
      this.add(new Span(label));
      applyState();
    }

    public String getLabel() {
      return label;
    }

    public StepState getState() {
      return state;
    }

    public void setState(StepState state) {
      this.state = Objects.requireNonNull(state);
      applyState();
    }

    private void applyState() {
      // The state lives on the element as an attribute so the styling needs no knowledge of the
      // component, and the glyph is applied together with the colour so the two cannot drift.
      getElement().setAttribute("data-state", state.name());
      marker.removeAll();
      if (state == StepState.COMPLETED) {
        marker.add(VaadinIcon.CHECK.create());
      } else {
        marker.add(new Span(String.valueOf(number)));
      }
    }
  }

  public static class StepSelectedEvent extends
      ComponentEvent<Div> {

    @Serial
    private static final long serialVersionUID = -8239112805330234097L;
    private final StepIndicator selectedStep;
    private final StepIndicator previousStep;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or from the server side.
     *
     * @param selectedStep the StepIndicator which was selected when this event was triggered
     * @param previousStep the previously selected StepIndicator
     * @param fromClient   <code>true</code> if the event originated from the client
     *                     side, <code>false</code> otherwise
     */

    public StepSelectedEvent(Div source, StepIndicator selectedStep, StepIndicator previousStep,
        boolean fromClient) {
      super(source, fromClient);
      this.selectedStep = selectedStep;
      this.previousStep = previousStep;
    }

    /**
     * Provides the step which was selected to trigger this event
     */
    public StepIndicator getSelectedStep() {
      return selectedStep;
    }

    /**
     * Provides the step which was selected before the event was triggered
     */
    public StepIndicator getPreviousStep() {
      return previousStep;
    }
  }
}
