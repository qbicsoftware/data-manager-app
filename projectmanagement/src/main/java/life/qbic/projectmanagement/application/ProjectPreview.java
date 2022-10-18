package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * A limited view of the more complex {@link Project}.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "projects_datamanager")
public class ProjectPreview {

  @EmbeddedId()
  private ProjectId id;

  private String projectTitle;

  @Column(name = "projectCode")
  private String projectCode;

  @Column(name = "lastModified")
  private Instant lastModified;

  protected ProjectPreview() {

  }

  /**
   * Creates a new instance based on the project title
   *
   * @param projectTitle the desired project title
   * @return a new instance of a project preview
   */
  public static ProjectPreview from(ProjectTitle projectTitle) {
    requireNonNull(projectTitle);
    return new ProjectPreview(projectTitle.title());
  }

  private ProjectPreview(String title) {
    this.projectTitle = title;
  }

  public String projectTitle() {
    return projectTitle;
  }

  public Instant lastModified() {
    return lastModified;
  }

  public String projectCode() {
    return projectCode;
  }

  public ProjectId projectId() {
    return this.id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectPreview that = (ProjectPreview) o;
    return Objects.equals(id, that.id) && Objects.equals(projectTitle,
        that.projectTitle) && Objects.equals(projectCode, that.projectCode)
        && Objects.equals(lastModified, that.lastModified);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, projectTitle, projectCode, lastModified);
  }

  @Override
  public String toString() {
    return "ProjectPreview{" +
        "id=" + id +
        ", projectTitle='" + projectTitle + '\'' +
        ", projectCode='" + projectCode + '\'' +
        ", lastModified=" + lastModified +
        '}';
  }
}
