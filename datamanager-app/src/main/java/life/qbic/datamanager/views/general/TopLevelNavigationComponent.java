package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import java.util.Objects;
import life.qbic.datamanager.views.groups.MyGroupsMain;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;

/**
 * <b>Top-level navigation</b>
 * <p>
 * GitHub-style top bar tabs for the application's primary surfaces: the project overview
 * ({@code projects/list}) and the user groups hub ({@code groups}). It is rendered once in the
 * navbar of the single outer shell {@code UserMainLayout}, which hosts both the projects outline
 * and — via the nested {@code GroupsMainLayout} — the groups hub, so the same top-level
 * navigation is visible wherever the user is in one of those areas.
 * <p>
 * Each entry is a real {@link RouterLink} using {@link HighlightConditions#locationPrefix()}, so
 * the active tab is highlighted based on the current location without any manual tab-to-route
 * syncing: "Projects" is highlighted on {@code projects/list}, "Groups" on every {@code groups*}
 * route (list, creation page and group detail), including the group routes nested under
 * {@code GroupsMainLayout} which render inside this same outer shell.
 */
public class TopLevelNavigationComponent extends Div {

  private final Div tabs = new Div();

  public TopLevelNavigationComponent() {
    addClassName("top-level-navigation");
    tabs.addClassName("top-level-navigation-tabs");
    addTab("Projects", ProjectOverviewMain.class);
    addTab("Groups", MyGroupsMain.class);
    add(tabs);
  }

  private void addTab(String label, Class<? extends Component> target) {
    RouterLink link = new RouterLink();
    link.add(new Span(label));
    link.setRoute(target);
    link.addClassName("top-level-navigation-tab");
    link.setHighlightCondition(HighlightConditions.locationPrefix());
    tabs.add(link);
  }

  /**
   * Returns the navigation tabs container so layouts can keep a direct reference for testing.
   *
   * @return the tabs container
   */
  public Div tabs() {
    return Objects.requireNonNull(tabs);
  }
}