package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;


import javax.persistence.*;
import java.util.Objects;
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

  public String getProjectTitle() {
    return projectTitle;
  }

  private void setId(ProjectId projectId) {
    this.id = projectId;
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
    return projectTitle.equals(that.projectTitle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectTitle);
  }
}
