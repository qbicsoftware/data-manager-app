package life.qbic.datamanager.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import java.util.Objects;
import life.qbic.datamanager.announcements.AnnouncementService;
import life.qbic.datamanager.views.account.PersonalAccessTokenMain;
import life.qbic.datamanager.views.account.UserProfileMain;
import life.qbic.datamanager.views.general.DataManagerMenu;
import life.qbic.datamanager.views.general.footer.FooterComponent;
import life.qbic.datamanager.views.groups.MyGroupsMain;
import life.qbic.datamanager.views.navigation.AppNavigationComponent;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.identity.api.UserInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>The home-scope shell layout</b>
 *
 * <p>Hosts every view outside a single project — the projects list ({@link ProjectOverviewMain}),
 * the groups hub and the settings hub (via their nested layouts), and {@link
 * PersonalAccessTokenMain}. It renders the prototype's two chrome rows:</p>
 *
 * <ol>
 *   <li>an identity bar — the {@code Data Manager} wordmark, the {@code Home} crumb and the account
 *       menu;</li>
 *   <li>the shell's navigation row {@code Projects · Groups · Settings}.</li>
 * </ol>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
@PermitAll
public class UserMainLayout extends DataManagerLayout implements BeforeEnterObserver {

  private final AppNavigationComponent navigation = AppNavigationComponent.forHome();

  public UserMainLayout(@Autowired AuthenticationContext authenticationContext,
      @Autowired UserInformationService userInformationService,
      @Autowired FooterComponent footerComponent,
      @Autowired AnnouncementService announcementService) {
    super(Objects.requireNonNull(footerComponent), announcementService);
    Objects.requireNonNull(userInformationService);
    addClassName("user-main-layout");

    Span breadcrumbs = new Span();
    breadcrumbs.addClassName("app-crumbs");
    Span home = new Span("Home");
    home.addClassNames("app-crumb", "app-crumb-current");
    breadcrumbs.add(home);

    Span identityBarRow = new Span(brand(), breadcrumbs,
        new DataManagerMenu(Objects.requireNonNull(authenticationContext)));
    identityBarRow.addClassName("app-identity-bar-row");
    Div identityBar = new Div(identityBarRow);
    identityBar.addClassName("app-identity-bar");

    Div chrome = new Div(identityBar, navigation);
    chrome.addClassName("app-chrome");
    addToNavbar(chrome);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    // The nav row highlights by scope, not by exact target: the groups and settings hubs have many
    // views under one destination.
    String path = beforeEnterEvent.getLocation().getPath();
    if (path.startsWith("groups")) {
      navigation.setActive(MyGroupsMain.class);
    } else if (path.startsWith("settings")) {
      navigation.setActive(UserProfileMain.class);
    } else {
      navigation.setActive(ProjectOverviewMain.class);
    }
  }

  private static Span brand() {
    Span brand = new Span();
    brand.addClassName("app-brand");
    RouterLink wordmark = new RouterLink();
    wordmark.setRoute(ProjectOverviewMain.class);
    wordmark.addClassName("app-wordmark");
    Span dot = new Span(".");
    dot.addClassName("app-wordmark-dot");
    wordmark.add(new Span("Data Manager"), dot);
    brand.add(wordmark);
    return brand;
  }
}
