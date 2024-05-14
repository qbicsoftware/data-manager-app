package life.qbic.projectmanagement.domain.model.sample.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChangedEvent;

/**
 * <b>Sample Deleted - Domain Event</b>
 * <p>
 * A registered sample has been deleted
 *
 * @since 1.0.0
 */
public class SampleUpdated extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 6349640209226646506L;

  private SampleUpdated(ProjectId projectId) {
    super(projectId);
  }

  /**
   * Creates a new {@link SampleUpdated} object instance.
   *
   * @param projectId    the id of the project the udpated sample belongs to
   * @return a new instance of this domain event
   * @since 1.0.0
   */
  public static SampleUpdated create(ProjectId projectId) {
    return new SampleUpdated(projectId);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return this.occurredOn;
  }
}
