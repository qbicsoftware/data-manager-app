package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.lang.NonNull;

/**
 * <b>Stepper Display</b>
 *
 * <p>Shows all available steps in a {@link StepperDialog}. </p>
 *
 * @since 1.7.0
 */
public class StepperDisplay extends Div implements NavigationListener {

  private final transient StepperDialog dialog;

  private final List<String> steps;

  private StepperDisplay(StepperDialog stepperDialog, List<String> stepNames) {
    this.dialog = stepperDialog;
    this.steps = new ArrayList<>(stepNames);
    dialog.registerNavigationListener(this);
    // Init the stepper to the dialogs current navigation point
    onNavigationChange(dialog.currentNavigation());
    this.addClassNames("width-full", "flex-horizontal", "gap-04", "flex-align-items-bottom");
  }

  /**
   * Creates a {@link StepperDisplay} for the provided {@link StepperDialog}.
   * <p>
   * The client does not need to do anything manually, the wiring with the stepper dialog happens
   * during instantiation. The stepper display will show the current step active in the stepper
   * dialog.
   *
   * @param stepperDialog the stepper dialog to subscribe to navigation changes
   * @param stepNames     the step names to display
   * @return a stepper display
   * @since 1.7.0
   */
  public static StepperDisplay with(@NonNull StepperDialog stepperDialog,
      @NonNull List<String> stepNames) {
    return new StepperDisplay(stepperDialog, stepNames);
  }

  @Override
  public void onNavigationChange(NavigationInformation navigationInformation) {
    this.removeAll();
    IntStream.range(0, steps.size()).forEach(index -> {
      // The user should see a 1-based counting of the steps
      var step = StepDisplay.with(index + 1, steps.get(index));
      // The current navigation point should be highlighted to the user
      if (index == navigationInformation.currentStep() - 1) {
        step.activate();
      }
      add(step);
      // Between the steps there need to be an arrow icon pointing to the next step
      if (index < navigationInformation.totalSteps() - 1) {
        add(new StepPointer(VaadinIcon.ARROW_RIGHT.create()));
      }
    });
    dialog.setStepper(this);
  }

  private static class StepPointer extends Div {

    StepPointer(@NonNull Icon icon) {
      this.add(icon);
      addClassNames("icon-color-default", "padding-horizontal-04", "dialog-step-icon-arrow");
    }

  }
}
