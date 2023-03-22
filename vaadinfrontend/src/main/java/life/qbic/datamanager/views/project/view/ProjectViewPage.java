package life.qbic.datamanager.views.project.view;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import java.io.Serial;
import javax.annotation.security.PermitAll;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.project.view.components.ExperimentalDesignDetailComponent;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
import life.qbic.datamanager.views.project.view.components.ProjectNavigationBarComponent;
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
public class ProjectViewPage extends Div implements
    BeforeEnterObserver, HasErrorParameter<ApplicationException>, RouterLayout {

  @Serial
  private static final long serialVersionUID = 3402433356187177105L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  private final transient ProjectViewHandler handler;

  public ProjectViewPage(@Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired ProjectDetailsComponent projectDetailsComponent, @Autowired
  ProjectLinksComponent projectLinksComponent, @Autowired ExperimentalDesignDetailComponent
      experimentalDesignDetailComponent) {


    handler = new ProjectViewHandler(projectNavigationBarComponent, projectDetailsComponent,
        projectLinksComponent, experimentalDesignDetailComponent);

    Span projectTitle = new Span("QABCD: "+"Some Title is here");
    projectTitle.addClassNames("text-3xl","title-component");

    addComponentAsFirst(projectTitle);
    add(projectNavigationBarComponent);
    add(projectDetailsComponent);
    add(projectLinksComponent);


    setPageStyles();
    setComponentStyles(projectNavigationBarComponent, projectDetailsComponent,
        projectLinksComponent, experimentalDesignDetailComponent);
    setComponentStyles(projectNavigationBarComponent, projectDetailsComponent,
        projectLinksComponent,
        experimentalDesignDetailComponent);

    log.debug(
        String.format("New instance for project view (#%s) created with detail component (#%s)",
            System.identityHashCode(this), System.identityHashCode(projectDetailsComponent)));
  }

  public void setPageStyles() {
    addClassNames("project-view-page");
  }

  public void setComponentStyles(ProjectNavigationBarComponent projectNavigationBarComponent,
      ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent,
      ExperimentalDesignDetailComponent
          experimentalDesignDetailComponent) {

    projectNavigationBarComponent.setStyles("project-navigation-component");

    projectDetailsComponent.setStyles("project-details-component");
    projectLinksComponent.setStyles("project-links-component");
    //Todo Determine if we want to have separate styles for each component
    experimentalDesignDetailComponent.setStyles("experimental-design-component");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    beforeEnterEvent.getRouteParameters().get("projectId")
        .ifPresentOrElse(
            handler::projectId,
            () -> {
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
