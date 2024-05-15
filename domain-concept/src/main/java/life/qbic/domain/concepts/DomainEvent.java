package life.qbic.domain.concepts;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serializable;
import java.time.Instant;

/**
 * Interface for domain events. A domain event is defined as follows:
 *
 * <p>Something happened that domain experts care about.
 *
 * <p>This interface provides access to information about the event occurrence timepoint. All other
 * domain event information must be provided by the implementing classes.
 */
public abstract class DomainEvent implements Serializable {

  protected final Instant occurredOn;

  protected DomainEvent() {
    this.occurredOn = Instant.now();
  }

  /**
   * The instant of event creation.
   *
   * @return the instant the of event creation.
   */

  @JsonGetter("occurredOn")
  public Instant occurredOn() {
    return occurredOn;
  }

}
