package life.qbic.datamanager.views.settings

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import life.qbic.identity.api.UserInfo
import life.qbic.usergroups.api.GroupMember
import life.qbic.usergroups.api.GroupRole
import spock.lang.Specification

/**
 * Regression tests for the {@link GroupManagementDialogs.ManageMembersBody}.
 *
 * <p>Covers the fix for the {@code UnsupportedOperationException} thrown when the dialog was
 * constructed with an immutable member list (the application service returns the roster via
 * {@code Stream.toList()}, which is immutable): the body must copy the roster into a mutable
 * list instead of clearing the caller's list.</p>
 */
class ManageMembersBodySpec extends Specification {

  def "constructing with an immutable member list does not throw (regression)"() {
    given: "members returned from the service as an immutable list"
    List<GroupMember> immutableMembers =
        [new GroupMember("alice", GroupRole.OWNER), new GroupMember("bob", GroupRole.MEMBER)]
            .stream().toList()

    and: "a handler that records the requests"
    List<GroupManagementDialogs.ManageAction> actions = []

    when: "the manage-members body is built with the immutable list"
    new GroupManagementDialogs.ManageMembersBody(
        "group-1", immutableMembers, GroupRole.OWNER,
        { userId -> userId }, { filter, offset, limit -> [] }, { request -> actions << request.action() }, null)

    then: "no exception is thrown and the immutable list is untouched"
    noExceptionThrown()
    immutableMembers*.userId() == ["alice", "bob"]
  }

  def "constructing with an immutable member list renders the roster"() {
    given:
    List<GroupMember> immutableMembers =
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("bob", GroupRole.MEMBER),
         new GroupMember("carol", GroupRole.MANAGER)].stream().toList()

    when:
    def body = new GroupManagementDialogs.ManageMembersBody(
        "group-1", immutableMembers, GroupRole.OWNER,
        { userId -> userId }, { filter, offset, limit -> [] }, { req -> }, null)

    then: "the roster contains one row per member"
    def roster = firstWithClass(body, "manage-members-roster")
    def rows = childrenOfClass(roster, "manage-members-row")
    rows.size() == 3
  }

  def "owner sees appoint and remove actions for members, remove for managers, and none for the owner"() {
    given:
    List<GroupMember> immutableMembers =
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("bob", GroupRole.MEMBER),
         new GroupMember("carol", GroupRole.MANAGER)].stream().toList()

    when:
    def body = new GroupManagementDialogs.ManageMembersBody(
        "group-1", immutableMembers, GroupRole.OWNER,
        { userId -> userId }, { filter, offset, limit -> [] }, { req -> }, null)
    def roster = firstWithClass(body, "manage-members-roster")
    def rows = childrenOfClass(roster, "manage-members-row")

    then: "the owner row has no action buttons"
    buttonsOf(rows[0]).isEmpty()

    and: "the member row has Make manager and Remove"
    buttonsOf(rows[1])*.text.containsAll(["Make manager", "Remove"])

    and: "the manager row has Remove as manager and Remove"
    buttonsOf(rows[2])*.text.containsAll(["Remove as manager", "Remove"])
  }

  def "a manager-only viewer sees no member mutation actions"() {
    given:
    List<GroupMember> immutableMembers =
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("bob", GroupRole.MANAGER)].stream().toList()

    when:
    def body = new GroupManagementDialogs.ManageMembersBody(
        "group-1", immutableMembers, GroupRole.MANAGER,
        { userId -> userId }, { filter, offset, limit -> [] }, { req -> }, null)
    def roster = firstWithClass(body, "manage-members-roster")
    def rows = childrenOfClass(roster, "manage-members-row")

    then: "the manager sees the roster with no mutation buttons"
    rows.size() == 2
    rows.every { row -> buttonsOf(row).isEmpty() }
  }

  def "clicking Remove on a member row performs the removal request with the member id"() {
    given: "an immutable roster with a member and a reload supplier"
    List<GroupMember> immutableMembers =
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("bob", GroupRole.MEMBER)].stream().toList()
    def requests = []
    def reloadCounter = 0

    when: "the owner clicks Remove on bob"
    def body = new GroupManagementDialogs.ManageMembersBody(
        "group-1", immutableMembers, GroupRole.OWNER,
        { userId -> userId }, { filter, offset, limit -> [] },
        { req -> requests << [req.action(), req.userId()] },
        { reloadCounter++; [new GroupMember("alice", GroupRole.OWNER)] })
    def roster = firstWithClass(body, "manage-members-roster")
    def rows = childrenOfClass(roster, "manage-members-row")
    buttonsOf(rows[1]).find { it.text == "Remove" }.click()

    then: "the removal request is handled and the roster reloads"
    requests == [[GroupManagementDialogs.ManageAction.REMOVE_MEMBER, "bob"]]
    // one reload at construction (hydration) + one after the mutation
    reloadCounter == 2
  }

  private static Component firstWithClass(Component root, String className) {
    if (root.element.classList.contains(className)) {
      return root
    }
    Component result = null
    root.children.forEach { child ->
      if (result == null) {
        result = firstWithClass(child, className)
      }
    }
    return result
  }

  private static List<Component> childrenOfClass(Component root, String className) {
    List<Component> result = []
    root.children.forEach { child ->
      if (child.element.classList.contains(className)) {
        result << child
      }
      result.addAll(childrenOfClass(child, className))
    }
    return result
  }

  private static List<Button> buttonsOf(Component component) {
    List<Button> result = []
    if (component instanceof Button button) {
      result << button
    }
    component.children.forEach { child -> result.addAll(buttonsOf(child)) }
    return result
  }
}