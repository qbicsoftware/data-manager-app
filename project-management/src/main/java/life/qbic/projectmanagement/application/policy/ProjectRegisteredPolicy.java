package life.qbic.projectmanagement.application.policy;

import java.util.Objects;
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

  private final CreateNewSampleStatisticsEntry createNewSampleStatisticsEntry;

  public ProjectRegisteredPolicy(CreateNewSampleStatisticsEntry createNewSampleStatisticsEntry) {
    this.createNewSampleStatisticsEntry = Objects.requireNonNull(createNewSampleStatisticsEntry);
    DomainEventDispatcher.instance().subscribe(createNewSampleStatisticsEntry);
  }

}
