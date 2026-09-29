package life.qbic.datamanager.views.groups;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
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
 * <b>Admin org-group manager management page</b>
 * <p>
 * The admin-only surface to govern the <em>managers</em> of one organisational group
 * (FEAT-USER-GROUPS-02, AC1/AC4). It lists the group's MANAGER members (admin oversight through
 * {@link GroupManagementService#listMembers(String, String)}' admin branch), offers
 * <b>Assign manager</b> (user search over non-member candidates; also allows promoting an
 * existing regular member) and a per-row <b>Remove</b> action.
 * <p>
 * Removing the <em>last</em> manager is guarded by an {@link AlertDialog} confirm explaining the
 * AC4 governance model: the group stays active, QBiC admins remain owner-equivalent and no OWNER
 * membership row is created.
 * <p>
 * Access control is <b>defense in depth</b>: the route gate re-checks the caller is a QBiC
 * administrator on every {@code beforeEnter} and reroutes to the not-found page otherwise. The
 * authoritative gate remains the application boundary — {@link GroupManagementService#appointOrgManager}
 * and {@link GroupManagementService#removeOrgManager} enforce the same admin check directly.
 * <p>
 * In line with the PO model, this page deliberately exposes <b>no</b> member roster beyond the
 * managers and <b>no</b> profile editing: org managers manage regular members and the group
 * profile through the existing {@link GroupDetailMain} surface exactly like ad-hoc groups.
 *
 * @since 1.22.0
 */
@Route(value = AppRoutes.GroupsRoutes.ADMIN_GROUP_MANAGERS, layout = GroupsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Groups · Manage Managers")
public class AdminGroupManagersMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 8112438654172894562L;

  public static final String GROUP_ID_ROUTE_PARAMETER = "groupId";

  private final GroupInformationService groupInformationService;
  private final GroupManagementService groupManagementService;
  private final UserInformationService userInformationService;
  private final GroupAdministrationPermission groupAdministrationPermission;
  private final AuthenticationToUserIdTranslationService userIdTranslator;
  private final MessageSourceNotificationFactory messageFactory;

  private transient SettingsSection section;
  private transient Div managerList;
  private transient String groupId;
  private transient String groupName;

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
      @Autowired MessageSourceNotificationFactory messageFactory) {
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
    if (section != null) {
      remove(section);
      section = null;
      managerList = null;
    }
    this.groupId = group.get().id();
    this.groupName = group.get().name();
    section = new SettingsSection("Manage Managers · " + groupName,
        "QBiC administrators govern the managers of this organisational group. Options for "
            + "managers: add/remove regular members and rename or describe the group — through "
            + "the My Groups surface, exactly like ad-hoc groups.");
    managerList = new Div();
    managerList.addClassName("admin-group-managers-list");
    renderManagers();
    section.addContent(managerList);
    Button assignManagerButton = new Button("Assign manager");
    assignManagerButton.addClassName("primary");
    assignManagerButton.addClickListener(click -> openAssignManagerDialog());
    section.addAction(assignManagerButton);
    add(section);
  }

  private void renderManagers() {
    managerList.removeAll();
    List<GroupMember> managers = currentManagers();
    if (managers.isEmpty()) {
      Span emptyState = new Span(
          "No managers yet. Assign a manager so the group can be operated by its members.");
      emptyState.addClassName("admin-group-managers-empty-state");
      managerList.add(emptyState);
      return;
    }
    managers.forEach(manager -> managerList.add(buildManagerRow(manager)));
  }

  private List<GroupMember> currentManagers() {
    return managersFrom(groupManagementService.listMembers(groupId, currentUserId()));
  }

  /**
   * Filters the MANAGER members from a roster (test seam; the admin-oversight {@code listMembers}
   * branch supplies the full org roster to a QBiC admin).
   */
  List<GroupMember> managersFrom(List<GroupMember> roster) {
    return roster.stream()
        .filter(member -> member.role() == GroupRole.MANAGER)
        .toList();
  }

  private Component buildManagerRow(GroupMember manager) {
    Div row = new Div();
    row.addClassName("admin-group-managers-row");

    Div identity = new Div();
    identity.addClassName("admin-group-managers-row__identity");
    UserInfo userInfo = userInformationService.findById(manager.userId()).orElse(null);
    String fullName = userInfo == null ? null : userInfo.fullName();
    String userName = userInfo == null ? null : userInfo.platformUserName();
    String primary = !isBlank(fullName) ? fullName
        : (!isBlank(userName) ? userName : manager.userId());
    UserAvatar avatar = new UserAvatar();
    avatar.setUserId(manager.userId());
    avatar.setName(primary);
    avatar.setAbbreviation(abbreviationFor(primary));
    avatar.addClassName("admin-group-managers-row__avatar");
    identity.add(avatar);

    Div title = new Div();
    title.addClassName("admin-group-managers-row__title");
    Span name = new Span(primary);
    name.addClassName("admin-group-managers-row__name");
    title.add(name);
    if (!isBlank(userName) && !userName.equals(primary)) {
      Span userNameSpan = new Span("(" + userName + ")");
      userNameSpan.addClassNames("text-s", "text-secondary");
      title.add(userNameSpan);
    }
    identity.add(title);

    Button removeButton = new Button("Remove");
    removeButton.addClassName("admin-group-managers-row__remove");
    removeButton.addClickListener(click -> confirmRemoveManager(manager));

    row.add(identity);
    row.add(removeButton);
    return row;
  }

  private void confirmRemoveManager(GroupMember manager) {
    boolean isLast = currentManagers().size() == 1;
    if (isLast) {
      // AC4 governance model: removing the last manager leaves the group governed by QBiC admins.
      AlertDialog.danger(this,
          "Remove the last manager?",
          "No managers remain; the group switches to QBiC-admin governance. The group stays "
              + "active and no owner is created. You can assign a new manager at any time.",
          "Remove manager",
          "Keep manager",
          () -> performRemoveManager(manager)).open();
    } else {
      AlertDialog.danger(this,
          "Remove this manager?",
          "This user loses their manager role in the group and their membership is removed.",
          "Remove manager",
          "Keep manager",
          () -> performRemoveManager(manager)).open();
    }
  }

  private void performRemoveManager(GroupMember manager) {
    try {
      groupManagementService.removeOrgManager(groupId, currentUserId(), manager.userId());
      toast("user-groups.org.manager.removed.success",
          new Object[]{displayName(manager.userId()), groupName}, Locale.getDefault());
      renderManagers();
    } catch (RuntimeException error) {
      toast("user-groups.org.manager.removed.error", new Object[0], Locale.getDefault());
    }
  }

  /**
   * Opens the assign-manager dialog: a user search over candidates that are <em>not</em> yet
   * managers (regular members are shown as promotable; other non-members as direct
   * appointments). Mirrors the add-member search pattern of {@link GroupMembersComponent}.
   */
  private void openAssignManagerDialog() {
    ComboBox<UserInfo> picker = new ComboBox<>("Select a user");
    picker.setPlaceholder("Search for username or full name");
    picker.setItemLabelGenerator(UserInfo::platformUserName);
    picker.setRenderer(new ComponentRenderer<>(candidate -> {
      String primary = !isBlank(candidate.fullName())
          ? candidate.fullName() : candidate.platformUserName();
      UserAvatar avatar = new UserAvatar();
      avatar.setUserId(candidate.id());
      avatar.setName(primary);
      avatar.setAbbreviation(abbreviationFor(primary));
      Div div = new Div();
      div.addClassName("user-search-result");
      avatar.addClassName("user-search-result__avatar");
      div.add(avatar);
      Div identity = new Div();
      identity.addClassName("user-search-result__identity");
      Span name = new Span(primary);
      name.addClassName("user-search-result__name");
      identity.add(name);
      if (!isBlank(candidate.platformUserName()) && !candidate.platformUserName().equals(primary)) {
        Span userNameSpan = new Span("(" + candidate.platformUserName() + ")");
        userNameSpan.addClassNames("text-s", "text-secondary");
        identity.add(userNameSpan);
      }
      div.add(identity);
      return div;
    }));
    picker.setItems(query -> searchCandidates(query.getFilter().orElse(null),
        query.getOffset(), query.getLimit()).stream());

    AppDialog dialog = AppDialog.small();
    DialogHeader.with(dialog, "Assign manager");
    picker.setWidthFull();
    DialogBody.withoutUserInput(dialog, picker);
    DialogFooter.with(dialog, "Cancel", "Assign manager");
    dialog.registerConfirmAction(() -> {
      UserInfo selected = picker.getValue();
      if (selected == null) {
        return;
      }
      if (isManager(selected.id())) {
        picker.setErrorMessage("This user is already a manager of the group.");
        picker.setInvalid(true);
        return;
      }
      performAssignManager(selected);
      dialog.close();
    });
    dialog.registerCancelAction(dialog::close);
    dialog.open();
  }

  private void performAssignManager(UserInfo user) {
    try {
      groupManagementService.appointOrgManager(groupId, currentUserId(), user.id());
      toast("user-groups.org.manager.assigned.success",
          new Object[]{displayName(user.id()), groupName}, Locale.getDefault());
      renderManagers();
    } catch (RuntimeException error) {
      toast("user-groups.org.manager.assigned.error", new Object[0], Locale.getDefault());
    }
  }

  private List<UserInfo> searchCandidates(String filter, int offset, int limit) {
    try {
      return userInformationService.queryActiveUsersWithFilter(filter, offset, limit,
          List.of(life.qbic.application.commons.SortOrder.of("userName"))).stream()
          .filter(userInfo -> !isManager(userInfo.id()))
          .toList();
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private boolean isManager(String userId) {
    return currentManagers().stream().anyMatch(m -> m.userId().equals(userId));
  }
  private String displayName(String userId) {
    UserInfo userInfo = userInformationService.findById(userId).orElse(null);
    if (userInfo == null) {
      return userId;
    }
    return !isBlank(userInfo.fullName()) ? userInfo.fullName() : userInfo.platformUserName();
  }

  private String currentUserId() {
    Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();
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

  private static RuntimeException notFound(BeforeEnterEvent event) {
    event.rerouteToError(NotFoundException.class);
    return new NotFoundException("missing group id");
  }

  private static String abbreviationFor(String name) {
    if (isBlank(name)) {
      return "";
    }
    String[] parts = name.trim().split("\\s+");
    if (parts.length == 1) {
      return parts[0].substring(0, 1).toUpperCase();
    }
    return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}