package life.qbic.datamanager.views.projects.overview;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.create.AddProjectDialog;
import life.qbic.datamanager.views.projects.create.AddProjectDialog.ProjectAddEvent;
import life.qbic.datamanager.views.projects.create.AddProjectDialog.ProjectDraft;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollectionComponent;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.domain.project.Project;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project view page that shows project information and additional components to manage project
 * data.
 *
 * @since 1.0.0
 */
@PageTitle("Project Overview")
@Route(value = Projects.PROJECTS, layout = MainLayout.class)
@PermitAll
public class ProjectOverviewPage extends Div {

  @Serial
  private static final long serialVersionUID = 4625607082710157069L;

  private final ProjectCollectionComponent projectCollectionComponent;
  private final AddProjectDialog addProjectDialog;
  private final ProjectCreationService projectCreationService;

  public ProjectOverviewPage(@Autowired ProjectCollectionComponent projectCollectionComponent,
      AddProjectDialog addProjectDialog,
      ProjectCreationService projectCreationService) {
    this.projectCollectionComponent = projectCollectionComponent;
    this.addProjectDialog = addProjectDialog;
    this.projectCreationService = projectCreationService;
    layoutPage();
    configurePage();

    stylePage();
  }

  private void layoutPage() {
    add(projectCollectionComponent);
  }

  private void configurePage() {
    projectCollectionComponent.addListener(projectCreationClickedEvent ->
        addProjectDialog.open()
    );
    addProjectDialog.addCancelListener(
        cancelEvent -> cancelEvent.getSource().close());
    addProjectDialog.addProjectAddEventListener(this::createProject);
  }

  private void stylePage() {
    this.setWidthFull();
    this.setHeightFull();
  }

  private void createProject(ProjectAddEvent projectAddEvent) {
    ProjectDraft projectDraft = projectAddEvent.projectDraft();
    Result<Project, ApplicationException> project = projectCreationService.createProject(
        projectDraft.getOfferId(),
        projectDraft.getProjectCode(),
        projectDraft.getProjectTitle(),
        projectDraft.getProjectObjective(),
        projectDraft.getPrincipalInvestigatorName(),
        projectDraft.getPrincipalInvestigatorEmail(),
        projectDraft.getResponsiblePersonName(),
        projectDraft.getResponsiblePersonEmail(),
        projectDraft.getProjectManagerName(),
        projectDraft.getProjectManagerEmail());

    project
        .onValue(result -> onProjectCreated(projectAddEvent))
        .onError(e -> {
          throw e;
        });
  }

  private void onProjectCreated(ProjectAddEvent projectAddEvent) {
    displaySuccessfulProjectCreationNotification();
    projectAddEvent.getSource().close();
    projectCollectionComponent.refresh();
  }

  private void displaySuccessfulProjectCreationNotification() {
    var successMessage = new SuccessMessage("Project creation succeeded.", "");
    var notification = new StyledNotification(successMessage);
    notification.open();
  }

}
