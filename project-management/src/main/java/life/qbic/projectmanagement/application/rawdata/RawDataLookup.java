package life.qbic.projectmanagement.application.rawdata;

import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.rawdata.RawDataService.RawData;
import life.qbic.projectmanagement.application.rawdata.RawDataService.RawDataDatasetInformation;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;

public interface RawDataLookup {

  /**
   * Queries {@link RawData} with a provided offset and limit that supports
   * pagination.
   *
   * @param filter     the results fields will be checked for the value within this filter
   * @param measurementCodes  the list of {@link MeasurementCode}s for which the raw Data
   *                   should be fetched
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the ordering to sort by
   * @return the results in the provided range
   */
  List<RawDataDatasetInformation> queryRawDataByMeasurementCodes(String filter,
      Collection<MeasurementCode> measurementCodes, int offset,
      int limit, List<SortOrder> sortOrders);

  long countRawDataByMeasurementIds(Collection<MeasurementCode> measurementCodes);
}
