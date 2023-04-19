package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent;
import life.qbic.datamanager.views.projects.project.info.ProjectDetailsComponent;
import life.qbic.datamanager.views.projects.project.info.ProjectLinksComponent;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment Information page
 * <p>
 * This page hosts the components necessary to show and update the
 * {@link life.qbic.projectmanagement.domain.project.experiment.Experiment} information associated
 * with a {@link life.qbic.projectmanagement.domain.project.Project} via the provided
 * {@link life.qbic.projectmanagement.domain.project.ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments", layout = ProjectViewPage.class)
@PermitAll
public class ExperimentInformationPage extends Div implements RouterLayout {

  @Serial
  private static final long serialVersionUID = -3443064087502678981L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  private final transient ExperimentInformationPageHandler experimentInformationPageHandler;

  public ExperimentInformationPage(@Autowired ProjectNavigationBarComponent projectNavigationBarComponent, @Autowired ExperimentDetailsComponent experimentDetailsComponent,
      @Autowired ExperimentListComponent experimentListComponent) {
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(experimentDetailsComponent);
    Objects.requireNonNull(experimentListComponent);
    setupBoard(projectNavigationBarComponent, experimentDetailsComponent, experimentListComponent);
    experimentInformationPageHandler = new ExperimentInformationPageHandler(projectNavigationBarComponent,
        experimentDetailsComponent, experimentListComponent);
    log.debug(String.format(
        "\"New instance for Experiment Information page (#%s) created with Project Navigation Bar Component (#%s) and Experiment Details Component (#%s) and Experiment List Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent), System.identityHashCode(experimentDetailsComponent),
        System.identityHashCode(experimentListComponent)));
  }

  private void setupBoard(ProjectNavigationBarComponent projectNavigationBarComponent, ExperimentDetailsComponent experimentDetailsComponent, ExperimentListComponent experimentListComponent) {
    Board board = new Board();

    Row rootRow = new Row();

    VerticalLayout mainComponents = new VerticalLayout();
    mainComponents.setPadding(false);
    mainComponents.add(projectNavigationBarComponent, experimentDetailsComponent);

    rootRow.add(mainComponents, 3);
    rootRow.add(experimentListComponent, 1);

    board.add(rootRow);

    board.setSizeFull();

    add(board);
  }

  public void projectId(ProjectId projectId) {
    experimentInformationPageHandler.setProjectId(projectId);
  }

  public void setComponentStyles(ExperimentDetailsComponent experimentDetailsComponent,
      ExperimentListComponent experimentListComponent) {
    experimentDetailsComponent.setId("experiment-details-component");
    experimentListComponent.setId("experiment-list-component");
  }

  private final class ExperimentInformationPageHandler {

    private final ProjectNavigationBarComponent projectNavigationBarComponent;
    private final ExperimentDetailsComponent experimentDetailsComponent;
    private final ExperimentListComponent experimentListComponent;

    public ExperimentInformationPageHandler(ProjectNavigationBarComponent projectNavigationBarComponent, ExperimentDetailsComponent experimentDetailsComponent,
        ExperimentListComponent experimentListComponent) {
      this.projectNavigationBarComponent = projectNavigationBarComponent;
      this.experimentDetailsComponent = experimentDetailsComponent;
      this.experimentListComponent = experimentListComponent;
    }

    public void setProjectId(ProjectId projectId) {
      projectNavigationBarComponent.projectId(projectId);
      experimentDetailsComponent.projectId(projectId);
      experimentListComponent.projectId(projectId);
    }
  }

}
