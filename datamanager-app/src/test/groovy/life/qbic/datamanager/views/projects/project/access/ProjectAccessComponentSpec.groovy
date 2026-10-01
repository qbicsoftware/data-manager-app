package life.qbic.datamanager.views.projects.project.access

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.select.Select
import java.util.Locale
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
    def messageSource = new org.springframework.context.support.StaticMessageSource()
    // StaticMessageSource requires exact-locale entries; the component resolves the locale from
    // the current UI / JVM default.
    def locale = Locale.getDefault()
    messageSource.addMessage("project-access.role.updated.success.message.type",
        locale, "text")
    messageSource.addMessage("project-access.role.updated.success.message.text",
        locale, "Role for 1 selected principal was changed to {0}.")
    messageSource.addMessage("project-access.role.updated.success.plural.message.type",
        locale, "text")
    messageSource.addMessage("project-access.role.updated.success.plural.message.text",
        locale, "Roles for {0} selected principals were changed to {1}.")
    messageSource.addMessage("project-access.role.updated.success.level",
        locale, "success")
    component = new ProjectAccessComponent(projectAccessService, userInformationService,
        groupInformationService, userPermissions, authenticationToUserIdTranslator, directExecutor,
        new life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory(messageSource))
    SecurityContextHolder.clearContext()
    SecurityContextHolder.getContext().setAuthentication(Mock(Authentication))
    authenticationToUserIdTranslator.translateToUserId(_ as Authentication) >> Optional.of("user-1")
    projectAccessService.listCollaborators(_ as ProjectId) >> { collaborators }
    projectAccessService.listSharedGroups(_ as ProjectId) >> { sharedGroups }
    groupInformationService.findGroupById(_ as String) >> Optional.empty()
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
        null, role)
  }

  private static AccessEntry aGroup(String id, String name, String description,
      ProjectRole role) {
    return new AccessEntry(PrincipalType.GROUP, id, null, null, null, null, name, description,
        null, role)
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
    // The role renderer now returns the role cell (fixed indicator slot + role badge).
    return component.@grid.getColumnByKey("role").getRenderer().createComponent(entry)
  }

  /**
   * True if the role cell (its children, incl. the fixed indicator slot) contains the role badge.
   */
  private static boolean roleBadgeHasBadge(Component roleCell) {
    return allComponents(roleCell).any {
      it instanceof Tag && it.getElement().getClassList().contains("access-role-badge")
    }
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
    roleBadge instanceof com.vaadin.flow.component.html.Div
    roleBadgeHasBadge(roleBadge)
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
    roleBadge instanceof com.vaadin.flow.component.html.Div
    roleBadgeHasBadge(roleBadge)
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

    then: "the role chooser is a management action, not a danger action"
    !component.@actionBar.getElement().getClassList().contains("access-inline-confirm-danger")

    when:
    def roleSelect = roleSelectInActionBar()
    roleSelect.setValue(ProjectRole.ADMIN)
    buttonsIn(component.@actionBar).find { it.text == "Apply" }.click()

    then:
    1 * projectAccessService.changeRole(projectId, "user-2", ProjectRole.ADMIN)
  }

  def "a successful role change requests a success toast with the role label"() {
    given: "a project shared with one user"
    collaborators = [new ProjectCollaborator("user-2", projectId, ProjectRole.READ)]
    userInformationService.findById("user-2") >> Optional.of(user("user-2", "Jane Doe", "jdoe"))
    setContext(true)
    component.@grid.asMultiSelect().select(component.@grid.getListDataView().getItems()
        .find { it.id() == "user-2" })

    when: "the manager changes the role to WRITE and applies"
    component.@changeRoleButton.click()
    def roleSelect = roleSelectInActionBar()
    roleSelect.setValue(ProjectRole.WRITE)
    buttonsIn(component.@actionBar).find { it.text == "Apply" }.click()

    then: "the role change went through"
    1 * projectAccessService.changeRole(projectId, "user-2", ProjectRole.WRITE)
    and: "a success toast key for the updated role was requested"
    component.@notificationFactory.toast("project-access.role.updated.success",
        ["editor"] as Object[], Locale.getDefault()) != null
  }

  def "increasing a role shows an up indicator on the row"() {
    given: "a project shared with one READ user"
    collaborators = [new ProjectCollaborator("user-2", projectId, ProjectRole.READ)]
    userInformationService.findById("user-2") >> Optional.of(user("user-2", "Jane Doe", "jdoe"))
    setContext(true)
    component.@grid.asMultiSelect().select(component.@grid.getListDataView().getItems()
        .find { it.id() == "user-2" })

    when: "the user's role is increased READ → ADMIN"
    component.@changeRoleButton.click()
    def roleSelect = roleSelectInActionBar()
    roleSelect.setValue(ProjectRole.ADMIN)
    buttonsIn(component.@actionBar).find { it.text == "Apply" }.click()

    then: "the change is recorded and the role cell shows the up indicator before the badge"
    component.@recentRoleChanges.containsKey("user-2")
    def roleCell = component.@grid.getColumnByKey("role").getRenderer()
        .createComponent(component.@grid.getListDataView().getItems().find { it.id() == "user-2" })
    roleCellHasIcon(roleCell, "role-change-up")
  }

  def "decreasing a role shows a down indicator on the row"() {
    given: "a project shared with one ADMIN user"
    collaborators = [new ProjectCollaborator("user-2", projectId, ProjectRole.ADMIN)]
    userInformationService.findById("user-2") >> Optional.of(user("user-2", "Jane Doe", "jdoe"))
    setContext(true)
    component.@grid.asMultiSelect().select(component.@grid.getListDataView().getItems()
        .find { it.id() == "user-2" })

    when: "the user's role is decreased ADMIN → READ"
    component.@changeRoleButton.click()
    def roleSelect = roleSelectInActionBar()
    roleSelect.setValue(ProjectRole.READ)
    buttonsIn(component.@actionBar).find { it.text == "Apply" }.click()

    then: "the change is recorded and the role cell shows the down indicator before the badge"
    component.@recentRoleChanges.containsKey("user-2")
    def roleCell = component.@grid.getColumnByKey("role").getRenderer()
        .createComponent(component.@grid.getListDataView().getItems().find { it.id() == "user-2" })
    roleCellHasIcon(roleCell, "role-change-down")
  }

  private static boolean roleCellHasIcon(com.vaadin.flow.component.Component roleCell,
      String iconClass) {
    // role cell -> slot -> (icon | empty). The icon lives one level deeper now that the fixed
    // indicator slot wraps it.
    return roleCell.getElement().getChildren().any { slot ->
      slot.getChildren().any { it.getAttribute("class") != null &&
          it.getAttribute("class").contains(iconClass) }
    }
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
    and: "the remove confirmation uses the danger styling"
    component.@actionBar.getElement().getClassList().contains("access-inline-confirm-danger")

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

  def "the roster defaults to role, then type, then name order"() {
    given: "users and groups with mixed roles and names"
    collaborators = [
        new ProjectCollaborator("u-read", projectId, ProjectRole.READ),
        new ProjectCollaborator("u-admin", projectId, ProjectRole.ADMIN),
        new ProjectCollaborator("u-write", projectId, ProjectRole.WRITE),
        new ProjectCollaborator("u-owner", projectId, ProjectRole.OWNER)]
    sharedGroups = [
        new SharedProjectGroup("g-read", "Zebra Team", null, projectId, ProjectRole.READ),
        new SharedProjectGroup("g-admin", "Alpha Lab", null, projectId, ProjectRole.ADMIN),
        new SharedProjectGroup("g-read2", "Beta Lab", null, projectId, ProjectRole.READ)]
    userInformationService.findById("u-read") >> Optional.of(user("u-read", "Read", "alice"))
    userInformationService.findById("u-admin") >> Optional.of(user("u-admin", "Admin", "carol"))
    userInformationService.findById("u-write") >> Optional.of(user("u-write", "Write", "bob"))
    userInformationService.findById("u-owner") >> Optional.of(user("u-owner", "Owner", "dave"))
    groupInformationService.findGroupById(_ as String) >> Optional.empty()
    setContext(true)

    when:
    def names = component.@grid.getListDataView().getItems()
        .collect { "${it.type()}:${it.displayName()}" }.toList()

    then: "all OWNERs first, then ADMINs, WRITEs, READs; users before groups; then name"
    names == [
        "USER:dave",
        "USER:carol", "GROUP:Alpha Lab",
        "USER:bob",
        "USER:alice", "GROUP:Beta Lab", "GROUP:Zebra Team"]
  }

  def "the principal column sorts by role, then type, then name"() {
    given:
    def entry1 = aGroup("g-1", "Zebra Team", null, ProjectRole.READ)
    def entry2 = aUser("u-1", "alice", "Alice", ProjectRole.READ)
    def entry3 = aUser("u-2", "carol", "Carol", ProjectRole.ADMIN)
    def column = component.@grid.getColumnByKey("principal")

    when:
    def sorted = [entry1, entry2, entry3].stream()
        .sorted(column.getComparator(com.vaadin.flow.data.provider.SortDirection.ASCENDING))
        .toList()

    then:
    sorted*.displayName() == ["carol", "alice", "Zebra Team"]
  }

  def "the stats overview shows the total and per-role counts"() {
    given:
    collaborators = [
        new ProjectCollaborator("u-1", projectId, ProjectRole.READ),
        new ProjectCollaborator("u-2", projectId, ProjectRole.ADMIN),
        new ProjectCollaborator("u-3", projectId, ProjectRole.ADMIN)]
    sharedGroups = [new SharedProjectGroup("g-1", "NGS Lab", null, projectId, ProjectRole.READ)]
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "One", "alice"))
    userInformationService.findById("u-2") >> Optional.of(user("u-2", "Two", "bob"))
    userInformationService.findById("u-3") >> Optional.of(user("u-3", "Three", "carol"))
    groupInformationService.findGroupById("g-1") >> Optional.empty()
    setContext(true)

    expect: "the chips show the right counts"
    statChipText(component.@allStatChip) == "Shared total 4"
    statChipText(component.@roleStatChips.get(ProjectRole.OWNER)) == "owner 0"
    statChipText(component.@roleStatChips.get(ProjectRole.ADMIN)) == "manager 2"
    statChipText(component.@roleStatChips.get(ProjectRole.WRITE)) == "editor 0"
    statChipText(component.@roleStatChips.get(ProjectRole.READ)) == "member 2"
  }

  def "clicking a role chip filters the grid to that role"() {
    given: "a project shared via users and groups with mixed roles"
    collaborators = [
        new ProjectCollaborator("u-1", projectId, ProjectRole.READ),
        new ProjectCollaborator("u-2", projectId, ProjectRole.ADMIN)]
    sharedGroups = [new SharedProjectGroup("g-1", "NGS Lab", null, projectId, ProjectRole.ADMIN)]
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "One", "alice"))
    userInformationService.findById("u-2") >> Optional.of(user("u-2", "Two", "bob"))
    groupInformationService.findGroupById("g-1") >> Optional.empty()
    setContext(true)

    when:
    clickChip(component.@roleStatChips.get(ProjectRole.ADMIN))

    then: "only the two managers are shown"
    component.@grid.getListDataView().getItems().collect { it.displayName() }.toList() ==
        ["bob", "NGS Lab"]

    and: "the type filter Select still allows narrowing further"
    component.@filterSelect.setValue(AccessFilter.PEOPLE)
    component.@grid.getListDataView().getItems().collect { it.displayName() }.toList() ==
        ["bob"]
  }

  def "clicking the total chip clears the role filter"() {
    given: "a project shared via users and groups with mixed roles"
    collaborators = [new ProjectCollaborator("u-1", projectId, ProjectRole.READ),
        new ProjectCollaborator("u-2", projectId, ProjectRole.ADMIN)]
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "One", "alice"))
    userInformationService.findById("u-2") >> Optional.of(user("u-2", "Two", "bob"))
    setContext(true)
    clickChip(component.@roleStatChips.get(ProjectRole.ADMIN))

    when:
    clickChip(component.@allStatChip)

    then: "the role filter is cleared and all principals are visible again"
    component.@grid.getListDataView().getItems().size() == 2
  }

  private static void clickChip(com.vaadin.flow.component.html.Div chip) {
    chip.fireEvent(new com.vaadin.flow.component.ClickEvent<>(chip))
  }

  def "an externally applied state filters the grid without rewriting the URL"() {
    given:
    collaborators = [
        new ProjectCollaborator("u-1", projectId, ProjectRole.READ),
        new ProjectCollaborator("u-2", projectId, ProjectRole.ADMIN)]
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "One", "alice"))
    userInformationService.findById("u-2") >> Optional.of(user("u-2", "Two", "bob"))
    setContext(true)

    when:
    component.applyExternalState(new AccessRosterState("alice", AccessFilter.ALL,
        Optional.empty()))

    then:
    component.@grid.getListDataView().getItems().collect { it.displayName() }.toList() ==
        ["alice"]
    and: "the search field reflects the applied search term"
    component.@searchField.getValue() == "alice"
  }

  def "applying an external state with a search term and type filter renders once and consistently"() {
    given: "a project shared with one user and one group"
    collaborators = [new ProjectCollaborator("u-1", projectId, ProjectRole.READ)]
    sharedGroups = [new SharedProjectGroup("g-1", "NGS Lab", null, projectId, ProjectRole.READ)]
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "One", "alice"))
    groupInformationService.findGroupById("g-1") >> Optional.empty()
    setContext(true)

    when: "an external state combines a search term with the groups filter"
    component.applyExternalState(new AccessRosterState("lab", AccessFilter.GROUPS,
        Optional.empty()))

    then: "the grid shows exactly the group matching both conditions"
    component.@grid.getListDataView().getItems().collect { it.displayName() }.toList() ==
        ["NGS Lab"]
    and: "the search field and filter both reflect the applied state"
    component.@searchField.getValue() == "lab"
    component.@filterSelect.getValue() == AccessFilter.GROUPS
  }

  def "applying an external state does not fire the value-change listeners a second time"() {
    given: "a project shared with one user"
    collaborators = [new ProjectCollaborator("u-1", projectId, ProjectRole.READ)]
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "One", "alice"))
    setContext(true)
    def applied = new AccessRosterState("alice", AccessFilter.ALL, Optional.empty())

    when: "an external state is applied"
    component.applyExternalState(applied)

    then: "rosterState is the exact applied instance, not a re-mutated copy"
    // Without the applyingExternalState guard, searchField.setValue(...) would fire the search
    // listener, which replaces rosterState via withSearch(...) — a different reference. Identity
    // therefore proves the guard held.
    component.@rosterState.is(applied)
    and: "the values are what was applied"
    component.@rosterState == new AccessRosterState("alice", AccessFilter.ALL, Optional.empty())
  }

  private static String statChipText(com.vaadin.flow.component.html.Div chip) {
    return chip.getChildren().collect { (it as com.vaadin.flow.component.html.Span).getText() }
        .join(" ")
  }
}
