package life.qbic.datamanager.views.projects.project.access

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasText
import com.vaadin.flow.component.UI
import java.util.concurrent.Executor
import life.qbic.identity.api.UserInfo
import life.qbic.identity.api.UserInformationService
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.SharedProjectGroup
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.GrantRequest
import life.qbic.datamanager.views.projects.project.access.ProjectSharingComposer.PrincipalType
import spock.lang.Specification

/**
 * Unit tests for the sharing drawer's roster ordering (FEAT-USER-GROUPS-08).
 *
 * <p>The roster is ordered by role privilege (owner &gt; admin &gt; write &gt; read) and then
 * lexicographically: people by last name, groups by group name. Headless, no Spring context.</p>
 */
class ProjectSharingDrawerSpec extends Specification {

  ProjectAccessService projectAccessService = Mock()
  UserInformationService userInformationService = Mock()
  GroupInformationService groupInformationService = Mock()
  Executor directExecutor = { Runnable runnable -> runnable.run() } as Executor
  ProjectId projectId = ProjectId.create()
  ProjectSharingDrawer drawer

  def setup() {
    UI.setCurrent(Mock(UI))
    drawer = new ProjectSharingDrawer(projectAccessService, userInformationService,
        groupInformationService, directExecutor, projectId, "QVAMP — Vampirism Test project")
  }

  def cleanup() {
    UI.setCurrent(null)
  }

  private static UserInfo user(String id, String fullName, String username) {
    return new UserInfo(id, fullName, "${username}@example.org", username, true, null, null)
  }

  def "orders people by role privilege then last name ascending"() {
    given: "collaborators in an arbitrary order across all roles"
    def collaborators = [
        new ProjectCollaborator("u-read-b", projectId, ProjectRole.READ),
        new ProjectCollaborator("u-owner-z", projectId, ProjectRole.OWNER),
        new ProjectCollaborator("u-admin-a", projectId, ProjectRole.ADMIN),
        new ProjectCollaborator("u-read-a", projectId, ProjectRole.READ),
        new ProjectCollaborator("u-write-m", projectId, ProjectRole.WRITE),
    ]
    userInformationService.findById("u-read-b") >> Optional.of(user("u-read-b", "Bob Zeta", "bzeta"))
    userInformationService.findById("u-owner-z") >> Optional.of(user("u-owner-z", "Zoe Alpha", "zalpha"))
    userInformationService.findById("u-admin-a") >> Optional.of(user("u-admin-a", "Ann Beta", "abeta"))
    userInformationService.findById("u-read-a") >> Optional.of(user("u-read-a", "Al Adams", "aadams"))
    userInformationService.findById("u-write-m") >> Optional.of(user("u-write-m", "Mia Young", "myoung"))

    when:
    def ordered = collaborators.stream().sorted(drawer.collaboratorOrder())
        .map { it.userId() }.toList()

    then: "owner first, then admin, write, and reads ordered by last name"
    ordered == ["u-owner-z", "u-admin-a", "u-write-m", "u-read-a", "u-read-b"]
  }

  def "falls back to the username when a full name is missing"() {
    given:
    def collaborators = [
        new ProjectCollaborator("u-b", projectId, ProjectRole.READ),
        new ProjectCollaborator("u-a", projectId, ProjectRole.READ),
    ]
    userInformationService.findById("u-a") >> Optional.of(user("u-a", null, "aaron"))
    userInformationService.findById("u-b") >> Optional.of(user("u-b", "  ", "zoe"))

    when:
    def ordered = collaborators.stream().sorted(drawer.collaboratorOrder())
        .map { it.userId() }.toList()

    then:
    ordered == ["u-a", "u-b"]
  }

  def "orders groups by role privilege then group name ascending"() {
    given:
    def groups = [
        new SharedProjectGroup("g-1", "Zeta Lab", null, projectId, ProjectRole.READ),
        new SharedProjectGroup("g-2", "Alpha Lab", null, projectId, ProjectRole.ADMIN),
        new SharedProjectGroup("g-3", "Beta Lab", null, projectId, ProjectRole.READ),
    ]

    when:
    def ordered = groups.stream().sorted(drawer.groupOrder())
        .map { it.groupName() }.toList()

    then: "admin first, then reads ordered alphabetically"
    ordered == ["Alpha Lab", "Beta Lab", "Zeta Lab"]
  }

  def "owner role uses a distinct colour from the other elevated roles"() {
    expect:
    ProjectSharingComposer.roleColor(ProjectRole.OWNER) ==
        life.qbic.datamanager.views.general.Tag.TagColor.GOLD
    ProjectSharingComposer.roleColor(ProjectRole.ADMIN) ==
        life.qbic.datamanager.views.general.Tag.TagColor.PRIMARY
    ProjectSharingComposer.roleColor(ProjectRole.WRITE) ==
        life.qbic.datamanager.views.general.Tag.TagColor.PRIMARY
    ProjectSharingComposer.roleColor(ProjectRole.READ) ==
        life.qbic.datamanager.views.general.Tag.TagColor.CONTRAST
  }

  def "describes a failed group grant with the principal, kind and a friendly reason"() {
    given: "a duplicate group grant whose service message is technical"
    def request = new GrantRequest(PrincipalType.GROUP, "g-1", ProjectRole.WRITE, "NGS Lab")
    def duplicate = new life.qbic.application.commons.ApplicationException(
        "Authority GROUP_g-1 already collaborates on ProjectId[abc]. Please change the project role instead")

    when:
    def described = ProjectSharingComposer.describeFailure(request, duplicate)

    then:
    described == "NGS Lab (group): already has access to this project. Change the role instead."
  }

  def "falls back to a generic reason when the cause has no message"() {
    given:
    def request = new GrantRequest(PrincipalType.USER, "u-1", ProjectRole.READ, "jdoe")

    when:
    def described = ProjectSharingComposer.describeFailure(request, new RuntimeException())

    then:
    described == "jdoe (user): access could not be granted."
  }

  def "roster row shows the username and the full name"() {
    given: "a collaborator with a resolvable full name"
    projectAccessService.listCollaborators(projectId) >> [
        new ProjectCollaborator("u-1", projectId, ProjectRole.WRITE)]
    projectAccessService.listSharedGroups(projectId) >> []
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "Jane Doe", "jdoe"))

    when:
    drawer.refresh()

    then:
    def texts = collectText(drawer.@summary)
    texts.contains("jdoe")
    texts.contains("Jane Doe")
  }

  def "roster row exposes the username and full name as a tooltip"() {
    given:
    projectAccessService.listCollaborators(projectId) >> [
        new ProjectCollaborator("u-1", projectId, ProjectRole.READ)]
    projectAccessService.listSharedGroups(projectId) >> []
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "Jane Doe", "jdoe"))

    when:
    drawer.refresh()

    then:
    collectAttribute(drawer.@summary, "title").contains("jdoe (Jane Doe)")
  }

  def "roster row shows the group avatar for a group"() {
    given: "a project shared with one group"
    projectAccessService.listCollaborators(projectId) >> []
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("g-1", "NGS Lab", "sequencing core", projectId,
            ProjectRole.WRITE)]

    when:
    drawer.refresh()

    then: "the group is rendered with its profile avatar, not a generic icon"
    collectComponents(drawer.@summary).any {
      it instanceof life.qbic.datamanager.views.account.UserAvatar
    }
  }

  def "an already-granted principal is marked in the picker instead of being hidden"() {
    given: "a project already shared with one person"
    projectAccessService.listCollaborators(projectId) >> [
        new ProjectCollaborator("u-1", projectId, ProjectRole.WRITE)]
    projectAccessService.listSharedGroups(projectId) >> []
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "Jane Doe", "jdoe"))
    drawer.refresh()

    when: "the picker renders that person as a search result"
    def composer = drawer.@composer
    def option = composer.renderPickerUser(user("u-1", "Jane Doe", "jdoe"))

    then: "the option carries a state marker that names the role instead of being filtered out"
    collectComponents(option).any {
      it.getElement().getClassList().contains("picker-access-state")
    }
    collectText(option).any { it.contains("Has access") && it.contains("editor") }

    and: "the marker is not a neutral pill like the group type badge"
    collectComponents(option).find {
      it.getElement().getClassList().contains("picker-access-state")
    }.getElement().getClassList().contains("success")

    and: "a person without access is rendered without the marker"
    def plain = composer.renderPickerUser(user("u-2", "Ann Other", "aother"))
    !collectComponents(plain).any {
      it.getElement().getClassList().contains("picker-access-state")
    }
  }

  def "the already-granted marker keeps the principal name at normal contrast"() {
    given: "a project already shared with one person"
    projectAccessService.listCollaborators(projectId) >> [
        new ProjectCollaborator("u-1", projectId, ProjectRole.WRITE)]
    projectAccessService.listSharedGroups(projectId) >> []
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "Jane Doe", "jdoe"))
    drawer.refresh()

    when: "an already-granted person is rendered"
    def option = drawer.@composer.renderPickerUser(user("u-1", "Jane Doe", "jdoe"))

    then: "the option is not de-emphasised as a whole — only the marker carries the state"
    !option.getElement().getClassList().contains("tertiary")
    !option.getElement().getClassList().contains("secondary")
  }

  def "the already-granted group marker trails the name row instead of stacking as an extra line"() {
    given: "a project already shared with one group"
    projectAccessService.listCollaborators(projectId) >> []
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("g-1", "NGS Lab", "sequencing core", projectId,
            ProjectRole.ADMIN)]
    drawer.refresh()
    def groupInfo = new life.qbic.usergroups.api.GroupInfo("g-1", "NGS Lab", "sequencing core",
        life.qbic.usergroups.api.GroupType.ORG)

    when:
    def option = drawer.@composer.renderGroupOption(groupInfo)

    then: "the marker names the current role"
    collectText(option).any { it.contains("Has access") && it.contains("manager") }

    and: "it lives inside the name row, not as a sibling line of the option"
    def nameRow = collectComponents(option).find {
      it.getElement().getClassList().contains("group-option-name-row")
    }
    nameRow != null
    collectComponents(nameRow).any {
      it.getElement().getClassList().contains("picker-access-state")
    }
  }

  def "selectable groups are listed before groups that already have access"() {
    given: "one granted and one selectable group, granted first in the source order"
    projectAccessService.listCollaborators(projectId) >> []
    projectAccessService.listSharedGroups(projectId) >> [
        new SharedProjectGroup("g-granted", "AAA Granted Lab", null, projectId,
            ProjectRole.READ)]
    def composer = drawer.@composer
    composer.setAlreadyGranted([], [
        new SharedProjectGroup("g-granted", "AAA Granted Lab", null, projectId,
            ProjectRole.READ)])
    def directory = [
        new life.qbic.usergroups.api.GroupInfo("g-granted", "AAA Granted Lab", null,
            life.qbic.usergroups.api.GroupType.ORG),
        new life.qbic.usergroups.api.GroupInfo("g-free", "ZZZ Free Lab", null,
            life.qbic.usergroups.api.GroupType.ORG)]
    groupInformationService.listPublicDirectory() >> directory

    when: "the picker data provider fetches the first page"
    def query = new com.vaadin.flow.data.provider.Query<>()
    def items = composer.@groupPicker.getDataProvider().fetch(query).toList()

    then: "the selectable group comes first even though it sorts later by name"
    items*.id() == ["g-free", "g-granted"]
  }

  def "selecting an already-granted principal explains the next step instead of staging it"() {
    given: "a project already shared with one person"
    projectAccessService.listCollaborators(projectId) >> [
        new ProjectCollaborator("u-1", projectId, ProjectRole.WRITE)]
    projectAccessService.listSharedGroups(projectId) >> []
    userInformationService.findById("u-1") >> Optional.of(user("u-1", "Jane Doe", "jdoe"))
    drawer.refresh()

    when: "the person is selected in the picker"
    def composer = drawer.@composer
    composer.@personPicker.setValue(user("u-1", "Jane Doe", "jdoe"))

    then: "nothing is staged and an informational note names the role and the alternative"
    composer.@stagedUsers.getComponentCount() == 0
    def texts = collectText(composer.@inlineMessage)
    texts.any { it.contains("already has access") && it.contains("editor") }
    texts.any { it.contains("Change the role") }
    and: "no grant is triggered by merely selecting an existing principal"
    0 * projectAccessService.addCollaborator(*_)
  }

  private static List<String> collectText(Component root) {
    def result = []
    root.children.forEach { child ->
      if (child instanceof HasText) {
        def text = (child as HasText).getText()
        if (text != null && !text.isEmpty()) {
          result << text
        }
      }
      result.addAll(collectText(child))
    }
    return result
  }

  private static List<Component> collectComponents(Component root) {
    def result = []
    root.children.forEach { child ->
      result << child
      result.addAll(collectComponents(child))
    }
    return result
  }

  private static List<String> collectAttribute(Component root, String attribute) {
    def result = []
    root.children.forEach { child ->
      def value = child.element.getAttribute(attribute)
      if (value != null && !value.isEmpty()) {
        result << value
      }
      result.addAll(collectAttribute(child, attribute))
    }
    return result
  }
}
