package life.qbic.datamanager.views.projects.project.experiments.experiment;

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
import java.util.Objects;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.navigation.ProjectSideNavigationComponent;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewPage;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The ProjectMainLayout functions as a layout which contains all views related to managing a
 * project. It provides an app drawer within which the {@link ProjectNavigationBarComponent} allows
 * the user to navigate within the selected project. </b> Additionally it provides a navbar which
 * provides buttons to toggle the app drawer, for logout purposes and for routing back to the home
 * {@link ProjectOverviewPage} view
 */
@PageTitle("Data Manager")
public class ExperimentMainLayout extends AppLayout implements BeforeEnterObserver {

  private static final Logger log = getLogger(ExperimentMainLayout.class);
  private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private final ProjectSideNavigationComponent projectSideNavigationComponent;
  private static final Div navBarContent = new Div();
  private final transient LogoutService logoutService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient ProjectInformationService projectInformationService;
  private Context context = new Context();
  private final Span experimentTitle = new Span();

  public ExperimentMainLayout(@Autowired LogoutService logoutService,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(logoutService);
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.logoutService = logoutService;
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.projectSideNavigationComponent = new ProjectSideNavigationComponent(
        projectInformationService,
        experimentInformationService);
    initializeAppDrawer();
    addToNavbar(navBarContent);
    addClassName("experiment-main-layout");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    resetContent();
    String projectId = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    context = new Context().with(parsedProjectId);
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresent(experimentId -> this.context = context.with(ExperimentId.parse(experimentId)));
    if (context.experimentId().isEmpty()) {
      setProjectNameAsTitle(parsedProjectId);
    } else {
      String experimentId = beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
          .orElseThrow();
      ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
      context = context.with(parsedExperimentId);
      setExperimentNameAsTitle(parsedExperimentId);
    }
    initializeNavbar();
    addListener(LogoutTriggeredEvent.class, this::logoutTriggeredListener);
  }

  //ToDo Remove me once experimentId exists in sample route
  private void setProjectNameAsTitle(ProjectId projectId) {
    projectInformationService.find(projectId)
        .ifPresent(
            project -> experimentTitle.setText(project.getProjectIntent().projectTitle().title()));
  }

  private void setExperimentNameAsTitle(ExperimentId experimentId) {
    experimentInformationService.find(experimentId)
        .ifPresent(
            experiment -> experimentTitle.setText(experiment.getName()));
  }

  private void initializeNavbar() {
    navBarContent.addClassName("project-navbar-content");
    //ToDo extract and load experiment and project information dynamically
    navBarContent.add(createAppNavigationBar(), createExperimentNavigationBar());
    addToNavbar(navBarContent);
  }

  private static Span createNavBarTitle(String title) {
    Span navBarTitle = new Span(title);
    navBarTitle.setClassName("project-navbar-title");
    return navBarTitle;
  }

  private void initializeAppDrawer() {
    Span drawerTitle = new Span("Data Manager");
    drawerTitle.addClassName("project-navigation-drawer-title");
    addToDrawer(drawerTitle, projectSideNavigationComponent);
    setPrimarySection(Section.DRAWER);
  }

  private static Span createButtonBar() {
    Span buttonBar = new Span();
    buttonBar.addClassName("project-navbar-buttonbar");
    Button homeButton = new Button("Home");
    Button logout = new Button("Log out");
    buttonBar.add(homeButton, logout);
    homeButton.addClickListener(event -> routeToProjectOverview());
    //ToDo how to route in static context?
    //logout.addClickListener(event -> fireEvent(new LogoutTriggeredEvent(logout, event.isFromClient())));
    return buttonBar;
  }

  private static Span createDrawerToggleAndTitleBar() {
    Span drawerToggleAndTitleBar = new Span();
    drawerToggleAndTitleBar.addClassName("project-navbar-drawer-bar");
    DrawerToggle drawerToggle = new DrawerToggle();
    Span navBarTitle = createNavBarTitle("Test");
    drawerToggleAndTitleBar.add(drawerToggle, navBarTitle);
    return drawerToggleAndTitleBar;
  }

  private static Span createAppNavigationBar() {
    navBarContent.addClassName("project-app-navbar");
    Span appNavigationBar = new Span();
    appNavigationBar.add(createDrawerToggleAndTitleBar(), createButtonBar());
    return appNavigationBar;
  }

  private Div createExperimentNavigationBar() {
    ExperimentNavigationComponent experimentNavigationComponent = new ExperimentNavigationComponent();
    experimentNavigationComponent.addListener(
        experimentNavigationTriggeredEvent -> routeToProjectPage(
            experimentNavigationTriggeredEvent.getSource().navigationTarget()));
    return experimentNavigationComponent;
  }

  //ToDo how to deal with missing ExperimentId Parameters within experiment(e.g. sample page)
  private void routeToProjectPage(Class<Component> navigationTarget) {
    RouteParam projectRouteParam = new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
        context.projectId().orElseThrow().value());
    getUI().ifPresent(ui -> ui.navigate(navigationTarget, projectRouteParam));
  }

  private void resetContent() {
    navBarContent.removeAll();
  }

  private void logoutTriggeredListener(LogoutTriggeredEvent logoutTriggeredEvent) {
    logoutService.logout();
  }

  private static void routeToProjectOverview() {
    //getUI is not possible on the ProjectSideNavigationComponent directly in a static context
    navBarContent.getUI().ifPresent(ui -> ui.navigate(ProjectOverviewPage.class));
    log.debug("Routing to ProjectOverview page");
  }

  private static class LogoutTriggeredEvent extends ComponentEvent<Component> {

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
