package life.qbic.projectmanagement.domain.model.sample.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Sample Updated - Domain Event</b>
 * <p>
 * A registered sample has been updated
 *
 * @since 1.0.0
 */
public class SampleUpdated extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 8134640947696646506L;
  @JsonProperty("sampleId")
  private final SampleId sampleID;

  private SampleUpdated(SampleId sampleID) {
    this.sampleID = Objects.requireNonNull(sampleID);
  }

  /**
   * Creates a new {@link SampleUpdated} object instance.
   *
   * @param sampleID the sample reference of that was updated
   * @return a new instance of this domain event
   * @since 1.0.0
   */
  public static SampleUpdated create(SampleId sampleID) {
    return new SampleUpdated(sampleID);
  }

  @JsonGetter("sampleID")
  public SampleId sampleId() {
    return this.sampleID;
  }
}
