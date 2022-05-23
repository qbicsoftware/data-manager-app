package life.qbic.domain.events;

import java.io.Serializable;
import java.time.Instant;

/**
 * Interface for domain events. A domain event is defined as follows:
 * <p>
 * Something happened that domain experts care about.
 * </p>
 * <p>
 * This interface provides access to information about the event occurrence timepoint. All other domain event information must be provided by the implementing classes.
 * </p>
 */
public abstract class DomainEvent implements Serializable {

    /**
     * The instant of event creation.
     *
     * @return the instant the of event creation.
     */
    public abstract Instant occurredOn();
}
