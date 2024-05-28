package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponExperimentCreation;
import life.qbic.projectmanagement.domain.model.experiment.event.ExperimentCreatedEvent;

/**
 * <b>Policy: Experiment Created</b>
 * <p>
 * A collection of all directives that need to be executed after an experiment has been created
 * <p>
 * The policy subscribes to events of type {@link ExperimentCreatedEvent} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class ExperimentCreatedPolicy {

  /**
   * Creates an instance of a {@link ExperimentCreatedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProjectUponExperimentCreation directive to update the respective project
   *
   * @since 1.0.0
   */
  public ExperimentCreatedPolicy(
      UpdateProjectUponExperimentCreation updateProjectUponExperimentCreation) {
    DomainEventDispatcher.instance().subscribe(updateProjectUponExperimentCreation);
  }
}
