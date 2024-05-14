package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectLastModified;

/**
 * <b>Measurements Registered Policy</b>
 * <p>
 * Implementation of QBiC's event policy, after one or more measurements were registered.
 * <p>
 * The policy contains the directives:
 * <ul>
 *   <li>Update project modification timestamp ({@link UpdateProjectLastModified})</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class MeasurementsRegisteredPolicy {

  public MeasurementsRegisteredPolicy(UpdateProjectLastModified updateProject) {
    DomainEventDispatcher.instance().subscribe(requireNonNull(updateProject,
        "updateProject must not be null"));
  }

}
