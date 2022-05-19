package life.qbic.domain.events;

import java.io.Serializable;

/**
 * A domain event that can be serialized.
 *
 * @since 1.0.0
 */
public abstract class SerializableDomainEvent implements DomainEvent, Serializable {

}
