package life.qbic.projectmanagement.application.measurement;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b>Measurement Metadata Type Interface</b>
 *
 * @since 1.0.0
 */
public interface MeasurementMetadata {

  default Optional<String> assignedSamplePoolGroup() {
    return Optional.empty();
  }

  default SampleCode associatedSample() {
    return null;
  }

  default Optional<String> measurementIdentifier() {
    return Optional.empty();
  }

  MeasurementCode measurementCode();
}
