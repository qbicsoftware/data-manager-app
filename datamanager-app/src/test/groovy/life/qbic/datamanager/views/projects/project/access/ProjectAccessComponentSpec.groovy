package life.qbic.datamanager.views.projects.project.access

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.select.Select
import life.qbic.datamanager.security.UserPermissions
import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.general.Tag
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.AccessFilter
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.ProjectGroup
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.ProjectUser
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.GrantRequest
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.GrantRequestedEvent
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.PrincipalType
import life.qbic.identity.api.AuthenticationToUserIdTranslator
import life.qbic.identity.api.UserInfo
import life.qbic.identity.api.UserInformationService
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.usergroups.api.GroupInformationService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

/**
 * Unit tests for the dialog-free project access page (FEAT-USER-GROUPS-08).
 *
 * <p>Covers: the roster renders editable role controls and removal for access-administrators, a
 * read-only view otherwise, role change and revoke wiring, batch grants through the inline
 * composer, the type filter, and the membership-leakage guard. Headless, no Spring context.</p>
 */
class ProjectAccessComponentSpec extends Specification {

  ProjectAccessService projectAccessService = Mock()
  UserInformationService userInformationService = Mock()
  GroupInformationService groupInformationService = Mock()
  UserPermissions userPermissions = Mock()
  AuthenticationToUserIdTranslator authenticationToUserIdTranslator = Mock()

  ProjectId projectId = ProjectId.create()
  ProjectAccessComponent component

  def setup() {
    component = new ProjectAccessComponent(projectAccessService, userInformationService,
        groupInformationService, userPermissions, authenticationToUserIdTranslator)
    SecurityContextHolder.clearContext()
    SecurityContextHolder.getContext().setAuthentication(Mock(Authentication))
    authenticationToUserIdTranslator.translateToUserId(_ as Authentication) >> Optional.of("user-1")
    projectAccessService.listCollaborators(_ as ProjectId) >> []
    projectAccessService.listSharedGroups(_ as ProjectId) >> []
    UI.setCurrent(Mock(UI))
  }

  def cleanup() {
    SecurityContextHolder.clearContext()
    UI.setCurrent(null)
  }

  private void setContext(boolean canChangeAccess) {
    userPermissions.changeProjectAccess(projectId) >> canChangeAccess
    component.setContext(new Context().with(projectId))
  }

  private static UserInfo user(String id, String fullName, String username) {
    return new UserInfo(id, fullName, "${username}@example.org", username, true, null, null)
  }

  private static List<Component> allComponents(Component root) {
    def result = []
    root.children.forEach { child ->
      result << child
      result.addAll(allComponents(child))
    }
    return result
  }

  private static List<Button> buttonsIn(Component root) {
    return allComponents(root).findAll { it instanceof Button } as List<Button>
  }

  def "an access-administrator sees editable role controls and a remove action per row"() {
    given:
    setContext(true)
    def projectUser = new ProjectUser("user-2", "jdoe", "Jane Doe", "", "", ProjectRole.READ)

    when:
    def roleControl = component.userRoleControl(projectUser)
    def row = component.userRow(projectUser)

    then:
    roleControl instanceof Select
    buttonsIn(row).any { it.text == "Remove" }
    component.@composer.isVisible()
  }

  def "a collaborator without administration rights gets a read-only roster"() {
    given:
    setContext(false)
    def projectUser = new ProjectUser("user-2", "jdoe", "Jane Doe", "", "", ProjectRole.READ)

    when:
    def roleControl = component.userRoleControl(projectUser)
    def row = component.userRow(projectUser)

    then:
    roleControl instanceof Tag
    buttonsIn(row).isEmpty()
    !component.@composer.isVisible()

    and: "no membership data is ever requested (AC3/AC4)"
    0 * groupInformationService.listMyGroups(_)
  }

  def "changing a user role invokes changeRole"() {
    given:
    setContext(true)
    def projectUser = new ProjectUser("user-2", "jdoe", "Jane Doe", "", "", ProjectRole.READ)
    def roleSelect = component.userRoleControl(projectUser) as Select<ProjectRole>

    when:
    roleSelect.setValue(ProjectRole.ADMIN)

    then:
    1 * projectAccessService.changeRole(projectId, "user-2", ProjectRole.ADMIN)
  }

  def "changing a group role invokes changeAuthorityAccess with the GROUP_ prefix"() {
    given:
    setContext(true)
    def group = new ProjectGroup("g-1", "NGS Lab", "sequencing core", ProjectRole.READ)
    def roleSelect = component.groupRoleControl(group) as Select<ProjectRole>

    when:
    roleSelect.setValue(ProjectRole.WRITE)

    then:
    1 * projectAccessService.changeAuthorityAccess(projectId, "GROUP_g-1", ProjectRole.WRITE)
  }

  def "the role control offers only READ, WRITE and ADMIN, never OWNER"() {
    given:
    setContext(true)
    def group = new ProjectGroup("g-1", "NGS Lab", null, ProjectRole.READ)

    when:
    def roleSelect = component.groupRoleControl(group) as Select<ProjectRole>

    then:
    def offered = roleSelect.listDataView.items.toSet()
    offered == ([ProjectRole.READ, ProjectRole.WRITE, ProjectRole.ADMIN] as Set)
    !offered.contains(ProjectRole.OWNER)
  }

  def "revoking a user requires an inline confirmation first"() {
    given:
    setContext(true)
    def projectUser = new ProjectUser("user-2", "jdoe", "Jane Doe", "", "", ProjectRole.READ)
    def row = component.userRow(projectUser)
    def removeButton = buttonsIn(row).find { it.text == "Remove" } as Button

    when: "the ADMIN clicks Remove"
    removeButton.click()

    then: "nothing is revoked yet and an inline confirmation is shown"
    0 * projectAccessService.removeCollaborator(*_)
    buttonsIn(row).any { it.text == "Cancel" }

    when: "the ADMIN confirms"
    def confirmButton = buttonsIn(row).find {
      it instanceof Button && (it as Button).text == "Remove"
    } as Button
    confirmButton.click()

    then:
    1 * projectAccessService.removeCollaborator(projectId, "user-2")
  }

  def "the composer grants several staged principals in one batch"() {
    given:
    setContext(true)
    def composer = component.@composer
    def event = new GrantRequestedEvent(composer, false, [
        new GrantRequest(PrincipalType.USER, "user-2", ProjectRole.WRITE, "jdoe"),
        new GrantRequest(PrincipalType.GROUP, "g-9", ProjectRole.READ, "Sprint Team")])

    when:
    component.onGrantRequested(event)

    then:
    1 * projectAccessService.addCollaborator(projectId, "user-2", ProjectRole.WRITE)
    1 * projectAccessService.addAuthorityAccess(projectId, "GROUP_g-9", ProjectRole.READ)
  }

  def "the type filter hides the other section"() {
    given: "a project with one person and one group"
    projectAccessService.listCollaborators(projectId) >> [
        new ProjectCollaborator("user-2", projectId, ProjectRole.READ)]
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("g-1", "NGS Lab", null, projectId, ProjectRole.ADMIN)]
    userInformationService.findById("user-2") >> Optional.of(user("user-2", "Jane Doe", "jdoe"))
    setContext(true)

    when: "the filter is set to Groups"
    component.@filterSelect.setValue(AccessFilter.GROUPS)

    then:
    !component.@peopleSection.isVisible()
    component.@groupsSection.isVisible()
  }
}
