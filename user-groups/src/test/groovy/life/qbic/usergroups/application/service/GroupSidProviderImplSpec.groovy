package life.qbic.usergroups.application.service
import life.qbic.usergroups.application.InMemoryUserInformationService

import java.time.Instant

import life.qbic.usergroups.api.GroupSidProvider
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
 * Tests for the {@link GroupSidProviderImpl}.
 *
 * <p>Verifies the P2 ACL seam: only <em>active</em> memberships produce sids, sids follow the
 * {@code GROUP_<groupId>} prefix convention keyed on the stable group id.</p>
 */
class GroupSidProviderImplSpec extends Specification {

  private static Instant NOW = Instant.parse("2026-09-22T10:00:00Z")

  private static GroupDescription DESC = GroupDescription.from("desc")

  private GroupDomainService domainService

  private InMemoryGroupDataStorage storage

  private GroupRepository repository

  private GroupService groupService

  private GroupSidProvider provider

  def setup() {
    storage = new InMemoryGroupDataStorage()
    repository = new GroupRepository(storage)
    domainService = new GroupDomainService(repository)
    DomainRegistry.instance().registerService(domainService)
    groupService = new GroupService(repository, new InMemoryUserInformationService())
    provider = new GroupSidProviderImpl(groupService)
  }

  def cleanup() {
    DomainRegistry.instance().registerService(null)
  }

  def "Two active and one dissolved membership produce exactly two group sids with correct prefix"() {
    given: "a user with three groups: two active, one dissolved"
    String userId = "alice"
    GroupId activeOne = GroupId.create()
    GroupId activeTwo = GroupId.create()
    GroupId gone = GroupId.create()
    domainService.createAdHocGroup(activeOne, GroupName.from("Active One"), DESC, userId, NOW)
    domainService.createAdHocGroup(activeTwo, GroupName.from("Active Two"), DESC, userId, NOW)
    domainService.createAdHocGroup(gone, GroupName.from("Gone"), DESC, userId, NOW)
    domainService.removeMembership(gone, userId)

    when: "the group sids of the user are resolved"
    List<String> sids = provider.listGroupSidsForUser(userId)

    then: "only the two active memberships produce sids, with the GROUP_ prefix"
    sids.size() == 2
    sids.contains(GroupSidProvider.GROUP_SID_PREFIX + activeOne.get())
    sids.contains(GroupSidProvider.GROUP_SID_PREFIX + activeTwo.get())
    sids.every { it.startsWith(GroupSidProvider.GROUP_SID_PREFIX) }
    sids.contains(GroupSidProvider.GROUP_SID_PREFIX + gone.get()) == false
  }

  def "A user without memberships resolves to an empty sid list"() {
    when:
    List<String> sids = provider.listGroupSidsForUser("nobody")

    then:
    sids.isEmpty()
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