package life.qbic.datamanager.views.general.dialog.stepper;

import com.vaadin.flow.component.Component;
import life.qbic.datamanager.views.general.dialog.UserInput;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface Step {

  String name();

  Component component();

  UserInput userInput();
}
