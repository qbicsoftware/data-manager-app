package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.button.Button;
import java.util.Optional;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;

/**
 * TODO! next step use this dialog to add people to a project
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class AddCollaboratorToProjectDialog extends DialogWindow {

  private ProjectCollaborator projectCollaborator;

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    Optional.ofNullable(projectCollaborator).ifPresent(it ->
        fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), it)));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  public static class CancelEvent extends ComponentEvent<AddCollaboratorToProjectDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(AddCollaboratorToProjectDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ConfirmEvent extends ComponentEvent<AddCollaboratorToProjectDialog> {

    private final ProjectCollaborator projectCollaborator;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ConfirmEvent(AddCollaboratorToProjectDialog source, boolean fromClient,
        ProjectCollaborator projectCollaborator) {
      super(source, fromClient);
      this.projectCollaborator = projectCollaborator;
    }

    public ProjectCollaborator projectCollaborator() {
      return projectCollaborator;
    }
  }
}
