package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectCreationClickedEvent extends ComponentEvent<ProjectsCollection> {

  @Serial
  private static final long serialVersionUID = 28673255958404464L;

  public ProjectCreationClickedEvent(ProjectsCollection source, boolean fromClient) {
    super(source, fromClient);
  }
}
