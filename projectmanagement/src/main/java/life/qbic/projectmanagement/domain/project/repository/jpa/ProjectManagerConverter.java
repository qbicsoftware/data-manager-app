package life.qbic.projectmanagement.domain.project.repository.jpa;

import javax.persistence.AttributeConverter;
import life.qbic.projectmanagement.domain.project.ProjectManager;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ProjectManagerConverter implements AttributeConverter<ProjectManager, String> {

  @Override
  public String convertToDatabaseColumn(ProjectManager attribute) {
    return null;
  }

  @Override
  public ProjectManager convertToEntityAttribute(String dbData) {
    return null;
  }
}
