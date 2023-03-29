package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project Information page
 * <p>
 * This page hosts the components necessary to show and update the actual
 * {@link life.qbic.projectmanagement.domain.project.Project} information associated with a provided
 * {@link life.qbic.projectmanagement.domain.project.ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/info", layout = ProjectViewPage.class)
@PermitAll
//ToDo Move CSS into own class
@CssImport("./styles/views/project/project-view.css")
public class ProjectInformationPage extends Div implements RouterLayout {

  @Serial
  private static final long serialVersionUID = 5797835576569148873L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  private final transient ProjectInformationPageHandler projectInformationPageHandler;

  public ProjectInformationPage(@Autowired ProjectDetailsComponent projectDetailsComponent,
      @Autowired ProjectLinksComponent projectLinksComponent) {
    Objects.requireNonNull(projectDetailsComponent);
    Objects.requireNonNull(projectLinksComponent);
    add(projectDetailsComponent);
    add(projectLinksComponent);
    setComponentStyles(projectDetailsComponent, projectLinksComponent);
    projectInformationPageHandler = new ProjectInformationPageHandler(projectDetailsComponent,
        projectLinksComponent);
    log.debug(String.format(
        "New instance for project Information Page (#%s) created with Project Details Component (#%s) and Project Links Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectDetailsComponent),
        System.identityHashCode(projectLinksComponent)));
  }

  public void projectId(String projectId) {
    projectInformationPageHandler.setProjectId(projectId);
  }

  public void setComponentStyles(ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent) {
    projectDetailsComponent.setId("project-details-component");
    projectLinksComponent.setId("project-links-component");
  }

  private final class ProjectInformationPageHandler {

    private final ProjectDetailsComponent projectDetailsComponent;
    private final ProjectLinksComponent projectLinksComponent;

    public ProjectInformationPageHandler(ProjectDetailsComponent projectDetailsComponent,
        ProjectLinksComponent projectLinksComponent) {
      this.projectDetailsComponent = projectDetailsComponent;
      this.projectLinksComponent = projectLinksComponent;
    }

    public void setProjectId(String projectId) {
      projectDetailsComponent.projectId(projectId);
      projectLinksComponent.projectId(projectId);
    }
  }

}
