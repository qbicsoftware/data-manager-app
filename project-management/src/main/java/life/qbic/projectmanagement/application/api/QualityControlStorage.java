package life.qbic.projectmanagement.application.api;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.qualityControl.QualityControl;

/**
 * <b>QualityControl Storage</b>
 * <p>
 * Quality Control storage interface, that enables storing of {@link QualityControl} items.
 * <p>
 */
public interface QualityControlStorage {

  /**
   * Stores a {@link QualityControl} item persistently.
   *
   * @param qualityControl the QualityControl item to store
   * @throws QualityControlStorageException is thrown if the storage fails
   * @since 1.0.0
   */
  void storeQualityControl(QualityControl qualityControl) throws QualityControlStorageException;

  void storeQualityControls(List<QualityControl> qualityControls)
      throws QualityControlStorageException;

  List<QualityControl> findQualityControlsForProject(ProjectId projectId);

  void deleteQualityControlsForProject(String projectId, long qualityControlId);

  Optional<QualityControl> findQualityControlForProject(String projectId, Long qualityControlId);

}
