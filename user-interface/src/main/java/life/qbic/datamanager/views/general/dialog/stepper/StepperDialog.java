package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import org.springframework.lang.NonNull;

/**
 * <b>Stepper Dialog</b>
 *
 * <p>A wizard-like dialog, that usually contains two or more steps a user needs
 * to navigate through in order to perform a certain task.</p>
 *
 * @since 1.7.0
 */
public class StepperDialog {

  private final AppDialog dialog;
  private final List<Step> steps;
  private final List<NavigationListener> navigationListeners;
  private final int numberOfSteps;
  private int currentStep;

  private StepperDialog(AppDialog dialog, List<Step> steps) {
    this.dialog = Objects.requireNonNull(dialog);
    this.steps = new ArrayList<>(Objects.requireNonNull(steps));
    this.navigationListeners = new ArrayList<>();
    if (steps.isEmpty()) {
      throw new IllegalArgumentException("Steps cannot be empty");
    }
    this.numberOfSteps = steps.size();
    currentStep = 1; // we use a 1-based indexing of steps
    setCurrentStep(steps.get(0), dialog);
  }

  public static StepperDialog create(@NonNull AppDialog dialog, @NonNull List<Step> steps) {
    return new StepperDialog(dialog, steps);
  }

  private static void informNavigationListeners(List<NavigationListener> listeners,
      NavigationInformation information) {
    listeners.forEach(listener -> listener.onNavigationChange(information));
  }

  private static boolean hasNextStep(int currentStep, int numberOfSteps) {
    return currentStep < numberOfSteps;
  }

  private static boolean hasPreviousStep(int currentStep) {
    return currentStep > 1;
  }

  private static boolean stepIsValid(Step step) {
    var userInput = step.userInput();
    return userInput.validate().hasPassed();
  }

  private static void setCurrentStep(Step step, AppDialog dialog) {
    dialog.setBody(step.component());
    dialog.registerUserInput(step.userInput());
  }

  public void registerNavigationListener(NavigationListener listener) {
    navigationListeners.add(listener);
  }

  public NavigationInformation currentNavigation() {
    return navigationInformation();
  }

  private NavigationInformation navigationInformation() {
    return new NavigationInformation(currentStep, numberOfSteps);
  }

  public void setStepper(@NonNull Component component) {
    dialog.setNavigation(Objects.requireNonNull(component));
    dialog.displayNavigation();
  }

  public void next() {
    if (hasNextStep(currentStep, numberOfSteps) && currentStepIsValid()) {
      currentStep++;
      setCurrentStep(steps.get(currentStep - 1), dialog);
      informNavigationListeners(navigationListeners, currentNavigation());
    }
  }

  private boolean currentStepIsValid() {
    return stepIsValid(steps.get(currentStep - 1));
  }

  public void previous() {
    if (hasPreviousStep(currentStep)) {
      currentStep--;
      setCurrentStep(steps.get(currentStep - 1), dialog);
      informNavigationListeners(navigationListeners, currentNavigation());
    }
  }

  public void setFooter(Component footer) {
    dialog.setFooter(footer);
  }

  public void open() {
    dialog.open();
  }

  public void cancel() {
    dialog.cancel();
  }

  public void confirm() {
    dialog.confirm();
  }
}
