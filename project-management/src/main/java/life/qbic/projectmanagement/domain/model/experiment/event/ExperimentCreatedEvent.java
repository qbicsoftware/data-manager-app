package life.qbic.projectmanagement.domain.model.experiment.event;

import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * Indicates that an experiment has been created.
 *
 * @since 1.0.0
 */
public class ExperimentCreatedEvent extends DomainEvent {

  private final ExperimentId experimentId;

  public ExperimentCreatedEvent(ExperimentId experimentId) {
    this.experimentId = Objects.requireNonNull(experimentId);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public ExperimentId experimentId() {
    return this.experimentId;
  }

}
