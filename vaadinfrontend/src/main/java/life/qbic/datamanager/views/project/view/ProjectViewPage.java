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
import life.qbic.datamanager.views.project.view.components.ExperimentDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ExperimentListComponent;
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

  public ProjectViewPage(@Autowired ProjectDetailsComponent projectDetailsComponent,
      @Autowired ExperimentDetailsComponent experimentDetailsComponent,
      @Autowired ProjectLinksComponent projectLinksComponent,
      @Autowired ExperimentListComponent experimentListComponent) {
    handler = new ProjectViewHandler(projectDetailsComponent, projectLinksComponent,
        experimentDetailsComponent, experimentListComponent);
    add(projectDetailsComponent);
    add(projectLinksComponent);
    //ToDo Replace with Dedicated Navbar component and routing
    initNavbar(projectDetailsComponent, experimentDetailsComponent, projectLinksComponent,
        experimentListComponent);
    add(navBar);
    setPageStyles();
    setComponentStyles(projectDetailsComponent, experimentDetailsComponent, projectLinksComponent,
        experimentListComponent);
    log.debug(
        String.format("New instance for project view (#%s) created with detail component (#%s)",
            System.identityHashCode(this), System.identityHashCode(projectDetailsComponent)));
  }

  private void initNavbar(ProjectDetailsComponent projectDetailsComponent,
      ExperimentDetailsComponent experimentDetailsComponent,
      ProjectLinksComponent projectLinksComponent,
      ExperimentListComponent experimentListComponent) {
    navBar.add(switchComponentsButton);
    switchComponentsButton.addClickListener(clickEvent -> {
      switchDetailsComponents(projectDetailsComponent, experimentDetailsComponent);
      switchListComponents(projectLinksComponent, experimentListComponent);
    });
  }

  private void switchDetailsComponents(ProjectDetailsComponent projectDetailsComponent,
      ExperimentDetailsComponent experimentDetailsComponent) {
    if (this.getChildren().toList().contains(projectDetailsComponent)) {
      add(experimentDetailsComponent);
      remove(projectDetailsComponent);
    } else if (this.getChildren().toList().contains(experimentDetailsComponent)) {
      remove(experimentDetailsComponent);
      add(projectDetailsComponent);
    }
  }

  private void switchListComponents(ProjectLinksComponent projectLinksComponent,
      ExperimentListComponent experimentListComponent) {
    if (this.getChildren().toList().contains(projectLinksComponent)) {
      add(experimentListComponent);
      remove(projectLinksComponent);
    } else if (this.getChildren().toList().contains(experimentListComponent)) {
      remove(experimentListComponent);
      add(projectLinksComponent);
    }
  }

  public void setPageStyles() {
    addClassNames("project-view-page");
  }

  public void setComponentStyles(ProjectDetailsComponent projectDetailsComponent,
      ExperimentDetailsComponent experimentDetailsComponent,
      ProjectLinksComponent projectLinksComponent,
      ExperimentListComponent experimentListComponent) {
    projectDetailsComponent.setStyles("project-details-component");
    projectLinksComponent.setStyles("project-links-component");
    //Todo Determine if we want to have seperate styles for each component
    experimentDetailsComponent.setStyles("experiment-details-component");
    experimentListComponent.setStyles("experiment-list-component");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    beforeEnterEvent.getRouteParameters().get("projectId")
        .ifPresentOrElse(handler::routeParameter, () -> {
          throw new ProjectManagementException("no project id provided");
        });
  }
}
