package life.qbic.projectmanagement.infrastructure.project.qualityControl;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.application.api.QualityControlStorage;
import life.qbic.projectmanagement.application.api.QualityControlStorageException;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.qualityControl.QualityControl;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * <b>Quality Control Store</b>
 *
 * <p>Implementation of the {@link QualityControlStorage} interface.</p>
 */
@Component
public class QualityControlStore implements QualityControlStorage {

  private static final Logger log = getLogger(QualityControlStore.class);


  public QualityControlStore() {
    //ToDo Implement Persistence store
  }

  /**
   * Stores a {@link QualityControl} item persistently.
   *
   * @param qualityControl the QualityControl item to store
   * @throws QualityControlStorageException is thrown if the storage fails
   * @since 1.0.0
   */
  @Override
  public void storeQualityControl(QualityControl qualityControl)
      throws QualityControlStorageException {

  }

  @Override
  public void storeQualityControls(List<QualityControl> qualityControls)
      throws QualityControlStorageException {

  }

  @Override
  public List<QualityControl> findQualityControlsForProject(ProjectId projectId) {
    return null;
  }

  @Override
  public void deleteQualityControlsForProject(String projectId, long qualityControlId) {

  }

  @Override
  public Optional<QualityControl> findQualityControlForProject(String projectId,
      Long qualityControlId) {
    return Optional.empty();
  }
}
