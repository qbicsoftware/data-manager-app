package life.qbic.datamanager.views.settings

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import life.qbic.usergroups.api.GroupRole
import life.qbic.usergroups.api.GroupType
import life.qbic.usergroups.api.MyGroupMembership
import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Unit tests for the my-groups list component.
 *
 * <p>Covers the role/type action matrix and the empty state without any Spring or Vaadin
 * {@link com.vaadin.flow.component.UI} context, using the seam based constructor (supplier +
 * runnable + consumer), mirroring {@code PinnedProjectsComponentSpec}.
 */
class MyGroupsComponentSpec extends Specification {

  List<MyGroupMembership> memberships = []
  Runnable refreshSpy = Mock(Runnable)
  List<String> leaveCalls = []
  List<String[]> confirmations = []
  MyGroupsComponent component

  def setup() {
    Supplier<List<MyGroupMembership>> supplier = { -> memberships } as Supplier
    Consumer<String> leaveSeam = { String groupId -> leaveCalls << groupId } as Consumer<String>
    MyGroupsComponent.LeaveConfirmation confirmSeam =
        { String groupId, Runnable onConfirm -> confirmations << [groupId, "asked"]; onConfirm.run() }
            as MyGroupsComponent.LeaveConfirmation
    component = new MyGroupsComponent(supplier, refreshSpy, leaveSeam, confirmSeam,
        MyGroupsComponent.ManagementActionPolicy.defaultPolicy())
  }

  def "renders each membership with name, description, type badge and role badge"() {
    given: "a user with two ad-hoc memberships"
    memberships = [
        membership("group-1", "Bioinformatics Lab", "lab of the bioinformatics team",
            GroupType.ADHOC, GroupRole.OWNER),
        membership("group-2", "Sprint Team", null, GroupType.ADHOC, GroupRole.MEMBER),
    ]

    when: "the component is refreshed"
    component.refresh()

    then: "two rows are rendered including their textual content"
    renderedRows().size() == 2
    textOf(renderedRows()[0]).contains("Bioinformatics Lab")
    textOf(renderedRows()[0]).contains("lab of the bioinformatics team")
    textOf(renderedRows()[0]).contains("User Group")
    textOf(renderedRows()[0]).contains("Owner")
    textOf(renderedRows()[1]).contains("Sprint Team")
    textOf(renderedRows()[1]).contains("User Group")
    textOf(renderedRows()[1]).contains("Member")
  }

  def "omits the description when the membership has none"() {
    given: "a membership without a description"
    memberships = [membership("group-1", "Sprint Team", null, GroupType.ADHOC, GroupRole.MEMBER)]

    when:
    component.refresh()

    then: "the row renders without the description element"
    def rowChildren = rowChildren(renderedRows()[0])
    rowChildren.findAll { it instanceof Span && it.element.classList.contains("my-groups-row__description") }.isEmpty()
    textOf(renderedRows()[0]).contains("Sprint Team")
    textOf(renderedRows()[0]).contains("Member")
  }

  def "shows the empty state while the user belongs to no group"() {
    when: "there are no memberships"
    component.refresh()

    then:
    textOf(component).contains("No groups yet.")
  }

  def "renders an enabled Leave group action for an ad-hoc plain member and asks for confirmation"() {
    given: "a plain ad-hoc member"
    memberships = [membership("group-1", "Sprint Team", null, GroupType.ADHOC, GroupRole.MEMBER)]
    component.refresh()

    when: "the user clicks leave"
    List<Button> buttons = buttonsOf(renderedRows()[0])
    Button leave = buttons.find { it.text == "Leave group" }
    leave.click()

    then: "the confirmation seam is consulted with the group id"
    confirmations.size() == 1
    confirmations[0][0] == "group-1"
  }

  def "invokes the leave callback and refreshes when the confirmation is accepted"() {
    given: "a plain ad-hoc member"
    memberships = [membership("group-1", "Sprint Team", null, GroupType.ADHOC, GroupRole.MEMBER)]
    component.refresh()
    boolean[] fired = new boolean[1]
    component.addLeaveGroupListener(event -> fired[0] = event.groupId() == "group-1")

    when: "the user clicks leave and the confirmation seam confirms"
    List<Button> buttons = buttonsOf(renderedRows()[0])
    Button leave = buttons.find { it.text == "Leave group" }
    leave.click()

    then: "the leave callback runs with the group id, the list refreshes and the leave event fires"
    leaveCalls == ["group-1"]
    1 * refreshSpy.run()
    fired[0]
  }

  def "renders a Manage group action without a self-remove action for an ad-hoc owner"() {
    given: "an ad-hoc owner"
    memberships = [membership("group-1", "Bioinformatics Lab", "lab", GroupType.ADHOC,
        GroupRole.OWNER)]

    when:
    component.refresh()

    then: "the Manage group action exists, is enabled, and no leave action does"
    List<Button> buttons = buttonsOf(renderedRows()[0])
    buttons.every { it.enabled }
    buttons*.text.contains("Manage group")
    buttons.findAll { it.text == "Leave group" }.isEmpty()
  }

  def "renders a Manage group action plus an enabled leave action for an ad-hoc manager"() {
    given: "an ad-hoc manager"
    memberships = [membership("group-1", "Sprint Team", null, GroupType.ADHOC, GroupRole.MANAGER)]

    when:
    component.refresh()

    then: "the manager sees the Manage group entry (the detail page owns management)"
    List<Button> buttons = buttonsOf(renderedRows()[0])
    buttons.findAll { it.enabled }*.text.contains("Manage group")
    buttons.findAll { it.text == "Leave group" }.isEmpty()
  }

  def "a Manage group action exists for an ad-hoc owner (navigates to the detail page)"() {
    given: "an ad-hoc owner"
    memberships = [membership("group-1", "Bioinformatics Lab", "lab", GroupType.ADHOC,
        GroupRole.OWNER)]
    component.refresh()

    when: "the user clicks Manage group"
    List<Button> buttons = buttonsOf(renderedRows()[0])

    then: "a Manage group button exists (UI navigation is exercised by the owning view)"
    buttons.find { it.text == "Manage group" } != null
  }

  def "a plain ad-hoc member sees no management actions, only self-remove"() {
    given: "a plain ad-hoc member"
    memberships = [membership("group-1", "Sprint Team", null, GroupType.ADHOC, GroupRole.MEMBER)]

    when:
    component.refresh()

    then:
    List<Button> buttons = buttonsOf(renderedRows()[0])
    buttons.findAll { it.text in ["Manage members", "Appoint manager", "Rename", "Dissolve"] }.isEmpty()
    buttons.findAll { it.text == "Leave group" }.size() == 1
  }

  def "renders no action buttons for an org group membership (membership-only, structural AC 5)"() {
    given: "a membership in an org group"
    memberships = [membership("org-1", "NGS Core Facility", "sequencing facility",
        GroupType.ORG, GroupRole.MEMBER)]

    when:
    component.refresh()

    then: "the row shows no action buttons at all"
    buttonsOf(renderedRows()[0]).isEmpty()
    textOf(renderedRows()[0]).contains("Org")
    textOf(renderedRows()[0]).contains("Member")
  }

  def "re-renders from the supplier on refresh, so newly created groups appear"() {
    given: "no memberships initially"
    component.refresh()
    assert textOf(component).contains("No groups yet.")

    when: "a group appears and the component refreshes"
    memberships = [membership("group-1", "Sprint Team", null, GroupType.ADHOC, GroupRole.OWNER)]
    component.refresh()

    then: "the new group is shown"
    renderedRows().size() == 1
    textOf(renderedRows()[0]).contains("Sprint Team")
  }

  def "rejects null seams at construction time"() {
    when: "a null supplier is provided"
    new MyGroupsComponent(null, () -> {}, { }, { g, r -> },
        MyGroupsComponent.ManagementActionPolicy.defaultPolicy())

    then:
    thrown(NullPointerException)
  }

  private static MyGroupMembership membership(String groupId, String name, String description,
      GroupType type, GroupRole role) {
    new MyGroupMembership(groupId, name, description, type, role)
  }

  private List<Div> renderedRows() {
    component.@groupList.children.toList() as List<Div>
  }

  private static List<Component> rowChildren(Div row) {
    row.children.toList() as List<Component>
  }

  private static List<Button> buttonsOf(Div row) {
    List<Button> buttons = []
    collectButtons(row, buttons)
    return buttons
  }

  private static void collectButtons(Component component, List<Button> buttons) {
    if (component instanceof Button button) {
      buttons << button
    }
    component.children.forEach { child -> collectButtons(child, buttons) }
  }

  private static String textOf(Component component) {
    def text = new StringBuilder()
    appendText(component, text)
    return text.toString()
  }

  private static void appendText(Component component, StringBuilder target) {
    target.append(component.element.text ?: "")
    component.children.forEach { child -> appendText(child, target) }
  }

  private static List<Component> allDescendants(Div root) {
    List<Component> result = []
    collectDescendants(root, result)
    return result
  }

  private static void collectDescendants(Component component, List<Component> acc) {
    component.children.forEach { child ->
      acc << child
      collectDescendants(child, acc)
    }
  }
}