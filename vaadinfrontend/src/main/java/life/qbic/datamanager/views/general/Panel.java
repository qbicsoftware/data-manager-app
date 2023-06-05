package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import java.io.Serial;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
@com.vaadin.flow.component.Tag(Tag.DIV)
public class Panel extends Component implements HasComponents {

  @Serial
  private static final long serialVersionUID = 1081412760079026927L;

  public Panel() {
    this.addClassName("panel");
  }

}
