package life.qbic.datamanager.views.project.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import java.io.Serial;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project view page that shows project information and additional components to manage project
 * data.
 *
 * @since 1.0.0
 */
@Route(value = "projects/view", layout = MainLayout.class)
@PermitAll
public class ProjectViewPage extends Div implements
    HasUrlParameter<String> {

  private static final String ROUTE = "projects/view";

  @Serial
  private static final long serialVersionUID = 3402433356187177105L;

  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);

  private transient final ProjectViewHandler handler;

  public ProjectViewPage(@Autowired ProjectDetailsComponent projectDetailsComponent, @Autowired
      ProjectLinksComponent projectLinksComponent) {
    handler = new ProjectViewHandler(projectDetailsComponent, projectLinksComponent);
    add(projectDetailsComponent);
    add(projectLinksComponent);

    log.debug(
        String.format("New instance for project view (#%s) created with detail component (#%s)",
            System.identityHashCode(this), System.identityHashCode(projectDetailsComponent)));
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, String s) {
    handler.routeParameter(s);

    log.debug("Route '" + ROUTE + "' called with parameter '" + s + "'");
  }
}
