package life.qbic.datamanager.views.projects.project.experiments;


import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteParam;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.DataManagerMenu;
import life.qbic.datamanager.views.navigation.ProjectSideNavigationComponent;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentNavigationComponent.RoutingTab;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The ExperimentMainLayout functions as a layout which contains all views related to managing
 * a project. It provides an app drawer within which the {@link ProjectSideNavigationComponent}
 * allows the user to navigate within the selected project. </b> Additionally it provides a navbar
 * which provides buttons to toggle the app drawer, for logout purposes and for routing back to the
 * home {@link ProjectOverviewMain} view. Finally, it provides a dedicated navbar
 * {@link ExperimentNavigationComponent} which allows the user to navigate within the pages of an
 * experiment.
 */
@PageTitle("Data Manager")
public class ExperimentMainLayout extends AppLayout implements BeforeEnterObserver {

  private static final Logger log = getLogger(ExperimentMainLayout.class);
  private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private final ProjectSideNavigationComponent projectSideNavigationComponent;
  private final ExperimentNavigationComponent experimentNavigationComponent = new ExperimentNavigationComponent();
  private final DataManagerMenu dataManagerMenu;
  private final transient ExperimentInformationService experimentInformationService;
  private Context context = new Context();
  private final Span navBarTitle = new Span();

  public ExperimentMainLayout(@Autowired LogoutService logoutService,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired UserPermissions userPermissions,
      @Autowired OntologyLookupService ontologyTermInformationService) {
    Objects.requireNonNull(logoutService);
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(addExperimentToProjectService);
    this.dataManagerMenu = new DataManagerMenu(logoutService);
    this.experimentInformationService = experimentInformationService;
    this.projectSideNavigationComponent = new ProjectSideNavigationComponent(
        projectInformationService, experimentInformationService, addExperimentToProjectService,
        userPermissions, ontologyTermInformationService);
    initializeNavbar();
    initializeAppDrawer();
    addClassName("experiment-main-layout");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectId = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    if (!ProjectId.isValid(projectId)) {
      throw new ApplicationException("invalid project id " + projectId);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    context = new Context().with(parsedProjectId);
    String experimentId = beforeEnterEvent.getRouteParameters()
        .get(EXPERIMENT_ID_ROUTE_PARAMETER).orElseThrow();
    if (!ExperimentId.isValid(experimentId)) {
      throw new ApplicationException("invalid experiment id " + experimentId);
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
    context = context.with(parsedExperimentId);
    setExperimentNameAsTitle(context.experimentId().orElseThrow());
    setSelectedExperimentTab(beforeEnterEvent.getNavigationTarget());
  }

  private void setExperimentNameAsTitle(ExperimentId experimentId) {
    experimentInformationService.find(context.projectId().orElseThrow().value(), experimentId)
        .ifPresent(
            experiment -> navBarTitle.setText(experiment.getName()));
  }

  private void initializeNavbar() {
    addToNavbar(createAppNavigationBar());
    addToNavbar(createExperimentNavigationBar());
    navBarTitle.setClassName("experiment-navbar-title");
  }

  private void initializeAppDrawer() {
    Span drawerTitle = new Span("Data Manager");
    drawerTitle.addClassName("project-navigation-drawer-title");
    addToDrawer(drawerTitle, projectSideNavigationComponent);
    setPrimarySection(Section.DRAWER);
  }

  private Span createDrawerToggleAndTitleBar() {
    Span drawerToggleAndTitleBar = new Span();
    DrawerToggle drawerToggle = new DrawerToggle();
    drawerToggleAndTitleBar.add(drawerToggle, navBarTitle);
    drawerToggleAndTitleBar.addClassName("drawer-title-bar");
    return drawerToggleAndTitleBar;
  }

  private Span createAppNavigationBar() {
    Span appNavigationBar = new Span();
    appNavigationBar.addClassNames("experiment-app-navbar");
    appNavigationBar.add(createDrawerToggleAndTitleBar(), dataManagerMenu);
    return appNavigationBar;
  }

  private Div createExperimentNavigationBar() {
    experimentNavigationComponent.addListener(
        experimentNavigationTriggeredEvent -> routeToExperimentPage(
            experimentNavigationTriggeredEvent.getSource().navigationTarget()));
    return experimentNavigationComponent;
  }

  /**
   * Sets the experiment tab within the {@link com.vaadin.flow.component.tabs.TabSheet}to the navigation target provided by the
   * {@link BeforeEnterEvent}
   *
   * @param navigationTarget java {@link Class} containing the route to which the selected tab
   *                         leads.
   */

  private void setSelectedExperimentTab(Class<?> navigationTarget) {
    List<RoutingTab> routingTabList = experimentNavigationComponent.experimentNavigationTabs.getChildren()
        .filter(component -> component instanceof RoutingTab<?>)
        .map(component -> (RoutingTab) component).toList();
    RoutingTab<?> selectedRoutingTab = routingTabList.stream()
        .filter(routingTab -> routingTab.navigationTarget().equals(navigationTarget))
        .findFirst().orElseThrow();
    experimentNavigationComponent.experimentNavigationTabs.setSelectedTab(selectedRoutingTab);
  }

  private void routeToExperimentPage(Class<Component> navigationTarget) {
    RouteParam projectRouteParam = new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
        context.projectId().orElseThrow().value());
    RouteParam experimentRouteParam = new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
        context.experimentId().orElseThrow().value());
    getUI().ifPresent(ui -> ui.navigate(navigationTarget, projectRouteParam, experimentRouteParam));
  }
}
