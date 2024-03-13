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
import life.qbic.projectmanagement.domain.model.sample.SampleId;
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
            List.of(SampleId.create(), SampleId.create()), Instant.now()),
        new NGSRawData(MeasurementId.create(),
            List.of(SampleId.create(), SampleId.create()), Instant.now()),
        new NGSRawData(MeasurementId.create(),
            List.of(SampleId.create(), SampleId.create()), Instant.now()),
        new NGSRawData(MeasurementId.create(),
            List.of(SampleId.create(), SampleId.create()), Instant.now())));
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
            List.of(SampleId.create(), SampleId.create()), Instant.now()),
        new ProteomicsRawData(MeasurementId.create(),
            List.of(SampleId.create(), SampleId.create()), Instant.now()),
        new ProteomicsRawData(MeasurementId.create(),
            List.of(SampleId.create(), SampleId.create()), Instant.now()),
        new ProteomicsRawData(MeasurementId.create(),
            List.of(SampleId.create(), SampleId.create()), Instant.now())));
    return dummyProteomicsData;
  }

  //Todo replace with real objects
  public record NGSRawData(MeasurementId measurementId, Collection<SampleId> measuredSamples,
                           Instant registrationDate) {

  }

  public record ProteomicsRawData(MeasurementId measurementId, Collection<SampleId> measuredSamples,
                                  Instant registrationDate) {

  }
}
