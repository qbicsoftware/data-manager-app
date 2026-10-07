package life.qbic.datamanager.views.groups;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.application.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Admin org-group creation page</b>
 * <p>
 * A dedicated, admin-only route hosting the organisational group creation form
 * (FEAT-USER-GROUPS-01). Submitting a valid form creates an org group (type {@code ORG}) via
 * {@link GroupService#createOrgGroup}, shows a success toast and navigates back to the admin
 * org-group directory ({@link AdminGroupsMain}).
 * <p>
 * Access control is <b>defense in depth</b>: the route gate re-checks the caller is a QBiC
 * administrator ({@link GroupAdministrationPermission}) on every {@code beforeEnter} and reroutes
 * to the not-found page otherwise. The authoritative gate remains the application boundary —
 * {@link GroupService#createOrgGroup} enforces the same admin check directly, so even a forged
 * request cannot create an org group without {@code ROLE_ADMIN}.
 * <p>
 * All side effects (navigation, toast, user-id resolution) are provided through seams so the
 * route logic is unit-testable without a Vaadin {@code UI}.
 *
 * @since 1.19.0
 */
@Route(value = AppRoutes.GroupsRoutes.NEW_ORG_GROUP, layout = GroupsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Groups · New Organisational Group")
public class AdminGroupCreationMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1324858971214200441L;

  private final GroupService groupService;
  private final GroupAdministrationPermission groupAdministrationPermission;
  private final Function<String, Boolean> nameAvailability;
  private final Consumer<String> successToast;
  private final Runnable errorToast;
  private final Runnable navigateToMyGroups;
  private final Supplier<String> currentUserId;

  private transient NewGroupForm form;
  private transient SettingsSection section;

  /**
   * Production constructor: wires the seams to the real services, toasts, navigation and the
   * current authenticated user.
   */
  public AdminGroupCreationMain(
      @Autowired GroupService groupService,
      @Autowired GroupInformationService groupInformationService,
      @Autowired GroupAdministrationPermission groupAdministrationPermission,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired MessageSourceNotificationFactory messageFactory) {
    this.groupService = requireNonNull(groupService, "groupService must not be null");
    this.groupAdministrationPermission = requireNonNull(groupAdministrationPermission,
        "groupAdministrationPermission must not be null");
    this.nameAvailability = defaultNameAvailability(groupInformationService);
    this.successToast = defaultSuccessToast(messageFactory);
    this.errorToast = defaultErrorToast(messageFactory);
    this.navigateToMyGroups = () -> UI.getCurrent().navigate(AdminGroupsMain.class);
    this.currentUserId = () -> {
      Authentication authentication =
          SecurityContextHolder.getContext().getAuthentication();
      return userIdTranslator.translateToUserId(authentication).orElseThrow();
    };
    addClassName("admin-group-creation");
  }

  private static Function<String, Boolean> defaultNameAvailability(
      GroupInformationService groupInformationService) {
    return name -> {
      try {
        return groupInformationService.isGroupNameAvailable(name);
      } catch (RuntimeException e) {
        // the hint is best-effort; the authoritative check happens on submit
        return true;
      }
    };
  }

  private static Consumer<String> defaultSuccessToast(
      MessageSourceNotificationFactory messageFactory) {
    return name -> {
      Toast toast = messageFactory.toast("user-groups.created.org.success",
          new Object[]{name}, Locale.getDefault());
      toast.open();
    };
  }

  private static Runnable defaultErrorToast(MessageSourceNotificationFactory messageFactory) {
    return () -> {
      Toast toast = messageFactory.toast("user-groups.created.error",
          new Object[]{}, Locale.getDefault());
      toast.open();
    };
  }

  /**
   * Returns the form instance currently displayed, for test assertions.
   *
   * @return the currently displayed form, or {@code null} before {@link #beforeEnter}
   */
  NewGroupForm form() {
    return form;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String userId = currentUserId.get();
    if (!groupAdministrationPermission.isAdmin(userId)) {
      // Defense in depth: hide the surface entirely. The service gate remains authoritative.
      event.rerouteToError(NotFoundException.class);
      return;
    }
    if (section != null) {
      remove(section);
      section = null;
      form = null;
    }
    form = new NewGroupForm(nameAvailability, groupService, currentUserId, successToast,
        errorToast, navigateToMyGroups, true);
    form.addCancelListener(cancelEvent -> navigateToMyGroups.run());
    section = new SettingsSection("New Organisational Group",
        "Create an organisational group for recurring teams.");
    section.addContent(form);
    add(section);
  }
}