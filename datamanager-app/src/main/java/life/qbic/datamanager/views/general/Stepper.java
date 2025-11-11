package life.qbic.datamanager.views.general;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import life.qbic.logging.api.Logger;
import org.springframework.util.CollectionUtils;

/**
 * Stepper Component
 *
 * <p>Horizontal linear stepper component enabling the indication of progress throughout the
 * defined steps  </p>
 */

public class Stepper extends Div {

  private static final Logger log = logger(Stepper.class);
  private final List<StepIndicator> stepList = new ArrayList<>();
  private StepIndicator selectedStep;
  private final Supplier<Component> separator;

  public Stepper(Supplier<Component> separatorSupplier) {
    this.separator = separatorSupplier;
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
    if (!stepList.isEmpty()) {
      Component separator = this.separator.get();
      separator.addClassName("separator-" + stepList.size());
      add(separator);
    }
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
    return CollectionUtils.firstElement(stepList);
  }

  /**
   * Returns the last defined step in the Stepper component
   */
  public StepIndicator getLastStep() {
    return CollectionUtils.lastElement(stepList);
  }

  private void setStepAsActive(StepIndicator activatableStep) {
    if (selectedStep != null) {
      selectedStep.getElement().setAttribute("selected", false);
    }
    activatableStep.getElement().setAttribute("selected", true);
  }

  private StepIndicator createStep(String label) {
    String stepNumber = String.valueOf(stepList.size() + 1);
    Avatar stepAvatar = new Avatar(stepNumber);
    stepAvatar.addClassName("avatar");
    stepAvatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
    StepIndicator step = new StepIndicator(stepAvatar, label);
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

  public static class StepIndicator extends Div {

    private final Avatar avatar;
    private final String label;


    public StepIndicator(Avatar avatar, String label) {
      this.avatar = avatar;
      this.label = label;
      this.add(avatar);
      this.add(new Span(label));
    }

    public Avatar getAvatar() {
      return avatar;
    }

    public String getLabel() {
      return label;
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
     * the client side or the server side.
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
