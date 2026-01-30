package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository.NgsMeasurementFilter;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository.NgsMeasurementInformation;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository.PxpMeasurementFilter;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository.PxpMeasurementInformation;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;


@Service
public class MeasurementLookup implements NgsMeasurementLookup, PxpMeasurementLookup {

  private final NgsMeasurementJpaRepository ngsMeasurementJpaRepository;
  private final PxpMeasurementJpaRepository pxpMeasurementJpaRepository;

  public MeasurementLookup(NgsMeasurementJpaRepository ngsMeasurementJpaRepository,
      PxpMeasurementJpaRepository pxpMeasurementJpaRepository) {
    this.ngsMeasurementJpaRepository = Objects.requireNonNull(ngsMeasurementJpaRepository);
    this.pxpMeasurementJpaRepository = Objects.requireNonNull(pxpMeasurementJpaRepository);
  }

  @Override
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public @NonNull Stream<NgsMeasurementLookup.MeasurementInfo> lookupNgsMeasurements(
      @NonNull String projectId,
      int offset, int limit, @NonNull Sort sort,
      @NonNull NgsMeasurementLookup.MeasurementFilter measurementFilter)
      throws NgsMeasurementLookup.SortKeyException {

    var invalidSortKeys = sort.stream()
        .filter(order -> !NgsSortKey.isValidSortKey(order.getProperty()))
        .toList();
    if (!invalidSortKeys.isEmpty()) {
      throw new NgsMeasurementLookup.SortKeyException(
          "Invalid sort keys for ngs measurements: " + invalidSortKeys.stream()
              .map(Order::getProperty).toList());
    }
    var pageable = new OffsetBasedRequest(offset, limit, sort);
    var filter = mapToDatabaseFilter(measurementFilter);
    return ngsMeasurementJpaRepository.findAll(filter.asSpecification(), pageable)
        .get()
        .map((NgsMeasurementInformation dbObject) -> toApiObject(projectId, dbObject));
  }


  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  @Override
  public int countNgsMeasurements(@NonNull String projectId,
      @NonNull NgsMeasurementLookup.MeasurementFilter measurementFilter) {
    var filter = mapToDatabaseFilter(measurementFilter);
    return (int) ngsMeasurementJpaRepository.count(filter.asSpecification());
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  @Override
  public @NonNull Stream<PxpMeasurementLookup.MeasurementInfo> lookupPxpMeasurements(
      @NonNull String projectId, int offset, int limit,
      @NonNull Sort sort,
      @NonNull PxpMeasurementLookup.MeasurementFilter measurementFilter)
      throws PxpMeasurementLookup.SortKeyException {

    var invalidSortKeys = sort.stream()
        .filter(order -> !PxpSortKey.isValidSortKey(order.getProperty()))
        .toList();
    if (!invalidSortKeys.isEmpty()) {
      throw new PxpMeasurementLookup.SortKeyException(
          "Invalid sort keys for ngs measurements: " + invalidSortKeys.stream()
              .map(Order::getProperty).toList());
    }
    var pageable = new OffsetBasedRequest(offset, limit, sort);
    var filter = mapToDatabaseFilter(measurementFilter);
    return pxpMeasurementJpaRepository.findAll(filter.asSpecification(), pageable)
        .get()
        .map((PxpMeasurementInformation dbObject) -> toApiObject(projectId, dbObject));
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  @Override
  public int countPxpMeasurements(
      @NonNull String projectId,
      @NonNull PxpMeasurementLookup.MeasurementFilter measurementFilter) {
    var filter = mapToDatabaseFilter(measurementFilter);
    return (int) pxpMeasurementJpaRepository.count(filter.asSpecification());
  }

  private static @NonNull NgsMeasurementJpaRepository.NgsMeasurementFilter mapToDatabaseFilter(
      @NonNull NgsMeasurementLookup.MeasurementFilter measurementFilter) {
    return NgsMeasurementFilter.forExperiment(measurementFilter.experimentId())
        .anyContaining(measurementFilter.searchTerm())
        .atClientTimeOffset(measurementFilter.timeZoneOffsetMillis());
  }

  private static @NonNull PxpMeasurementFilter mapToDatabaseFilter(
      @NonNull PxpMeasurementLookup.MeasurementFilter measurementFilter) {
    return PxpMeasurementFilter.forExperiment(measurementFilter.experimentId())
        .anyContaining(measurementFilter.searchTerm())
        .atClientTimeOffset(measurementFilter.timeZoneOffsetMillis());
  }

  private NgsMeasurementLookup.MeasurementInfo toApiObject(String projectId,
      NgsMeasurementInformation dbMeasurement) {
    List<NgsMeasurementLookup.SampleInfo> sampleInfos = dbMeasurement.sampleInfos().stream()
        .map(sampleInfo ->
            new NgsMeasurementLookup.SampleInfo(sampleInfo.sampleId(),
                sampleInfo.sampleCode(),
                sampleInfo.sampleLabel(),
                sampleInfo.indexI5(),
                sampleInfo.indexI7(),
                sampleInfo.comment()))
        .toList();
    NgsMeasurementJpaRepository.Organisation organisation = Objects.requireNonNull(
        dbMeasurement.organisation());
    NgsMeasurementJpaRepository.Instrument instrument = Objects.requireNonNull(
        dbMeasurement.instrument());

    return new NgsMeasurementLookup.MeasurementInfo(
        dbMeasurement.measurementId(),
        projectId,
        dbMeasurement.getExperimentId(),
        dbMeasurement.measurementCode(),
        dbMeasurement.measurementName(),
        dbMeasurement.facility(),
        new NgsMeasurementLookup.Organisation(organisation.label(), organisation.iri()),
        new Instrument(instrument.label(), instrument.oboId(),
            instrument.iri()),
        dbMeasurement.samplePool(),
        dbMeasurement.registeredAt(),
        dbMeasurement.sequencingReadType(),
        dbMeasurement.libraryKit(),
        dbMeasurement.flowCell(),
        dbMeasurement.sequencingRunProtocol(),
        sampleInfos);
  }

  private PxpMeasurementLookup.MeasurementInfo toApiObject(String projectId,
      PxpMeasurementInformation dbMeasurement) {
    List<PxpMeasurementLookup.SampleInfo> sampleInfos = dbMeasurement.sampleInfos().stream()
        .map(sampleInfo ->
            new PxpMeasurementLookup.SampleInfo(sampleInfo.sampleId(),
                sampleInfo.sampleCode(),
                sampleInfo.sampleLabel(),
                sampleInfo.fractionName(),
                sampleInfo.measurementLabel(),
                sampleInfo.comment()))
        .toList();
    PxpMeasurementJpaRepository.Organisation organisation = Objects.requireNonNull(
        dbMeasurement.organisation());
    PxpMeasurementJpaRepository.MsDevice msDevice = Objects.requireNonNull(
        dbMeasurement.msDevice());

    return new PxpMeasurementLookup.MeasurementInfo(
        dbMeasurement.measurementId(),
        projectId,
        dbMeasurement.experimentId(),
        dbMeasurement.measurementCode(),
        dbMeasurement.measurementName(),
        dbMeasurement.facility(),
        new PxpMeasurementLookup.Organisation(organisation.label(), organisation.iri()),
        new MsDevice(msDevice.msLabel(), msDevice.oboId(),
            msDevice.iri()),
        dbMeasurement.samplePool(),
        dbMeasurement.registeredAt(),
        dbMeasurement.digestionEnzyme(),
        dbMeasurement.digestionMethod(),
        dbMeasurement.enrichmentMethod(),
        dbMeasurement.injectionVolume(),
        dbMeasurement.labelType(),
        dbMeasurement.label(),
        dbMeasurement.technicalReplicateName(),
        dbMeasurement.lcmsMethod(),
        dbMeasurement.lcColumn(),
        sampleInfos);
  }
}
