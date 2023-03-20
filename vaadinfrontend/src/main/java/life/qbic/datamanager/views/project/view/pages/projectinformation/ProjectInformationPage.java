package life.qbic.datamanager.views.project.view.pages.projectinformation;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.project.view.ProjectNavigationBarComponent;
import life.qbic.datamanager.views.project.view.ProjectViewPage;
import life.qbic.datamanager.views.project.view.pages.projectinformation.components.ProjectDetailsComponent;
import life.qbic.datamanager.views.project.view.pages.projectinformation.components.ProjectLinksComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
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

  public ProjectInformationPage(
      @Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired ProjectDetailsComponent projectDetailsComponent,
      @Autowired ProjectLinksComponent projectLinksComponent) {
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(projectDetailsComponent);
    Objects.requireNonNull(projectLinksComponent);
    add(projectNavigationBarComponent);
    add(projectDetailsComponent);
    add(projectLinksComponent);
    setComponentStyles(projectNavigationBarComponent, projectDetailsComponent,
        projectLinksComponent);
    projectInformationPageHandler = new ProjectInformationPageHandler(projectNavigationBarComponent,
        projectDetailsComponent, projectLinksComponent);
    log.debug(String.format(
        "New instance for project Information Page (#%s) created with Project Details Component (#%s) and Project Links Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectDetailsComponent),
        System.identityHashCode(projectLinksComponent)));
  }

  public void projectId(String projectId) {
    projectInformationPageHandler.setProjectId(projectId);
  }

  public void setComponentStyles(ProjectNavigationBarComponent projectNavigationBarComponent,
      ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent) {
    projectNavigationBarComponent.setStyles("project-navigation-component");
    projectDetailsComponent.setStyles("project-details-component");
    projectLinksComponent.setStyles("project-links-component");
  }

  private final class ProjectInformationPageHandler {

    private final ProjectNavigationBarComponent projectNavigationBarComponent;
    private final ProjectDetailsComponent projectDetailsComponent;
    private final ProjectLinksComponent projectLinksComponent;

    public ProjectInformationPageHandler(
        ProjectNavigationBarComponent projectNavigationBarComponent,
        ProjectDetailsComponent projectDetailsComponent,
        ProjectLinksComponent projectLinksComponent) {
      this.projectNavigationBarComponent = projectNavigationBarComponent;
      this.projectDetailsComponent = projectDetailsComponent;
      this.projectLinksComponent = projectLinksComponent;
    }

    public void setProjectId(String projectId) {
      projectNavigationBarComponent.projectId(projectId);
      projectDetailsComponent.projectId(projectId);
      projectLinksComponent.projectId(projectId);
    }
  }

}
