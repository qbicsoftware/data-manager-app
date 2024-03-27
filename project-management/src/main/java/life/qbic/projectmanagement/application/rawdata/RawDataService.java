package life.qbic.projectmanagement.application.rawdata;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.measurement.MeasurementLookupService;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
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
    var measurements = measurementLookupService.retrieveAllMeasurementsWithSampleIds(
        samplesInExperiment);
    var codes = measurements.stream().map(MeasurementMetadata::measurementCode).toList();
    return rawDataLookupService.countRawDataByMeasurementCodes(codes) > 0;
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<RawDataFileInformation> findProteomicsRawData(String filter,
      ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrder, ProjectId projectId) {
    var measurements = retrieveProteomicsMeasurementsForExperiment(experimentId, filter, offset,
        limit,
        sortOrder);
    var measurementCodes = measurements.stream().map(ProteomicsMeasurement::measurementCode).toList();
    var rawDomainData = rawDataLookupService.queryRawDataByMeasurementCodes(filter, measurementCodes,
        offset, limit, sortOrder);
    return rawDomainData.stream().map(rawData -> {
      ProteomicsMeasurement measurementWithData = measurements.stream().filter(
              proteomicsMeasurement -> proteomicsMeasurement.measurementId()
                  .equals(rawData.measurememeantId())).findAny().orElseThrow();
      List<RawDataSampleInformation> sampleInformation = sampleInformationService.retrieveSamplesByIds(
              measurementWithData.measuredSamples()).stream()
          .map(sample -> new RawDataSampleInformation(sample.sampleCode(), sample.label()))
          .toList();
      return new RawDataFileInformation(measurementWithData.measurementCode(), sampleInformation,
          rawData.registrationDate());
    }).toList();
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<RawDataFileInformation> findNGSRawData(String filter,
      ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrder, ProjectId projectId) {
    var measurements = retrieveNGSMeasurementsForExperiment(experimentId, filter, offset, limit,
        sortOrder);
    var measurementCodes = measurements.stream().map(NGSMeasurement::measurementCode).toList();
    var rawDomainData = rawDataLookupService.queryRawDataByMeasurementCodes(filter, measurementCodes,
        offset, limit, sortOrder);
    return rawDomainData.stream().map(rawData -> {
      NGSMeasurement measurementWithData = measurements.stream()
          .filter(ngsMeasurement -> ngsMeasurement.measurementId().equals(rawData.measurementId()))
          .findAny().orElseThrow();
      List<RawDataSampleInformation> sampleInformation = sampleInformationService.retrieveSamplesByIds(
              measurementWithData.measuredSamples()).stream()
          .map(sample -> new RawDataSampleInformation(sample.sampleCode(), sample.label()))
          .toList();
    }).toList();
  }


  public RawDataFileInformation findRawDataFileInformationForMeasurementCode(
      MeasurementCode measurementCode) {
    //Todo implement OpenBis connection?
    return new RawDataFileInformation("Q0000_primary_results.zip", "78GB", "20",
        "120EA8A25E5D487BF68B5F7096440019");
  }

  private List<NGSMeasurement> retrieveNGSMeasurementsForExperiment(ExperimentId experimentId,
      String filter, int offset, int limit,
      List<SortOrder> sortOrder) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    /*We need to cast the id property manually*/
    var measurementSortOrder = sortOrder.stream().map(sortOrder1 -> new SortOrder("id", true))
        .toList();
    return measurementLookupService.queryNGSMeasurementsBySampleIds(filter,
        samplesInExperiment, offset, limit, measurementSortOrder);
  }

  private List<ProteomicsMeasurement> retrieveProteomicsMeasurementsForExperiment(
      ExperimentId experimentId,
      String filter, int offset, int limit,
      List<SortOrder> sortOrder) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    /*We need to cast the id property manually*/
    var measurementSortOrder = sortOrder.stream().map(sortOrder1 -> new SortOrder("id", true))
        .toList();
    return measurementLookupService.queryProteomicsMeasurementsBySampleIds(filter,
        samplesInExperiment, offset, limit, measurementSortOrder);
  }

  /**
   * Raw Data File information to be employed in the frontend containing information associated
   * with {@link life.qbic.projectmanagement.domain.model.rawdata.RawData} collected from the connected datastore
   */
  public record RawDataFileInformation(MeasurementCode measurementCode, Set<String> fileEndings,
                                       String fileSize, int numberOfFiles, Date registrationDate) {
  }

  /**
   * Sample Information associated with the measurements to which the {@link life.qbic.projectmanagement.domain.model.rawdata.RawData}
   * is linked and meant to be employed in the frontend
   */
  public record RawDataSampleInformation(SampleCode sampleCode, String sampleLabel) {

  }
}
