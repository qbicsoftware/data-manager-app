package life.qbic.datamanager.views.projects.project.measurements.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationIP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificIP;

/**
 * <b>Measurement Registration Processor Immunopeptidomics</b>
 * <p>
 * Processes {@link MeasurementRegistrationInformationIP} and merges metadata entries that belong
 * to the same measurement pool.
 *
 * @since 1.11.0
 */
class MeasurementRegistrationProcessorIP implements
    MeasurementProcessor<MeasurementRegistrationInformationIP> {

  @Override
  public List<MeasurementRegistrationInformationIP> process(
      List<MeasurementRegistrationInformationIP> requests) {
    // 1. we want to aggregate measurement registration information that have the same sample pool name
    var measurementsBySamplePool = new HashMap<String, List<MeasurementRegistrationInformationIP>>();
    var finalMeasurements = new ArrayList<MeasurementRegistrationInformationIP>();

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
      Map<String, MeasurementSpecificIP> specificMetadata;
      try {
        specificMetadata = entry.getValue().stream()
            .flatMap(m -> m.specificMetadata().entrySet().stream())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      } catch (IllegalStateException e) {
        throw new ProcessingException("Preparation of specific measurement data failed.", e);
      }
      var commonMetadata = entry.getValue().getFirst();
      var pooledMeasurement = new MeasurementRegistrationInformationIP(
          commonMetadata.organisationId(),
          commonMetadata.instrumentCURIE(),
          commonMetadata.instrumentName(),
          commonMetadata.facility(),
          commonMetadata.samplePoolGroup(),
          specificMetadata,
          commonMetadata.measurementName()
      );
      finalMeasurements.add(pooledMeasurement);
    }

    return finalMeasurements;
  }
}
