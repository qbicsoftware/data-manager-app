package life.qbic.projectmanagement.application.api;

import java.util.Collection;
import java.util.Optional;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
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
                                     String comment) implements MeasurementMetadata,
    ValidationRequestBody {

  public static NGSMeasurementMetadata copyWithNewProperties(
      Collection<SampleCode> associatedSamples, String indexI7, String indexI5,
      NGSMeasurementMetadata metadata) {
    return new NGSMeasurementMetadata(
        metadata.measurementId(),
        associatedSamples.stream().toList(),
        metadata.organisationId(),
        metadata.instrumentCURI(),
        metadata.facility(),
        metadata.sequencingReadType(),
        metadata.libraryKit(),
        metadata.flowCell(),
        metadata.sequencingRunProtocol(),
        metadata.samplePoolGroup(),
        indexI7,
        indexI5,
        metadata.comment());
  }

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
