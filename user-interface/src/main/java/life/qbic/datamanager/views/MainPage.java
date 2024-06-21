package life.qbic.datamanager.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.AppRoutes.Projects;

/**
 * <b>Main Page</b>
 * <p>
 * Dummy main landing page after login, if no path has been provided.
 * <p>
 * Represents the "root" route, the top entry point of our page that has no semantic information and
 * will redirect to a default entry page that we want to show the logged in user.
 *
 * @since 1.0.0
 */
@Route(value = "/landing", layout = UserMainLayout.class)
@PermitAll
public class MainPage extends Div implements BeforeEnterObserver {

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (event.getLocation().getPath().isBlank()) { // Just to be sure there are no params
      // Forward to the default entry page we want to show after login and not specifying any
      // specific navigation target
      event.forwardTo(Projects.PROJECTS);
    }
  }
}
