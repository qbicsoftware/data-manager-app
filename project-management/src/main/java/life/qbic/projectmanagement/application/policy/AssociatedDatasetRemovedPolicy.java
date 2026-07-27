package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.InformProjectCollaboratorsAboutDatasetRemoval;

/**
 * <b>Policy: Associated Dataset Removed</b>
 *
 * <p>Business policy that needs to be executed after a dataset connection
 * has been removed from a project. Wire up the email notification
 * directive that informs every collaborator (except the actor).</p>
 *
 * @since 1.12.0
 */
public class AssociatedDatasetRemovedPolicy {

  public AssociatedDatasetRemovedPolicy(
      InformProjectCollaboratorsAboutDatasetRemoval informCollaborators) {
    DomainEventDispatcher.instance().subscribe(
        requireNonNull(informCollaborators, "informCollaborators must not be null"));
  }
}
