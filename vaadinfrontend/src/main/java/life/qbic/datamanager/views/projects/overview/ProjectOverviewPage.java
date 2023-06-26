package life.qbic.datamanager.views.projects.overview;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.ZoneId;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollection;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project view page that shows project information and additional components to manage project
 * data.
 *
 * @since 1.0.0
 */
@PageTitle("Project Overview")
@Route(value = Projects.PROJECTS, layout = MainLayout.class)
@PermitAll
public class ProjectOverviewPage extends Div {

  @Serial
  private static final long serialVersionUID = 4625607082710157069L;

  private static final Logger log = LoggerFactory.logger(ProjectOverviewPage.class);

  public ProjectOverviewPage(@Autowired ProjectInformationService projectInformationService) {
    add(ProjectCollection.create("Projects", ZoneId.of("UTC"), projectInformationService));
    stylePage();
  }

  private void stylePage() {
    this.setWidthFull();
    this.setHeightFull();
  }

}
