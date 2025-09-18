package life.qbic.projectmanagement.application.measurement;

import java.util.Collection;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b>NGS Measurement Metadata</b>
 * <p>
 * Indicating NGS measurement metadata registration request.
 *
 * @since 1.0.0
 */
public record NGSMeasurementMetadata(String measurementId, Collection<SampleCode> sampleCodes,
                                     String organisationId, String instrumentCURI, String facility,
                                     String sequencingReadType, String libraryKit, String flowCell,
                                     String sequencingRunProtocol, String samplePoolGroup,
                                     String indexI7, String indexI5,
                                     String comment) implements MeasurementMetadata {

  @Override
  public SampleCode associatedSample() {
    return sampleCodes.iterator().next();
  }

  @Override
  public Optional<String> assignedSamplePoolGroup() {
    return Optional.ofNullable(samplePoolGroup.isBlank() ? null : samplePoolGroup);
  }

  public Optional<String> measurementIdentifier() {
    return Optional.ofNullable(measurementId.isBlank() ? null : measurementId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NGSMeasurementMetadata that)) {
      return false;
    }

    return measurementId.equals(that.measurementId);
  }

  @Override
  public int hashCode() {
    return measurementId.hashCode();
  }
}
