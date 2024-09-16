package life.qbic.datamanager.views.projects.project.measurements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.measurement.NGSIndex;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.NGSSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Measurement presenter</b>
 * <p>
 * Some helper methods to render {@link ProteomicsMeasurement} information.
 *
 * @since 1.0.0
 */
@Component
public class MeasurementPresenter {

  private final SampleInformationService sampleInformationService;

  public MeasurementPresenter(@Autowired SampleInformationService sampleInformationService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
  }

  private static ProteomicsMeasurementEntry convertProteomicsMeasurement(
      ProteomicsMeasurement measurement,
      SampleInformation sampleInfo,
      ProteomicsSpecificMeasurementMetadata specificMeasurementMetadata) {
    return new ProteomicsMeasurementEntry(measurement.measurementCode().value(),
        sampleInfo,
        measurement.technicalReplicateName().orElse(""),
        measurement.organisation().IRI(),
        measurement.organisation().label(),
        measurement.msDevice().getOboId().replace("_", ":"),
        measurement.msDevice().getLabel(),
        measurement.samplePoolGroup().orElse(""),
        measurement.facility(),
        specificMeasurementMetadata.fractionName(),
        measurement.digestionEnzyme(),
        measurement.digestionMethod(),
        measurement.enrichmentMethod(),
        String.valueOf(measurement.injectionVolume()),
        measurement.lcColumn(),
        measurement.lcmsMethod(),
        measurement.labelType(),
        specificMeasurementMetadata.label(),
        "");
  }

  private static NGSMeasurementEntry convertNGSMeasurement(NGSMeasurement measurement,
      SampleInformation sampleInfo, NGSSpecificMeasurementMetadata specificMeasurementMetadata) {
    return new NGSMeasurementEntry(measurement.measurementCode().value(),
        sampleInfo, measurement.organisation().IRI(), measurement.organisation().label(),
        measurement.instrument().getOboId().replace("_", ":"),
        measurement.instrument().getLabel(),
        measurement.samplePoolGroup().orElse(""), measurement.facility(),
        measurement.sequencingReadType(),
        measurement.libraryKit().orElse(""), measurement.flowCell().orElse(""),
        measurement.sequencingRunProtocol().orElse(""),
        specificMeasurementMetadata.index().orElse(new NGSIndex("", "")).indexI7(),
        specificMeasurementMetadata.index().orElse(new NGSIndex("", "")).indexI5(),
        specificMeasurementMetadata.comment().orElse(""));
  }

  public List<ProteomicsMeasurementEntry> expandProteomicsPools(
      ProteomicsMeasurement proteomicsMeasurement) {
    List<ProteomicsMeasurementEntry> expandedEntries = new ArrayList<>();
    for (SampleId sampleId : proteomicsMeasurement.measuredSamples()) {
      var sampleInfo = sampleInformationService.findSample(sampleId)
          .map(sample -> new SampleInformation(sample.sampleCode().code(), sample.label()))
          .orElse(new SampleInformation("", ""));
      var specificMetadata = proteomicsMeasurement.specificMetadata().stream()
          .filter(metadata -> metadata.measuredSample().equals(sampleId)).findFirst().orElse(null);
      expandedEntries.add(
          convertProteomicsMeasurement(proteomicsMeasurement, sampleInfo, specificMetadata));
    }
    return expandedEntries;
  }

  public List<NGSMeasurementEntry> expandNGSPools(NGSMeasurement ngsMeasurement) {
    List<NGSMeasurementEntry> expandedEntries = new ArrayList<>();
    for (NGSSpecificMeasurementMetadata specificMeasurementMetadata : ngsMeasurement.specificMeasurementMetadata()) {
      var sampleInfo = sampleInformationService.findSample(
              specificMeasurementMetadata.measuredSample())
          .map(sample -> new SampleInformation(sample.sampleCode().code(),
              sample.label())).orElse(new SampleInformation("", ""));
      expandedEntries.add(
          convertNGSMeasurement(ngsMeasurement, sampleInfo, specificMeasurementMetadata));
    }
    return expandedEntries;
  }
}
