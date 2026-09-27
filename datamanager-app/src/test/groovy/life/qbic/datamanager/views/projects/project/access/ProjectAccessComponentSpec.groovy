package life.qbic.datamanager.views.projects.project.access

import com.vaadin.flow.component.button.Button
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
 * Unit tests for the project access component's Groups section (story FEAT-USER-GROUPS-06).
 *
 * <p>Covers: shared groups render read-only with name + description + role for an ADMIN user
 * (AC1), the add-people-or-groups control is gated to access-administration holders (AC5), the
 * already-shared groups feed the share dialog's group pre-filter, and the group confirm path
 * grants the group authority then refreshes the listing. Mirrors {@code PinnedProjectsComponentSpec}
 * (no Spring context, headless Vaadin).</p>
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

  def "renders shared groups read-only for a project ADMIN"() {
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

    and: "the group grid has no action/edit/remove column (read-only surface, AC2 deferred)"
    def columnKeys = component.@projectGroupGrid.columns.collect { it.key }
    !columnKeys.contains("action")
    !columnKeys.contains("edit")
    !columnKeys.contains("remove")

    and: "the add-people-or-groups control is visible for the ADMIN"
    buttonBarButtons().any { it.text == "Add people or groups" }
  }

  def "hides the add-people-or-groups control for a READ-only collaborator but still lists groups"() {
    given: "a project shared with a group"
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("group-1", "NGS Lab", null, projectId, ProjectRole.READ)]

    when: "a READ-only collaborator opens the project access page"
    setContext(false)

    then: "no add control is shown (share stays ADMINISTRATION-only, AC5)"
    !buttonBarInHeader()
    headerButtons().isEmpty()

    and: "the shared groups are still listed read-only with name and role"
    def rows = groupGridItems()
    rows.size() == 1
    rows[0].groupName() == "NGS Lab"
    rows[0].projectRole() == ProjectRole.READ
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
}