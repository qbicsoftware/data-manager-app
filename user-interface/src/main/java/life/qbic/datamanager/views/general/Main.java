package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;

/**
 * <b>Main component functions as a page to which the user can be guided</b>
 *
 * <p>Contains the {@link PageArea} containing the components,
 * which will be shown on the page and handles the routing logic and access rights.
 */
@com.vaadin.flow.component.Tag(Tag.DIV)
public abstract class Main extends Div {

  private final Span title = new Span();
  @Serial
  private static final long serialVersionUID = 6764184508972422298L;

  protected Main() {
    addComponentAsFirst(title);
    title.addClassName("main-title");
    addClassName("main");
  }

  public void setTitle(String title) {
    this.title.setText(title);
  }

}
