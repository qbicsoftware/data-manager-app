package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.notification.Notification;
import life.qbic.projectmanagement.application.ProjectCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateProjectHandler implements CreateProjectHandlerInterface {

  private final ProjectCreationService projectCreationService;
  private CreateProjectLayout createProjectLayout;

  public CreateProjectHandler(@Autowired ProjectCreationService projectCreationService) {
    this.projectCreationService = projectCreationService;
  }

  @Override
  public void handle(CreateProjectLayout createProjectLayout) {
    if (this.createProjectLayout != createProjectLayout) {
      this.createProjectLayout = createProjectLayout;
      addSaveClickListener();
    }
  }

  private void addSaveClickListener() {
    createProjectLayout.saveButton.addClickListener(it -> saveClicked());
  }

  private void saveClicked() {
    String titleFieldValue = createProjectLayout.titleField.getValue();
    projectCreationService.createProject(titleFieldValue)
        .ifSuccess(it -> displaySuccessfulProjectCreationNotification());
  }

  private void displaySuccessfulProjectCreationNotification() {
    Notification.show("Project creation succeeded.");
  }
}
