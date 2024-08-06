package life.qbic.projectmanagement.application.measurement.foobar;

import java.util.List;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public sealed interface PooledMeasurement<T extends SampleSpecificMeasurementMetadata> extends
    Measurement, HasSampleSpecificMetadata<T> permits NGSPooledMeasurement,
    ProteomicsPooledMeasurement {


  default List<SampleId> measuredSamples() {
    return getSampleSpecificMetadata().stream().map(SampleSpecificMeasurementMetadata::sampleId)
        .toList();
  }

}
