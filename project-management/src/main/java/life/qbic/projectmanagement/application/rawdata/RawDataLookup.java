package life.qbic.projectmanagement.application.rawdata;

import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.rawdata.RawData;

public interface RawDataLookup {

  /**
   * Queries {@link RawData} with a provided offset and limit that supports
   * pagination.
   *
   * @param measurementIds  the list of {@link MeasurementId} for which the {@link RawData}
   *                   should be fetched
   * @param filter     the results fields will be checked for the value within this filter
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the ordering to sort by
   * @return the results in the provided range
   */
  List<RawData> queryRawDataByMeasurementIds(String filter,
      Collection<MeasurementId> measurementIds, int offset,
      int limit, List<SortOrder> sortOrders);

  long countRawDataByMeasurementIds(Collection<MeasurementId> measurementIds);
}
