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
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The projectMainLayout functions as a view which contains all views related to managing a
 * project. </b>
 *
 * @since 1.0.0
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
  private final transient ExperimentInformationService experimentInformationService;
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
    this.experimentInformationService = experimentInformationService;
    this.projectSideNavigationComponent = new ProjectSideNavigationComponent(
        projectInformationService,
        experimentInformationService);
    initializeHeaderLayout();
    addToNavbar(navBarContent);
    addClickListeners();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectId = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    this.context = new Context().with(parsedProjectId);
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresent(experimentId -> this.context = context.with(ExperimentId.parse(experimentId)));
    setContext();
  }

  private void setContext() {
    context.experimentId().ifPresentOrElse(this::setExperimentNameAsTitle,
        () -> setProjectNameAsTitle(context.projectId().orElseThrow()));
  }

  private void setProjectNameAsTitle(ProjectId projectId) {
    projectInformationService.find(projectId)
        .ifPresent(
            project -> projectTitle.setText(project.getProjectIntent().projectTitle().title()));
  }

  private void setExperimentNameAsTitle(ExperimentId experimentId) {
    experimentInformationService.find(experimentId)
        .ifPresent(
            experiment -> projectTitle.setText(experiment.getName()));
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
