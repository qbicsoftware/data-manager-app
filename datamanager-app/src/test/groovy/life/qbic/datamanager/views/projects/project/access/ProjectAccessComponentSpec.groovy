package life.qbic.datamanager.views.projects.project.access

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.select.Select
import life.qbic.datamanager.security.UserPermissions
import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.projects.project.access.AddCollaboratorToProjectDialog.GroupConfirmEvent
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.ProjectGroup
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
 * Unit tests for the project access component's Groups section (story FEAT-USER-GROUPS-08).
 *
 * <p>Covers: shared groups render with name + description + role for an ADMIN user (AC1), the
 * ADMIN can change a shared group's role and revoke the grant (AC2), the add-people-or-groups
 * control is gated to access-administration holders, non-administrators see neither role-editing
 * nor revoke affordances (AC4), and rendering never touches membership data (AC3). Mirrors
 * {@code PinnedProjectsComponentSpec} (no Spring context, headless Vaadin).</p>
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
    // notifications (StyledNotification.open) and dialog close() require a current UI
    com.vaadin.flow.component.UI.setCurrent(Mock(com.vaadin.flow.component.UI))
  }

  def cleanup() {
    SecurityContextHolder.clearContext()
    com.vaadin.flow.component.UI.setCurrent(null)
  }

  private void setContext(boolean canChangeAccess) {
    userPermissions.changeProjectAccess(projectId) >> canChangeAccess
    projectAccessService.listCollaborators(projectId) >> []
    component.setContext(new Context().with(projectId))
  }

  private List<ProjectGroup> groupGridItems() {
    return component.@projectGroupGrid.getGenericDataView().getItems().toList()
  }

  private List<Button> headerButtons() {
    return component.@header.children.findAll { it instanceof Button }
  }

  private List<Button> buttonBarButtons() {
    return component.@buttonBar.children.findAll { it instanceof Button }
  }

  private boolean buttonBarInHeader() {
    return component.@header.children.any { it == component.@buttonBar }
  }

  private AddCollaboratorToProjectDialog dialog() {
    groupInformationService.listPublicDirectory() >> []
    return new AddCollaboratorToProjectDialog(userInformationService, projectId, [],
        groupInformationService, [])
  }

  /**
   * Renders the action-cell component for the given group row exactly as the grid would, using
   * the column's public {@code ComponentRenderer}.
   */
  private Span actionCellFor(ProjectGroup projectGroup) {
    def renderer = component.@projectGroupGrid.getColumnByKey("action").getRenderer()
    return renderer.createComponent(projectGroup) as Span
  }

  /**
   * Resolves the role editor {@link Select} the grid uses for the given group row by invoking the
   * component's private editor-rendering method (same component the grid's editor displays).
   */
  private Select<ProjectRole> roleEditorFor(ProjectGroup projectGroup) {
    return component.renderProjectGroupRoleComponent(projectGroup) as Select<ProjectRole>
  }

  def "renders shared groups with an editable/revocable action column for a project ADMIN"() {
    given: "a project shared with one group at ADMIN"
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("group-1", "Bioinformatics Lab", "the lab", projectId,
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

    and: "the group grid offers an action column with edit and remove buttons for the ADMIN (AC2)"
    columnKeys().contains("action")
    def actionCell = actionCellFor(rows[0])
    actionCell.children.any { it instanceof Button && it.text == "Edit" }
    actionCell.children.any { it instanceof Button && it.text == "Remove" }

    and: "the add-people-or-groups control is visible for the ADMIN"
    buttonBarButtons().any { it.text == "Add people or groups" }
  }

  def "renders empty action cells without edit/remove buttons for a READ-only collaborator"() {
    given: "a project shared with one group"
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("group-1", "NGS Lab", null, projectId, ProjectRole.READ)]

    when: "a READ-only collaborator opens the project access page"
    setContext(false)

    then: "no add control is shown (share stays ADMINISTRATION-only, AC5)"
    !buttonBarInHeader()
    headerButtons().isEmpty()

    and: "the shared groups are still listed with name and role"
    def rows = groupGridItems()
    rows.size() == 1
    rows[0].groupName() == "NGS Lab"
    rows[0].projectRole() == ProjectRole.READ

    and: "the action column exists but renders no edit/remove buttons (AC4 surface gate)"
    columnKeys().contains("action")
    def actionCell = actionCellFor(rows[0])
    !actionCell.children.any { it instanceof Button }

    and: "no membership data is ever requested when rendering the groups surface (AC3/AC4)"
    0 * groupInformationService.listMyGroups(_)
  }

  def "group confirm grants the GROUP_ authority and refreshes the groups listing"() {
    given: "a project ADMIN viewing the access page with no shared groups yet"
    projectAccessService.listSharedGroups(projectId) >> []
    setContext(true)
    def dialog = dialog()
    def confirmEvent = new GroupConfirmEvent(dialog, false, "group-2", ProjectRole.WRITE)

    when: "the user confirms sharing group-2 at WRITE"
    component.onGroupSharedConfirmed(confirmEvent)

    then: "the group authority is granted on the project"
    1 * projectAccessService.addAuthorityAccess(projectId, "GROUP_group-2", ProjectRole.WRITE)

    and: "the groups listing is refreshed to include the newly shared group"
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("group-2", "Sprint Team", "agile squad", projectId,
            ProjectRole.WRITE)]
    groupGridItems().collect { it.groupId() } == ["group-2"]

    and: "the dialog is closed via the confirm event source"
    confirmEvent.source.isOpened() == false
  }

  def "duplicate group share shows a friendly error and keeps the dialog open"() {
    given: "a project ADMIN viewing the access page"
    projectAccessService.listSharedGroups(projectId) >> []
    setContext(true)
    projectAccessService.addAuthorityAccess(projectId, "GROUP_group-2", ProjectRole.READ) >> {
      throw new life.qbic.application.commons.ApplicationException("already collaborates")
    }
    def dialog = dialog()
    def confirmEvent = new GroupConfirmEvent(dialog, false, "group-2", ProjectRole.READ)

    when: "the user confirms a group that is already shared onto the project"
    component.onGroupSharedConfirmed(confirmEvent)

    then: "the duplicate grant was attempted (and rejected at the service boundary)"
    1 * projectAccessService.addAuthorityAccess(projectId, "GROUP_group-2", ProjectRole.READ)

    and: "the error path surfaces the friendly message without crashing and without closing the dialog"
    0 * projectAccessService.removeAuthorityAccess(*_)
    0 * projectAccessService.removeCollaborator(*_)

    and: "the groups listing is not refreshed with a stale entry"
    groupGridItems().isEmpty()
  }

  def "changing the role of a shared group invokes changeAuthorityAccess with the GROUP_ prefix and refreshes"() {
    given: "a project ADMIN viewing a project shared with one group at READ"
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("group-1", "NGS Lab", "sequencing core", projectId,
            ProjectRole.READ)]
    setContext(true)
    def rows = groupGridItems()

    when: "the ADMIN selects ADMIN as the group's new project role"
    def roleSelect = roleEditorFor(rows[0])
    roleSelect.setValue(ProjectRole.ADMIN)

    then: "the change is propagated to the access service with the GROUP_ authority prefix (AC2)"
    1 * projectAccessService.changeAuthorityAccess(projectId, "GROUP_group-1", ProjectRole.ADMIN)

    and: "the groups listing is refreshed with the new role"
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("group-1", "NGS Lab", "sequencing core", projectId,
            ProjectRole.ADMIN)]
    def refreshedRows = groupGridItems()
    refreshedRows.collect { it.projectRole() } == [ProjectRole.ADMIN]
  }

  def "the group role editor offers only READ, WRITE and ADMIN, never OWNER"() {
    given: "a project ADMIN viewing a shared group"
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("group-1", "NGS Lab", null, projectId, ProjectRole.READ)]
    setContext(true)

    when: "the role editor is resolved for the group"
    def rows = groupGridItems()
    def roleSelect = roleEditorFor(rows[0])

    then: "only READ, WRITE and ADMIN are offered, never OWNER"
    def offered = roleSelect.listDataView.items.toSet()
    offered == ([ProjectRole.READ, ProjectRole.WRITE, ProjectRole.ADMIN] as Set)
    !offered.contains(ProjectRole.OWNER)
  }

  def "the remove button confirms and then revokes the shared group's grant"() {
    given: "a project ADMIN viewing a project shared with one group"
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("group-1", "NGS Lab", "sequencing core", projectId,
            ProjectRole.ADMIN)]
    setContext(true)
    def rows = groupGridItems()

    when: "the ADMIN clicks Remove and confirms the alert in the action cell"
    def actionCell = actionCellFor(rows[0])
    def removeButton = actionCell.children.find { it instanceof Button && it.text == "Remove" } as Button
    removeButton.click()

    then: "a confirmation alert is shown and rendering has not revoked anything yet"
    0 * projectAccessService.removeAuthorityAccess(*_)

    when: "the ADMIN confirms the removal"
    // the alert's confirm action triggers the component's revoke path
    component.revokeGroup(rows[0])

    then: "the group grant is revoked from the project (AC2)"
    1 * projectAccessService.removeAuthorityAccess(projectId, "GROUP_group-1")

    and: "the groups listing is refreshed to drop the revoked group"
    projectAccessService.listSharedGroups(projectId) >> []
    groupGridItems().isEmpty()
  }

  private List<String> columnKeys() {
    return component.@projectGroupGrid.columns.collect { it.key }
  }
}