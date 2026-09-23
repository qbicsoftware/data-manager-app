package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.settings.GroupManagementDialogs.ManageAction;
import life.qbic.datamanager.views.settings.GroupManagementDialogs.ManagementRequest;
import life.qbic.datamanager.views.settings.MyGroupsComponent.ManagementAction;
import life.qbic.datamanager.views.settings.MyGroupsComponent.ManagementActionRequest;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupManagementService;
import life.qbic.usergroups.api.MyGroupMembership;
import life.qbic.usergroups.application.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * My Groups Main
 * <p>
 * This component hosts the "My Groups" settings section of the current logged-in user. It lists
 * the groups the user belongs to together with their internal role and provides access to group
 * management actions.
 * <p>
 * The leave-group flow is wired here: the list component asks through a
 * {@link MyGroupsComponent.LeaveConfirmation} seam, this view shows an {@link AlertDialog}
 * confirmation and, when confirmed, calls {@link GroupService#removeMembership} and toasts the
 * outcome before refreshing the list.
 * <p>
 * The owner/manager management actions (FEAT-USER-GROUPS-04, task #1577) are handled here: the
 * list component fires {@link ManagementActionRequest} events and this view opens the
 * corresponding dialog (manage members / rename / description / dissolve), invokes the
 * {@link GroupManagementService} and refreshes the list on success.
 *
 * @since 1.19.0
 */
@Route(value = AppRoutes.GroupsRoutes.MY_GROUPS, layout = SettingsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Settings · My Groups")
public class MyGroupsMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 6902406260794581649L;

  private final transient GroupInformationService groupInformationService;
  private final transient GroupService groupService;
  private final transient GroupManagementService groupManagementService;
  private final transient UserInformationService userInformationService;
  private transient AuthenticationToUserIdTranslationService userIdTranslator;
  private transient MessageSourceNotificationFactory messageFactory;

  private final Consumer<String> successToast;
  private final Consumer<String> errorToast;
  private final Supplier<String> currentUserId;
  private final MyGroupsComponent.LeaveConfirmation leaveConfirmation;

  private SettingsSection section;
  private MyGroupsComponent myGroupsComponent;

  /**
   * Production constructor: wires the seams to the real services, toasts, user-id resolution
   * and the {@link AlertDialog} based leave confirmation.
   */
  public MyGroupsMain(
      @Autowired GroupInformationService groupInformationService,
      @Autowired GroupService groupService,
      @Autowired GroupManagementService groupManagementService,
      @Autowired UserInformationService userInformationService,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired MessageSourceNotificationFactory messageFactory) {
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.groupService = requireNonNull(groupService, "groupService must not be null");
    this.groupManagementService = requireNonNull(groupManagementService,
        "groupManagementService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
    this.messageFactory = requireNonNull(messageFactory,
        "messageFactory must not be null");
    this.successToast = defaultSuccessToast(messageFactory);
    this.errorToast = defaultErrorToast(messageFactory);
    this.currentUserId = () -> {
      Authentication authentication =
          SecurityContextHolder.getContext().getAuthentication();
      return userIdTranslator.translateToUserId(authentication).orElseThrow();
    };
    this.leaveConfirmation = (groupId, onConfirm) ->
        MyGroupsMain.showLeaveConfirmation(groupId, onConfirm);
    addClassName("my-groups");
  }

  private static Consumer<String> defaultSuccessToast(
      MessageSourceNotificationFactory messageFactory) {
    return groupName -> {
      Toast toast = messageFactory.toast("user-groups.leave.success",
          new Object[]{groupName}, Locale.getDefault());
      toast.open();
    };
  }

  private static Consumer<String> defaultErrorToast(
      MessageSourceNotificationFactory messageFactory) {
    return groupName -> {
      Toast toast = messageFactory.toast("user-groups.leave.error",
          new Object[]{groupName}, Locale.getDefault());
      toast.open();
    };
  }

  private static void showLeaveConfirmation(String groupId, Runnable onConfirm) {
    AlertDialog.alert(com.vaadin.flow.component.UI.getCurrent())
        .warning()
        .title("Leave group?")
        .message("You will lose access to projects this group is shared with.")
        .confirmButton("Leave", onConfirm::run)
        .cancelButton("Cancel", () -> { /* dismiss only */ })
        .build()
        .open();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = userIdTranslator.translateToUserId(authentication).orElseThrow();

    if (Objects.nonNull(section)) {
      remove(section);
    }
    section = new SettingsSection("My Groups",
        "Groups you belong to and manage.");
    myGroupsComponent = new MyGroupsComponent(
        () -> groupInformationService.listMyGroups(userId),
        this::refreshList,
        this::onLeaveConfirmed,
        MyGroupsMain::showLeaveConfirmation,
        MyGroupsComponent.ManagementActionPolicy.defaultPolicy());
    myGroupsComponent.refresh();
    myGroupsComponent.addManagementActionListener(this::onManagementActionRequest);
    section.addContent(myGroupsComponent);
    Button newGroupButton = new Button("New group");
    newGroupButton.addClassName("primary");
    newGroupButton.addClickListener(click ->
        com.vaadin.flow.component.UI.getCurrent().navigate(NewGroupMain.class));
    section.addAction(newGroupButton);
    add(section);
  }

  private void refreshList() {
    if (myGroupsComponent != null) {
      myGroupsComponent.refresh();
    }
  }

  void confirmLeave(String groupId) {
    leaveConfirmation.confirm(groupId, () -> onLeaveConfirmed(groupId));
  }

  void onLeaveConfirmed(String groupId) {
    String groupName = groupInformationService.listMyGroups(currentUserId.get()).stream()
        .filter(membership -> membership.groupId().equals(groupId))
        .map(MyGroupMembership::groupName)
        .findFirst()
        .orElse(groupId);

    Result<Void, ApplicationException> result =
        groupService.removeMembership(groupId, currentUserId.get());

    result.onValue(v -> {
          successToast.accept(groupName);
          refreshList();
        })
        .onError(error -> errorToast.accept(groupName));
  }

  private void onManagementActionRequest(ManagementActionRequest event) {
    String groupId = event.groupId();
    String actingUserId = currentUserId.get();
    MyGroupMembership membership = groupInformationService.listMyGroups(actingUserId).stream()
        .filter(m -> m.groupId().equals(groupId))
        .findFirst()
        .orElse(null);
    if (membership == null) {
      // group dissolved or the grant vanished; refresh and ignore
      refreshList();
      return;
    }

    switch (event.action()) {
      case MANAGE_MEMBERS -> openManageMembers(membership, actingUserId);
      case APPOINT_MANAGER -> openAppointManager(membership, actingUserId);
      case RENAME -> openRename(membership, actingUserId);
      case DISSOLVE -> openDissolve(membership, actingUserId);
    }
  }

  private void openManageMembers(MyGroupMembership membership, String actingUserId) {
    String groupId = membership.groupId();
    GroupManagementDialogs.openManageMembersDialog(
        this,
        groupId,
        groupManagementService.listMembers(groupId, actingUserId),
        membership.myRole(),
        this::displayNameFor,
        this::searchAddableUsers,
        this::handleManagementRequest,
        () -> groupManagementService.listMembers(groupId, actingUserId));
  }

  private void openAppointManager(MyGroupMembership membership, String actingUserId) {
    String groupId = membership.groupId();
    List<life.qbic.usergroups.api.GroupMember> members =
        groupManagementService.listMembers(groupId, actingUserId);
    GroupManagementDialogs.openAppointManagerDialog(this, groupId, members,
        this::displayNameFor, this::searchAddableUsers, this::handleManagementRequest);
  }

  private void openRename(MyGroupMembership membership, String actingUserId) {
    GroupManagementDialogs.openRenameDialog(this, membership.groupId(), membership.groupName(),
        this::handleManagementRequest);
  }

  private void openDissolve(MyGroupMembership membership, String actingUserId) {
    GroupManagementDialogs.openDissolveDialog(this, membership.groupId(),
        membership.groupName(), this::handleManagementRequest);
  }

  private String displayNameFor(String userId) {
    UserInfo userInfo = userInformationService.findById(userId).orElse(null);
    if (userInfo == null) {
      return null;
    }
    return userInfo.fullName() == null || userInfo.fullName().isBlank()
        ? userInfo.platformUserName() : userInfo.fullName();
  }

  private List<UserInfo> searchAddableUsers(String filter, int offset, int limit) {
    try {
      return userInformationService.queryActiveUsersWithFilter(filter, offset, limit,
          List.of(SortOrder.of("userName")));
    } catch (RuntimeException e) {
      // best-effort search; an empty list just means "no suggestions"
      return List.of();
    }
  }

  private void handleManagementRequest(ManagementRequest request) {
    String actingUserId = currentUserId.get();
    String groupName = resolveGroupName(request.groupId());
    try {
      switch (request.action()) {
        case ADD_MEMBER -> groupManagementService.addMember(request.groupId(), actingUserId,
            request.userId());
        case REMOVE_MEMBER -> groupManagementService.removeMember(request.groupId(),
            actingUserId, request.userId());
        case APPOINT_MANAGER -> groupManagementService.appointManager(request.groupId(),
            actingUserId, request.userId());
        case DEMOTE_MANAGER -> groupManagementService.demoteManager(request.groupId(),
            actingUserId, request.userId());
        case RENAME -> groupManagementService.renameGroup(request.groupId(), actingUserId,
            request.newName());
        case UPDATE_DESCRIPTION -> groupManagementService.updateDescription(request.groupId(),
            actingUserId, request.newDescription());
        case DISSOLVE -> {
          groupManagementService.dissolveGroup(request.groupId(), actingUserId);
          showDissolveSuccess(groupName);
          refreshList();
          return;
        }
      }
      showManagementSuccess(request.action() == ManageAction.RENAME && request.newName() != null
          ? request.newName() : groupName);
      refreshList();
    } catch (Exception error) {
      if (request.action() == ManageAction.DISSOLVE) {
        showDissolveError(groupName);
      } else {
        showManagementError(groupName);
      }
    }
  }

  private void showManagementSuccess(String groupName) {
    Toast toast = messageFactory.toast("user-groups.manage.success",
        new Object[]{groupName}, Locale.getDefault());
    toast.open();
  }

  private void showDissolveSuccess(String groupName) {
    Toast toast = messageFactory.toast("user-groups.dissolve.success",
        new Object[]{groupName}, Locale.getDefault());
    toast.open();
  }

  private void showDissolveError(String groupName) {
    Toast toast = messageFactory.toast("user-groups.dissolve.error",
        new Object[]{groupName}, Locale.getDefault());
    toast.open();
  }

  private void showManagementError(String groupName) {
    Toast toast = messageFactory.toast("user-groups.manage.error",
        new Object[]{groupName}, Locale.getDefault());
    toast.open();
  }

  private String resolveGroupName(String groupId) {
    return groupInformationService.listMyGroups(currentUserId.get()).stream()
        .filter(m -> m.groupId().equals(groupId))
        .map(MyGroupMembership::groupName)
        .findFirst()
        .orElse(groupId);
  }
}