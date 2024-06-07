package life.qbic.projectmanagement.application.sample.qualitycontrol;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControl;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControlUpload;

/**
 * <b>QualityControlUpload Storage</b>
 * <p>
 * Quality Control storage interface, that enables storing of {@link QualityControl}
 * items.
 * <p>
 */
public interface QualityControlStorage {

  /**
   * Stores a {@link QualityControl} item persistently.
   *
   * @param qualityControl the QualityControl item to store
   * @throws QualityControlStorageException is thrown if the storage fails
   */
  void storeQualityControl(QualityControl qualityControl)
      throws QualityControlStorageException;

  /**
   * Stores a list of {@link QualityControl} item persistently.
   *
   * @param qualityControls the QualityControl item list to store
   * @return
   * @throws QualityControlStorageException is thrown if the storage fails
   */
  Iterable<QualityControl> storeQualityControls(List<QualityControl> qualityControls)
      throws QualityControlStorageException;

  /**
   * Returns a list of {@link QualityControlUpload} items associated with the provided
   * {@link ProjectId} from the database.
   *
   * @param projectId the projectId for which the {@link QualityControl} should be returned
   */
  List<QualityControlUpload> findQualityControlsForProject(ProjectId projectId);

  /**
   * Deletes {@link QualityControl} item persistently.
   *
   * @param projectId        the projectId for which the {@link QualityControl} should be
   *                         deleted
   * @param qualityControlId the id of the quality control to be deleted
   */
  void deleteQualityControlForProject(String projectId, long qualityControlId);


  /**
   * Returns a {@link QualityControlUpload} item with the provided qualityControlId which is
   * associated with the provided {@link ProjectId} from the database.
   *
   * @param projectId        the projectId for which the {@link QualityControl} should be
   *                         returned
   * @param qualityControlId the id of the quality control to be returned
   */
  Optional<QualityControlUpload> findQualityControlForProject(String projectId,
      Long qualityControlId);

  /**
   * Returns a {@link QualityControl} item with the provided qualityControlId, if found. Used when
   * project id is not available (e.g. events). For user interactions findQualityControlForProject
   * should be used.
   *
   * @param qualityControlId the id of the quality control to be returned
   */
  Optional<QualityControl> findQualityControl(Long qualityControlId);
}
