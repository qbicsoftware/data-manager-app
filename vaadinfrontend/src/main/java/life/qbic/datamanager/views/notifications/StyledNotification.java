package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class StyledNotification {

  public static final int DEFAULT_DURATION_MILLI_SECONDS = 20000;
  private final DisplayMessage displayMessage;


  public StyledNotification(DisplayMessage displayMessage) {
    this.displayMessage = displayMessage;
  }

  public void open() {
    Notification notification = new Notification(displayMessage);
    notification.setDuration(DEFAULT_DURATION_MILLI_SECONDS);
    notification.setPosition(Position.TOP_CENTER);
    notification.open();
  }


}
