package life.qbic.projectmanagement.domain.project;

import static java.util.Objects.requireNonNull;

/**
 * A project intent contains information on the project that is related to the intent of the
 * project.
 */
public record ProjectIntent(ProjectTitle projectTitle) {

  public ProjectIntent {
    requireNonNull(projectTitle);
  }
}
