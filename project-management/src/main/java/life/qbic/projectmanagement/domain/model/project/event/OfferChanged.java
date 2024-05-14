package life.qbic.projectmanagement.domain.model.project.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Offer File Changed Event</b>
 * <p>
 * An event that indicates that a Offer object was uploaded, deleted or changed in any way
 *
 * @since 1.0.0
 */
public class OfferChanged extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 365584984568819361L;

  private OfferChanged(ProjectId projectId) {
    super(projectId);
  }

  public static OfferChanged create(ProjectId projectId) {
    return new OfferChanged(projectId);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

}
