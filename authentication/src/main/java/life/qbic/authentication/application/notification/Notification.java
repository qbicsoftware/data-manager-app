package life.qbic.authentication.application.notification;

import com.fasterxml.jackson.annotation.JsonGetter;
import life.qbic.domain.concepts.DomainEvent;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * <b>Notification</b>
 *
 * <p>This class is intended to ship {@link DomainEvent}s and provide quick access information like
 * the event type, the time-point of the event and a unique notification id.
 *
 * @since 1.0.0
 */
public class Notification implements Serializable {

  @Serial
  private static final long serialVersionUID = -7295988841576228409L;
  private String eventType;

  private Instant occurredOn;

  private String notificationId;

  private DomainEvent event;

  /**
   * Creates a new {@link Notification} instance.
   *
   * @param eventType the event type
   * @param occurredOn the time-point of the event
   * @param notificationId a unique notification id
   * @param event the domain event
   * @return a notification with the arguments provided
   */
  public static Notification create(
      String eventType, Instant occurredOn, String notificationId, DomainEvent event) {
    return new Notification(eventType, occurredOn, notificationId, event);
  }

  private Notification() {

  }

  @JsonGetter("eventType")
  public String eventType() {
    return eventType;
  }

  @JsonGetter("occurredOn")
  public Instant occurredOn() {
    return occurredOn;
  }
  @JsonGetter("notificationId")
  public String notificationId() {
    return notificationId;
  }

  @JsonGetter("event")
  public DomainEvent event() {
    return event;
  }

  protected Notification(
      String eventType, Instant occurredOn, String notificationId, DomainEvent event) {
    this.eventType = eventType;
    this.occurredOn = occurredOn;
    this.notificationId = notificationId;
    this.event = event;
  }
}
