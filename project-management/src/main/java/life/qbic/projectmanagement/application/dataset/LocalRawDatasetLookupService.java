package life.qbic.projectmanagement.application.dataset;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationNgs;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortRawData;
import org.springframework.beans.factory.annotation.Autowired;
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

  private final LocalRawDatasetRepository rawDatasetRepository;

  @Autowired
  public LocalRawDatasetLookupService(LocalRawDatasetRepository localRawDatasetRepository) {
    this.rawDatasetRepository = Objects.requireNonNull(localRawDatasetRepository);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<RawDatasetInformationPxP> findAllPxP(String projectId, String experimentId,
      int offset,
      int limit, SortRawData sorting, String filter) throws ServiceException {
    return rawDatasetRepository.findAllPxP(experimentId, offset, limit, sorting, filter);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<RawDatasetInformationNgs> findAllNgs(String projectId, String experimentId,
      int offset,
      int limit, SortRawData sorting, String filter) throws ServiceException {
    return rawDatasetRepository.findAllNgs(experimentId, offset, limit, sorting, filter);
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
