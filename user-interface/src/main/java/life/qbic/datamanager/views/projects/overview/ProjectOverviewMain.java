package life.qbic.datamanager.views.projects.overview;

import static java.util.Objects.requireNonNull;
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
import java.util.Locale;
import java.util.Set;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.create.AddProjectDialog;
import life.qbic.datamanager.views.projects.create.AddProjectDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.create.AddProjectDialog.ProjectCreationInformation;
import life.qbic.datamanager.views.projects.create.CollaboratorsLayout.ProjectCollaborators;
import life.qbic.datamanager.views.projects.create.ExperimentalInformationLayout.ExperimentalInformation;
import life.qbic.datamanager.views.projects.create.ProjectDesignLayout.ProjectDesign;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollectionComponent;
import life.qbic.finances.api.FinanceService;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
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
@Route(value = ProjectRoutes.PROJECTS, layout = UserMainLayout.class)
@PermitAll
public class ProjectOverviewMain extends Main {

  @Serial
  private static final long serialVersionUID = 4625607082710157069L;
  private static final Logger log = logger(ProjectOverviewMain.class);
  private final ProjectCollectionComponent projectCollectionComponent;
  private final transient ProjectCreationService projectCreationService;
  private final transient ProjectInformationService projectInformationService;
  private final transient FinanceService financeService;
  private final transient SpeciesLookupService ontologyTermInformationService;
  private final transient AddExperimentToProjectService addExperimentToProjectService;
  private final transient UserInformationService userInformationService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final transient MessageSourceNotificationFactory messageSourceNotificationFactory;

  public ProjectOverviewMain(@Autowired ProjectCollectionComponent projectCollectionComponent,
      ProjectCreationService projectCreationService, FinanceService financeService,
      ProjectInformationService projectInformationService,
      SpeciesLookupService ontologyTermInformationService,
      AddExperimentToProjectService addExperimentToProjectService,
      UserInformationService userInformationService,
      AuthenticationToUserIdTranslationService userIdTranslator,
      TerminologyService terminologyService,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.projectCollectionComponent = requireNonNull(projectCollectionComponent,
        "project collection component can not be null");
    this.projectCreationService = requireNonNull(projectCreationService,
        "project creation service can not be null");
    this.financeService = requireNonNull(financeService, "finance service can not be null");
    this.projectInformationService = requireNonNull(projectInformationService,
        "project information service can not be null");
    this.ontologyTermInformationService = requireNonNull(ontologyTermInformationService,
        "ontology term information service can not be null");
    this.addExperimentToProjectService = requireNonNull(addExperimentToProjectService,
        "add experiment to project service cannot be null");
    this.userInformationService = requireNonNull(userInformationService,
        "user information service can not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");
    requireNonNull(terminologyService, "terminologyService must not be null");
    requireNonNull(cancelConfirmationDialogFactory,
        "cancelConfirmationDialogFactory must not be null");

    addTitleAndDescription();
    add(projectCollectionComponent);
    this.projectCollectionComponent.addCreateClickedListener(projectCreationClickedEvent -> {
      AddProjectDialog addProjectDialog = new AddProjectDialog(this.projectInformationService,
          this.financeService,
          this.ontologyTermInformationService, terminologyService,
          cancelConfirmationDialogFactory);
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
    this.messageSourceNotificationFactory = messageSourceNotificationFactory;
  }

  private static Toast notificationFor(NotificationType type, ConfirmEvent confirmEvent,
      MessageSourceNotificationFactory factory, Locale locale) {
    return switch (type) {
      case PROJECT_CREATED_SUCCESSFULLY -> factory.toast("project.created.success",
          new Object[]{confirmEvent.projectCreationInformation().projectDesign().getProjectTitle()},
          locale);
      case PROJECT_CREATION_FAILED ->
          factory.toast("project.created.error", new Object[]{}, locale);
      case EXPERIMENT_CREATED_SUCCESSFULLY -> factory.toast("experiment.created.success",
          new Object[]{confirmEvent.experimentalInformation().getExperimentName()}, locale);
      case EXPERIMENT_CREATION_FAILED ->
          factory.toast("experiment.created.error", new Object[]{}, locale);
    };
  }

  private void addTitleAndDescription() {
    Div titleAndDescription = new Div();
    titleAndDescription.addClassName("title-and-description");
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = userIdTranslator.translateToUserId(authentication).orElseThrow();
    var user = userInformationService.findById(userId).orElseThrow();
    Span title = new Span(
        String.format("Welcome Back %s!", user.platformUserName()));
    title.addClassNames("project-overview-title");
    Span descriptionStart = new Span(
        "Manage all your scientific data in one place with the Data Manager. You can access our ");
    Anchor descriptionLinkToDoc = new Anchor(
        "https://qbicsoftware.github.io/research-data-management/",
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
    ProjectCreationInformation projectCreationInformation = confirmEvent.projectCreationInformation();
    if (projectCreationInformation.getFundingEntry() != null
        && !projectCreationInformation.getFundingEntry()
        .isEmpty()) {
      funding = Funding.of(projectCreationInformation.getFundingEntry().get().getLabel(),
          projectCreationInformation.getFundingEntry().get().getReferenceId());
    }
    ProjectDesign projectDesign = projectCreationInformation.projectDesign();
    ProjectCollaborators projectCollaborators = projectCreationInformation.projectCollaborators();
    Result<Project, ApplicationException> project = projectCreationService.createProject(
        projectDesign.getOfferId(),
        projectDesign.getProjectCode(),
        projectDesign.getProjectTitle(),
        projectDesign.getProjectObjective(),
        projectCollaborators.getPrincipalInvestigator().toDomainContact(),
        projectCollaborators.getResponsiblePerson()
            .map(Contact::toDomainContact).orElse(null),
        projectCollaborators.getProjectManager().toDomainContact(),
        funding);
    handleResultProject(project, confirmEvent);
    ExperimentalInformation experimentalInformation = confirmEvent.experimentalInformation();
    var experiment = addExperimentToProjectService.addExperimentToProject(
        project.getValue().getId(),
        experimentalInformation.getExperimentName(),
        experimentalInformation.getSpecies(),
        experimentalInformation.getSpecimens(),
        experimentalInformation.getAnalytes(),
        experimentalInformation.getSpeciesIcon().getLabel(),
        experimentalInformation.getSpecimenIcon().getLabel());
    handleResultExperiment(experiment, confirmEvent);
    projectCollectionComponent.refresh();
    projectCollectionComponent.resetSearch();
  }

  private void handleResultProject(Result<?, ?> result, ConfirmEvent event) {
    if (result.isError()) {
      processNotification(notificationFor(NotificationType.PROJECT_CREATION_FAILED, event,
          messageSourceNotificationFactory, getLocale()));
      return;
    }
    processNotification(notificationFor(NotificationType.PROJECT_CREATED_SUCCESSFULLY, event,
        messageSourceNotificationFactory, getLocale()));
    closeDialog(event);
  }

  private void handleResultExperiment(Result<?, ?> result, ConfirmEvent event) {
    if (result.isError()) {
      processNotification(notificationFor(NotificationType.EXPERIMENT_CREATION_FAILED, event,
          messageSourceNotificationFactory, getLocale()));
      return;
    }
    closeDialog(event);
  }

  private void processNotification(Toast t) {
    t.open();
  }

  private void closeDialog(ConfirmEvent event) {
    event.getSource().close();
  }

  enum NotificationType {
    PROJECT_CREATED_SUCCESSFULLY,
    PROJECT_CREATION_FAILED,
    EXPERIMENT_CREATED_SUCCESSFULLY,
    EXPERIMENT_CREATION_FAILED,
  }
}
