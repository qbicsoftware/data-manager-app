package life.qbic.datamanager.views.projects.project;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import java.util.Objects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.navigation.ProjectNavigationDrawer;
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
  private final ProjectNavigationDrawer projectNavigationDrawer;
  private final Span navBarContent = new Span();
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private Context context = new Context();
  private final Span projectTitle = new Span();

  public ProjectMainLayout(@Autowired ProjectMainHandlerInterface projectMainHandlerInterface,
      ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.projectNavigationDrawer = new ProjectNavigationDrawer(
        projectInformationService,
        experimentInformationService);
    initializeHeaderLayout();
    addToNavbar(navBarContent);
    registerToHandler(projectMainHandlerInterface);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectId = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    ProjectId parsedProjectId = ProjectId.parse(projectId);
    this.context = new Context().with(parsedProjectId);
    if (beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER).isEmpty()) {
      setContext(this.context);
      return; // abort the before-enter event
    }
    String experimentId = beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .get();
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
    this.context = context.with(parsedExperimentId);
    setContext(this.context);
  }

  private void setContext(Context context) {
    if (context.experimentId().isEmpty()) {
      setProjectNameAsTitle(context.projectId().orElseThrow());
    } else {
      setExperimentNameAsTitle(context.experimentId().get());
    }
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
    navBarContent.add(createDrawerAndTitleBar(), createButtonBar());
  }

  private Span createButtonBar() {
    Span buttonBar = new Span();
    buttonBar.addClassName("project-navbar-buttonbar");
    homeButton = new Button("Home");
    logout = new Button("Log out");
    buttonBar.add(homeButton, logout);
    return buttonBar;
  }

  private Span createDrawerAndTitleBar() {
    Span toggleAndTitleBar = new Span();
    toggleAndTitleBar.addClassName("project-navbar-drawer-bar");
    DrawerToggle drawerToggle = new DrawerToggle();
    Span drawerTitle = new Span("Data Manager");
    drawerTitle.addClassName("project-navigation-drawer-title");
    addToDrawer(drawerTitle, projectNavigationDrawer);
    setPrimarySection(Section.DRAWER);
    toggleAndTitleBar.add(drawerToggle, projectTitle);
    projectTitle.setClassName("project-navbar-title");
    return toggleAndTitleBar;
  }

  private void registerToHandler(ProjectMainHandlerInterface startHandler) {
    startHandler.handle(this);
  }

  public Button logout() {
    return logout;
  }

  public Button homeButton() {
    return homeButton;
  }
}
