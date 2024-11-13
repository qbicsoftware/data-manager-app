package life.qbic.datamanager.views.events;

import com.vaadin.flow.component.ComponentEvent;
import java.util.Optional;
import life.qbic.datamanager.views.projects.edit.EditFundingInformationDialog;
import life.qbic.datamanager.views.projects.ProjectInformation;

public class FundingInformationUpdateEvent extends ComponentEvent<EditFundingInformationDialog> {

  private final ProjectInformation projectInformation;

  public FundingInformationUpdateEvent(EditFundingInformationDialog source, boolean fromClient, ProjectInformation projectInformation) {
    super(source, fromClient);
    this.projectInformation = projectInformation;
  }

  public Optional<ProjectInformation> content() {
    return Optional.ofNullable(projectInformation);
  }
}
