package life.qbic.projectmanagement.domain.project;

import static java.util.Objects.requireNonNull;


import javax.persistence.*;
import java.util.Objects;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "projects_datamanager")
public class ProjectPreview {

  @Id
  @Column(name = "uuid")
  private Long id;

  private String projectTitle;

  protected ProjectPreview(){

  }

  /**
   *
   * @param projectTitle
   * @return
   */
  public static ProjectPreview from(ProjectTitle projectTitle){
    requireNonNull(projectTitle);
    return new ProjectPreview(projectTitle.title());
  }

  private ProjectPreview(String title){
    this.projectTitle = title;
  }

  public String getProjectTitle() {
    return projectTitle;
  }

  private void setId(Long id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectPreview that = (ProjectPreview) o;
    return projectTitle.equals(that.projectTitle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectTitle);
  }
}
