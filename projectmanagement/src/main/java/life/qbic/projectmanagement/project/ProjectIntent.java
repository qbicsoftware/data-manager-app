package life.qbic.projectmanagement.project;

import static java.util.Objects.requireNonNull;

/**
 * A project intent contains information on the project that is related to the intent of the
 * project.
 */
public record ProjectIntent(ProjectTitle projectTitle) {

  public ProjectIntent {
    requireNonNull(projectTitle);
  }

  public static ProjectIntent from(String s) {
    return new ProjectIntent(new ProjectTitle(s));
  }

  public String get() {
    return projectTitle.title();
  }
}
