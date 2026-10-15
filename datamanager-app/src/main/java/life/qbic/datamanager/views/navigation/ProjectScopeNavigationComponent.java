package life.qbic.datamanager.views.navigation;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import life.qbic.datamanager.views.projects.project.access.ProjectAccessMain;
import life.qbic.datamanager.views.projects.project.datasets.ConnectedDatasetsMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMain;
import life.qbic.datamanager.views.projects.project.ontology.OntologyLookupMainComponent;
import life.qbic.datamanager.views.projects.project.rawdata.RawDataMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;

/**
 * Scope-contextual navigation row of the project shell.
 *
 * <p>The shell shows exactly one navigation row, and its contents are the views of the entity named
 * at the end of the identity bar's breadcrumb:</p>
 *
 * <ul>
 *   <li>{@link #forProject(String, boolean)} — the project's own views
 *       (<em>Summary · Datasets · Access · Ontology search</em>).</li>
 *   <li>{@link #forExperiment(String, String)} — the experiment's views
 *       (<em>Overview · Samples · Measurements · Raw data</em>).</li>
 * </ul>
 *
 * <p>Two scopes are never navigable at once, and the row contains no entity-specific content, so it
 * renders identical geometry in every project. Each item is a real {@link RouterLink}; the active
 * item is set explicitly by the layout from the routing target, because several routes share a
 * location prefix (e.g. an experiment overview prefixes its sub-views).</p>
 *
 * @since 1.19.0
 */
public class ProjectScopeNavigationComponent extends Div {

  private final Nav nav = new Nav();
  private final Map<Class<?>, RouterLink> links = new LinkedHashMap<>();

  private ProjectScopeNavigationComponent() {
    addClassName("project-scope-navigation");
    nav.getElement().setAttribute("aria-label", "Project navigation");
    nav.addClassName("project-scope-navigation-strip");
    add(nav);
  }

  /**
   * Navigation row for project scope.
   *
   * @param projectId     the project identifier
   * @param canViewAccess whether the caller may see the project Access view
   */
  public static ProjectScopeNavigationComponent forProject(String projectId, boolean canViewAccess) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    var component = new ProjectScopeNavigationComponent();
    var parameters = projectParameters(projectId);
    component.addLink("Summary", ProjectInformationMain.class, parameters);
    component.addLink("Datasets", ConnectedDatasetsMain.class, parameters);
    if (canViewAccess) {
      component.addLink("Access", ProjectAccessMain.class, parameters);
    }
    component.addLink("Ontology search", OntologyLookupMainComponent.class, parameters);
    return component;
  }

  /**
   * Navigation row for experiment scope.
   *
   * @param projectId    the project identifier
   * @param experimentId the experiment identifier
   */
  public static ProjectScopeNavigationComponent forExperiment(String projectId,
      String experimentId) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(experimentId, "experimentId must not be null");
    var component = new ProjectScopeNavigationComponent();
    var parameters = experimentParameters(projectId, experimentId);
    component.addLink("Overview", ExperimentInformationMain.class, parameters);
    component.addLink("Samples", SampleInformationMain.class, parameters);
    component.addLink("Measurements", MeasurementMain.class, parameters);
    component.addLink("Raw data", RawDataMain.class, parameters);
    return component;
  }

  private void addLink(String label, Class<? extends Component> target, RouteParameters parameters) {
    var link = new RouterLink();
    link.add(new Span(label));
    link.setRoute(target, parameters);
    link.addClassName("project-scope-navigation-item");
    links.put(target, link);
    nav.add(link);
  }

  /**
   * Marks the navigation item whose view is the current routing target as active.
   *
   * @param navigationTarget the target class of the route being entered
   */
  public void setActive(Class<?> navigationTarget) {
    links.forEach((target, link) ->
        link.getElement().getClassList().set("is-active", target.equals(navigationTarget)));
  }

  private static RouteParameters projectParameters(String projectId) {
    return new RouteParameters(new RouteParam("projectId", projectId));
  }

  private static RouteParameters experimentParameters(String projectId, String experimentId) {
    return new RouteParameters(
        new RouteParam("projectId", projectId),
        new RouteParam("experimentId", experimentId));
  }
}
