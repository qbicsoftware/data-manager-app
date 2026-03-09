package life.qbic.projectmanagement.application.dataset;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import life.qbic.projectmanagement.application.measurement.MeasurementLookupService;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.MeasurementFilter;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.MeasurementInfo;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Raw Data Service
 * <p>
 * Service that provides an API to manage and query raw Data information
 */
@Service
public class RemoteRawDataService {

  private final RemoteRawDataLookupService remoteRawDataLookupService;
  private final RemoteRawDataLookup remoteRawDataLookup;
  private final NgsMeasurementLookup ngsMeasurementLookup;
  private final PxpMeasurementLookup pxpMeasurementLookup;

  @Autowired
  public RemoteRawDataService(MeasurementLookupService measurementLookupService,
      RemoteRawDataLookupService remoteRawDataLookupService,
      RemoteRawDataLookup remoteRawDataLookup,
      NgsMeasurementLookup ngsMeasurementLookup, PxpMeasurementLookup pxpMeasurementLookup) {
    this.remoteRawDataLookupService = Objects.requireNonNull(remoteRawDataLookupService);
    this.remoteRawDataLookup = Objects.requireNonNull(remoteRawDataLookup);
    this.ngsMeasurementLookup = ngsMeasurementLookup;
    this.pxpMeasurementLookup = pxpMeasurementLookup;
  }

  /**
   * Checks if there is raw Data registered for the provided experimentId
   *
   * @param experimentId {@link ExperimentId}s of the experiment for which it should be determined
   *                     if its contained {@link Sample} have {@link MeasurementMetadata} with
   *                     associated raw data
   * @return true if experiments has measurements with associated measurements, false if not
   */
  public boolean hasRawData(String projectId, ExperimentId experimentId) {
    NgsMeasurementLookup.MeasurementFilter ngsFilter = MeasurementFilter.forExperiment(
        experimentId.value());
    var ngsMeasurementCount = ngsMeasurementLookup.countNgsMeasurements(projectId,
        ngsFilter);
    Stream<String> ngsCodes =
        ngsMeasurementCount < 1 ? Stream.empty() : ngsMeasurementLookup.lookupNgsMeasurements(
                projectId,
                0, ngsMeasurementCount, Sort.unsorted(), ngsFilter)
            .map(MeasurementInfo::measurementCode);
    PxpMeasurementLookup.MeasurementFilter pxpFilter = PxpMeasurementLookup.MeasurementFilter.forExperiment(
        experimentId.value());
    var pxpMeasurementCount = pxpMeasurementLookup.countPxpMeasurements(projectId,
        pxpFilter);
    Stream<String> pxpCodes =
        pxpMeasurementCount < 1 ? Stream.empty() : pxpMeasurementLookup.lookupPxpMeasurements(
                projectId,
                0, pxpMeasurementCount, Sort.unsorted(), pxpFilter)
            .map(PxpMeasurementLookup.MeasurementInfo::measurementCode);

    var measurementCount = ngsMeasurementCount + pxpMeasurementCount;
    if (measurementCount == 0) {
      return false;
    }

    Set<MeasurementCode> allCodes = Stream.concat(ngsCodes, pxpCodes)
        .map(MeasurementCode::parse)
        .collect(Collectors.toSet());
    return remoteRawDataLookupService.countRawDataByMeasurementCodes(allCodes) > 0;
  }

  public List<RawDataset> registeredSince(Instant registeredSince, int offset, int limit) {
    var result = remoteRawDataLookup.queryRawDataSince(registeredSince, offset, limit);
    return result.stream().map(RemoteRawDataService::convert).toList();
  }


  private static RawDataset convert(RawDataDatasetInformation datasetInformation) {
    return new RawDataset(
        datasetInformation.measurementCode.value(),
        datasetInformation.fileSizeBytes,
        datasetInformation.numberOfFiles,
        Set.copyOf(datasetInformation.fileEndings),
        datasetInformation.registrationDate);
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
                                          Instant registrationDate, long fileSizeBytes) {

  }
}
