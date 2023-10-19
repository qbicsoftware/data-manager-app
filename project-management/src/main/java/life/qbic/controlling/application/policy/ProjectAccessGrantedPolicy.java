package life.qbic.controlling.application.policy;

import java.util.Objects;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.controlling.application.policy.directive.InformUserAboutGrantedAccess;

/**
 * <b>Policy: Project access granted</b>
 * <p>
 * Business policy that needs to be executed after a project access has been granted.
 *
 * @since 1.0.0
 */
public class ProjectAccessGrantedPolicy {

  private InformUserAboutGrantedAccess informUserAboutGrantedAccess;

  public ProjectAccessGrantedPolicy(InformUserAboutGrantedAccess informUserAboutGrantedAccess) {
    this.informUserAboutGrantedAccess = Objects.requireNonNull(informUserAboutGrantedAccess);
    DomainEventDispatcher.instance().subscribe(informUserAboutGrantedAccess);
  }

}
