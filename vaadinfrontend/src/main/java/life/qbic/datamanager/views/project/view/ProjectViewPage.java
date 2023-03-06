package life.qbic.datamanager.views.project.view;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import java.io.Serial;
import javax.annotation.security.PermitAll;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project view page that shows project information and additional components to manage project
 * data.
 *
 * @since 1.0.0
 */
@Route(value = "projects/:projectId?", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/views/project/project-view.css")
public class ProjectViewPage extends Div implements
    BeforeEnterObserver, HasErrorParameter<ApplicationException> {

  @Serial
  private static final long serialVersionUID = 3402433356187177105L;

  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);

  private final transient ProjectViewHandler handler;

  public ProjectViewPage(@Autowired ProjectDetailsComponent projectDetailsComponent, @Autowired
  ProjectLinksComponent projectLinksComponent) {
    handler = new ProjectViewHandler(projectDetailsComponent, projectLinksComponent);
    add(projectDetailsComponent);
    add(projectLinksComponent);
    setPageStyles();
    setComponentStyles(projectDetailsComponent, projectLinksComponent);
    log.debug(
        String.format("New instance for project view (#%s) created with detail component (#%s)",
            System.identityHashCode(this), System.identityHashCode(projectDetailsComponent)));
  }

  public void setPageStyles() {
    addClassNames("project-view-page");
  }

  public void setComponentStyles(ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent) {
    projectDetailsComponent.setStyles("project-details-component");
    projectLinksComponent.setStyles("project-links-component");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    beforeEnterEvent.getRouteParameters().get("projectId")
        .ifPresentOrElse(handler::routeParameter, () -> {
          throw new ApplicationException() {
            @Override
            public ErrorCode errorCode() {
              return ErrorCode.INVALID_PROJECT_CODE;
            }

            @Override
            public ErrorParameters errorParameters() {
              return ErrorParameters.create();
            }
          };
        });
  }

  @Override
  public int setErrorParameter(BeforeEnterEvent beforeEnterEvent,
      ErrorParameter<ApplicationException> errorParameter) {
    return 0;
  }
}
