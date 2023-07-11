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
import life.qbic.datamanager.views.projects.create.ProjectDraft;
import life.qbic.datamanager.views.projects.create.AddProjectDialog;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollectionComponent;
import life.qbic.projectmanagement.application.ProjectRegistrationService;
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
  private final ProjectRegistrationService projectRegistrationService;

  public ProjectOverviewPage(@Autowired ProjectCollectionComponent projectCollectionComponent,
      AddProjectDialog addProjectDialog,
      ProjectRegistrationService projectRegistrationService) {
    this.projectCollectionComponent = projectCollectionComponent;
    this.addProjectDialog = addProjectDialog;
    this.projectRegistrationService = projectRegistrationService;
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
    addProjectDialog.addCancelEventListener(projectCreationDialogUserCancelEvent ->
        addProjectDialog.resetAndClose());
    addProjectDialog.addProjectAddEventListener(projectCreationEvent ->
        createProject(projectCreationEvent.getSource().content())
    );
  }

  private void stylePage() {
    this.setWidthFull();
    this.setHeightFull();
  }

  private void createProject(ProjectDraft projectCreationContent) {
    Result<Project, ApplicationException> project = projectRegistrationService.registerProject(
        projectCreationContent.offerId(), projectCreationContent.projectCode(),
        projectCreationContent.title(), projectCreationContent.objective(),
        projectCreationContent.experimentalDesignDescription(), projectCreationContent.species(),
        projectCreationContent.specimen(), projectCreationContent.analyte(),
        projectCreationContent.principalInvestigator(),
        projectCreationContent.projectResponsible(),
        projectCreationContent.projectManager());

    project
        .onValue(result -> {
          displaySuccessfulProjectCreationNotification();
          addProjectDialog.resetAndClose();
          projectCollectionComponent.refresh();
        })
        .onError(e -> {
          throw e;
        });
  }

  private void displaySuccessfulProjectCreationNotification() {
    var successMessage = new SuccessMessage("Project creation succeeded.", "");
    var notification = new StyledNotification(successMessage);
    notification.open();
  }

}
