package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 */
@Tag(Tag.DIV)
public class Card extends Component implements HasComponents {

  @Serial
  private static final long serialVersionUID = -3312674361164160926L;

  public Card() {
    this.addClassName("card");
  }


}
