package life.qbic.datamanager.views.events;

import com.vaadin.flow.component.ComponentEvent;
import java.util.Optional;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.projects.edit.EditProjectDesignDialog;


public class ProjectDesignUpdateEvent extends ComponentEvent<EditProjectDesignDialog> {

  private final ProjectInformation projectInformation;

  public ProjectDesignUpdateEvent(EditProjectDesignDialog source, boolean fromClient, ProjectInformation projectInformation) {
    super(source, fromClient);
    this.projectInformation = projectInformation;
  }

  public Optional<ProjectInformation> content() {
    return Optional.ofNullable(projectInformation);
  }
}
