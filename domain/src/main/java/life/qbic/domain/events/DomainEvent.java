package life.qbic.domain.events;

import java.time.Instant;

/**
 * Interface for domain events. A domain event is defined as follows:
 * <p>
 *   Something happened that domain experts care about.
 * </p>
 * <p>
 * This interface provides access to information about the event occurrence timepoint. All other domain event information must be provided by the implementing class. 
 * </p>
 */
public interface DomainEvent {

  /**
   * The instant of event creation.
   * @return the instant the of event.
   */
  Instant occurredOn();
}
