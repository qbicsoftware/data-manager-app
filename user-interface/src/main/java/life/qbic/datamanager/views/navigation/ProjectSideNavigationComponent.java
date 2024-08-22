package life.qbic.datamanager.views.navigation;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog.ExperimentAddEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.AddExperimentDialog.ExperimentDraft;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Project Side Navigation Component
 * <p>
 * Allows the user to switch between the components shown in each {@link ProjectMainLayout} by
 * clicking on the corresponding {@link SideNavItem} within Component which routes the user to the
 * respective route defined in {@link life.qbic.datamanager.views.AppRoutes} for the component in
 * question.
 */

@SpringComponent
@UIScope
public class ProjectSideNavigationComponent extends Div implements
    BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private static final Logger log = LoggerFactory.logger(ProjectSideNavigationComponent.class);
  private final Div content;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final AddExperimentToProjectService addExperimentToProjectService;
  private final transient UserPermissions userPermissions;
  private final TerminologyService terminologyService;
  private final SpeciesLookupService speciesLookupService;
  private Context context = new Context();

  public ProjectSideNavigationComponent(
      ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService,
      AddExperimentToProjectService addExperimentToProjectService,
      UserPermissions userPermissions,
      SpeciesLookupService speciesLookupService,
      TerminologyService terminologyService) {
    content = new Div();
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(addExperimentToProjectService);
    this.speciesLookupService = speciesLookupService;
    this.addExperimentToProjectService = addExperimentToProjectService;
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    addClassName("project-navigation-drawer");
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.terminologyService = Objects.requireNonNull(terminologyService);
    content.addClassName("content");
    add(content);
    addListener(ProjectNavigationEvent.class,
        ProjectSideNavigationComponent::addProjectNavigationListener);
    log.debug(
       "New instance for %s(#%s) created".formatted(
            this.getClass().getSimpleName(), (Integer) System.identityHashCode(this)));
  }

  private static Div createProjectSection(Project project,
      List<ProjectOverview> lastModifiedProjects, boolean canUserAdministrate) {
    Div projectSection = new Div();
    projectSection.add(createProjectHeader(),
        createProjectSelection(project.getProjectIntent().projectTitle().title(),
            lastModifiedProjects),
        generateSectionDivider(), createProjectItems(project.getId().value(), canUserAdministrate));
    projectSection.addClassName("project-section");
    return projectSection;
  }

  private static SideNavItem createProjectHeader() {
    SideNavItem projectHeader = new SideNavItem("PROJECT");
    projectHeader.setLabel("PROJECT");
    projectHeader.setPrefixComponent(VaadinIcon.NOTEBOOK.create());
    projectHeader.addClassName("primary");
    return projectHeader;
  }

  private static MenuBar createProjectSelection(String projectTitle,
      List<ProjectOverview> projectOverviews) {
    MenuBar projectSelection = new MenuBar();
    projectSelection.addClassNames("project-selection-menu");
    Span selectedProjectTitle = new Span(projectTitle);
    selectedProjectTitle.addClassName("selected-project-title");
    Icon dropdownIcon = VaadinIcon.CHEVRON_DOWN_SMALL.create();
    dropdownIcon.addClassName(IconSize.SMALL);
    Span dropDownField = new Span(selectedProjectTitle, dropdownIcon);
    dropDownField.addClassName("dropdown-field");
    MenuItem item = projectSelection.addItem(dropDownField, projectTitle);
    SubMenu subMenu = createProjectSelectionSubMenu(item);
    projectOverviews.forEach(preview -> addRecentProjectItemToSubMenu(preview, subMenu));
    return projectSelection;
  }

  private static SubMenu createProjectSelectionSubMenu(MenuItem menuItem) {
    SubMenu projectSelectionSubMenu = menuItem.getSubMenu();
    Span recentProjectsHeader = new Span("Recent Projects");
    recentProjectsHeader.addClassName("recent-projects-header");
    Span projectOverview = new Span("Go to Projects");
    MenuItem projectOverviewItem = projectSelectionSubMenu.addItem(projectOverview);
    projectOverviewItem.addClassName("transparent-icon");
    projectOverviewItem.addSingleClickListener(event -> routeToProjectOverview());
    projectSelectionSubMenu.add(generateSectionDivider());
    projectSelectionSubMenu.add(recentProjectsHeader);
    return projectSelectionSubMenu;
  }

  private static void addRecentProjectItemToSubMenu(ProjectOverview preview, SubMenu subMenu) {
    MenuItem projectItem = subMenu.addItem(
        createRecentProjectItem(preview.projectCode(), preview.projectTitle()));
    projectItem.addClassName("transparent-icon");
    projectItem.addSingleClickListener(event -> routeToProject(preview.projectId()));
  }

  private static Span createRecentProjectItem(String projectCode, String projectTitle) {
    return new Span(String.format("%s - %s", projectCode, projectTitle));
  }

  private static Span generateSectionDivider() {
    Span sectionDivider = new Span(new Hr());
    sectionDivider.addClassName("section-divider");
    return sectionDivider;
  }

  private static Div createProjectItems(String projectId, boolean canUserAdministrate) {
    Div projectItems = new Div();
    projectItems.add(createProjectSummaryLink(projectId));
    if (canUserAdministrate) {
      projectItems.add(createProjectUsers(projectId));
    }
    projectItems.addClassName("project-items");
    return projectItems;
  }

  private static SideNavItem createProjectSummaryLink(String projectId) {
    String projectSummaryPath = String.format(Projects.PROJECT_INFO,
        projectId);
    return new SideNavItem("SUMMARY",
        projectSummaryPath, VaadinIcon.DEINDENT.create());
  }

  private static SideNavItem createProjectUsers(String projectId) {
    String projectUsersPath = String.format(Projects.ACCESS, projectId);
    return new SideNavItem("USERS", projectUsersPath,
        VaadinIcon.USERS.create());
  }

  private static SideNavItem createItemFromExperiment(String projectId, String experimentId,
      String experimentLabel) {
    String experimentPath = String.format(Projects.EXPERIMENT, projectId,
        experimentId);
    SideNavItem sideNavItem = new SideNavItem(experimentLabel, experimentPath);
    sideNavItem.addClassName("hoverable");
    return sideNavItem;
  }

  private static SideNavItem createOntologyLookupSideNavItem(String projectId) {
    String projectOntologyPath = String.format(Projects.ONTOLOGY, projectId);
    SideNavItem ontologySearch = new SideNavItem("Ontology Search", projectOntologyPath,
        LumoIcon.SEARCH.create());
    ontologySearch.addClassName("hoverable");
    ontologySearch.addClassName("primary");
    return ontologySearch;
  }

  private static void addProjectNavigationListener(ProjectNavigationEvent projectNavigationEvent) {
    projectNavigationEvent.projectId().ifPresentOrElse(
        ProjectSideNavigationComponent::routeToProject,
        ProjectSideNavigationComponent::routeToProjectOverview);
  }

  private static void routeToProjectOverview() {
    //getUI is not possible on the ProjectSideNavigationComponent directly in a static context
    Optional.ofNullable(UI.getCurrent())
        .ifPresentOrElse(ui -> ui.navigate(ProjectOverviewMain.class),
            () -> {
              throw new ApplicationException("No UI present for project navigation");
            });
    log.debug("Routing to ProjectOverview page");
  }

  private static void routeToProject(ProjectId projectId) {
    RouteParameters routeParameters = new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectId.value()));
    //getUI is not possible on the ProjectSideNavigationComponent directly in a static context
    Optional.ofNullable(UI.getCurrent())
        .ifPresentOrElse(ui -> ui.navigate(ProjectInformationMain.class, routeParameters), () ->
        {
          throw new ApplicationException("No UI present for project navigation");
        });
    log.debug("Routing to ProjectDesign page for project " + projectId.value());
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    content.removeAll();
    String projectId = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    context = new Context().with(parsedProjectId);
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresent(experimentId -> context = context.with(ExperimentId.parse(experimentId)));
    var project = loadProject(parsedProjectId);
    List<Experiment> experiments = loadExperimentsForProject(project);
    List<ProjectOverview> lastModifiedProjects = retrieveLastModifiedProjects();
    boolean canUserAdministrate = userPermissions.changeProjectAccess(parsedProjectId);
    content.add(
        generateNavigationSections(project, lastModifiedProjects, experiments, canUserAdministrate)
            .toArray(Component[]::new));
    content.add(createOntologyLookupSideNavItem(projectId));
  }

  private Project loadProject(ProjectId id) {
    return projectInformationService.find(id).orElseThrow();
  }

  private List<Experiment> loadExperimentsForProject(Project project) {
    return project.experiments().stream()
        .map(experimentId -> experimentInformationService.find(
            context.projectId().orElseThrow().value(), experimentId)).filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private List<ProjectOverview> retrieveLastModifiedProjects() {
    List<SortOrder> sortOrders = Collections.singletonList(
        SortOrder.of("lastModified").descending());
    return projectInformationService.queryOverview("", 0, 4, sortOrders);
  }

  private List<Div> generateNavigationSections(Project project,
      List<ProjectOverview> lastModifiedProjects, List<Experiment> experiments,
      boolean canUserAdministrate) {
    Div projectSection = createProjectSection(project, lastModifiedProjects, canUserAdministrate);
    Div experimentSection = createExperimentSection(project.getId().value(), experiments);
    return List.of(projectSection, experimentSection);
  }

  private Div createExperimentSection(String projectId, List<Experiment> experimentsList) {
    Div experimentSection = new Div();
    SideNavItem expHeader = new SideNavItem("EXPERIMENTS");
    Icon flask = VaadinIcon.FLASK.create();

    if (context.experimentId().isPresent()) {
      expHeader.addClassName("primary");
    }

    expHeader.setPrefixComponent(flask);
    experimentSection.add(expHeader);

    Icon addIcon = LumoIcon.PLUS.create();
    addIcon.addClassName("clickable");
    addIcon.addClickListener(
        event -> showAddExperimentDialog());
    expHeader.setSuffixComponent(addIcon);

    OpenSideNavItem experiments = new OpenSideNavItem("");
    experimentsList.forEach(
        experiment -> experiments.addItem(
            createItemFromExperiment(projectId, experiment.experimentId().value(),
                experiment.getName())));
    experimentSection.add(experiments);
    return experimentSection;
  }

  private void showAddExperimentDialog() {
    var creationDialog = new AddExperimentDialog(speciesLookupService,
        terminologyService);
    creationDialog.addExperimentAddEventListener(this::onExperimentAddEvent);
    creationDialog.addCancelListener(event -> event.getSource().close());
    creationDialog.open();
  }

  private void onExperimentAddEvent(ExperimentAddEvent event) {
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId createdExperiment = createExperiment(projectId, event.getExperimentDraft());
    event.getSource().close();
    displayExperimentCreationSuccess();
    routeToExperiment(createdExperiment);
  }

  private void routeToExperiment(ExperimentId experimentId) {
    RouteParameters routeParameters = new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
            context.projectId().map(ProjectId::value).orElseThrow()),
        new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER, experimentId.value()));
    getUI().ifPresent(ui -> ui.navigate(ExperimentInformationMain.class, routeParameters));
    log.debug("re-routing to ExperimentInformation page for experiment " + experimentId.value());
  }

  private ExperimentId createExperiment(ProjectId projectId,
      ExperimentDraft experimentDraft) {
    Result<ExperimentId, RuntimeException> result = addExperimentToProjectService.addExperimentToProject(
        projectId,
        experimentDraft.getExperimentName(),
        experimentDraft.getSpecies(),
        experimentDraft.getSpecimens(),
        experimentDraft.getAnalytes(),
        experimentDraft.getSpeciesIcon().getLabel(),
        experimentDraft.getSpecimenIcon().getLabel());
    if (result.isValue()) {

      return result.getValue();
    } else {
      throw new ApplicationException("Experiment Creation failed");
    }
  }

  private void displayExperimentCreationSuccess() {
    SuccessMessage successMessage = new SuccessMessage("Experiment Creation succeeded", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  public static class AddExperimentClickEvent extends ComponentEvent<ExperimentListComponent> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public AddExperimentClickEvent(ExperimentListComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  private static class ProjectNavigationEvent extends ComponentEvent<Component> {

    @Serial
    private static final long serialVersionUID = 7399764169934605506L;
    private final ProjectId projectId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param projectId  the {@link ProjectId} of the project to be navigated to
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ProjectNavigationEvent(Component source, ProjectId projectId,
        boolean fromClient) {
      super(source, fromClient);
      this.projectId = projectId;
    }

    Optional<ProjectId> projectId() {
      return Optional.ofNullable(projectId);
    }
  }

}
