package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectLastModified;

/**
 * <b>Experiment Updated Policy</b>
 * <p>
 * Implementation of QBiC's event policy, after an experiment has been updated.
 * <p>
 * The policy contains the directives:
 * <ul>
 *   <li>Update project modification timestamp ({@link UpdateProjectLastModified})</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ExperimentUpdatedPolicy {

  public ExperimentUpdatedPolicy(UpdateProjectLastModified updateProject) {
    DomainEventDispatcher.instance().subscribe(requireNonNull(updateProject,
        "updateProject must not be null"));
  }

}
