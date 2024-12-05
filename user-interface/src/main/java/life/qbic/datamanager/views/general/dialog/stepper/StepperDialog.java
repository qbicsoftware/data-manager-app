package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.dialog.AppDialog;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class StepperDialog {

  private final AppDialog dialog;
  private final List<Step> steps;
  private StepperNavigation navigation;
  private final int numberOfSteps;
  private int currentStep;

  private StepperDialog(AppDialog dialog, List<Step> steps) {
    this.dialog = Objects.requireNonNull(dialog);
    this.steps = new ArrayList<>(Objects.requireNonNull(steps));
    if (steps.isEmpty()) {
      throw new IllegalArgumentException("Steps cannot be empty");
    }
    this.numberOfSteps = steps.size();
    currentStep = 1; // we use a 1-based indexing of steps
    setCurrentStep(steps.get(0), dialog);
  }

  public static StepperDialog create(AppDialog dialog, List<Step> steps) {
    return new StepperDialog(dialog, steps);
  }

  public void setNavigation(StepperNavigation navigation) {
    this.navigation = Objects.requireNonNull(navigation);
    updateNavigation(navigation, currentStep, numberOfSteps);
  }

  private static void updateNavigation(StepperNavigation navigation, int currentStep, int numberOfSteps) {
    if (isIntermediateStep(currentStep, numberOfSteps)) {
      navigation.intermediate();
      return;
    }
    if (currentStep == numberOfSteps) {
      navigation.last();
    } else {
      navigation.first();
    }
  }

  private static boolean hasNextStep(int currentStep, int numberOfSteps) {
    return currentStep < numberOfSteps;
  }

  private static boolean hasPreviousStep(int currentStep) {
    return currentStep > 1;
  }

  private static boolean isIntermediateStep(int currentStep, int numberOfSteps) {
    return hasNextStep(currentStep, numberOfSteps) && hasPreviousStep(currentStep);
  }

  public void next() {
    if (hasNextStep(currentStep, numberOfSteps)) {
      currentStep++;
      setCurrentStep(steps.get(currentStep - 1), dialog);
      updateNavigation(navigation, currentStep, numberOfSteps);
    }
  }

  public void previous() {
    if (hasPreviousStep(currentStep)) {
      currentStep--;
      setCurrentStep(steps.get(currentStep - 1), dialog);
      updateNavigation(navigation, currentStep, numberOfSteps);
    }
  }

  public void setFooter(Component footer) {
    dialog.setFooter(footer);
  }

  private static void setCurrentStep(Step step, AppDialog dialog) {
    dialog.setBody(step.component());
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
