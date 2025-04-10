package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.notification.Notification;

/**
 * Former styled notification that is superseded by {@link Toast}.
 */
@Deprecated
public class StyledNotification extends Notification {

  public static final int DEFAULT_DURATION_MILLI_SECONDS = 3500;

  public StyledNotification(DisplayMessage displayMessage) {
    super(displayMessage);
    setDuration(DEFAULT_DURATION_MILLI_SECONDS);
    setPosition(Position.TOP_CENTER);
  }
}
