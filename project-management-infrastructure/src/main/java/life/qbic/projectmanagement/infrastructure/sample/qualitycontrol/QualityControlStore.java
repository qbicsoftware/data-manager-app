package life.qbic.projectmanagement.infrastructure.sample.qualitycontrol;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.projectmanagement.application.sample.qualitycontrol.QualityControlStorage;
import life.qbic.projectmanagement.application.sample.qualitycontrol.QualityControlStorageException;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControl;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControlUpload;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Quality Control Store</b>
 *
 * <p>Implementation of the {@link QualityControlStorage} interface.</p>
 */
@Component
public class QualityControlStore implements QualityControlStorage {

  private static final Logger log = getLogger(QualityControlStore.class);

  private final QualityControlJpa persistenceStore;

  @Autowired
  public QualityControlStore(QualityControlJpa persistenceStore) {
    this.persistenceStore = persistenceStore;
  }

  /**
   * Stores a {@link QualityControl} item persistently.
   *
   * @param qualityControl the QualityControlUpload item to store
   * @throws QualityControlStorageException is thrown if the storage fails
   * @since 1.0.0
   */
  @Override
  public void storeQualityControl(QualityControl qualityControl)
      throws QualityControlStorageException {
    try {
      persistenceStore.save(qualityControl);
    } catch (RuntimeException e) {
      throw new QualityControlStorageException(
          "Storing the quality control for project %s failed".formatted(qualityControl.project()),
          e);
    }
  }

  @Override
  public Iterable<QualityControl> storeQualityControls(List<QualityControl> qualityControls)
      throws QualityControlStorageException {
    try {
      return persistenceStore.saveAll(qualityControls);
    } catch (RuntimeException e) {
      throw new QualityControlStorageException((
          "Storing the quality control for project %s failed".formatted(
              qualityControls.stream().findFirst().orElseThrow().project())),
          e);
    }
  }

  @Override
  public List<QualityControlUpload> findQualityControlsForProject(ProjectId projectId) {
    try {
      return persistenceStore.findQualityControlAssociationByProjectIdEquals(projectId).stream()
          .sorted(Comparator.comparing(QualityControl::providedOn)) //ensures same ordering
          .map(QualityControl::qualityControlUpload).toList();
    } catch (RuntimeException e) {
      throw new ApplicationException(
          "Retrieving quality controls for project %s failed.".formatted(projectId), e);
    }
  }

  @Override
  public void deleteQualityControlForProject(String projectId, long qualityControlId) {
    List<QualityControl> associations = persistenceStore.findQualityControlAssociationByProjectIdEquals(
        ProjectId.parse(projectId));
    List<QualityControl> associationsWithQualityControl = associations.stream()
        .filter(association -> association.qualityControlUpload().id().equals(qualityControlId))
        .toList();
    persistenceStore.deleteAll(associationsWithQualityControl);
  }

  @Override
  public Optional<QualityControlUpload> findQualityControlForProject(String projectId,
      Long qualityControlId) {
    try {
      List<QualityControl> associations = persistenceStore.findQualityControlAssociationByProjectIdEquals(
          ProjectId.parse(projectId));
      Optional<QualityControlUpload> foundQualityControl = associations.stream()
          .map(QualityControl::qualityControlUpload)
          .filter(qualityControl -> qualityControl.id().equals(qualityControlId))
          .findFirst();
      foundQualityControl.ifPresent(QualityControlUpload::fileContent); // make sure it is loaded
      return foundQualityControl;
    } catch (RuntimeException e) {
      throw new ApplicationException(
          "Retrieving quality control %d for project %s failed.".formatted(qualityControlId,
              projectId), e);
    }
  }

  /**
   * Returns a {@link QualityControl} item with the provided qualityControlId, if found. 
   * This method is intended to be used when no project id is available.
   * <p>
   * For user interactions {@link #findQualityControlForProject} instead!
   *
   * @param qualityControlId the id of the quality control to be returned
   * @see {@link #findQualityControlForProject}
   */
  @Override
  public Optional<QualityControl> findQualityControl(Long qualityControlId) {
    return persistenceStore.findById(qualityControlId);
  }
}
