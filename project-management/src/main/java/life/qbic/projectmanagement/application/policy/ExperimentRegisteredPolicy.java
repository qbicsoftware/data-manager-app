package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectLastModified;

/**
 * <b>Experiment Registered Policy</b>
 * <p>
 * Implementation of QBiC's event policy, after an experiment has been registered.
 * <p>
 * The policy contains the directives:
 * <ul>
 *   <li>Update project modification timestamp ({@link UpdateProjectLastModified})</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ExperimentRegisteredPolicy {

  public ExperimentRegisteredPolicy(UpdateProjectLastModified updateProject) {
    DomainEventDispatcher.instance().subscribe(requireNonNull(updateProject,
        "updateProject must not be null"));
  }

}
