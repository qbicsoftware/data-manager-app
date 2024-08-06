package life.qbic.projectmanagement.application.measurement.foobar;

import java.time.Instant;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public sealed interface Measurement permits SingleSampleMeasurement, PooledMeasurement {

  MeasurementCode getIdentifier();

  MeasurementMetadata getMetadata();

  Optional<Instant> getRegistrationTime();

  MeasuredSample getMeasuredSample();

  default boolean isPooled() {
    return getMeasuredSample() instanceof SamplePool;
  }
}
