package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
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
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.application.GroupInfoProjection;
import life.qbic.usergroups.application.GroupService;
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>New Group page</b>
 * <p>
 * A dedicated, shareable route hosting the ad-hoc group creation form. Submitting a valid form
 * creates the group via {@link GroupService#createAdHocGroup}, shows a success toast and
 * navigates back to the My Groups list so the new group is visible with the caller as OWNER.
 * <p>
 * A case-insensitive duplicate name is rejected inline on the name field; any other failure is
 * surfaced as a generic error toast. All side effects (navigation, toast, user-id resolution)
 * are provided through seams so the route logic is unit-testable without a Vaadin {@code UI}.
 *
 * @since 1.19.0
 */
@Route(value = AppRoutes.GroupsRoutes.NEW_GROUP, layout = SettingsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Settings · New Group")
public class NewGroupMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1155319990453929046L;

  static final String DUPLICATE_NAME_FIELD_MESSAGE =
      "A group with this name already exists. Group names are unique (case-insensitive).";

  private final GroupService groupService;
  private final Function<String, Boolean> nameAvailability;
  private final Consumer<String> successToast;
  private final Runnable errorToast;
  private final Runnable navigateToMyGroups;
  private final Supplier<String> currentUserId;
  private final Consumer<String> nameError;

  private transient NewGroupForm form;

  /**
   * Production constructor: wires the seams to the real services, toasts, navigation and the
   * current authenticated user.
   */
  public NewGroupMain(
      @Autowired GroupService groupService,
      @Autowired GroupInformationService groupInformationService,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired MessageSourceNotificationFactory messageFactory) {
    this(groupService,
        defaultNameAvailability(groupInformationService),
        defaultSuccessToast(messageFactory),
        defaultErrorToast(messageFactory),
        () -> UI.getCurrent().navigate(MyGroupsMain.class),
        () -> {
          Authentication authentication =
              SecurityContextHolder.getContext().getAuthentication();
          return userIdTranslator.translateToUserId(authentication).orElseThrow();
        },
        message -> { /* the form is marked directly by nameErrorSeam */ });
  }

  /**
   * Test constructor: injects all seams directly. Intentionally package-private so only tests in
   * this package can bypass the Vaadin {@code UI} and Spring context wiring.
   */
  NewGroupMain(GroupService groupService,
      Function<String, Boolean> nameAvailability,
      Consumer<String> successToast,
      Runnable errorToast,
      Runnable navigateToMyGroups,
      Supplier<String> currentUserId,
      Consumer<String> nameError) {
    this.groupService = requireNonNull(groupService, "groupService must not be null");
    this.nameAvailability = requireNonNull(nameAvailability,
        "nameAvailability must not be null");
    this.successToast = requireNonNull(successToast, "successToast must not be null");
    this.errorToast = requireNonNull(errorToast, "errorToast must not be null");
    this.navigateToMyGroups = requireNonNull(navigateToMyGroups,
        "navigateToMyGroups must not be null");
    this.currentUserId = requireNonNull(currentUserId, "currentUserId must not be null");
    this.nameError = requireNonNull(nameError, "nameError must not be null");
    addClassName("new-group");
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
      Toast toast = messageFactory.toast("user-groups.created.success",
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
    if (form != null) {
      remove(form);
      form = null;
    }
    form = new NewGroupForm(nameAvailability);
    form.addCreateListener(this::onCreate);
    form.addCancelListener(cancelEvent -> navigateToMyGroups.run());
    SettingsSection section = new SettingsSection("New Group",
        "Create a new ad-hoc group for your collaboration.");
    section.addContent(form);
    add(section);
  }

  private void onCreate(NewGroupForm.CreateEvent createEvent) {
    if (form != null) {
      form.clearNameError();
    }

    GroupName groupName = GroupName.from(createEvent.groupName());
    GroupDescription groupDescription = GroupDescription.from(createEvent.groupDescription());

    Result<GroupInfoProjection, ApplicationException> result =
        groupService.createAdHocGroup(currentUserId.get(), groupName, groupDescription);

    result.onValue(created -> {
          successToast.accept(created.groupName().value());
          navigateToMyGroups.run();
        })
        .onError(error -> {
          if (error.errorCode() == ErrorCode.DUPLICATE_GROUP_NAME) {
            nameErrorSeam(DUPLICATE_NAME_FIELD_MESSAGE);
          } else {
            errorToast.run();
          }
        });
  }

  private void nameErrorSeam(String message) {
    nameError.accept(message);
    if (form != null) {
      form.markNameError(message);
    }
  }
}