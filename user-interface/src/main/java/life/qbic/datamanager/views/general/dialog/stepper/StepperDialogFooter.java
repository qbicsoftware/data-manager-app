package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.html.Div;
import java.util.Objects;
import life.qbic.datamanager.views.general.dialog.ButtonFactory;

/**
 * <b>Stepper Dialog Footer</b>
 *
 * <p>A more specialised footer compared to the
 * {@link life.qbic.datamanager.views.general.dialog.DialogFooter}</p>. This footer can be used in
 * the context of {@link StepperDialog} and implements the {@link NavigationListener} interface to
 * get notified about navigation changes in the stepper.
 *
 * @since 1.7.0
 */
public class StepperDialogFooter implements NavigationListener {

  private static final String FLEX_HORIZONTAL = "flex-horizontal";
  private static final String GAP_04 = "gap-04";
  private static final String CANCEL = "Cancel";

  private final StepperDialog dialog;

  private final FooterFactory footerFactory = new FooterFactory();

  private StepperDialogFooter(StepperDialog dialog) {
    this.dialog = dialog;
    dialog.registerNavigationListener(this);
    onNavigationChange(dialog.currentNavigation()); // we want to init the footer properly.
  }

  /**
   * Creates a {@link StepperDialogFooter} that wires to the provided {@link StepperDialog}. During
   * instantiation, the footer component of the {@link StepperDialog} is set automatically and also
   * the footer subscribes to navigation changes of the {@link StepperDialog}.
   *
   * @param dialog the stepper dialog to wire into
   * @return the fully set-up stepper dialog footer
   * @since 1.7.0
   */
  public static StepperDialogFooter with(StepperDialog dialog) {
    return new StepperDialogFooter(Objects.requireNonNull(dialog));
  }

  private static void updateFooter(StepperDialog dialog, Div footer) {
    dialog.setFooter(footer);
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

  @Override
  public void onNavigationChange(NavigationInformation navigationInformation) {
    int currentStep = navigationInformation.currentStep();
    int totalSteps = navigationInformation.totalSteps();
    if (isIntermediateStep(currentStep, totalSteps)) {
      updateFooter(dialog, footerFactory.createIntermediate(dialog));
      return;
    }
    if (currentStep == totalSteps) {
      updateFooter(dialog, footerFactory.createLast(dialog));
    } else {
      updateFooter(dialog, footerFactory.createFirst(dialog));
    }
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
      addClassNames(FLEX_HORIZONTAL, GAP_04, "footer");
      var buttonFactory = new ButtonFactory();
      var cancelButton = buttonFactory.createCancelButton(CANCEL);
      var nextButton = buttonFactory.createNavigationButton("Next");
      cancelButton.addClickListener(listener -> dialog.cancel());
      nextButton.addClickListener(listener -> dialog.next());
      add(cancelButton, nextButton);
    }
  }

  private static class IntermediateFooter extends Div {

    public IntermediateFooter(StepperDialog dialog) {
      addClassNames(FLEX_HORIZONTAL, "footer-intermediate");
      var buttonFactory = new ButtonFactory();
      var cancelButton = buttonFactory.createCancelButton(StepperDialogFooter.CANCEL);
      var nextButton = buttonFactory.createNavigationButton("Next");
      var previousButton = buttonFactory.createNavigationButton("Previous");
      previousButton.addClickListener(listener -> dialog.previous());
      cancelButton.addClickListener(listener -> dialog.cancel());
      nextButton.addClickListener(listener -> dialog.next());
      var containerRight = new Div();
      containerRight.addClassNames(FLEX_HORIZONTAL, GAP_04);
      containerRight.add(cancelButton, nextButton);
      add(previousButton, containerRight);
    }
  }

  private static class LastFooter extends Div {

    public LastFooter(StepperDialog dialog) {
      addClassNames(FLEX_HORIZONTAL, "footer-intermediate");
      var buttonFactory = new ButtonFactory();
      var cancelButton = buttonFactory.createCancelButton(StepperDialogFooter.CANCEL);
      var confirmButton = buttonFactory.createConfirmButton("Submit");
      var previousButton = buttonFactory.createNavigationButton("Previous");
      previousButton.addClickListener(listener -> dialog.previous());
      cancelButton.addClickListener(listener -> dialog.cancel());
      confirmButton.addClickListener(listener -> dialog.confirm());
      var containerRight = new Div();
      containerRight.addClassNames(FLEX_HORIZONTAL, GAP_04);
      containerRight.add(cancelButton, confirmButton);
      add(previousButton, containerRight);
    }
  }
}
