package life.qbic.apps.datamanager.notifications;

import java.time.Instant;

import life.qbic.domain.events.DomainEvent;

/**
 * <b>Notification</b>
 * <p>
 * This class is intended to ship {@link DomainEvent}s and provide quick access information
 * like the event type, the time-point of the event and a unique notification id.
 *
 * @since 1.0.0
 */
public class Notification {

    final String eventType;

    final Instant occuredOn;

    final String notificationId;

    final DomainEvent event;

    /**
     * Creates a new {@link Notification} instance.
     *
     * @param eventType      the event type
     * @param occuredOn      the time-point of the event
     * @param notificationId a unique notification id
     * @param event          the domain event
     * @return
     */
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
