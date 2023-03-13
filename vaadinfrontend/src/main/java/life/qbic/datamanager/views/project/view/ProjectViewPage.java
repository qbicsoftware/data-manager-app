package life.qbic.datamanager.views.project.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import java.io.Serial;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.project.view.components.ExperimentalDesignDetailComponent;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
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
@Route(value = "projects/:projectId?", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/views/project/project-view.css")
public class ProjectViewPage extends Div implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 3402433356187177105L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  private final Button switchComponentsButton = new Button("switchComponent");
  //Todo Generate Dedicated navbar for switching between the pages of a project
  private final HorizontalLayout navBar = new HorizontalLayout();
  private final transient ProjectViewHandler handler;

  public ProjectViewPage(@Autowired ProjectDetailsComponent projectDetailsComponent, @Autowired
  ProjectLinksComponent projectLinksComponent,
      @Autowired ExperimentalDesignDetailComponent experimentalDesignDetailComponent) {
    handler = new ProjectViewHandler(projectDetailsComponent,
        projectLinksComponent, experimentalDesignDetailComponent);
    add(projectDetailsComponent);
    add(projectLinksComponent);
    //ToDo Replace with Dedicated Navbar component and routing
    initNavbar(projectDetailsComponent, experimentalDesignDetailComponent);
    add(navBar);
    setPageStyles();
    setComponentStyles(projectDetailsComponent, projectLinksComponent,
        experimentalDesignDetailComponent);
    log.debug(
        String.format("New instance for project view (#%s) created with detail component (#%s)",
            System.identityHashCode(this), System.identityHashCode(projectDetailsComponent)));
  }

  private void initNavbar(ProjectDetailsComponent projectDetailsComponent,
      ExperimentalDesignDetailComponent experimentalDesignDetailComponent) {
    navBar.add(switchComponentsButton);
    switchComponentsButton.addClickListener(
        clickEvent -> switchDetailsComponents(projectDetailsComponent,
            experimentalDesignDetailComponent));
  }

  private void switchDetailsComponents(ProjectDetailsComponent projectDetailsComponent,
      ExperimentalDesignDetailComponent experimentalDesignDetailComponent) {
    if (this.getChildren().toList().contains(projectDetailsComponent)) {
      add(experimentalDesignDetailComponent);
      remove(projectDetailsComponent);
    } else if (this.getChildren().toList().contains(experimentalDesignDetailComponent)) {
      remove(experimentalDesignDetailComponent);
      add(projectDetailsComponent);
    }
  }

  public void setPageStyles() {
    addClassNames("project-view-page");
  }

  public void setComponentStyles(ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent,
      ExperimentalDesignDetailComponent experimentalDesignDetailComponent) {
    projectDetailsComponent.setStyles("project-details-component");
    projectLinksComponent.setStyles("project-links-component");
    //Todo Determine if we want to have seperate styles for each component
    experimentalDesignDetailComponent.setStyles("experimental-design-component");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    beforeEnterEvent.getRouteParameters().get("projectId")
        .ifPresentOrElse(
            handler::routeParameter,
            () -> {
              throw new ProjectManagementException("no project id provided");
            });
  }
}
