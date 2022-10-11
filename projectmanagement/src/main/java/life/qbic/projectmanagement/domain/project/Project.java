package life.qbic.projectmanagement.domain.project;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import life.qbic.projectmanagement.domain.project.repository.jpa.ProjectManagerConverter;

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

  @Convert(converter = ProjectManagerConverter.class)
  @Column(name = "projectManager", nullable = false)
  private ProjectManager projectManager;

  @Column(name = "lastModified", nullable = false)
  private Instant lastModified;

  private Project(ProjectId projectId, ProjectIntent projectIntent, ProjectCode projectCode,
      ProjectManager projectManager) {
    requireNonNull(projectId);
    requireNonNull(projectIntent);
    requireNonNull(projectCode);
    requireNonNull(projectManager);

    setProjectId(projectId);
    setProjectIntent(projectIntent);
    setProjectCode(projectCode);
    setProjectManager(projectManager);
  }

  public void setProjectManager(ProjectManager projectManager) {
    this.projectManager = projectManager;
    updateModificationDate();
  }

  private void setProjectCode(ProjectCode projectCode) {
    this.projectCode = projectCode;
    updateModificationDate();
  }

  protected Project() {

  }

  protected void setProjectId(ProjectId projectId) {
    this.projectId = projectId;
    updateModificationDate();
  }

  protected void setProjectIntent(ProjectIntent projectIntent) {
    this.projectIntent = projectIntent;
    updateModificationDate();
  }

  /**
   * Creates a new project with code and project intent
   *
   * @param projectIntent  the intent of the project
   * @param projectCode    the human-readable code of a project
   * @param projectManager the manager of the project
   * @return a new project instance
   */
  public static Project create(ProjectIntent projectIntent, ProjectCode projectCode,
      ProjectManager projectManager) {
    return new Project(ProjectId.create(), projectIntent, projectCode, projectManager);
  }

  /**
   * Generates a project with the specified values injected.
   *
   * @param projectId      the identifier of the project
   * @param projectIntent  the project intent
   * @param projectCode    the human-readable code of a project
   * @param projectManager the manager of the project
   * @return a project with the given identity and project intent
   */
  public static Project of(ProjectId projectId, ProjectIntent projectIntent,
      ProjectCode projectCode, ProjectManager projectManager) {
    return new Project(projectId, projectIntent, projectCode, projectManager);
  }

  public ProjectId getId() {
    return projectId;
  }

  private void updateModificationDate() {
    this.lastModified = Instant.now();
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
