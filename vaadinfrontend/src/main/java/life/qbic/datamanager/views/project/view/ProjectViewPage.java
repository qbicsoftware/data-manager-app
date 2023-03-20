package life.qbic.datamanager.views.project.view;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import java.io.Serial;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.project.view.pages.experiment.ExperimentInformationPage;
import life.qbic.datamanager.views.project.view.pages.projectinformation.ProjectInformationPage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectManagementException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project view page that shows project information and additional components to manage project
 * data.
 *
 * @since 1.0.0
 */
@Route(value = "projects/:projectId?")
@PermitAll
@ParentLayout(MainLayout.class)
@CssImport("./styles/views/project/project-view.css")
public class ProjectViewPage extends Div implements BeforeEnterObserver,
    HasErrorParameter<ApplicationException>, RouterLayout {

  @Serial
  private static final long serialVersionUID = 3402433356187177105L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  private final transient ProjectViewHandler handler;

  public ProjectViewPage(@Autowired ProjectInformationPage projectInformationPage,
      @Autowired ExperimentInformationPage experimentInformationPage) {
    Objects.requireNonNull(projectInformationPage);
    Objects.requireNonNull(experimentInformationPage);
    setPageStyles(projectInformationPage, experimentInformationPage);
    handler = new ProjectViewHandler(projectInformationPage, experimentInformationPage);
    log.debug(
        String.format(
            "New instance for project view (#%s) created with project information page (#%s) and experiment information page (#%s)",
            System.identityHashCode(this), System.identityHashCode(projectInformationPage),
            System.identityHashCode(experimentInformationPage)));
  }

  public void setPageStyles(ProjectInformationPage projectInformationPage,
      ExperimentInformationPage experimentInformationPage) {
    projectInformationPage.addClassName("project-view-page");
    experimentInformationPage.addClassName("project-view-page");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    beforeEnterEvent.getRouteParameters().get("projectId")
        .ifPresentOrElse(handler::projectId, () -> {
          throw new ProjectManagementException("no project id provided");
        });
  }

  private void setComponentContext(String projectId) {
    this.handler.projectId(projectId);
  }

  @Override
  public int setErrorParameter(BeforeEnterEvent beforeEnterEvent,
      ErrorParameter<ApplicationException> errorParameter) {
    return 0;
  }
}
