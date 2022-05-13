package life.qbic.events;

import java.time.Instant;

/**
 * Interface for domain events. A domain event is defined as follows:
 * <p>
 *   Something happened that domain experts care about.
 * </p>
 * <p>
 * This interface does not provide any information but the instant of event occurrence. All other domain event information must be provided by the implementing classes not dependant of this interface.
 * </p>
 */
public interface DomainEvent {

  /**
   * The instant of event creation.
   * @return the instant the of event creation.
   */
  Instant occurredOn();
}
