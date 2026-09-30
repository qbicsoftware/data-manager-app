package life.qbic.usergroups.domain.service

import java.time.Instant

import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.usergroups.domain.event.GroupCreated
import life.qbic.usergroups.domain.event.GroupDissolved
import life.qbic.usergroups.domain.event.GroupMembershipRoleChanged
import life.qbic.usergroups.domain.event.GroupProfileUpdated
import life.qbic.usergroups.domain.event.MemberAddedToGroup
import life.qbic.usergroups.domain.event.MemberRemovedFromGroup
import life.qbic.usergroups.domain.model.GroupDescription
import life.qbic.usergroups.domain.model.GroupId
import life.qbic.usergroups.domain.model.GroupName
import life.qbic.usergroups.domain.model.GroupRole
import life.qbic.usergroups.domain.model.GroupStatus
import life.qbic.usergroups.domain.model.GroupType
import life.qbic.usergroups.domain.model.UserGroup
import life.qbic.usergroups.domain.repository.GroupDataStorage
import life.qbic.usergroups.domain.repository.GroupRepository
import spock.lang.Specification

/**
 * Tests for the {@link GroupDomainService}.
 *
 * <p>Uses an in-memory fake {@link GroupDataStorage} to observe persistence state and a
 * {@link DomainEventSubscriber} to capture dispatched events.</p>
 */
class GroupDomainServiceSpec extends Specification {

  private static Instant NOW = Instant.parse("2026-09-22T10:00:00Z")

  private static GroupName NAME = GroupName.from("NGS Lab")

  private static GroupDescription DESC = GroupDescription.from("A test lab")

  def "A created ad-hoc group is stored and a GroupCreated event is dispatched"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<GroupCreated> captor = subscribe(GroupCreated)

    and:
    GroupId id = GroupId.create()
    String creator = "creator-user"

    when:
    service.createAdHocGroup(id, NAME, DESC, creator, NOW)

    then:
    def stored = storage.findById(id)
    stored.isPresent()
    stored.get().type() == GroupType.ADHOC
    stored.get().status() == GroupStatus.ACTIVE
    stored.get().createdBy() == creator
    stored.get().memberships().size() == 1
    stored.get().memberships().get(0).userId() == creator
    stored.get().memberships().get(0).role() == GroupRole.OWNER

    and: "the GroupCreated event was dispatched with the group data"
    captor.getEvent().isPresent()
    captor.getEvent().get().groupId() == id.get()
    captor.getEvent().get().groupName() == "NGS Lab"
    captor.getEvent().get().groupType() == GroupType.ADHOC
    captor.getEvent().get().creatorUserId() == creator
  }

  def "A created org group is stored with an empty roster and a GroupCreated event is dispatched"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<GroupCreated> captor = subscribe(GroupCreated)

    and:
    GroupId id = GroupId.create()
    String admin = "admin-user"

    when:
    service.createOrgGroup(id, GroupName.from("NGS Lab Org"), DESC, admin, NOW)

    then:
    def stored = storage.findById(id)
    stored.isPresent()
    stored.get().type() == GroupType.ORG
    stored.get().status() == GroupStatus.ACTIVE
    stored.get().createdBy() == admin
    stored.get().memberships().isEmpty()

    and: "the GroupCreated event was dispatched with type ORG"
    captor.getEvent().isPresent()
    captor.getEvent().get().groupId() == id.get()
    captor.getEvent().get().groupName() == "NGS Lab Org"
    captor.getEvent().get().groupType() == GroupType.ORG
    captor.getEvent().get().creatorUserId() == admin
  }

  def "Removing a membership never dissolves the group and dispatches MemberRemovedFromGroup"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<GroupDissolved> dissolvedCaptor = subscribe(GroupDissolved)
    GroupCaptor<MemberRemovedFromGroup> removedCaptor = subscribe(MemberRemovedFromGroup)

    and:
    GroupId id = GroupId.create()
    String creator = "creator-user"
    service.createAdHocGroup(id, NAME, DESC, creator, NOW)
    service.addMember(id, creator, "member-2", NOW)

    when: "a member is removed (leaving only the owner)"
    Optional<UserGroup> result = service.removeMembership(id, "member-2")

    then: "the removal happened, the group stays ACTIVE and no GroupDissolved fires"
    result.isPresent()
    result.get().status() == GroupStatus.ACTIVE
    result.get().memberships().size() == 1
    result.get().memberships().get(0).userId() == creator
    removedCaptor.getEvent().isPresent()
    removedCaptor.getEvent().get().groupId() == id.get()
    removedCaptor.getEvent().get().userId() == "member-2"
    dissolvedCaptor.getEvent().isEmpty()
  }

  def "Removing a member of a non-empty group keeps the group ACTIVE and dispatches no GroupDissolved"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<GroupDissolved> captor = subscribe(GroupDissolved)

    and:
    GroupId id = GroupId.create()
    String creator = "creator-user"
    service.createAdHocGroup(id, NAME, DESC, creator, NOW)

    and: "add a second member directly on the aggregate before storing"
    def group = storage.findById(id).get()
    group.addMember("member-2", NOW)
    storage.save(group)

    when:
    Optional<UserGroup> result = service.removeMembership(id, "member-2")

    then:
    result.isPresent()
    result.get().status() == GroupStatus.ACTIVE
    result.get().memberships().size() == 1

    and:
    captor.getEvent().isEmpty()
  }

  def "Removing a non-member does nothing and stores no change"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<GroupDissolved> captor = subscribe(GroupDissolved)

    and:
    GroupId id = GroupId.create()
    service.createAdHocGroup(id, NAME, DESC, "creator-user", NOW)

    when:
    Optional<UserGroup> result = service.removeMembership(id, "i-am-not-a-member")

    then:
    result.isEmpty()
    storage.findById(id).get().status() == GroupStatus.ACTIVE
    storage.findById(id).get().memberships().size() == 1
    captor.getEvent().isEmpty()
  }

  def "Removing a membership from a nonexistent group is a no-op"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)

    when:
    Optional<UserGroup> result = service.removeMembership(GroupId.create(), "some-user")

    then:
    result.isEmpty()
  }

  def "my-groups returns only active groups the user is a member of"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)

    and: "two groups exist; the user is a member of both, one is dissolved"
    GroupId myActiveGroup = GroupId.create()
    service.createAdHocGroup(myActiveGroup, GroupName.from("My Active"), DESC, "alice", NOW)

    GroupId myDissolvedGroup = GroupId.create()
    service.createAdHocGroup(myDissolvedGroup, GroupName.from("My Gone"), DESC, "alice", NOW)
    service.dissolve(myDissolvedGroup, "alice")

    GroupId otherGroup = GroupId.create()
    service.createAdHocGroup(otherGroup, GroupName.from("Not Mine"), DESC, "bob", NOW)

    when:
    def myGroups = service.listMyGroups("alice")

    then:
    myGroups*.id() == [myActiveGroup]
  }

  def "the public directory returns all active groups and excludes dissolved groups"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)

    and:
    GroupId activeGroup = GroupId.create()
    service.createAdHocGroup(activeGroup, GroupName.from("Active Group"), DESC, "alice", NOW)

    GroupId dissolvedGroup = GroupId.create()
    service.createAdHocGroup(dissolvedGroup, GroupName.from("Dissolving Group"), DESC, "alice", NOW)
    service.dissolve(dissolvedGroup, "alice")

    when:
    def directory = service.listPublicDirectory()

    then:
    directory*.id() == [activeGroup]
  }

  def "adding a member dispatches a MemberAddedToGroup event"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<MemberAddedToGroup> captor = subscribe(MemberAddedToGroup)

    and:
    GroupId id = GroupId.create()
    service.createAdHocGroup(id, NAME, DESC, "alice", NOW)

    when:
    Optional<UserGroup> result = service.addMember(id, "alice", "bob", NOW)

    then:
    result.isPresent()
    captor.getEvent().isPresent()
    captor.getEvent().get().groupId() == id.get()
    captor.getEvent().get().userId() == "bob"
    captor.getEvent().get().triggeredByUserId() == "alice"
  }

  def "removing a member dispatches MemberRemovedFromGroup and no GroupDissolved when the group stays non-empty"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<MemberRemovedFromGroup> removedCaptor = subscribe(MemberRemovedFromGroup)
    GroupCaptor<GroupDissolved> dissolvedCaptor = subscribe(GroupDissolved)

    and:
    GroupId id = GroupId.create()
    service.createAdHocGroup(id, NAME, DESC, "alice", NOW)
    service.addMember(id, "alice", "bob", NOW)

    when:
    Optional<UserGroup> result = service.removeMember(id, "alice", "bob")

    then:
    result.isPresent()
    result.get().status() == GroupStatus.ACTIVE
    removedCaptor.getEvent().isPresent()
    removedCaptor.getEvent().get().userId() == "bob"
    dissolvedCaptor.getEvent().isEmpty()
  }

  def "removing a member never dispatches GroupDissolved, even when only the owner remains"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<MemberRemovedFromGroup> removedCaptor = subscribe(MemberRemovedFromGroup)
    GroupCaptor<GroupDissolved> dissolvedCaptor = subscribe(GroupDissolved)

    and:
    GroupId id = GroupId.create()
    service.createAdHocGroup(id, NAME, DESC, "alice", NOW)
    service.addMember(id, "alice", "bob", NOW)

    when: "the owner removes the last remaining member (bob), leaving only the owner"
    Optional<UserGroup> result = service.removeMember(id, "alice", "bob")

    then:
    result.isPresent()
    removedCaptor.getEvent().isPresent()
    removedCaptor.getEvent().get().userId() == "bob"
    result.get().status() == GroupStatus.ACTIVE
    result.get().memberships().size() == 1
    result.get().memberships().get(0).userId() == "alice"
    dissolvedCaptor.getEvent().isEmpty()
  }

  def "appointing and demoting a manager dispatch GroupMembershipRoleChanged events"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<GroupMembershipRoleChanged> captor = subscribe(GroupMembershipRoleChanged)

    and:
    GroupId id = GroupId.create()
    service.createAdHocGroup(id, NAME, DESC, "alice", NOW)
    service.addMember(id, "alice", "bob", NOW)

    when: "alice appoints bob as manager"
    service.appointManager(id, "alice", "bob")

    then: "the role-changed event carries MEMBER -> MANAGER"
    captor.getEvent().isPresent()
    captor.getEvent().get().userId() == "bob"
    captor.getEvent().get().previousRole() == GroupRole.MEMBER
    captor.getEvent().get().newRole() == GroupRole.MANAGER

    when: "alice demotes bob back"
    service.demoteManager(id, "alice", "bob")

    then: "the role-changed event carries MANAGER -> MEMBER"
    captor.getEvent().get().previousRole() == GroupRole.MANAGER
    captor.getEvent().get().newRole() == GroupRole.MEMBER
  }

  def "renaming and changing the description dispatch GroupProfileUpdated events"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<GroupProfileUpdated> captor = subscribe(GroupProfileUpdated)

    and:
    GroupId id = GroupId.create()
    service.createAdHocGroup(id, NAME, DESC, "alice", NOW)

    when: "alice renames the group"
    service.renameGroup(id, "alice", GroupName.from("Renamed Lab"))

    then:
    captor.getEvent().isPresent()
    captor.getEvent().get().groupId() == id.get()
    captor.getEvent().get().oldName() == NAME.value()
    captor.getEvent().get().newName() == "Renamed Lab"

    when: "alice changes the description"
    service.updateDescription(id, "alice", GroupDescription.from("new description"))

    then: "a profile-updated event fires"
    captor.getEvent().isPresent()
  }

  def "dissolve is an explicit, authorized path and dispatches GroupDissolved"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<GroupDissolved> dissolvedCaptor = subscribe(GroupDissolved)

    and:
    GroupId id = GroupId.create()
    service.createAdHocGroup(id, NAME, DESC, "alice", NOW)
    service.addMember(id, "alice", "bob", NOW)
    service.appointManager(id, "alice", "bob")

    when: "the group is dissolved explicitly"
    Optional<UserGroup> result = service.dissolve(id, "alice")

    then: "the dissolve succeeds (authorization is the app layer's concern)"
    result.isPresent()
    result.get().status() == GroupStatus.DISSOLVED
    result.get().memberships().isEmpty()
    storage.findById(id).get().status() == GroupStatus.DISSOLVED
    dissolvedCaptor.getEvent().isPresent()
    dissolvedCaptor.getEvent().get().groupId() == id.get()
  }

  // ── appointOrgManager / removeOrgManager (FEAT-USER-GROUPS-02) ──────────────

  def "appointOrgManager direct appointment dispatches MemberAddedToGroup and no role-changed"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<MemberAddedToGroup> addedCaptor = subscribe(MemberAddedToGroup)
    GroupCaptor<GroupMembershipRoleChanged> roleCaptor = subscribe(GroupMembershipRoleChanged)

    and: "an org group"
    GroupId id = GroupId.create()
    service.createOrgGroup(id, GroupName.from("NGS Lab Org"), DESC, "admin-user", NOW)

    when: "the admin directly appoints a non-member as manager"
    Optional<UserGroup> result =
        service.appointOrgManager(id, "admin-user", "lab-lead", NOW)

    then: "the roster gains a MANAGER and a MemberAddedToGroup event fires"
    result.isPresent()
    result.get().memberships().size() == 1
    result.get().memberships().get(0).role() == GroupRole.MANAGER
    addedCaptor.getEvent().isPresent()
    addedCaptor.getEvent().get().groupId() == id.get()
    addedCaptor.getEvent().get().userId() == "lab-lead"
    addedCaptor.getEvent().get().triggeredByUserId() == "admin-user"
    roleCaptor.getEvent().isEmpty()
  }

  def "appointOrgManager promoting an existing MEMBER dispatches GroupMembershipRoleChanged and no added event"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<MemberAddedToGroup> addedCaptor = subscribe(MemberAddedToGroup)
    GroupCaptor<GroupMembershipRoleChanged> roleCaptor = subscribe(GroupMembershipRoleChanged)

    and: "an org group whose manager added a regular member"
    GroupId id = GroupId.create()
    service.createOrgGroup(id, GroupName.from("NGS Lab Org"), DESC, "admin-user", NOW)
    service.appointOrgManager(id, "admin-user", "manager-1", NOW)
    service.addMember(id, "manager-1", "alice", NOW)
    // clear the captor so the promotion event is the one under observation
    addedCaptor.clearEvent()
    roleCaptor.clearEvent()

    when: "the admin promotes the member to manager"
    Optional<UserGroup> result =
        service.appointOrgManager(id, "admin-user", "alice", NOW)

    then: "a role-changed event MEMBER->MANAGER fires and no added-member event"
    result.isPresent()
    result.get().memberships().find { it.userId() == "alice" }.role() == GroupRole.MANAGER
    roleCaptor.getEvent().isPresent()
    roleCaptor.getEvent().get().userId() == "alice"
    roleCaptor.getEvent().get().previousRole() == GroupRole.MEMBER
    roleCaptor.getEvent().get().newRole() == GroupRole.MANAGER
    addedCaptor.getEvent().isEmpty()
  }

  def "removeOrgManager dispatches MemberRemovedFromGroup and never GroupDissolved for an org group"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<MemberRemovedFromGroup> removedCaptor = subscribe(MemberRemovedFromGroup)
    GroupCaptor<GroupDissolved> dissolvedCaptor = subscribe(GroupDissolved)

    and: "an org group with a single manager"
    GroupId id = GroupId.create()
    service.createOrgGroup(id, GroupName.from("NGS Lab Org"), DESC, "admin-user", NOW)
    service.appointOrgManager(id, "admin-user", "solo", NOW)

    when: "the admin removes the last manager"
    Optional<UserGroup> result = service.removeOrgManager(id, "admin-user", "solo")

    then: "the membership is gone, the group stays ACTIVE, a MemberRemovedFromGroup fires and no dissolve"
    result.isPresent()
    result.get().memberships().isEmpty()
    result.get().status() == GroupStatus.ACTIVE
    removedCaptor.getEvent().isPresent()
    removedCaptor.getEvent().get().userId() == "solo"
    removedCaptor.getEvent().get().triggeredByUserId() == "admin-user"
    dissolvedCaptor.getEvent().isEmpty()
  }

  def "appointOrgManager and removeOrgManager are type-agnostic (ad-hoc groups work too)"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupId adHocId = GroupId.create()
    service.createAdHocGroup(adHocId, GroupName.from("Sprint Team"), DESC, "creator-user", NOW)

    expect: "the org entry points delegate to the shared primitives (no type branch here)"
    service.appointOrgManager(adHocId, "creator-user", "alice", NOW).isPresent()
    service.removeOrgManager(adHocId, "creator-user", "alice").isPresent()
  }

  def "appointOrgManager and removeOrgManager reject a dissolved group"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupId dissolvedOrgId = GroupId.create()
    service.createOrgGroup(dissolvedOrgId, GroupName.from("Gone Org"), DESC, "admin-user", NOW)
    storage.findById(dissolvedOrgId).get().dissolve()
    storage.save(storage.findById(dissolvedOrgId).get())

    expect:
    service.appointOrgManager(dissolvedOrgId, "admin-user", "alice", NOW).isEmpty()
    service.removeOrgManager(dissolvedOrgId, "admin-user", "alice").isEmpty()
  }

  def "removeOrgManager returns empty for a non-member"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupId orgId = GroupId.create()
    service.createOrgGroup(orgId, GroupName.from("NGS Lab"), DESC, "admin-user", NOW)

    expect:
    service.removeOrgManager(orgId, "admin-user", "ghost").isEmpty()
  }

  // ── addOrgMember / rename / description / dissolve (FEAT-USER-GROUPS-02) ──────

  def "addOrgMember adds a regular MEMBER and dispatches MemberAddedToGroup"() {
    given: "an org group and a subscriber for MemberAddedToGroup"
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupId orgId = GroupId.create()
    service.createOrgGroup(orgId, GroupName.from("NGS Lab"), DESC, "admin-user", NOW)
    def captor = subscribe(MemberAddedToGroup)

    when: "an admin adds a regular member"
    Optional<UserGroup> result = service.addOrgMember(orgId, "admin-user", "alice", NOW)

    then: "the member is added with the MEMBER role and the event fires"
    result.isPresent()
    result.get().memberships().size() == 1
    result.get().memberships().get(0).role() == GroupRole.MEMBER
    captor.getEvent().isPresent()
    captor.getEvent().get().userId() == "alice"
  }

  def "addOrgMember is a no-op for an existing member (no event, no duplicate)"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupId orgId = GroupId.create()
    service.createOrgGroup(orgId, GroupName.from("NGS Lab"), DESC, "admin-user", NOW)
    service.addOrgMember(orgId, "admin-user", "alice", NOW)
    def captor = subscribe(MemberAddedToGroup)

    when:
    Optional<UserGroup> result = service.addOrgMember(orgId, "admin-user", "alice", NOW)

    then:
    result.isPresent()
    result.get().memberships().size() == 1
    // no new event fires for the existing member
    captor.getEvent().isEmpty()
  }

  def "renameOrgGroup renames an org group and dispatches GroupProfileUpdated"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupId orgId = GroupId.create()
    service.createOrgGroup(orgId, GroupName.from("NGS Lab"), DESC, "admin-user", NOW)
    def captor = subscribe(GroupProfileUpdated)

    when: "an admin renames the org group"
    Optional<UserGroup> result = service.renameOrgGroup(orgId, "admin-user",
        GroupName.from("QBiC NGS Core"))

    then: "the name is updated and the profile-updated event fires with the old name"
    result.isPresent()
    result.get().name().value() == "QBiC NGS Core"
    captor.getEvent().isPresent()
    captor.getEvent().get().oldName() == "NGS Lab"
  }

  def "updateOrgGroupDescription updates an org group and dispatches GroupProfileUpdated"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupId orgId = GroupId.create()
    service.createOrgGroup(orgId, GroupName.from("NGS Lab"), DESC, "admin-user", NOW)
    def captor = subscribe(GroupProfileUpdated)

    when: "an admin updates the description"
    Optional<UserGroup> result = service.updateOrgGroupDescription(orgId, "admin-user",
        GroupDescription.from("Updated description"))

    then:
    result.isPresent()
    result.get().description().value().get() == "Updated description"
    captor.getEvent().isPresent()
  }

  def "dissolveOrgGroup dissolves an org group, purges the roster and dispatches GroupDissolved"() {
    given: "an org group with members and a GroupDissolved subscriber"
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupId orgId = GroupId.create()
    service.createOrgGroup(orgId, GroupName.from("NGS Lab"), DESC, "admin-user", NOW)
    service.appointOrgManager(orgId, "admin-user", "alice", NOW)
    service.addOrgMember(orgId, "admin-user", "bob", NOW)
    def captor = subscribe(GroupDissolved)

    when: "an admin dissolves the org group"
    Optional<UserGroup> result = service.dissolveOrgGroup(orgId, "admin-user")

    then: "the org group is dissolved and its roster is purged; the event fires"
    result.isPresent()
    result.get().status() == GroupStatus.DISSOLVED
    result.get().memberships().isEmpty()
    captor.getEvent().isPresent()
    captor.getEvent().get().groupType() == GroupType.ORG
  }

  def "the org entry points delegate to the shared primitives for any group type"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupId adHocId = GroupId.create()
    service.createAdHocGroup(adHocId, GroupName.from("Sprint Team"), DESC, "creator-user", NOW)

    expect: "add/rename/describe/dissolve work on an ad-hoc group through the shared primitives"
    service.addOrgMember(adHocId, "creator-user", "alice", NOW).isPresent()
    service.renameOrgGroup(adHocId, "creator-user", GroupName.from("X")).isPresent()
    service.updateOrgGroupDescription(adHocId, "creator-user", GroupDescription.from("X")).isPresent()
    service.dissolveOrgGroup(adHocId, "creator-user").isPresent()
  }

  /**
   * A simple subscriber that records the last event of the given type.
   */
  private static <T extends DomainEvent> GroupCaptor<T> subscribe(Class<T> type) {
    def captor = new GroupCaptor<T>(type)
    DomainEventDispatcher.instance().subscribe(captor)
    return captor
  }

  static class GroupCaptor<T extends DomainEvent> implements DomainEventSubscriber<T> {

    private final Class<T> type

    private T event

    GroupCaptor(Class<T> type) {
      this.type = type
    }

    @Override
    Class<? extends DomainEvent> subscribedToEventType() {
      return type
    }

    @Override
    void handleEvent(T domainEvent) {
      this.event = domainEvent
    }

    void clearEvent() {
      this.event = null
    }

    Optional<T> getEvent() {
      return Optional.ofNullable(event)
    }
  }

  /**
   * In-memory fake of the {@link GroupDataStorage} port.
   */
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