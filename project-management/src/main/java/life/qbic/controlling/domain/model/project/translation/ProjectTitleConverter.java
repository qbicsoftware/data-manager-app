package life.qbic.controlling.domain.model.project.translation;

import jakarta.persistence.AttributeConverter;
import life.qbic.controlling.domain.model.project.ProjectTitle;

/**
 * <b>Converts {@link ProjectTitle} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link ProjectTitle}. Additionally converts the {@link ProjectTitle} to a string value to be
 * stored in the database.
 * </p>
 *
 * @since 1.0.0
 */
public class ProjectTitleConverter implements AttributeConverter<ProjectTitle, String> {

  @Override
  public String convertToDatabaseColumn(ProjectTitle title) {
    return title.title();
  }

  @Override
  public ProjectTitle convertToEntityAttribute(String string) {
    return ProjectTitle.of(string);
  }
}
