package life.qbic.usergroups.application.policy.directive
import life.qbic.usergroups.application.policy.NoOpJobScheduler

import life.qbic.identity.api.UserInformationService
import life.qbic.usergroups.application.GroupService
import life.qbic.usergroups.application.InMemoryUserInformationService
import life.qbic.usergroups.application.communication.Content
import life.qbic.usergroups.application.communication.EmailService
import life.qbic.usergroups.application.communication.Recipient
import life.qbic.usergroups.application.communication.Subject
import life.qbic.usergroups.domain.event.MemberRemovedFromGroup
import life.qbic.usergroups.domain.model.GroupDescription
import life.qbic.usergroups.domain.model.GroupId
import life.qbic.usergroups.domain.model.GroupName
import life.qbic.usergroups.domain.model.UserGroup
import life.qbic.usergroups.domain.registry.DomainRegistry
import life.qbic.usergroups.domain.repository.GroupDataStorage
import life.qbic.usergroups.domain.repository.GroupRepository
import life.qbic.usergroups.domain.service.GroupDomainService
import spock.lang.Specification

/**
 * Tests for the {@link InformRemovedGroupMember} directive: a removed group member receives an
 * email about their revoked membership (notification profile GROUP-R-10).
 *
 * <p>The directive's {@code handleEvent} merely enqueues a JobRunr job; the observable behavior
 * (the email) lives in the {@code @Job}-annotated job body, which is exercised directly here.</p>
 */
class InformRemovedGroupMemberSpec extends Specification {

  def "the removed-member notification job emails the removed member"() {
    given: "a real group service with a stored group containing the member"
    def storage = new InMemoryGroupDataStorage()
    def repository = new GroupRepository(storage)
    DomainRegistry.instance().registerService(new GroupDomainService(repository))
    def groupService = new GroupService(repository,
        new InMemoryUserInformationService())
    def groupId = groupService.createAdHocGroup("alice", GroupName.from("NGS Lab"),
        GroupDescription.from("desc")).getValue().groupId().get()
    groupService.addMember(groupId, "alice", "bob")

    and: "bob exists as a user, and mail capture is wired through the EmailService"
    UserInformationService userInformationService = new InMemoryUserInformationService(["bob"] as Set)
    def sentSubject
    def sentRecipient
    def sentContent
    EmailService emailService = Mock(EmailService) {
      send(_, _, _) >> { Subject s, Recipient r, Content c -> sentSubject = s; sentRecipient = r; sentContent = c }
    }
    def directive = new InformRemovedGroupMember(emailService,
        new NoOpJobScheduler(), userInformationService, groupService)

    when: "the notification job runs for the removed member"
    directive.notifyRemovedMember(groupId, "bob")

    then: "the removed user receives a polite email naming the group"
    sentSubject.content().contains("removed from a user group")
    sentRecipient.address() == "bob@example.org"
    sentContent.content().contains("bob")
    sentContent.content().contains("NGS Lab")
  }

  def "a dissolved group still produces a removal email using the group id as fallback label"() {
    given: "a directive whose group can no longer be resolved (e.g. dissolved — not in storage)"
    def storage = new InMemoryGroupDataStorage()
    def repository = new GroupRepository(storage)
    DomainRegistry.instance().registerService(new GroupDomainService(repository))
    def groupService = new GroupService(repository,
        new InMemoryUserInformationService())
    UserInformationService userInformationService = new InMemoryUserInformationService(["bob"] as Set)
    def sentContent
    EmailService emailService = Mock(EmailService) {
      send(_, _, _) >> { Subject s, Recipient r, Content c -> sentContent = c }
    }
    def directive = new InformRemovedGroupMember(emailService,
        new NoOpJobScheduler(), userInformationService, groupService)

    when: "the notify job runs for a group that no longer resolves"
    directive.notifyRemovedMember("ghost-group", "bob")

    then: "the email still goes out with the group id as a label"
    sentContent.content().contains("ghost-group")
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