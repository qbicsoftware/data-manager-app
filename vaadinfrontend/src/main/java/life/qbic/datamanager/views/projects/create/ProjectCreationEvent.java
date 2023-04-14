package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.ComponentEvent;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
