package life.qbic.projectmanagement.domain;

import static java.util.Objects.requireNonNull;

/**
 * A project planned and run at QBiC.
 *
 * @since <version tag>
 */
public class Project {

  private final ProjectId projectId;
  private final ProjectIntent projectIntent;

  private Project(ProjectId projectId, ProjectIntent projectIntent) {
    requireNonNull(projectId);
    requireNonNull(projectIntent);
    this.projectId = projectId;
    this.projectIntent = projectIntent;
  }

  /**
   * Creates a new project with code and project intent
   *
   * @param projectIntent the intent of the project
   * @return a new project instance
   */
  public static Project create(ProjectIntent projectIntent) {
    return new Project(ProjectId.create(), projectIntent);
  }

  /**
   * Generates a project with the specified values injected.
   *
   * @param projectId     the identifier of the project
   * @param projectIntent the project intent
   * @return a project with the given identity and project intent
   */
  public static Project of(ProjectId projectId, ProjectIntent projectIntent) {
    return new Project(projectId, projectIntent);
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
