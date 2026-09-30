package life.qbic.datamanager.views.projects.project.access

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.select.Select
import java.util.concurrent.Executor
import life.qbic.datamanager.security.UserPermissions
import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.general.Tag
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.AccessEntry
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.AccessFilter
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
 * <p>The roster follows the measurements/samples layout: a searchable Grid with a toolbar (search,
 * type filter, selection-based Change role / Remove). Roles are shown as badges; role changes and
 * removals go through an inline action bar. Covers editable/read-only views, user/group role
 * change, inline-confirmed removal, batch grants and the type filter.</p>
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
  Executor directExecutor = { Runnable runnable -> runnable.run() } as Executor

  def setup() {
    component = new ProjectAccessComponent(projectAccessService, userInformationService,
        groupInformationService, userPermissions, authenticationToUserIdTranslator, directExecutor)
    SecurityContextHolder.clearContext()
    SecurityContextHolder.getContext().setAuthentication(Mock(Authentication))
    authenticationToUserIdTranslator.translateToUserId(_ as Authentication) >> Optional.of("user-1")
    projectAccessService.listCollaborators(_ as ProjectId) >> { collaborators }
    projectAccessService.listSharedGroups(_ as ProjectId) >> { sharedGroups }
    def ui = Mock(UI)
    ui.isAttached() >> true
    def session = Mock(com.vaadin.flow.server.VaadinSession)
    session.hasLock() >> true
    ui.getSession() >> session
    UI.setCurrent(ui)
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

  private static AccessEntry aUser(String id, String username, String fullName,
      ProjectRole role) {
    return new AccessEntry(PrincipalType.USER, id, username, fullName, null, null, null, null,
        role)
  }

  private static AccessEntry aGroup(String id, String name, String description,
      ProjectRole role) {
    return new AccessEntry(PrincipalType.GROUP, id, null, null, null, null, name, description, role)
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

  private Component roleBadgeOf(AccessEntry entry) {
    return component.@grid.getColumnByKey("role").getRenderer().createComponent(entry)
  }

  private Select<ProjectRole> roleSelectInActionBar() {
    return allComponents(component.@actionBar).find { it instanceof Select } as Select<ProjectRole>
  }

  def "an access-administrator sees a role badge and the toolbar actions"() {
    given:
    setContext(true)
    def entry = aUser("user-2", "jdoe", "Jane Doe", ProjectRole.READ)

    when:
    def roleBadge = roleBadgeOf(entry)

    then:
    roleBadge instanceof Tag
    component.@changeRoleButton.isVisible()
    component.@removeButton.isVisible()
    component.@composer.isVisible()
  }

  def "a collaborator without administration rights gets a read-only view"() {
    given:
    setContext(false)
    def entry = aUser("user-2", "jdoe", "Jane Doe", ProjectRole.READ)

    when:
    def roleBadge = roleBadgeOf(entry)

    then:
    roleBadge instanceof Tag
    !component.@changeRoleButton.isVisible()
    !component.@removeButton.isVisible()
    !component.@composer.isVisible()

    and: "no membership data is ever requested (AC3/AC4)"
    0 * groupInformationService.listMyGroups(_)
  }

  def "changing a user role invokes changeRole"() {
    given: "a project shared with one user"
    collaborators = [new ProjectCollaborator("user-2", projectId, ProjectRole.READ)]
    userInformationService.findById("user-2") >> Optional.of(user("user-2", "Jane Doe", "jdoe"))
    setContext(true)
    component.@grid.asMultiSelect().select(component.@grid.getListDataView().getItems()
        .find { it.id() == "user-2" })

    when: "the ADMIN picks ADMIN in the role dropdown and applies"
    component.@changeRoleButton.click()
    def roleSelect = roleSelectInActionBar()
    roleSelect.setValue(ProjectRole.ADMIN)
    buttonsIn(component.@actionBar).find { it.text == "Apply" }.click()

    then:
    1 * projectAccessService.changeRole(projectId, "user-2", ProjectRole.ADMIN)
  }

  def "changing a group role invokes changeAuthorityAccess with the GROUP_ prefix"() {
    given: "a project shared with one group"
    sharedGroups = [new SharedProjectGroup("g-1", "NGS Lab", "sequencing core", projectId,
        ProjectRole.READ)]
    setContext(true)
    component.@grid.asMultiSelect().select(component.@grid.getListDataView().getItems()
        .find { it.id() == "g-1" })

    when:
    component.@changeRoleButton.click()
    def roleSelect = roleSelectInActionBar()
    roleSelect.setValue(ProjectRole.WRITE)
    buttonsIn(component.@actionBar).find { it.text == "Apply" }.click()

    then:
    1 * projectAccessService.changeAuthorityAccess(projectId, "GROUP_g-1", ProjectRole.WRITE)
  }

  def "the role dropdown defaults to member and offers only READ, WRITE and ADMIN"() {
    given:
    collaborators = [new ProjectCollaborator("user-2", projectId, ProjectRole.READ)]
    userInformationService.findById("user-2") >> Optional.of(user("user-2", "Jane Doe", "jdoe"))
    setContext(true)
    component.@grid.asMultiSelect().select(component.@grid.getListDataView().getItems()
        .find { it.id() == "user-2" })

    when:
    component.@changeRoleButton.click()
    def roleSelect = roleSelectInActionBar()

    then:
    roleSelect.getValue() == ProjectRole.READ
    def offered = roleSelect.listDataView.items.toSet()
    offered == ([ProjectRole.READ, ProjectRole.WRITE, ProjectRole.ADMIN] as Set)
    !offered.contains(ProjectRole.OWNER)
  }

  def "the toolbar highlights the number of selected principals"() {
    given:
    collaborators = [new ProjectCollaborator("user-2", projectId, ProjectRole.READ)]
    userInformationService.findById("user-2") >> Optional.of(user("user-2", "Jane Doe", "jdoe"))
    setContext(true)

    when:
    component.@grid.asMultiSelect().select(component.@grid.getListDataView().getItems()
        .find { it.id() == "user-2" })

    then:
    component.@selectionCount.isVisible()
    component.@selectionCount.getText() == "1 selected"
  }

  def "removing a selected principal requires an inline confirmation"() {
    given: "a project shared with one user"
    collaborators = [new ProjectCollaborator("user-2", projectId, ProjectRole.READ)]
    userInformationService.findById("user-2") >> Optional.of(user("user-2", "Jane Doe", "jdoe"))
    setContext(true)
    component.@grid.asMultiSelect().select(component.@grid.getListDataView().getItems()
        .find { it.id() == "user-2" })

    when: "the ADMIN clicks Remove"
    component.@removeButton.click()

    then: "an inline confirmation is shown and nothing is removed yet"
    0 * projectAccessService.removeCollaborator(*_)
    component.@actionBar.isVisible()

    when: "the ADMIN confirms"
    def confirm = buttonsIn(component.@actionBar).find { it.text == "Remove" } as Button
    confirm.click()

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

  def "the type filter narrows the table"() {
    given:
    collaborators = [new ProjectCollaborator("user-2", projectId, ProjectRole.READ)]
    sharedGroups = [new SharedProjectGroup("g-1", "NGS Lab", null, projectId, ProjectRole.ADMIN)]
    userInformationService.findById("user-2") >> Optional.of(user("user-2", "Jane Doe", "jdoe"))
    setContext(true)

    when:
    component.@filterSelect.setValue(AccessFilter.GROUPS)

    then:
    component.@grid.getListDataView().getItems().collect { it.displayName() }.toList() ==
        ["NGS Lab"]
  }
}
