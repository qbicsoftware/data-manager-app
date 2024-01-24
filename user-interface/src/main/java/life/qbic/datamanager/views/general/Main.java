package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import java.io.Serial;

/**
 * <b>Main component functions as a page to which the user can be guided</b>
 *
 * <p>Contains the {@link PageArea} containing the components,
 * which will be shown on the page and handles the routing logic and access rights.
 */
@com.vaadin.flow.component.Tag(Tag.DIV)
public abstract class Main extends Div implements BeforeEnterObserver, HasComponents,
    RouterLayout {

  @Serial
  private static final long serialVersionUID = 6764184508972422298L;

  protected Main() {
    addClassName("main");
  }

}
