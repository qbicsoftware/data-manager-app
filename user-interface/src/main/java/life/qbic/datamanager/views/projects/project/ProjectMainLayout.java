package life.qbic.datamanager.views.projects.project;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import java.util.Objects;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.navigation.ProjectSideNavigationComponent;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewPage;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The ProjectMainLayout functions as a layout which contains all views related to managing a
 * project. It provides an app drawer within which the {@link ProjectSideNavigationComponent} allows
 * the user to navigate within the selected project. </b> Additionally it provides a navbar which
 * provides buttons to toggle the app drawer, for logout purposes and for routing back to the home
 * {@link ProjectOverviewPage} view
 *
 */
@PageTitle("Data Manager")
public class ProjectMainLayout extends AppLayout implements BeforeEnterObserver {

  private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private Button homeButton;
  private Button logout;
  private final ProjectSideNavigationComponent projectSideNavigationComponent;
  private final Span navBarContent = new Span();
  private final LogoutService logoutService;
  private final transient ProjectInformationService projectInformationService;
  private Context context = new Context();
  private final Span projectTitle = new Span();

  public ProjectMainLayout(@Autowired LogoutService logoutService,
      ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(logoutService);
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.logoutService = logoutService;
    this.projectInformationService = projectInformationService;
    this.projectSideNavigationComponent = new ProjectSideNavigationComponent(
        projectInformationService,
        experimentInformationService);
    initializeHeaderLayout();
    addToNavbar(navBarContent);
    addClickListeners();
    addClassName("project-main-layout");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectId = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    this.context = new Context().with(parsedProjectId);
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresent(experimentId -> this.context = context.with(ExperimentId.parse(experimentId)));
    setProjectNameAsTitle(context.projectId().orElseThrow());
  }

  private void setProjectNameAsTitle(ProjectId projectId) {
    projectInformationService.find(projectId)
        .ifPresent(
            project -> projectTitle.setText(project.getProjectIntent().projectTitle().title()));
  }

  private void initializeHeaderLayout() {
    navBarContent.addClassName("project-navbar");
    navBarContent.add(createDrawerToggleAndTitleBar(), createButtonBar());
  }

  private Span createButtonBar() {
    Span buttonBar = new Span();
    buttonBar.addClassName("project-navbar-buttonbar");
    homeButton = new Button("Home");
    logout = new Button("Log out");
    buttonBar.add(homeButton, logout);
    return buttonBar;
  }

  private Span createDrawerToggleAndTitleBar() {
    Span drawerToggleAndTitleBar = new Span();
    drawerToggleAndTitleBar.addClassName("project-navbar-drawer-bar");
    DrawerToggle drawerToggle = new DrawerToggle();
    drawerToggleAndTitleBar.add(drawerToggle, projectTitle);
    projectTitle.setClassName("project-navbar-title");
    initializeDrawer();
    return drawerToggleAndTitleBar;
  }

  private void initializeDrawer() {
    Span drawerTitle = new Span("Data Manager");
    drawerTitle.addClassName("project-navigation-drawer-title");
    addToDrawer(drawerTitle, projectSideNavigationComponent);
    setPrimarySection(Section.DRAWER);
  }

  private void addClickListeners() {
    homeButton.addClickListener(event -> UI.getCurrent().getPage().setLocation(
        Projects.PROJECTS));
    logout.addClickListener(event -> logoutService.logout());
  }
}
