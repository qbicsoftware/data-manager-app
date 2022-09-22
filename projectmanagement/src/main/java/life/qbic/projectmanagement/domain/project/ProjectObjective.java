package life.qbic.projectmanagement.domain.project;

import java.util.Objects;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
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
