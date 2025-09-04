package life.qbic.projectmanagement.application.dataset;

import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class LocalRawDatasetLookupService {

  private final LocalRawDatasetRepository rawDatasetRepostiory;

  @Autowired
  public LocalRawDatasetLookupService(LocalRawDatasetRepository localRawDatasetRepository) {
    this.rawDatasetRepostiory = Objects.requireNonNull(localRawDatasetRepository)
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<RawDatasetInformationPxP> findAllPxP(String projectId, String experimentId, int offset,
      int limit, List<SortOrder> sortOrders, String filter) throws ServiceException{
    return rawDatasetRepostiory.findAllPxP(experimentId, offset, limit, sortOrders, filter);
  }

  static class ServiceException extends RuntimeException {
    public ServiceException(String message) {
      super(message);
    }

    public ServiceException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
