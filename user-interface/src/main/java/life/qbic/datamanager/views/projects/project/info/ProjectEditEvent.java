package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;
import life.qbic.controlling.domain.model.project.ProjectId;

/**
 * <b>Project Edit Event</b>
 * <p>
 * Event that indicates that the user wants to edit a project via the
 * {@link ProjectDetailsComponent}
 *
 * @since 1.0.0
 */
public class ProjectEditEvent extends ComponentEvent<ProjectDetailsComponent> {

  @Serial
  private static final long serialVersionUID = -4045489562991683868L;
  private final ProjectId projectId;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source     the source component
   * @param projectId  the {@link ProjectId} of the edited project
   * @param fromClient <code>true</code> if the event originated from the client
   *                   side, <code>false</code> otherwise
   */
  public ProjectEditEvent(ProjectDetailsComponent source, ProjectId projectId,
      boolean fromClient) {
    super(source, fromClient);
    this.projectId = projectId;
  }

  public ProjectId projectId() {
    return projectId;
  }
}
