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
 * <p>Covers the OWNER invariant on the authority write path (AC3), the duplicate-authority
 * guard (AC2) and the shared-groups listing (AC1) without a database or Spring context.</p>
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
    }
  }

  private static AccessControlEntry ace(Permission permission, Sid sid, boolean granting) {
    return new StubAccessControlEntry(permission, sid, granting)
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

  def "duplicate authority grant throws"() {
    given: "an authority already granted the ADMIN role"
    // Pre-seed the ACL with a ROLE_EXAMPLE authority grant so the duplicate
    // check is deterministic regardless of mock insert-order behavior.
    entries.add(ace(BasePermission.ADMINISTRATION,
        new GrantedAuthoritySid("ROLE_EXAMPLE"), true))

    when: "the same authority is granted again"
    service.addAuthorityAccess(projectId, "ROLE_EXAMPLE", ProjectRole.ADMIN)

    then: "a duplicate-grant error is raised mentioning the authority and project"
    def error = thrown(Exception)
    error.message.contains("ROLE_EXAMPLE")
    error.message.contains(projectId.value())
  }

  def "listSharedGroups exposes shared groups with name, role and no member data"() {
    given: "a project with a shared group at READ"
    service.addAuthorityAccess(projectId, "GROUP_group-1", ProjectRole.READ)
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
    service.addAuthorityAccess(projectId, "ROLE_EXAMPLE", ProjectRole.ADMIN)

    when:
    List<SharedProjectGroup> sharedGroups = service.listSharedGroups(projectId)

    then: "no group is listed"
    sharedGroups.isEmpty()
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