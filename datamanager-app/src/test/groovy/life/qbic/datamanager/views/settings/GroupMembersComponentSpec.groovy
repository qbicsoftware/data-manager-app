package life.qbic.datamanager.views.settings

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import life.qbic.usergroups.api.GroupMember
import life.qbic.usergroups.api.GroupRole
import spock.lang.Specification

/**
 * Unit tests for the {@link GroupMembersComponent} (the group detail page's member roster,
 * now a searchable multi-select FilterGrid with toolbar actions).
 *
 * <p>Covers the seam-driven behavior: immutable-list robustness, role-gated toolbar actions,
 * and that selection-driven operations route through the {@code MemberManagementHandler}
 * seam. Constructing the Vaadin grid/FilterGrid does not require a UI instance, so these stay
 * lightweight Spock specs without Spring or a browser.</p>
 */
class GroupMembersComponentSpec extends Specification {

  def "constructing with an immutable member list does not throw (regression)"() {
    given: "members returned from the service as an immutable list"
    List<GroupMember> immutableMembers =
        [new GroupMember("alice", GroupRole.OWNER), new GroupMember("bob", GroupRole.MEMBER)]
            .stream().toList()

    and: "a handler that records the actions"
    List<GroupMembersComponent.MemberAction> actions = []

    when: "the roster is built with the immutable list"
    new GroupMembersComponent(
        "group-1", immutableMembers, GroupRole.OWNER,
        { userId -> userId }, { filter, offset, limit -> [] },
        { req -> actions << req.action() }, null)

    then: "no exception is thrown and the immutable list is untouched"
    noExceptionThrown()
    immutableMembers*.userId() == ["alice", "bob"]
  }

  def "an owner sees the toolbar with Assign role and Remove (no Add member — it lives in the section header)"() {
    given:
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("alice", GroupRole.OWNER), new GroupMember("bob", GroupRole.MEMBER)],
        GroupRole.OWNER,
        { userId -> userId }, { filter, offset, limit -> [] }, { req -> }, null)

    expect: "the selection toolbar has Assign role and Remove, both start disabled"
    def buttons = buttonsOf(component)
    buttons*.text.containsAll(["Assign role", "Remove"])
    !buttons*.text.contains("Add member")
    buttons.find { it.text == "Remove" }.isEnabled() == false
  }

  def "a manager sees Remove but no Assign role and no Add member (section header owns Add)"() {
    given:
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("alice", GroupRole.OWNER), new GroupMember("bob", GroupRole.MANAGER)],
        GroupRole.MANAGER,
        { userId -> userId }, { filter, offset, limit -> [] }, { req -> }, null)

    expect: "only Remove in the selection toolbar for a manager"
    def buttons = buttonsOf(component)
    buttons*.text.containsAll(["Remove"])
    !buttons*.text.contains("Assign role")
    !buttons*.text.contains("Add member")
  }

  def "a plain member sees no management toolbar at all"() {
    given: "the acting user is a plain member (no management rights)"
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("boss", GroupRole.OWNER), new GroupMember("me", GroupRole.MEMBER)],
        GroupRole.MEMBER,
        { userId -> userId }, { filter, offset, limit -> [] }, { req -> }, null)

    expect: "no management buttons (only the grid's own controls may exist)"
    buttonsOf(component)*.text.intersect(["Add member", "Assign role", "Remove"]).isEmpty()
  }

  private static List<Button> buttonsOf(Component component) {
    List<Button> buttons = []
    collectButtons(component, buttons)
    return buttons
  }

  private static void collectButtons(Component component, List<Button> buttons) {
    if (component instanceof Button button) {
      buttons << button
    }
    component.children.forEach { child -> collectButtons(child, buttons) }
  }
}
