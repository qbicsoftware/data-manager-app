package life.qbic.projectmanagement.application.dataset;

import java.util.List;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationNgs;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortRawData;

/**
 * <b>Local Raw Dataset Repository</b>
 * <p>
 * A repository that provides access to information about locally cached raw dataset metadata from
 * external resources.
 *
 * @since 1.11.0
 */
public interface LocalRawDatasetRepository {

  /**
   * This method provides an API for synchronisation of the local cache with {@link RawDataset}
   * information.
   *
   * @param rawDatasets a {@link List} of {@link RawDataset} with information about fetched external
   *                    raw dataset meta-data.
   * @since 1.11.0
   */
  void saveAll(List<RawDataset> rawDatasets);

  /**
   * Searches for information about raw datasets stored in the local cache. The method also supports
   * paginated requests.
   *
   * @param experimentId the identifier of the experiment raw datasets derive from
   * @param offset       the offset value for paginated requests
   * @param limit        the maximal number of entries contained in the returned {@link List}
   * @param sorting      sorting configuration based on properties
   * @param filter       a filter to be applied on the search. Leave empty if you want to get all
   *                     hits
   * @return a {@link List} of {@link RawDatasetInformationPxP} hits from the search
   * @since 1.11.0
   */
  List<RawDatasetInformationPxP> findAllPxP(String experimentId, int offset, int limit,
      SortRawData sorting, String filter);

  /**
   * Searches for information about raw datasets stored in the local cache. The method also supports
   * paginated requests.
   *
   * @param experimentId the identifier of the experiment raw datasets derive from
   * @param offset       the offset value for paginated requests
   * @param limit        the maximal number of entries contained in the returned {@link List}
   * @param sorting      sorting configuration based on properties
   * @param filter       a filter to be applied on the search. Leave empty if you want to get all
   *                     hits
   * @return a {@link List} of {@link RawDatasetInformationNgs} hits from the search
   * @since 1.11.0
   */
  List<RawDatasetInformationNgs> findAllNgs(String experimentId, int offset, int limit,
      SortRawData sorting, String filter);
}
