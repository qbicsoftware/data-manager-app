package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import life.qbic.usergroups.api.GroupRole;
import life.qbic.usergroups.api.GroupType;
import life.qbic.usergroups.api.MyGroupMembership;

/**
 * <b>My Groups list component</b>
 * <p>
 * Renders the group memberships of the current user ("My Groups"). Each membership is shown as a
 * row with the group name, its description (when present), a type badge (Org / User Group) and the
 * caller's role badge (Owner / Manager / Member).
 * <p>
 * Actions are role-gated:
 * <ul>
 *   <li>User Group MEMBER: an enabled "Leave group" action.</li>
 *   <li>User Group MANAGER: management actions (add/remove members, rename/describe) plus an
 *   enabled "Leave group" self-remove action.</li>
 *   <li>User Group OWNER: management actions including appoint-manager and dissolve, no self-remove
 *   (owner-leave/transfer is handled by FEAT-USER-GROUPS-05).</li>
 *   <li>Org groups: no action buttons, membership-only rendering (structural AC 5).</li>
 * </ul>
 * <p>
 * The which-actions-to-show decision is delegated to an injectable
 * {@link ManagementActionPolicy} so the component stays UI-state-free and unit-testable without a
 * Spring or Vaadin {@code UI} context. When the policy grants management actions, the row offers a
 * "Manage group" navigation button that opens the dedicated group detail page
 * ({@link GroupDetailMain}, route {@code settings/groups/:groupId}) where the non-destructive
 * management happens <em>in place</em> instead of in modal dialogs.
 *
 * @since 1.19.0
 */
public class MyGroupsComponent extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = 7426951543168024956L;

  private final Supplier<List<MyGroupMembership>> membershipsSupplier;
  private final Runnable refreshCallback;
  private final Consumer<String> leaveGroupCallback;
  private final LeaveConfirmation leaveConfirmation;
  private final ManagementActionPolicy managementActionPolicy;
  private final Div groupList = new Div();
  private final Span emptyState = new Span("No groups yet.");

  /**
   * Creates a new my-groups list component.
   *
   * @param membershipsSupplier    supplies the caller's current memberships; must not be
   *                               {@code null}
   * @param refreshCallback        re-runs the membership query and re-renders this component; must
   *                               not be {@code null}
   * @param leaveGroupCallback     invoked with the group id when the caller confirms leaving a
   *                               group; must not be {@code null}
   * @param leaveConfirmation      asks the user to confirm leaving a group and runs the provided
   *                               action when confirmed; must not be {@code null}
   * @param managementActionPolicy decides which management actions a row offers based on the group
   *                               type and the caller's role; must not be {@code null}
   */
  public MyGroupsComponent(Supplier<List<MyGroupMembership>> membershipsSupplier,
      Runnable refreshCallback, Consumer<String> leaveGroupCallback,
      LeaveConfirmation leaveConfirmation, ManagementActionPolicy managementActionPolicy) {
    this.membershipsSupplier = Objects.requireNonNull(membershipsSupplier,
        "membershipsSupplier must not be null");
    this.refreshCallback = Objects.requireNonNull(refreshCallback,
        "refreshCallback must not be null");
    this.leaveGroupCallback = Objects.requireNonNull(leaveGroupCallback,
        "leaveGroupCallback must not be null");
    this.leaveConfirmation = Objects.requireNonNull(leaveConfirmation,
        "leaveConfirmation must not be null");
    this.managementActionPolicy = Objects.requireNonNull(managementActionPolicy,
        "managementActionPolicy must not be null");
    addClassName("my-groups-component");
    groupList.addClassName("my-groups-list");
    emptyState.addClassName("my-groups-empty-state");
    groupList.add(emptyState);
    add(groupList);
  }

  /**
   * Re-runs the membership supplier and re-renders all rows. Groups without a membership never
   * appear, dissolved groups do not surface (the backend already filters them).
   */
  public void refresh() {
    groupList.removeAll();
    List<MyGroupMembership> memberships = membershipsSupplier.get();
    if (memberships.isEmpty()) {
      emptyState.setText("No groups yet.");
      groupList.add(emptyState);
      return;
    }
    memberships.forEach(membership -> groupList.add(buildRow(membership)));
  }

  private Div buildRow(MyGroupMembership membership) {
    Div row = new Div();
    row.addClassName("my-groups-row");

    Div identity = new Div();
    identity.addClassName("my-groups-row__identity");

    Span name = new Span(membership.groupName());
    name.addClassName("my-groups-row__name");
    identity.add(name);

    if (membership.groupDescription() != null && !membership.groupDescription().isBlank()) {
      Span description = new Span(membership.groupDescription());
      description.addClassName("my-groups-row__description");
      identity.add(description);
    }

    Div badges = new Div();
    badges.addClassName("my-groups-row__badges");
    badges.add(buildTypeBadge(membership.groupType()));
    badges.add(buildRoleBadge(membership.myRole()));

    Div actions = new Div();
    actions.addClassName("my-groups-row__actions");
    appendActions(actions, membership);

    row.add(identity, badges, actions);
    return row;
  }

  private void appendActions(Div actions, MyGroupMembership membership) {
    if (membership.groupType() == GroupType.ORG) {
      // Org groups: membership-only rendering, no management controls (structural AC 5).
      return;
    }
    List<ManagementAction> actionsForRole = managementActionPolicy.actionsFor(membership);
    if (!actionsForRole.isEmpty()) {
      // Non-destructive management (members, appoint, rename) now opens the group detail page
      // (settings/groups/:groupId) instead of a modal dialog; the row carries a navigation
      // button that jumps to the detail surface of this group.
      Button openDetail = new Button("Manage group");
      openDetail.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      openDetail.addClickListener(click -> navigateToDetail(membership));
      actions.add(openDetail);
      return;
    }

    switch (membership.myRole()) {
      case OWNER -> {
        // no self-remove; owner-leave/transfer is handled by a later story
      }
      case MANAGER, MEMBER -> addLeaveButton(actions, membership.groupId());
    }
  }

  private void navigateToDetail(MyGroupMembership membership) {
    RouteParameters parameters = new RouteParameters(
        new RouteParam(GroupDetailMain.GROUP_ID_ROUTE_PARAMETER, membership.groupId()));
    UI.getCurrent().navigate(GroupDetailMain.class, parameters);
  }

  private void addLeaveButton(Div actions, String groupId) {
    Button leaveButton = new Button("Leave group");
    leaveButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    leaveButton.addClassName("my-groups-action--leave");
    leaveButton.addClickListener(event ->
        leaveConfirmation.confirm(groupId, () -> onLeaveConfirmed(groupId)));
    actions.add(leaveButton);
  }

  private void onLeaveConfirmed(String groupId) {
    leaveGroupCallback.accept(groupId);
    refreshCallback.run();
    fireEvent(new LeaveGroupEvent(this, true, groupId));
  }

  /**
   * Fired after the caller confirmed leaving an ad-hoc group. The leave callback and the refresh
   * have already run when this event is dispatched; the receiver may use it for bookkeeping.
   */
  public static class LeaveGroupEvent extends ComponentEvent<MyGroupsComponent> {

    @Serial
    private static final long serialVersionUID = -6310975212370339700L;

    private final String groupId;

    public LeaveGroupEvent(MyGroupsComponent source, boolean fromClient, String groupId) {
      super(source, fromClient);
      this.groupId = groupId;
    }

    public String groupId() {
      return groupId;
    }
  }

  /**
   * Registers a listener for {@link LeaveGroupEvent}s.
   *
   * @return the registration, usable for removing the listener later
   */
  public Registration addLeaveGroupListener(ComponentEventListener<LeaveGroupEvent> listener) {
    return addListener(LeaveGroupEvent.class, listener);
  }

  /**
   * Asks the user to confirm leaving a group.
   * <p>
   * Implementations show the confirmation UI (e.g. an {@code AlertDialog}) and invoke the
   * {@code onConfirm} action when the user confirms. Kept as a seam so the component remains
   * unit-testable without a Vaadin {@code UI} context.
   */
  @FunctionalInterface
  public interface LeaveConfirmation {

    /**
     * Requests confirmation for leaving the given group.
     *
     * @param groupId   the group the caller wants to leave
     * @param onConfirm the action to run when the user confirms the leave
     */
    void confirm(String groupId, Runnable onConfirm);
  }

  /**
   * A management action a membership row may offer.
   */
  public enum ManagementAction {
    MANAGE_MEMBERS("Manage members"),
    APPOINT_MANAGER("Appoint manager"),
    RENAME("Rename"),
    DISSOLVE("Dissolve");

    private final String label;

    ManagementAction(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  /**
   * Decides which {@link ManagementAction}s a membership row offers, given the group type and the
   * caller's role. The default policy encodes the FEAT-USER-GROUPS-04 role gates:
   * <ul>
   *   <li>OWNER of an ad-hoc group: manage members, appoint manager, rename, dissolve</li>
   *   <li>MANAGER of an ad-hoc group: manage members, rename</li>
   *   <li>all other rows (MEMBER/ORG): no management actions</li>
   * </ul>
   */
  @FunctionalInterface
  public interface ManagementActionPolicy {

    List<ManagementAction> actionsFor(MyGroupMembership membership);

    static ManagementActionPolicy defaultPolicy() {
      return membership -> {
        if (membership.groupType() != GroupType.ADHOC) {
          return List.of();
        }
        return switch (membership.myRole()) {
          case OWNER -> List.of(ManagementAction.MANAGE_MEMBERS,
              ManagementAction.APPOINT_MANAGER, ManagementAction.RENAME,
              ManagementAction.DISSOLVE);
          case MANAGER -> List.of(ManagementAction.MANAGE_MEMBERS, ManagementAction.RENAME);
          case MEMBER -> List.of();
        };
      };
    }
  }

  private static Span buildTypeBadge(GroupType type) {
    Span badge = new Span(type == GroupType.ORG ? "Org" : "User Group");
    badge.addClassName("my-groups-badge");
    badge.addClassName(
        type == GroupType.ORG ? "my-groups-badge--type-org" : "my-groups-badge--type-adhoc");
    return badge;
  }

  private static Span buildRoleBadge(GroupRole role) {
    String roleClass = switch (role) {
      case OWNER, MANAGER -> "my-groups-badge--role-owner";
      case MEMBER -> "my-groups-badge--role-member";
    };
    Span badge = new Span(role == GroupRole.OWNER ? "Owner"
        : role == GroupRole.MANAGER ? "Manager" : "Member");
    badge.addClassName("my-groups-badge");
    badge.addClassName(roleClass);
    return badge;
  }
}