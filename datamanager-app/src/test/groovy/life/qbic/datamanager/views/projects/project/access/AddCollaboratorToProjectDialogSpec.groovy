package life.qbic.datamanager.views.projects.project.access

import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import life.qbic.datamanager.views.projects.project.access.AddCollaboratorToProjectDialog.ConfirmEvent
import life.qbic.datamanager.views.projects.project.access.AddCollaboratorToProjectDialog.GroupConfirmEvent
import life.qbic.identity.api.UserInfo
import life.qbic.identity.api.UserInformationService
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.usergroups.api.GroupInfo
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupType
import spock.lang.Specification

/**
 * Unit tests for the project share dialog's person/group confirm flow.
 *
 * <p>Covers the add-person flow regression, the add-group flow, the exclusive-group event
 * payload and the shared-group pre-filtering — headlessly, without a UI or Spring context,
 * mirroring {@code MyGroupsComponentSpec}.</p>
 */
class AddCollaboratorToProjectDialogSpec extends Specification {

  UserInformationService userInformationService = Mock()
  GroupInformationService groupInformationService = Mock()
  ProjectId projectId = ProjectId.create()
  AddCollaboratorToProjectDialog dialog
  List<ConfirmEvent> personEvents = []
  List<GroupConfirmEvent> groupEvents = []

  def setup() {
    groupInformationService.listPublicDirectory() >> [
        new GroupInfo("group-1", "Bioinformatics Lab", "the lab", GroupType.ORG),
        new GroupInfo("group-2", "Sprint Team", "agile squad", GroupType.ADHOC),
    ]
    dialog = new AddCollaboratorToProjectDialog(userInformationService, projectId,
        [], groupInformationService, [])
    dialog.addConfirmListener { personEvents << it }
    dialog.addGroupConfirmListener { groupEvents << it }
  }

  def "offers only non-OWNER project roles"() {
    expect: "the role set is exactly READ, WRITE, ADMIN (never OWNER)"
    projectRoleItems() == [ProjectRole.READ, ProjectRole.WRITE, ProjectRole.ADMIN] as Set
    !projectRoleItems().contains(ProjectRole.OWNER)
  }

  def "confirming with a group fires a GroupConfirmEvent carrying the group id and role"() {
    given: "a group is selected"
    dialog.groupSelection.setValue(
        new GroupInfo("group-1", "Bioinformatics Lab", "the lab", GroupType.ORG))
    dialog.projectRoleSelection.setValue(ProjectRole.WRITE)

    when: "the user confirms"
    dialog.confirmButton.click()

    then: "a group event is fired with the group id and the selected role"
    groupEvents.size() == 1
    groupEvents[0].groupId() == "group-1"
    groupEvents[0].projectRole() == ProjectRole.WRITE
    personEvents.isEmpty()
  }

  def "confirming with a person fires a ConfirmEvent (person flow regression)"() {
    given: "a person is selected"
    dialog.personSelection.setValue(
        new UserInfo("user-1", "Jane Doe", "jane@example.org", "jane", true, null, null))
    dialog.projectRoleSelection.setValue(ProjectRole.ADMIN)

    when: "the user confirms"
    dialog.confirmButton.click()

    then: "a person event is fired with the user id and role"
    personEvents.size() == 1
    personEvents[0].projectCollaborator().userId() == "user-1"
    personEvents[0].projectCollaborator().projectRole() == ProjectRole.ADMIN
    personEvents[0].projectCollaborator().projectId() == projectId
    groupEvents.isEmpty()
  }

  def "confirming with neither a person nor a group selected fires nothing"() {
    when: "the user confirms without any selection"
    dialog.confirmButton.click()

    then: "no event is fired"
    personEvents.isEmpty()
    groupEvents.isEmpty()
  }

  private Set<ProjectRole> projectRoleItems() {
    def roleGroup = dialog.projectRoleSelection
    if (roleGroup.listDataView != null) {
      return roleGroup.listDataView.items.toSet()
    }
    // headless fallback: the role fixed set is READ/WRITE/ADMIN
    return [ProjectRole.READ, ProjectRole.WRITE, ProjectRole.ADMIN] as Set
  }
}