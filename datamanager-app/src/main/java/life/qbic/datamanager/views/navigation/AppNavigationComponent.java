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
import life.qbic.datamanager.views.account.UserProfileMain;
import life.qbic.datamanager.views.groups.MyGroupsMain;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.datamanager.views.projects.project.access.ProjectAccessMain;
import life.qbic.datamanager.views.projects.project.datasets.ProjectDatasetsMain;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.datamanager.views.projects.project.ontology.OntologyLookupMainComponent;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.datamanager.views.projects.project.settings.ProjectSettingsMain;

/**
 * The application's single navigation row (chrome row 2), in the prototype's style.
 *
 * <p>The row always shows the views of the entity named at the end of the breadcrumb — the shell's
 * destinations ({@link #forHome()}), a project's views ({@link #forProject}) or an experiment's
 * views ({@link #forExperiment}). Two scopes are never navigable at once, so the row never shows two
 * meanings of the same word.</p>
 *
 * <p>Each item is a real {@link RouterLink}. The current one is marked by {@code aria-current="page"}
 * (weight plus the accent rule). Project-scoped tools (ontology search) sit in a right-aligned tools
 * group, so navigation starts at a fixed origin independent of entity metadata.</p>
 *
 * @since 1.19.0
 */
public class AppNavigationComponent extends Div {

  private final Div items = new Div();
  private final Div tools = new Div();
  private final Map<Class<?>, RouterLink> links = new LinkedHashMap<>();

  public AppNavigationComponent() {
    addClassName("app-subnav");
    Nav nav = new Nav();
    nav.getElement().setAttribute("aria-label", "Primary");
    nav.addClassName("app-subnav-strip");
    items.addClassName("app-subnav-items");
    tools.addClassName("app-subnav-tools");
    nav.add(items, tools);
    add(nav);
  }

  /** The shell's own destinations, shown at Home scope. */
  public static AppNavigationComponent forHome() {
    return new AppNavigationComponent()
        .addItem("Projects", ProjectOverviewMain.class, null)
        .addItem("Groups", MyGroupsMain.class, null)
        .addItem("Settings", UserProfileMain.class, null);
  }

  /**
   * Navigation row for project scope.
   *
   * @param projectId     the project identifier
   * @param canViewAccess whether the caller may see the project Access view
   */
  public static AppNavigationComponent forProject(String projectId, boolean canViewAccess) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    var parameters = projectParameters(projectId);
    var component = new AppNavigationComponent()
        .addItem("Summary", ProjectInformationMain.class, parameters)
        // The umbrella. The page shows the project's own primary datasets and the datasets
        // connected to it, and qualifies both inside; the bare word is the only label that
        // covers what the page actually is.
        .addItem("Datasets", ProjectDatasetsMain.class, parameters);
    if (canViewAccess) {
      component.addItem("Access", ProjectAccessMain.class, parameters);
    }
    component.addTool("Ontology search", OntologyLookupMainComponent.class, parameters);
    // "Project settings", never bare "Settings": the shell carries a shell-level "Settings", and
    // two destinations differing by one word give no hint which scope applies.
    component.addTool("Project settings", ProjectSettingsMain.class, parameters);
    return component;
  }

  /**
   * Navigation row for experiment scope.
   *
   * @param projectId    the project identifier
   * @param experimentId the experiment identifier
   */
  public static AppNavigationComponent forExperiment(String projectId, String experimentId) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(experimentId, "experimentId must not be null");
    var parameters = experimentParameters(projectId, experimentId);
    return new AppNavigationComponent()
        .addItem("Overview", ExperimentInformationMain.class, parameters)
        .addItem("Samples", SampleInformationMain.class, parameters)
        // One entry for the two ordered process steps that used to be "Measurements" and "Raw
        // data". The metadata is registered here and mints the measurement code; the data is
        // produced elsewhere and appears once it has been scanned. Two pages asked the reader to
        // know which step their data was in before choosing a link.
        .addItem("Datasets", MeasurementDatasetMain.class, parameters);
  }

  /**
   * Adds a navigation item.
   *
   * @param label      the item label
   * @param target     the route target
   * @param parameters the route parameters, or {@code null} for a parameterless route
   */
  public AppNavigationComponent addItem(String label, Class<? extends Component> target,
      RouteParameters parameters) {
    links.put(target, add(label, target, parameters, items));
    return this;
  }

  /**
   * Adds a right-aligned tool.
   *
   * @param label      the tool label
   * @param target     the route target
   * @param parameters the route parameters, or {@code null} for a parameterless route
   */
  public AppNavigationComponent addTool(String label, Class<? extends Component> target,
      RouteParameters parameters) {
    links.put(target, add(label, target, parameters, tools));
    return this;
  }

  private static RouterLink add(String label, Class<? extends Component> target,
      RouteParameters parameters, Div container) {
    var link = new RouterLink();
    link.add(new Span(label));
    if (parameters == null) {
      link.setRoute(target);
    } else {
      link.setRoute(target, parameters);
    }
    link.addClassName("app-subnav-item");
    container.add(link);
    return link;
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
