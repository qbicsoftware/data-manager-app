package life.qbic.datamanager.views.projects.project.measurements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class MeasurementPresenter {

  private final SampleInformationService sampleInformationService;

  public MeasurementPresenter(@Autowired SampleInformationService sampleInformationService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
  }

  private static ProteomicsMeasurementEntry convert(ProteomicsMeasurement measurement,
      SampleInformation sampleInfo) {
    return new ProteomicsMeasurementEntry(measurement.measurementCode().value(),
        sampleInfo, measurement.organisation().IRI(), measurement.organisation().label(),
        measurement.instrument().getName().replace("_", ":"),
        measurement.instrument().getLabel(),
        measurement.samplePoolGroup().orElse(""), measurement.facility(), measurement.fraction().orElse(""),
        measurement.digestionEnzyme(), measurement.digestionMethod(),
        measurement.enrichmentMethod(), String.valueOf(measurement.injectionVolume()),
        measurement.lcColumn(), measurement.lcmsMethod(), measurement.labelingType().orElse(""),
        measurement.label().orElse(""), measurement.comment().orElse(""));
  }

  public List<ProteomicsMeasurementEntry> expandPools(ProteomicsMeasurement proteomicsMeasurement) {
    List<ProteomicsMeasurementEntry> expandedEntries = new ArrayList<>();
    for (SampleId sampleId : proteomicsMeasurement.measuredSamples()) {
      var sampleInfo = sampleInformationService.findSample(sampleId)
          .map(sample -> new SampleInformation(sample.sampleCode().code(), sample.label()))
          .orElse(new SampleInformation("", ""));
      expandedEntries.add(convert(proteomicsMeasurement, sampleInfo));
    }
    return expandedEntries;
  }

}
