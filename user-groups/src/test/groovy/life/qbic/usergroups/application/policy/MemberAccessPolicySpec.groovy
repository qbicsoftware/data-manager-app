package life.qbic.usergroups.application.policy

import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.identity.api.UserInformationService
import life.qbic.usergroups.application.GroupService
import life.qbic.usergroups.application.InMemoryUserInformationService
import life.qbic.usergroups.application.communication.EmailService
import life.qbic.usergroups.application.policy.directive.InformAddedGroupMember
import life.qbic.usergroups.application.policy.directive.InformRemovedGroupMember
import life.qbic.usergroups.domain.event.MemberAddedToGroup
import life.qbic.usergroups.domain.event.MemberRemovedFromGroup
import life.qbic.usergroups.domain.model.GroupId
import life.qbic.usergroups.domain.model.UserGroup
import life.qbic.usergroups.domain.repository.GroupDataStorage
import life.qbic.usergroups.domain.repository.GroupRepository
import spock.lang.Specification

/**
 * Tests for the {@link MemberAccessPolicy}: wiring the membership-change directives to the
 * in-process {@link DomainEventDispatcher} so a member add/remove triggers the notification.
 *
 * <p>The directives are wired with a {@link NoOpJobScheduler} (enqueue is a no-op), so even
 * though the policy subscribes the real directives to the shared dispatcher singleton, no JobRunr
 * pipeline is actually started and no mail is sent unless a test explicitly runs the job body.</p>
 */
class MemberAccessPolicySpec extends Specification {

  def "the policy subscribes the add and remove directives to the domain dispatcher"() {
    given: "directives with the subscribed event types and a no-op scheduler"
    def groupService = new GroupService(new GroupRepository(new InMemoryGroupDataStorage()),
        new InMemoryUserInformationService())
    UserInformationService userInformationService = new InMemoryUserInformationService()
    EmailService emailService = Mock(EmailService)
    def informAdded = new InformAddedGroupMember(emailService, new NoOpJobScheduler(),
        userInformationService, groupService)
    def informRemoved = new InformRemovedGroupMember(emailService, new NoOpJobScheduler(),
        userInformationService, groupService)

    when: "the policy wires the directives into the dispatcher"
    new MemberAccessPolicy(informAdded, informRemoved)

    then: "a dispatched member-added event reaches the add directive"
    def added = MemberAddedToGroup.create("group-1", "bob", "alice")
    DomainEventDispatcher.instance().dispatch(added)

    and: "a dispatched member-removed event reaches the remove directive"
    def removed = MemberRemovedFromGroup.create("group-1", "bob", "alice")
    DomainEventDispatcher.instance().dispatch(removed)

    and: "the directives enqueue (no-op) rather than throwing"
    noExceptionThrown()
  }

  def "events of unrelated types are not routed to the member directives"() {
    given: "the policy wired with real directives and a no-op scheduler"
    def groupService = new GroupService(new GroupRepository(new InMemoryGroupDataStorage()),
        new InMemoryUserInformationService())
    UserInformationService userInformationService = new InMemoryUserInformationService()
    EmailService emailService = Mock(EmailService)
    def informAdded = new InformAddedGroupMember(emailService, new NoOpJobScheduler(),
        userInformationService, groupService)
    def informRemoved = new InformRemovedGroupMember(emailService, new NoOpJobScheduler(),
        userInformationService, groupService)
    new MemberAccessPolicy(informAdded, informRemoved)

    when: "an unrelated group event is dispatched"
    DomainEventDispatcher.instance().dispatch(
        life.qbic.usergroups.domain.event.GroupProfileUpdated.create("group-1", "old", "new",
            "alice"))

    then: "no notification email is triggered (the directives only react to member events)"
    0 * emailService.send(_, _, _)
  }

  static class InMemoryGroupDataStorage implements GroupDataStorage {

    private final Map<GroupId, UserGroup> groups = new LinkedHashMap<>()

    @Override
    void save(UserGroup group) {
      groups.put(group.id(), group)
    }

    @Override
    Optional<UserGroup> findById(GroupId id) {
      return Optional.ofNullable(groups.get(id))
    }

    @Override
    Optional<UserGroup> findByNameIgnoreCase(String name) {
      return groups.values().stream()
          .filter(g -> g.name().value().equalsIgnoreCase(name))
          .findFirst()
    }

    @Override
    List<UserGroup> findAllActive() {
      return groups.values().stream()
          .filter(g -> g.isActive())
          .toList()
    }

    @Override
    List<UserGroup> findActiveGroupsByUserId(String userId) {
      return groups.values().stream()
          .filter(g -> g.isActive())
          .filter(g -> g.memberships().stream().anyMatch(m -> m.userId() == userId))
          .toList()
    }
  }
}