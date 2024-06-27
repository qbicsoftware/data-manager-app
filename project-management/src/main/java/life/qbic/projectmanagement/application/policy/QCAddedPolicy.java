package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponQCCreation;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControlCreatedEvent;

/**
 * <b>Policy: Quality Control Object Added</b>
 * <p>
 * A collection of all directives that need to be executed after a QC object has been created
 * <p>
 * The policy subscribes to events of type {@link QualityControlCreatedEvent} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class QCAddedPolicy {

  /**
   * Creates an instance of a {@link QCAddedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProjectUponQCCreation directive to update the respective project
   *
   * @since 1.0.0
   */
  public QCAddedPolicy(
      UpdateProjectUponQCCreation updateProjectUponQCCreation) {
    DomainEventDispatcher.instance().subscribe(updateProjectUponQCCreation);
  }
}
