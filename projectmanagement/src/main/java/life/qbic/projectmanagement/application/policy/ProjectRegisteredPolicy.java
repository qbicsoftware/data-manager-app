package life.qbic.projectmanagement.application.policy;

import java.util.Objects;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.CreateNewSampleStatisticsEntry;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectRegisteredPolicy {

  private final CreateNewSampleStatisticsEntry createNewSampleStatisticsEntry;

  public ProjectRegisteredPolicy(CreateNewSampleStatisticsEntry createNewSampleStatisticsEntry) {
    this.createNewSampleStatisticsEntry = Objects.requireNonNull(createNewSampleStatisticsEntry);
    DomainEventDispatcher.instance().subscribe(createNewSampleStatisticsEntry);
  }

}
