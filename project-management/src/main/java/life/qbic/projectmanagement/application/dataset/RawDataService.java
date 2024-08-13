package life.qbic.projectmanagement.application.dataset;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.measurement.MeasurementLookupService;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.NGSSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsSpecificMeasurementMetadata;
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
    if (samplesInExperiment.isEmpty()) {
      return false;
    }
    if (measurementLookupService.countMeasurementsBySampleIds(samplesInExperiment) == 0) {
      return false;
    }
    var ngsMeasurementCodes = measurementLookupService.queryAllNGSMeasurements(samplesInExperiment)
        .stream().map(NGSMeasurement::measurementCode);
    var pxpMeasurementCodes = measurementLookupService.queryAllProteomicsMeasurements(
        samplesInExperiment).stream().map(ProteomicsMeasurement::measurementCode);
    var codes = Stream.concat(ngsMeasurementCodes, pxpMeasurementCodes)
        .collect(Collectors.toList());
    return rawDataLookupService.countRawDataByMeasurementCodes(codes) > 0;
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<RawData> findProteomicsRawData(String filter,
      ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrder, ProjectId projectId) {

    var measurements = retrieveProteomicsMeasurementsForExperiment(experimentId);
    var measurementCodes = measurements.stream().map(ProteomicsMeasurement::measurementCode)
        .toList();
    var rawDataDatasetInformation = rawDataLookupService.queryRawDataByMeasurementCodes(filter,
        measurementCodes,
        offset, limit, sortOrder);
    return rawDataDatasetInformation.stream()
        .map((RawDataDatasetInformation datasetInformation) -> {
          var measurement = measurements.stream().filter(
                  proteomicsMeasurement -> proteomicsMeasurement.measurementCode()
                      .equals(datasetInformation.measurementCode()))
              .findFirst().orElseThrow();
          var metadataSampleInformation = sampleInformationService.retrieveSamplesByIds(
              measurement.specificMetadata().stream()
                  .map(ProteomicsSpecificMeasurementMetadata::measuredSample).toList());
          var rawDataSampleInformation = metadataSampleInformation.stream()
              .map(sample -> new RawDataSampleInformation(sample.sampleCode(), sample.label()))
              .toList();
          return new RawData(measurement.measurementCode(), rawDataSampleInformation,
              datasetInformation);
        }).toList();
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<RawData> findNGSRawData(String filter,
      ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrder, ProjectId projectId) {
    var measurements = retrieveNGSMeasurementsForExperiment(experimentId);
    var measurementCodes = measurements.stream().map(NGSMeasurement::measurementCode).toList();
    var rawDataDatasetInformation = rawDataLookupService.queryRawDataByMeasurementCodes(filter,
        measurementCodes,
        offset, limit, sortOrder);
    return rawDataDatasetInformation.stream()
        .map((RawDataDatasetInformation datasetInformation) -> {
          var measurement = measurements.stream().filter(
                  proteomicsMeasurement -> proteomicsMeasurement.measurementCode()
                      .equals(datasetInformation.measurementCode()))
              .findFirst().orElseThrow();
          var metadataSampleInformation = sampleInformationService.retrieveSamplesByIds(
              measurement.specificMeasurementMetadata().stream()
                  .map(NGSSpecificMeasurementMetadata::measuredSample).toList());
          var rawDataSampleInformation = metadataSampleInformation.stream()
              .map(sample -> new RawDataSampleInformation(sample.sampleCode(), sample.label()))
              .toList();
          return new RawData(measurement.measurementCode(), rawDataSampleInformation,
              datasetInformation);
        }).toList();
  }


  private List<NGSMeasurement> retrieveNGSMeasurementsForExperiment(ExperimentId experimentId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.queryAllNGSMeasurements(samplesInExperiment);
  }

  private List<ProteomicsMeasurement> retrieveProteomicsMeasurementsForExperiment(
      ExperimentId experimentId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.queryAllProteomicsMeasurements(samplesInExperiment);
  }

  public int countNGSDatasets(ExperimentId experimentId) {
    var measurements = retrieveNGSMeasurementsForExperiment(experimentId);
    var measurementCodes = measurements.stream().map(NGSMeasurement::measurementCode).toList();
    return rawDataLookupService.countRawDataByMeasurementCodes(measurementCodes);
  }

  public int countProteomicsDatasets(ExperimentId experimentId) {
    var measurements = retrieveProteomicsMeasurementsForExperiment(experimentId);
    var measurementCodes = measurements.stream().map(ProteomicsMeasurement::measurementCode).toList();
    return rawDataLookupService.countRawDataByMeasurementCodes(measurementCodes);
  }

  /**
   * Raw Data File information to be employed in the frontend containing information collected from
   * the connected datastore
   */
  public record RawData(MeasurementCode measurementCode,
                        List<RawDataSampleInformation> sampleInformation,
                        RawDataDatasetInformation rawDataDatasetInformation) {

  }

  /**
   * Sample Information associated with the measurements to which the {@link RawData} is linked and
   * meant to be employed in the frontend
   */
  public record RawDataSampleInformation(SampleCode sampleCode, String sampleName) {

  }

  /**
   * Sample Information associated with the measurements to which the {@link RawData} is linked and
   * meant to be employed in the frontend
   */
  public record RawDataDatasetInformation(MeasurementCode measurementCode, String fileSize,
                                          int numberOfFiles, Set<String> fileEndings,
                                          Date registrationDate) {

  }
}
