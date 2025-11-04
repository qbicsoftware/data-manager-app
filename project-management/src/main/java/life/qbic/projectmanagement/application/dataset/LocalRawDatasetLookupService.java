package life.qbic.projectmanagement.application.dataset;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetFilter;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationNgs;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortRawData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * <b>Local Raw Dataset Lookup Service</b>
 * <p>
 * Service that provides access to information about locally cached raw dataset metadata, where the
 * actually raw dataset is hosted in an external resource.
 *
 * @since 1.11.0
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

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Integer countAllNgs(String projectId, String experimentId, RawDatasetFilter filter) {
    return rawDatasetRepository.countNGS(experimentId, filter);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Integer countAllPxP(String projectId, String experimentId, RawDatasetFilter filter) {
    return rawDatasetRepository.countPxP(experimentId, filter);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<RawDatasetInformationNgs> findAllNgs(String projectId, String experimentId,
      int offset, int limit, RawDatasetFilter filter) {
    return rawDatasetRepository.findAllNgs(experimentId, offset, limit, filter);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<RawDatasetInformationPxP> findAllPxP(String projectId, String experimentId,
      int offset, int limit, RawDatasetFilter filter) {
    return rawDatasetRepository.findAllPxP(experimentId, offset, limit, filter);
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
