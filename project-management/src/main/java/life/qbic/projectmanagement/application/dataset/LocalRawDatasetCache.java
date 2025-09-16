package life.qbic.projectmanagement.application.dataset;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Local Raw Dataset Cache</b>
 * <p>
 * A simple cache that enables the application to persist metadata about raw dataset metadata from
 * external resources associated with measurements from the application.
 *
 * @since 1.11.0
 */
@Service
public class LocalRawDatasetCache {

  private final LocalRawDatasetRepository repository;

  @Autowired
  public LocalRawDatasetCache(LocalRawDatasetRepository repository) {
    this.repository = Objects.requireNonNull(repository);
  }

  public void saveAll(List<RawDataset> rawDataset) {
    Objects.requireNonNull(rawDataset);
    repository.saveAll(rawDataset);
  }

}
