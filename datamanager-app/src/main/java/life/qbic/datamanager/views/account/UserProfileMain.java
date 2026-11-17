package life.qbic.datamanager.views.account;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.security.OidcLinkController;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.settings.SettingsMainLayout;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.domain.model.UserId;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * User Profile Main
 * <p>
 * This component hosts the components necessary to show the profile for the current logged-in User
 * via the {@link UserProfileComponent}. Additionally, the user can see the information within his
 * {@link life.qbic.identity.api.UserInfo} and change his Username via the provided UI elements
 */

@Route(value = "settings/profile", layout = SettingsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Settings · Profile")
public class UserProfileMain extends Main implements BeforeEnterObserver, AfterNavigationObserver {

  @Serial
  private static final long serialVersionUID = -5203169099713671493L;

  private static final life.qbic.logging.api.Logger log =
      life.qbic.logging.service.LoggerFactory.logger(UserProfileMain.class);

  private final transient UserInformationService userInformationService;
  private final transient life.qbic.identity.api.OidcLinkService oidcLinkService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final IdentityService identityService;
  private final transient life.qbic.datamanager.profilepicture.ProfilePictureService profilePictureService;
  private final transient MessageSourceNotificationFactory messageFactory;
  private UserProfileComponent profileComponent;
  private SettingsSection section;


  public UserProfileMain(
      @Autowired UserInformationService userInformationService,
      @Autowired MessageSourceNotificationFactory messageFactory,
      AuthenticationToUserIdTranslationService userIdTranslator,
      IdentityService identityService,
      @Autowired life.qbic.datamanager.profilepicture.ProfilePictureService profilePictureService,
      @Autowired life.qbic.identity.api.OidcLinkService oidcLinkService) {
    this.oidcLinkService = requireNonNull(oidcLinkService, "oidcLinkService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");
    this.messageFactory = requireNonNull(messageFactory);
    this.profilePictureService = requireNonNull(profilePictureService);
    addClassName("user-profile");
    this.identityService = identityService;
  }

  /**
   * Upon initialization of the main Component, the {@link UserProfileComponent} should be provided
   * with the {@link UserId} of the currently logged-in user
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = userIdTranslator.translateToUserId(authentication).orElseThrow();
    var userInfo = userInformationService.findById(userId).orElseThrow();
    if (nonNull(section)) {
      remove(section);
    }
    profileComponent = new UserProfileComponent(identityService, profilePictureService,
        messageFactory, userInformationService, oidcLinkService, userInfo, event.getLocation(),
        this::onUsernameChanged, this::onAccountReloaded);

    section = new SettingsSection("Profile",
        "Manage your personal information and linked accounts.");
    section.addContent(profileComponent);
    add(section);
  }

  /**
   * Re-renders the profile component with freshly read account data, so a link change is reflected in
   * place — the same treatment a username change gets, for the same reason.
   */
  private void onAccountReloaded(life.qbic.identity.api.UserInfo refreshed) {
    if (section == null) {
      return;
    }
    remove(section);
    profileComponent = new UserProfileComponent(identityService, profilePictureService,
        messageFactory, userInformationService, oidcLinkService, refreshed,
        new com.vaadin.flow.router.Location("settings/profile"),
        this::onUsernameChanged, this::onAccountReloaded);
    section = new SettingsSection("Profile",
        "Manage your personal information and linked accounts.");
    section.addContent(profileComponent);
    add(section);
  }

  /**
   * Refreshes the account overview header in the surrounding settings layout in place, so a
   * username change is reflected everywhere without a full page reload.
   * <p>
   * The settings layout is a nested {@link RouterLayout} under the global shell, so the profile
   * view may be several levels deep; walk up the component tree to find it.
   */
  private void onUsernameChanged(String newUserName) {
    findSettingsLayout()
        .ifPresent(SettingsMainLayout::refreshAccountOverview);
    messageFactory.toast("profile.username.change.success", new Object[]{newUserName},
        getLocale()).open();
  }

  private Optional<SettingsMainLayout> findSettingsLayout() {
    Component current = this;
    while (current.getParent().isPresent()) {
      current = current.getParent().get();
      if (current instanceof SettingsMainLayout settingsMainLayout) {
        return Optional.of(settingsMainLayout);
      }
    }
    return Optional.empty();
  }

  /**
   * Renders the outcome of the most recent ORCID link attempt, if there is one.
   *
   * <h3>Consumed exactly once</h3>
   * <p>The value is removed as it is read. That is what keeps a retry honest: a failed attempt followed
   * by a successful one must show the success, not the failure that is still sitting in the session — and
   * a page refresh must not replay an outcome the user has already seen. The removal happens before the
   * toast is built, so a rendering failure cannot leave the message to reappear later.</p>
   *
   * <h3>Why the session and not the URL</h3>
   * <p>See {@code OidcLinkCompletionFilter#redirect}. Briefly: the outcome is decided in the request that
   * redirects here, so it needs no round trip through the address bar — and text in a query parameter
   * would let anyone put their own sentence into a trusted notification.</p>
   */
  private void processLinkOutcome() {
    // The outcome is written by a servlet filter, so it lives in the HTTP session. VaadinSession keeps
    // its own attribute map — reading the same key from VaadinSession silently returns null, which is
    // how a rejected link came to show the user nothing at all. The wrapped HTTP session is the shared
    // store on both sides.
    var vaadinSession = com.vaadin.flow.server.VaadinSession.getCurrent();
    if (vaadinSession == null) {
      return;
    }
    var httpSession = vaadinSession.getSession();
    if (httpSession == null) {
      return;
    }

    Object stored;
    try {
      stored = httpSession.getAttribute(OidcLinkController.OUTCOME);
    } catch (IllegalStateException invalidSession) {
      // The session was invalidated between the redirect and this read. Nothing to report, and nothing
      // worth an error page: the settings page still renders the true account state.
      return;
    }
    if (stored == null) {
      return;
    }
    // Remove first, so the value is consumed exactly once even if rendering fails below. A stale
    // outcome next to a fresh one is precisely how this feature starts telling users the wrong thing.
    httpSession.removeAttribute(OidcLinkController.OUTCOME);

    var outcome = stored instanceof life.qbic.identity.api.OidcLinkOutcomeMessage message
        ? message
        : null;
    if (outcome == null) {
      log.warn("Ignoring an ORCID link outcome of an unexpected type in the session");
      return;
    }

    String key = "profile.oidc.link." + outcome.messageKey();
    // Rendering is deferred to ui.access, so a toast failure must not be mistaken for "no outcome".
    getUI().ifPresent(ui -> ui.access(() -> {
      try {
        messageFactory.toast(key, new Object[]{}, getLocale()).open();
      } catch (RuntimeException e) {
        // A missing message key must not break the settings page: the account state it renders is
        // still correct, and a broken notification is not worth an error screen.
        log.error("Could not render the ORCID link outcome " + outcome, e);
      }
    }));
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    processLinkOutcome();
  }

}
