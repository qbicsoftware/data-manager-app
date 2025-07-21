package life.qbic.datamanager.views.projects.project.measurements.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationNGS;

/**
 * <b>Measurement Update Processor NGS</b>
 * <p>
 * Processes {@link MeasurementUpdateInformationNGS}
 * and merges metadata entries that belong to the same measurement pool.
 *
 * @since 1.11.0
 */
public class MeasurementUpdateProcessorNGS implements MeasurementProcessor<MeasurementUpdateInformationNGS> {

  @Override
  public List<MeasurementUpdateInformationNGS> process(
      List<MeasurementUpdateInformationNGS> requests) {
    // 1. In the update use case we need to group measurements first by measurement ID, since multiple
    // measurements for the same sample ID can exist
    var measurementsById = new HashMap<String, List<MeasurementUpdateInformationNGS>>();
    for (MeasurementUpdateInformationNGS measurement : requests) {
      measurementsById.computeIfAbsent(measurement.measurementId(), k -> new ArrayList<>())
          .add(measurement);
    }
    var finalMeasurements = new ArrayList<MeasurementUpdateInformationNGS>();
    // 2. Now we need to iterate through the measurements for every unique measurement ID
    // and process pooled measurements
    for (var entry : measurementsById.entrySet()) {
      var measurements = entry.getValue();

      var processedMeasurements = processPools(measurements);
      finalMeasurements.addAll(processedMeasurements);
    }
    return finalMeasurements;
  }


  private static List<MeasurementUpdateInformationNGS> processPools(
      List<MeasurementUpdateInformationNGS> measurements) {
    var finalMeasurements = new ArrayList<MeasurementUpdateInformationNGS>();

    // Lookup table for aggregated measurements by pool name
    var measurementsBySamplePool = new HashMap<String, List<MeasurementUpdateInformationNGS>>();

    for (var measurement : measurements) {
      if (measurement.samplePoolGroup().isBlank()) {
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
      var specificMetadata = pooledEntry.getValue().stream()
          .flatMap(m -> m.specificMetadata().entrySet().stream())
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      var commonMetadata = pooledEntry.getValue().getFirst();
      var pooledMeasurement = new MeasurementUpdateInformationNGS(
          commonMetadata.measurementId(),
          commonMetadata.organisationId(),
          commonMetadata.instrumentCURIE(),
          commonMetadata.facility(),
          commonMetadata.sequencingReadType(),
          commonMetadata.libraryKit(),
          commonMetadata.flowCell(),
          commonMetadata.sequencingRunProtocol(),
          commonMetadata.samplePoolGroup(),
          specificMetadata);
      finalMeasurements.add(pooledMeasurement);
    }
    return finalMeasurements;
  }
}
