package life.qbic.datamanager.views.groups;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import java.util.HashMap;
import java.util.Map;

/**
 * Groups Navigation Component
 * <p>
 * Vertical navigation menu for the top-level Groups area. Each entry is a real {@link RouterLink}
 * to a group route, so sections remain deep-linkable and the current section can be highlighted
 * based on the active route. Additional group categories can be added by registering further tabs
 * via {@link #addTab(String, VaadinIcon, Class)}.
 */
public class GroupsNavigationComponent extends Div {

  private final Tabs tabs = new Tabs();
  private final Map<Class<?>, Tab> tabsByNavigationTarget = new HashMap<>();

  public GroupsNavigationComponent() {
    addClassName("groups-navigation-component");
    tabs.addClassName("groups-navigation-tabs");
    tabs.getElement().setAttribute("orientation", "vertical");
    addTab("My Groups", VaadinIcon.USERS, MyGroupsMain.class);
    add(tabs);
  }

  private void addTab(String label, VaadinIcon icon, Class<? extends Component> navigationTarget) {
    RouterLink link = new RouterLink();
    link.add(new Icon(icon), new Text(label));
    link.setRoute(navigationTarget);
    Tab tab = new Tab(link);
    tab.addClassName("groups-nav-tab");
    tabs.add(tab);
    tabsByNavigationTarget.put(navigationTarget, tab);
  }

  /**
   * Marks the tab matching the given navigation target as the selected one.
   *
   * @param navigationTarget the class of the currently displayed route target
   */
  public void selectTabFor(Class<?> navigationTarget) {
    // the create page and the group detail page belong to the My Groups tab
    if (navigationTarget == NewGroupMain.class || navigationTarget == GroupDetailMain.class) {
      navigationTarget = MyGroupsMain.class;
    }
    Tab tab = tabsByNavigationTarget.get(navigationTarget);
    if (tab != null) {
      tabs.setSelectedTab(tab);
    }
  }
}