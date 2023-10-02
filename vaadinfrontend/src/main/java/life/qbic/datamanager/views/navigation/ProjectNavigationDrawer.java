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
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

@SpringComponent
@UIScope
public class ProjectNavigationDrawer extends Div implements BeforeEnterObserver {

  private static final Logger log = getLogger(ProjectNavigationDrawer.class);
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

  public ProjectNavigationDrawer(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.addClassName("project-navigation-drawer");
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    projectSection.addClassName("project-section");
    experimentSection.addClassName("experiment-section");
    add(projectSection, experimentSection);
    log.debug(String.format(
        "New instance for ProjectNavigationDrawer (#%s) was created",
        System.identityHashCode(this))
    );
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    resetDrawers();
    String projectId = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    this.context = new Context().with(parsedProjectId);
    if (beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER).isPresent()) {
      String experimentId = beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
          .orElseThrow();
      ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
      this.context = context.with(parsedExperimentId);
    }
    setContext();
  }

  private void setContext() {
    initializeProjectDrawerSection(context.projectId().orElseThrow());
    initializeExperimentDrawerSection(context.projectId().orElseThrow());
  }

  private void initializeProjectDrawerSection(ProjectId projectId) {
    projectSectionHeader.setLabel("PROJECT");
    projectSectionHeader.setPrefixComponent(VaadinIcon.BOOK.create());
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
    Span projectTitle = new Span(retrieveCurrentlySelectedProject(projectId).projectTitle());
    Icon dropdownIcon = VaadinIcon.CHEVRON_DOWN_SMALL.create();
    dropdownIcon.addClassName(IconSize.SMALL);
    Span selectedProject = new Span(projectTitle, dropdownIcon);
    selectedProject.addClassName("selected-project");
    MenuItem item = projectSelectMenu.addItem(selectedProject);
    SubMenu subMenu = item.getSubMenu();
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
    return projectSelectMenu;
  }

  private ProjectPreview retrieveCurrentlySelectedProject(ProjectId projectId) {
    Project project = projectInformationService.find(projectId).orElseThrow();
    return projectInformationService.queryPreview(project.getProjectIntent().projectTitle().title(),
        0, 1, new ArrayList<>()).stream().findFirst().orElseThrow();
  }

  private List<ProjectPreview> retrieveLastModifiedProjects() {
    List<SortOrder> sortOrders = Collections.singletonList(
        SortOrder.of("lastModified").descending());
    return projectInformationService.queryPreview("", 0, 3, sortOrders);
  }

  private void initializeExperimentDrawerSection(ProjectId projectId) {
    experimentSectionHeader.removeAll();
    experimentSectionHeader.setLabel("Experiments");
    experimentSectionHeader.setPrefixComponent(VaadinIcon.FLASK.create());
    experimentSectionHeader.addClassName("primary");
    List<Experiment> experiments = experimentInformationService.findAllForProject(projectId);
    experiments.forEach(
        experiment -> experimentSectionHeader.addItem(createExperimentItem(projectId, experiment)));
    experimentSection.addComponentAsFirst(experimentSectionHeader);
    context.experimentId().ifPresent(this::setActiveExperimentInSideNav);
  }

  /*Highlight the experiment as active if it was navigated to outside the drawer*/
  //ToDo Works on URL but not if clicked on experiment in ExperimentList?
  private void setActiveExperimentInSideNav(ExperimentId experimentId) {
    experimentSectionHeader.getItems().stream()
        .filter(sideNavItem -> sideNavItem.getPath().contains(experimentId.value()))
        .findFirst()
        .ifPresent(sideNavItem -> sideNavItem.getElement().setProperty("active", true));
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

  private void resetDrawers() {
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
