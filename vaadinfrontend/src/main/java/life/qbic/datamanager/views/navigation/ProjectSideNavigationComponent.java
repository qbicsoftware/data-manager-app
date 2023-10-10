package life.qbic.datamanager.views.navigation;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
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
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewPage;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

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
  private static final Logger log = getLogger(ProjectSideNavigationComponent.class);
  private static Div content;
  private static ProjectInformationService projectInformationService;
  private static ExperimentInformationService experimentInformationService;
  private Context context = new Context();

  public ProjectSideNavigationComponent(
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.addClassName("project-navigation-drawer");
    ProjectSideNavigationComponent.projectInformationService = projectInformationService;
    ProjectSideNavigationComponent.experimentInformationService = experimentInformationService;
    content = new Div();
    content.addClassName("content");
    add(content);
    log.debug(
        "New instance for ProjectSideNavigationComponent {} was created",
        System.identityHashCode(this));
  }

  private static List<Experiment> loadExperimentsForProject(Project project) {
    return project.experiments().stream()
        .map(experimentInformationService::find).filter(Optional::isPresent).map(Optional::get)
        .toList();
  }

  private static SideNavItem createProjectHeader() {
    SideNavItem projectHeader = new SideNavItem("PROJECT");
    projectHeader.setLabel("PROJECT");
    projectHeader.setPrefixComponent(VaadinIcon.NOTEBOOK.create());
    projectHeader.addClassName("primary");
    return projectHeader;
  }

  private static List<ProjectPreview> retrieveLastModifiedProjects() {
    List<SortOrder> sortOrders = Collections.singletonList(
        SortOrder.of("lastModified").descending());
    return projectInformationService.queryPreview("", 0, 3, sortOrders);
  }

  private static Span generateSectionDivider() {
    Span sectionDivider = new Span(new Hr());
    sectionDivider.addClassName("section-divider");
    return sectionDivider;
  }

  private static void resetContent() {
    content.removeAll();
  }

  private static Project loadProject(ProjectId id) {
    return projectInformationService.find(id).orElseThrow();
  }

  private Div createProjectSection(String projectId, String projectTitle) {
    Div projectSection = new Div();
    projectSection.add(createProjectHeader(), createProjectSelection(projectTitle),
        generateSectionDivider(), createProjectItems(projectId));
    projectSection.addClassName("project-section");
    return projectSection;
  }

  private static Div createProjectItems(String projectId) {
    Div projectItems = new Div();
    projectItems.add(createProjectSummaryLink(projectId), createProjectUsers(projectId));
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

  private static Div createExperimentSection(String projectId, List<Experiment> experimentsList) {
    Div experimentSection = new Div();
    SideNavItem experiments = new SideNavItem("");
    experiments.setLabel("EXPERIMENTS");
    experiments.setPrefixComponent(VaadinIcon.FLASK.create());
    experimentsList.forEach(
        experiment -> experiments.addItem(
            createItemFromExperiment(projectId, experiment.experimentId().value(),
                experiment.getName())));
    experiments.setExpanded(true);
    experiments.addClassName("experiment-section");
    experiments.addClassName("primary");
    experimentSection.add(experiments);
    return experimentSection;
  }

  private static SideNavItem createItemFromExperiment(String projectId, String experimentId,
      String experimentLabel) {
    String experimentPath = String.format(Projects.EXPERIMENT, projectId,
        experimentId);
    SideNavItem sideNavItem = new SideNavItem(experimentLabel, experimentPath);
    sideNavItem.addClassName("hoverable");
    return sideNavItem;
  }

  //ToDo Make static and move NavigationEventTriggering to director class
  private MenuBar createProjectSelection(String projectTitle) {
    MenuBar projectSelection = new MenuBar();
    projectSelection.addClassNames("project-selection-menu");
    Span selectedProjectTitle = new Span(projectTitle);
    selectedProjectTitle.addClassName("selected-project-title");
    Icon dropdownIcon = VaadinIcon.CHEVRON_DOWN_SMALL.create();
    dropdownIcon.addClassName(IconSize.SMALL);
    Span dropDownField = new Span(selectedProjectTitle, dropdownIcon);
    dropDownField.addClassName("dropdown-field");
    MenuItem item = projectSelection.addItem(dropDownField, projectTitle);
    addSelectionOptions(item);
    return projectSelection;
  }

  //ToDo Make static and move NavigationEventTriggering to director class
  private void addSelectionOptions(MenuItem menuItem) {
    SubMenu projectSelectionSubMenu = menuItem.getSubMenu();
    Span recentProjectsHeader = new Span("Recent Projects");
    recentProjectsHeader.addClassName("recent-projects-header");
    Span projectOverviewRouteComponent = new Span("Go To Projects");
    projectSelectionSubMenu.addItem(projectOverviewRouteComponent,
        event -> fireNavigationEvent(event.getSource(), null, event.isFromClient()));
    projectSelectionSubMenu.add(generateSectionDivider());
    projectSelectionSubMenu.add(recentProjectsHeader);
    retrieveLastModifiedProjects().forEach(
        preview -> addRecentProjectItem(projectSelectionSubMenu, preview));
  }

  //ToDo Make static and move NavigationEventTriggering to director class
  private void addRecentProjectItem(SubMenu subMenu, ProjectPreview preview) {
    MenuItem recentProject = subMenu.addItem(String.format("%s - %s", preview.projectCode(),
            preview.projectTitle()),
        (ComponentEventListener<ClickEvent<MenuItem>>) menuItemClickEvent -> fireNavigationEvent(
            menuItemClickEvent.getSource(), preview.projectId(),
            menuItemClickEvent.isFromClient()));
    recentProject.addClassName("transparent-icon");
  }

  private void routeToProject(ProjectId projectId) {
    RouteParameters routeParameters = new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectId.value()));
    getUI().ifPresent(ui -> ui.navigate(ProjectInformationMain.class, routeParameters));
    log.debug("Routing to ProjectInformation page for project " + projectId.value());
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    resetContent();
    String projectId = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    context = new Context().with(parsedProjectId);
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresent(experimentId -> context = context.with(ExperimentId.parse(experimentId)));
    var project = loadProject(parsedProjectId);
    List<Experiment> experiments = loadExperimentsForProject(project);
    //Todo generate director class handling building of each component section
    content.add(
        createProjectSection(parsedProjectId.value(),
            project.getProjectIntent().projectTitle().title()),
        createExperimentSection(parsedProjectId.value(), experiments));
    addListener(ProjectNavigationEvent.class, this::addNavigationListener);
  }

  private void fireNavigationEvent(Component component, ProjectId projectId,
      boolean fromClient) {

    var projectNavigationEvent = new ProjectNavigationEvent(component, projectId, fromClient);
    fireEvent(projectNavigationEvent);
  }

  private void addNavigationListener(ProjectNavigationEvent projectNavigationEvent) {
    projectNavigationEvent.projectId().ifPresentOrElse(this::routeToProject,
        this::routeToProjectOverview);
  }

  private void routeToProjectOverview() {
    getUI().ifPresent(ui -> ui.navigate(ProjectOverviewPage.class));
    log.debug("Routing to ProjectOverview page");
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
     * @param projectId  the {@link ProjectId} of the project to be navigated
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
