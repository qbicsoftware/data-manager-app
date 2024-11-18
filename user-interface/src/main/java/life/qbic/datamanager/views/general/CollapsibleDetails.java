package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import java.util.Objects;

/**
 * <b>Collapsable Details</b>
 *
 * <p>Implementation of the {@link Collapsible} interface for the Vaadin component
 * {@link Details}.</p>
 * <p>
 * For the Vaadin {@link Details} component, it would not be necessary to provide a wrapper object. However
 * the interface gives a lot of flexibility to add collapsable elements wrapping other custom
 * components while exposing a unified behaviour.
 * <p>
 * Also we favor a more declarative and readable object API, like {@link #collapse()} or {@link #expand() } over
 * e.g. {@link Details#setOpened(boolean)}.
 *
 * @since 1.7.0
 */
public class CollapsibleDetails extends Div implements Collapsible  {

  private final Details details;

  public CollapsibleDetails(Details details) {
    this.details = Objects.requireNonNull(details);
    add(details);
  }

  @Override
  public void collapse() {
    this.details.setOpened(false);
  }

  @Override
  public void expand() {
    this.details.setOpened(true);
  }
}
