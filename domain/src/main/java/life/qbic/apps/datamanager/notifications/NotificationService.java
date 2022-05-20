package life.qbic.apps.datamanager.notifications;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import life.qbic.apps.datamanager.services.ServiceException;

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

  public void send(Notification notification) throws ServiceException {
    var messageParams =
        MessageParameters.durableTextParameters(notification.eventType, notification.notificationId, notification.occuredOn);

    String message = null;
    try {
      message = ObjectSerializer.instance().serialise(notification.event);
    } catch (IOException e) {
      throw new ServiceException("Notification was not send", e.getCause());
    }

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
