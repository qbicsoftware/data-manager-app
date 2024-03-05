package life.qbic.projectmanagement.application.measurement;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b>Measurement Registration Request</b>
 *
 * <p>A request for measurement metadata registration for a {@link MeasurementMetadata} type.</p>
 *
 * @since 1.0.0
 */
public class MeasurementRegistrationRequest<T extends MeasurementMetadata> {

  private final T measurementMetadata;

  private final ExperimentId experimentId;

  public MeasurementRegistrationRequest(T measurementMetadata,
      ExperimentId experimentId) {
    this.measurementMetadata = Objects.requireNonNull(measurementMetadata);
    this.experimentId = experimentId;
  }

  public List<SampleCode> associatedSamples() {
    return this.measurementMetadata.associatedSamples();
  }

  public T metadata() {
    return this.measurementMetadata;
  }

  public ExperimentId experimentId() {
    return experimentId;
  }
}
