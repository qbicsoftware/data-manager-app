package life.qbic.projectmanagement.application.measurement.foobar.ngs;

import java.time.Instant;
import java.util.Optional;
import life.qbic.projectmanagement.application.measurement.foobar.MeasuredSample;
import life.qbic.projectmanagement.application.measurement.foobar.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.foobar.SingleSampleMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class NGSSingleSampleMeasurement implements SingleSampleMeasurement {

  @Override
  public MeasurementCode getIdentifier() {
    return null;
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
    return null;
  }

  @Override
  public SampleId measuredSample() {
    return null;
  }
}
