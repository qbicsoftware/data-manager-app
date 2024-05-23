package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import java.io.Serial;

/**
 * <b>A specific display area of the app that is neither header nor footer</b>
 *
 * <p>Contains main content OR support content (side-bar) of the respective page.</p>
 *
 * @since <1.0.0>
 */
@Tag(Tag.DIV)
public abstract class PageArea extends Component implements HasComponents {

  @Serial
  private static final long serialVersionUID = 4895835532001673549L;

  protected PageArea() {
    this.addClassName("page-area");
  }

}
