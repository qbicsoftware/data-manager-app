package life.qbic.datamanager.views.projects.project.measurements.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;

/**
 * <b>Measurement Registration Processor NGS</b>
 * <p>
 * Processes
 * {@link MeasurementRegistrationInformationPxP}
 * and merges metadata entries that belong to the same measurement pool.
 *
 * @since 1.11.0
 */
public class MeasurementRegistrationProcessorPxP implements MeasurementProcessor<MeasurementRegistrationInformationPxP> {

  @Override
  public List<MeasurementRegistrationInformationPxP> process(
      List<MeasurementRegistrationInformationPxP> requests) {
    // 1. we want to aggregate measurement registration information that have the same sample pool name (we omit blank pool names)
    var measurementsBySamplePool = new HashMap<String, List<MeasurementRegistrationInformationPxP>>();
    var finalMeasurements = new ArrayList<MeasurementRegistrationInformationPxP>();
    for (var measurement : requests) {
      if (measurement.samplePoolGroup().isBlank()) {
        finalMeasurements.add(measurement);
      } else {
        measurementsBySamplePool.computeIfAbsent(measurement.samplePoolGroup(),
            k -> new ArrayList<>()).add(measurement);
      }
    }
    // 2. now we need to merge sample-specific metadata of the pooled measurements
    for (var entry : measurementsBySamplePool.entrySet()) {
      // every entry has the same pool name and by definition are only distinct in their specific metadata
      var specificMetadata = entry.getValue().stream()
          .flatMap(m -> m.specificMetadata().entrySet().stream())
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      var commonMetadata = entry.getValue().getFirst();
      var pooledMeasurement = new MeasurementRegistrationInformationPxP(
          commonMetadata.technicalReplicateName(),
          commonMetadata.organisationId(),
          commonMetadata.msDeviceCURIE(),
          commonMetadata.samplePoolGroup(),
          commonMetadata.facility(),
          commonMetadata.digestionEnzyme(),
          commonMetadata.digestionMethod(),
          commonMetadata.enrichmentMethod(),
          commonMetadata.injectionVolume(),
          commonMetadata.lcColumn(),
          commonMetadata.lcmsMethod(),
          commonMetadata.labelingType(),
          specificMetadata,
          commonMetadata.measurementName());
      finalMeasurements.add(pooledMeasurement);
    }

    return finalMeasurements;
  }
}
