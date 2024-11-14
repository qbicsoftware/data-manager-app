package life.qbic.datamanager.views.events;

import com.vaadin.flow.component.ComponentEvent;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.projects.edit.EditContactDialog;


public class ContactUpdateEvent extends ComponentEvent<EditContactDialog> {

  private final ProjectInformation projectInfo;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source     the source component
   * @param fromClient <code>true</code> if the event originated from the client
   *                   side, <code>false</code> otherwise
   */
  public ContactUpdateEvent(EditContactDialog source, boolean fromClient, ProjectInformation projectInformation) {
    super(source, fromClient);
    this.projectInfo = Objects.requireNonNull(projectInformation);
  }

  public Optional<ProjectInformation> content() {
    return Optional.ofNullable(projectInfo);
  }
}
