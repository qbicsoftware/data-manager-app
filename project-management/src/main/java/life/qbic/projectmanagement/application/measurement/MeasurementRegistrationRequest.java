package life.qbic.projectmanagement.application.measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
  private final List<SampleCode> sampleCodes;

  public MeasurementRegistrationRequest(List<SampleCode> sampleCodes, T measurementMetadata) {
    this.measurementMetadata = Objects.requireNonNull(measurementMetadata);
    this.sampleCodes = sampleCodes.stream().toList();
  }

  public MeasurementRegistrationRequest(SampleCode sampleCode, T measurementMetadata) {
    this.measurementMetadata = Objects.requireNonNull(measurementMetadata);
    this.sampleCodes = new ArrayList<>();
    this.sampleCodes.add(sampleCode);
  }

  public List<SampleCode> associatedSamples() {
    return this.sampleCodes;
  }

  public T metadata() {
    return this.measurementMetadata;
  }

}
