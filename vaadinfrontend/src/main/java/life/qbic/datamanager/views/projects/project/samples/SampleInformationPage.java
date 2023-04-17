package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.datamanager.views.projects.project.info.ProjectDetailsComponent;
import life.qbic.datamanager.views.projects.project.info.ProjectLinksComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Information page
 * <p>
 * This page hosts the components necessary to show and update the actual sample information
 * associated with a provided {@link life.qbic.projectmanagement.domain.project.ProjectId} in the
 * URL
 */

@Route(value = "projects/:projectId?/samples", layout = ProjectViewPage.class)
@SpringComponent
@UIScope
@PermitAll
public class SampleInformationPage extends Div {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(SampleInformationPage.class);
  private final transient SampleInformationPageHandler sampleInformationPageHandler;

  public SampleInformationPage(@Autowired ProjectNavigationBarComponent projectNavigationBarComponent, @Autowired SampleOverviewComponent sampleOverviewComponent) {
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(sampleOverviewComponent);
    setupBoard(projectNavigationBarComponent, sampleOverviewComponent);
    sampleInformationPageHandler = new SampleInformationPageHandler(projectNavigationBarComponent, sampleOverviewComponent);
    log.debug(String.format(
        "\"New instance for Sample Information page (#%s) created with Project Navigation Bar Component (#%s) and Sample Overview Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent), System.identityHashCode(sampleOverviewComponent)));
  }

  private void setupBoard(ProjectNavigationBarComponent projectNavigationBarComponent, SampleOverviewComponent sampleOverviewComponent) {
    Board board = new Board();

    Row topRow = new Row();
    topRow.add(projectNavigationBarComponent, 3);
    topRow.add(new Div());

    Row secondRow = new Row();
    secondRow.add(sampleOverviewComponent, 4);

    board.add(topRow, secondRow);

    board.setSizeFull();

    add(board);
  }

  public void projectId(ProjectId projectId) {
    sampleInformationPageHandler.setProjectId(projectId);
  }

  public void setComponentStyles(SampleOverviewComponent sampleOverviewComponent) {
    sampleOverviewComponent.setId("sample-overview-component");
  }

  private final class SampleInformationPageHandler {

    ProjectNavigationBarComponent projectNavigationBarComponent;
    SampleOverviewComponent sampleOverviewComponent;

    public SampleInformationPageHandler(ProjectNavigationBarComponent projectNavigationBarComponent, SampleOverviewComponent sampleOverviewComponent) {
      this.sampleOverviewComponent = sampleOverviewComponent;
      this.projectNavigationBarComponent = projectNavigationBarComponent;
    }

    public void setProjectId(ProjectId projectId) {
      projectNavigationBarComponent.projectId(projectId);
      sampleOverviewComponent.projectId(projectId);
    }
  }

}
