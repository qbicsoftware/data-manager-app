package life.qbic.projectmanagement.application.rawdata;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.measurement.MeasurementLookupService;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;


/**
 * Raw Data Service
 * <p>
 * Service that provides an API to manage and query raw Data information
 */
@Service
public class RawDataService {
  private static final Logger log = logger(RawDataService.class);
  private final SampleInformationService sampleInformationService;
  private final MeasurementLookupService measurementLookupService;
  private final RawDataLookupService rawDataLookupService;

  @Autowired
  public RawDataService(SampleInformationService sampleInformationService,
      MeasurementLookupService measurementLookupService,
      RawDataLookupService rawDataLookupService) {
    this.measurementLookupService = Objects.requireNonNull(measurementLookupService);
    this.rawDataLookupService = Objects.requireNonNull(rawDataLookupService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
  }

  /**
   * Checks if there is raw Data registered for the provided experimentId
   *
   * @param experimentId {@link ExperimentId}s of the experiment for which it should be determined
   *                     if its contained {@link Sample} have {@link MeasurementMetadata} with
   *                     associated raw data
   * @return true if experiments has measurements with associated measurements, false if not
   */
  public boolean hasRawData(ExperimentId experimentId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    //Todo figure out on how to best solve this
    rawDataLookupService.countRawDataByMeasurementIds(new ArrayList<>());
    return true;
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<RawData> findRawData(String filter,
      ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrder, ProjectId projectId) {
    var measurementIds = retrieveMeasurementsForExperiment(experimentId, filter, offset, limit,
        sortOrder);
    var rawDomainData = rawDataLookupService.queryRawDataByMeasurementIds(filter, measurementIds,
        offset, limit, sortOrder);
    //Todo load data from other application services and fill in here
    return rawDomainData.stream().map(rawdata -> new RawData(rawdata.measurementId(),
        List.of(
            new RawDataSampleInformation(SampleCode.create("Q0000"), "My awesome sample 4")),
        rawdata.registrationDate(), "Proteomics Dataset", "Q0000_primary_results.zip", "78GB", "20",
        "120EA8A25E5D487BF68B5F7096440019")).toList();
    /*Todo implement
    List<RawData> dummyProteomicsData = new ArrayList<>(List.of(
        new RawData(MeasurementId.create(),
            List.of(new RawDataSampleInformation(SampleCode.create("Q1234"), "My awesome sample 1"),
                new RawDataSampleInformation(SampleCode.create("Q2345"),
                    "My awesome sample again 1")),
            Instant.now(), "Proteomics Dataset", "Q1234_primary_results.zip", "12GB", "5",
            "120EA8A25E5D487BF68B5F7096440019"),
        new RawData(MeasurementId.create(),
            List.of(
                new RawDataSampleInformation(SampleCode.create("Q4567"), "My awesome sample 2")),
            Instant.now(), "Proteomics Dataset", "Q4567_primary_results.zip", "34GB", "10",
            "120EA8A25E5D487BF68B5F7096440019"),
        new RawData(MeasurementId.create(),
            List.of(new RawDataSampleInformation(SampleCode.create("Q7890"), "My awesome sample 3"),
                new RawDataSampleInformation(SampleCode.create("Q9345"),
                    "My awesome sample again 3")),
            Instant.now(), "Proteomics Dataset", "Q7890_primary_results.zip", "56GB", "15",
            "120EA8A25E5D487BF68B5F7096440019"),
        new RawData(MeasurementId.create(),
            List.of(
                new RawDataSampleInformation(SampleCode.create("Q0000"), "My awesome sample 4")),
            Instant.now(), "Proteomics Dataset", "Q0000_primary_results.zip", "78GB", "20",
            "120EA8A25E5D487BF68B5F7096440019")));
    return dummyProteomicsData; */
  }

  private List<MeasurementId> retrieveMeasurementsForExperiment(ExperimentId experimentId,
      String filter, int offset, int limit,
      List<SortOrder> sortOrder) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    var measurements = measurementLookupService.queryProteomicsMeasurementsBySampleIds(filter,
        samplesInExperiment, offset, limit, sortOrder);
    return measurements.stream().map(ProteomicsMeasurement::measurementId).toList();
  }

  //Todo replace with real objects
  public record RawData(MeasurementId measurementId,
                        Collection<RawDataSampleInformation> measuredSamples,
                        Instant registrationDate, String description,
                        String dataSetFileName,
                        String fileSize, String numberOfFiles, String checksum) {

  }

  public record RawDataSampleInformation(SampleCode sampleCode, String sampleLabel) {

  }
}
