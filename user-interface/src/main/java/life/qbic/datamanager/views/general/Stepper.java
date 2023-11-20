package life.qbic.datamanager.views.general;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.springframework.util.CollectionUtils;

/**
 * Stepper Component
 *
 * <p>Horizontal linear stepper component enabling the indication of progress throughout the
 * defined steps  </p>
 */

@SpringComponent
public class Stepper extends Div {

  private static final Logger log = getLogger(Stepper.class);
  private final List<Step> stepList = new ArrayList<>();
  private Step selectedStep;

  public Stepper() {
    addClassName("stepper");
    log.debug(
        String.format("New instance for %s(#%s) created",
            this.getClass().getSimpleName(), System.identityHashCode(this)));
  }

  /**
   * Add a listener that is called, when a new {@link StepSelectedEvent event} is emitted.
   *
   * @param listener a listener that should be called
   */
  public void addListener(ComponentEventListener<StepSelectedEvent> listener) {
    Objects.requireNonNull(listener);
    addListener(StepSelectedEvent.class, listener);
  }


  /**
   * Creates and adds a new Step to the stepper with the provided label
   *
   * @param label the label with which the step should be created
   */
  public Step addStep(String label) {
    Step newStep = createStep(label);
    add(newStep);
    return newStep;
  }


  /**
   * Removes a step from the stepper
   *
   * @param step the step to be removed from the stepper
   */
  public void removeStep(Step step) {
    if (getChildren().anyMatch(component -> component.equals(step))) {
      remove(step);
    }
    stepList.remove(step);
  }

  /**
   * Adds a component to the stepper which should not act as a step
   *
   * @param component the component to be removed from the stepper
   */
  public void addComponent(Component component) {
    add(component);
  }


  /**
   * Removes a component from the stepper
   *
   * @param component the component to be removed from the stepper
   */
  public void removeComponent(Component component) {
    if (getChildren().anyMatch(cmp -> cmp.equals(component))) {
      remove(component);
    }
  }

  /**
   * Specifies to which step the stepper should be set
   *
   * @param step       the step to which the stepper should be set
   * @param fromClient indicates if the step was selected by the client
   */
  public void setSelectedStep(Step step, boolean fromClient) {
    if (selectedStep != null && stepList.contains(step)) {
      Step originalStep = getSelectedStep();
      setStepAsActive(step);
      selectedStep = step;
      fireStepSelected(this, getSelectedStep(), originalStep, fromClient);
    } else {
      selectedStep = step;
    }
  }

  /**
   * Specifies that the stepper should be set to the next step if possible
   *
   * @param fromClient indicates if the step was selected by the client
   */
  public void selectNextStep(boolean fromClient) {
    Step originalStep = getSelectedStep();
    int originalIndex = stepList.indexOf(originalStep);
    if (originalIndex < stepList.size() - 1) {
      setSelectedStep(stepList.get(originalIndex + 1), false);
      fireStepSelected(this, getSelectedStep(), originalStep, fromClient);
    }
  }

  /**
   * Specifies that the stepper should be set to the previous step if possible
   *
   * @param fromClient indicates if the step was selected by the client
   */
  public void selectPreviousStep(boolean fromClient) {
    Step originalStep = getSelectedStep();
    int currentIndex = stepList.indexOf(originalStep);
    if (currentIndex > 0) {
      setSelectedStep(stepList.get(currentIndex - 1), false);
      fireStepSelected(this, getSelectedStep(), originalStep, fromClient);
    }
  }

  /**
   * Returns the currently selected step in the Stepper component
   */
  public Step getSelectedStep() {
    return selectedStep;
  }

  /**
   * Returns a list of defined steps within the Stepper component
   */
  public List<Step> getDefinedSteps() {
    return stepList;
  }

  /**
   * Returns the first defined step in the Stepper component
   */
  public Step getFirstStep() {
    return CollectionUtils.firstElement(stepList);
  }

  /**
   * Returns the last defined step in the Stepper component
   */
  public Step getLastStep() {
    return CollectionUtils.lastElement(stepList);
  }

  private void setStepAsActive(Step activatableStep) {
    selectedStep.getElement().setAttribute("selected", false);
    activatableStep.getElement().setAttribute("selected", true);
  }

  private Step createStep(String label) {
    String stepNumber = String.valueOf(stepList.size() + 1);
    Avatar stepAvatar = new Avatar(stepNumber);
    stepAvatar.addClassName("avatar");
    stepAvatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
    Step step = new Step(stepAvatar, new Span(label));
    step.addClassName("step");
    step.setEnabled(false);
    stepList.add(step);
    setSelectedStep(getFirstStep(), false);
    return step;
  }

  private void fireStepSelected(Div source, Step selectedStep, Step previousStep,
      boolean fromClient) {
    var stepSelectedEvent = new StepSelectedEvent(source,
        selectedStep, previousStep, fromClient);
    fireEvent(stepSelectedEvent);
  }

  public static class Step extends Div {

    private final Avatar avatar;

    public Step(Avatar avatar, Component label) {
      this.avatar = avatar;
      this.add(avatar);
      this.add(label);
    }

    public Avatar getAvatar() {
      return avatar;
    }

  }

  public static class StepSelectedEvent extends
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

    public StepSelectedEvent(Div source, Step selectedStep, Step previousStep,
        boolean fromClient) {
      super(source, fromClient);
      this.selectedStep = selectedStep;
      this.previousStep = previousStep;
    }

    /**
     * Provides the step which was selected to trigger this event
     */
    public Step getSelectedStep() {
      return selectedStep;
    }

    /**
     * Provides the step which was selected before the event was triggered
     */
    public Step getPreviousStep() {
      return previousStep;
    }
  }
}
