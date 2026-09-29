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

  def "Removing the last member persists the dissolved group and dispatches GroupDissolved"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)
    GroupCaptor<GroupDissolved> captor = subscribe(GroupDissolved)

    and:
    GroupId id = GroupId.create()
    String creator = "creator-user"
    service.createAdHocGroup(id, NAME, DESC, creator, NOW)

    when:
    Optional<UserGroup> result = service.removeMembership(id, creator)

    then: "the removal happened and dissolved the group"
    result.isPresent()
    result.get().status() == GroupStatus.DISSOLVED
    result.get().memberships().isEmpty()

    and: "the storage reflects the dissolved state"
    def stored = storage.findById(id)
    stored.isPresent()
    stored.get().status() == GroupStatus.DISSOLVED
    stored.get().memberships().isEmpty()

    and: "the GroupDissolved event was dispatched"
    captor.getEvent().isPresent()
    captor.getEvent().get().groupId() == id.get()
    captor.getEvent().get().groupName() == "NGS Lab"
    captor.getEvent().get().groupType() == GroupType.ADHOC
    captor.getEvent().get().triggeredByUserId() == creator
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
    group.addMember(creator, "member-2", NOW)
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
    service.removeMembership(myDissolvedGroup, "alice")

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
    service.removeMembership(dissolvedGroup, "alice")

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

  def "removing the last member dispatches both MemberRemovedFromGroup and GroupDissolved"() {
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
    result.get().status() == GroupStatus.ACTIVE
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

  def "only the owner can dissolve an ad-hoc group explicitly"() {
    given:
    GroupDataStorage storage = new InMemoryGroupDataStorage()
    GroupRepository repository = new GroupRepository(storage)
    GroupDomainService service = new GroupDomainService(repository)

    and:
    GroupId id = GroupId.create()
    service.createAdHocGroup(id, NAME, DESC, "alice", NOW)
    service.addMember(id, "alice", "bob", NOW)
    service.appointManager(id, "alice", "bob")

    when: "the manager attempts to dissolve"
    Optional<UserGroup> managerResult = service.dissolve(id, "bob")

    then: "the operation is rejected"
    managerResult.isEmpty()
    storage.findById(id).get().status() == GroupStatus.ACTIVE

    when: "the owner dissolves"
    Optional<UserGroup> ownerResult = service.dissolve(id, "alice")

    then:
    ownerResult.isPresent()
    ownerResult.get().status() == GroupStatus.DISSOLVED
    ownerResult.get().memberships().isEmpty()
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