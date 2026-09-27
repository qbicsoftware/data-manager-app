package life.qbic.datamanager.views.projects.project;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.announcements.AnnouncementService;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.general.DataManagerMenu;
import life.qbic.datamanager.views.general.HomeLink;
import life.qbic.datamanager.views.general.footer.FooterComponent;
import life.qbic.datamanager.views.navigation.ProjectSideNavigationComponent;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The ProjectMainLayout functions as a layout which contains all views related to managing a
 * project. It provides an app drawer within which the {@link ProjectSideNavigationComponent} allows
 * the user to navigate within the selected project. </b> Additionally it provides a navbar which
 * provides buttons to toggle the app drawer, for logout purposes and for routing back to the home
 * {@link ProjectOverviewMain} view
 *
 */
@PageTitle("Data Manager")
@PermitAll
public class ProjectMainLayout extends DataManagerLayout implements BeforeEnterObserver {

  private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private final ProjectSideNavigationComponent projectSideNavigationComponent;
  private final DataManagerMenu dataManagerMenu;
  private final transient ProjectInformationService projectInformationService;

  private Context context = new Context();
  private final RouterLink projectTitle = new RouterLink("", ProjectInformationMain.class);

  public ProjectMainLayout(@Autowired AuthenticationContext authenticationContext,
      @Autowired UserInformationService userInformationService,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired UserPermissions userPermissions,
      @Autowired SpeciesLookupService speciesLookupService,
      @Autowired FooterComponent footerComponent,
      @Autowired TerminologyService terminologyService,
      @Autowired MessageSourceNotificationFactory messageSourceNotificationFactory,
      @Autowired AnnouncementService announcementService) {
    super(requireNonNull(footerComponent), announcementService);
    requireNonNull(authenticationContext);
    requireNonNull(userInformationService);
    requireNonNull(projectInformationService);
    requireNonNull(experimentInformationService);
    requireNonNull(addExperimentToProjectService);
    requireNonNull(speciesLookupService);
    requireNonNull(messageSourceNotificationFactory,
        "messageSourceNotificationFactory must not be null");
    this.projectInformationService = projectInformationService;
    this.projectSideNavigationComponent = new ProjectSideNavigationComponent(
        projectInformationService,
        experimentInformationService, addExperimentToProjectService,
        userPermissions, speciesLookupService, terminologyService,
        messageSourceNotificationFactory);
    dataManagerMenu = new DataManagerMenu(authenticationContext);
    Span projectMainNavbar = new Span(createDrawerToggleAndTitleBar(), dataManagerMenu);
    projectMainNavbar.addClassName("project-main-layout-navbar");
    addToNavbar(projectMainNavbar);
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
            project -> {
              projectTitle.removeAll();

              Text projectCode = new Text(project.getProjectCode().value() + " - ");
              Text projectName = new Text(project.getProjectIntent().projectTitle().title());

              // The project title in the navbar links to the project summary
              // (projects/:projectId/info) so users can navigate back to the
              // project's home without opening the drawer.
              projectTitle.setRoute(ProjectInformationMain.class,
                  new RouteParameters(
                      new RouteParam(ProjectInformationMain.PROJECT_ID_ROUTE_PARAMETER,
                          projectId.value())));
              projectTitle.add(projectCode, projectName);
            });
  }

  private Span createDrawerToggleAndTitleBar() {
    Span drawerToggleAndTitleBar = new Span();
    drawerToggleAndTitleBar.addClassName("drawer-title-bar");
    DrawerToggle drawerToggle = new DrawerToggle();
    projectTitle.setClassName("navbar-title");
    drawerToggleAndTitleBar.add(drawerToggle, new HomeLink(), projectTitle);

    initializeDrawer();
    return drawerToggleAndTitleBar;
  }

  private void initializeDrawer() {
    Span drawerTitle = new Span("Data Manager");
    drawerTitle.addClassName("project-navigation-drawer-title");
    addToDrawer(drawerTitle, projectSideNavigationComponent);
    setPrimarySection(Section.DRAWER);
    // Vaadin 25: Open drawer by default when navigating to a project
    setDrawerOpened(true);
  }
}
