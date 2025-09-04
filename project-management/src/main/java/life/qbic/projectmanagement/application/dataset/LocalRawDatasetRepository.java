package life.qbic.projectmanagement.application.dataset;

import java.util.List;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface LocalRawDatasetRepository {

  void saveAll(List<RawDataset> rawDataset);

  List<RawDatasetInformationPxP> findAllPxP(String experimentId, int offset, int limit, List<SortOrder> sortOrders, String filter);

}
