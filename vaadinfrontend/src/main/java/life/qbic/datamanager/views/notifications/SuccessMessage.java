package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b> A SuccessMessage component which shows a success message with a title and a detailed
 * description. </b>
 *
 * @since 1.0.0
 */
@Tag("success-message")
public class SuccessMessage extends DisplayMessage {

  public SuccessMessage(String titleText, String descriptionText) {
    super(titleText, descriptionText);
  }

  @Override
  protected void configureIcon() {
    messageIcon = new Icon(VaadinIcon.CHECK_CIRCLE_O);
    messageIcon.addClassName("icon-s");
  }

  @Override
  protected void styleSpecificLayout() {
    super.getContent()
        .addClassNames("p-s", "text-success", "bg-success-10", "rounded-l", "gap-y-s");
  }
}
