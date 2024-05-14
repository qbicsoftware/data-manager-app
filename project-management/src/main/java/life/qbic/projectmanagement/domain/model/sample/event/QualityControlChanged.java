package life.qbic.projectmanagement.domain.model.sample.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChangedEvent;

/**
 * <b>Quality Control Changed Event</b>
 * <p>
 * An event that indicates that a Quality Control object was added, deleted or changed in any way
 *
 * @since 1.0.0
 */
public class QualityControlChanged extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 365584699238819361L;

  private QualityControlChanged(ProjectId projectId) {
    super(projectId);
  }

  public static QualityControlChanged create(ProjectId projectId) {
    return new QualityControlChanged(projectId);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

}
