package life.qbic.datamanager.views.settings;

import static java.util.Objects.nonNull;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * My Groups Main
 * <p>
 * This component hosts the "My Groups" settings section of the current logged-in user. It lists
 * the groups the user belongs to together with their internal role and provides access to group
 * management actions.
 *
 * @since 1.19.0
 */
@Route(value = AppRoutes.GroupsRoutes.MY_GROUPS, layout = SettingsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Settings · My Groups")
public class MyGroupsMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 6902406260794581649L;

  private SettingsSection section;

  public MyGroupsMain() {
    addClassName("my-groups");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (nonNull(section)) {
      remove(section);
    }
    section = new SettingsSection("My Groups",
        "Groups you belong to and manage.");
    add(section);
  }
}