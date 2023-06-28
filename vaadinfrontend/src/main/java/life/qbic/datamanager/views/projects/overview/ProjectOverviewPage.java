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
import life.qbic.datamanager.views.projects.create.ProjectCreationContent;
import life.qbic.datamanager.views.projects.create.ProjectCreationDialog;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollection;
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

  private final ProjectCollection projectCollection;
  private final ProjectCreationDialog projectCreationDialog;
  private final ProjectRegistrationService projectRegistrationService;

  public ProjectOverviewPage(@Autowired ProjectCollection projectCollection,
      ProjectCreationDialog projectCreationDialog,
      ProjectRegistrationService projectRegistrationService) {
    this.projectCollection = projectCollection;
    this.projectCreationDialog = projectCreationDialog;
    this.projectRegistrationService = projectRegistrationService;
    layoutPage();
    configurePage();

    stylePage();
  }

  private void layoutPage() {
    add(projectCollection);
  }

  private void configurePage() {
    projectCollection.addListener(projectCreationClickedEvent ->
        projectCreationDialog.open()
    );
    projectCreationDialog.addCancelEventListener(projectCreationDialogUserCancelEvent ->
        projectCreationDialog.resetAndClose());
    projectCreationDialog.addProjectAddEventListener(projectCreationEvent ->
        createProject(projectCreationEvent.getSource().content())
    );
  }

  private void stylePage() {
    this.setWidthFull();
    this.setHeightFull();
  }

  private void createProject(ProjectCreationContent projectCreationContent) {
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
          projectCreationDialog.resetAndClose();
          projectCollection.refresh();
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
