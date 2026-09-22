package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import life.qbic.usergroups.api.GroupRole;
import life.qbic.usergroups.api.GroupType;
import life.qbic.usergroups.api.MyGroupMembership;

/**
 * <b>My Groups list component</b>
 * <p>
 * Renders the group memberships of the current user ("My Groups"). Each membership is shown as a
 * row with the group name, its description (when present), a type badge (Org / Ad-hoc) and the
 * caller's role badge (Owner / Manager / Member).
 * <p>
 * Actions are role-gated:
 * <ul>
 *   <li>Ad-hoc MEMBER: an enabled "Leave group" action firing a {@link LeaveGroupEvent}.</li>
 *   <li>Ad-hoc OWNER and MANAGER: management actions rendered as <em>disabled</em> stubs with a
 *   tooltip pointing to the upcoming management update (FEAT-USER-GROUPS-04). Disabled buttons do
 *   not fire events in Vaadin, so they are wrapped in a {@link Span} carrying the {@link Tooltip}.</li>
 *   <li>Org groups: no action buttons, membership-only rendering (structural AC 5).</li>
 * </ul>
 * <p>
 * The component is intentionally free of UI-specific state and services: memberships are provided
 * through a {@link Supplier}, re-rendering through a {@link Runnable} refresh callback and leaving
 * a group through a {@link Consumer} seam. This keeps it unit-testable without a Spring or Vaadin
 * {@code UI} context, mirroring the {@code PinnedProjectsComponent} pattern.
 *
 * @since 1.19.0
 */
public class MyGroupsComponent extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = 7426951543168024956L;

  private static final String DISABLED_MANAGEMENT_TOOLTIP =
      "Available in an upcoming update — FEAT-USER-GROUPS-04";

  private final Supplier<List<MyGroupMembership>> membershipsSupplier;
  private final Runnable refreshCallback;
  private final Consumer<String> leaveGroupCallback;
  private final Div groupList = new Div();
  private final Span emptyState = new Span("No groups yet.");

  /**
   * Creates a new my-groups list component.
   *
   * @param membershipsSupplier supplies the caller's current memberships; must not be
   *                            {@code null}
   * @param refreshCallback     re-runs the membership query and re-renders this component; must
   *                            not be {@code null}
   * @param leaveGroupCallback  invoked with the group id when the caller wants to leave a group;
   *                            must not be {@code null}
   */
  public MyGroupsComponent(Supplier<List<MyGroupMembership>> membershipsSupplier,
      Runnable refreshCallback, Consumer<String> leaveGroupCallback) {
    this.membershipsSupplier = java.util.Objects.requireNonNull(membershipsSupplier,
        "membershipsSupplier must not be null");
    this.refreshCallback = java.util.Objects.requireNonNull(refreshCallback,
        "refreshCallback must not be null");
    this.leaveGroupCallback = java.util.Objects.requireNonNull(leaveGroupCallback,
        "leaveGroupCallback must not be null");
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
    switch (membership.myRole()) {
      case OWNER, MANAGER -> addDisabledManagementStubs(actions);
      case MEMBER -> addLeaveButton(actions, membership.groupId());
    }
  }

  private void addDisabledManagementStubs(Div actions) {
    for (String label : List.of("Manage members", "Appoint manager", "Rename", "Dissolve")) {
      Button disabledButton = new Button(label);
      disabledButton.setEnabled(false);
      disabledButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      disabledButton.addClassName("my-groups-action--disabled");

      Span wrapper = new Span(disabledButton);
      wrapper.addClassName("my-groups-action-wrapper");
      Tooltip.forComponent(wrapper).setText(DISABLED_MANAGEMENT_TOOLTIP);
      actions.add(wrapper);
    }
  }

  private void addLeaveButton(Div actions, String groupId) {
    Button leaveButton = new Button("Leave group");
    leaveButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    leaveButton.addClassName("my-groups-action--leave");
    leaveButton.addClickListener(
        event -> fireEvent(new LeaveGroupEvent(this, event.isFromClient(), groupId)));
    actions.add(leaveButton);
  }

  /**
   * Fired when the caller triggers the "Leave group" action on an ad-hoc group they are a plain
   * member of. The receiver (e.g. the hosting view) is responsible for showing a confirmation and
   * invoking {@code GroupService.removeMembership}.
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

  private static Span buildTypeBadge(GroupType type) {
    return new Span(type == GroupType.ORG ? "Org" : "Ad-hoc");
  }

  private static Span buildRoleBadge(GroupRole role) {
    return switch (role) {
      case OWNER -> new Span("Owner");
      case MANAGER -> new Span("Manager");
      case MEMBER -> new Span("Member");
    };
  }
}