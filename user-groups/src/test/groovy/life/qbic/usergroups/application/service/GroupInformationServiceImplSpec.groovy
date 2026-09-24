package life.qbic.usergroups.application.service

import java.time.Instant

import life.qbic.usergroups.api.GroupInfo
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupRole
import life.qbic.usergroups.api.GroupType
import life.qbic.usergroups.api.MyGroupMembership
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
 * Tests for the {@link GroupInformationServiceImpl} API facade.
 *
 * <p>Verifies the mapping from domain/application projections to the self-contained API DTOs,
 * the exclusion of dissolved groups, and the visibility rule (my-groups is member-only, the
 * public directory never carries membership information).</p>
 */
class GroupInformationServiceImplSpec extends Specification {

  private static Instant NOW = Instant.parse("2026-09-22T10:00:00Z")

  private GroupDomainService domainService

  private InMemoryGroupDataStorage storage

  private GroupRepository repository

  private GroupService groupService

  private GroupInformationService service

  def setup() {
    storage = new InMemoryGroupDataStorage()
    repository = new GroupRepository(storage)
    domainService = new GroupDomainService(repository)
    DomainRegistry.instance().registerService(domainService)
    groupService = new GroupService(repository)
    service = new GroupInformationServiceImpl(groupService)
  }

  def cleanup() {
    DomainRegistry.instance().registerService(null)
  }

  def "listPublicDirectory maps to API DTOs with only identity, name, description and type"() {
    given: "an active group with description and a dissolved group"
    def groupId = GroupId.create()
    domainService.createAdHocGroup(groupId, GroupName.from("NGS Lab"),
        GroupDescription.from("A test lab"), "alice", NOW)
    def goneId = GroupId.create()
    domainService.createAdHocGroup(goneId, GroupName.from("Gone Group"),
        GroupDescription.from("gone"), "bob", NOW)
    domainService.removeMembership(goneId, "bob")

    when:
    List<GroupInfo> directory = service.listPublicDirectory()

    then: "only the active group appears"
    directory.size() == 1
    directory.get(0).id() == groupId.get()
    directory.get(0).name() == "NGS Lab"
    directory.get(0).description() == "A test lab"
    directory.get(0).type() == GroupType.ADHOC
  }

  def "findGroupById resolves active groups and returns empty for dissolved or unknown groups"() {
    given: "an active group and a dissolved group"
    def activeId = GroupId.create()
    domainService.createAdHocGroup(activeId, GroupName.from("Active"), DESC, "alice", NOW)
    def goneId = GroupId.create()
    domainService.createAdHocGroup(goneId, GroupName.from("Gone"), DESC, "bob", NOW)
    domainService.removeMembership(goneId, "bob")

    expect: "active resolves, dissolved and unknown resolve to empty"
    service.findGroupById(activeId.get()).isPresent()
    service.findGroupById(goneId.get()).isEmpty()
    service.findGroupById(GroupId.create().get()).isEmpty()
    service.findGroupById("not-a-uuid").isEmpty()
  }

  def "listMyGroups returns only the caller's own memberships, mapped with their role"() {
    given: "alice owns a group, bob owns another, and alice's group is joined by bob"
    GroupId aliceGroup = GroupId.create()
    domainService.createAdHocGroup(aliceGroup, GroupName.from("Alice's Group"), DESC, "alice", NOW)
    GroupId bobGroup = GroupId.create()
    domainService.createAdHocGroup(bobGroup, GroupName.from("Bob's Group"), DESC, "bob", NOW)

    when:
    List<MyGroupMembership> aliceGroups = service.listMyGroups("alice")

    then: "only alice's own membership appears, with role OWNER, mapped to the API enum"
    aliceGroups.size() == 1
    aliceGroups.get(0).groupId() == aliceGroup.get()
    aliceGroups.get(0).groupName() == "Alice's Group"
    aliceGroups.get(0).groupType() == GroupType.ADHOC
    aliceGroups.get(0).myRole() == GroupRole.OWNER

    and: "bob's group is not exposed to alice"
    aliceGroups*.groupId().contains(bobGroup.get()) == false
  }

  def "listMyGroups carries the total member count of each group"() {
    given: "a group owned by alice with bob added as a second member"
    GroupId aliceGroup = GroupId.create()
    domainService.createAdHocGroup(aliceGroup, GroupName.from("Alice's Group"), DESC, "alice", NOW)
    domainService.addMember(aliceGroup, "alice", "bob", NOW)

    and: "a solo group owned by bob"
    GroupId bobGroup = GroupId.create()
    domainService.createAdHocGroup(bobGroup, GroupName.from("Bob's Group"), DESC, "bob", NOW)

    when:
    List<MyGroupMembership> aliceGroups = service.listMyGroups("alice")
    List<MyGroupMembership> bobGroups = service.listMyGroups("bob")

    then: "alice sees the full roster size of her group (2) and bob sees his solo group (1)"
    aliceGroups.find { it.groupId() == aliceGroup.get() }.memberCount() == 2
    bobGroups.find { it.groupId() == bobGroup.get() }.memberCount() == 1
  }

  def "isGroupNameAvailable is case-insensitive"() {
    given: "a group named 'NGS Lab' exists"
    domainService.createAdHocGroup(GroupId.create(), GroupName.from("NGS Lab"), DESC, "alice", NOW)

    expect: "exact and different-case variations are unavailable, fresh names are available"
    !service.isGroupNameAvailable("NGS Lab")
    !service.isGroupNameAvailable("ngs lab")
    service.isGroupNameAvailable("Proteomics Core")
  }

  private static GroupDescription DESC = GroupDescription.from("desc")

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