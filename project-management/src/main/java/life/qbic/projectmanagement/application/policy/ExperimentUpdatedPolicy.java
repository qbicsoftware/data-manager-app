package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponExperimentUpdate;
import life.qbic.projectmanagement.domain.model.experiment.event.ExperimentUpdatedEvent;

/**
 * <b>Policy: Experiment Updated</b>
 * <p>
 * A collection of all directives that need to be executed after an experiment has been updated
 * <p>
 * The policy subscribes to events of type {@link ExperimentUpdatedEvent} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class ExperimentUpdatedPolicy {

  /**
   * Creates an instance of a {@link ExperimentUpdatedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProjectUponExperimentUpdate directive to update the respective project
   *
   * @since 1.0.0
   */
  public ExperimentUpdatedPolicy(
      UpdateProjectUponExperimentUpdate updateProjectUponExperimentUpdate) {
    DomainEventDispatcher.instance().subscribe(updateProjectUponExperimentUpdate);
  }
}
