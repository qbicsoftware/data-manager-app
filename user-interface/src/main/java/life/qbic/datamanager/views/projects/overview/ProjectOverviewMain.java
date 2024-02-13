package life.qbic.datamanager.views.projects.overview;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.create.AddProjectDialog;
import life.qbic.datamanager.views.projects.create.AddProjectDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollectionComponent;
import life.qbic.finances.api.FinanceService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ContactRepository;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.domain.model.project.Funding;
import life.qbic.projectmanagement.domain.model.project.Project;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project overview {@link Main} component that shows project information and additional components to manage project
 * data.
 *
 * @since 1.0.0
 */
@PageTitle("Project Overview")
@Route(value = Projects.PROJECTS, layout = UserMainLayout.class)
@PermitAll
public class ProjectOverviewMain extends Main {
  @Serial
  private static final long serialVersionUID = 4625607082710157069L;
  private static final Logger log = logger(ProjectOverviewMain.class);
  private final ProjectCollectionComponent projectCollectionComponent;
  private final ProjectCreationService projectCreationService;
  private final FinanceService financeService;
  private final OntologyTermInformationService ontologyTermInformationService;
  private final AddExperimentToProjectService addExperimentToProjectService;
  private final ContactRepository contactRepository;

  public ProjectOverviewMain(@Autowired ProjectCollectionComponent projectCollectionComponent,
      ProjectCreationService projectCreationService, FinanceService financeService,
      OntologyTermInformationService ontologyTermInformationService,
      AddExperimentToProjectService addExperimentToProjectService,
      ContactRepository contactRepository) {
    this.projectCollectionComponent = projectCollectionComponent;
    this.projectCreationService = projectCreationService;
    this.financeService = financeService;
    this.ontologyTermInformationService = ontologyTermInformationService;
    this.addExperimentToProjectService = addExperimentToProjectService;
    add(projectCollectionComponent);
    this.contactRepository = contactRepository;
    add(this.projectCollectionComponent);
    this.projectCollectionComponent.addCreateClickedListener(projectCreationClickedEvent -> {
      AddProjectDialog addProjectDialog = new AddProjectDialog(this.financeService,
          this.ontologyTermInformationService, this.contactRepository);
      addProjectDialog.addConfirmListener(this::createProject);
      addProjectDialog.addCancelListener(it -> it.getSource().close());
      addProjectDialog.open();
    });
    addClassName("project-overview");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        projectCollectionComponent.getClass().getSimpleName(),
        System.identityHashCode(projectCollectionComponent)));
  }

  private void createProject(ConfirmEvent confirmEvent) {
    Funding funding = null;
    if (confirmEvent.getFundingEntry() != null && !confirmEvent.getFundingEntry()
        .isEmpty()) {
      funding = Funding.of(confirmEvent.getFundingEntry().getLabel(),
          confirmEvent.getFundingEntry().getReferenceId());
    }
    Result<Project, ApplicationException> project = projectCreationService.createProject(
        confirmEvent.getProjectDesign().getOfferId(),
        confirmEvent.getProjectDesign().getProjectCode(),
        confirmEvent.getProjectDesign().getProjectTitle(),
        confirmEvent.getProjectDesign().getProjectObjective(),
        confirmEvent.getProjectCollaborators().getPrincipalInvestigator().toDomainContact(),
        confirmEvent.getProjectCollaborators().getResponsiblePerson()
            .map(Contact::toDomainContact).orElse(null),
        confirmEvent.getProjectCollaborators().getProjectManager().toDomainContact(),
        funding);
    project
        .onValue(result -> onProjectCreated(confirmEvent))
        .onError(e -> {
          throw e;
        });
    var experiment = addExperimentToProjectService.addExperimentToProject(
        project.getValue().getId(),
        confirmEvent.getExperimentalInformation().getExperimentName(),
        confirmEvent.getExperimentalInformation().getSpecies(),
        confirmEvent.getExperimentalInformation().getSpecimens(),
        confirmEvent.getExperimentalInformation().getAnalytes());
    experiment.onError(e -> {
      throw e;
    });
  }

  private void onProjectCreated(ConfirmEvent confirmEvent) {
    displaySuccessfulProjectCreationNotification();
    confirmEvent.getSource().close();
    projectCollectionComponent.refresh();
  }

  private void displaySuccessfulProjectCreationNotification() {
    var successMessage = new SuccessMessage("Project creation succeeded.", "");
    var notification = new StyledNotification(successMessage);
    notification.open();
  }

}
