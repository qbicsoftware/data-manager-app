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
 * Scope-contextual navigation row of the project shell — the prototype's second chrome row.
 *
 * <p>Its contents are the views of the entity named at the end of the breadcrumb: the project's own
 * views for project scope, the experiment's views for experiment scope. Two scopes are never
 * navigable at once, so the row never shows two meanings of the same word.</p>
 *
 * <p>Styling follows the prototype: each item is a plain link with a transparent bottom rule; the
 * current one is marked by {@code aria-current="page"} (weight plus the accent rule), never a filled
 * pill. Project-scoped tools (ontology search) sit in a right-aligned tools group, so navigation
 * starts at a fixed origin and does not depend on project metadata.</p>
 *
 * @since 1.19.0
 */
public class ProjectScopeNavigationComponent extends Div {

  private final Div items = new Div();
  private final Div tools = new Div();
  private final Map<Class<?>, RouterLink> links = new LinkedHashMap<>();

  private ProjectScopeNavigationComponent() {
    addClassName("project-scope-navigation");
    Nav nav = new Nav();
    nav.getElement().setAttribute("aria-label", "Project navigation");
    nav.addClassName("project-scope-navigation-strip");
    items.addClassName("project-scope-navigation-items");
    tools.addClassName("project-scope-navigation-tools");
    nav.add(items, tools);
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
    component.addItem("Summary", ProjectInformationMain.class, parameters, false);
    component.addItem("Datasets", ConnectedDatasetsMain.class, parameters, false);
    if (canViewAccess) {
      component.addItem("Access", ProjectAccessMain.class, parameters, false);
    }
    component.addItem("Ontology search", OntologyLookupMainComponent.class, parameters, true);
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
    component.addItem("Overview", ExperimentInformationMain.class, parameters, false);
    component.addItem("Samples", SampleInformationMain.class, parameters, false);
    component.addItem("Measurements", MeasurementMain.class, parameters, false);
    component.addItem("Raw data", RawDataMain.class, parameters, false);
    return component;
  }

  private void addItem(String label, Class<? extends Component> target, RouteParameters parameters,
      boolean tool) {
    var link = new RouterLink();
    link.add(new Span(label));
    link.setRoute(target, parameters);
    link.addClassName("project-scope-navigation-item");
    links.put(target, link);
    (tool ? tools : items).add(link);
  }

  /**
   * Marks the item whose view is the current routing target as the current page.
   *
   * @param navigationTarget the target class of the route being entered
   */
  public void setActive(Class<?> navigationTarget) {
    links.forEach((target, link) -> {
      if (target.equals(navigationTarget)) {
        link.getElement().setAttribute("aria-current", "page");
      } else {
        link.getElement().removeAttribute("aria-current");
      }
    });
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
