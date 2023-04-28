package life.qbic.authentication.application.notification;

import java.time.Instant;
import life.qbic.domain.concepts.DomainEvent;

/**
 * <b>Notification</b>
 *
 * <p>This class is intended to ship {@link DomainEvent}s and provide quick access information like
 * the event type, the time-point of the event and a unique notification id.
 *
 * @since 1.0.0
 */
public class Notification {

  final String eventType;

  final Instant occurredOn;

  final String notificationId;

  final DomainEvent event;

  /**
   * Creates a new {@link Notification} instance.
   *
   * @param eventType      the event type
   * @param occurredOn     the time-point of the event
   * @param notificationId a unique notification id
   * @param event          the domain event
   * @return a notification with the arguments provided
   */
  public static Notification create(
      String eventType, Instant occurredOn, String notificationId, DomainEvent event) {
    return new Notification(eventType, occurredOn, notificationId, event);
  }

  protected Notification(
      String eventType, Instant occurredOn, String notificationId, DomainEvent event) {
    this.eventType = eventType;
    this.occurredOn = occurredOn;
    this.notificationId = notificationId;
    this.event = event;
  }
}
