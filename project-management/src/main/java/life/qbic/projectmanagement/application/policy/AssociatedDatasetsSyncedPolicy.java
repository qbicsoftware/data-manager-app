package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.InformProjectCollaboratorsAboutDatasetSync;

/**
 * <b>Policy: Associated Datasets Synced</b>
 *
 * <p>Business policy that needs to be executed after a sync trigger
 * updated one or more connected datasets (DATSET-04/08, ADR-0005).
 * Currently wires the email notification directive that informs every
 * collaborator (except the actor) with a single combined email listing
 * the updated records.</p>
 *
 * @since 1.13.0
 */
public class AssociatedDatasetsSyncedPolicy {

  public AssociatedDatasetsSyncedPolicy(
      InformProjectCollaboratorsAboutDatasetSync informCollaborators) {
    DomainEventDispatcher.instance().subscribe(requireNonNull(informCollaborators,
        "informCollaborators must not be null"));
  }
}