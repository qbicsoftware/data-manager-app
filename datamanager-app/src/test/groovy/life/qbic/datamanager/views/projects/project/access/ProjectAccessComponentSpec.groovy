package life.qbic.datamanager.views.projects.project.access

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.select.Select
import life.qbic.datamanager.security.UserPermissions
import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.ProjectGroup
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.ProjectUser
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.GrantRequest
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.GrantRequestedEvent
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.PrincipalType
import life.qbic.identity.api.AuthenticationToUserIdTranslator
import life.qbic.identity.api.UserInformationService
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.usergroups.api.GroupInformationService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

/**
 * Unit tests for the dialog-free project access component (story FEAT-USER-GROUPS-08).
 *
 * <p>Covers: shared groups render with name, description and an always-visible role control for an
 * ADMIN (AC1), the ADMIN can change a shared group's role and revoke the grant through an inline
 * confirmation (AC2), the inline composer grants one or several principals in a single action,
 * READ-only collaborators get a read-only view (AC4/D7), and rendering never touches membership
 * data (AC3). No Spring context, headless Vaadin.</p>
 */
class ProjectAccessComponentSpec extends Specification {

  ProjectAccessService projectAccessService = Mock()
  UserInformationService userInformationService = Mock()
  GroupInformationService groupInformationService = Mock()
  UserPermissions userPermissions = Mock()
  AuthenticationToUserIdTranslator authenticationToUserIdTranslator = Mock()

  ProjectId projectId = ProjectId.create()
  ProjectAccessComponent component

  /** Mutable backing lists the service stubs read from, so tests can define the state. */
  List collaborators = []
  List sharedGroups = []

  def setup() {
    component = new ProjectAccessComponent(projectAccessService, userInformationService,
        groupInformationService, userPermissions, authenticationToUserIdTranslator)
    SecurityContextHolder.clearContext()
    SecurityContextHolder.getContext().setAuthentication(Mock(Authentication))
    authenticationToUserIdTranslator.translateToUserId(_ as Authentication) >> Optional.of("user-1")
    projectAccessService.listCollaborators(_ as ProjectId) >> { collaborators }
    projectAccessService.listSharedGroups(_ as ProjectId) >> { sharedGroups }
    // notifications (StyledNotification.open) require a current UI
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

  private List<ProjectGroup> groupGridItems() {
    return component.@projectGroupGrid.getGenericDataView().getItems().toList()
  }

  private List<Component> allComponents(Component root) {
    def result = []
    root.children.forEach { child ->
      result << child
      result.addAll(allComponents(child))
    }
    return result
  }

  private List<Button> buttonsIn(Component root) {
    return allComponents(root).findAll { it instanceof Button } as List<Button>
  }

  private boolean buttonBarInHeader() {
    return component.@header.children.any { it == component.@buttonBar }
  }

  /**
   * Renders the action-cell component for the given group row exactly as the grid would, using
   * the column's public {@code ComponentRenderer}.
   */
  private Div actionCellFor(ProjectGroup projectGroup) {
    def renderer = component.@projectGroupGrid.getColumnByKey("action").getRenderer()
    return renderer.createComponent(projectGroup) as Div
  }

  def "renders shared groups with an always-visible role control and a remove action for a project ADMIN"() {
    given: "a project shared with one group at ADMIN"
    sharedGroups = [new SharedProjectGroup("group-1", "Bioinformatics Lab", "the lab", projectId,
        ProjectRole.ADMIN)]

    when: "an ADMIN opens the project access page"
    setContext(true)

    then: "the group appears with its name, description and granted role"
    def rows = groupGridItems()
    rows.size() == 1
    rows[0].groupId() == "group-1"
    rows[0].groupName() == "Bioinformatics Lab"
    rows[0].groupDescription() == "the lab"
    rows[0].projectRole() == ProjectRole.ADMIN

    and: "the role is editable inline, without an extra edit step"
    component.renderGroupRoleComponent(rows[0]) instanceof Select

    and: "the action column offers a remove button for the ADMIN (AC2)"
    def actionCell = actionCellFor(rows[0])
    buttonsIn(actionCell).any { it.text == "Remove" }

    and: "the add-people-or-groups control is visible for the ADMIN"
    buttonBarInHeader()
  }

  def "renders a read-only view for a collaborator without access-administration rights"() {
    given: "a project shared with one group"
    sharedGroups = [new SharedProjectGroup("group-1", "NGS Lab", null, projectId, ProjectRole.READ)]

    when: "a READ-only collaborator opens the project access page"
    setContext(false)

    then: "no add control is shown"
    !buttonBarInHeader()

    and: "the group role is a static label, not an editable control"
    def rows = groupGridItems()
    rows.size() == 1
    rows[0].groupName() == "NGS Lab"
    component.renderGroupRoleComponent(rows[0]) instanceof Span

    and: "the action column renders no remove button"
    buttonsIn(actionCellFor(rows[0])).isEmpty()

    and: "no membership data is ever requested when rendering the groups surface (AC3/AC4)"
    0 * groupInformationService.listMyGroups(_)
  }

  def "changing the role of a shared group invokes changeAuthorityAccess with the GROUP_ prefix and refreshes"() {
    given: "a project ADMIN viewing a project shared with one group at READ"
    sharedGroups = [new SharedProjectGroup("group-1", "NGS Lab", "sequencing core", projectId,
        ProjectRole.READ)]
    setContext(true)
    def rows = groupGridItems()

    when: "the ADMIN selects ADMIN as the group's new project role"
    def roleSelect = component.renderGroupRoleComponent(rows[0]) as Select<ProjectRole>
    roleSelect.setValue(ProjectRole.ADMIN)

    then: "the change is propagated to the access service with the GROUP_ authority prefix (AC2)"
    1 * projectAccessService.changeAuthorityAccess(projectId, "GROUP_group-1", ProjectRole.ADMIN)
  }

  def "the group role control offers only READ, WRITE and ADMIN, never OWNER"() {
    given: "a project ADMIN viewing a shared group"
    sharedGroups = [new SharedProjectGroup("group-1", "NGS Lab", null, projectId, ProjectRole.READ)]
    setContext(true)

    when: "the role control is resolved for the group"
    def rows = groupGridItems()
    def roleSelect = component.renderGroupRoleComponent(rows[0]) as Select<ProjectRole>

    then: "only READ, WRITE and ADMIN are offered, never OWNER"
    def offered = roleSelect.listDataView.items.toSet()
    offered == ([ProjectRole.READ, ProjectRole.WRITE, ProjectRole.ADMIN] as Set)
    !offered.contains(ProjectRole.OWNER)
  }

  def "revoking a shared group requires an inline confirmation and then removes the grant"() {
    given: "a project ADMIN viewing a project shared with one group"
    sharedGroups = [new SharedProjectGroup("group-1", "NGS Lab", "sequencing core", projectId,
        ProjectRole.ADMIN)]
    setContext(true)
    def rows = groupGridItems()
    def actionCell = actionCellFor(rows[0])
    def removeButton = buttonsIn(actionCell).find { it.text == "Remove" } as Button

    when: "the ADMIN clicks Remove"
    removeButton.click()

    then: "an inline confirmation is shown and nothing has been revoked yet"
    0 * projectAccessService.removeAuthorityAccess(*_)
    buttonsIn(actionCell).any { it.text == "Cancel" }

    when: "the ADMIN confirms the removal inline"
    def confirmButton = buttonsIn(actionCell).find {
      it instanceof Button && (it as Button).text == "Remove"
    } as Button
    confirmButton.click()

    then: "the group grant is revoked from the project (AC2)"
    1 * projectAccessService.removeAuthorityAccess(projectId, "GROUP_group-1")
  }

  def "cancelling the inline confirmation restores the remove action without revoking"() {
    given: "a project ADMIN viewing a project shared with one group"
    sharedGroups = [new SharedProjectGroup("group-1", "NGS Lab", null, projectId, ProjectRole.READ)]
    setContext(true)
    def rows = groupGridItems()
    def actionCell = actionCellFor(rows[0])
    def removeButton = buttonsIn(actionCell).find { it.text == "Remove" } as Button
    removeButton.click()

    when: "the ADMIN cancels"
    def cancelButton = buttonsIn(actionCell).find { it.text == "Cancel" } as Button
    cancelButton.click()

    then: "nothing is revoked and the remove action is available again"
    0 * projectAccessService.removeAuthorityAccess(*_)
    buttonsIn(actionCell).any { it.text == "Remove" }
  }

  def "the composer grants several staged principals in a single batch action"() {
    given: "a project ADMIN viewing a project without shared groups"
    setContext(true)
    def composer = component.@composer
    def event = new GrantRequestedEvent(composer, false, [
        new GrantRequest(PrincipalType.USER, "user-2", ProjectRole.WRITE, "jdoe"),
        new GrantRequest(PrincipalType.GROUP, "group-9", ProjectRole.READ, "Sprint Team")])

    when: "the user grants the staged batch"
    component.onGrantRequested(event)

    then: "every staged principal is granted through the correct service path"
    1 * projectAccessService.addCollaborator(projectId, "user-2", ProjectRole.WRITE)
    1 * projectAccessService.addAuthorityAccess(projectId, "GROUP_group-9", ProjectRole.READ)
  }

  def "changing a user role invokes changeRole and refreshes"() {
    given: "a project ADMIN viewing a direct collaborator"
    setContext(true)
    def projectUser = new ProjectUser("user-2", "jdoe", "Jane Doe", "", "",
        ProjectRole.READ)

    when: "the ADMIN changes the user's role to WRITE"
    def roleSelect = component.renderUserRoleComponent(projectUser) as Select<ProjectRole>
    roleSelect.setValue(ProjectRole.WRITE)

    then: "the change is propagated to the access service"
    1 * projectAccessService.changeRole(projectId, "user-2", ProjectRole.WRITE)
  }
}
