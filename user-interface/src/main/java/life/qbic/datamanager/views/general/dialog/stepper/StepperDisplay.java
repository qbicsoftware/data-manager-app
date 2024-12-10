package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.lang.NonNull;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class StepperDisplay extends Div implements NavigationListener {

  private final StepperDialog dialog;

  private final List<String> steps;

  private StepperDisplay(StepperDialog stepperDialog, List<String> stepNames) {
    this.dialog = stepperDialog;
    this.steps = new ArrayList<>(stepNames);
    dialog.registerNavigationListener(this);
    onNavigationChange(dialog.currentNavigation());
    this.addClassNames("full-width", "flex-horizontal", "gap-04", "flex-align-items-center");
  }

  public static StepperDisplay with(@NonNull StepperDialog stepperDialog,
      @NonNull List<String> stepNames) {
    return new StepperDisplay(stepperDialog, stepNames);
  }

  @Override
  public void onNavigationChange(NavigationInformation navigationInformation) {
    this.removeAll();
    IntStream.range(0, steps.size()).forEach(index -> {
      var step = StepDisplay.with(index + 1, steps.get(index));
      if (index == navigationInformation.currentStep() - 1) {
        step.activate();
      }
      add(step);
      if (index < navigationInformation.totalSteps() - 1) {
        add(new StepPointer(VaadinIcon.ARROW_RIGHT.create()));
      }
    });
    dialog.setStepper(this);
  }

  private static class StepPointer extends Div {

    StepPointer(@NonNull Icon icon) {
      this.add(icon);
      addClassNames("icon-color-default", "padding-left-right-03", "icon-size-xs",
          "flex-horizontal");
    }

  }
}
