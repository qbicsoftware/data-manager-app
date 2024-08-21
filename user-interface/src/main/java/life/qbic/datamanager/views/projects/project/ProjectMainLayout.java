package life.qbic.datamanager.views.projects.project;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.general.DataManagerMenu;
import life.qbic.datamanager.views.general.footer.FooterComponentFactory;
import life.qbic.datamanager.views.navigation.ProjectSideNavigationComponent;
import life.qbic.datamanager.views.notifications.toasts.MessageSourceToastFactory;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
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
public class ProjectMainLayout extends DataManagerLayout implements BeforeEnterObserver {

  private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private final ProjectSideNavigationComponent projectSideNavigationComponent;
  private final DataManagerMenu dataManagerMenu;
  private final transient ProjectInformationService projectInformationService;

  private Context context = new Context();
  private final Span projectTitle = new Span();

  public ProjectMainLayout(@Autowired LogoutService logoutService,
      @Autowired UserInformationService userInformationService,
      ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired UserPermissions userPermissions,
      @Autowired OntologyLookupService ontologyLookupService,
      @Autowired FooterComponentFactory footerComponentFactory,
      MessageSourceToastFactory messageSourceToastFactory) {

    super(requireNonNull(footerComponentFactory));
    requireNonNull(logoutService);
    requireNonNull(userInformationService);
    requireNonNull(projectInformationService);
    requireNonNull(experimentInformationService);
    requireNonNull(addExperimentToProjectService);
    requireNonNull(ontologyLookupService);
    requireNonNull(messageSourceToastFactory, "messageSourceToastFactory must not be null");
    this.projectInformationService = projectInformationService;
    this.projectSideNavigationComponent = new ProjectSideNavigationComponent(
        projectInformationService,
        experimentInformationService, addExperimentToProjectService,
        userPermissions, ontologyLookupService, messageSourceToastFactory);
    dataManagerMenu = new DataManagerMenu(logoutService);
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
              Icon book = styleIcon(VaadinIcon.NOTEBOOK);

              projectTitle.add(book, projectCode, projectName);
            });
  }

  private Icon styleIcon(VaadinIcon vaadinIcon) {
    Icon icon = vaadinIcon.create();
    icon.addClassName("primary");
    icon.addClassName("smallest");
    return icon;
  }

  private Span createDrawerToggleAndTitleBar() {
    Span drawerToggleAndTitleBar = new Span();
    drawerToggleAndTitleBar.addClassName("drawer-title-bar");
    DrawerToggle drawerToggle = new DrawerToggle();
    projectTitle.setClassName("navbar-title");
    drawerToggleAndTitleBar.add(drawerToggle, projectTitle);

    initializeDrawer();
    return drawerToggleAndTitleBar;
  }

  private void initializeDrawer() {
    Span drawerTitle = new Span("Data Manager");
    drawerTitle.addClassName("project-navigation-drawer-title");
    addToDrawer(drawerTitle, projectSideNavigationComponent);
    setPrimarySection(Section.DRAWER);
  }
}
