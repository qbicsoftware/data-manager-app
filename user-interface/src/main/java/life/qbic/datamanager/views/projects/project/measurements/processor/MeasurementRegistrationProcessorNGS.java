package life.qbic.datamanager.views.projects.project.measurements.processor;

import static org.reflections.Reflections.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificNGS;

/**
 * <b>Measurement Registration Processor NGS</b>
 * <p>
 * Processes
 * {@link MeasurementRegistrationInformationNGS}
 * and merges metadata entries that belong to the same measurement pool.
 *
 * @since 1.11.0
 */
class MeasurementRegistrationProcessorNGS implements
    MeasurementProcessor<MeasurementRegistrationInformationNGS> {

  @Override
  public List<MeasurementRegistrationInformationNGS> process(
      List<MeasurementRegistrationInformationNGS> requests) {
    // 1. we want to aggregate measurement registration information that have the same sample pool name (we omit blank pool names)
    var measurementsBySamplePool = new HashMap<String, List<MeasurementRegistrationInformationNGS>>();
    var finalMeasurements = new ArrayList<MeasurementRegistrationInformationNGS>();
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
      Map<String, MeasurementSpecificNGS> specificMetadata;
      try{
        specificMetadata = entry.getValue().stream()
            .flatMap(m -> m.specificMetadata().entrySet().stream())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      } catch (IllegalStateException e){
        log.error("Preparation of specific measurement data failed.", e);
        throw new ProcessingException("Preparation of specific measurement data failed.", e);
      }
      var commonMetadata = entry.getValue().getFirst();
      var pooledMeasurement = new MeasurementRegistrationInformationNGS(
          commonMetadata.organisationId(),
          commonMetadata.instrumentCURIE(),
          commonMetadata.facility(),
          commonMetadata.sequencingReadType(),
          commonMetadata.libraryKit(),
          commonMetadata.flowCell(),
          commonMetadata.sequencingRunProtocol(),
          commonMetadata.samplePoolGroup(),
          specificMetadata,
          commonMetadata.measurementName());
      finalMeasurements.add(pooledMeasurement);
    }

    return finalMeasurements;
  }
}
