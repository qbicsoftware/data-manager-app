package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.html.Div;
import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class StepDisplay extends Div implements NavigationListener  {

  private final StepperDialog dialog;

  private final List<String> steps;

  private StepDisplay(StepperDialog stepperDialog, List<String> stepNames) {
    this.dialog = stepperDialog;
    this.steps = new ArrayList<>(stepNames);
    dialog.registerNavigationListener(this);
    onNavigationChange(dialog.currentNavigation());
    this.addClassNames("full-width", "flex-horizontal", "gap-04");
  }

  public static StepDisplay with(@NonNull StepperDialog stepperDialog, @NonNull List<String> stepNames) {
    return new StepDisplay(stepperDialog, stepNames);
  }

  @Override
  public void onNavigationChange(NavigationInformation navigationInformation) {
    this.removeAll();
    steps.forEach(step -> add(new Div(step)));
    dialog.setStepper(this);
  }
}
