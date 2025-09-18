package life.qbic.projectmanagement.application.measurement;

import java.util.Optional;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b>Proteomics Measurement Metadata</b>
 * <p>
 * Indicating proteomics measurement metadata registration request.
 *
 * @since 1.0.0
 */
public record ProteomicsMeasurementMetadata(
    String measurementId,
    String measurementName,
    SampleCode sampleCode,
    String technicalReplicateName,
    String organisationId,
    String msDeviceCURIE,
    String samplePoolGroup,
    String facility,
    String fractionName,
    String digestionEnzyme,
    String digestionMethod,
    String enrichmentMethod,
    String injectionVolume,
    String lcColumn,
    String lcmsMethod,
    Labeling labeling,
    String comment
) implements MeasurementMetadata {


  @Override
  public Optional<String> assignedSamplePoolGroup() {
    return Optional.ofNullable(samplePoolGroup.isBlank() ? null : samplePoolGroup);
  }

  public Optional<String> measurementIdentifier() {
    return Optional.ofNullable(measurementId.isBlank() ? null : measurementId);
  }

  @Override
  public SampleCode associatedSample() {
    return sampleCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProteomicsMeasurementMetadata that)) {
      return false;
    }

    return measurementId.equals(that.measurementId);
  }

  @Override
  public int hashCode() {
    return measurementId.hashCode();
  }

}
