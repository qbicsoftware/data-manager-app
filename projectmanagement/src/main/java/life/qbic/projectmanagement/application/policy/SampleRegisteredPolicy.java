package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.AddSampleToBatch;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SampleRegisteredPolicy {

  private final AddSampleToBatch addSampleToBatch;

  public SampleRegisteredPolicy(AddSampleToBatch addSampleToBatch) {
    this.addSampleToBatch = addSampleToBatch;
    DomainEventDispatcher.instance().subscribe(this.addSampleToBatch);
  }
}
