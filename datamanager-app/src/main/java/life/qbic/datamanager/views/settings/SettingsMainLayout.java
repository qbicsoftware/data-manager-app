package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

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
import life.qbic.identity.api.UserInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> Settings Main Layout </b>
 * <p>
 * The layout hosting all user-specific settings sections. It provides a persistent, non-collapsible
 * aside column with the {@link SettingsNavigationComponent} on the left and renders the currently
 * selected settings section in the content area on the right.
 * <p>
 * The layout is the parent layout of the settings section routes, e.g. {@code /settings/profile},
 * {@code /settings/api-tokens} and {@code /settings/external-providers}. The selected tab is kept
 * in sync with the active route via {@link #beforeEnter(BeforeEnterEvent)}.
 */
@PermitAll
public class SettingsMainLayout extends DataManagerLayout implements BeforeEnterObserver {

  private final SettingsNavigationComponent settingsNavigationComponent = new SettingsNavigationComponent();

  public SettingsMainLayout(@Autowired AuthenticationContext authenticationContext,
      @Autowired UserInformationService userInformationService,
      @Autowired FooterComponentFactory footerComponentFactory,
      @Autowired AnnouncementService announcementService) {
    super(requireNonNull(footerComponentFactory), announcementService);
    Objects.requireNonNull(authenticationContext);
    Objects.requireNonNull(userInformationService);
    Span navBarTitle = new Span("Settings");
    navBarTitle.setClassName("navbar-title");
    DataManagerMenu dataManagerMenu = new DataManagerMenu(authenticationContext);
    addToNavbar(navBarTitle, dataManagerMenu);
    addClassName("settings-main-layout");
    setAside(settingsNavigationComponent);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    settingsNavigationComponent.selectTabFor(event.getNavigationTarget());
  }
}