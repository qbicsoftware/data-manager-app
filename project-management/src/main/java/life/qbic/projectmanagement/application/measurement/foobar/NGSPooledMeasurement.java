package life.qbic.projectmanagement.application.measurement.foobar;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.application.measurement.foobar.ngs.NGSSampleSpecificMetadata;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public final class NGSPooledMeasurement implements PooledMeasurement<NGSSampleSpecificMetadata> {

  private final MeasurementCode measurementCode;
  private final SamplePool measuredSample;

  public NGSPooledMeasurement(MeasurementCode measurementCode, SamplePool measuredSample) {
    this.measurementCode = measurementCode;
    this.measuredSample = measuredSample;
  }

  @Override
  public List<NGSSampleSpecificMetadata> getSampleSpecificMetadata() {
    return List.of();
  }

  @Override
  public MeasurementCode getIdentifier() {
    return measurementCode;
  }

  @Override
  public MeasurementMetadata getMetadata() {
    return null;
  }

  @Override
  public Optional<Instant> getRegistrationTime() {
    return Optional.empty();
  }

  @Override
  public MeasuredSample getMeasuredSample() {
    return measuredSample;
  }

}
