package life.qbic.projectmanagement.application.dataset;

import java.util.List;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface LocalRawDatasetRepository {

  void saveAll(List<RawDataset> rawDataset);

}
