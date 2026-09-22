package life.qbic.projectmanagement.domain.model.sample.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Sample Registered - Domain Event</b>
 * <p>
 * A new physical sample has been registered and prepared for measurement in the lab.
 *
 * @since 1.0.0
 */
public class SampleRegistered extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 788473395775625302L;

  @JsonProperty("projectId")
  private final String projectId;

  @JsonProperty("experimentId")
  private final String experimentId;

  @JsonProperty("sampleId")
  private final SampleId registeredSample;

  private SampleRegistered(String projectId, String experimentId, SampleId registeredSample) {
    this.projectId = Objects.requireNonNull(projectId);
    this.experimentId = Objects.requireNonNull(experimentId);
    this.registeredSample = Objects.requireNonNull(registeredSample);
  }

  /**
   * Creates a new {@link SampleRegistered} object instance.
   *
   * @param projectId        the project reference the sample belongs to
   * @param experimentId     the experiment reference the sample belongs to
   * @param registeredSample the sample reference of the newly registered physical sample
   * @return a new instance of this domain event
   * @since 1.0.0
   */
  public static SampleRegistered create(String projectId, String experimentId,
      SampleId registeredSample) {
    return new SampleRegistered(projectId, experimentId, registeredSample);
  }

  @JsonGetter("projectId")
  public String projectId() {
    return this.projectId;
  }

  @JsonGetter("registeredSample")
  public SampleId registeredSample() {
    return this.registeredSample;
  }

  @JsonGetter("experimentId")
  public String experimentId() {
    return experimentId;
  }
}