package life.qbic.projectmanagement.application.measurement;

import java.util.Collection;
import java.util.List;
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
                                            String samplePoolGroup, String facility,
                                            String fractionName,
                                            String digestionEnzyme,
                                            String digestionMethod, String enrichmentMethod,
                                            String injectionVolume, String lcColumn,
                                            String lcmsMethod, String labelingType, String label,
                                            String comment) implements MeasurementMetadata {

  @Override
  public Optional<String> assignedSamplePoolGroup() {
    return Optional.ofNullable(samplePoolGroup.isBlank() ? null : samplePoolGroup);
  }

  @Override
  public List<SampleCode> associatedSamples() {
    return sampleCodes.stream().toList();
  }

  public static ProteomicsMeasurementMetadata copyWithNewSamples(Collection<SampleCode> associatedSamples,
      ProteomicsMeasurementMetadata metadata) {
    return new ProteomicsMeasurementMetadata(
        associatedSamples.stream().toList(),
        metadata.organisationId(),
        metadata.instrumentCURI(),
        metadata.samplePoolGroup(),
        metadata.facility(),
        metadata.fractionName(),
        metadata.digestionEnzyme(),
        metadata.digestionMethod(),
        metadata.enrichmentMethod(),
        metadata.injectionVolume(),
        metadata.lcColumn(),
        metadata.lcmsMethod(),
        metadata.labelingType(),
        metadata.label(),
        metadata.comment());
  }
}
