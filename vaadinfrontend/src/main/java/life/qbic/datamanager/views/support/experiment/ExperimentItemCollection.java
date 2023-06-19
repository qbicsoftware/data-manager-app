package life.qbic.datamanager.views.support.experiment;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Tag(Tag.DIV)
public class ExperimentItemCollection extends Div {

  @Serial
  private static final long serialVersionUID = -2196400941684042549L;



  private ExperimentItemCollection() {
    layoutComponent();
  }

  private void layoutComponent() {
    addClassName("experiment-item-collection");
  }

  public static ExperimentItemCollection create() {
    return new ExperimentItemCollection();
  }

  public void addExperimentItem(ExperimentItem item) {
    add(item);
  }
}
