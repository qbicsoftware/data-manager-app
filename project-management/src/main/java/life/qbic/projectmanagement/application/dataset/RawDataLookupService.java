package life.qbic.projectmanagement.application.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.dataset.RawDataService.RawData;
import life.qbic.projectmanagement.application.dataset.RawDataService.RawDataDatasetInformation;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Raw Data Lookup Service
 * <p>
 * Service that provides an API to search and filter measurement information
 */
@Service
public class RawDataLookupService {

  private final RawDataLookup rawDataLookup;

  public RawDataLookupService(@Autowired RawDataLookup rawDataLookup) {
    this.rawDataLookup = Objects.requireNonNull(rawDataLookup);
  }

  /**
   * Queries {@link RawData}s with a provided offset and limit that supports
   * pagination.
   *
   * @param filter     the user's input will be applied to filter results
   * @param measurementCodes the measurementCodes to which the filter should be applied
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   */
  protected List<RawDataDatasetInformation> queryRawDataByMeasurementCodes(String filter,
      List<MeasurementCode> measurementCodes, int offset, int limit, List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    var dataList = rawDataLookup.queryRawDataByMeasurementCodes(
        filter, measurementCodes, offset,
        limit, sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(dataList);
  }

  /**
   * Provides the count of the registered measurements for the provided measurementIds
   *
   * @param measurementCodes {@link MeasurementId}s for which the number of associated raw Data should be
   *                  determined
   * @return number of raw data for all domains associated with the provided measurementIds
   */
  public int countRawDataByMeasurementCodes(Collection<MeasurementCode> measurementCodes) {
    return rawDataLookup.countRawDataByMeasurementIds(measurementCodes);
  }

}
