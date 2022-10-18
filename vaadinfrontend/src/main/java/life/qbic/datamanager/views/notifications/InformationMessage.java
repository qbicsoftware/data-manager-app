package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;


/**
 * <b> A InformationMessage component which shows a primarily styled information message with a
 * title and a detailed description. </b>
 *
 * @since 1.0.0
 */

@Tag("information-message")
public class InformationMessage extends DisplayMessage {

  public InformationMessage(String titleText, String descriptionText) {
    super(titleText, descriptionText);
  }

  @Override
  protected void configureIcon() {
    messageIcon = new Icon(VaadinIcon.INFO_CIRCLE_O);
    messageIcon.addClassName("icon-s");
  }

  @Override
  protected void styleSpecificLayout() {
    getContent()
        .addClassNames("p-s", "text-primary", "bg-primary-10", "rounded-l", "gap-y-s");
  }
}
