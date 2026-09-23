package life.qbic.usergroups.application.service

import java.time.Instant

import life.qbic.usergroups.api.GroupManagementService
import life.qbic.usergroups.api.GroupMember
import life.qbic.usergroups.api.GroupRole
import life.qbic.usergroups.domain.model.GroupDescription
import life.qbic.usergroups.domain.model.GroupId
import life.qbic.usergroups.domain.model.GroupName
import life.qbic.usergroups.domain.model.UserGroup
import life.qbic.usergroups.domain.registry.DomainRegistry
import life.qbic.usergroups.domain.repository.GroupDataStorage
import life.qbic.usergroups.domain.repository.GroupRepository
import life.qbic.usergroups.domain.service.GroupDomainService
import life.qbic.usergroups.application.GroupService
import spock.lang.Specification

/**
 * Tests for the {@link GroupManagementServiceImpl} API facade.
 *
 * <p>Verifies the command contract (role gates, member mutations, rename/dissolve) and the
 * member-listing visibility rule through the self-contained API DTOs.</p>
 */
class GroupManagementServiceImplSpec extends Specification {

  private static Instant NOW = Instant.parse("2026-09-22T10:00:00Z")

  private static GroupName NAME = GroupName.from("NGS Lab")

  private static GroupDescription DESC = GroupDescription.from("A test lab")

  private GroupDomainService domainService

  private InMemoryGroupDataStorage storage

  private GroupRepository repository

  private GroupService groupService

  private GroupManagementService service

  def setup() {
    storage = new InMemoryGroupDataStorage()
    repository = new GroupRepository(storage)
    domainService = new GroupDomainService(repository)
    DomainRegistry.instance().registerService(domainService)
    groupService = new GroupService(repository)
    service = new GroupManagementServiceImpl(groupService)
  }

  def cleanup() {
    DomainRegistry.instance().registerService(null)
  }

  private String createGroupOwnedBy(String owner) {
    return groupService.createAdHocGroup(owner, NAME, DESC).getValue().groupId().get()
  }

  def "addMember adds a regular member and listMembers exposes the roster to members only"() {
    given: "a group owned by alice"
    String groupId = createGroupOwnedBy("alice")

    when:
    service.addMember(groupId, "alice", "bob")

    then: "a member sees the roster with the API DTO roles"
    List<GroupMember> roster = service.listMembers(groupId, "alice")
    roster*.userId().containsAll(["alice", "bob"])
    roster.find { it.userId() == "alice" }.role() == GroupRole.OWNER
    roster.find { it.userId() == "bob" }.role() == GroupRole.MEMBER

    and: "a stranger sees nothing"
    service.listMembers(groupId, "stranger").isEmpty()
  }

  def "a plain member calling addMember is rejected with IllegalArgumentException"() {
    given: "a group with a plain member"
    String groupId = createGroupOwnedBy("alice")
    groupService.addMember(groupId, "alice", "bob")

    when:
    service.addMember(groupId, "bob", "carol")

    then:
    thrown(IllegalArgumentException)
  }

  def "appointManager + demoteManager change the member's role"() {
    given: "a group with a member"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")

    when: "alice appoints bob as manager"
    service.appointManager(groupId, "alice", "bob")

    then:
    service.listMembers(groupId, "alice").find { it.userId() == "bob" }.role() == GroupRole.MANAGER

    when: "alice demotes bob back"
    service.demoteManager(groupId, "alice", "bob")

    then:
    service.listMembers(groupId, "alice").find { it.userId() == "bob" }.role() == GroupRole.MEMBER
  }

  def "renameGroup and updateDescription update the group profile"() {
    given: "a group"
    String groupId = createGroupOwnedBy("alice")

    when:
    service.renameGroup(groupId, "alice", "Renamed Lab")
    service.updateDescription(groupId, "alice", "new description")

    then:
    def group = storage.findById(GroupId.from(groupId)).get()
    group.name().value() == "Renamed Lab"
    group.description().value().get() == "new description"
  }

  def "dissolveGroup dissolves the group and removes it from the directory"() {
    given: "a group with a member"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")

    when:
    service.dissolveGroup(groupId, "alice")

    then:
    storage.findById(GroupId.from(groupId)).get().status() ==
        life.qbic.usergroups.domain.model.GroupStatus.DISSOLVED
    groupService.listPublicDirectory().findAll { it.groupId().get() == groupId }.isEmpty()
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
      return groups.values().stream().filter(g -> g.isActive()).toList()
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