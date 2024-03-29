package life.qbic.projectmanagement.application.measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MeasurementLookupService
 * <p>
 * Service that provides an API to query and filter measurement information
 */
@Service
public class MeasurementLookupService {

  private static final Logger log = LoggerFactory.logger(MeasurementLookupService.class);
  private final MeasurementRepository measurementRepository;
  private final MeasurementLookup measurementLookup;

  public MeasurementLookupService(@Autowired MeasurementLookup measurementLookup,
      @Autowired MeasurementRepository measurementRepository) {
    this.measurementLookup = Objects.requireNonNull(measurementLookup);
    this.measurementRepository = Objects.requireNonNull(measurementRepository);
  }

  /**
   * Queries {@link ProteomicsMeasurement}s with a provided offset and limit that supports pagination.
   *
   * @param termFilter the user's input will be applied to filter results
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   */
  public List<ProteomicsMeasurement> queryProteomicsMeasurementsBySampleIds(String termFilter,
      List<SampleId> sampleIds, int offset, int limit, List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<ProteomicsMeasurement> termList = measurementLookup.queryProteomicsMeasurementsBySampleIds(
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
  public List<NGSMeasurement> queryNGSMeasurementsBySampleIds(String termFilter,
      List<SampleId> sampleIds, int offset, int limit, List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<NGSMeasurement> termList = measurementLookup.queryNGSMeasurementsBySampleIds(termFilter,
        sampleIds, offset,
        limit, sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(termList);
  }

}
