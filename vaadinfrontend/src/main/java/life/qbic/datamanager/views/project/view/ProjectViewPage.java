package life.qbic.datamanager.views.project.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import java.io.Serial;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Route(value = "projects/view", layout = MainLayout.class)
@PermitAll
public class ProjectViewPage extends Div implements
    HasUrlParameter<String> {

  private static final String ROUTE = "projects/view";

  @Serial
  private static final long serialVersionUID = 3402433356187177105L;

  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);

  private final ProjectViewHandler handler;

  public ProjectViewPage() {
    ProjectDetailsComponent projectDetailsComponent = new ProjectDetailsComponent(null);
    handler = new ProjectViewHandler(this, projectDetailsComponent);
    add(projectDetailsComponent);
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, String s) {
    log.debug("Route '" + ROUTE + "' called with parameter '" + s + "'");
    handler.routeParameter(s);
  }
}
