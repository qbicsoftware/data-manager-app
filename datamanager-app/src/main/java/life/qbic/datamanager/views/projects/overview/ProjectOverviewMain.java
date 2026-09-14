package life.qbic.datamanager.views.projects.overview;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.create.AddProjectDialog;
import life.qbic.datamanager.views.projects.create.AddProjectDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.create.AddProjectDialog.ProjectCreationInformation;
import life.qbic.datamanager.views.projects.create.ExperimentalInformationLayout.ExperimentalInformation;
import life.qbic.datamanager.views.projects.create.ProjectDesignLayout.ProjectDesign;
import life.qbic.datamanager.views.projects.overview.components.ProjectCollectionComponent;
import life.qbic.datamanager.views.projects.overview.components.ProjectOverviewSortOption;
import life.qbic.finances.api.FinanceService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectContact;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectContacts;
import life.qbic.projectmanagement.application.contact.PersonLookupService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
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
public class ProjectOverviewMain extends Main implements BeforeEnterObserver, BeforeLeaveObserver {

  @Serial
  private static final long serialVersionUID = 4625607082710157069L;
  private static final Logger log = logger(ProjectOverviewMain.class);
  private final ProjectCollectionComponent projectCollectionComponent;
  private final transient ProjectCreationService projectCreationService;
  private final transient ProjectInformationService projectInformationService;
  private final transient FinanceService financeService;
  private final transient SpeciesLookupService ontologyTermInformationService;
  private final transient AddExperimentToProjectService addExperimentToProjectService;
  /**
   * The framework's own history state change handler, captured before this view installs its own.
   *
   * <p>{@link History#setHistoryStateChangeHandler(History.HistoryStateChangeHandler)} allows a
   * single handler per UI and Vaadin uses that one handler to perform router navigation — it is
   * invoked for browser back/forward <b>and</b> for every {@code RouterLink} click. Replacing it
   * without chaining therefore swallows all navigation out of this view, which is why the captured
   * handler is restored on leave and delegated to for foreign locations.
   */
  private History.HistoryStateChangeHandler routerHistoryStateChangeHandler;
  /** Stable identity for this view's handler, so re-entry does not chain the view onto itself. */
  private final History.HistoryStateChangeHandler listStateHistoryHandler = this::onHistoryStateChange;
  private final transient MessageSourceNotificationFactory messageSourceNotificationFactory;

  public ProjectOverviewMain(@Autowired ProjectCollectionComponent projectCollectionComponent,
      ProjectCreationService projectCreationService, FinanceService financeService,
      ProjectInformationService projectInformationService,
      SpeciesLookupService ontologyTermInformationService,
      PersonLookupService personLookupService,
      AddExperimentToProjectService addExperimentToProjectService,
      TerminologyService terminologyService,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.projectCollectionComponent = requireNonNull(projectCollectionComponent,
        "project collection component can not be null");
    this.projectCreationService = requireNonNull(projectCreationService,
        "project creation service can not be null");
    this.financeService = requireNonNull(financeService, "finance service can not be null");
    requireNonNull(personLookupService,
        "person lookup service can not be null");
    this.projectInformationService = requireNonNull(projectInformationService,
        "project information service can not be null");
    this.ontologyTermInformationService = requireNonNull(ontologyTermInformationService,
        "ontology term information service can not be null");
    this.addExperimentToProjectService = requireNonNull(addExperimentToProjectService,
        "add experiment to project service cannot be null");
    requireNonNull(terminologyService, "terminologyService must not be null");

    addWelcomeText();
    add(projectCollectionComponent);
    this.projectCollectionComponent.addCreateClickedListener(projectCreationClickedEvent -> {
      AddProjectDialog addProjectDialog = new AddProjectDialog(this.projectInformationService,
          this.financeService,
          personLookupService,
          this.ontologyTermInformationService, terminologyService);
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

  private void addWelcomeText() {
    // Recurring users do not need a welcome text: it only consumes the vertical
    // space the projects list needs. Only users without project access yet see
    // the onboarding welcome screen.
    if (hasAccessibleProjects()) {
      return;
    }
    Div titleAndDescription = new Div();
    titleAndDescription.addClassName("title-and-description");
    titleAndDescription.addClassName("detailed-welcome");
    Span title = new Span("Welcome to the Data Manager!");
    title.addClassNames("project-overview-title");
    Span descriptionStart = new Span(
        "Manage all your scientific data in one place with the Data Manager. You can access our ");
    Anchor descriptionLinkToDoc = new Anchor(
        "https://qbicsoftware.github.io/research-data-management/",
        "documentation", AnchorTarget.BLANK);
    Span descriptionEnd = new Span(
        " and learn more about using the Data Manager.");
    Div description = new Div(descriptionStart, descriptionLinkToDoc, descriptionEnd);
    description.addClassName("description");
    titleAndDescription.add(title, description);
    add(titleAndDescription);
  }

  /**
   * Checks whether the current user can access at least one project.
   *
   * <p>Reuses the same access-rights lookup as the project overview queries: the
   * count is restricted to the user's accessible projects, so it stays cheap.</p>
   *
   * @return true if the current user has access to at least one project
   */
  private boolean hasAccessibleProjects() {
    return projectInformationService.countOverview("") > 0;
  }

  /**
   * Seeds the paginated project collection from the URL on page load/reload and registers the
   * browser-history handler so that back/forward navigation restores previous list states
   * (USER-R-03).
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    History history = UI.getCurrent().getPage().getHistory();
    History.HistoryStateChangeHandler currentHandler = history.getHistoryStateChangeHandler();
    if (currentHandler != listStateHistoryHandler) {
      // Remember what to hand back on leave; on re-entry without a leave (e.g. a reroute) the
      // previously captured router handler must not be overwritten by this view's own handler.
      routerHistoryStateChangeHandler = currentHandler;
    }
    history.setHistoryStateChangeHandler(listStateHistoryHandler);
    ListState state = ListStateCodec.parse(event.getLocation().getQueryParameters(),
        ProjectOverviewSortOption.defaultSort(), ProjectOverviewSortOption.allowedSortOrders());
    projectCollectionComponent.applyExternalState(state);
  }

  /**
   * Gives the history state change handler back to the router when this view is left.
   *
   * <p>Without this, the handler registered in {@link #beforeEnter(BeforeEnterEvent)} stays
   * installed for the whole UI and every later navigation — link clicks included — is delivered to
   * this view instead of to the router.
   */
  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    getUI().ifPresent(ui -> ui.getPage().getHistory()
        .setHistoryStateChangeHandler(routerHistoryStateChangeHandler));
  }

  /**
   * Re-applies the list state when the browser history changes (back/forward or router-link
   * navigation). State changes triggered by the view itself are ignored because they are applied
   * directly by the component and never fire a history change event server-side.
   */
  private void onHistoryStateChange(HistoryStateChangeEvent event) {
    if (!ProjectRoutes.PROJECTS.equals(event.getLocation().getPath())) {
      // Not our list (e.g. a project card router link): the event belongs to the router, so it is
      // passed to the handler this view replaced instead of being dropped.
      if (routerHistoryStateChangeHandler != null) {
        routerHistoryStateChangeHandler.onHistoryStateChange(event);
      }
      return;
    }
    ListState state = ListStateCodec.parse(event.getLocation().getQueryParameters(),
        ProjectOverviewSortOption.defaultSort(), ProjectOverviewSortOption.allowedSortOrders());
    projectCollectionComponent.applyExternalState(state);
  }

  private boolean isOfferSearchAllowed() {
    Set<String> allowedRoles = new HashSet<>(Arrays.asList("ROLE_ADMIN", "ROLE_PROJECT_MANAGER"));
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(r -> allowedRoles.contains(r.getAuthority()));
  }

  Optional<ProjectContact> mapToServiceApi(Contact contact) {
    if (contact.hasMinimalInformation()) {
      return Optional.of(new ProjectContact(contact.fullName(), contact.email(), contact.oidc(),
          contact.oidcIssuer()));
    }
    return Optional.empty();
  }

  private void createProject(ConfirmEvent confirmEvent) {
    ProjectCreationInformation projectCreationInformation = confirmEvent.projectCreationInformation();
    FundingEntry fundingEntry = projectCreationInformation.fundingEntry();
    ProjectDesign projectDesign = projectCreationInformation.projectDesign();

    var principalInvestigator = mapToServiceApi(projectCreationInformation.projectCollaborators()
        .principalInvestigator()).orElseThrow();
    var projectManager = mapToServiceApi(projectCreationInformation.projectCollaborators()
        .projectManager()).orElseThrow();
    var responsiblePerson = mapToServiceApi(projectCreationInformation.projectCollaborators()
        .responsiblePerson());

    var projectContacts = responsiblePerson
        .map(responsible -> new ProjectContacts(principalInvestigator, projectManager, responsible))
        .orElse(new ProjectContacts(principalInvestigator, projectManager));
    FundingInformation fundingInformation = null;
    if (fundingEntry != null && !fundingEntry.isEmpty()) {
      fundingInformation = new FundingInformation(fundingEntry.getLabel(),
          fundingEntry.getReferenceId());
    }

    Result<Project, ApplicationException> project = projectCreationService.createProject(
        projectDesign.getOfferId(), projectDesign.getProjectCode(), projectDesign.getProjectTitle(),
        projectDesign.getProjectObjective(), projectContacts, fundingInformation);
    handleResultProject(project, confirmEvent);
    ExperimentalInformation experimentalInformation = confirmEvent.experimentalInformation();
    var experiment = addExperimentToProjectService.addExperimentToProject(
        project.getValue().getId(),
        experimentalInformation.getExperimentName(),
        experimentalInformation.getSpecies(),
        experimentalInformation.getSpecimens(),
        experimentalInformation.getAnalytes());
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
