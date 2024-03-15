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
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
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
  private final MeasurementLookupService measurementLookupService;

  @Autowired
  public RawDataService(MeasurementLookupService measurementLookupService) {
    this.measurementLookupService = Objects.requireNonNull(measurementLookupService);
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
    //Todo Implement
    return true;
  }
  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<NGSRawData> findNGSRawData(String filter, ExperimentId experimentId,
      int offset,
      int limit,
      List<SortOrder> sortOrders, ProjectId projectId) {
    List<NGSRawData> dummyNGSData = new ArrayList<>(List.of(
        new NGSRawData(MeasurementId.create(),
            List.of(new RawDataSampleInformation(SampleCode.create("Q1234"), "My awesome sample 1"),
                new RawDataSampleInformation(SampleCode.create("Q2345"),
                    "My awesome sample again 1")),
            Instant.now(), "Genomics Dataset", "Q1234_primary_results.zip", "12GB", "5",
            "120EA8A25E5D487BF68B5F7096440019"),
        new NGSRawData(MeasurementId.create(),
            List.of(
                new RawDataSampleInformation(SampleCode.create("Q4567"), "My awesome sample 2")),
            Instant.now(), "Genomics Dataset", "Q4567_primary_results.zip", "34GB", "10",
            "120EA8A25E5D487BF68B5F7096440019"),
        new NGSRawData(MeasurementId.create(),
            List.of(new RawDataSampleInformation(SampleCode.create("Q7890"), "My awesome sample 3"),
                new RawDataSampleInformation(SampleCode.create("Q9345"),
                    "My awesome sample again 3")),
            Instant.now(), "Genomics Dataset", "Q7890_primary_results.zip", "56GB", "15",
            "120EA8A25E5D487BF68B5F7096440019"),
        new NGSRawData(MeasurementId.create(),
            List.of(
                new RawDataSampleInformation(SampleCode.create("Q0000"), "My awesome sample 4")),
            Instant.now(), "Genomics Dataset", "Q0000_primary_results.zip", "78GB", "20",
            "120EA8A25E5D487BF68B5F7096440019")));
    //Todo implement
    return dummyNGSData;
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<ProteomicsRawData> findProteomicsRawData(String filter,
      ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrder, ProjectId projectId) {
    //Todo implement
    List<ProteomicsRawData> dummyProteomicsData = new ArrayList<>(List.of(
        new ProteomicsRawData(MeasurementId.create(),
            List.of(new RawDataSampleInformation(SampleCode.create("Q1234"), "My awesome sample 1"),
                new RawDataSampleInformation(SampleCode.create("Q2345"),
                    "My awesome sample again 1")),
            Instant.now(), "Proteomics Dataset", "Q1234_primary_results.zip", "12GB", "5",
            "120EA8A25E5D487BF68B5F7096440019"),
        new ProteomicsRawData(MeasurementId.create(),
            List.of(
                new RawDataSampleInformation(SampleCode.create("Q4567"), "My awesome sample 2")),
            Instant.now(), "Proteomics Dataset", "Q4567_primary_results.zip", "34GB", "10",
            "120EA8A25E5D487BF68B5F7096440019"),
        new ProteomicsRawData(MeasurementId.create(),
            List.of(new RawDataSampleInformation(SampleCode.create("Q7890"), "My awesome sample 3"),
                new RawDataSampleInformation(SampleCode.create("Q9345"),
                    "My awesome sample again 3")),
            Instant.now(), "Proteomics Dataset", "Q7890_primary_results.zip", "56GB", "15",
            "120EA8A25E5D487BF68B5F7096440019"),
        new ProteomicsRawData(MeasurementId.create(),
            List.of(
                new RawDataSampleInformation(SampleCode.create("Q0000"), "My awesome sample 4")),
            Instant.now(), "Proteomics Dataset", "Q0000_primary_results.zip", "78GB", "20",
            "120EA8A25E5D487BF68B5F7096440019")));
    return dummyProteomicsData;
  }

  //Todo replace with real objects
  public record ProteomicsRawData(MeasurementId measurementId,
                                  Collection<RawDataSampleInformation> measuredSamples,
                                  Instant registrationDate, String description,
                                  String dataSetFileName,
                                  String fileSize, String numberOfFiles, String checksum) {

  }

  //Todo replace with real objects
  public record NGSRawData(MeasurementId measurementId,
                           Collection<RawDataSampleInformation> measuredSamples,
                           Instant registrationDate, String description, String dataSetFileName,
                           String fileSize, String numberOfFiles, String checksum) {

  }

  public record RawDataSampleInformation(SampleCode sampleCode, String sampleLabel) {

  }
}
