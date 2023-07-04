package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.notification.Notification;

/**
 * <b>short description</b>
 * TODO
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class StyledNotification extends Notification {

  public static final int DEFAULT_DURATION_MILLI_SECONDS = 3500;

  public StyledNotification(DisplayMessage displayMessage) {
    super(displayMessage);
    setDuration(DEFAULT_DURATION_MILLI_SECONDS);
    setPosition(Position.TOP_CENTER);
  }
}
