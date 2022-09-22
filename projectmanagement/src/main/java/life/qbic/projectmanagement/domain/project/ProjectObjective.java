package life.qbic.projectmanagement.domain.project;

import java.util.Objects;

/**
 * The objective of a project.
 * <li> Must not be empty or null
 *
 * @since 1.0.0
 */
public record ProjectObjective(String objective) {

    public ProjectObjective {
        Objects.requireNonNull(objective);
        if (objective.isEmpty()) {
            throw new ProjectManagementDomainException("Project objective is empty.");
        }
    }

    public static ProjectObjective create(String objective) {
        return new ProjectObjective(objective);
    }
}
