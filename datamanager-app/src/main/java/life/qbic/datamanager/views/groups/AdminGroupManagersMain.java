package life.qbic.datamanager.views.groups;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
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
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.profilepicture.ProfilePictureDialog;
import life.qbic.datamanager.profilepicture.ProfilePictureMessages;
import life.qbic.datamanager.profilepicture.ProfilePictureOwnerType;
import life.qbic.datamanager.profilepicture.ProfilePictureService;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.InlineEditableField;
import life.qbic.datamanager.views.general.InlineEditableField.InputKind;
import life.qbic.datamanager.views.general.InlineEditableField.SaveEvent;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.dialog.TypeToConfirmInput;
import life.qbic.datamanager.views.groups.GroupMembersComponent.GroupMembersUpdatedRequest;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import life.qbic.usergroups.api.GroupInfo;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupManagementService;
import life.qbic.usergroups.api.GroupMember;
import life.qbic.usergroups.api.GroupRole;
import life.qbic.usergroups.api.GroupType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Admin org-group management page</b>
 * <p>
 * The admin-only surface to govern one organisational group (FEAT-USER-GROUPS-02). It reuses the
 * ad-hoc group management layout of {@link GroupDetailMain} <em>exactly</em> — a flat
 * {@link SettingsSection} made of {@code settings-group} blocks (no card chrome):
 * <ul>
 *   <li><b>Group</b> — the group name and description as inline-editable rows
 *   ({@link InlineEditableField}), editable by the QBiC admin via the admin-gated org methods.</li>
 *   <li><b>Members (N)</b> — the full org roster in the <em>same</em> {@link GroupMembersComponent}
 *   grid used by ad-hoc groups. The acting role is {@code OWNER} (owner-equivalent), so the full
 *   toolbar is available exactly as for an ad-hoc owner: <b>Add member</b>, multi-select
 *   <b>Remove</b> (with confirmation) and <b>Assign role</b> (promote to MANAGER / demote back to
 *   MEMBER). Every action is routed to the admin-gated org service methods.</li>
 *   <li><b>Danger zone</b> — admin dissolve of the org group, guarded by an inline
 *   type-to-confirm form exactly like the ad-hoc dissolve.</li>
 * </ul>
 * <p>
 * Access control is <b>defense in depth</b>: the route gate re-checks the caller is a QBiC
 * administrator on every {@code beforeEnter} and reroutes to the not-found page otherwise; the
 * service layer re-enforces the admin gate on every mutation.
 *
 * @since 1.22.0
 */
@Route(value = AppRoutes.GroupsRoutes.ADMIN_GROUP_MANAGERS, layout = GroupsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Groups · Admin · Group")
public class AdminGroupManagersMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 8232438654172894563L;

  public static final String GROUP_ID_ROUTE_PARAMETER = "groupId";

  private final GroupInformationService groupInformationService;
  private final GroupManagementService groupManagementService;
  private final UserInformationService userInformationService;
  private final GroupAdministrationPermission groupAdministrationPermission;
  private final AuthenticationToUserIdTranslationService userIdTranslator;
  private final MessageSourceNotificationFactory messageFactory;
  private final transient ProfilePictureService profilePictureService;

  private transient SettingsSection section;
  private transient InlineEditableField nameField;
  private transient InlineEditableField descriptionField;
  private transient H3 membersHeading;
  private transient RouterLink backLink;
  private transient TypeToConfirmInput dissolveConfirmInput;
  private transient String groupId;
  private transient String groupName;
  private transient String groupDescription;
  private transient MenuItem removeGroupPictureItem;

  /**
   * Production constructor: wires the services, the admin gate, the user-id resolution and the
   * notification factory.
   */
  public AdminGroupManagersMain(
      @Autowired GroupInformationService groupInformationService,
      @Autowired GroupManagementService groupManagementService,
      @Autowired UserInformationService userInformationService,
      @Autowired GroupAdministrationPermission groupAdministrationPermission,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired MessageSourceNotificationFactory messageFactory,
      @Autowired ProfilePictureService profilePictureService) {
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.groupManagementService = requireNonNull(groupManagementService,
        "groupManagementService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.groupAdministrationPermission = requireNonNull(groupAdministrationPermission,
        "groupAdministrationPermission must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
    this.messageFactory = requireNonNull(messageFactory,
        "messageFactory must not be null");
    this.profilePictureService = requireNonNull(profilePictureService,
        "profilePictureService must not be null");
    addClassName("admin-group-managers");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!isCurrentUserAdmin()) {
      event.rerouteToError(NotFoundException.class);
      return;
    }
    String requestedGroupId = event.getRouteParameters().get(GROUP_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> notFound(event));
    Optional<GroupInfo> group = groupInformationService.findGroupById(requestedGroupId);
    if (group.isEmpty() || group.get().type() != GroupType.ORG) {
      event.rerouteToError(NotFoundException.class);
      return;
    }
    clearPageState();
    this.groupId = group.get().id();
    this.groupName = group.get().name();
    this.groupDescription = group.get().description() == null ? "" : group.get().description();
    buildPage(group.get());
  }

  private void clearPageState() {
    if (section != null) {
      remove(section);
      section = null;
    }
    if (backLink != null) {
      remove(backLink);
      backLink = null;
    }
    nameField = null;
    descriptionField = null;
    membersHeading = null;
    dissolveConfirmInput = null;
  }

  private void buildPage(GroupInfo group) {
    String actingUserId = currentUserId();

    // Back navigation as a settings-style breadcrumb link (matches the aside links), above the
    // section — same as the GroupDetailMain pattern.
    backLink = new RouterLink("Admin · Organisational Groups", AdminGroupsMain.class);
    backLink.addClassNames("group-detail-back-link", "admin-group-managers-back-link");
    add(backLink);

    section = new SettingsSection(group.name(),
        "Manage this organisational group and its members.");

    // ── Group profile group (inline editable, same as GroupDetailMain) ──
    Div profileGroup = settingsGroup("Group");

    UserAvatar groupAvatar = new UserAvatar();
    groupAvatar.setGroupId(groupId);
    groupAvatar.addClassName("profile-picture-block__avatar");

    Div avatarWrapper = new Div(groupAvatar);
    avatarWrapper.addClassName("profile-picture-block__avatar-wrapper");

    Button pictureSettingsButton = new Button(new Icon(VaadinIcon.COG));
    pictureSettingsButton.addClassName("profile-picture-block__overlay-button");
    pictureSettingsButton.setAriaLabel("Group picture options");
    pictureSettingsButton.getElement().setAttribute("title", "Change picture");

    ContextMenu pictureMenu = new ContextMenu();
    pictureMenu.setTarget(pictureSettingsButton);
    pictureMenu.setOpenOnClick(true);
    pictureMenu.addItem(groupPictureMenuItem(new Icon(VaadinIcon.EXCHANGE), "Change"), event -> {
      var dialog = new ProfilePictureDialog();
      dialog.addPictureSelectedListener(png -> {
        var result = profilePictureService.setGroupPicture(groupId, currentUserId(), png);
        if (result.isError()) {
          dialog.showError(ProfilePictureMessages.userMessage(result.getError()));
          return;
        }
        dialog.close();
        groupAvatar.refresh(ProfilePictureOwnerType.GROUP, groupId);
        if (removeGroupPictureItem != null) {
          removeGroupPictureItem.setEnabled(true);
        }
        messageFactory.toast("group.picture.change.success", new Object[]{}, getLocale()).open();
      });
      dialog.open();
    });
    removeGroupPictureItem = pictureMenu.addItem(
        groupPictureMenuItem(new Icon(VaadinIcon.CLOSE_SMALL), "Remove"),
        event -> AlertDialog.danger(this,
            "Remove group picture?",
            "Are you sure you want to remove the group picture? The default placeholder will "
                + "be shown instead.",
            "Remove picture",
            "Keep picture",
            () -> removeGroupPicture(groupId, groupAvatar))
            .open());
    removeGroupPictureItem.setEnabled(profilePictureService.findContentHash(
        ProfilePictureOwnerType.GROUP, groupId).isPresent());
    avatarWrapper.add(pictureSettingsButton);
    profileGroup.add(avatarWrapper);

    nameField = new InlineEditableField("Group name", group.name());
    nameField.setEditable(true);
    nameField.setMinDisplayWidth(28);
    nameField.addSaveListener(this::onNameSave);
    nameField.addCancelListener(e -> { /* UI revert is handled by the component */ });
    profileGroup.add(nameField);

    descriptionField = new InlineEditableField(InputKind.TEXTAREA, "Description",
        group.description() == null ? "" : group.description());
    descriptionField.setEditable(true);
    descriptionField.setMinDisplayWidth(40);
    descriptionField.addSaveListener(this::onDescriptionSave);
    descriptionField.addCancelListener(e -> { /* UI revert is handled by the component */ });
    profileGroup.add(descriptionField);
    section.addContent(profileGroup);

    // ── Members group: the SAME roster component as ad-hoc groups, with the full owner
    // toolbar (Add member / multi-select Remove / Assign role). The QBiC admin acts as
    // owner-equivalent, so actingUserRole = OWNER reveals the Assign-role action; every action
    // is routed to the admin-gated org methods. ──
    List<GroupMember> members = groupManagementService.listMembers(groupId, actingUserId);
    Div membersGroup = newMembersGroup(members.size());
    GroupMembersComponent membersComponent = new GroupMembersComponent(
        groupId, members, GroupRole.OWNER, actingUserId, this::displayNameFor,
        this::searchAddableUsers, this::handleMemberRequest,
        () -> groupManagementService.listMembers(groupId, currentUserId()));
    var addMemberButton = new Button("Add member",
        new com.vaadin.flow.component.icon.Icon(com.vaadin.flow.component.icon.VaadinIcon.PLUS));
    addMemberButton.addClassName("primary");
    addMemberButton.addClickListener(click -> membersComponent.openAddMemberDialog());
    addGroupAction(membersGroup, addMemberButton);
    membersComponent.addMembersUpdatedListener(this::onMembersUpdated);
    membersComponent.addClassName("group-members-in-settings-group");
    membersGroup.add(membersComponent);
    section.addContent(membersGroup);

    // ── Danger zone group (admin dissolve, type-to-confirm like GroupDetailMain) ──
    Div dangerGroup = settingsGroup("Danger zone");
    Span dissolveNote = new Span(
        "Permanently dissolves this organisational group, removes all members and revokes "
            + "access to projects shared with it. This cannot be undone.");
    dissolveNote.addClassName("group-detail-danger-note");
    dangerGroup.add(dissolveNote);

    Div dangerRow = new Div();
    dangerRow.addClassName("group-detail-danger-zone");
    Button dissolveButton = new Button("Dissolve Group", new com.vaadin.flow.component.icon.Icon(
        com.vaadin.flow.component.icon.VaadinIcon.WARNING));
    dissolveButton.addClassName("button-danger");
    dangerRow.add(dissolveButton);
    dangerGroup.add(dangerRow);

    TypeToConfirmInput confirmInput = new TypeToConfirmInput(() -> groupName);
    dissolveConfirmInput = confirmInput;
    confirmInput.addClassNames("group-detail-dissolve-confirm-input", "width-full");
    com.vaadin.flow.component.textfield.TextField confirmField = confirmInput.textField();
    confirmField.addClassNames("width-full");
    Button cancelButton = new Button("Keep group");
    Button confirmButton = new Button("Dissolve group");
    confirmButton.addClassName("button-danger");
    confirmButton.setEnabled(false);

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
      if (confirmInput.validate().hasPassed()) {
        dissolveGroup(groupId, actingUserId);
      }
    });

    section.addContent(dangerGroup);
    add(section);
  }

  private void onNameSave(SaveEvent event) {
    String newName = event.value().trim();
    String actingUserId = currentUserId();
    if (newName.isEmpty()) {
      event.getSource().setError("A group name is required.");
      return;
    }
    if (newName.equals(groupName)) {
      event.getSource().cancelEditing();
      return;
    }
    try {
      groupManagementService.renameOrgGroup(groupId, actingUserId, newName);
      groupName = newName;
      event.getSource().setValue(newName);
      event.getSource().cancelEditing();
      if (section != null) {
        section.setTitle(newName);
      }
      if (dissolveConfirmInput != null) {
        dissolveConfirmInput.refresh();
      }
      toast("user-groups.manage.success", new Object[]{newName}, Locale.getDefault());
    } catch (RuntimeException error) {
      event.getSource().setError("Could not rename the group. "
          + "A group with this name may already exist (names are unique).");
    }
  }

  private void onDescriptionSave(SaveEvent event) {
    String newDescription = event.value().trim();
    String actingUserId = currentUserId();
    if (newDescription.equals(groupDescription)) {
      event.getSource().cancelEditing();
      return;
    }
    try {
      groupManagementService.updateOrgGroupDescription(groupId, actingUserId, newDescription);
      groupDescription = newDescription;
      event.getSource().setValue(newDescription);
      event.getSource().cancelEditing();
      toast("user-groups.manage.success", new Object[]{groupName}, Locale.getDefault());
    } catch (RuntimeException error) {
      event.getSource().setError("Could not update the group description.");
    }
  }

  private void onMembersUpdated(GroupMembersComponent.GroupMembersUpdatedEvent event) {
    if (membersHeading != null) {
      int freshCount = groupManagementService.listMembers(groupId, currentUserId()).size();
      membersHeading.setText("Members (" + freshCount + ")");
    }
    toast("user-groups.manage.success", new Object[]{groupName}, Locale.getDefault());
  }

  /**
   * Routes each roster action from the <em>shared</em> {@link GroupMembersComponent} to the
   * admin-gated org service method. The component is configured with
   * {@code actingUserRole = OWNER} (owner-equivalent), so the toolbar offers Add member /
   * multi-select Remove / Assign role exactly like an ad-hoc group owner sees them.
   */
  private void handleMemberRequest(GroupMembersUpdatedRequest request) {
    String actingUserId = currentUserId();
    switch (request.action()) {
      case ADD_MEMBER -> groupManagementService.addOrgMember(request.groupId(), actingUserId,
          request.userId());
      case REMOVE_MEMBER -> groupManagementService.removeOrgMember(request.groupId(),
          actingUserId, request.userId());
      case APPOINT_MANAGER -> groupManagementService.appointOrgManager(request.groupId(),
          actingUserId, request.userId());
      case DEMOTE_MANAGER -> groupManagementService.demoteOrgManager(request.groupId(),
          actingUserId, request.userId());
    }
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
      return userInformationService.queryActiveUsersWithFilter(filter, offset, limit,
          List.of(life.qbic.application.commons.SortOrder.of("userName")));
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private void dissolveGroup(String groupId, String actingUserId) {
    try {
      groupManagementService.dissolveOrgGroup(groupId, actingUserId);
      toast("user-groups.dissolve.success", new Object[]{groupName}, Locale.getDefault());
      getUI().ifPresent(ui -> ui.navigate(AdminGroupsMain.class));
    } catch (RuntimeException error) {
      toast("user-groups.dissolve.error", new Object[]{groupName}, Locale.getDefault());
    }
  }

  private String currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return userIdTranslator.translateToUserId(authentication).orElseThrow();
  }

  private boolean isCurrentUserAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return userIdTranslator.translateToUserId(authentication)
        .map(groupAdministrationPermission::isAdmin)
        .orElse(false);
  }

  private void toast(String key, Object[] params, Locale locale) {
    Toast toast = messageFactory.toast(key, params, locale);
    toast.open();
  }

  /** Context-menu item content: icon + short label. */
  private static Div groupPictureMenuItem(Icon icon, String label) {
    var content = new Div(icon, new Span(label));
    content.addClassName("profile-picture-menu-item");
    return content;
  }

  private void removeGroupPicture(String groupId, UserAvatar groupAvatar) {
    var result = profilePictureService.removeGroupPicture(groupId, currentUserId());
    if (result.isError()) {
      return;
    }
    groupAvatar.refresh(ProfilePictureOwnerType.GROUP, groupId);
    if (removeGroupPictureItem != null) {
      removeGroupPictureItem.setEnabled(false);
    }
    messageFactory.toast("group.picture.remove.success", new Object[]{}, getLocale()).open();
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
   * ("Members (N)"). The heading is captured so the count stays in sync.
   */
  private Div newMembersGroup(int memberCount) {
    Div group = new Div();
    group.addClassName("settings-group");
    membersHeading = new H3("Members (" + memberCount + ")");
    membersHeading.addClassName("settings-group__title");
    group.add(membersHeading);
    return group;
  }

  /** Adds a primary action on the same line as the group's title (Profile settings convention). */
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

  private static RuntimeException notFound(BeforeEnterEvent event) {
    event.rerouteToError(NotFoundException.class);
    return new NotFoundException("missing group id");
  }
}