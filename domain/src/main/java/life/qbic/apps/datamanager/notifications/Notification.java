package life.qbic.apps.datamanager.notifications;

import java.time.Instant;
import life.qbic.domain.events.DomainEvent;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Notification {

  final String eventType;

  final Instant occuredOn;

  final String notificationId;

  final DomainEvent event;

  public static Notification create(String eventType, Instant occuredOn, String notificationId, DomainEvent event) {
    return new Notification(eventType, occuredOn, notificationId, event);
  }

  protected Notification(String eventType, Instant occuredOn, String notificationId, DomainEvent event) {
    this.eventType = eventType;
    this.occuredOn = occuredOn;
    this.notificationId = notificationId;
    this.event = event;
  }


}
