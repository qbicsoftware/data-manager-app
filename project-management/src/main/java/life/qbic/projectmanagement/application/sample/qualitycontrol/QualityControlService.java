package life.qbic.projectmanagement.application.sample.qualitycontrol;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.PurchaseStoreException;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.event.BatchUpdated;
import life.qbic.projectmanagement.domain.model.sample.event.QualityControlChanged;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControl;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControlUpload;
import org.springframework.stereotype.Service;

/**
 * <b>Quality Control Service</b>
 * <p>
 * A service that enables actions on quality control services.
 *
 * @since 1.0.0
 */
@Service
public class QualityControlService {

  private static final Logger log = logger(QualityControlService.class);
  private final QualityControlStorage storage;

  public QualityControlService(QualityControlStorage storage) {
    this.storage = storage;
  }

  /**
   * Adds a quality controls to a project.
   *
   * @param projectId      the project the quality control is related to
   * @param qualityControl the quality control information item
   */
  public void addQualityControl(String projectId, QualityControlReport qualityControl) {
    addQualityControls(projectId, List.of(qualityControl));
  }

  /**
   * Adds a list of quality controls to a project.
   *
   * @param projectId           the project the quality control is related to
   * @param qualityControlsList list of quality controls information items to be added
   */
  public void addQualityControls(String projectId, List<QualityControlReport> qualityControlsList) {
    var projectReference = ProjectId.parse(projectId);
    var qualityControlUploadDate = Instant.now();
    List<QualityControl> qualityControls = qualityControlsList.stream()
        .map(it -> QualityControlUpload.create(it.fileName(), it.experimentId(), it.content()))
        .map(it -> QualityControl.create(projectReference, qualityControlUploadDate, it))
        .toList();
    try {
      storage.storeQualityControls(qualityControls);
      dispatchSuccessfulQCUpdate(projectReference);
    } catch (PurchaseStoreException e) {
      throw ApplicationException.wrapping(e);
    }
  }

  private void dispatchSuccessfulQCUpdate(ProjectId projectReference) {
    QualityControlChanged qcChanged = QualityControlChanged.create(projectReference);
    DomainEventDispatcher.instance().dispatch(qcChanged);
  }

  /**
   * Lists all quality controls files linked to a project
   *
   * @param projectId the projectId for which to search quality controls for
   * @return a list of all linked quality controls, can be empty, never null.
   */
  public List<QualityControlUpload> linkedQualityControls(String projectId) {
    ProjectId parsedId = ProjectId.parse(projectId);
    return requireNonNull(storage.findQualityControlsForProject(parsedId),
        "Provided quality controls must not be null");
  }

  /**
   * Triggers the deletion of a {@link QualityControl} item.
   *
   * @param projectId        the projectId for which the {@link QualityControl} should be deleted
   * @param qualityControlId the id of the quality control to be deleted
   */
  public void deleteQualityControl(String projectId, long qualityControlId) {
    storage.deleteQualityControlForProject(projectId, qualityControlId);
    dispatchSuccessfulQCUpdate(ProjectId.parse(projectId));
  }

  /**
   * Returns a {@link QualityControlUpload} item with the provided qualityControlId which is
   * associated with the provided {@link ProjectId} from the database.
   *
   * @param projectId        the projectId for which the {@link QualityControl} should be
   *                         returned
   * @param qualityControlId the id of the quality control to be returned
   */
  public Optional<QualityControlUpload> getQualityControlWithContent(String projectId,
      Long qualityControlId) {
    return storage.findQualityControlForProject(projectId, qualityControlId);
  }

}
