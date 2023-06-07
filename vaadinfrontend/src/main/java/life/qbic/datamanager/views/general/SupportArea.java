package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import java.io.Serial;

/**
 * <b>A specific display area of the app that shows supporting information.</b>
 *
 * <p>Contains supporting content (side-bar) of the respective page. It does not contain the main content or header/footer information.</p>
 *
 * @since <1.0.0>
 */
@Tag(Tag.DIV)
public class SupportArea extends PageArea {

  @Serial
  private static final long serialVersionUID = 8725002232001994549L;

  public SupportArea() {
    super();
    this.addClassName("support-area");
  }

}
