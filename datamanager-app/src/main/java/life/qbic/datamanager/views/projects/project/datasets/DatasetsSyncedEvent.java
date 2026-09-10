package life.qbic.datamanager.views.projects.project.datasets;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * Fired when a sync trigger (single dataset or Sync All) has completed —
 * regardless of how many records were updated. The parent view refreshes
 * the connected-resources grid in response so the cards show the latest
 * version / access status.
 *
 * @see SyncResultsSidebar
 */
public class DatasetsSyncedEvent extends ComponentEvent<SyncResultsSidebar> {

  @Serial
  private static final long serialVersionUID = 1L;

  public DatasetsSyncedEvent(SyncResultsSidebar source) {
    super(source, false);
  }
}