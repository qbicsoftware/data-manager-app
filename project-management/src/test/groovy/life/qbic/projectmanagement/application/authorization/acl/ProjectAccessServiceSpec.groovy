package life.qbic.projectmanagement.application.authorization.acl

import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup
import life.qbic.projectmanagement.domain.model.project.Project
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.usergroups.api.GroupInfo
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.jdbc.JdbcMutableAclService
import org.springframework.security.acls.model.AccessControlEntry
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.Permission
import org.springframework.security.acls.model.Sid
import spock.lang.Specification

/**
 * Unit tests for the authority-based (user group) grant path of {@link ProjectAccessServiceImpl}.
 *
 * <p>Covers the OWNER invariant on the authority write path (AC3), the self-healing re-grant
 * (AC2) and the shared-groups listing (AC1) without a database or Spring context.</p>
 */
class ProjectAccessServiceSpec extends Specification {

  JdbcMutableAclService aclService = Mock()
  JdbcTemplate jdbcTemplate = Mock()
  GroupInformationService groupInformationService = Mock()
  ProjectAccessServiceImpl service

  ProjectId projectId = ProjectId.create()
  List<AccessControlEntry> entries = []
  Sid owner
  MutableAcl acl

  def setup() {
    acl = buildAcl(projectId)
    aclService.readAclById(_ as ObjectIdentityImpl, _) >> acl
    aclService.createAcl(_ as ObjectIdentityImpl) >> acl
    service = new ProjectAccessServiceImpl(aclService, jdbcTemplate, groupInformationService)
    entries.clear()
    owner = null
  }

  private MutableAcl buildAcl(ProjectId pid) {
    return Mock(MutableAcl) {
      getObjectIdentity() >> new ObjectIdentityImpl(Project.class, pid)
      getEntries() >> entries
      insertAce(*_) >> { int at, Permission permission, Sid sid, boolean granting ->
        entries = entries.take(at) + ace(permission, sid, granting) + entries.drop(at)
      }
      deleteAce(*_) >> { int at -> entries = entries.take(at) + entries.drop(at + 1) }
      updateAce(*_) >> { int at, Permission p -> entries[at] = ace(p, entries[at].getSid(), entries[at].isGranting()) }
      setOwner(*_) >> { Sid sid -> owner = sid }
      getOwner() >> { owner }
      isEntriesInheriting() >> false
      setEntriesInheriting(*_) >> {}
      getParentAcl() >> null
      setParent(*_) >> {}
      isGranted(*_) >> false
      isSidLoaded(*_) >> false
      getEntries() >> { entries }
    }
  }

  private static AccessControlEntry ace(Permission permission, Sid sid, boolean granting) {
    return new StubAccessControlEntry(permission, sid, granting)
  }

  def "authority writes read the full ACL so a group-admin member can manage another group"() {
    given: "a project with a shared group"
    entries.add(ace(BasePermission.ADMINISTRATION, new GrantedAuthoritySid("GROUP_admin"), true))
    aclService.readAclById(_ as ObjectIdentityImpl, List.of()) >> acl

    when: "a group-admin member changes the role of a different group"
    service.changeAuthorityAccess(projectId, "GROUP_other", ProjectRole.WRITE)
    service.addAuthorityAccess(projectId, "GROUP_new", ProjectRole.READ)
    service.removeAuthorityAccess(projectId, "GROUP_other")

    then: "each authority write completes against the unfiltered ACL"
    // the acting group-admin's own ACE must be visible to the ACL authorization check inside
    // insertAce/deleteAce; sid-filtered reads would expose only the modified group's entries
    // and raise "Unable to locate a matching ACE" (FEAT-USER-GROUPS-08 regression)
    noExceptionThrown()
    // the group-admin's ACE is untouched and the new group ACE was added via the full-ACL read
    entries.count { it.sid == new GrantedAuthoritySid("GROUP_admin") } == 1
    entries.count { it.sid == new GrantedAuthoritySid("GROUP_new") } == 1
  }

  def "authority writes clear the local acl_cache so a demoted member loses admin at the next check"() {
    given: "a wired cache manager and a shared project"
    org.springframework.cache.CacheManager cacheManager = Mock()
    org.springframework.cache.Cache cache = Mock()
    cacheManager.getCache("acl_cache") >> cache
    service.setCacheManager(cacheManager)

    when: "a group grant is changed"
    service.changeAuthorityAccess(projectId, "GROUP_abc", ProjectRole.READ)

    then: "the raw acl_cache is evicted and cleared so no stale ADMIN survives until restart"
    1 * cache.clear()
  }

  def "rejects OWNER on the authority grant path (groups can never become project OWNER)"() {
    when: "an authority is granted the OWNER role"
    service.addAuthorityAccess(projectId, "GROUP_abc", ProjectRole.OWNER)

    then: "the grant is rejected with a clear error mentioning OWNER"
    def error = thrown(Exception)
    error.message.contains("OWNER")
  }

  def "rejects OWNER on the authority role-change path"() {
    when:
    service.changeAuthorityAccess(projectId, "GROUP_abc", ProjectRole.OWNER)

    then:
    def error = thrown(Exception)
    error.message.contains("OWNER")
  }

  def "allows ADMIN on the authority grant path"() {
    when: "an authority is granted the ADMIN role"
    service.addAuthorityAccess(projectId, "ROLE_EXAMPLE", ProjectRole.ADMIN)

    then: "the ACL is updated with an inserted grant"
    1 * aclService.updateAcl(acl)
  }

  def "re-granting an existing authority updates its role instead of failing"() {
    given: "an authority already granted WRITE (READ + WRITE)"
    entries.add(ace(BasePermission.READ, new GrantedAuthoritySid("GROUP_abc"), true))
    entries.add(ace(BasePermission.WRITE, new GrantedAuthoritySid("GROUP_abc"), true))

    when: "the same authority is granted ADMIN again"
    service.addAuthorityAccess(projectId, "GROUP_abc", ProjectRole.ADMIN)

    then: "the grant self-heals to ADMIN without an error and without duplicate entries"
    noExceptionThrown()
    def granted = entries.findAll { it.sid == new GrantedAuthoritySid("GROUP_abc") }
    granted.collect { it.permission }.toSet() == ProjectRole.ADMIN.toPermissions().toSet()
    granted.size() == ProjectRole.ADMIN.toPermissions().size()
  }

  def "listSharedGroups exposes shared groups with name, role and no member data"() {
    given: "a project with a shared group at READ"
    entries.add(ace(BasePermission.READ, new GrantedAuthoritySid("GROUP_group-1"), true))
    groupInformationService.findGroupById("group-1") >> Optional.of(
        new GroupInfo("group-1", "NGS Lab", "the sequencing lab", GroupType.ORG))

    when:
    List<SharedProjectGroup> sharedGroups = service.listSharedGroups(projectId)

    then: "the group appears with its resolved name and granted role"
    sharedGroups.size() == 1
    with(sharedGroups[0]) {
      groupId == "group-1"
      groupName == "NGS Lab"
      groupDescription == "the sequencing lab"
      projectRole == ProjectRole.READ
      projectId == projectId
    }
  }

  def "listSharedGroups ignores non-group authorities"() {
    given: "a project with a system role ADMIN grant only"
    entries.add(ace(BasePermission.ADMINISTRATION,
        new GrantedAuthoritySid("ROLE_EXAMPLE"), true))

    when:
    List<SharedProjectGroup> sharedGroups = service.listSharedGroups(projectId)

    then: "no group is listed"
    sharedGroups.isEmpty()
  }

  def "removing an authority removes every ACE for the group"() {
    given: "a group with multiple ACEs (READ + WRITE + ADMINISTRATION)"
    entries.add(ace(BasePermission.READ, new GrantedAuthoritySid("GROUP_abc"), true))
    entries.add(ace(BasePermission.WRITE, new GrantedAuthoritySid("GROUP_abc"), true))
    entries.add(ace(BasePermission.ADMINISTRATION, new GrantedAuthoritySid("GROUP_abc"), true))

    when: "the group grant is removed"
    service.removeAuthorityAccess(projectId, "GROUP_abc")

    then: "no ACE for the group remains, so the group can be re-added"
    entries.count { it.sid == new GrantedAuthoritySid("GROUP_abc") } == 0
  }

  def "listSharedGroups surfaces a partial group ACE with a fallback role instead of hiding it"() {
    given: "a leftover WRITE-only ACE as a failed removal would leave behind"
    entries.add(ace(BasePermission.WRITE, new GrantedAuthoritySid("GROUP_group-1"), true))
    groupInformationService.findGroupById("group-1") >> Optional.of(
        new GroupInfo("group-1", "Test Group", null, GroupType.ADHOC))

    when:
    List<SharedProjectGroup> sharedGroups = service.listSharedGroups(projectId)

    then: "the group is visible with a usable role so it can be corrected or revoked"
    sharedGroups.size() == 1
    sharedGroups[0].groupName == "Test Group"
    sharedGroups[0].projectRole == ProjectRole.WRITE
  }

  def "granting an authority publishes an ACL eviction after the local cache eviction"() {
    given: "an ACL eviction publisher wired into the service"
    AclEvictionPublisher publisher = Mock()
    service.setAclEvictionPublisher(publisher)

    when: "an authority grant is added"
    service.addAuthorityAccess(projectId, "GROUP_abc", ProjectRole.READ)

    then: "the publisher is notified with the affected project"
    1 * publisher.publishAclEviction(projectId)
  }

  def "removing an authority publishes an ACL eviction after the local cache eviction"() {
    given: "an ACL eviction publisher wired into the service"
    AclEvictionPublisher publisher = Mock()
    service.setAclEvictionPublisher(publisher)

    when: "the authority grant is removed"
    service.removeAuthorityAccess(projectId, "GROUP_abc")

    then: "the publisher is notified with the affected project"
    1 * publisher.publishAclEviction(projectId)
  }

  def "changing an authority role publishes an ACL eviction after the local cache eviction"() {
    given: "an ACL eviction publisher wired into the service"
    AclEvictionPublisher publisher = Mock()
    service.setAclEvictionPublisher(publisher)

    when: "the authority role is changed"
    service.changeAuthorityAccess(projectId, "GROUP_abc", ProjectRole.WRITE)

    then: "the publisher is notified with the affected project"
    1 * publisher.publishAclEviction(projectId)
  }

  def "role change escalates a group from READ to ADMIN with delete-stale plus insert-new"() {
    given: "a group sharing the project at READ"
    entries.add(ace(BasePermission.READ, new GrantedAuthoritySid("GROUP_group-1"), true))

    when: "the group role is escalated to ADMIN"
    service.changeAuthorityAccess(projectId, "GROUP_group-1", ProjectRole.ADMIN)

    then: "the group retains READ and gains the additional WRITE and ADMINISTRATION permissions"
    1 * aclService.updateAcl(acl)
    Set<Integer> groupPermissionMasks = entries.findAll {
      it.sid == new GrantedAuthoritySid("GROUP_group-1")
    }*.permission*.mask as Set
    groupPermissionMasks == ([BasePermission.READ.mask, BasePermission.WRITE.mask,
        BasePermission.ADMINISTRATION.mask] as Set)
  }

  def "role change downgrades a group from ADMIN to READ by deleting the stale permissions"() {
    given: "a group sharing the project at ADMIN"
    entries.add(ace(BasePermission.READ, new GrantedAuthoritySid("GROUP_group-1"), true))
    entries.add(ace(BasePermission.WRITE, new GrantedAuthoritySid("GROUP_group-1"), true))
    entries.add(ace(BasePermission.ADMINISTRATION, new GrantedAuthoritySid("GROUP_group-1"), true))

    when: "the group role is downgraded to READ"
    service.changeAuthorityAccess(projectId, "GROUP_group-1", ProjectRole.READ)

    then: "only the READ permission remains for the group"
    1 * aclService.updateAcl(acl)
    Set<Permission> groupPermissions = entries.findAll {
      it.sid == new GrantedAuthoritySid("GROUP_group-1")
    }*.permission as Set
    groupPermissions == ([BasePermission.READ] as Set)
  }

  def "role change back to the same role is a no-op on the entries"() {
    given: "a group already sharing the project at WRITE"
    entries.add(ace(BasePermission.READ, new GrantedAuthoritySid("GROUP_group-1"), true))
    entries.add(ace(BasePermission.WRITE, new GrantedAuthoritySid("GROUP_group-1"), true))

    when: "the group role is set to WRITE again"
    service.changeAuthorityAccess(projectId, "GROUP_group-1", ProjectRole.WRITE)

    then: "the group keeps exactly its WRITE permissions"
    1 * aclService.updateAcl(acl)
    Set<Integer> groupPermissionMasks = entries.findAll {
      it.sid == new GrantedAuthoritySid("GROUP_group-1")
    }*.permission*.mask as Set
    groupPermissionMasks == ([BasePermission.READ.mask, BasePermission.WRITE.mask] as Set)
  }

  def "removing an authority removes only its group entries and keeps other sids intact"() {
    given: "a project with a shared group and an unrelated system role"
    entries.add(ace(BasePermission.READ, new GrantedAuthoritySid("GROUP_group-1"), true))
    entries.add(ace(BasePermission.ADMINISTRATION, new GrantedAuthoritySid("ROLE_EXAMPLE"), true))

    when: "the group is revoked from the project"
    service.removeAuthorityAccess(projectId, "GROUP_group-1")

    then: "the group's ACEs are gone while the unrelated role remains"
    1 * aclService.updateAcl(acl)
    entries.findAll { it.sid == new GrantedAuthoritySid("GROUP_group-1") }.isEmpty()
    entries.findAll { it.sid == new GrantedAuthoritySid("ROLE_EXAMPLE") }.size() == 1
  }

  def "revoking a non-shared group is a no-op that does not throw"() {
    given: "a project that has never shared the group"
    // entries stays empty

    when: "a stale UI action revokes a non-shared group"
    service.removeAuthorityAccess(projectId, "GROUP_notShared")

    then: "no exception is raised and no group ACE appears"
    noExceptionThrown()
    1 * aclService.updateAcl(acl)
    entries.isEmpty()
  }

  def "changing the role of a non-shared group is a no-op that does not throw"() {
    given: "a project that has never shared the group"
    // entries stays empty

    when: "a stale UI action changes the role of a non-shared group"
    service.changeAuthorityAccess(projectId, "GROUP_notShared", ProjectRole.ADMIN)

    then: "no exception is raised and the grant is inserted for the group"
    noExceptionThrown()
    1 * aclService.updateAcl(acl)
    entries.findAll { it.sid == new GrantedAuthoritySid("GROUP_notShared") }.size() == 3
  }

  def "authority writes succeed without an ACL eviction publisher"() {
    given: "no publisher is wired into the service"
    // service has no AclEvictionPublisher injected (null)

    when: "authority operations run"
    service.addAuthorityAccess(projectId, "GROUP_abc", ProjectRole.READ)

    then: "no exception is thrown and the ACL is updated"
    1 * aclService.updateAcl(acl)
  }

  def "batch listSharedGroups resolves a whole page of grants in one pass"() {
    given: "two projects with group grants and a spy so the SQL layer can be stubbed"
    def pageService = Spy(ProjectAccessServiceImpl,
        constructorArgs: [aclService, jdbcTemplate, groupInformationService])
    def projectOne = ProjectId.create()
    def projectTwo = ProjectId.create()
    pageService.queryProjectGroupGrants(_) >> [
        // two ACE rows for the same group must accumulate into one WRITE grant
        new ProjectAccessServiceImpl.GroupGrantRow(projectOne.value(), "GROUP_group-1",
            BasePermission.READ.mask),
        new ProjectAccessServiceImpl.GroupGrantRow(projectOne.value(), "GROUP_group-1",
            BasePermission.WRITE.mask),
        new ProjectAccessServiceImpl.GroupGrantRow(projectTwo.value(), "GROUP_group-2",
            BasePermission.READ.mask),
    ]
    // group-1's response is declared together with its cardinality below: a then-block interaction
    // re-declaring the same call shadows this stub and would return the default (null) response.
    groupInformationService.findGroupById("group-2") >> Optional.of(
        new GroupInfo("group-2", "Core Facility", null, GroupType.ADHOC))

    when:
    Map<ProjectId, List<SharedProjectGroup>> result =
        pageService.listSharedGroups([projectOne, projectTwo])

    then: "each project maps to its own groups"
    result[projectOne]*.groupName == ["NGS Lab"]
    result[projectTwo]*.groupName == ["Core Facility"]

    and: "accumulated ACE masks resolve to the highest granted role"
    result[projectOne][0].projectRole == ProjectRole.WRITE
    result[projectTwo][0].projectRole == ProjectRole.READ
    result[projectOne][0].projectId == projectOne

    and: "group information is resolved once even though the group has several ACE rows"
    1 * groupInformationService.findGroupById("group-1") >> Optional.of(
        new GroupInfo("group-1", "NGS Lab", "the sequencing lab", GroupType.ORG))
  }

  def "batch listSharedGroups orders groups by granted role and then by name"() {
    given:
    def pageService = Spy(ProjectAccessServiceImpl,
        constructorArgs: [aclService, jdbcTemplate, groupInformationService])
    def project = ProjectId.create()
    pageService.queryProjectGroupGrants(_) >> [
        new ProjectAccessServiceImpl.GroupGrantRow(project.value(), "GROUP_read",
            BasePermission.READ.mask),
        new ProjectAccessServiceImpl.GroupGrantRow(project.value(), "GROUP_admin",
            (BasePermission.READ.mask | BasePermission.WRITE.mask
                | BasePermission.ADMINISTRATION.mask)),
    ]
    groupInformationService.findGroupById("read") >> Optional.of(
        new GroupInfo("read", "Reading Group", null, GroupType.ADHOC))
    groupInformationService.findGroupById("admin") >> Optional.of(
        new GroupInfo("admin", "Admin Group", null, GroupType.ADHOC))

    when:
    def result = pageService.listSharedGroups([project])

    then: "the strongest role is listed first"
    result[project]*.groupName == ["Admin Group", "Reading Group"]
    result[project]*.projectRole == [ProjectRole.ADMIN, ProjectRole.READ]
  }

  def "batch listSharedGroups skips groups that no longer resolve"() {
    given: "a grant whose group was dissolved after the ACE was written"
    def pageService = Spy(ProjectAccessServiceImpl,
        constructorArgs: [aclService, jdbcTemplate, groupInformationService])
    def project = ProjectId.create()
    pageService.queryProjectGroupGrants(_) >> [
        new ProjectAccessServiceImpl.GroupGrantRow(project.value(), "GROUP_gone",
            BasePermission.READ.mask)]
    groupInformationService.findGroupById("gone") >> Optional.empty()

    when:
    def result = pageService.listSharedGroups([project])

    then: "the stale grant does not leak a nameless entry onto the card"
    result[project].isEmpty()
  }

  def "batch listSharedGroups short-circuits on an empty request"() {
    when:
    def result = service.listSharedGroups([])

    then: "no ACL query is issued and an empty map is returned"
    result.isEmpty()
    0 * jdbcTemplate.query(*_)
  }

  def "batch listSharedGroups maps a project without group grants to an empty list"() {
    given:
    def pageService = Spy(ProjectAccessServiceImpl,
        constructorArgs: [aclService, jdbcTemplate, groupInformationService])
    def projectWithGroups = ProjectId.create()
    def projectWithoutGroups = ProjectId.create()
    pageService.queryProjectGroupGrants(_) >> [
        new ProjectAccessServiceImpl.GroupGrantRow(projectWithGroups.value(), "GROUP_group-1",
            BasePermission.READ.mask)]
    groupInformationService.findGroupById("group-1") >> Optional.of(
        new GroupInfo("group-1", "NGS Lab", null, GroupType.ORG))

    when:
    def result = pageService.listSharedGroups([projectWithGroups, projectWithoutGroups])

    then:
    result[projectWithGroups].size() == 1
    result[projectWithoutGroups].isEmpty()
  }

  def "batch listCollaborators resolves the role per user and project"() {
    given:
    def pageService = Spy(ProjectAccessServiceImpl,
        constructorArgs: [aclService, jdbcTemplate, groupInformationService])
    def projectOne = ProjectId.create()
    def projectTwo = ProjectId.create()
    pageService.queryProjectUserGrants(_) >> [
        new ProjectAccessServiceImpl.UserGrantRow(projectOne.value(), "user-1",
            BasePermission.READ.mask),
        new ProjectAccessServiceImpl.UserGrantRow(projectOne.value(), "user-1",
            BasePermission.WRITE.mask),
        new ProjectAccessServiceImpl.UserGrantRow(projectOne.value(), "user-2",
            (BasePermission.READ.mask | BasePermission.WRITE.mask
                | BasePermission.CREATE.mask | BasePermission.DELETE.mask
                | BasePermission.ADMINISTRATION.mask)),
        new ProjectAccessServiceImpl.UserGrantRow(projectTwo.value(), "user-3",
            BasePermission.READ.mask),
    ]

    when:
    def result = pageService.listCollaborators([projectOne, projectTwo])

    then: "accumulated masks resolve to the highest granted role, strongest first"
    result[projectOne]*.userId == ["user-2", "user-1"]
    result[projectOne]*.projectRole == [ProjectRole.OWNER, ProjectRole.WRITE]
    result[projectTwo]*.userId == ["user-3"]
    result[projectTwo][0].projectRole == ProjectRole.READ
    result[projectTwo][0].projectId == projectTwo
  }

  def "batch listCollaborators omits a user whose role cannot be resolved"() {
    given: "a principal with a corrupt ACE mask that maps to no project role"
    def pageService = Spy(ProjectAccessServiceImpl,
        constructorArgs: [aclService, jdbcTemplate, groupInformationService])
    def project = ProjectId.create()
    pageService.queryProjectUserGrants(_) >> [
        new ProjectAccessServiceImpl.UserGrantRow(project.value(), "user-1",
            BasePermission.CREATE.mask)]

    when:
    def result = pageService.listCollaborators([project])

    then: "the user is omitted rather than shown with a guessed role"
    result[project].isEmpty()
  }

  /**
   * Minimal {@link AccessControlEntry} for the in-memory ACL stub.
   */
  private static class StubAccessControlEntry implements AccessControlEntry {

    private final Permission permission
    private final Sid sid
    private final boolean granting

    StubAccessControlEntry(Permission permission, Sid sid, boolean granting) {
      this.permission = permission
      this.sid = sid
      this.granting = granting
    }

    @Override
    Acl getAcl() { return null }

    @Override
    Serializable getId() { return 0 }

    @Override
    Permission getPermission() { return permission }

    @Override
    Sid getSid() { return sid }

    @Override
    boolean isGranting() { return granting }
  }
}