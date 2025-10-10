package life.qbic.projectmanagement.application.dataset;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService.RawData;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService.RawDataDatasetInformation;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;

public interface RemoteRawDataLookup {

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

  int countRawDataByMeasurementIds(Collection<MeasurementCode> measurementCodes);

  List<RawDataDatasetInformation> queryRawDataSince(Instant instant, int offset, int limit);
}
