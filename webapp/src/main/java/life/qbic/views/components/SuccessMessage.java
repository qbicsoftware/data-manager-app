package life.qbic.views.components;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b> A SuccessMessage component which shows a successful message with a title and a detailed
 * description. </b>
 *
 * @since 1.0.0
 */
public class SuccessMessage extends DisplayMessage {

  public SuccessMessage(String titleText, String descriptionText) {
    super(titleText, descriptionText);
  }

  @Override
  protected void configureIcon() {
    messageIcon = new Icon(VaadinIcon.CHECK_CIRCLE);
    messageIcon.addClassName("icon-s");
  }

  @Override
  protected void styleSpecificLayout() {
    getContent()
        .addClassNames("p-s", "text-secondary", "bg-primary-10", "rounded-l", "gap-y-s");
  }
}
