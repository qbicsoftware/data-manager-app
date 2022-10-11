package life.qbic.projectmanagement.domain.project;

//TODO is the definition correct?

import java.util.Objects;

/**
 * A project manager for a project. This person is responsible for managing, monitoring and
 * communicating the planning, execution and reporting of a project from start to finish. Project
 * managers of a project may change during the project life cycle.
 */
public record ProjectManager(String fullName) {

  public ProjectManager {
    Objects.requireNonNull(fullName);
    if (fullName.isBlank()) {
      throw new IllegalArgumentException("The name of a project manager must not be blank.");
    }
  }

  public static ProjectManager of(String fullName) {
    return new ProjectManager(fullName);
  }
}
