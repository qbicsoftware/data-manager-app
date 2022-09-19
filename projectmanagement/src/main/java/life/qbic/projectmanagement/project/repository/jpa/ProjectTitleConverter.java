package life.qbic.projectmanagement.project.repository.jpa;

import life.qbic.projectmanagement.project.ProjectTitle;

import javax.persistence.AttributeConverter;

/**
 * <b>Converts {@link ProjectTitle} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link ProjectTitle}. Additionally converts the
 * {@link ProjectTitle} to a string value to be stored in the database.
 * </p>
 *
 * @since 1.0.0
 */
public class ProjectTitleConverter implements AttributeConverter<ProjectTitle, String> {

  @Override
  public String convertToDatabaseColumn(ProjectTitle title) {
    return title.get();
  }

  @Override
  public ProjectTitle convertToEntityAttribute(String s) {
    return ProjectTitle.from(s);
  }
}
