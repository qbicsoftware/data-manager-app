package life.qbic.projectmanagement.application.rawdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.rawdata.RawData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Raw Data Lookup Service
 * <p>
 * Service that provides an API to query and filter measurement information
 */
@Service
public class RawDataLookupService {

  private static final Logger log = LoggerFactory.logger(RawDataLookupService.class);
  private final RawDataLookup rawDataLookup;

  public RawDataLookupService(@Autowired RawDataLookup rawDataLookup) {
    this.rawDataLookup = Objects.requireNonNull(rawDataLookup);
  }

  /**
   * Queries {@link RawData}s with a provided offset and limit that supports
   * pagination.
   *
   * @param termFilter the user's input will be applied to filter results
   * @param measurementIds the measurementIds to which the filter should be applied
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   */
  public List<RawData> queryRawDataByMeasurementIds(String termFilter,
      List<MeasurementId> measurementIds, int offset, int limit, List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<RawData> termList = rawDataLookup.queryRawDataByMeasurementIds(
        termFilter, measurementIds, offset,
        limit, sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(termList);
  }


  /**
   * Provides the count of the registered measurements for the provided measurementIds
   *
   * @param measurementIds {@link MeasurementId}s for which the number of associated raw Data should be
   *                  determined
   * @return number of raw data for all domains associated with the provided measurementIds
   */
  public long countRawDataByMeasurementIds(Collection<MeasurementId> measurementIds) {
    return rawDataLookup.countRawDataByMeasurementIds(measurementIds);
  }

}
