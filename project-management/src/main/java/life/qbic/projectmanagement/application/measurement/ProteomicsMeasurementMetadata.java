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
public record ProteomicsMeasurementMetadata(String measurementId,
                                            Collection<SampleCode> sampleCodes,
                                            String organisationId, String instrumentCURI,
                                            String samplePoolGroup, String facility,
                                            String fractionName,
                                            String digestionEnzyme,
                                            String digestionMethod, String enrichmentMethod,
                                            String injectionVolume, String lcColumn,
                                            String lcmsMethod, Collection<Labeling> labeling,
                                            String comment) implements MeasurementMetadata {


  public static ProteomicsMeasurementMetadata copyWithNewProperties(Collection<SampleCode> associatedSamples, Collection<Labeling> labeling,
      ProteomicsMeasurementMetadata metadata) {
    return new ProteomicsMeasurementMetadata(metadata.measurementId(),
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
        labeling,
        metadata.comment());
  }

  @Override
  public Optional<String> assignedSamplePoolGroup() {
    return Optional.ofNullable(samplePoolGroup.isBlank() ? null : samplePoolGroup);
  }

  public Optional<String> measurementIdentifier() {
    return Optional.ofNullable(measurementId.isBlank() ? null : measurementId);
  }

  @Override
  public List<SampleCode> associatedSamples() {
    return sampleCodes.stream().toList();
  }
}
