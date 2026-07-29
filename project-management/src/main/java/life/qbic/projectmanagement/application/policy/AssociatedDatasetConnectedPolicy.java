package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.InformProjectCollaboratorsAboutDatasetConnection;

/**
 * <b>Policy: Associated Dataset Connected</b>
 *
 * <p>Business policy that needs to be executed after a dataset has been
 * connected to a project. Currently wires the email notification
 * directive that informs every collaborator (except the actor).</p>
 *
 * @since 1.12.0
 */
public class AssociatedDatasetConnectedPolicy {

  public AssociatedDatasetConnectedPolicy(
      InformProjectCollaboratorsAboutDatasetConnection informCollaborators) {
    DomainEventDispatcher.instance().subscribe(requireNonNull(informCollaborators,
        "informCollaborators must not be null"));
  }
}
