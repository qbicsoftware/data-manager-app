package life.qbic.datamanager.views.groups;

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
import life.qbic.datamanager.views.general.HomeLink;
import life.qbic.datamanager.views.general.footer.FooterComponent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> Groups Main Layout </b>
 * <p>
 * The layout hosting the top-level Groups area. It provides a persistent, non-collapsible aside
 * column with the {@link GroupsNavigationComponent} on the left and renders the currently
 * selected group route in the content area on the right.
 * <p>
 * The layout is the parent layout of the group routes, e.g. {@code /groups},
 * {@code /groups/new} and {@code /groups/:groupId}. The selected tab is kept in sync with the
 * active route via {@link #beforeEnter(BeforeEnterEvent)}.
 */
@PermitAll
public class GroupsMainLayout extends DataManagerLayout implements BeforeEnterObserver {

  private final GroupsNavigationComponent groupsNavigationComponent = new GroupsNavigationComponent();

  public GroupsMainLayout(@Autowired AuthenticationContext authenticationContext,
      @Autowired FooterComponent footerComponent,
      @Autowired AnnouncementService announcementService) {
    super(requireNonNull(footerComponent), announcementService);
    Objects.requireNonNull(authenticationContext);
    Span navBarTitle = new Span("Groups");
    navBarTitle.setClassName("navbar-title");
    DataManagerMenu dataManagerMenu = new DataManagerMenu(authenticationContext);
    addToNavbar(new HomeLink(), navBarTitle, dataManagerMenu);
    addClassName("groups-main-layout");
    setAside(groupsNavigationComponent);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    groupsNavigationComponent.selectTabFor(event.getNavigationTarget());
  }
}