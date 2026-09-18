package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

/**
 * Base class for settings-related redirect views. Implementing views forward the navigation to a
 * concrete navigation target, which is used to converge redirect entry points and legacy URLs onto
 * the canonical settings routes.
 */
public abstract class ForwardingView extends Div implements BeforeEnterObserver {

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    event.forwardTo(navigationTarget());
  }

  /**
   * @return the navigation target the view should forward to
   */
  protected abstract Class<? extends Component> navigationTarget();
}