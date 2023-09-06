package life.qbic.projectmanagement.application.policy;

import java.util.Objects;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.InformUserAboutGrantedAccess;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectAccessGrantedPolicy {

  private InformUserAboutGrantedAccess informUserAboutGrantedAccess;

  public ProjectAccessGrantedPolicy(InformUserAboutGrantedAccess informUserAboutGrantedAccess) {
    this.informUserAboutGrantedAccess = Objects.requireNonNull(informUserAboutGrantedAccess);
    DomainEventDispatcher.instance().subscribe(informUserAboutGrantedAccess);
  }

}
