package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b> An ErrorMessage component which shows an error message with a title and a detailed
 * description. </b>
 *
 * @since 1.0.0
 */
@Tag("error-message")
public class ErrorMessage extends DisplayMessage {

  public ErrorMessage(String titleText, String descriptionText) {
    super(titleText, descriptionText);
  }

  @Override
  protected void configureIcon() {
    messageIcon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O);
    messageIcon.addClassName("icon-s");
  }

  @Override
  protected void styleSpecificLayout() {
    super.getContent().addClassNames("p-s", "text-error", "bg-error-10", "rounded-l", "gap-y-s");
  }
}
