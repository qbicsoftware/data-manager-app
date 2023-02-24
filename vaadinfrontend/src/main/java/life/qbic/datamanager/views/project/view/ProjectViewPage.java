package life.qbic.datamanager.views.project.view;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import java.io.Serial;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.project.view.components.ExperimentalDesignDetailComponent;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
import life.qbic.datamanager.views.project.view.components.ProjectNavigationBarComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project view page that shows project information and additional components to manage project
 * data.
 *
 * @since 1.0.0
 */
@Route(value = "projects/view", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/views/project/project-view.css")
public class ProjectViewPage extends Div implements
    HasUrlParameter<String> {

  private static final String ROUTE = "projects/view";
  @Serial
  private static final long serialVersionUID = 3402433356187177105L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);

  //ToDo Add ClickListeners to Navbar Buttons to enable switch between components
  private final transient ProjectViewHandler handler;

  public ProjectViewPage(@Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired ProjectDetailsComponent projectDetailsComponent,
      @Autowired ProjectLinksComponent projectLinksComponent,
      @Autowired ExperimentalDesignDetailComponent experimentalDesignDetailComponent) {
    handler = new ProjectViewHandler(projectNavigationBarComponent, projectDetailsComponent,
        projectLinksComponent, experimentalDesignDetailComponent);
    add(projectNavigationBarComponent);
    //ToDo Add swapping method between components
    add(experimentalDesignDetailComponent);
    add(projectLinksComponent);
    setPageStyles();
    setComponentStyles(projectDetailsComponent, projectLinksComponent,
        projectNavigationBarComponent, experimentalDesignDetailComponent);
    log.debug(
        String.format("New instance for project view (#%s) created with detail component (#%s)",
            System.identityHashCode(this), System.identityHashCode(projectDetailsComponent)));
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, String s) {
    handler.routeParameter(s);

    log.debug("Route '" + ROUTE + "' called with parameter '" + s + "'");
  }

  public void setPageStyles() {
    addClassNames("project-view-page");
  }

  public void setComponentStyles(ProjectDetailsComponent projectDetailsComponent,
      ProjectLinksComponent projectLinksComponent,
      ProjectNavigationBarComponent projectNavigationBarComponent,
      ExperimentalDesignDetailComponent experimentalDesignDetailComponent) {
    projectDetailsComponent.setStyles("project-details-component");
    projectLinksComponent.setStyles("project-links-component");
    projectNavigationBarComponent.setStyles("project-navigation-component");
    //Todo Determine if we want to have seperate styles for each component
    experimentalDesignDetailComponent.setStyles("experimental-design-component");
  }
}
