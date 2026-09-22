package life.qbic.datamanager.views.settings

import life.qbic.application.commons.ApplicationException
import life.qbic.application.commons.Result
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupRole
import life.qbic.usergroups.api.GroupType
import life.qbic.usergroups.api.MyGroupMembership
import life.qbic.usergroups.application.GroupService
import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Unit tests for the leave-group flow wiring in {@link MyGroupsMain}.
 *
 * <p>Uses the package-private seam constructor: toasts, user-id resolution and the leave
 * confirmation are injected, so no Vaadin {@code UI} or Spring context is required. Mirrors the
 * {@code NewGroupMainSpec} seam-testing pattern.
 */
class MyGroupsMainSpec extends Specification {

  GroupInformationService groupInformationService = Mock(GroupInformationService)
  GroupService groupService = Mock(GroupService)

  List<String> successToasts = []
  List<String> errorToasts = []
  List<String> userIds = ["user-1"]
  List<String[]> confirmations = []
  MyGroupsMain main

  def setup() {
    Consumer<String> success = { name -> successToasts << name } as Consumer<String>
    Consumer<String> error = { name -> errorToasts << name } as Consumer<String>
    Supplier<String> userId = { -> userIds[0] } as Supplier<String>
    MyGroupsComponent.LeaveConfirmation confirm =
        { String groupId, Runnable onConfirm -> confirmations << [groupId, "asked"]; onConfirm.run() }
            as MyGroupsComponent.LeaveConfirmation
    main = new MyGroupsMain(groupInformationService, groupService, success, error, userId, confirm)
  }

  def "asks for confirmation and removes the membership on confirm, refreshing and toasting success"() {
    given: "the user is a member of a group"
    groupInformationService.listMyGroups("user-1") >> [
        new MyGroupMembership("group-1", "Sprint Team", null, GroupType.ADHOC, GroupRole.MEMBER)]

    when: "the leave flow is triggered"
    main.confirmLeave("group-1")

    then: "the confirmation is asked, the membership is removed, a success toast shows and the list refreshes"
    confirmations.size() == 1
    confirmations[0][0] == "group-1"
    1 * groupService.removeMembership("group-1", "user-1") >> Result.fromValue(null)
    successToasts == ["Sprint Team"]
    errorToasts.isEmpty()
  }

  def "toasts an error and keeps the list unchanged when removal fails"() {
    given: "the user is a member of a group but removal fails"
    groupInformationService.listMyGroups("user-1") >> [
        new MyGroupMembership("group-1", "Sprint Team", null, GroupType.ADHOC, GroupRole.MEMBER)]

    when: "the leave flow is triggered"
    main.confirmLeave("group-1")

    then: "the confirmation is asked, no success toast shows, an error toast shows"
    confirmations.size() == 1
    1 * groupService.removeMembership("group-1", "user-1") >> Result.fromError(
        new ApplicationException("boom"))
    errorToasts == ["Sprint Team"]
    successToasts.isEmpty()
  }

  def "falls back to the group id in the success toast when the group is gone from the list"() {
    given: "removal succeeds but the membership query no longer returns the group"
    groupInformationService.listMyGroups("user-1") >> []

    when: "the leave flow is triggered"
    main.confirmLeave("group-1")

    then: "the toast uses the group id as a fallback name"
    1 * groupService.removeMembership("group-1", "user-1") >> Result.fromValue(null)
    successToasts == ["group-1"]
    errorToasts.isEmpty()
  }
}