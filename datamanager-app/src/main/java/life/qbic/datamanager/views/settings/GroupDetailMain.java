package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import java.util.Locale;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.InlineEditableField;
import life.qbic.datamanager.views.general.InlineEditableField.InputKind;
import life.qbic.datamanager.views.general.InlineEditableField.SaveEvent;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.TypeToConfirmInput;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.settings.GroupMembersComponent.GroupMembersUpdatedRequest;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupManagementService;
import life.qbic.usergroups.api.GroupMember;
import life.qbic.usergroups.api.GroupRole;
import life.qbic.usergroups.api.GroupType;
import life.qbic.usergroups.api.MyGroupMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Group detail page</b>
 * <p>
 * A dedicated, shareable settings route (<code>settings/groups/:groupId</code>) showing one
 * group's details and its member-management surface <em>in place</em> instead of in a modal
 * dialog (FEAT-USER-GROUPS-04 cognitive-load decision).
 * <p>
 * The page follows the Profile settings convention: a flat {@link SettingsSection} made of
 * {@code settings-group} blocks separated by subheadings and whitespace (no card chrome), with
 * the group name and description as inline-editable rows ({@link InlineEditableField}) and the
 * member roster in a group of its own. The destructive dissolve operation is guarded by an
 * inline type-to-confirm form that expands in place inside the danger zone (the owner must
 * retype the exact group name); non-destructive modifications happen on the page.
 * <p>
 * Back navigation reuses the settings navigation concept: a text link "My Groups" in the
 * section header points back to the list ({@link MyGroupsMain}), matching the aside links.
 * <p>
 * Access control: the page resolves the group id from the route, verifies the caller is a
 * member of the group, and reroutes to the not-found page otherwise (so no membership
 * information leaks to non-members).
 *
 * @since 1.20.0
 */
@Route(value = AppRoutes.GroupsRoutes.GROUP_DETAIL, layout = SettingsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Settings · Group")
public class GroupDetailMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = -2002222222222222222L;

  public static final String GROUP_ID_ROUTE_PARAMETER = "groupId";

  private final transient GroupInformationService groupInformationService;
  private final transient GroupManagementService groupManagementService;
  private final transient UserInformationService userInformationService;
  private final transient MessageSourceNotificationFactory messageFactory;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  private transient MyGroupMembership membership;
  private transient SettingsSection section;
  private transient InlineEditableField nameField;
  private transient InlineEditableField descriptionField;
  private transient RouterLink backLink;
  private transient Div membersGroup;
  private transient H3 membersHeading;

  /**
   * Production constructor: wires the seams to the real services, toasts and navigation.
   */
  public GroupDetailMain(
      @Autowired GroupInformationService groupInformationService,
      @Autowired GroupManagementService groupManagementService,
      @Autowired UserInformationService userInformationService,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired MessageSourceNotificationFactory messageFactory) {
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.groupManagementService = requireNonNull(groupManagementService,
        "groupManagementService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
    this.messageFactory = requireNonNull(messageFactory,
        "messageFactory must not be null");
    addClassName("group-detail");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (section != null) {
      remove(section);
      section = null;
    }
    // The breadcrumb is rebuilt on every enter; remove the previous instance so navigating
    // from one group detail to another never accumulates duplicate links.
    if (backLink != null) {
      remove(backLink);
      backLink = null;
    }
    membership = null;
    nameField = null;
    descriptionField = null;
    membersGroup = null;
    membersHeading = null;
    String groupId = event.getRouteParameters().get(GROUP_ID_ROUTE_PARAMETER).orElseThrow();
    String actingUserId = currentUserId();

    // Resolve the caller's membership first: the detail page must never leak membership
    // information to non-members (visibility policy). A dissolved/unknown group or a
    // non-member caller ends on the not-found page, exactly like the project detail pages.
    membership = groupInformationService.listMyGroups(actingUserId).stream()
        .filter(m -> m.groupId().equals(groupId))
        .findFirst()
        .orElseThrow(() -> {
          event.rerouteToError(NotFoundException.class);
          return null;
        });

    buildPage(groupId, actingUserId);
  }

  private void buildPage(String groupId, String actingUserId) {
    GroupRole myRole = membership.myRole();
    boolean canManageProfile = myRole == GroupRole.OWNER || myRole == GroupRole.MANAGER;

    // Back navigation as a settings-style breadcrumb link (matches the aside links), placed
    // directly above the section so its purpose (return to the My Groups list) is obvious.
    backLink = new RouterLink("My Groups", MyGroupsMain.class);
    backLink.addClassName("group-detail-back-link");
    add(backLink);

    section = new SettingsSection(membership.groupName(),
        "Manage this group and its members.");

    // ── Group profile group (inline editable, Profile concept) ──────────
    Div profileGroup = settingsGroup("Group");
    nameField = new InlineEditableField("Group name", membership.groupName());
    nameField.setEditable(canManageProfile);
    nameField.setMinDisplayWidth(28); // generous width: group names read comfortably
    nameField.addSaveListener(this::onNameSave);
    nameField.addCancelListener(e -> { /* UI revert is handled by the component */ });
    profileGroup.add(nameField);

    descriptionField = new InlineEditableField(InputKind.TEXTAREA, "Description",
        membership.groupDescription() == null ? "" : membership.groupDescription());
    descriptionField.setEditable(canManageProfile);
    descriptionField.setMinDisplayWidth(40); // even wider: descriptions are long texts
    descriptionField.addSaveListener(this::onDescriptionSave);
    descriptionField.addCancelListener(e -> { /* UI revert is handled by the component */ });
    profileGroup.add(descriptionField);
    section.addContent(profileGroup);

    // ── Members group (roster in place) ─────────────────────────────────
    List<GroupMember> members = groupManagementService.listMembers(groupId, actingUserId);
    membersGroup = newMembersGroup(members.size());
    GroupMembersComponent membersComponent = new GroupMembersComponent(
        groupId, members, myRole, actingUserId, this::displayNameFor, this::searchAddableUsers,
        this::handleMemberRequest, () -> groupManagementService.listMembers(groupId, actingUserId));
    if (myRole == GroupRole.OWNER || myRole == GroupRole.MANAGER) {
      var addMemberButton = new com.vaadin.flow.component.button.Button("Add member",
          new com.vaadin.flow.component.icon.Icon(
              com.vaadin.flow.component.icon.VaadinIcon.PLUS));
      addMemberButton.addClassName("primary");
      addMemberButton.addClickListener(click -> membersComponent.openAddMemberDialog());
      addGroupAction(membersGroup, addMemberButton);
    }
    membersComponent.addMembersUpdatedListener(this::onMembersUpdated);
    membersComponent.addClassName("group-members-in-settings-group");
    membersGroup.add(membersComponent);
    section.addContent(membersGroup);

    // ── Danger zone group (owner-only, destructive → inline type-to-confirm) ──
    // Dissolving a group is irreversible, so the action is guarded by an inline
    // type-to-confirm form that expands in place (expand-on-click, GitHub/GitLab-style)
    // instead of a modal: the owner must retype the exact group name to unlock the
    // destructive button while keeping the blast radius (roster, member count) visible.
    // Org groups are admin-governed (no OWNER membership) and never expose a dissolve
    // affordance; only ad-hoc owners can dissolve their group.
    if (myRole == GroupRole.OWNER && membership.groupType() == GroupType.ADHOC) {
      Div dangerGroup = settingsGroup("Danger zone");

      // Explainer: always visible above the trigger/confirm actions, so the scope of the
      // destructive operation stays in sight even while the type-to-confirm guard is open.
      Span dissolveNote = new Span(
          "Permanently dissolves this group, removes all members and revokes access to "
              + "projects shared with it. This cannot be undone.");
      dissolveNote.addClassName("group-detail-danger-note");
      dangerGroup.add(dissolveNote);

      // Calm state: the dissolve trigger. The guard form is hidden until the owner
      // actively engages with the trigger; the note above stays visible regardless.
      Div dangerRow = new Div();
      dangerRow.addClassName("group-detail-danger-zone");
      Button dissolveButton = new Button("Dissolve Group", new Icon(VaadinIcon.WARNING));
      dissolveButton.addClassName("button-danger");
      dangerRow.add(dissolveButton);
      dangerGroup.add(dangerRow);

      // Guard state: replaces the calm row while engaged. The destructive button stays
      // locked until the retyped name exactly matches the (live) group name; the server
      // re-checks on click regardless (defense in depth, never trust the client-side
      // button state). The expected name is resolved lazily: an inline rename on this page
      // must not stale the guard against the group's current name.
      TypeToConfirmInput confirmInput = new TypeToConfirmInput(
          () -> membership.groupName());
      confirmInput.addClassNames("group-detail-dissolve-confirm-input", "width-full");
      com.vaadin.flow.component.textfield.TextField confirmField = confirmInput.textField();
      confirmField.addClassNames("width-full");
      Button cancelButton = new Button("Keep group");
      Button confirmButton = new Button("Dissolve group");
      confirmButton.addClassName("button-danger");
      confirmButton.setEnabled(false); // locked until the typed name matches

      Div confirmButtons = new Div();
      confirmButtons.addClassNames("flex-horizontal", "gap-02");
      confirmButtons.add(cancelButton, confirmButton);

      Div confirmSection = new Div();
      confirmSection.addClassNames("group-detail-dissolve-confirm", "flex-vertical",
          "gap-03", "width-full");
      confirmSection.setVisible(false);
      confirmSection.add(confirmInput, confirmButtons);
      dangerGroup.add(confirmSection);

      dissolveButton.addClickListener(click -> {
        dangerRow.setVisible(false);
        confirmField.clear();
        confirmButton.setEnabled(false);
        confirmSection.setVisible(true);
        confirmField.focus();
      });
      cancelButton.addClickListener(click -> {
        confirmSection.setVisible(false);
        confirmField.clear();
        dangerRow.setVisible(true);
      });
      confirmField.addValueChangeListener(event ->
          confirmButton.setEnabled(confirmInput.validate().hasPassed()));
      confirmButton.addClickListener(click -> {
        // Re-validate server-side: never trigger the irreversible operation based on the
        // client-side button state alone.
        if (confirmInput.validate().hasPassed()) {
          dissolveGroup(groupId, actingUserId, membership.groupName());
        }
      });

      section.addContent(dangerGroup);
    }

    add(section);
  }

  private void onNameSave(SaveEvent event) {
    String newName = event.value().trim();
    String actingUserId = currentUserId();
    if (newName.isEmpty()) {
      event.getSource().setError("A group name is required.");
      return;
    }
    if (newName.equals(membership.groupName())) {
      event.getSource().cancelEditing();
      return;
    }
    try {
      groupManagementService.renameGroup(membership.groupId(), actingUserId, newName);
      refreshMembership(membership.groupId(), actingUserId);
      event.getSource().setValue(newName);
      event.getSource().cancelEditing();
      // keep the section heading in sync with the new name
      if (section != null) {
        section.setTitle(newName);
      }
      toast("user-groups.manage.success",
          new Object[]{newName}, Locale.getDefault());
    } catch (RuntimeException error) {
      event.getSource().setError("Could not rename the group. "
          + "A group with this name may already exist (names are unique).");
    }
  }

  private void onDescriptionSave(SaveEvent event) {
    String newDescription = event.value().trim();
    String actingUserId = currentUserId();
    String old = membership.groupDescription() == null ? "" : membership.groupDescription();
    if (newDescription.equals(old)) {
      event.getSource().cancelEditing();
      return;
    }
    try {
      groupManagementService.updateDescription(membership.groupId(), actingUserId, newDescription);
      refreshMembership(membership.groupId(), actingUserId);
      event.getSource().setValue(newDescription);
      event.getSource().cancelEditing();
      toast("user-groups.manage.success",
          new Object[]{membership.groupName()}, Locale.getDefault());
    } catch (RuntimeException error) {
      event.getSource().setError("Could not update the group description.");
    }
  }

  private void onMembersUpdated(GroupMembersComponent.GroupMembersUpdatedEvent event) {
    // keep the Members section heading count in sync with the roster after add/remove
    if (membersHeading != null) {
      int freshCount = groupManagementService.listMembers(membership.groupId(),
          currentUserId()).size();
      membersHeading.setText("Members (" + freshCount + ")");
    }
    toast("user-groups.manage.success",
        new Object[]{membership.groupName()}, Locale.getDefault());
  }

  private void handleMemberRequest(GroupMembersUpdatedRequest request) {
    String actingUserId = currentUserId();
    switch (request.action()) {
      case ADD_MEMBER -> groupManagementService.addMember(request.groupId(), actingUserId,
          request.userId());
      case REMOVE_MEMBER -> groupManagementService.removeMember(request.groupId(), actingUserId,
          request.userId());
      case APPOINT_MANAGER -> groupManagementService.appointManager(request.groupId(),
          actingUserId, request.userId());
      case DEMOTE_MANAGER -> groupManagementService.demoteManager(request.groupId(),
          actingUserId, request.userId());
    }
  }

  /**
   * Executes the irreversible dissolve and reacts to the outcome. On success the dismissal
   * toast is shown and the owner returns to the My Groups list. On failure an error toast is
   * shown and the page remains; the guard form stays open so the owner can retry.
   */
  private void dissolveGroup(String groupId, String actingUserId, String groupName) {
    try {
      groupManagementService.dissolveGroup(groupId, actingUserId);
      toast("user-groups.dissolve.success",
          new Object[]{groupName}, Locale.getDefault());
      getUI().ifPresent(ui -> ui.navigate(MyGroupsMain.class));
    } catch (RuntimeException error) {
      toast("user-groups.dissolve.error",
          new Object[]{groupName}, Locale.getDefault());
    }
  }

  private void refreshMembership(String groupId, String actingUserId) {
    groupInformationService.listMyGroups(actingUserId).stream()
        .filter(m -> m.groupId().equals(groupId))
        .findFirst()
        .ifPresent(updated -> membership = updated);
  }

  private String currentUserId() {
    Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();
    return userIdTranslator.translateToUserId(authentication).orElseThrow();
  }

  private GroupMembersComponent.MemberDisplayInfo displayNameFor(String userId) {
    UserInfo userInfo = userInformationService.findById(userId).orElse(null);
    if (userInfo == null) {
      return null;
    }
    return new GroupMembersComponent.MemberDisplayInfo(userInfo.fullName(),
        userInfo.platformUserName());
  }

  private List<UserInfo> searchAddableUsers(String filter, int offset, int limit) {
    try {
      // best-effort search honoring the combobox paging contract; the application layer
      // enforces the membership/role gates
      return userInformationService.queryActiveUsersWithFilter(
          filter, offset, limit, List.of(life.qbic.application.commons.SortOrder.of("userName")));
    } catch (RuntimeException e) {
      // best-effort search; an empty list just means "no suggestions"
      return List.of();
    }
  }

  private void toast(String key, Object[] params, Locale locale) {
    Toast toast = messageFactory.toast(key, params, locale);
    toast.open();
  }

  /** Profile-style group block: subheading + whitespace, no card chrome. */
  private static Div settingsGroup(String title) {
    var group = new Div();
    group.addClassName("settings-group");
    var heading = new H3(title);
    heading.addClassName("settings-group__title");
    group.add(heading);
    return group;
  }

  /**
   * Members section: a settings-group whose subheading carries the current roster size
   * ("Members (N)"). The heading is captured so the count stays in sync when members are
   * added or removed in place.
   */
  private Div newMembersGroup(int memberCount) {
    Div group = new Div();
    group.addClassName("settings-group");
    membersHeading = new H3("Members (" + memberCount + ")");
    membersHeading.addClassName("settings-group__title");
    group.add(membersHeading);
    return group;
  }

  /**
   * Adds a primary action (e.g. "Add member") on the same line as the group's title, at the
   * top-right of the group block. The action is decoupled from the roster's per-selection
   * toolbar, so it visually belongs to the section, not to the list rows.
   */
  private static void addGroupAction(Div settingsGroup, Component action) {
    requireNonNull(settingsGroup);
    requireNonNull(action);
    Div headingRow = new Div();
    headingRow.addClassName("settings-group__header-row");
    for (Component child : settingsGroup.getChildren().toList()) {
      headingRow.add(child);
    }
    action.addClassName("settings-group__action");
    headingRow.add(action);
    settingsGroup.removeAll();
    settingsGroup.add(headingRow);
  }
}