package life.qbic.projectmanagement.application.measurement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MeasurementLookupService
 * <p>
 * Service that provides an API to search and filter measurement information
 */
@Service
public class MeasurementLookupService {
  private final MeasurementRepository measurementRepository;
  private final MeasurementLookup measurementLookup;

  public MeasurementLookupService(@Autowired MeasurementLookup measurementLookup,
      @Autowired MeasurementRepository measurementRepository) {
    this.measurementLookup = Objects.requireNonNull(measurementLookup);
    this.measurementRepository = Objects.requireNonNull(measurementRepository);
  }

  /**
   * Queries {@link ProteomicsMeasurement}s with a provided offset and limit that supports
   * pagination.
   *
   * @param termFilter the user's input will be applied to filter results
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   */
  protected List<ProteomicsMeasurement> queryProteomicsMeasurementsBySampleIds(String termFilter,
      List<SampleId> sampleIds, int offset, int limit, List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<ProteomicsMeasurement> termList = measurementLookup.findProteomicsMeasurementsBySampleIds(
        termFilter, sampleIds, offset,
        limit, sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(termList);
  }

  /**
   * Queries {@link NGSMeasurement}s with a provided offset and limit that supports pagination.
   *
   * @param termFilter the user's input will be applied to filter results
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   */
  protected List<NGSMeasurement> queryNGSMeasurementsBySampleIds(String termFilter,
      List<SampleId> sampleIds, int offset, int limit, List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<NGSMeasurement> termList = measurementLookup.queryNGSMeasurementsBySampleIds(termFilter,
        sampleIds, offset,
        limit, sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(termList);
  }

  public long countNGSMeasurementsBySampleIds(Collection<SampleId> sampleIds) {
    return measurementLookup.countNgsMeasurementsBySampleIds(sampleIds);
  }

  public long countProteomicsMeasurementsBySampleIds(Collection<SampleId> sampleIds) {
    return measurementLookup.countProteomicsMeasurementsBySampleIds(sampleIds);
  }

  /**
   * Provides the count of the registered measurements for the provided sampleIds
   *
   * @param sampleIds {@link SampleId}s for which the number of associated measurements should be
   *                  determined
   * @return number of measurements for all domains associated with the provided sampleIds
   */
  public long countMeasurementsBySampleIds(Collection<SampleId> sampleIds) {
    return measurementLookup.countNgsMeasurementsBySampleIds(sampleIds)
        + measurementLookup.countProteomicsMeasurementsBySampleIds(sampleIds);
  }

  public List<ProteomicsMeasurement> queryAllProteomicsMeasurements(List<SampleId> sampleIds) {
    return measurementLookup.findProteomicsMeasurementsBySampleIds(sampleIds);
  }

  public List<NGSMeasurement> queryAllNGSMeasurements(List<SampleId> sampleIds) {
    return measurementLookup.findNGSMeasurementsBySampleIds(sampleIds);
  }

  public Optional<ProteomicsMeasurement> findProteomicsMeasurementById(String measurementId) {
    return measurementRepository.findProteomicsMeasurementById(measurementId);
  }

  public Optional<ProteomicsMeasurement> findProteomicsMeasurement(String measurementCode) {
    return measurementRepository.findProteomicsMeasurement(measurementCode);
  }

  public Optional<NGSMeasurement> findNGSMeasurementById(String measurementId) {
    return measurementRepository.findNGSMeasurementById(measurementId);
  }

  public List<NGSMeasurement> queryAllNGSMeasurement(List<SampleId> sampleIds) {
    return measurementLookup.findNGSMeasurementsBySampleIds(sampleIds);
  }

  public Optional<NGSMeasurement> findNGSMeasurement(String measurementId) {
    return measurementRepository.findNGSMeasurement(measurementId);
  }
}
