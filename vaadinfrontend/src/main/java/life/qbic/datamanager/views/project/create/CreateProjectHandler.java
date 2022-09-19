package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.notification.Notification;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.Project;
import org.springframework.stereotype.Component;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Component
public class CreateProjectHandler implements CreateProjectHandlerInterface {

  private CreateProjectLayout createProjectLayout;
  private final ProjectCreationService projectCreationService;

  public CreateProjectHandler(ProjectCreationService projectCreationService) {
    this.projectCreationService = projectCreationService;
  }

  public CreateProjectHandler() {
    this.projectCreationService = new ProjectCreationService();
  }

  //TODO add ProjectCreationService
  public static class ProjectCreationService {

    Result<Project, Exception> createProject(String title) {
      return Result.failure(new RuntimeException("Not implemented"));
    }

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
        .ifSuccessOrElse(it -> Notification.show("Success"),
            it -> Notification.show("Error + " + it.getMessage()));
  }
}
