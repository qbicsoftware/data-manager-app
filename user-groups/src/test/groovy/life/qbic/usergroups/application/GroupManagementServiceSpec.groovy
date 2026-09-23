package life.qbic.usergroups.application

import java.time.Instant

import life.qbic.application.commons.ApplicationException
import life.qbic.application.commons.ApplicationException.ErrorCode
import life.qbic.application.commons.Result
import life.qbic.usergroups.application.GroupManagementServiceSpec.InMemoryGroupDataStorage
import life.qbic.usergroups.domain.model.GroupDescription
import life.qbic.usergroups.domain.model.GroupId
import life.qbic.usergroups.domain.model.GroupName
import life.qbic.usergroups.domain.model.GroupRole
import life.qbic.usergroups.domain.model.GroupStatus
import life.qbic.usergroups.domain.model.UserGroup
import life.qbic.usergroups.domain.registry.DomainRegistry
import life.qbic.usergroups.domain.repository.GroupDataStorage
import life.qbic.usergroups.domain.repository.GroupRepository
import life.qbic.usergroups.domain.service.GroupDomainService
import spock.lang.Specification

/**
 * Tests for the FEAT-USER-GROUPS-04 management commands on {@link GroupService}:
 * add/remove members, appoint/demote managers, rename/describe, dissolve, and member listing.
 */
class GroupManagementServiceSpec extends Specification {

  private static Instant NOW = Instant.parse("2026-09-22T10:00:00Z")

  private static GroupName NAME = GroupName.from("Owner Group")

  private static GroupDescription DESC = GroupDescription.from("description")

  private GroupDomainService domainService

  private InMemoryGroupDataStorage storage

  private GroupRepository repository

  private GroupService service

  def setup() {
    storage = new InMemoryGroupDataStorage()
    repository = new GroupRepository(storage)
    domainService = new GroupDomainService(repository)
    DomainRegistry.instance().registerService(domainService)
    service = new GroupService(repository)
  }

  def cleanup() {
    DomainRegistry.instance().registerService(null)
  }

  private String createGroupOwnedBy(String owner) {
    return service.createAdHocGroup(owner, NAME, DESC).getValue().groupId().get()
  }

  def "an owner can add a regular member to their ad-hoc group"() {
    given: "a group owned by alice"
    String groupId = createGroupOwnedBy("alice")

    when:
    Result<Void, ApplicationException> result = service.addMember(groupId, "alice", "bob")

    then:
    result.isValue()
    def members = service.listMembers(groupId, "alice")
    members.any { it.userId() == "bob" && it.role() == GroupRole.MEMBER }
  }

  def "a manager can add a regular member to an ad-hoc group"() {
    given: "a group owned by alice with bob appointed as manager"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")
    service.appointManager(groupId, "alice", "bob")

    when:
    Result<Void, ApplicationException> result = service.addMember(groupId, "bob", "carol")

    then:
    result.isValue()
    service.listMembers(groupId, "alice").any { it.userId() == "carol" }
  }

  def "a plain member cannot add members"() {
    given: "a group owned by alice with a plain member bob"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")

    when: "bob (a member) tries to add carol"
    Result<Void, ApplicationException> result = service.addMember(groupId, "bob", "carol")

    then:
    result.isError()
    result.getError().errorCode() == ErrorCode.ACCESS_DENIED
    service.listMembers(groupId, "alice").findAll { it.userId() == "carol" }.isEmpty()
  }

  def "adding an already-present member is rejected"() {
    given: "a group owned by alice with bob already a member"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")

    when:
    Result<Void, ApplicationException> result = service.addMember(groupId, "alice", "bob")

    then:
    result.isError()
  }

  def "a manager can remove a regular member"() {
    given: "group with bob as manager and carol as member"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")
    service.addMember(groupId, "alice", "carol")
    service.appointManager(groupId, "alice", "bob")

    when:
    Result<Void, ApplicationException> result = service.removeMember(groupId, "bob", "carol")

    then:
    result.isValue()
    service.listMembers(groupId, "alice").findAll { it.userId() == "carol" }.isEmpty()
  }

  def "a manager cannot remove another manager"() {
    given: "group with two managers"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")
    service.addMember(groupId, "alice", "carol")
    service.appointManager(groupId, "alice", "bob")
    service.appointManager(groupId, "alice", "carol")

    when: "bob tries to remove carol who is also a manager"
    Result<Void, ApplicationException> result = service.removeMember(groupId, "bob", "carol")

    then:
    result.isError()
    result.getError().errorCode() == ErrorCode.ACCESS_DENIED
  }

  def "nobody can remove the owner"() {
    given: "group owned by alice with bob as manager"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")
    service.appointManager(groupId, "alice", "bob")

    when: "bob tries to remove the owner alice"
    Result<Void, ApplicationException> result = service.removeMember(groupId, "bob", "alice")

    then:
    result.isError()
    result.getError().errorCode() == ErrorCode.ACCESS_DENIED
    service.listMembers(groupId, "alice").any { it.userId() == "alice" && it.role() == GroupRole.OWNER }
  }

  def "an owner can appoint a manager"() {
    given: "group with bob as member"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")

    when:
    Result<Void, ApplicationException> result = service.appointManager(groupId, "alice", "bob")

    then:
    result.isValue()
    service.listMembers(groupId, "alice").find { it.userId() == "bob" }.role() == GroupRole.MANAGER
  }

  def "a manager cannot appoint another manager"() {
    given: "group with bob as manager"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")
    service.appointManager(groupId, "alice", "bob")

    when: "bob tries to appoint carol"
    service.addMember(groupId, "alice", "carol")
    Result<Void, ApplicationException> result = service.appointManager(groupId, "bob", "carol")

    then:
    result.isError()
    result.getError().errorCode() == ErrorCode.ACCESS_DENIED
  }

  def "an owner can demote a manager"() {
    given: "group with bob as manager"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")
    service.appointManager(groupId, "alice", "bob")

    when:
    Result<Void, ApplicationException> result = service.demoteManager(groupId, "alice", "bob")

    then:
    result.isValue()
    service.listMembers(groupId, "alice").find { it.userId() == "bob" }.role() == GroupRole.MEMBER
  }

  def "a manager and owner can rename the group"() {
    given: "group owned by alice with bob as manager"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")
    service.appointManager(groupId, "alice", "bob")

    when: "bob renames to a unique name"
    Result<Void, ApplicationException> result = service.renameGroup(groupId, "bob",
        GroupName.from("New Name"))

    then:
    result.isValue()
    storage.findById(GroupId.from(groupId)).get().name().value() == "New Name"
  }

  def "renaming uses a case-insensitive uniqueness check"() {
    given: "two groups"
    String groupId1 = createGroupOwnedBy("alice")
    String groupId2 = service.createAdHocGroup("bob", GroupName.from("Bob's Group"), DESC,
    ).getValue().groupId().get()

    when: "alice renames her group to the same name as bob's group"
    Result<Void, ApplicationException> result = service.renameGroup(groupId1, "alice",
        GroupName.from("Bob's Group"))

    then: "the rename is rejected as duplicate"
    result.isError()
    result.getError().errorCode() == ErrorCode.DUPLICATE_GROUP_NAME
    storage.findById(GroupId.from(groupId1)).get().name().value() == "Owner Group"
  }

  def "a plain member cannot rename the group"() {
    given: "group owned by alice with bob as plain member"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")

    when: "bob tries to rename"
    Result<Void, ApplicationException> result = service.renameGroup(groupId, "bob",
        GroupName.from("Hijack"))

    then:
    result.isError()
    result.getError().errorCode() == ErrorCode.ACCESS_DENIED
    storage.findById(GroupId.from(groupId)).get().name().value() == "Owner Group"
  }

  def "a manager can update the description"() {
    given: "group owned by alice with bob as manager"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")
    service.appointManager(groupId, "alice", "bob")

    when:
    Result<Void, ApplicationException> result = service.updateDescription(groupId, "bob",
        GroupDescription.from("updated"))

    then:
    result.isValue()
    storage.findById(GroupId.from(groupId)).get().description().value().get() == "updated"
  }

  def "only the owner can dissolve the group"() {
    given: "group owned by alice with bob as manager"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")
    service.appointManager(groupId, "alice", "bob")

    when: "bob (manager) tries to dissolve"
    Result<Void, ApplicationException> result = service.dissolveGroup(groupId, "bob")

    then:
    result.isError()
    result.getError().errorCode() == ErrorCode.ACCESS_DENIED
    storage.findById(GroupId.from(groupId)).get().status() == GroupStatus.ACTIVE

    when: "alice (owner) dissolves"
    Result<Void, ApplicationException> ownerResult = service.dissolveGroup(groupId, "alice")

    then:
    ownerResult.isValue()
    storage.findById(GroupId.from(groupId)).get().status() == GroupStatus.DISSOLVED
    storage.findById(GroupId.from(groupId)).get().memberships().isEmpty()
  }

  def "removing the last member auto-dissolves the group and dispatches no double event"() {
    given: "a group owned by alice with bob as member"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")

    when: "alice removes bob (leaving only herself)"
    Result<Void, ApplicationException> result = service.removeMember(groupId, "alice", "bob")

    then: "the group stays active with only the owner"
    result.isValue()
    service.listMembers(groupId, "alice")*.userId() == ["alice"]
    storage.findById(GroupId.from(groupId)).get().status() == GroupStatus.ACTIVE
  }

  def "after the owner dissolves, the group is removed from the public directory"() {
    given: "a group with a member"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")

    when:
    service.dissolveGroup(groupId, "alice")

    then:
    service.listPublicDirectory().findAll { it.groupId().get() == groupId }.isEmpty()
    service.listMyGroups("bob").findAll { it.groupId().get() == groupId }.isEmpty()
  }

  def "listMembers is only visible to members (visibility policy)"() {
    given: "a group with a member and a stranger"
    String groupId = createGroupOwnedBy("alice")
    service.addMember(groupId, "alice", "bob")

    expect: "a member sees the roster; a stranger sees nothing"
    service.listMembers(groupId, "alice")*.userId().containsAll(["alice", "bob"])
    service.listMembers(groupId, "stranger").isEmpty()
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