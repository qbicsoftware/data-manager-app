package life.qbic.datamanager.views.events;

import com.vaadin.flow.component.ComponentEvent;
import java.util.Optional;
import life.qbic.datamanager.views.projects.edit.EditFundingInformationDialog;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FundingInformationUpdateEvent extends ComponentEvent<EditFundingInformationDialog> {

  private final ProjectInformation projectInformation;

  public FundingInformationUpdateEvent(EditFundingInformationDialog source, boolean fromClient, ProjectInformation projectInformation) {
    super(source, fromClient);
    this.projectInformation = projectInformation;
  }

  public Optional<ProjectInformation> projectInformation() {
    return Optional.ofNullable(projectInformation);
  }
}
