package life.qbic.datamanager.views.projects.overview.components;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Project Add Submit Event</b>
 * <p>
 * An event that indicates, that a user wants to add a new project.
 *
 * @since 1.0.0
 */
public class ProjectAddSubmitEvent extends ComponentEvent<ProjectCollectionComponent> {

  @Serial
  private static final long serialVersionUID = 28673255958404464L;

  public ProjectAddSubmitEvent(ProjectCollectionComponent source, boolean fromClient) {
    super(source, fromClient);
  }
}
