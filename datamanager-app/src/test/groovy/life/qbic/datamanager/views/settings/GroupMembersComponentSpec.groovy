package life.qbic.datamanager.views.settings

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
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
        "group-1", immutableMembers, GroupRole.OWNER, "acting-user",
        { userId -> new GroupMembersComponent.MemberDisplayInfo(userId, userId) }, { filter, offset, limit -> [] },
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
        "acting-user",
        { userId -> new GroupMembersComponent.MemberDisplayInfo(userId, userId) }, { filter, offset, limit -> [] }, { req -> }, null)

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
        "acting-user",
        { userId -> new GroupMembersComponent.MemberDisplayInfo(userId, userId) }, { filter, offset, limit -> [] }, { req -> }, null)

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
        "acting-user",
        { userId -> new GroupMembersComponent.MemberDisplayInfo(userId, userId) }, { filter, offset, limit -> [] }, { req -> }, null)

    expect: "no management buttons (only the grid's own controls may exist)"
    buttonsOf(component)*.text.intersect(["Add member", "Assign role", "Remove"]).isEmpty()
  }

  def "roster is sorted owner, then managers, then members (each alphabetically by full name)"() {
    given: "an unsorted roster with mixed roles and names"
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("m3", GroupRole.MEMBER),
         new GroupMember("o1", GroupRole.OWNER),
         new GroupMember("m2", GroupRole.MEMBER),
         new GroupMember("mgr2", GroupRole.MANAGER),
         new GroupMember("mgr1", GroupRole.MANAGER),
         new GroupMember("m1", GroupRole.MEMBER)],
        GroupRole.OWNER,
        "acting-user",
        { id -> new GroupMembersComponent.MemberDisplayInfo(
            id.replace("o", "Owner ").replace("mgr", "Manager ").replace("m", "Member "),
            id) },
        { filter, offset, limit -> [] }, { req -> }, null)

    when: "the roster is read from the grid's in-memory data"
    def rosterOrder = component.membersForTest()

    then: "owners first, then managers, then members, each alphabetical by name"
    rosterOrder*.role() == [GroupRole.OWNER,
                            GroupRole.MANAGER, GroupRole.MANAGER,
                            GroupRole.MEMBER, GroupRole.MEMBER, GroupRole.MEMBER]
    rosterOrder[0].userId() == "o1"
    rosterOrder*.userId().take(2)[1] == "mgr1"
    rosterOrder*.userId().take(3)[2] == "mgr2"
    rosterOrder*.userId().drop(3) == ["m1", "m2", "m3"]
  }

  def "the roster renders a dedicated Role column and grows with its rows"() {
    given: "a roster with mixed roles"
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("alice", GroupRole.OWNER), new GroupMember("bob", GroupRole.MANAGER)],
        GroupRole.OWNER,
        "acting-user",
        { id -> new GroupMembersComponent.MemberDisplayInfo("A. Member", "a.member") },
        { filter, offset, limit -> [] }, { req -> }, null)

    when: "the wrapped grid is inspected via the component tree"
    def grid = gridOf(component)

    then: "a dedicated Role column exists (headers Member + Role)"
    grid.columns*.headerText.containsAll(["Member", "Role"])

    and: "the Member column absorbs the remaining space and the Role column is content-sized"
    grid.columns.find { it.headerText == "Role" }.flexGrow == 0
    grid.columns.find { it.headerText == "Member" }.flexGrow == 1

    and: "all rows are rendered at their natural height (no embedded scroll container)"
    grid.element.getProperty("allRowsVisible") == "true"
  }

  def "the roster excludes the acting user and the owner row from selection (governance NFR)"() {
    given: "an owner managing a roster that contains themselves, another owner-less member and a manager"
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("me", GroupRole.MEMBER),
         new GroupMember("bob", GroupRole.MANAGER)],
        GroupRole.OWNER,
        "me",
        { id -> new GroupMembersComponent.MemberDisplayInfo(id, id) },
        { filter, offset, limit -> [] }, { req -> }, null)
    def grid = gridOf(component)

    when: "the selectable provider is queried for each row"
    def aliceSelectable = grid.itemSelectableProvider.test(new GroupMember("alice", GroupRole.OWNER))
    def meSelectable = grid.itemSelectableProvider.test(new GroupMember("me", GroupRole.MEMBER))
    def bobSelectable = grid.itemSelectableProvider.test(new GroupMember("bob", GroupRole.MANAGER))

    then: "the owner row and the acting user's own row are not selectable, others are"
    !aliceSelectable
    !meSelectable
    bobSelectable
  }

  def "a manager cannot select another manager row (governance NFR: managers act on regular members only)"() {
    given: "a manager acting on a roster that contains the owner, another manager and a plain member"
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("me", GroupRole.MANAGER),
         new GroupMember("bob", GroupRole.MANAGER),
         new GroupMember("carol", GroupRole.MEMBER)],
        GroupRole.MANAGER,
        "me",
        { id -> new GroupMembersComponent.MemberDisplayInfo(id, id) },
        { filter, offset, limit -> [] }, { req -> }, null)
    def grid = gridOf(component)

    when: "the selectable provider is queried for each row"
    def aliceSelectable = grid.itemSelectableProvider.test(new GroupMember("alice", GroupRole.OWNER))
    def selfSelectable = grid.itemSelectableProvider.test(new GroupMember("me", GroupRole.MANAGER))
    def bobSelectable = grid.itemSelectableProvider.test(new GroupMember("bob", GroupRole.MANAGER))
    def carolSelectable = grid.itemSelectableProvider.test(new GroupMember("carol", GroupRole.MEMBER))

    then: "the owner, the acting manager's own row and other manager rows are not selectable; plain members are"
    !aliceSelectable
    !selfSelectable
    !bobSelectable
    carolSelectable
  }

  def "an owner can still select a manager row (regression guard)"() {
    given: "an owner managing a roster that contains a manager"
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("bob", GroupRole.MANAGER)],
        GroupRole.OWNER,
        "acting-user",
        { id -> new GroupMembersComponent.MemberDisplayInfo(id, id) },
        { filter, offset, limit -> [] }, { req -> }, null)
    def grid = gridOf(component)

    when: "the selectable provider is queried for the manager row"
    def bobSelectable = grid.itemSelectableProvider.test(new GroupMember("bob", GroupRole.MANAGER))

    then: "the owner may select and act on the manager"
    bobSelectable
  }

  def "select-all that includes protected rows is sanitized before any action is armed"() {
    given: "an owner roster; Vaadin's select-all can select the owner or the acting user's own row"
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("me", GroupRole.MEMBER),
         new GroupMember("carol", GroupRole.MEMBER)],
        GroupRole.OWNER,
        "me",
        { id -> new GroupMembersComponent.MemberDisplayInfo(id, id) },
        { filter, offset, limit -> [] }, { req -> }, null)

    when: "the selection sneaks in protected rows (owner + own row)"
    def protectedInSelection = component.protectedSelection(
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("me", GroupRole.MEMBER)] as Set)

    then: "those protected rows are reported for deselection"
    protectedInSelection*.userId().sort() == ["alice", "me"]

    and: "a selection without protected rows yields nothing to deselect"
    component.protectedSelection([new GroupMember("carol", GroupRole.MEMBER)] as Set).isEmpty()
  }

  def "a MANAGER actor's select-all cannot sneak a peer manager row through"() {
    given: "a manager acting on a roster with a peer manager"
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("me", GroupRole.MANAGER),
         new GroupMember("bob", GroupRole.MANAGER),
         new GroupMember("carol", GroupRole.MEMBER)],
        GroupRole.MANAGER,
        "me",
        { id -> new GroupMembersComponent.MemberDisplayInfo(id, id) },
        { filter, offset, limit -> [] }, { req -> }, null)

    when: "select-all includes the peer manager and a plain member"
    def protectedInSelection = component.protectedSelection(
        [new GroupMember("bob", GroupRole.MANAGER),
         new GroupMember("carol", GroupRole.MEMBER)] as Set)

    then: "the peer manager is flagged for deselection, the plain member is kept"
    protectedInSelection*.userId() == ["bob"]
  }

  def "applyRole over a mixed Manager+Member selection only touches members whose role differs"() {
    given: "a roster with both a manager and a plain member selected"
    def actions = []
    def component = new GroupMembersComponent(
        "group-1",
        [new GroupMember("alice", GroupRole.OWNER),
         new GroupMember("bob", GroupRole.MANAGER),
         new GroupMember("carol", GroupRole.MEMBER)],
        GroupRole.OWNER,
        "acting-user",
        { id -> new GroupMembersComponent.MemberDisplayInfo(id, id) },
        { filter, offset, limit -> [] },
        { req -> actions << [req.userId(), req.action()] }, null)

    when: "the owner demotes the selected members to plain MEMBER"
    component.applyRole(
        [new GroupMember("bob", GroupRole.MANAGER), new GroupMember("carol", GroupRole.MEMBER)],
        GroupRole.MEMBER)

    then: "only the manager is demoted; the already-member is skipped"
    actions == [["bob", GroupMembersComponent.MemberAction.DEMOTE_MANAGER]]

    when: "the owner promotes the selected members to MANAGER"
    actions.clear()
    component.applyRole(
        [new GroupMember("bob", GroupRole.MANAGER), new GroupMember("carol", GroupRole.MEMBER)],
        GroupRole.MANAGER)

    then: "only the plain member is appointed; the already-manager is skipped"
    actions == [["carol", GroupMembersComponent.MemberAction.APPOINT_MANAGER]]
  }

  def "the grid reflects a role change after the component reloads its members"() {
    given: "a component whose reload supplier returns an upgraded roster"
    List<GroupMember> current =
        [new GroupMember("alice", GroupRole.OWNER), new GroupMember("bob", GroupRole.MEMBER)]
    def component = new GroupMembersComponent(
        "group-1", current, GroupRole.OWNER, "acting-user",
        { id -> new GroupMembersComponent.MemberDisplayInfo(id, id) },
        { filter, offset, limit -> [] },
        { req -> },
        { -> current })
    def grid = gridOf(component)

    when: "a role-change operation is performed and the reload supplier returns the new roles"
    current = [new GroupMember("alice", GroupRole.OWNER), new GroupMember("bob", GroupRole.MANAGER)]
    component.performMemberActionForTest("bob")

    then: "the grid reads bob with the updated role from its in-memory data"
    def bob = grid.getListDataView().items.toList().find { it.userId() == "bob" }
    bob.role() == GroupRole.MANAGER
  }

  private static Grid gridOf(GroupMembersComponent component) {
    component.@filterGrid.children.toList()
        .findAll { it instanceof com.vaadin.flow.component.Composite }
        .collectMany { it.children.toList() }
        .findAll { it instanceof com.vaadin.flow.component.html.Div }
        .collectMany { it.children.toList() }
        .find { it instanceof com.vaadin.flow.component.grid.Grid } as com.vaadin.flow.component.grid.Grid
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

