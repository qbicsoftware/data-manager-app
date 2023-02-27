package life.qbic.datamanager.views.project.overview;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import java.io.Serial;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.project.overview.components.ProjectOverviewComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project view page that shows project information and additional components to manage project
 * data.
 *
 * @since 1.0.0
 */
@PageTitle("Project Overview")
@Route(value = AppRoutes.PROJECTS, layout = MainLayout.class)
@RouteAlias(value = AppRoutes.PROJECTS_ALIAS)
@PermitAll
public class ProjectOverviewPage extends Div {
  @Serial
  private static final long serialVersionUID = 4625607082710157069L;

  private static final Logger log = LoggerFactory.logger(ProjectOverviewPage.class);

  public ProjectOverviewPage(@Autowired ProjectOverviewComponent projectOverviewComponent) {
    add(projectOverviewComponent);
  }

}
