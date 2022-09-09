package life.qbic.projectmanagement;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

/**
 * A project planned and run at QBiC.
 *
 * @since <version tag>
 */
public class Project {

  private final UUID uuid;
  private final ProjectIntent projectIntent;

  private Project(UUID uuid, ProjectIntent projectIntent) {
    requireNonNull(uuid);
    requireNonNull(projectIntent);
    this.uuid = uuid;
    this.projectIntent = projectIntent;
  }

  /**
   * Creates a new project with code and project intent
   *
   * @param projectIntent the intent of the project
   * @return a new project instance
   */
  public static Project create(ProjectIntent projectIntent) {
    UUID uuid = UUID.randomUUID();
    return new Project(uuid, projectIntent);
  }

  public static Project of(UUID uuid, ProjectIntent projectIntent) {
    return new Project(uuid, projectIntent);
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

    return uuid.equals(project.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }
}
