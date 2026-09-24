package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import life.qbic.datamanager.views.general.grid.component.FilterGrid;
import life.qbic.datamanager.views.general.grid.component.FilterGridConfigurations;
import life.qbic.datamanager.views.general.grid.component.GridConfiguration.FilterTester;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.settings.GroupMembersComponent.MemberAction;
import life.qbic.identity.api.UserInfo;
import life.qbic.usergroups.api.GroupMember;
import life.qbic.usergroups.api.GroupRole;

/**
 * <b>Group members component</b>
 * <p>
 * Renders the member roster of one group with the owner/manager member-management controls
 * directly on the group detail page — <em>not</em> in a modal dialog (FEAT-USER-GROUPS-04,
 * cognitive-load decision).
 * <p>
 * The roster is a {@link FilterGrid}: a searchable, multi-select table with a toolbar of primary
 * actions for the currently selected members (matching the measurements/raw data views):
 * <ul>
 *   <li><b>Add member</b> — opens the user search (non-member candidates).</li>
 *   <li><b>Assign role</b> — promotes selected members to MANAGER or demotes them back to MEMBER
 *   (owner only for these role changes).</li>
 *   <li><b>Remove</b> — removes the selected members.</li>
 * </ul>
 * <p>
 * The table is laid out as a <b>Member</b> column (full name + platform user name) plus a
 * dedicated <b>Role</b> column so every member's role lines up vertically. All rows are rendered
 * at their natural height (no embedded scrollbar): the roster size equals the number of members,
 * so the native page scroll handles overflow (USER-NFR-01).
 * <p>
 * The component is seam-driven: the actual service calls and the refresh happen behind the
 * {@link MemberManagementHandler} seam, and the add-member search runs through the
 * {@link MemberSearch} seam honoring the Vaadin data-provider paging contract. This keeps the
 * component unit-testable without a Spring or Vaadin {@code UI} context.
 * <p>
 * The acting user's role gates which toolbar actions appear (MANAGER sees no owner-only role
 * assignment), matching the application layer's role gates.
 *
 * @since 1.20.0
 */
public final class GroupMembersComponent extends Div {

  @Serial
  private static final long serialVersionUID = -2012222222222222223L;

  private final String groupId;
  private final List<GroupMember> members;
  private final GroupRole actingUserRole;
  private final String actingUserId;
  private final Function<String, MemberDisplayInfo> memberInfoResolver;
  private final MemberSearch memberSearch;
  private final MemberManagementHandler handler;
  private final Supplier<List<GroupMember>> reloadMembers;

  private FilterGrid<GroupMember, String> filterGrid;
  private Button assignRoleButton;
  private Button removeButton;

  /**
   * Creates the roster component.
   *
   * @param groupId             the group being managed; must not be {@code null}
   * @param members             the current members of the group (a snapshot; never mutated)
   * @param actingUserRole      the acting user's role inside the group; must not be {@code null}
   * @param actingUserId        the acting user's own user id — used to render the acting user's
   *                            row (and any owner row) as non-actionable; must not be
   *                            {@code null}
   * @param memberInfoResolver  maps a user id to the member's display info (full name + user
   *                            name); must not be {@code null}
   * @param memberSearch        returns users who could be added (not yet members); must honor the
   *                            given offset/limit paging contract
   * @param handler             handles a concrete member op and refreshes; must not be {@code null}
   * @param reloadMembers       reloads the members after a mutation; may be {@code null}
   */
  public GroupMembersComponent(String groupId, List<GroupMember> members,
      GroupRole actingUserRole, String actingUserId,
      Function<String, MemberDisplayInfo> memberInfoResolver,
      MemberSearch memberSearch, MemberManagementHandler handler,
      Supplier<List<GroupMember>> reloadMembers) {
    this.groupId = requireNonNull(groupId, "groupId must not be null");
    // The passed-in roster is the current snapshot; copy it so we never mutate the caller's
    // (possibly immutable) list. reloadMembers is only used to refresh after a mutation.
    this.members = new ArrayList<>(requireNonNull(members, "members must not be null"));
    this.actingUserRole = Objects.requireNonNull(actingUserRole,
        "actingUserRole must not be null");
    this.actingUserId = requireNonNull(actingUserId, "actingUserId must not be null");
    this.memberInfoResolver = requireNonNull(memberInfoResolver,
        "memberInfoResolver must not be null");
    this.memberSearch = memberSearch;
    this.handler = requireNonNull(handler, "handler must not be null");
    this.reloadMembers = reloadMembers;
    addClassName("group-members-component");
    build();
  }

  /**
   * Applies one member operation to the given users (single or selection).
   */
  private void perform(MemberAction action, List<String> userIds) {
    for (String userId : userIds) {
      handler.handle(new GroupMembersUpdatedRequest(groupId, userId, action));
    }
    refreshMembersFromSupplier();
    fireEvent(new GroupMembersUpdatedEvent(this, true,
        new GroupMembersUpdatedRequest(groupId, null, action)));
  }

  private void refreshMembersFromSupplier() {
    if (reloadMembers != null) {
      List<GroupMember> fresh = reloadMembers.get();
      members.clear();
      members.addAll(fresh);
    }
    // Re-sort after a reload so the hierarchy (owner → managers → members) is restored even
    // when a reload returns an unsorted roster.
    sortMembers();
    if (filterGrid != null) {
      filterGrid.refreshAll();
      filterGrid.deselectAll();
      updateToolbarForSelection();
    }
  }

  private void build() {
    // Hierarchy order: owner first, then managers, then members — each group ordered
    // lexicographically by the resolved full name (fallback: user id).
    sortMembers();

    FilterTester<GroupMember, String> filterTester = (member, searchTerm) -> {
      if (searchTerm == null || searchTerm.isBlank()) {
        return true;
      }
      String lower = searchTerm.toLowerCase();
      MemberDisplayInfo info = memberInfoResolver.apply(member.userId());
      if (info != null) {
        String full = info.fullName() == null ? "" : info.fullName();
        String handle = info.userName() == null ? "" : info.userName();
        if (full.toLowerCase().contains(lower) || handle.toLowerCase().contains(lower)) {
          return true;
        }
      }
      return member.userId().toLowerCase().contains(lower);
    };
    // The grid's in-memory data provider is wired to this component's own roster list: reloads
    // (clear + addAll) mutate the very collection the provider reads, so role changes and
    // add/remove operations are reflected in the grid immediately after a refresh — no page
    // reload needed. (The list itself is already a copy of the caller's snapshot, see ctor.)
    var configuredGrid = FilterGridConfigurations.<GroupMember, String>inMemory(members,
        filterTester);

    Grid<GroupMember> grid = new Grid<>();
    grid.setSelectionMode(Grid.SelectionMode.MULTI);
    // The roster is a small, member-only list: render all rows at their natural height so the
    // native page scroll handles overflow (USER-NFR-01) instead of an embedded grid scrollbar.
    grid.setAllRowsVisible(true);
    // Group-governance NFR: the owner can never be demoted or removed by anyone from the roster,
    // and nobody may act on themselves (a manager may not remove the owner; an owner manages
    // self-removal via the governed transfer/dissolve flow, FEAT-USER-GROUPS-05). These rows are
    // marked non-selectable (checkbox disabled) so actions can never target them; as a belt-and-
    // braces measure any such row that still ends up selected (Vaadin's header "select all" does
    // NOT consult the item-selectable provider) is immediately deselected again, see
    // {@link #updateToolbarForSelection()}.
    grid.setItemSelectableProvider(this::isActionable);
    // Rows that cannot be acted on receive a part name so they can be visually quieted.
    grid.setPartNameGenerator(
        member -> isActionable(member) ? null : "protected-row");

    // Member column: full name (fallback: user name, then user id), the user name in
    // parentheses with weaker contrast (like the account overview header).
    grid.addColumn(new ComponentRenderer<>(member -> {
      MemberDisplayInfo info = memberInfoResolver.apply(member.userId());
      String fullName = info == null ? null : info.fullName();
      String userName = info == null ? null : info.userName();
      Div cell = new Div();
      cell.addClassName("group-member-cell");
      String primary = !isBlank(fullName) ? fullName
          : (!isBlank(userName) ? userName : member.userId());
      Span name = new Span(primary);
      name.addClassName("group-member-cell__name");
      cell.add(name);
      if (isBlank(fullName) && !isBlank(userName) && !userName.equals(member.userId())) {
        // no full name; the user name is already shown as primary, nothing extra to add
      } else if (!isBlank(userName) && !userName.equals(primary)) {
        Span userNameSpan = new Span("(" + userName + ")");
        userNameSpan.addClassNames("text-s", "text-secondary");
        cell.add(userNameSpan);
      }
      return cell;
    })).setHeader("Member").setAutoWidth(true).setFlexGrow(1);

    // Role column: its own column so the members' roles line up vertically (own the alignment
    // instead of mixing the badge into the Member cell). Content-sized: the role badges line up
    // directly under the Role heading, while the Member column absorbs the remaining space.
    grid.addColumn(new ComponentRenderer<>(member -> {
      Span role = new Span(roleLabel(member.role()));
      role.addClassName("my-groups-badge");
      role.addClassName(member.role() == GroupRole.MEMBER
          ? "my-groups-badge--role-member" : "my-groups-badge--role-owner");
      return role;
    })).setHeader("Role").setAutoWidth(true).setFlexGrow(0);

    grid.setMultiSort(true);

    filterGrid = FilterGrid.create(
        GroupMember.class,
        String.class,
        configuredGrid.applyConfiguration(grid),
        () -> "",
        (searchTerm, oldFilter) -> searchTerm);
    filterGrid.searchFieldPlaceholder("Search members");
    filterGrid.itemDisplayLabel("member");
    // The column set is fixed (Member + role badge); a Show/Hide Columns menu would only
    // add clutter here.
    filterGrid.setShowHideColumnsMenuVisible(false);
    filterGrid.addSelectionListener(e -> updateToolbarForSelection());

    boolean canManageMembers = actingUserRole == GroupRole.OWNER
        || actingUserRole == GroupRole.MANAGER;
    boolean isOwner = actingUserRole == GroupRole.OWNER;

    if (isOwner) {
      assignRoleButton = new Button("Assign role");
      assignRoleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      assignRoleButton.addClickListener(click -> openAssignRoleDialog());
      assignRoleButton.setEnabled(false);
    }

    removeButton = new Button("Remove");
    removeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
    removeButton.addClickListener(click -> removeSelectedMembers());
    removeButton.setEnabled(false);
    removeButton.setVisible(canManageMembers);

    if (assignRoleButton != null) {
      filterGrid.setSecondaryActionGroup(assignRoleButton, removeButton);
    } else if (canManageMembers) {
      filterGrid.setSecondaryActionGroup(removeButton);
    }

    add(filterGrid);
  }

  private void updateToolbarForSelection() {
    Set<GroupMember> selected = filterGrid.selectedElements();
    // Vaadin's header "select all" selects every row including non-selectable ones, so ensure
    // no protected row stays selected (a selected non-actionable row would arm the toolbar
    // buttons for an operation the domain must reject anyway).
    List<GroupMember> protectedSelected = protectedSelection(selected);
    if (!protectedSelected.isEmpty()) {
      filterGrid.deselect(protectedSelected);
      selected = filterGrid.selectedElements();
    }
    boolean hasSelection = !selected.isEmpty();
    if (removeButton != null) {
      removeButton.setEnabled(hasSelection);
    }
    if (assignRoleButton != null) {
      assignRoleButton.setEnabled(hasSelection);
    }
  }

  /**
   * The subset of the given selection that must never stay selected: rows that are not
   * actionable (self/owner, and peer-manager rows for a MANAGER actor). Vaadin's header
   * "select all" does not honour the item-selectable provider, so these can sneak into the
   * selection; they are removed again (and never acted upon).
   *
   * @param selected the current selection
   * @return the non-actionable rows among the selection; empty if none
   */
  List<GroupMember> protectedSelection(Set<GroupMember> selected) {
    return selected.stream()
        .filter(m -> !isActionable(m))
        .toList();
  }

  /**
   * Opens the "Add member" dialog (user search for non-member candidates). Called from the
   * page's Members section header button — the add action is decoupled from the roster-selection
   * toolbar.
   */
  public void openAddMemberDialog() {
    if (memberSearch == null) {
      return;
    }
    ComboBox<UserInfo> picker = new ComboBox<>("Select a user");
    picker.setPlaceholder("Search for username or full name");
    picker.setItemLabelGenerator(UserInfo::platformUserName);
    picker.setRenderer(new ComponentRenderer<>(candidate -> {
      Div div = new Div();
      div.setText(candidate.fullName() == null || candidate.fullName().isBlank()
          ? candidate.platformUserName()
          : candidate.fullName() + " (" + candidate.platformUserName() + ")");
      return div;
    }));
    picker.setItems(query -> memberSearch.search(
            query.getFilter().orElse(null), query.getOffset(), query.getLimit())
        .stream());

    AppDialog dialog = AppDialog.small();
    DialogHeader.with(dialog, "Add member");
    picker.setWidthFull();
    DialogBody.withoutUserInput(dialog, picker);
    DialogFooter.with(dialog, "Cancel", "Add member");
    dialog.registerConfirmAction(() -> {
      UserInfo selected = picker.getValue();
      if (selected != null) {
        perform(MemberAction.ADD_MEMBER, List.of(selected.id()));
        dialog.close();
      }
      // no selection: keep the dialog open so the user can search again
    });
    dialog.registerCancelAction(dialog::close);
    dialog.open();
  }

  private void openAssignRoleDialog() {
    Set<GroupMember> selected = filterGrid.selectedElements();
    if (selected.isEmpty()) {
      return;
    }
    // Only members that are not OWNER can have their role changed.
    List<GroupMember> changeable = selected.stream()
        .filter(m -> m.role() != GroupRole.OWNER)
        .toList();
    if (changeable.isEmpty()) {
      // owner-only selection: nothing to do
      filterGrid.deselectAll();
      return;
    }
    // Always ask for the target role explicitly: assigning a role is a deliberate action and
    // the user must choose Manager or Member for the selected members (no implicit toggle).
    // The confirm button carries an explicit action label that reflects the chosen target role.
    AppDialog dialog = AppDialog.small();
    DialogHeader.with(dialog, "Assign role");

    RadioButtonGroup<GroupRole> rolePicker = new RadioButtonGroup<>();
    rolePicker.setLabel("Role for " + changeable.size() + " selected member(s)");
    rolePicker.setItems(GroupRole.MANAGER, GroupRole.MEMBER);
    rolePicker.setItemLabelGenerator(role -> role == GroupRole.MANAGER ? "Manager" : "Member");
    rolePicker.setValue(GroupRole.MANAGER);
    DialogBody.withoutUserInput(dialog, rolePicker);

    DialogFooter.with(dialog, "Cancel", "Assign Manager role");
    rolePicker.addValueChangeListener(event -> {
      if (rolePicker.getValue() == GroupRole.MANAGER) {
        DialogFooter.with(dialog, "Cancel", "Assign Manager role");
      } else {
        DialogFooter.with(dialog, "Cancel", "Assign Member role");
      }
    });
    dialog.registerConfirmAction(() -> {
      applyRole(changeable, rolePicker.getValue());
      dialog.close();
    });
    dialog.registerCancelAction(dialog::close);
    dialog.open();
  }

  /**
   * Applies the chosen target role to the selected members.
   * <p>
   * Only members whose current role differs from the target are touched: promoting to MANAGER
   * only appoints plain MEMBERs, demoting to MEMBER only demotes current MANAGERs. Members already
   * holding the target role are skipped. This makes the operation robust against a mixed
   * selection (e.g. selecting both a MEMBER and a MANAGER and assigning one target role) and
   * never submits a role-changing command the application layer cannot perform.
   */
  void applyRole(List<GroupMember> changeable, GroupRole target) {
    for (GroupMember m : changeable) {
      if (m.role() == target) {
        // already holds the target role — nothing to do
        continue;
      }
      MemberAction action = target == GroupRole.MANAGER
          ? MemberAction.APPOINT_MANAGER : MemberAction.DEMOTE_MANAGER;
      perform(action, List.of(m.userId()));
    }
  }

  private void removeSelectedMembers() {
    Set<GroupMember> selected = filterGrid.selectedElements();
    if (selected.isEmpty()) {
      return;
    }
    // confirm for the destructive multi-remove
    AlertDialog.danger(this,
        "Remove selected members?",
        "Are you sure you want to remove the selected member(s) from this group?",
        "Remove",
        "Keep",
        () -> perform(MemberAction.REMOVE_MEMBER,
            selected.stream().map(GroupMember::userId).toList()))
        .open();
  }

  private static String roleLabel(GroupRole role) {
    return role == GroupRole.OWNER ? "Owner"
        : role == GroupRole.MANAGER ? "Manager" : "Member";
  }

  /**
   * Sorts the roster by hierarchy: owner first, then managers, then members. Within each role
   * group members are ordered lexicographically by their resolved full name (fallback: user id),
   * so the list always reads as a clear role hierarchy with people sorted alphabetically.
   */
  private void sortMembers() {
    members.sort(Comparator
        .comparingInt((GroupMember m) -> roleRank(m.role()))
        .thenComparing(this::sortKeyFor));
  }

  private int roleRank(GroupRole role) {
    return switch (role) {
      case OWNER -> 0;
      case MANAGER -> 1;
      case MEMBER -> 2;
    };
  }

  /**
   * Whether a roster row may be acted on (selected for Assign role / Remove).
   * <p>
   * A row is <em>not</em> actionable when it is the acting user themselves or the group owner:
   * the domain refuses self-demotion/self-removal and the removal/demotion of an owner
   * (FEAT-USER-GROUPS-04/05 governance), so the UI must not even offer the attempt. A
   * MANAGER may only add/remove regular members (GROUP-R-03) and has no authority over other
   * managers, so a MANAGER acting user cannot select a peer MANAGER row either — only the
   * OWNER may act on managers.
   */
  private boolean isActionable(GroupMember member) {
    return !member.userId().equals(actingUserId)
        && member.role() != GroupRole.OWNER
        && !(actingUserRole == GroupRole.MANAGER && member.role() == GroupRole.MANAGER);
  }

  private String sortKeyFor(GroupMember member) {
    MemberDisplayInfo info = memberInfoResolver.apply(member.userId());
    String fullName = info == null ? null : info.fullName();
    String userName = info == null ? null : info.userName();
    String key = !isBlank(fullName) ? fullName
        : (!isBlank(userName) ? userName : member.userId());
    return key.toLowerCase();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * Display information of a group member: the full name (primary) and the platform user name
   * (shown in parentheses with weaker contrast, like the account overview header).
   */
  public record MemberDisplayInfo(String fullName, String userName) {

    public MemberDisplayInfo {
      // keep nulls: callers fall back to the user id when nothing is available
    }
  }

  /**
   * Package-private test seam: performs a real member operation against the handler and refreshes
   * the roster — the exact production path a dialog confirm triggers.
   */
  void performMemberActionForTest(String userId) {
    perform(MemberAction.APPOINT_MANAGER, List.of(userId));
  }

  /**
   * Package-private test seam: performs a real REMOVE_MEMBER operation against the handler and
   * refreshes — mirrors what {@link #removeSelectedMembers()} does on confirm.
   */
  void performRemoveForTest(List<String> userIds) {
    perform(MemberAction.REMOVE_MEMBER, userIds);
  }

  /**
   * Package-private test seam: returns the roster in its current display (sorted) order.
   */
  List<GroupMember> membersForTest() {
    return List.copyOf(members);
  }

  /**
   * Registers a listener invoked whenever a member operation is performed (add/remove/
   * assign role). The owning page uses it to toast + refresh as needed.
   *
   * @param listener the listener
   * @return the registration
   */
  public Registration addMembersUpdatedListener(
      ComponentEventListener<GroupMembersUpdatedEvent> listener) {
    return addListener(GroupMembersUpdatedEvent.class, listener);
  }

  /**
   * Callback handler for a completed member-management operation. Receives the request and
   * performs the service call + refresh; may throw to signal a failure the page should surface.
   */
  @FunctionalInterface
  public interface MemberManagementHandler {

    void handle(GroupMembersUpdatedRequest request);
  }

  /**
   * A completed member-management request carrying the affected group and member.
   */
  public static class GroupMembersUpdatedRequest {

    private final String groupId;
    private final String userId;
    private final MemberAction action;

    public GroupMembersUpdatedRequest(String groupId, String userId, MemberAction action) {
      this.groupId = groupId;
      this.userId = userId;
      this.action = action;
    }

    public String groupId() {
      return groupId;
    }

    public String userId() {
      return userId;
    }

    public MemberAction action() {
      return action;
    }
  }

  /**
   * The typed member-management operation.
   */
  public enum MemberAction {
    ADD_MEMBER,
    REMOVE_MEMBER,
    APPOINT_MANAGER,
    DEMOTE_MANAGER
  }

  /**
   * Search callback for the "add member" combobox honoring the Vaadin data-provider paging
   * contract (the fetch callback must read the query bounds).
   */
  @FunctionalInterface
  public interface MemberSearch {

    List<UserInfo> search(String filter, int offset, int limit);
  }

  /**
   * Event fired after a member operation succeeded so the owning page can toast and refresh.
   */
  public static class GroupMembersUpdatedEvent extends ComponentEvent<GroupMembersComponent> {

    @Serial
    private static final long serialVersionUID = -2012222222222222224L;

    private final GroupMembersUpdatedRequest request;

    public GroupMembersUpdatedEvent(GroupMembersComponent source, boolean fromClient,
        GroupMembersUpdatedRequest request) {
      super(source, fromClient);
      this.request = request;
    }

    public GroupMembersUpdatedRequest request() {
      return request;
    }
  }
}