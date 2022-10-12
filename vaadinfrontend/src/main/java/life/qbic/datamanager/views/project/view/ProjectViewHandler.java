package life.qbic.datamanager.views.project.view;

import java.util.Objects;
import life.qbic.datamanager.views.project.view.components.ProjectDetailsComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ProjectViewHandler {

  private final ProjectViewPage view;
  private final ProjectDetailsComponent projectDetailsComponent;

  private static final Logger log = LoggerFactory.logger(ProjectViewHandler.class);

  public ProjectViewHandler(ProjectViewPage view, ProjectDetailsComponent projectDetailsComponent) {
    Objects.requireNonNull(view);
    Objects.requireNonNull(projectDetailsComponent);
    this.view = view;
    this.projectDetailsComponent = projectDetailsComponent;
    log.debug("Registered layout " + view.getClass().getName() + ":" + System.identityHashCode(view));
  }

  public void routeParameter(String parameter) {
    this.projectDetailsComponent.projectId(parameter);
  }



}
