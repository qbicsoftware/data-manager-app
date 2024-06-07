package life.qbic.projectmanagement.domain.model.sample.qualitycontrol;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Quality Control Association</b>
 *
 * <p>Entity which contains context-related information around the provided {@link QualityControlUpload}
 * file such as the associated {@link Project} via its {@link ProjectId} and the file upload timestamp
 * </p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "quality_control")
public class QualityControl {

  private ProjectId projectId;

  private Instant providedOn;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "qualityControlReference", referencedColumnName = "id")
  private QualityControlUpload qualityControlUpload;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  protected QualityControl() {

  }

  protected QualityControl(ProjectId projectId, Instant providedOn,
      QualityControlUpload qualityControlUpload) {
    this.projectId = projectId;
    this.providedOn = providedOn;
    this.qualityControlUpload = qualityControlUpload;
  }

  public static QualityControl create(ProjectId projectId, Instant providedOn,
      QualityControlUpload qualityControlUpload) {
    return new QualityControl(projectId, providedOn, qualityControlUpload);
  }

  public ProjectId project() {
    return this.projectId;
  }

  public QualityControlUpload qualityControlUpload() {
    return qualityControlUpload;
  }

  public Instant providedOn() {
    return providedOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualityControl that = (QualityControl) o;
    return Objects.equals(projectId, that.projectId) && Objects.equals(
        providedOn, that.providedOn) && Objects.equals(
        qualityControlUpload, that.qualityControlUpload)
        && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, providedOn, qualityControlUpload, id);
  }

  public Long getId() {
    return id;
  }
}
