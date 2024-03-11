package life.qbic.projectmanagement.application.measurement;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
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

  public ProteomicsMeasurementMetadata merge(ProteomicsMeasurementMetadata other) {
    return ProteomicsMeasurementMetadata.merge(this, other);
  }

  public static ProteomicsMeasurementMetadata merge(ProteomicsMeasurementMetadata one,
      ProteomicsMeasurementMetadata two) {
    if (one == null && two == null) {
      return null;
    }
    if (one == null) {
      return two;
    }
    if (two == null) {
      return one;
    }

    if (!Objects.equals(one.organisationId, two.organisationId)) {
      throw new RuntimeException("Could not merge. Different Organisation.");
    }
    var organisationId = one.organisationId;

    if (!Objects.equals(one.instrumentCURI, two.instrumentCURI)) {
      throw new RuntimeException("Could not merge. Different Instrument.");
    }
    var instrumentCURI = one.instrumentCURI;

    if (!Objects.equals(one.samplePoolGroup, two.samplePoolGroup)) {
      throw new RuntimeException("Could not merge. Different sample pool group.");
    }
    var samplePoolGroup = one.samplePoolGroup;

    if (!Objects.equals(one.facility, two.facility)) {
      throw new RuntimeException("Could not merge. Different facility.");
    }
    var facility = one.facility;

    if (!Objects.equals(one.fractionName, two.fractionName)) {
      throw new RuntimeException("Could not merge. Different fraction name.");
    }
    var fractionName = one.fractionName;

    if (!Objects.equals(one.digestionEnzyme, two.digestionEnzyme)) {
      throw new RuntimeException("Could not merge. Different digestion enzyme.");
    }
    var digestionEnzyme = one.digestionEnzyme;

    if (!Objects.equals(one.digestionMethod, two.digestionMethod)) {
      throw new RuntimeException("Could not merge. Different digestion enzyme.");
    }
    var digestionMethod = one.digestionMethod;

    if (!Objects.equals(one.enrichmentMethod, two.enrichmentMethod)) {
      throw new RuntimeException("Could not merge. Different enrichment method.");
    }
    var enrichmentMethod = one.enrichmentMethod;

    if (!Objects.equals(one.injectionVolume, two.injectionVolume)) {
      throw new RuntimeException("Could not merge. Different injection volume.");
    }
    var injectionVolume = one.injectionVolume;

    if (!Objects.equals(one.lcColumn, two.lcColumn)) {
      throw new RuntimeException("Could not merge. Different LC column.");
    }
    var lcColumn = one.lcColumn;

    if (!Objects.equals(one.lcmsMethod, two.lcmsMethod)) {
      throw new RuntimeException("Could not merge. Different LCMS method.");
    }
    var lcmsMethod = one.lcmsMethod;

    if (!Objects.equals(one.labelingType, two.labelingType)) {
      throw new RuntimeException("Could not merge. Different labeling type.");
    }
    var labelingType = one.labelingType;

    if (!Objects.equals(one.label, two.label)) {
      throw new RuntimeException("Could not merge. Different label.");
    }
    var label = one.label;

    if (!Objects.equals(one.comment, two.comment)) {
      //maybe merge comments as well?
      throw new RuntimeException("Could not merge. Different comments.");
    }
    var comment = one.comment;

    var mergedSampleCodes = Stream.of(one.sampleCodes(), two.sampleCodes())
        .flatMap(Collection::stream)
        .toList();
    return new ProteomicsMeasurementMetadata(mergedSampleCodes,
        organisationId,
        instrumentCURI,
        samplePoolGroup,
        facility,
        fractionName,
        digestionEnzyme,
        digestionMethod,
        enrichmentMethod,
        injectionVolume,
        lcColumn,
        lcmsMethod,
        labelingType,
        label,
        comment
    );
  }
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
