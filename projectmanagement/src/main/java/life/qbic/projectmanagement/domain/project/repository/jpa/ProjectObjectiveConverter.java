package life.qbic.projectmanagement.domain.project.repository.jpa;

import life.qbic.projectmanagement.domain.project.ProjectObjective;

import javax.persistence.AttributeConverter;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ProjectObjectiveConverter implements AttributeConverter<ProjectObjective, String> {
    @Override
    public String convertToDatabaseColumn(ProjectObjective objective) {
        return objective.objective();
    }

    @Override
    public ProjectObjective convertToEntityAttribute(String s) {
        return ProjectObjective.create(s);
    }
}
