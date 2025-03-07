package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.InformUserAboutGrantedAccess;

/**
 * <b>Policy: Project access granted</b>
 * <p>
 * Business policy that needs to be executed after a project access has been granted.
 *
 * @since 1.0.0
 */
public class ProjectAccessGrantedPolicy {

  public ProjectAccessGrantedPolicy(InformUserAboutGrantedAccess informUserAboutGrantedAccess) {
    DomainEventDispatcher.instance().subscribe(requireNonNull(informUserAboutGrantedAccess,
        "informUserAboutGrantedAccess must not be null"));
  }

}
