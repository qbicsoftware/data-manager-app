package life.qbic.usergroups.application

import java.time.Instant

import life.qbic.application.commons.ApplicationException
import life.qbic.application.commons.ApplicationException.ErrorCode
import life.qbic.application.commons.Result
import life.qbic.usergroups.domain.model.GroupDescription
import life.qbic.usergroups.domain.model.GroupId
import life.qbic.usergroups.domain.model.GroupName
import life.qbic.usergroups.domain.model.GroupRole
import life.qbic.usergroups.domain.model.GroupStatus
import life.qbic.usergroups.domain.model.GroupType
import life.qbic.usergroups.domain.model.UserGroup
import life.qbic.usergroups.application.GroupInfoProjection
import life.qbic.usergroups.domain.registry.DomainRegistry
import life.qbic.usergroups.domain.repository.GroupDataStorage
import life.qbic.usergroups.domain.repository.GroupRepository
import life.qbic.usergroups.domain.service.GroupDomainService
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

/**
 * Tests for the {@link GroupService}.
 *
 * <p>Uses an in-memory fake {@link GroupDataStorage} and registers a {@link GroupDomainService}
 * in the singleton {@link DomainRegistry} so the application service can delegate.</p>
 */
class GroupServiceSpec extends Specification {

  private static Instant NOW = Instant.parse("2026-09-22T10:00:00Z")

  private static GroupName NAME = GroupName.from("NGS Lab")

  private static GroupDescription DESC = GroupDescription.from("A test lab")

  private static final String DUPLICATE_MESSAGE_PREFIX =
      "A group with the name "

  private GroupDomainService domainService

  private InMemoryGroupDataStorage storage

  private GroupRepository repository

  private GroupService service

  def setup() {
    storage = new InMemoryGroupDataStorage()
    repository = new GroupRepository(storage)
    domainService = new GroupDomainService(repository)
    DomainRegistry.instance().registerService(domainService)
    service = new GroupService(repository, new InMemoryUserInformationService())
  }

  def cleanup() {
    DomainRegistry.instance().registerService(null)
  }

  def "Creating an ad-hoc group sets the creator as OWNER and results in the created group"() {
    given: "a registered user"
    String creator = "creator-user"

    when:
    Result<GroupInfoProjection, ApplicationException> result =
        service.createAdHocGroup(creator, NAME, DESC)

    then: "the result contains the created group as a projection"
    result.isValue()
    def group = result.getValue()
    group.groupId() != null
    group.groupName() == NAME
    group.groupType() == GroupType.ADHOC

    and: "the creator's OWNER membership is observable via my-groups"
    def myGroups = service.listMyGroups(creator)
    myGroups.size() == 1
    myGroups.get(0).myRole() == GroupRole.OWNER
  }

  def "Creating a group with an already used name is rejected with the exact uniqueness message"() {
    given: "a group with name 'NGS Lab' already exists"
    service.createAdHocGroup("creator-user", NAME, DESC)

    when: "a second group with the exact same name is created"
    Result<GroupInfoProjection, ApplicationException> result =
        service.createAdHocGroup("other-user", NAME, DESC)

    then: "an error with the duplicate name code and exact message is returned"
    result.isError()
    result.getError().errorCode() == ErrorCode.DUPLICATE_GROUP_NAME
    result.getError().getMessage() ==
        "A group with the name 'NGS Lab' already exists. Group names must be unique (case-insensitive)."

    and: "nothing was persisted"
    storage.allGroups().size() == 1
  }

  def "Creating a group with a name that differs only in case is rejected as duplicate"() {
    given: "a group with name 'NGS Lab' already exists"
    service.createAdHocGroup("creator-user", NAME, DESC)

    when: "a group with 'ngs lab' (different case) is created"
    Result<GroupInfoProjection, ApplicationException> result =
        service.createAdHocGroup("other-user", GroupName.from("ngs lab"), DESC)

    then: "the creation is rejected with the duplicate name code"
    result.isError()
    result.getError().errorCode() == ErrorCode.DUPLICATE_GROUP_NAME
    result.getError().getMessage() ==
        "A group with the name 'ngs lab' already exists. Group names must be unique (case-insensitive)."

    and: "nothing was persisted"
    storage.allGroups().size() == 1
  }

  def "A race condition on the unique index is translated to the same friendly duplicate error"() {
    given: "the storage rejects the save with a DataIntegrityViolationException"
    InMemoryGroupDataStorage racingStorage = new InMemoryGroupDataStorage() {
      @Override
      void save(UserGroup group) {
        throw new DataIntegrityViolationException("duplicate key on name")
      }
    }
    GroupRepository racingRepository = new GroupRepository(racingStorage)
    DomainRegistry.instance().registerService(new GroupDomainService(racingRepository))
    GroupService racingService = new GroupService(racingRepository,
        new InMemoryUserInformationService())

    when:
    Result<GroupInfoProjection, ApplicationException> result =
        racingService.createAdHocGroup("creator-user", GroupName.from("Brand New Name"), DESC)

    then: "a friendly duplicate-name error is returned"
    result.isError()
    result.getError().errorCode() == ErrorCode.DUPLICATE_GROUP_NAME
    result.getError().getMessage() ==
        "A group with the name 'Brand New Name' already exists. Group names must be unique (case-insensitive)."
  }

  def "Creating a group with a blank creator user id is rejected"() {
    when:
    Result<GroupInfoProjection, ApplicationException> result =
        service.createAdHocGroup("  ", NAME, DESC)

    then:
    result.isError()
    result.getError().errorCode() == ErrorCode.GENERAL
  }

  def "listMyGroups returns only active groups the user is a member of, with the caller's role"() {
    given: "three groups: one active & owned by the caller, one dissolved, one belonging to someone else"
    service.createAdHocGroup("alice", GroupName.from("Alice's Group"), DESC)
    def dissolvedId = GroupId.create()
    domainService.createAdHocGroup(dissolvedId, GroupName.from("Gone Group"), DESC, "alice", NOW)
    domainService.removeMembership(dissolvedId, "alice")
    service.createAdHocGroup("bob", GroupName.from("Bob's Group"), DESC)

    when:
    def myGroups = service.listMyGroups("alice")

    then:
    myGroups*.groupName() == [GroupName.from("Alice's Group")]
    myGroups.get(0).myRole() == GroupRole.OWNER
  }

  def "A created ad-hoc group appears in the public directory AND in the creator's my-groups (AC a)"() {
    given: "a registered user"
    String creator = "researcher-1"

    when: "the user creates an ad-hoc group with a unique name and description"
    Result<GroupInfoProjection, ApplicationException> result =
        service.createAdHocGroup(creator, GroupName.from("Collab Team"),
            GroupDescription.from("Assembled collaboration team"))

    then: "the group is created (projection) and the creator is OWNER"
    result.isValue()
    result.getValue().groupType() == GroupType.ADHOC
    service.listMyGroups(creator).get(0).myRole() == GroupRole.OWNER

    and: "the group appears in the public directory"
    service.listPublicDirectory()*.groupName() == [GroupName.from("Collab Team")]

    and: "the group appears in the creator's my-groups"
    def myGroups = service.listMyGroups(creator)
    myGroups*.groupName() == [GroupName.from("Collab Team")]
    myGroups.get(0).myRole() == GroupRole.OWNER
  }

  def "An ad-hoc group that becomes empty is dissolved and disappears from directory and my-groups (AC c)"() {
    given: "an ad-hoc group owned by a single user"
    String solo = "solo-user"
    String groupId = service.createAdHocGroup(solo, NAME, DESC).getValue().groupId().get()
    service.listPublicDirectory().size() == 1
    service.listMyGroups(solo).size() == 1

    when: "the last member (myself) is removed"
    Result<Void, ApplicationException> result = service.removeMembership(groupId, solo)

    then: "the removal succeeds and the group is auto-dissolved"
    result.isValue()
    storage.findById(GroupId.from(groupId)).get().status() == GroupStatus.DISSOLVED

    and: "it is removed from the public directory"
    service.listPublicDirectory().isEmpty()

    and: "it is removed from my-groups"
    service.listMyGroups(solo).isEmpty()
  }

  def "listPublicDirectory exposes identity, name, description and type only, never members"() {
    given: "an active group and a dissolved group"
    service.createAdHocGroup("alice", GroupName.from("Public Group"), DESC)
    def dissolvedId = GroupId.create()
    domainService.createAdHocGroup(dissolvedId, GroupName.from("Secret Gone"), DESC, "bob", NOW)
    domainService.removeMembership(dissolvedId, "bob")

    when:
    def directory = service.listPublicDirectory()

    then: "only the active group appears"
    directory*.groupName() == [GroupName.from("Public Group")]

    and: "the projection carries no membership information"
    directory.get(0).groupId() != null
    directory.get(0).groupType() == GroupType.ADHOC
    directory.get(0).groupDescription() == DESC
    directory.get(0).toString().toLowerCase().contains("members") == false
  }

  def "removeMembership of the last member dissolves the group"() {
    given: "a group with a single member (the owner)"
    Result<GroupInfoProjection, ApplicationException> created =
        service.createAdHocGroup("solo-user", NAME, DESC)
    String groupId = created.getValue().groupId().get()

    when:
    Result<Void, ApplicationException> result = service.removeMembership(groupId, "solo-user")

    then: "the removal succeeds"
    result.isValue()

    and: "the group is dissolved and purged of memberships in storage"
    def stored = storage.findById(GroupId.from(groupId)).get()
    stored.status() == GroupStatus.DISSOLVED
    stored.memberships().isEmpty()
  }

  def "removeMembership of a group that does not exist fails with a not-found error"() {
    given: "a group id that does not exist"
    String unknownGroupId = GroupId.create().get()

    when:
    Result<Void, ApplicationException> result =
        service.removeMembership(unknownGroupId, "some-user")

    then:
    result.isError()
    result.getError().getMessage() == "Group " + unknownGroupId + " not found."
  }

  def "removeMembership by a non-member fails with an error"() {
    given: "a group owned by alice"
    Result<GroupInfoProjection, ApplicationException> created =
        service.createAdHocGroup("alice", NAME, DESC)
    String groupId = created.getValue().groupId().get()

    when: "a non-member attempts to remove themselves"
    Result<Void, ApplicationException> result = service.removeMembership(groupId, "not-a-member")

    then:
    result.isError()
    result.getError().getMessage().contains("not a member")
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

    List<UserGroup> allGroups() {
      return new ArrayList<>(groups.values())
    }
  }
}