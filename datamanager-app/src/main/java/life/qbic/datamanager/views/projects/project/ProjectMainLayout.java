package life.qbic.datamanager.views.projects.project;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.announcements.AnnouncementService;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.general.DataManagerMenu;
import life.qbic.datamanager.views.general.footer.FooterComponent;
import life.qbic.datamanager.views.navigation.AppNavigationComponent;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Project shell layout</b>
 *
 * <p>The single layout for everything inside a project — project-scoped views and experiment-scoped
 * views alike. It replaces the former separate {@code ProjectMainLayout} (drawer-based) and
 * {@code ExperimentMainLayout} (drawer + tab bar).</p>
 *
 * <p>Two chrome rows, in the prototype's style:</p>
 * <ol>
 *   <li>an identity bar — the {@code Data Manager} wordmark, the breadcrumb
 *       ({@code Home / CODE / Title}, extended with the experiment crumb when inside one) and the
 *       account menu;</li>
 *   <li>one scope-contextual navigation row ({@link AppNavigationComponent}).</li>
 * </ol>
 *
 * <p>Breadcrumb and identity band both name the project on purpose: the breadcrumb is the way up,
 * the band is the page's title. Only the entity being viewed is not a link.</p>
 *
 * @since 1.19.0
 */
@PageTitle("Data Manager")
@PermitAll
public class ProjectMainLayout extends DataManagerLayout implements BeforeEnterObserver {

  private static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  private final DataManagerMenu dataManagerMenu;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient UserPermissions userPermissions;

  private final Span breadcrumbs = new Span();
  private final Span navigationContainer = new Span();

  public ProjectMainLayout(@Autowired AuthenticationContext authenticationContext,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired UserPermissions userPermissions,
      @Autowired FooterComponent footerComponent,
      @Autowired AnnouncementService announcementService) {
    super(requireNonNull(footerComponent), announcementService);
    this.projectInformationService = requireNonNull(projectInformationService);
    this.experimentInformationService = requireNonNull(experimentInformationService);
    this.userPermissions = requireNonNull(userPermissions);
    this.dataManagerMenu = new DataManagerMenu(requireNonNull(authenticationContext));

    breadcrumbs.addClassName("app-crumbs");
    Span identityBarRow = new Span(brand(), breadcrumbs, dataManagerMenu);
    identityBarRow.addClassName("app-identity-bar-row");
    Div identityBar = new Div(identityBarRow);
    identityBar.addClassName("app-identity-bar");
    navigationContainer.addClassName("app-navigation");
    Div chrome = new Div(identityBar, navigationContainer);
    chrome.addClassName("app-chrome");
    addToNavbar(chrome);
    addClassNames("project-main-layout", "project-shell");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectIdValue = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing project id in route"));
    ProjectId projectId = ProjectId.parse(projectIdValue);
    Project project = projectInformationService.find(projectId).orElseThrow(
        () -> new ApplicationException("Unknown project " + projectIdValue));

    String projectCode = project.getProjectCode().value();
    String projectTitle = project.getProjectIntent().projectTitle().title();
    navigationContainer.removeAll();
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresentOrElse(
            experimentId -> renderExperimentScope(projectId, projectCode, projectTitle, experimentId,
                beforeEnterEvent.getNavigationTarget()),
            () -> renderProjectScope(projectId, projectCode, projectTitle,
                beforeEnterEvent.getNavigationTarget()));
  }

  private void renderProjectScope(ProjectId projectId, String projectCode, String projectTitle,
      Class<?> navigationTarget) {
    breadcrumbs.removeAll();
    breadcrumbs.add(homeCrumb(), separator(), codeSpan(projectCode), separator(),
        currentCrumb(projectTitle));
    boolean canViewAccess = userPermissions.readProject(projectId);
    var navigation = AppNavigationComponent.forProject(projectId.value(), canViewAccess);
    navigation.setActive(navigationTarget);
    navigationContainer.add(navigation);
  }

  private void renderExperimentScope(ProjectId projectId, String projectCode, String projectTitle,
      String experimentId, Class<?> navigationTarget) {
    String experimentName = experimentInformationService
        .find(projectId.value(), ExperimentId.parse(experimentId))
        .map(Experiment::getName)
        .orElse("");
    breadcrumbs.removeAll();
    breadcrumbs.add(homeCrumb(), separator(), codeLink(projectCode, projectId), separator(),
        titleLink(projectTitle, projectId), separator(), currentCrumb(experimentName));
    var navigation = AppNavigationComponent.forExperiment(projectId.value(), experimentId);
    navigation.setActive(navigationTarget);
    navigationContainer.add(navigation);
  }

  private static Span brand() {
    Span brand = new Span();
    brand.addClassName("app-brand");
    RouterLink wordmark = new RouterLink();
    wordmark.setRoute(ProjectOverviewMain.class);
    wordmark.addClassName("app-wordmark");
    Span dot = new Span(".");
    dot.addClassName("app-wordmark-dot");
    wordmark.add(new Span("Data Manager"), dot);
    brand.add(wordmark);
    return brand;
  }

  private static RouterLink homeCrumb() {
    RouterLink home = new RouterLink("Home", ProjectOverviewMain.class);
    home.addClassName("app-crumb");
    return home;
  }

  private static Span codeSpan(String code) {
    Span span = new Span(code);
    span.addClassNames("app-crumb", "app-crumb-code");
    return span;
  }

  private static RouterLink codeLink(String code, ProjectId projectId) {
    RouterLink link = new RouterLink(code, ProjectInformationMain.class,
        projectRouteParameters(projectId));
    link.addClassNames("app-crumb", "app-crumb-code");
    return link;
  }

  private static RouterLink titleLink(String title, ProjectId projectId) {
    RouterLink link = new RouterLink(title, ProjectInformationMain.class,
        projectRouteParameters(projectId));
    link.addClassName("app-crumb");
    return link;
  }

  private static Span currentCrumb(String text) {
    Span span = new Span(text);
    span.addClassNames("app-crumb", "app-crumb-current");
    span.getElement().setAttribute("title", text);
    return span;
  }

  private static Span separator() {
    Span span = new Span("/");
    span.addClassName("app-crumb-separator");
    return span;
  }

  private static RouteParameters projectRouteParameters(ProjectId projectId) {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectId.value()));
  }
}
