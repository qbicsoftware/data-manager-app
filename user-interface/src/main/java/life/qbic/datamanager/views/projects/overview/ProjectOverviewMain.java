package life.qbic.datamanager.views.projects.overview;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ContactRepository;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.domain.model.project.Funding;
import life.qbic.projectmanagement.domain.model.project.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Project overview {@link Main} component that shows project information and additional components
 * to manage project data.
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
  private final transient ProjectCreationService projectCreationService;
  private final transient ProjectInformationService projectInformationService;
  private final transient FinanceService financeService;
  private final transient OntologyLookupService ontologyTermInformationService;
  private final transient AddExperimentToProjectService addExperimentToProjectService;
  private final transient ContactRepository contactRepository;
  private final transient UserInformationService userInformationService;

  public ProjectOverviewMain(@Autowired ProjectCollectionComponent projectCollectionComponent,
      ProjectCreationService projectCreationService, FinanceService financeService,
      ProjectInformationService projectInformationService,
      OntologyLookupService ontologyTermInformationService,
      AddExperimentToProjectService addExperimentToProjectService,
      UserInformationService userInformationService,
      ContactRepository contactRepository) {
    this.projectCollectionComponent = Objects.requireNonNull(projectCollectionComponent,
        "project collection component can not be null");
    this.projectCreationService = Objects.requireNonNull(projectCreationService,
        "project creation service can not be null");
    this.financeService = Objects.requireNonNull(financeService, "finance service can not be null");
    this.projectInformationService = Objects.requireNonNull(projectInformationService,
        "project information service can not be null");
    this.ontologyTermInformationService = Objects.requireNonNull(ontologyTermInformationService,
        "ontology term information service can not be null");
    this.addExperimentToProjectService = Objects.requireNonNull(addExperimentToProjectService,
        "add experiment to project service cannot be null");
    this.contactRepository = Objects.requireNonNull(contactRepository,
        "contact repository can not be null");
    this.userInformationService = Objects.requireNonNull(userInformationService,
        "user information service can not be null");
    addTitleAndDescription();
    add(projectCollectionComponent);
    this.projectCollectionComponent.addCreateClickedListener(projectCreationClickedEvent -> {
      AddProjectDialog addProjectDialog = new AddProjectDialog(this.projectInformationService,
          this.financeService,
          this.ontologyTermInformationService, this.contactRepository);
      if (isOfferSearchAllowed()) {
        addProjectDialog.enableOfferSearch();
      }
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

  private void addTitleAndDescription() {
    Div titleAndDescription = new Div();
    titleAndDescription.addClassName("title-and-description");
    var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var userId = "";
    if (principal instanceof QbicUserDetails qbicUserDetails) {
      userId = qbicUserDetails.getUserId();
    }
    if (principal instanceof QbicOidcUser qbicOidcUser) {
      userId = qbicOidcUser.getQbicUserId();
    }
//    var user = userInformationService.findById(userId);
    Span title = new Span(
        String.format("Welcome Back %s!", ""/*user.orElseThrow().platformUserName()*/));
    title.addClassNames("project-overview-title");
    Span descriptionStart = new Span(
        "Manage all your scientific data in one place with the Data Manager. You can access our ");
    Anchor descriptionLinkToDoc = new Anchor("https://qbicsoftware.github.io/data-manager-app/",
        "documentation", AnchorTarget.BLANK);
    Span descriptionEnd = new Span(
        " and learn more about using the Data Manager.\n"
            + "Start by creating a new project or continue working on an already existing project.");
    Div description = new Div(descriptionStart, descriptionLinkToDoc, descriptionEnd);
    description.addClassName("description");
    titleAndDescription.add(title, description);
    add(titleAndDescription);
  }

  private boolean isOfferSearchAllowed() {
    Set<String> allowedRoles = new HashSet<>(Arrays.asList("ROLE_ADMIN", "ROLE_PROJECT_MANAGER"));
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(r -> allowedRoles.contains(r.getAuthority()));
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
        confirmEvent.getExperimentalInformation().getAnalytes(),
        confirmEvent.getExperimentalInformation().getSpeciesIcon().getLabel(),
        confirmEvent.getExperimentalInformation().getSpecimenIcon().getLabel());
    experiment.onError(e -> {
      throw e;
    });
  }

  private void onProjectCreated(ConfirmEvent confirmEvent) {
    displaySuccessfulProjectCreationNotification();
    confirmEvent.getSource().close();
    projectCollectionComponent.refresh();
    projectCollectionComponent.resetSearch();
  }

  private void displaySuccessfulProjectCreationNotification() {
    var successMessage = new SuccessMessage("Project creation succeeded.", "");
    var notification = new StyledNotification(successMessage);
    notification.open();
  }

}
