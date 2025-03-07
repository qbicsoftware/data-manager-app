package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.CreateNewSampleStatisticsEntry;

/**
 * <b>Project Registered Policy</b>
 * <p>
 * Implementation of QBiC's event policy, after a project has been registered.
 * <p>
 * The policy contains the directives:
 * <ul>
 *   <li>Create new sample statistics entry ({@link CreateNewSampleStatisticsEntry})</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ProjectRegisteredPolicy {

  public ProjectRegisteredPolicy(CreateNewSampleStatisticsEntry createNewSampleStatisticsEntry) {
    DomainEventDispatcher.instance().subscribe(requireNonNull(createNewSampleStatisticsEntry,
        "createNewSampleStatisticsEntry must not be null"));
  }

}
