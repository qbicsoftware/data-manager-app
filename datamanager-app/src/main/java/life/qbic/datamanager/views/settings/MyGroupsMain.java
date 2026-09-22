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
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupInformationService;
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
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired MessageSourceNotificationFactory messageFactory) {
    this(groupInformationService,
        groupService,
        defaultSuccessToast(messageFactory),
        defaultErrorToast(messageFactory),
        () -> {
          Authentication authentication =
              SecurityContextHolder.getContext().getAuthentication();
          return userIdTranslator.translateToUserId(authentication).orElseThrow();
        },
        (groupId, onConfirm) -> MyGroupsMain.showLeaveConfirmation(groupId, onConfirm));
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
    this.messageFactory = requireNonNull(messageFactory,
        "messageFactory must not be null");
  }

  /**
   * Test constructor: injects all seams directly. Intentionally package-private so only tests in
   * this package can bypass the Vaadin {@code UI} and Spring context wiring.
   */
  MyGroupsMain(GroupInformationService groupInformationService,
      GroupService groupService,
      Consumer<String> successToast,
      Consumer<String> errorToast,
      Supplier<String> currentUserId,
      MyGroupsComponent.LeaveConfirmation leaveConfirmation) {
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.groupService = requireNonNull(groupService, "groupService must not be null");
    this.successToast = requireNonNull(successToast, "successToast must not be null");
    this.errorToast = requireNonNull(errorToast, "errorToast must not be null");
    this.currentUserId = requireNonNull(currentUserId, "currentUserId must not be null");
    this.leaveConfirmation = requireNonNull(leaveConfirmation,
        "leaveConfirmation must not be null");
    this.userIdTranslator = null;
    this.messageFactory = null;
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
        MyGroupsMain::showLeaveConfirmation);
    myGroupsComponent.refresh();
    section.addContent(myGroupsComponent);
    Button newGroupButton = new Button("New group");
    newGroupButton.setClassName("my-groups__new-group");
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
}