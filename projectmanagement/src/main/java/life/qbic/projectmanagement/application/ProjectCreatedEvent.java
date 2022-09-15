package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.UUID;
import life.qbic.projectmanagement.domain.ProjectId;

/**
 * An integration event indicating successful project creation.
 */
public record ProjectCreatedEvent(UUID eventId, String projectId, Instant occurredOn) {

  public ProjectCreatedEvent(ProjectId projectId) {
    this(UUID.randomUUID(), projectId.exportToString(), Instant.now());
  }

  public ProjectCreatedEvent {
    requireNonNull(eventId);
    requireNonNull(projectId);
    requireNonNull(occurredOn);
  }
}
