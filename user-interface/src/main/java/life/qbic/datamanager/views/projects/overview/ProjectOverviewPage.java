package life.qbic.datamanager.views.projects.overview;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.create.ProjectCreationDialog;
import life.qbic.datamanager.views.projects.create.ProjectCreationDialog.ProjectCreationEvent;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollectionComponent;
import life.qbic.finances.api.FinanceService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.domain.model.project.Funding;
import life.qbic.projectmanagement.domain.model.project.Project;
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
  private static final Logger log = logger(ProjectOverviewPage.class);
  private final ProjectCollectionComponent projectCollectionComponent;
  private final ProjectCreationService projectCreationService;
  private final FinanceService financeService;
  private final ExperimentalDesignSearchService experimentalDesignSearchService;
  private final AddExperimentToProjectService addExperimentToProjectService;

  public ProjectOverviewPage(@Autowired ProjectCollectionComponent projectCollectionComponent,
      ProjectCreationService projectCreationService, FinanceService financeService,
      ExperimentalDesignSearchService experimentalDesignSearchService,
      AddExperimentToProjectService addExperimentToProjectService) {
    this.projectCollectionComponent = projectCollectionComponent;
    this.projectCreationService = projectCreationService;
    this.financeService = financeService;
    this.experimentalDesignSearchService = experimentalDesignSearchService;
    this.addExperimentToProjectService = addExperimentToProjectService;
    layoutPage();
    configurePage();
    stylePage();
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        projectCollectionComponent.getClass().getSimpleName(),
        System.identityHashCode(projectCollectionComponent)));
  }

  private void layoutPage() {
    add(projectCollectionComponent);
  }

  private void configurePage() {
    projectCollectionComponent.addListener(projectCreationClickedEvent -> {
      ProjectCreationDialog projectCreationDialog = new ProjectCreationDialog(financeService,
          experimentalDesignSearchService);
      projectCreationDialog.addListener(this::createProject);
      projectCreationDialog.open();
    });
    //ToDo add Cancel and close listeners
  }

  private void stylePage() {
    this.setWidthFull();
    this.setHeightFull();
  }

  private void createProject(ProjectCreationEvent projectCreationEvent) {
    //ToDo Better way to get funding out of funding entry?
    Funding funding = null;
    if (!projectCreationEvent.getFundingEntry().isEmpty()) {
      funding = Funding.of(projectCreationEvent.getFundingEntry().getLabel(),
          projectCreationEvent.getFundingEntry().getReferenceId());
    }
    Result<Project, ApplicationException> project = projectCreationService.createProject(
        projectCreationEvent.getProjectDesign().getOfferId(),
        projectCreationEvent.getProjectDesign().getProjectCode(),
        projectCreationEvent.getProjectDesign().getProjectTitle(),
        projectCreationEvent.getProjectDesign().getProjectObjective(),
        projectCreationEvent.getProjectCollaborators().getPrincipalInvestigator().toDomainContact(),
        projectCreationEvent.getProjectCollaborators().getResponsiblePerson()
            .map(Contact::toDomainContact)
            .orElse(null),
        projectCreationEvent.getProjectCollaborators().getProjectManager().toDomainContact(),
        funding);
    project
        .onValue(result -> onProjectCreated(projectCreationEvent))
        .onError(e -> {
          throw e;
        });
    var experiment = addExperimentToProjectService.addExperimentToProject(
        project.getValue().getId(),
        projectCreationEvent.getExperimentalInformation().getExperimentName(),
        projectCreationEvent.getExperimentalInformation().getSpecies(),
        projectCreationEvent.getExperimentalInformation().getSpecimens(),
        projectCreationEvent.getExperimentalInformation().getAnalytes());
    experiment.onError(e -> {
      throw e;
    });
  }

  private void onProjectCreated(ProjectCreationEvent projectCreationEvent) {
    displaySuccessfulProjectCreationNotification();
    projectCreationEvent.getSource().close();
    projectCollectionComponent.refresh();
  }

  private void displaySuccessfulProjectCreationNotification() {
    var successMessage = new SuccessMessage("Project creation succeeded.", "");
    var notification = new StyledNotification(successMessage);
    notification.open();
  }

}
