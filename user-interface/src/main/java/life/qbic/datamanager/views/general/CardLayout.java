package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
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
