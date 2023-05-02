package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Project Creation Event</b>
 *
 * <p>Indicates that a user submitted a project creation request</p>
 *
 * @since 1.0.0
 */
public class ProjectCreationEvent extends ComponentEvent<ProjectInformationDialog> {

  @Serial
  private static final long serialVersionUID = 1072173555312630829L;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source     the source component
   * @param fromClient <code>true</code> if the event originated from the client
   *                   side, <code>false</code> otherwise
   */
  public ProjectCreationEvent(ProjectInformationDialog source, boolean fromClient) {
    super(source, fromClient);
  }
}
