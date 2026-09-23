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
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import life.qbic.datamanager.views.general.grid.component.FilterGrid;
import life.qbic.datamanager.views.general.grid.component.FilterGridConfigurations;
import life.qbic.datamanager.views.general.grid.component.GridConfiguration.FilterTester;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
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
  private final Function<String, String> memberNameResolver;
  private final MemberSearch memberSearch;
  private final MemberManagementHandler handler;
  private final Supplier<List<GroupMember>> reloadMembers;

  private FilterGrid<GroupMember, String> filterGrid;
  private Button assignRoleButton;
  private Button removeButton;

  /**
   * Creates the roster component.
   *
   * @param groupId            the group being managed; must not be {@code null}
   * @param members            the current members of the group (a snapshot; never mutated)
   * @param actingUserRole     the acting user's role inside the group; must not be {@code null}
   * @param memberNameResolver maps a user id to a display name for the roster
   * @param memberSearch       returns users who could be added (not yet members); must honor the
   *                           given offset/limit paging contract
   * @param handler            handles a concrete member op and refreshes; must not be {@code null}
   * @param reloadMembers      reloads the members after a mutation; may be {@code null}
   */
  public GroupMembersComponent(String groupId, List<GroupMember> members,
      GroupRole actingUserRole, Function<String, String> memberNameResolver,
      MemberSearch memberSearch, MemberManagementHandler handler,
      Supplier<List<GroupMember>> reloadMembers) {
    this.groupId = requireNonNull(groupId, "groupId must not be null");
    // The passed-in roster is the current snapshot; copy it so we never mutate the caller's
    // (possibly immutable) list. reloadMembers is only used to refresh after a mutation.
    this.members = new ArrayList<>(requireNonNull(members, "members must not be null"));
    this.actingUserRole = Objects.requireNonNull(actingUserRole,
        "actingUserRole must not be null");
    this.memberNameResolver = memberNameResolver;
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
    if (filterGrid != null) {
      filterGrid.refreshAll();
      filterGrid.deselectAll();
      updateToolbarForSelection();
    }
  }

  private void build() {
    FilterTester<GroupMember, String> filterTester = (member, searchTerm) -> {
      if (searchTerm == null || searchTerm.isBlank()) {
        return true;
      }
      String lower = searchTerm.toLowerCase();
      String displayName = memberNameResolver == null ? null
          : memberNameResolver.apply(member.userId());
      if (displayName != null && displayName.toLowerCase().contains(lower)) {
        return true;
      }
      return member.userId().toLowerCase().contains(lower);
    };
    var configuredGrid = FilterGridConfigurations.<GroupMember, String>inMemory(
        new ArrayList<>(members), filterTester);

    Grid<GroupMember> grid = new Grid<>();
    grid.setSelectionMode(Grid.SelectionMode.MULTI);
    // Member column: display name (fallback: user id) with a role badge next to it
    grid.addColumn(new ComponentRenderer<>(member -> {
      String displayName = memberNameResolver == null ? null
          : memberNameResolver.apply(member.userId());
      Div cell = new Div();
      cell.addClassName("group-member-cell");
      Span name = new Span(displayName == null || displayName.isBlank()
          ? member.userId() : displayName);
      name.addClassName("group-member-cell__name");
      cell.add(name);
      Span role = new Span(roleLabel(member.role()));
      role.addClassName("group-members-role");
      role.addClassName("group-members-role--" + member.role().name().toLowerCase());
      cell.add(role);
      return cell;
    })).setHeader("Member").setAutoWidth(true).setFlexGrow(1);

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
    boolean hasSelection = !selected.isEmpty();
    if (removeButton != null) {
      removeButton.setEnabled(hasSelection);
    }
    if (assignRoleButton != null) {
      assignRoleButton.setEnabled(hasSelection);
    }
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
    ComboBox<UserInfo> picker = new ComboBox<>("Add member");
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
    com.vaadin.flow.component.dialog.Dialog dialog =
        new com.vaadin.flow.component.dialog.Dialog();
    dialog.setHeaderTitle("Add member");
    dialog.add(picker);
    var addButton = new com.vaadin.flow.component.button.Button("Add",
        click -> {
          UserInfo selected = picker.getValue();
          if (selected != null) {
            perform(MemberAction.ADD_MEMBER, List.of(selected.id()));
            dialog.close();
          }
        });
    addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    dialog.getFooter().add(addButton);
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
    com.vaadin.flow.component.dialog.Dialog dialog =
        new com.vaadin.flow.component.dialog.Dialog();
    dialog.setHeaderTitle("Assign role");
    com.vaadin.flow.component.radiobutton.RadioButtonGroup<GroupRole> rolePicker =
        new com.vaadin.flow.component.radiobutton.RadioButtonGroup<>();
    rolePicker.setLabel("Role for " + changeable.size() + " selected member(s)");
    rolePicker.setItems(GroupRole.MANAGER, GroupRole.MEMBER);
    rolePicker.setItemLabelGenerator(role -> role == GroupRole.MANAGER ? "Manager" : "Member");
    rolePicker.setValue(GroupRole.MANAGER);
    dialog.add(rolePicker);
    var applyButton = new com.vaadin.flow.component.button.Button("Apply",
        click -> {
          applyRole(changeable, rolePicker.getValue());
          dialog.close();
        });
    applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    dialog.getFooter().add(applyButton);
    dialog.open();
  }

  private void applyRole(List<GroupMember> changeable, GroupRole target) {
    for (GroupMember m : changeable) {
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
            selected.stream().map(GroupMember::userId).toList()));
  }

  private static String roleLabel(GroupRole role) {
    return role == GroupRole.OWNER ? "Owner"
        : role == GroupRole.MANAGER ? "Manager" : "Member";
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