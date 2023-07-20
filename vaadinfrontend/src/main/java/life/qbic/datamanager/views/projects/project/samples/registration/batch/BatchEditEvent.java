package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent;
import life.qbic.projectmanagement.domain.project.sample.Batch;

/**
 * <b>Batch Edit Event</b>
 *
 * <p>Indicates that a user wants to edit {@link Batch} information
 * within the {@link BatchDetailsComponent} of a project</p>
 */
public class BatchEditEvent extends ComponentEvent<BatchDetailsComponent> {

  @Serial
  private static final long serialVersionUID = -5424056755722207848L;

  public BatchEditEvent(BatchDetailsComponent source, boolean fromClient) {
    super(source, fromClient);
  }
}
