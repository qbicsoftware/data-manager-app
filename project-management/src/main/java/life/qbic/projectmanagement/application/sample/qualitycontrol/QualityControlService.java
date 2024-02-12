package life.qbic.projectmanagement.application.sample.qualitycontrol;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControl;
import org.springframework.stereotype.Service;

;

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
   * Adds a qualityControl to a project.
   *
   * @param projectId      the project the qualityControl is related to
   * @param qualityControl the qualityControl content
   * @since 1.0.0
   */
  public void addQualityControl(String projectId, QualityControlReport qualityControl) {
    addQualityControls(projectId, List.of(qualityControl));
  }

  public void addQualityControls(String projectId, List<QualityControlReport> qualityControlsList) {
    /*
    var projectReference = ProjectId.parse(projectId);
    var qualityControlUploadDate = Instant.now();
    //Todo implement backend
    List<QualityControl> qualityControls = qualityControlsList.stream()
        .map(it -> QualityControl.create(it.fileName(), it.experimentName(), it.content()))
        .map(it -> ServicePurchase.create(projectReference, purchaseDate, it))
        .toList();
    try {
      storage.storeQualityControls(qualityControls);
    } catch (PurchaseStoreException e) {
      throw ApplicationException.wrapping(e);
    }

    */
  }

  /**
   * Lists all qualityControls linked to a project
   *
   * @param projectId the projectId for which to search qualityControls for
   * @return a list of all linked qualityControls, can be empty, never null.
   */
  public List<QualityControl> linkedOffers(String projectId) {
    ProjectId parsedId = ProjectId.parse(projectId);
    return requireNonNull(storage.findQualityControlsForProject(parsedId),
        "result must not be null");
  }

  public void deleteQualityControl(String projectId, long qualityControlId) {
    storage.deleteQualityControlsForProject(projectId, qualityControlId);
  }

  public Optional<QualityControl> getQualityControlWithContent(String projectId,
      Long qualityControlId) {
    return storage.findQualityControlForProject(projectId, qualityControlId);
  }

}
