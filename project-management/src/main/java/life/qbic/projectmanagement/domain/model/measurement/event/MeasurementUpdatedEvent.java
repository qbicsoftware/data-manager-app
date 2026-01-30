package life.qbic.projectmanagement.domain.model.measurement.event;

import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Indicates that a certain measurement's metadata has been updated.
 *
 * @since 1.0.0
 */
public class MeasurementUpdatedEvent extends DomainEvent {

  private final ProjectId projectId;
  private final MeasurementId measurementId;

  public MeasurementUpdatedEvent(ProjectId projectId, MeasurementId measurementId) {
    this.measurementId = Objects.requireNonNull(measurementId);
    this.projectId = Objects.requireNonNull(projectId);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public ProjectId projectId() {
    return projectId;
  }

  public MeasurementId measurementId() {
    return this.measurementId;
  }

}
