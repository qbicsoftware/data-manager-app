package life.qbic.datamanager.views.projects.project.experiments;


import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteParam;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.navigation.ProjectSideNavigationComponent;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewPage;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentNavigationComponent.RoutingTab;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The ExperimentMainLayout functions as a layout which contains all views related to managing
 * a project. It provides an app drawer within which the {@link ProjectSideNavigationComponent}
 * allows the user to navigate within the selected project. </b> Additionally it provides a navbar
 * which provides buttons to toggle the app drawer, for logout purposes and for routing back to the
 * home {@link ProjectOverviewPage} view. Finally, it provides a dedicated navbar
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
  private final Div navBarContent = new Div();
  private final transient LogoutService logoutService;
  private final transient ExperimentInformationService experimentInformationService;
  private Context context = new Context();
  private final Span navBarTitle = new Span("Test");

  public ExperimentMainLayout(@Autowired LogoutService logoutService,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(logoutService);
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.logoutService = logoutService;
    this.experimentInformationService = experimentInformationService;
    this.projectSideNavigationComponent = new ProjectSideNavigationComponent(
        projectInformationService,
        experimentInformationService);
    initializeAppDrawer();
    addToNavbar(navBarContent);
    initializeNavbar();
    addListener(LogoutTriggeredEvent.class, this::logoutTriggeredListener);
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
    experimentInformationService.find(experimentId)
        .ifPresent(
            experiment -> navBarTitle.setText(experiment.getName()));
  }

  private void initializeNavbar() {
    navBarContent.addClassName("experiment-navbar");
    navBarContent.addComponentAsFirst(createAppNavigationBar());
    navBarContent.add(createExperimentNavigationBar());
    addToNavbar(navBarContent);
    navBarTitle.setClassName("experiment-navbar-title");
  }

  private void initializeAppDrawer() {
    Span drawerTitle = new Span("Data Manager");
    drawerTitle.addClassName("project-navigation-drawer-title");
    addToDrawer(drawerTitle, projectSideNavigationComponent);
    setPrimarySection(Section.DRAWER);
  }

  private Span createButtonBar() {
    Span buttonBar = new Span();
    buttonBar.addClassName("experiment-navbar-buttonbar");
    Button homeButton = new Button("Home");
    Button logout = new Button("Log out");
    buttonBar.add(homeButton, logout);
    homeButton.addClickListener(event -> routeToProjectOverview());
    logout.addClickListener(
        event -> fireEvent(new LogoutTriggeredEvent(logout, event.isFromClient())));
    return buttonBar;
  }

  private Span createDrawerToggleAndTitleBar() {
    Span drawerToggleAndTitleBar = new Span();
    DrawerToggle drawerToggle = new DrawerToggle();
    drawerToggleAndTitleBar.add(drawerToggle, navBarTitle);
    return drawerToggleAndTitleBar;
  }

  private Span createAppNavigationBar() {
    Span appNavigationBar = new Span();
    appNavigationBar.addClassNames("experiment-app-navbar");
    appNavigationBar.add(createDrawerToggleAndTitleBar(), createButtonBar());
    return appNavigationBar;
  }

  private Div createExperimentNavigationBar() {
    experimentNavigationComponent.addListener(
        experimentNavigationTriggeredEvent -> routeToExperimentPage(
            experimentNavigationTriggeredEvent.getSource().navigationTarget()));
    return experimentNavigationComponent;
  }

  /**
   * Sets the experiment tab within the tabsheet to the navigation target provided by the
   * beforeEnterEvent
   *
   * @param navigationTarget {@link Class} java class containing the route to which the selected tab
   *                         leads.
   */

  private void setSelectedExperimentTab(Class<?> navigationTarget) {
    List<RoutingTab> routingTabList = experimentNavigationComponent.experimentNavigationTabs.getChildren()
        .filter(component -> component instanceof RoutingTab<?>)
        .map(component -> (RoutingTab) component).toList();
    RoutingTab selectedRoutingTab = routingTabList.stream()
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

  private void logoutTriggeredListener(LogoutTriggeredEvent logoutTriggeredEvent) {
    logoutService.logout();
  }

  private void routeToProjectOverview() {
    //getUI is not possible on the ProjectSideNavigationComponent directly in a static context
    navBarContent.getUI().ifPresent(ui -> ui.navigate(ProjectOverviewPage.class));
    log.debug("Routing to ProjectOverview page");
  }

  private class LogoutTriggeredEvent extends ComponentEvent<Component> {

    @Serial
    private static final long serialVersionUID = 5541927397309901474L;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public LogoutTriggeredEvent(Component source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }
}
