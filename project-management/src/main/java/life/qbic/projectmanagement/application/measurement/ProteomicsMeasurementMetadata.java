package life.qbic.projectmanagement.application.measurement;

import java.util.Collection;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b>Proteomics Measurement Metadata</b>
 * <p>
 * Indicating proteomics measurement metadata registration request.
 *
 * @since 1.0.0
 */
public record ProteomicsMeasurementMetadata(Collection<SampleCode> sampleCodes,
                                            String organisationId, String instrumentCURI,
                                            String samplePoolGroup) implements MeasurementMetadata {

  @Override
  public Optional<String> assignedSamplePoolGroup() {
    return Optional.ofNullable(samplePoolGroup.isBlank() ? null : samplePoolGroup);
  }

}
