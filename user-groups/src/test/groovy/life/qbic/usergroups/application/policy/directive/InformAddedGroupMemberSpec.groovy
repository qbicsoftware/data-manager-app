package life.qbic.usergroups.application.policy.directive
import life.qbic.usergroups.application.policy.NoOpJobScheduler

import life.qbic.identity.api.UserInformationService
import life.qbic.usergroups.application.GroupService
import life.qbic.usergroups.application.InMemoryUserInformationService
import life.qbic.usergroups.application.communication.Content
import life.qbic.usergroups.application.communication.EmailService
import life.qbic.usergroups.application.communication.Recipient
import life.qbic.usergroups.application.communication.Subject
import life.qbic.usergroups.domain.event.MemberAddedToGroup
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
 * Tests for the {@link InformAddedGroupMember} directive: a newly added group member receives
 * an email about their new membership (notification profile GROUP-R-10).
 *
 * <p>The directive's {@code handleEvent} merely enqueues a JobRunr job; the observable behavior
 * (the email) lives in the {@code @Job}-annotated job body, which is exercised directly here —
 * constructing a real {@link JobScheduler} for the enqueue path would require the JobRunr
 * background-job generator (byte-buddy) not present in these unit tests.</p>
 */
class InformAddedGroupMemberSpec extends Specification {

  def "the added-member notification job emails the newly added member"() {
    given: "a real group service with a stored group containing the added member"
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
    def directive = new InformAddedGroupMember(emailService, new NoOpJobScheduler(),
        userInformationService, groupService)

    when: "the notification job runs for the added member"
    directive.notifyAddedMember(groupId, "bob")

    then: "the added user receives a polite email naming the group"
    sentSubject.content().contains("added to a user group")
    sentRecipient.address() == "bob@example.org"
    sentRecipient.fullName() == "bob"
    sentContent.content().contains("bob")
    sentContent.content().contains("NGS Lab")
  }

  def "a missing recipient user surfaces a directive execution error"() {
    given: "a directive whose user lookup returns nothing"
    def storage = new InMemoryGroupDataStorage()
    def repository = new GroupRepository(storage)
    DomainRegistry.instance().registerService(new GroupDomainService(repository))
    def groupService = new GroupService(repository,
        new InMemoryUserInformationService())
    def groupId = groupService.createAdHocGroup("alice", GroupName.from("NGS Lab"),
        GroupDescription.from("desc")).getValue().groupId().get()
    UserInformationService userInformationService = new InMemoryUserInformationService([] as Set)
    EmailService emailService = Mock(EmailService)
    def directive = new InformAddedGroupMember(emailService, new NoOpJobScheduler(),
        userInformationService, groupService)

    when: "the notify job runs for an unknown user"
    directive.notifyAddedMember(groupId, "ghost")

    then: "a directive execution exception is raised and no mail is sent"
    thrown(DirectiveExecutionException)
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