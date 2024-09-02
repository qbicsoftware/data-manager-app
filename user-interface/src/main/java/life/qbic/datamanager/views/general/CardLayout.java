package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * Card Layout
 * <p>
 * Card styled Layout employed within all {@link Main} in the
 * {@link life.qbic.datamanager.views.login.LoginLayout}
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
