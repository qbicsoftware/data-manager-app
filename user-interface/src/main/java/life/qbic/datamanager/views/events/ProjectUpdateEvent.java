package life.qbic.datamanager.views.events;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import java.util.Optional;
import life.qbic.datamanager.views.projects.edit.EditProjectDesignDialog;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;
import org.apache.poi.ss.formula.functions.T;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectUpdateEvent extends ComponentEvent<EditProjectDesignDialog> {

  private final ProjectInformation projectInformation;

  public ProjectUpdateEvent(EditProjectDesignDialog source, boolean fromClient, ProjectInformation projectInformation) {
    super(source, fromClient);
    this.projectInformation = projectInformation;
  }

  public Optional<ProjectInformation> content() {
    return Optional.ofNullable(projectInformation);
  }
}
