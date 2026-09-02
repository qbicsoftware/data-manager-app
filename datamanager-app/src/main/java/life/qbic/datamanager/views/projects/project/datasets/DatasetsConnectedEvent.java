package life.qbic.datamanager.views.projects.project.datasets;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * Fired when one or more datasets have been successfully connected to the
 * project. The parent view refreshes its connected-resources grid in response.
 *
 * <p>Popped out of {@link ConnectDatasetSidebar} (where it was previously a
 * nested {@code public static class}) so it is discoverable as a first-class
 * part of the sidebar's public API.</p>
 *
 * @see ConnectDatasetSidebar#addDatasetsConnectedListener(com.vaadin.flow.component.ComponentEventListener)
 */
public class DatasetsConnectedEvent extends ComponentEvent<ConnectDatasetSidebar> {

  @Serial
  private static final long serialVersionUID = 1L;

  public DatasetsConnectedEvent(ConnectDatasetSidebar source) {
    super(source, false);
  }
}