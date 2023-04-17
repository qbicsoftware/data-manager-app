package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.ProjectId;
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
public class ProjectInformationPage extends Div implements RouterLayout {

  @Serial
  private static final long serialVersionUID = 5797835576569148873L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  private final transient ProjectInformationPageHandler projectInformationPageHandler;

  public ProjectInformationPage(@Autowired ProjectNavigationBarComponent projectNavigationBarComponent, @Autowired ProjectDetailsComponent projectDetailsComponent,
      @Autowired ProjectLinksComponent projectLinksComponent) {
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(projectDetailsComponent);
    Objects.requireNonNull(projectLinksComponent);
    setupBoard(projectNavigationBarComponent, projectDetailsComponent, projectLinksComponent);
    setComponentStyles(projectDetailsComponent, projectLinksComponent);
    projectInformationPageHandler = new ProjectInformationPageHandler(projectNavigationBarComponent, projectDetailsComponent,
        projectLinksComponent);
    log.debug(String.format(
        "New instance for project Information Page (#%s) created with Project Navigation Bar Component (#%s) and Project Details Component (#%s) and Project Links Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent), System.identityHashCode(projectDetailsComponent),
        System.identityHashCode(projectLinksComponent)));
  }

  private void setupBoard(ProjectNavigationBarComponent projectNavigationBarComponent, ProjectDetailsComponent projectDetailsComponent, ProjectLinksComponent projectLinksComponent) {
    Board board = new Board();

    Row topRow = new Row();
    topRow.add(projectNavigationBarComponent, 3);
    topRow.add(new Div());

    Row secondRow = new Row();
    secondRow.add(projectDetailsComponent, 3);
    secondRow.add(projectLinksComponent);

    board.add(topRow, secondRow);

    board.setSizeFull();

    add(board);
  }

  public void projectId(ProjectId projectId) {
    projectInformationPageHandler.setProjectId(projectId);
  }

  public void setComponentStyles(ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent) {
    projectDetailsComponent.setId("project-details-component");
    projectLinksComponent.setId("project-links-component");
  }

  private final class ProjectInformationPageHandler {

    private final ProjectNavigationBarComponent projectNavigationComponent;
    private final ProjectDetailsComponent projectDetailsComponent;
    private final ProjectLinksComponent projectLinksComponent;

    public ProjectInformationPageHandler(ProjectNavigationBarComponent projectNavigationComponent, ProjectDetailsComponent projectDetailsComponent,
        ProjectLinksComponent projectLinksComponent) {
      this.projectNavigationComponent = projectNavigationComponent;
      this.projectDetailsComponent = projectDetailsComponent;
      this.projectLinksComponent = projectLinksComponent;
    }

    public void setProjectId(ProjectId projectId) {
      projectNavigationComponent.projectId(projectId);
      projectDetailsComponent.projectId(projectId);
      projectLinksComponent.projectId(projectId);
    }
  }

}
