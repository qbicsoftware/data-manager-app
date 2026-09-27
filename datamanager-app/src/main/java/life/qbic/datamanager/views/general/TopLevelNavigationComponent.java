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
 * ({@code projects/list}) and the user groups hub ({@code groups}). The component is rendered in
 * the navbar of every outer (non-project-specific) layout — {@link UserMainLayout} for the
 * projects overview and {@code GroupsMainLayout} for the groups hub — so a user sees the same
 * top-level navigation wherever they are in one of those areas.
 * <p>
 * Each entry is a real {@link RouterLink} using {@link HighlightConditions#locationPrefix()}, so
 * the active tab is highlighted based on the current location without any manual tab-to-route
 * syncing: "Projects" is highlighted on {@code projects/list}, "Groups" on every {@code groups*}
 * route (list, creation page and group detail).
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