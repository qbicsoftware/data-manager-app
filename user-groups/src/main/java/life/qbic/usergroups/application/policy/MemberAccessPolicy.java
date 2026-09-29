package life.qbic.usergroups.application.policy;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.usergroups.application.policy.directive.InformAddedGroupMember;
import life.qbic.usergroups.application.policy.directive.InformRemovedGroupMember;

/**
 * <b>Policy: member access notification</b>
 * <p>
 * Business policy executed when the membership of an ad-hoc user group changes. Registers the
 * notification directives as subscribers to the in-process {@link DomainEventDispatcher}:
 * <ul>
 *   <li>{@link InformAddedGroupMember} — emails the newly added member (GROUP-R-10).</li>
 *   <li>{@link InformRemovedGroupMember} — emails the removed member about the revocation.</li>
 * </ul>
 * <p>
 * Mirrors the {@code UserRegisteredPolicy} / {@code ProjectAccessGrantedPolicy} wiring pattern:
 * the policy is a Spring {@code @Bean} created in the composition root so the dispatcher
 * subscription happens exactly once at startup.
 *
 * @since 1.20.0
 */
public class MemberAccessPolicy {

  public MemberAccessPolicy(InformAddedGroupMember informAddedGroupMember,
      InformRemovedGroupMember informRemovedGroupMember) {
    DomainEventDispatcher.instance()
        .subscribe(requireNonNull(informAddedGroupMember,
            "informAddedGroupMember must not be null"));
    DomainEventDispatcher.instance()
        .subscribe(requireNonNull(informRemovedGroupMember,
            "informRemovedGroupMember must not be null"));
  }
}