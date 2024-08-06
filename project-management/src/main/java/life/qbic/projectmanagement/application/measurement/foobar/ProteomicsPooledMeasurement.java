package life.qbic.projectmanagement.application.measurement.foobar;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.application.measurement.foobar.proteomics.ProteomicsSampleSpecificMetadata;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public final class ProteomicsPooledMeasurement implements
    PooledMeasurement<ProteomicsSampleSpecificMetadata> {

  private final SamplePool samplePool;

  public ProteomicsPooledMeasurement(SamplePool samplePool) {
    this.samplePool = samplePool;
  }

  @Override
  public List<ProteomicsSampleSpecificMetadata> getSampleSpecificMetadata() {
    return List.of();
  }

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
    return samplePool;
  }
}
