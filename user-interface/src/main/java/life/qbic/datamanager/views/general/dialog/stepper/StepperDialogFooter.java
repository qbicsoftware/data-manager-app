package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.html.Div;
import java.util.Objects;
import life.qbic.datamanager.views.general.dialog.ButtonFactory;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class StepperDialogFooter implements NavigationListener {

  private final StepperDialog dialog;

  private final FooterFactory footerFactory = new FooterFactory();

  private StepperDialogFooter(StepperDialog dialog) {
    this.dialog = dialog;
    dialog.registerNavigationListener(this);
    onNavigationChange(dialog.currentNavigation()); // we want to init the footer properly.
  }

  public static StepperDialogFooter with(StepperDialog dialog) {
    return new StepperDialogFooter(Objects.requireNonNull(dialog));
  }

  private static void updateFooter(StepperDialog dialog, Div footer) {
    dialog.setFooter(footer);
  }

  @Override
  public void onNavigationChange(NavigationInformation navigationInformation) {
    int currentStep = navigationInformation.currentStep();
    int totalSteps = navigationInformation.totalSteps();
    if (isIntermediateStep(currentStep, totalSteps)) {
      updateFooter(dialog,footerFactory.createIntermediate(dialog));
      return;
    }
    if (currentStep == totalSteps) {
      updateFooter(dialog,footerFactory.createLast(dialog));
    } else {
      updateFooter(dialog, footerFactory.createFirst(dialog));
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

  private static class FooterFactory {


    Div createFirst(StepperDialog dialog) {
      return new FirstFooter(dialog);
    }

    Div createIntermediate(StepperDialog dialog) {
      return new IntermediateFooter(dialog);
    }

    Div createLast(StepperDialog dialog) {
      return new LastFooter(dialog);
    }
  }

  private static class FirstFooter extends Div {

    FirstFooter(StepperDialog dialog) {
      addClassNames("flex-horizontal", "gap-04", "footer");
      var buttonFactory = new ButtonFactory();
      var cancelButton = buttonFactory.createCancelButton("Cancel");
      var nextButton = buttonFactory.createNavigationButton("Next");
      cancelButton.addClickListener(listener-> dialog.cancel());
      nextButton.addClickListener(listener -> dialog.next());
      add(cancelButton, nextButton);
    }
  }

  private static class IntermediateFooter extends Div {

    public IntermediateFooter(StepperDialog dialog) {
      addClassNames("flex-horizontal", "footer-intermediate");
      var buttonFactory = new ButtonFactory();
      var cancelButton = buttonFactory.createCancelButton("Cancel");
      var nextButton = buttonFactory.createNavigationButton("Next");
      var previousButton = buttonFactory.createNavigationButton("Previous");
      previousButton.addClickListener(listener-> dialog.previous());
      cancelButton.addClickListener(listener-> dialog.cancel());
      nextButton.addClickListener(listener -> dialog.next());
      var containerRight = new Div();
      containerRight.addClassNames("flex-horizontal", "gap-04");
      containerRight.add(cancelButton, nextButton);
      add(previousButton, containerRight);
    }
  }

  private static class LastFooter extends Div {

    public LastFooter(StepperDialog dialog) {
      addClassNames("flex-horizontal", "footer-intermediate");
      var buttonFactory = new ButtonFactory();
      var cancelButton = buttonFactory.createCancelButton("Cancel");
      var confirmButton = buttonFactory.createConfirmButton("Submit");
      var previousButton = buttonFactory.createNavigationButton("Previous");
      previousButton.addClickListener(listener-> dialog.previous());
      cancelButton.addClickListener(listener-> dialog.cancel());
      confirmButton.addClickListener(listener -> dialog.confirm());
      var containerRight = new Div();
      containerRight.addClassNames("flex-horizontal", "gap-04");
      containerRight.add(cancelButton, confirmButton);
      add(previousButton, containerRight);
    }
  }
}
