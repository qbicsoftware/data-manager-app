package life.qbic.datamanager.views.project.view.pages.experiment;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.project.view.ProjectViewPage;
import life.qbic.datamanager.views.project.view.pages.experiment.components.ExperimentDetailsComponent;
import life.qbic.datamanager.views.project.view.pages.experiment.components.ExperimentListComponent;
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
@Route(value = "projects/:projectId?/experiments", layout = ProjectViewPage.class)
@PermitAll
//ToDo Move CSS into own class
@CssImport("./styles/views/project/project-view.css")
public class ExperimentInformationPage extends Div implements RouterLayout {

  @Serial
  private static final long serialVersionUID = -3443064087502678981L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  private final transient ExperimentInformationPageHandler experimentInformationPageHandler;

  public ExperimentInformationPage(@Autowired ExperimentDetailsComponent experimentDetailsComponent,
      @Autowired ExperimentListComponent experimentListComponent) {
    Objects.requireNonNull(experimentDetailsComponent);
    Objects.requireNonNull(experimentListComponent);
    add(experimentDetailsComponent);
    add(experimentListComponent);
    setComponentStyles(experimentDetailsComponent, experimentListComponent);
    experimentInformationPageHandler = new ExperimentInformationPageHandler(
        experimentDetailsComponent, experimentListComponent);
    log.debug(String.format(
        "\"New instance for Experiment Information page (#%s) created with Experiment Details Component (#%s) and Experiment List Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(experimentDetailsComponent),
        System.identityHashCode(experimentListComponent)));
  }

  public void projectId(String projectId) {
    experimentInformationPageHandler.setProjectId(projectId);
  }

  public void setComponentStyles(ExperimentDetailsComponent experimentDetailsComponent,
      ExperimentListComponent experimentListComponent) {
    experimentDetailsComponent.setStyles("experiment-details-component");
    experimentListComponent.setStyles("experiment-list-component");
  }

  private final class ExperimentInformationPageHandler {

    private final ExperimentDetailsComponent experimentDetailsComponent;
    private final ExperimentListComponent experimentListComponent;

    public ExperimentInformationPageHandler(ExperimentDetailsComponent experimentDetailsComponent,
        ExperimentListComponent experimentListComponent) {
      this.experimentDetailsComponent = experimentDetailsComponent;
      this.experimentListComponent = experimentListComponent;
    }

    public void setProjectId(String projectId) {
      experimentDetailsComponent.projectId(projectId);
      experimentListComponent.projectId(projectId);
    }
  }

}
