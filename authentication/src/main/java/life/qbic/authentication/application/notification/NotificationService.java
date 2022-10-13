package life.qbic.authentication.application.notification;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.UUID;
import life.qbic.authentication.application.ServiceException;
import life.qbic.broadcasting.MessageBusSubmission;
import life.qbic.broadcasting.MessageParameters;

/**
 * <b>Notification Service</b>
 *
 * <p>A service to send notifications out to broadcast important business events.
 *
 * @since 1.0.0
 */
public class NotificationService {

  private final MessageBusSubmission messageBus;

  /**
   * Creates a new notification service. The service will prepare all incoming notifications and
   * send them to the {@link MessageBusSubmission}.
   *
   * @param messageBus an implementation of the {@link MessageBusSubmission}
   */
  public NotificationService(MessageBusSubmission messageBus) {
    this.messageBus = messageBus;
  }

  /**
   * Sends the notification out via the {@link MessageBusSubmission}.
   *
   * @param notification the notification of interest to send out
   * @throws ServiceException if the submission to the messaging bus failed.
   */
  public void send(Notification notification) throws ServiceException {
    var messageParams =
        MessageParameters.durableTextParameters(
            notification.eventType, notification.notificationId, notification.occurredOn);

    String message;
    try {
      message = ObjectSerializer.instance().serialise(notification.event);
    } catch (IOException e) {
      throw new ServiceException("Notification was not send", e.getCause());
    }

    messageBus.submit(message, messageParams);
  }

  /**
   * Generates a unique notification id.
   *
   * @return a String representation of a new notification id.
   */
  public String newNotificationId() {
    return UUID.randomUUID().toString();
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
