package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import static java.util.Objects.requireNonNull;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.logging.api.Logger;
import static life.qbic.logging.service.LoggerFactory.logger;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A component that allows to manage project access
 */

@Route(value = "projects/:projectId?/access", layout = ProjectMainLayout.class)
@PermitAll
public class ProjectAccessMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 4979017702364519296L;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private static final Logger log = logger(ProjectAccessMain.class);
  private final ProjectAccessComponent projectAccessComponent;
  private final transient UserPermissions userPermissions;
  private transient Context context;

  protected ProjectAccessMain(@Autowired ProjectAccessComponent projectAccessComponent,
      @Autowired UserPermissions userPermissions) {
    requireNonNull(projectAccessComponent);
    requireNonNull(userPermissions);
    this.projectAccessComponent = projectAccessComponent;
    this.userPermissions = userPermissions;
    addClassName("project-access");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        projectAccessComponent.getClass().getSimpleName(),
        System.identityHashCode(projectAccessComponent)));
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectID = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    this.context = new Context().with(parsedProjectId);
    if (userPermissions.changeProjectAccess(parsedProjectId)) {
      initializeComponentsWithContext();
    } else {
      event.rerouteToError(NotFoundException.class);
    }
    initializeComponentsWithContext();

  }

  private void initializeComponentsWithContext() {
    projectAccessComponent.setContext(context);
    add(projectAccessComponent);
  }
}
