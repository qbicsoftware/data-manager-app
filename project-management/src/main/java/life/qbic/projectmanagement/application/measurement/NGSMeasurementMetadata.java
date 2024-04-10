package life.qbic.projectmanagement.application.measurement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b>NGS Measurement Metadata</b>
 * <p>
 * Indicating NGS measurement metadata registration request.
 *
 * @since 1.0.0
 */
public record NGSMeasurementMetadata(Collection<SampleCode> sampleCodes,
                                     String organisationId, String instrumentCURI, String facility,
                                     String sequencingReadType, String libraryKit, String flowCell,
                                     String sequencingRunProtocol, String samplePoolGroup,
                                     String indexI7, String indexI5,
                                     String comment) implements MeasurementMetadata {

  @Override
  public List<SampleCode> associatedSamples() {
    return sampleCodes.stream().toList();
  }

  @Override
  public Optional<String> assignedSamplePoolGroup() {
    return Optional.ofNullable(samplePoolGroup.isBlank() ? null : samplePoolGroup);
  }

  @Override
  public MeasurementCode measurementCode() {
    return null;
  }

  public static NGSMeasurementMetadata copyWithNewProperties(
      Collection<SampleCode> associatedSamples, String indexI7, String indexI5,
      NGSMeasurementMetadata metadata) {
    return new NGSMeasurementMetadata(
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
}
