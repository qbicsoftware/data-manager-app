package life.qbic.projectmanagement.domain.project;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A project planned and run at QBiC.
 *
 * @since <version tag>
 */
@Entity
@Table(name = "projects_datamanager")
public class Project {

  @EmbeddedId
  private ProjectId projectId;

  @Embedded
  private ProjectIntent projectIntent;

  @Convert(converter = ProjectCode.Converter.class)
  @Column(name = "projectCode", nullable = false)
  private ProjectCode projectCode;

  @Column(name = "lastModified", nullable = false)
  private Instant lastModified;

  private Project(ProjectId projectId, ProjectIntent projectIntent, ProjectCode projectCode) {
    requireNonNull(projectId);
    requireNonNull(projectIntent);
    requireNonNull(projectCode);
    setProjectId(projectId);
    setProjectIntent(projectIntent);
    setProjectCode(projectCode);
  }

  private void setProjectCode(ProjectCode projectCode) {
    this.projectCode = projectCode;
  }

  protected Project() {

  }

  protected void setProjectId(ProjectId projectId) {
    this.projectId = projectId;
    this.lastModified = Instant.now();
  }

  protected void setProjectIntent(ProjectIntent projectIntent) {
    this.projectIntent = projectIntent;
    this.lastModified = Instant.now();
  }

  /**
   * Creates a new project with code and project intent
   *
   * @param projectIntent the intent of the project
   * @return a new project instance
   */
  public static Project create(ProjectIntent projectIntent, ProjectCode projectCode) {
    return new Project(ProjectId.create(), projectIntent, projectCode);
  }

  /**
   * Generates a project with the specified values injected.
   *
   * @param projectId     the identifier of the project
   * @param projectIntent the project intent
   * @return a project with the given identity and project intent
   */
  public static Project of(ProjectId projectId, ProjectIntent projectIntent,
      ProjectCode projectCode) {
    return new Project(projectId, projectIntent, projectCode);
  }

  public ProjectId getId() {
    return projectId;
  }

  public ProjectIntent getProjectIntent() {
    return projectIntent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Project project = (Project) o;

    return projectId.equals(project.projectId);
  }

  @Override
  public int hashCode() {
    return projectId.hashCode();
  }
}
