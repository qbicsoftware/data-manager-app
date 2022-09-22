package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.exceptionhandlers.ApplicationExceptionHandler;
import life.qbic.datamanager.views.components.SuccessMessage;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.domain.project.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateProjectHandler implements CreateProjectHandlerInterface {

  private final ProjectCreationService projectCreationService;
  private CreateProjectLayout createProjectLayout;

  private final ApplicationExceptionHandler exceptionHandler;

  public CreateProjectHandler(@Autowired ProjectCreationService projectCreationService,
      @Autowired ApplicationExceptionHandler exceptionHandler) {
    this.projectCreationService = projectCreationService;
    this.exceptionHandler = exceptionHandler;
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
    Result<Project, ApplicationException> project =
        createProjectLayout.experimentalDesignField.isEmpty()
            ? projectCreationService.createProject(titleFieldValue)
            : projectCreationService.createProjectWithExperimentalDesign(titleFieldValue,
                createProjectLayout.experimentalDesignField.getValue());

    project
        .ifSuccessOrElse(
            result -> displaySuccessfulProjectCreationNotification(),
            applicationException -> exceptionHandler.handle(UI.getCurrent(), applicationException));
  }

  private void displaySuccessfulProjectCreationNotification() {
    SuccessMessage successMessage = new SuccessMessage("Project creation succeeded.", "");
    Notification notification = new Notification(successMessage);
    notification.setPosition(Position.MIDDLE);
    notification.open();
  }
}
