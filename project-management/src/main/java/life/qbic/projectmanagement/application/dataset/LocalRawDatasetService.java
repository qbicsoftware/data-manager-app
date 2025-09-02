package life.qbic.projectmanagement.application.dataset;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class LocalRawDatasetService {

  private final LocalRawDatasetRepository repository;

  @Autowired
  public LocalRawDatasetService(LocalRawDatasetRepository repository) {
    this.repository = Objects.requireNonNull(repository);
  }

  public void saveAll(List<RawDataset> rawDataset) {
    Objects.requireNonNull(rawDataset);
    repository.saveAll(rawDataset);
  }

}
