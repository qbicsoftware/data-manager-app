package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.Component;
import life.qbic.datamanager.views.general.dialog.UserInput;

/**
 * <b>Step</b>
 *
 * <p>Used in the context of {@link StepperDialog}. Represents an individual step to be displayed
 * in a more complex user input scenario.</p>
 * <p>
 * A step carries three main building blocks essential for a {@link StepperDialog} to render its
 * step properly and include validation behaviour.
 * <p>
 * These are:
 *
 * <ul>
 *   <li>name - a short but precise name of the step</li>
 *   <li>component - the display component to be shown in the dialog</li>
 *   <li>userInput - the actual validation behaviour of the current step</li>
 * </ul>
 *
 * @since 1.7.0
 */
public interface Step {

  /**
   * The name of the current step.
   *
   * @since 1.7.0
   */
  String name();

  /**
   * The {@link Component} of the current step to be displayed in the {@link StepperDialog}.
   *
   * @since 1.7.0
   */
  Component component();

  /**
   * The {@link UserInput} that can be validated.
   *
   * @since 1.7.0
   */
  UserInput userInput();
}
