package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.create.ProjectCreationWizard.ProjectCreationInformation;
import life.qbic.datamanager.views.projects.create.ProjectDesignLayout.ProjectDesign;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.finances.api.FinanceService;
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
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Project creation page.
 *
 * <p>Hosts {@link ProjectCreationWizard} at {@code projects/create} instead of opening the former
 * {@code AddProjectDialog} (project owner, 2026-10-19). The last wizard step creates the first
 * experiment, but it is optional: a project can be created on its own, and experiments are then
 * created from the project summary. On success the user lands on the new project's summary.</p>
 *
 * @since 1.19.0
 */
@SpringComponent
@UIScope
@Route(value = ProjectRoutes.PROJECT_CREATION, layout = UserMainLayout.class)
@PermitAll
public class ProjectCreateMain extends Main {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String PROJECT_ID_PARAMETER = "projectId";

  private final transient ProjectCreationService projectCreationService;
  private final transient AddExperimentToProjectService addExperimentToProjectService;
  private final transient MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final ProjectCreationWizard wizard;

  public ProjectCreateMain(
      @Autowired ProjectInformationService projectInformationService,
      @Autowired FinanceService financeService,
      @Autowired PersonLookupService personLookupService,
      @Autowired SpeciesLookupService speciesLookupService,
      @Autowired TerminologyService terminologyService,
      @Autowired ProjectCreationService projectCreationService,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.projectCreationService = requireNonNull(projectCreationService);
    this.addExperimentToProjectService = requireNonNull(addExperimentToProjectService);
    this.messageSourceNotificationFactory = requireNonNull(messageSourceNotificationFactory);

    this.wizard = new ProjectCreationWizard(projectInformationService, financeService,
        personLookupService, speciesLookupService, terminologyService);
    if (isOfferSearchAllowed()) {
      wizard.enableOfferSearch();
    }
    wizard.addCreateListener(this::createProject);
    wizard.addCancelListener(event -> navigateToProjects());

    addClassName("project-create");
    layout();
  }

  private void layout() {
    Div band = new Div();
    band.addClassName("project-create-band");

    Button back = new Button("Back to projects", VaadinIcon.ARROW_LEFT.create());
    back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    back.addClassName("experiment-design-back");
    back.addClickListener(event -> navigateToProjects());

    Span title = new Span("Create project");
    title.addClassName("project-create-title");

    Span hint = new Span("Set up the project. Creating an experiment is an optional last step — you "
        + "can also create it later from the project summary.");
    hint.addClassName("project-create-hint");

    Div titleBlock = new Div(back, title, hint);
    titleBlock.addClassName("experiment-design-title-block");

    band.add(titleBlock, wizard);
    add(band);
  }

  private void createProject(ProjectCreationWizard.CreateEvent event) {
    ProjectCreationInformation information = event.projectCreationInformation();
    ProjectDesign design = information.projectDesign();
    FundingEntry fundingEntry = information.fundingEntry();

    var principalInvestigator = mapToServiceApi(
        information.projectCollaborators().principalInvestigator()).orElseThrow();
    var projectManager = mapToServiceApi(
        information.projectCollaborators().projectManager()).orElseThrow();
    var responsiblePerson = mapToServiceApi(
        information.projectCollaborators().responsiblePerson());

    ProjectContacts projectContacts = responsiblePerson
        .map(responsible -> new ProjectContacts(principalInvestigator, projectManager, responsible))
        .orElse(new ProjectContacts(principalInvestigator, projectManager));
    FundingInformation fundingInformation = null;
    if (fundingEntry != null && !fundingEntry.isEmpty()) {
      fundingInformation = new FundingInformation(fundingEntry.getLabel(),
          fundingEntry.getReferenceId());
    }

    Result<Project, ApplicationException> project = projectCreationService.createProject(
        design.getOfferId(), design.getProjectCode(), design.getProjectTitle(),
        design.getProjectObjective(), projectContacts, fundingInformation);
    if (project.isError()) {
      displayToast("project.created.error", null);
      return;
    }
    ProjectId projectId = project.getValue().getId();

    event.experimentalInformation().ifPresent(experimental -> {
      var experiment = addExperimentToProjectService.addExperimentToProject(projectId,
          experimental.getExperimentName(), experimental.getSpecies(),
          experimental.getSpecimens(), experimental.getAnalytes());
      if (experiment.isError()) {
        displayToast("experiment.created.error", null);
      } else {
        displayToast("experiment.created.success", experimental.getExperimentName());
      }
    });
    displayToast("project.created.success", design.getProjectTitle());

    navigateToSummary(projectId);
  }

  private static Optional<ProjectContact> mapToServiceApi(Contact contact) {
    if (contact.hasMinimalInformation()) {
      return Optional.of(new ProjectContact(contact.fullName(), contact.email(), contact.oidc(),
          contact.oidcIssuer()));
    }
    return Optional.empty();
  }

  private void displayToast(String messageKey, Object argument) {
    Object[] arguments = argument == null ? new Object[]{} : new Object[]{argument};
    Toast toast = messageSourceNotificationFactory.toast(messageKey, arguments, getLocale());
    toast.open();
  }

  private void navigateToSummary(ProjectId projectId) {
    RouteParameters routeParameters = new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, projectId.value()));
    getUI().ifPresent(ui -> ui.navigate(ProjectInformationMain.class, routeParameters));
  }

  private void navigateToProjects() {
    getUI().ifPresent(ui -> ui.navigate(ProjectOverviewMain.class));
  }

  private static boolean isOfferSearchAllowed() {
    Set<String> allowedRoles = new HashSet<>(Arrays.asList("ROLE_ADMIN", "ROLE_PROJECT_MANAGER"));
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getAuthorities().stream()
        .anyMatch(role -> allowedRoles.contains(role.getAuthority()));
  }
}
