package life.qbic.datamanager.views.navigation;

import static org.slf4j.LoggerFactory.getLogger;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

  private static final Logger log = getLogger(ProjectSideNavigationComponent.class);
  private final Div projectSection = new Div();
  private final SideNavItem projectSectionHeader = new SideNavItem("");
  private final Div experimentSection = new Div();
  private final SideNavItem experimentSectionHeader = new SideNavItem("");
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private transient Context context = new Context();
  private final MenuBar projectSelectMenu = new MenuBar();

  public ProjectSideNavigationComponent(
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.addClassName("project-navigation-drawer");
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    projectSection.addClassName("project-section");
    experimentSection.addClassName("experiment-section");
    add(projectSection, experimentSection);
    log.debug(
        "New instance for ProjectSideNavigationComponent {} was created",
        System.identityHashCode(this));
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    resetSections();
    String projectId = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    this.context = new Context().with(parsedProjectId);
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresent(experimentId -> this.context = context.with(ExperimentId.parse(experimentId)));
    setContext();
  }

  private void setContext() {
    initializeProjectSection(context.projectId().orElseThrow());
    initializeExperimentSection(context.projectId().orElseThrow());
  }

  private void initializeProjectSection(ProjectId projectId) {
    projectSectionHeader.setLabel("PROJECT");
    projectSectionHeader.setPrefixComponent(VaadinIcon.NOTEBOOK.create());
    projectSectionHeader.addClassName("primary");
    String projectInformationPath = String.format(Projects.PROJECT_INFO, projectId.value());
    String projectAccessPath = String.format(Projects.ACCESS, projectId.value());
    SideNavItem projectInformationItem = new SideNavItem("PROJECT INFORMATION",
        projectInformationPath, VaadinIcon.DEINDENT.create());
    SideNavItem projectAccessItem = new SideNavItem("PROJECT ACCESS MANAGEMENT", projectAccessPath,
        VaadinIcon.USERS.create());
    projectSection.addComponentAsFirst(projectSectionHeader);
    MenuBar projectSelect = createProjectSelect(projectId);
    projectSection.add(projectSelect, generateLineDivider(), projectInformationItem,
        projectAccessItem);
  }

  private MenuBar createProjectSelect(ProjectId projectId) {
    projectSelectMenu.removeAll();
    projectSelectMenu.addClassNames("project-select-menu");
    String projectTitle = retrieveCurrentlySelectedProject(projectId).projectTitle();
    Span currentlySelectedProject = new Span(projectTitle);
    currentlySelectedProject.addClassName("selected-project-title");
    Icon dropdownIcon = VaadinIcon.CHEVRON_DOWN_SMALL.create();
    dropdownIcon.addClassName(IconSize.SMALL);
    Span selectedProject = new Span(currentlySelectedProject, dropdownIcon);
    selectedProject.addClassName("selected-project");
    MenuItem item = projectSelectMenu.addItem(selectedProject, projectTitle);
    generateProjectMenuOptions(item);
    return projectSelectMenu;
  }

  private ProjectPreview retrieveCurrentlySelectedProject(ProjectId projectId) {
    Project project = projectInformationService.find(projectId).orElseThrow();
    return projectInformationService.queryPreview(project.getProjectIntent().projectTitle().title(),
        0, 1, new ArrayList<>()).stream().findFirst().orElseThrow();
  }

  private void generateProjectMenuOptions(MenuItem menuItem) {
    SubMenu subMenu = menuItem.getSubMenu();
    Span recentProjectsHeader = new Span("Recent Projects");
    recentProjectsHeader.addClassName("recent-projects-header");
    Span projectOverviewRouteComponent = new Span("Go To Projects");
    subMenu.addItem(projectOverviewRouteComponent, event -> routeToProjectOverview());
    subMenu.add(generateLineDivider());
    subMenu.add(recentProjectsHeader);
    retrieveLastModifiedProjects().forEach(
        preview -> {
          MenuItem recentProject = subMenu.addItem(String.format("%s - %s", preview.projectCode(),
              preview.projectTitle()), event -> routeToProject(preview.projectId()));
          recentProject.addClassName("transparent-icon");
        });
  }

  private List<ProjectPreview> retrieveLastModifiedProjects() {
    List<SortOrder> sortOrders = Collections.singletonList(
        SortOrder.of("lastModified").descending());
    return projectInformationService.queryPreview("", 0, 3, sortOrders);
  }

  private void initializeExperimentSection(ProjectId projectId) {
    experimentSectionHeader.removeAll();
    experimentSectionHeader.setLabel("Experiments");
    experimentSectionHeader.setPrefixComponent(VaadinIcon.FLASK.create());
    experimentSectionHeader.addClassName("primary");
    List<Experiment> experiments = experimentInformationService.findAllForProject(projectId);
    experiments.forEach(
        experiment -> experimentSectionHeader.addItem(createExperimentItem(projectId, experiment)));
    experimentSection.addComponentAsFirst(experimentSectionHeader);
    experimentSectionHeader.setExpanded(true);
  }

  private SideNavItem createExperimentItem(ProjectId projectId, Experiment experiment) {
    String experimentPath = String.format(Projects.EXPERIMENT, projectId.value(),
        experiment.experimentId().value());
    SideNavItem sideNavItem = new SideNavItem(experiment.getName(), experimentPath);
    sideNavItem.addClassName("hoverable");
    return sideNavItem;
  }

  private Hr generateLineDivider() {
    return new Hr();
  }

  private void resetSections() {
    projectSection.removeAll();
    experimentSection.removeAll();
  }

  private void routeToProject(ProjectId projectId) {
    RouteParameters routeParameters = new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectId.value()));
    getUI().ifPresent(ui -> ui.navigate(ProjectInformationMain.class, routeParameters));
    log.debug("Routing to ProjectInformation page for project " + projectId.value());
  }

  private void routeToProjectOverview() {
    getUI().ifPresent(ui -> ui.navigate(ProjectOverviewPage.class));
    log.debug("Routing to ProjectOverview page");
  }
}
