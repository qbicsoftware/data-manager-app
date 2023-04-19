package life.qbic.datamanager.views.projects.project.info;

import java.util.Objects;

/**
 * <p>Represents a project link in the {@link ProjectLinksComponent}</p>
 *
 * @since 1.0.0
 */
public record ProjectLink(String type, String reference) {

  public ProjectLink {
    if (Objects.isNull(type)) {
      throw new IllegalArgumentException("No type provided. Input is null");
    }
    Objects.requireNonNull(type);

    if (Objects.isNull(reference)) {
      throw new IllegalArgumentException("No type provided. Input is null");
    }
    Objects.requireNonNull(reference);
  }

  public static ProjectLink of(String type, String reference) {
    return new ProjectLink(type, reference);
  }
}
