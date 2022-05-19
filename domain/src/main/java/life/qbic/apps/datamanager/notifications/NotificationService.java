package life.qbic.apps.datamanager.notifications;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import life.qbic.apps.datamanager.notifications.Message.MessageParameters;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class NotificationService {

  private final MessageBusInterface messageBus;

  public NotificationService(MessageBusInterface messageBus) {
    this.messageBus = messageBus;
  }

  public void publish(Notification notification) {
    var messageParams =
        MessageParameters.durableTextParameters(notification.eventType, notification.notificationId, notification.occuredOn);

    var message = ObjectSerializer.instance().serialise(notification.event);

    messageBus.submit(message, messageParams);
  }

  static class ObjectSerializer {

    static ObjectSerializer instance;

    static ObjectSerializer instance() {
      if (instance == null) {
        instance = new ObjectSerializer();
      }
      return instance;
    }

    String serialise(Serializable object) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(object);
      oos.close();
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
  }

}
