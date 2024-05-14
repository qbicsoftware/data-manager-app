package life.qbic.projectmanagement.domain.model.project.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Interface for domain events that change projects:
 *
 * <p>Something happened and this should be reflected in the project (modification data)
 *
 * <p>This interface provides access to information about the project and (through extension) the
 * event occurrence timepoint. All other domain event information must be provided by the
 * implementing classes.
 */
public abstract class ProjectChangedEvent extends DomainEvent {

  private final ProjectId projectId;

  protected ProjectChangedEvent(ProjectId projectId) {
    super();
    this.projectId = Objects.requireNonNull(projectId);
  }

  /**
   * The identifier of the project that changed
   *
   * @return the project ID
   */
  @JsonGetter("projectId")
  public ProjectId projectId() {
    return projectId;
  }
}
