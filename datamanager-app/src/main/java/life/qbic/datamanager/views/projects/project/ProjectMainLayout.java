package life.qbic.datamanager.views.projects.project;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import life.qbic.datamanager.views.general.HomeLink;
import life.qbic.datamanager.views.general.footer.FooterComponent;
import life.qbic.datamanager.views.navigation.ProjectScopeNavigationComponent;
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
 * <p>The single layout for everything inside a project — project-scoped views (summary, datasets,
 * access, ontology search) and experiment-scoped views (overview, samples, measurements, raw data)
 * alike. It replaces the former separate {@code ProjectMainLayout} (drawer-based) and
 * {@code ExperimentMainLayout} (drawer + tab bar) so the chrome no longer shifts when navigating
 * into an experiment.</p>
 *
 * <p>Two chrome rows, never more (implementation strategy, Phase 1):</p>
 * <ol>
 *   <li>an identity bar — home link, project/experiment identity, account menu;</li>
 *   <li>one scope-contextual navigation row ({@link ProjectScopeNavigationComponent}) whose contents
 *       are the views of the entity named in the identity bar.</li>
 * </ol>
 *
 * <p>The left navigation drawer is retired. There is no experiment-level tab bar: the experiment's
 * views live in the same single navigation row.</p>
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

  private final RouterLink identity = new RouterLink();
  private final Span scopeNavigationContainer = new Span();

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

    Span identityBar = new Span(createIdentityBar(), dataManagerMenu);
    identityBar.addClassName("project-shell-identity-bar");
    scopeNavigationContainer.addClassName("project-shell-navigation");
    addToNavbar(identityBar, scopeNavigationContainer);
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

    scopeNavigationContainer.removeAll();
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresentOrElse(
            experimentIdValue -> showExperimentScope(project, projectId, experimentIdValue,
                beforeEnterEvent.getNavigationTarget()),
            () -> showProjectScope(project, projectId, beforeEnterEvent.getNavigationTarget()));
  }

  private void showProjectScope(Project project, ProjectId projectId,
      Class<?> navigationTarget) {
    setProjectIdentity(project, projectId);
    boolean canViewAccess = userPermissions.readProject(projectId);
    var navigation = ProjectScopeNavigationComponent.forProject(projectId.value(), canViewAccess);
    navigation.setActive(navigationTarget);
    scopeNavigationContainer.add(navigation);
  }

  private void showExperimentScope(Project project, ProjectId projectId, String experimentId,
      Class<?> navigationTarget) {
    setExperimentIdentity(project, projectId, ExperimentId.parse(experimentId));
    var navigation = ProjectScopeNavigationComponent.forExperiment(projectId.value(),
        experimentId);
    navigation.setActive(navigationTarget);
    scopeNavigationContainer.add(navigation);
  }

  private Span createIdentityBar() {
    Span identityBar = new Span();
    identityBar.addClassName("project-shell-identity");
    identity.setClassName("project-shell-title");
    identityBar.add(new HomeLink(), identity);
    return identityBar;
  }

  private void setProjectIdentity(Project project, ProjectId projectId) {
    identity.setRoute(ProjectInformationMain.class, projectRouteParameters(projectId));
    identity.removeAll();
    identity.add(new Text(project.getProjectCode().value() + " — "
        + project.getProjectIntent().projectTitle().title()));
  }

  private void setExperimentIdentity(Project project, ProjectId projectId,
      ExperimentId experimentId) {
    identity.setRoute(ProjectInformationMain.class, projectRouteParameters(projectId));
    String experimentName = experimentInformationService.find(projectId.value(), experimentId)
        .map(Experiment::getName)
        .orElse("");
    Icon beaker = VaadinIcon.FLASK.create();
    beaker.addClassNames("primary", "smallest");
    identity.removeAll();
    identity.add(new Text(project.getProjectCode().value() + " / "), beaker,
        new Text(experimentName));
  }

  private static RouteParameters projectRouteParameters(ProjectId projectId) {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectId.value()));
  }
}
