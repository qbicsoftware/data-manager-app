package life.qbic.datamanager.views.groups.notifications;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.usergroups.domain.event.MemberAddedToGroup;
import life.qbic.usergroups.domain.event.MemberRemovedFromGroup;

/**
 * <b>Policy: inform project administrators about membership changes on shared groups</b>
 *
 * <p>Registers the {@link InformProjectAdministratorsAboutMembershipChange} directive with the
 * in-process {@link DomainEventDispatcher} for both membership-change event types of the user
 * groups context ({@link MemberAddedToGroup} and {@link MemberRemovedFromGroup}). The dispatcher
 * binds a subscriber instance to exactly one event type, so the directive (a plain component)
 * is wrapped in two thin {@code DomainEventSubscriber} adapters here.</p>
 *
 * <p>Mirrors the {@code MemberAccessPolicy} wiring pattern: this is a Spring bean created in the
 * composition root so the dispatcher subscription happens exactly once at startup.</p>
 *
 * @since 1.22.0
 */
public class InformProjectAdministratorsPolicy {

  public InformProjectAdministratorsPolicy(
      InformProjectAdministratorsAboutMembershipChange directive) {
    requireNonNull(directive, "directive must not be null");
    DomainEventDispatcher.instance().subscribe(new DomainEventSubscriber<MemberAddedToGroup>() {
      @Override
      public Class<? extends life.qbic.domain.concepts.DomainEvent> subscribedToEventType() {
        return MemberAddedToGroup.class;
      }

      @Override
      public void handleEvent(MemberAddedToGroup event) {
        directive.handleMemberChange(event.groupId());
      }
    });
    DomainEventDispatcher.instance().subscribe(new DomainEventSubscriber<MemberRemovedFromGroup>() {
      @Override
      public Class<? extends life.qbic.domain.concepts.DomainEvent> subscribedToEventType() {
        return MemberRemovedFromGroup.class;
      }

      @Override
      public void handleEvent(MemberRemovedFromGroup event) {
        directive.handleMemberChange(event.groupId());
      }
    });
  }
}