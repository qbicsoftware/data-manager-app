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
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;


@Service
public class MeasurementLookup implements NgsMeasurementLookup, PxpMeasurementLookup {

  private final NgsMeasurementJpaRepository ngsMeasurementJpaRepository;

  public MeasurementLookup(NgsMeasurementJpaRepository ngsMeasurementJpaRepository) {
    this.ngsMeasurementJpaRepository = Objects.requireNonNull(ngsMeasurementJpaRepository);
  }

  @Override
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public @NonNull Stream<MeasurementInfo> lookupNgsMeasurements(@NonNull String projectId,
      int offset, int limit, @NonNull Sort sort,
      @NonNull NgsMeasurementLookup.MeasurementFilter measurementFilter) throws SortKeyException {

    var invalidSortKeys = sort.stream()
        .filter(order -> !NgsSortKey.isValidSortKey(order.getProperty()))
        .toList();
    if (!invalidSortKeys.isEmpty()) {
      throw new SortKeyException(
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

  private static @NonNull NgsMeasurementJpaRepository.NgsMeasurementFilter mapToDatabaseFilter(
      @NonNull MeasurementFilter measurementFilter) {
    return new NgsMeasurementFilter(measurementFilter.experimentId(),
        measurementFilter.searchTerm());
  }

  private MeasurementInfo toApiObject(String projectId, NgsMeasurementInformation dbMeasurement) {
    List<SampleInfo> sampleInfos = dbMeasurement.sampleInfos().stream()
        .map(sampleInfo ->
            new SampleInfo(sampleInfo.sampleId(),
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

    return new MeasurementInfo(
        dbMeasurement.measurementId(),
        projectId,
        dbMeasurement.getExperimentId(),
        dbMeasurement.measurementCode(),
        dbMeasurement.measurementName(),
        dbMeasurement.facility(),
        new Organisation(organisation.label(), organisation.iri()),
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

}
