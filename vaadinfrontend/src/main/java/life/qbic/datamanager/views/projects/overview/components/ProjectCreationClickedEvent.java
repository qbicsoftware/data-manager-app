package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Project Creation Clicked Event</b>
 * <p>
 * An event that indicates, that a user wants to add a new project.
 *
 * @since 1.0.0
 */
public class ProjectCreationClickedEvent extends ComponentEvent<ProjectCollection> {

  @Serial
  private static final long serialVersionUID = 28673255958404464L;

  public ProjectCreationClickedEvent(ProjectCollection source, boolean fromClient) {
    super(source, fromClient);
  }
}
