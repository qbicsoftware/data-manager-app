package life.qbic.datamanager.views.projects.project.measurements.processor;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificIP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationIP;

/**
 * <b>Measurement Update Processor IP</b>
 * <p>
 * Processes {@link MeasurementUpdateInformationIP} and merges metadata entries that belong to the
 * same measurement pool.
 *
 * @since 1.11.0
 */
class MeasurementUpdateProcessorIP implements MeasurementProcessor<MeasurementUpdateInformationIP> {

  private static final Logger log = logger(MeasurementUpdateProcessorIP.class);

  @Override
  public List<MeasurementUpdateInformationIP> process(
      List<MeasurementUpdateInformationIP> requests) {
    // 1. In the update use case we need to group measurements first by measurement ID, since multiple
    // measurements for the same sample ID can exist
    var measurementsById = new HashMap<String, List<MeasurementUpdateInformationIP>>();
    for (MeasurementUpdateInformationIP measurement : requests) {
      measurementsById.computeIfAbsent(measurement.measurementId(), k -> new ArrayList<>())
          .add(measurement);
    }
    var finalMeasurements = new ArrayList<MeasurementUpdateInformationIP>();
    // 2. Now we need to iterate through the measurements for every unique measurement ID
    // and process pooled measurements
    for (var entry : measurementsById.entrySet()) {
      var measurements = entry.getValue();

      var processedMeasurements = processPools(measurements);
      finalMeasurements.addAll(processedMeasurements);
    }
    return finalMeasurements;
  }


  private static List<MeasurementUpdateInformationIP> processPools(
      List<MeasurementUpdateInformationIP> measurements) {
    var finalMeasurements = new ArrayList<MeasurementUpdateInformationIP>();

    // Lookup table for aggregated measurements by pool name
    var measurementsBySamplePool = new HashMap<String, List<MeasurementUpdateInformationIP>>();

    for (var measurement : measurements) {
      if (measurement.samplePoolGroup() == null || measurement.samplePoolGroup().isBlank()) {
        // no need to further process
        finalMeasurements.add(measurement);
      } else {
        // store measurement for pool name
        measurementsBySamplePool.computeIfAbsent(measurement.samplePoolGroup(),
            k -> new ArrayList<>()).add(measurement);
      }
    }

    // We need to merge sample-specific metadata of the pooled measurements
    for (var pooledEntry : measurementsBySamplePool.entrySet()) {
      // Every entry has the same pool name and by definition are only distinct in their specific metadata
      Map<String, MeasurementSpecificIP> specificMetadata;
      try {
        specificMetadata = pooledEntry.getValue().stream()
            .flatMap(m -> m.specificMetadata().entrySet().stream())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      } catch (IllegalStateException e) {
        throw new ProcessingException("Preparation of specific measurement data failed.", e);
      }
      var commonMetadata = pooledEntry.getValue().getFirst();
      var pooledMeasurement = new MeasurementUpdateInformationIP(
          commonMetadata.measurementId(),
          commonMetadata.organisationId(),
          commonMetadata.instrumentCURIE(),
          commonMetadata.facility(),
          commonMetadata.samplePoolGroup(),
          specificMetadata,
          commonMetadata.measurementName());
      finalMeasurements.add(pooledMeasurement);
    }
    return finalMeasurements;
  }
}