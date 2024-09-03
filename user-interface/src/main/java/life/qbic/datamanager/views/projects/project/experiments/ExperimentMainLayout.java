package life.qbic.datamanager.views.projects.project.experiments;


import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteParam;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.general.DataManagerMenu;
import life.qbic.datamanager.views.general.footer.FooterComponentFactory;
import life.qbic.datamanager.views.navigation.ProjectSideNavigationComponent;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentNavigationComponent.RoutingTab;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
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
public class ExperimentMainLayout extends DataManagerLayout implements BeforeEnterObserver {

  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final ProjectSideNavigationComponent projectSideNavigationComponent;
  private final ExperimentNavigationComponent experimentNavigationComponent = new ExperimentNavigationComponent();
  private final DataManagerMenu dataManagerMenu;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient ProjectInformationService projectInformationService;
  private final Span navBarTitle = new Span();
  private Context context = new Context();

  public ExperimentMainLayout(@Autowired LogoutService logoutService,
      @Autowired UserInformationService userInformationService,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired UserPermissions userPermissions,
      @Autowired SpeciesLookupService ontologyTermInformationService,
      @Autowired FooterComponentFactory footerComponentFactory,
      @Autowired  TerminologyService terminologyService,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    super(requireNonNull(footerComponentFactory));
    requireNonNull(logoutService);
    requireNonNull(userInformationService);
    requireNonNull(projectInformationService);
    requireNonNull(experimentInformationService);
    requireNonNull(userPermissions);
    requireNonNull(addExperimentToProjectService);
    requireNonNull(ontologyTermInformationService);
    requireNonNull(messageSourceNotificationFactory,
        "messageSourceNotificationFactory must not be null");

    this.dataManagerMenu = new DataManagerMenu(logoutService);
    this.experimentInformationService = experimentInformationService;
    this.projectInformationService = projectInformationService;
    this.projectSideNavigationComponent = new ProjectSideNavigationComponent(
        projectInformationService, experimentInformationService, addExperimentToProjectService,
        userPermissions, ontologyTermInformationService, terminologyService,
        cancelConfirmationDialogFactory, messageSourceNotificationFactory);
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
    setProjectAndExperimentAsTitle(projectId, context.experimentId().orElseThrow());
    setSelectedExperimentTab(beforeEnterEvent.getNavigationTarget());
  }

  private void setProjectAndExperimentAsTitle(String projectId, ExperimentId experimentId) {
    Optional<Project> project = projectInformationService.find(projectId);
    experimentInformationService.find(projectId, experimentId)
        .ifPresent(
            experiment -> {
              navBarTitle.removeAll();
              Text projectCode = new Text(project.orElseThrow().getProjectCode().value() + "  /");
              Text expName = new Text(experiment.getName());
              Icon book = styleIcon(VaadinIcon.NOTEBOOK);
              Icon beaker = styleIcon(VaadinIcon.FLASK);
              navBarTitle.add(book, projectCode, beaker, expName);
            });
  }

  private Icon styleIcon(VaadinIcon vaadinIcon) {
    Icon icon = vaadinIcon.create();
    icon.addClassName("primary");
    icon.addClassName("smallest");
    return icon;
  }

  private void initializeNavbar() {
    Div experimentNavbar = new Div();
    experimentNavbar.addClassName("experiment-main-layout-navbar-container");
    experimentNavbar.add(createAppNavigationBar(), createExperimentNavigationBar());
    addToNavbar(experimentNavbar);
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
    navBarTitle.setClassName("navbar-title");
    drawerToggleAndTitleBar.add(drawerToggle, navBarTitle);
    drawerToggleAndTitleBar.addClassName("drawer-title-bar");
    return drawerToggleAndTitleBar;
  }

  private Span createAppNavigationBar() {
    Span appNavigationBar = new Span();
    appNavigationBar.addClassNames("experiment-main-layout-navbar");
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
   * Sets the experiment tab within the {@link com.vaadin.flow.component.tabs.TabSheet}to the
   * navigation target provided by the {@link BeforeEnterEvent}
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
