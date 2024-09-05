package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * Card Layout
 * <p>
 * Card styled Layout employed within all Components of the {@link Main} classes in the
 * {@link life.qbic.datamanager.views.login.LoginLayout}.
 * It's intended purpose is to give a harmonized style via css based on the native {@link Div}
 * for all components related to user login, registration and password reset.
 */
public class CardLayout extends Div {

  /**
   * Creates a new div with the given child components.
   *
   * @param components the child components
   */
  public CardLayout(Component... components) {
    super(components);
    addClassName("card-layout");
  }
}
