package life.qbic.projectmanagement.application.measurement;

import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

public interface MeasurementLookup {

  /**
   * Queries {@link ProteomicsMeasurement} with a provided offset and limit that supports
   * pagination.
   *
   * @param sampleIds  the list of {@link SampleId} for which the {@link ProteomicsMeasurement}
   *                   should be fetched
   * @param filter     the results fields will be checked for the value within this filter
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the ordering to sort by
   * @return the results in the provided range
   */
  List<ProteomicsMeasurement> queryProteomicsMeasurementsBySampleIds(String filter,
      Collection<SampleId> sampleIds, int offset,
      int limit, List<SortOrder> sortOrders);

  List<NGSMeasurement> queryNGSMeasurementsBySampleIds(String filter,
      Collection<SampleId> sampleIds, int offset,
      int limit, List<SortOrder> sortOrders);
}
