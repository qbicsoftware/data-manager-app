package life.qbic.projectmanagement.application.measurement;

import java.util.Collection;
import java.util.List;
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
                                     String sequencingRunProtocol, String indexI7, String indexI5,
                                     String comment) implements MeasurementMetadata {

  @Override
  public List<SampleCode> associatedSamples() {
    return sampleCodes.stream().toList();
  }

  public static NGSMeasurementMetadata copyWithNewProperties(
      Collection<SampleCode> associatedSamples,
      NGSMeasurementMetadata metadata) {
    return new NGSMeasurementMetadata(
        associatedSamples.stream().toList(),
        metadata.organisationId(),
        metadata.facility(),
        metadata.instrumentCURI(),
        metadata.sequencingReadType(),
        metadata.libraryKit(),
        metadata.flowCell(),
        metadata.sequencingRunProtocol(),
        metadata.indexI7(),
        metadata.indexI5(),
        metadata.comment());
  }
}
