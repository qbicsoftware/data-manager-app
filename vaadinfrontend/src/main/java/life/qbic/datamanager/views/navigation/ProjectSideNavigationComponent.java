package life.qbic.datamanager.views.navigation;

import static org.slf4j.LoggerFactory.getLogger;

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
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
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
  private final Div content;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private Context context = new Context();

  public ProjectSideNavigationComponent(
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    content = new Div();
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    addClassName("project-navigation-drawer");
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    content.addClassName("content");
    log.debug(
        "New instance for ProjectSideNavigationComponent {} was created",
        System.identityHashCode(this));
    add(content);
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
    List<ProjectPreview> lastModifiedProjects = retrieveLastModifiedProjects();
    content.add(generateNavigationSections(project, lastModifiedProjects, experiments).toArray(
        Component[]::new));
    addListener(ProjectNavigationEvent.class,
        ProjectSideNavigationComponent::addProjectNavigationListener);
  }

  private Project loadProject(ProjectId id) {
    return projectInformationService.find(id).orElseThrow();
  }

  private List<Experiment> loadExperimentsForProject(Project project) {
    return project.experiments().stream()
        .map(experimentInformationService::find).filter(Optional::isPresent).map(Optional::get)
        .toList();
  }

  private List<ProjectPreview> retrieveLastModifiedProjects() {
    List<SortOrder> sortOrders = Collections.singletonList(
        SortOrder.of("lastModified").descending());
    return projectInformationService.queryPreview("", 0, 4, sortOrders);
  }

  private static List<Div> generateNavigationSections(Project project,
      List<ProjectPreview> lastModifiedProjects, List<Experiment> experiments) {
    Div projectSection = createProjectSection(project, lastModifiedProjects);
    Div experimentSection = createExperimentSection(project.getId().value(), experiments);
    return List.of(projectSection, experimentSection);
  }

  private static Div createProjectSection(Project project,
      List<ProjectPreview> lastModifiedProjects) {
    Div projectSection = new Div();
    projectSection.add(createProjectHeader(),
        createProjectSelection(project.getProjectIntent().projectTitle().title(),
            lastModifiedProjects),
        generateSectionDivider(), createProjectItems(project.getId().value()));
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
      List<ProjectPreview> projectPreviews) {
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
    projectPreviews.forEach(preview -> addRecentProjectItemToSubMenu(preview, subMenu));
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

  private static void addRecentProjectItemToSubMenu(ProjectPreview preview, SubMenu subMenu) {
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

  private static void addProjectNavigationListener(ProjectNavigationEvent projectNavigationEvent) {
    projectNavigationEvent.projectId().ifPresentOrElse(
        ProjectSideNavigationComponent::routeToProject,
        ProjectSideNavigationComponent::routeToProjectOverview);
  }

  private static void routeToProjectOverview() {
    //getUI is not possible on the ProjectSideNavigationComponent directly in a static context
    Optional.ofNullable(UI.getCurrent())
        .ifPresentOrElse(ui -> ui.navigate(ProjectOverviewPage.class),
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
    log.debug("Routing to ProjectInformation page for project " + projectId.value());
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
