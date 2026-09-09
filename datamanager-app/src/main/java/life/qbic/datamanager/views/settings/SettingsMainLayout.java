package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import java.util.Objects;
import life.qbic.datamanager.announcements.AnnouncementService;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.general.DataManagerMenu;
import life.qbic.datamanager.views.general.footer.FooterComponentFactory;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b> Settings Main Layout </b>
 * <p>
 * The layout hosting all user-specific settings sections. It provides a persistent, non-collapsible
 * aside column with the {@link AccountOverviewHeader} and the {@link SettingsNavigationComponent}
 * on the left and renders the currently selected settings section in the content area on the right.
 * <p>
 * The layout is the parent layout of the settings section routes, e.g. {@code /settings/profile},
 * {@code /settings/api-tokens} and {@code /settings/external-providers}. The selected tab is kept
 * in sync with the active route via {@link #beforeEnter(BeforeEnterEvent)}.
 */
@PermitAll
public class SettingsMainLayout extends DataManagerLayout implements BeforeEnterObserver {

  private final SettingsNavigationComponent settingsNavigationComponent = new SettingsNavigationComponent();
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final transient UserInformationService userInformationService;

  public SettingsMainLayout(@Autowired AuthenticationContext authenticationContext,
      @Autowired UserInformationService userInformationService,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired FooterComponentFactory footerComponentFactory,
      @Autowired AnnouncementService announcementService) {
    super(requireNonNull(footerComponentFactory), announcementService);
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
    Objects.requireNonNull(authenticationContext);
    Span navBarTitle = new Span("Settings");
    navBarTitle.setClassName("navbar-title");
    DataManagerMenu dataManagerMenu = new DataManagerMenu(authenticationContext);
    addToNavbar(navBarTitle, dataManagerMenu);
    addClassName("settings-main-layout");
    setAside(buildAside());
  }

  private Div buildAside() {
    Div aside = new Div();
    aside.addClassName("settings-aside");
    aside.add(new AccountOverviewHeader(loadCurrentUser()), settingsNavigationComponent);
    return aside;
  }

  private UserInfo loadCurrentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = userIdTranslator.translateToUserId(authentication).orElseThrow();
    return userInformationService.findById(userId).orElseThrow();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    settingsNavigationComponent.selectTabFor(event.getNavigationTarget());
  }
}