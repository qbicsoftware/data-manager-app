package life.qbic.projectmanagement.domain.project.repository.jpa;

import life.qbic.projectmanagement.domain.project.ProjectObjective;

import javax.persistence.AttributeConverter;

/**
 * <b>Converts {@link ProjectObjective} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link ProjectObjective}. Additionally converts the
 * {@link ProjectObjective} to a string value to be stored in the database.
 * </p>
 *
 * @since 1.0.0
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
