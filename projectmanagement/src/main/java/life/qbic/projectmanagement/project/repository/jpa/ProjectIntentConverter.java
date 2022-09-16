package life.qbic.projectmanagement.project.repository.jpa;

import life.qbic.projectmanagement.project.ProjectIntent;

import javax.persistence.AttributeConverter;

/**
 * <b>Converts {@link ProjectIntent} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link ProjectIntent}. Additionally converts the
 * {@link ProjectIntent} to a string value to be stored in the database.
 * </p>
 *
 * @since 1.0.0
 */
public class ProjectIntentConverter implements AttributeConverter<ProjectIntent, String> {

  @Override
  public String convertToDatabaseColumn(ProjectIntent intent) {
    return intent.get();
  }

  @Override
  public ProjectIntent convertToEntityAttribute(String s) {
    return ProjectIntent.from(s);
  }
}
