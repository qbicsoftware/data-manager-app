package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.AssayType;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The experiment's <b>Measurement Dataset</b>: one surface for the two ordered process steps that
 * were previously two pages.
 *
 * <p>A measurement is the metadata describing how a result was produced; the measurement data is
 * the result itself. They belong together for the reader but are produced by different systems:
 * the metadata is registered here and mints the measurement code, the data is produced by the
 * facility, transferred out of band and synced back keyed by that code. Neither page could state
 * that relationship before: the measurements page had no data field, and the data page could not
 * represent a measurement whose data had not arrived, because its views inner-join the data table.
 * </p>
 *
 * <p>The page therefore leads with the process rather than with a table: a status line stating how
 * many entries exist and how many are still waiting, then the entries themselves with the second
 * step as a column. The stage band appears only while something is outstanding — a permanent band
 * stating "everything is fine" is furniture a returning reader learns to skip.</p>
 *
 * <p>Segmenting by measurement method is the outer structure because a measurement dataset is the
 * experiment's data for <em>one</em> method: an experiment has one per method it measured, not one
 * in total.</p>
 *
 * @since 1.21.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/dataset",
    layout = ProjectMainLayout.class)
@PermitAll
public class MeasurementDatasetMain extends Main
    implements BeforeEnterObserver, BeforeLeaveObserver {

  @Serial
  private static final long serialVersionUID = -1L;

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  private static final Logger log = LoggerFactory.logger(MeasurementDatasetMain.class);

  private final transient ProjectQueryService queryService;

  private final H1 title = new H1("Measurement Dataset");
  private final Span statusLine = new Span();
  private final Div stageBand = new Div();
  private final Div content = new Div();

  private History.HistoryStateChangeHandler routerHistoryStateChangeHandler;
  private final History.HistoryStateChangeHandler listStateHistoryHandler =
      this::onHistoryStateChange;
  private String basePath;
  private boolean suppressUrlWrite;

  private MeasurementDatasetDetailsComponent detailsComponent;
  private Context context;

  public MeasurementDatasetMain(@Autowired ProjectQueryService queryService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
    addClassName("measurement-dataset");
    addClassNames("measurement-dataset-shell");

    title.addClassName("measurement-dataset-title");
    statusLine.addClassName("measurement-dataset-header-status");
    Div titleBlock = new Div(title, statusLine);
    titleBlock.addClassName("measurement-dataset-header-title-block");
    Div header = new Div(titleBlock);
    header.addClassName("measurement-dataset-header");

    stageBand.addClassName("measurement-dataset-stage-band");
    stageBand.setVisible(false);

    content.add(header, stageBand);
    content.addClassName("measurement-dataset-content");
    add(content);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectIdValue = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing project id in route"));
    if (!ProjectId.isValid(projectIdValue)) {
      throw new ApplicationException("invalid project id " + projectIdValue);
    }
    ProjectId projectId = ProjectId.parse(projectIdValue);
    String experimentIdValue = event.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing experiment id in route"));
    if (!ExperimentId.isValid(experimentIdValue)) {
      throw new ApplicationException("invalid experiment id " + experimentIdValue);
    }
    context = new Context().with(projectId).with(ExperimentId.parse(experimentIdValue));

    // URL list-state synchronisation (USER-R-03): capture the router handler, install ours, and
    // seed the list from the URL on direct load / reload / shared links.
    History history = UI.getCurrent().getPage().getHistory();
    History.HistoryStateChangeHandler currentHandler = history.getHistoryStateChangeHandler();
    if (currentHandler != listStateHistoryHandler) {
      routerHistoryStateChangeHandler = currentHandler;
    }
    history.setHistoryStateChangeHandler(listStateHistoryHandler);
    basePath = String.format(ProjectRoutes.MEASUREMENT_DATASET, projectIdValue, experimentIdValue);

    suppressUrlWrite = true;
    try {
      render(projectIdValue, experimentIdValue);
      if (detailsComponent != null) {
        ListState urlState = ListStateCodec.parse(event.getLocation().getQueryParameters(),
            MeasurementDataSort.DEFAULT, MeasurementDataSort.allowedSortOrders());
        detailsComponent.applyExternalState(urlState);
      }
    } finally {
      suppressUrlWrite = false;
    }
  }

  /**
   * Renders the page for the current experiment: the counts, the outstanding-work band and the
   * list. The experiment's totals come from the project read model, so the page states its own
   * facts in one query instead of counting what the grid happens to render.
   */
  private void render(String projectId, String experimentId) {
    ExperimentProgress progress = queryService.getExperimentProgress(projectId, experimentId)
        .blockOptional()
        .orElseThrow(() -> new ApplicationException(
            "Unknown experiment " + experimentId + " in project " + projectId));

    int total = progress.measurementCount();
    int waiting = progress.measurementsWithoutPrimaryData();
    updateHeaderAndStage(total, waiting);

    removeDetailsComponent();
    if (total == 0) {
      // No measurements at all: registration is the only path, so the page states the empty
      // condition and offers nothing it cannot do. The list is withheld rather than rendered
      // empty, and the shared empty state explains what a measurement entry is.
      content.add(emptyState());
      return;
    }

    detailsComponent = new MeasurementDatasetDetailsComponent(queryService, projectId,
        experimentId);
    detailsComponent.applySegments(segmentsWithMeasurements(progress));
    detailsComponent.addPageLoadedListener(event -> writeUrl(detailsComponent.listState()));
    add(detailsComponent);
  }

  /**
   * The methods that actually hold measurements.
   *
   * <p>Derived from measurement counts, never from data presence: a method whose data has not
   * arrived yet still has a dataset to show. Hiding such a segment was the defect class that made
   * the proteomics tab disappear from the raw-data page.</p>
   */
  private static List<MeasurementDatasetSegment> segmentsWithMeasurements(
      ExperimentProgress progress) {
    List<MeasurementDatasetSegment> segments = new ArrayList<>();
    for (AssayType assayType : progress.assayTypes()) {
      segments.add(MeasurementDatasetSegment.from(assayType));
    }
    return segments;
  }

  private void updateHeaderAndStage(int total, int waiting) {
    if (total == 0) {
      statusLine.setText("No measurement metadata registered yet");
    } else {
      String entries = total == 1 ? "1 measurement entry" : "%d measurement entries".formatted(total);
      statusLine.setText(waiting == 0
          ? entries + " \u00b7 all with data available"
          : entries + " \u00b7 " + (waiting == 1
              ? "1 waiting for data"
              : "%d waiting for data".formatted(waiting)));
    }
    renderStageBand(waiting > 0, waiting);
  }

  /**
   * The stage band states the outstanding work as a gap that has to reach zero, and names the
   * dependency behind it.
   *
   * <p>It is shown only while something is waiting. When everything has data, there is no
   * outstanding step to report, and a band saying so would be a permanent fixture that readers
   * learn to skip — the same reasoning that removed the milestone band once samples existed.</p>
   */
  private void renderStageBand(boolean outstanding, int waiting) {
    stageBand.removeAll();
    stageBand.setVisible(outstanding);
    if (!outstanding) {
      return;
    }
    Span icon = new Span(VaadinIcon.CLOCK.create());
    icon.addClassName("measurement-dataset-stage-icon");

    Span heading = new Span(waiting == 1
        ? "1 measurement is waiting for its data"
        : "%d measurements are waiting for their data".formatted(waiting));
    heading.addClassName("measurement-dataset-stage-heading");

    Span body = new Span(
        "Measurement metadata is registered here; the data files are produced by the facility and "
            + "appear once they have been uploaded and scanned. This view updates automatically.");
    body.addClassName("measurement-dataset-stage-body");

    Div text = new Div(heading, body);
    text.addClassName("measurement-dataset-stage-text");
    stageBand.add(icon, text);
  }

  private Div emptyState() {
    Div empty = new Div();
    empty.addClassName("measurement-dataset-empty-state");

    Span heading = new Span("No measurement data yet");
    heading.addClassName("measurement-dataset-empty-heading");

    Span body = new Span(
        "A measurement entry is the metadata describing how a result was produced, together with "
            + "the data produced by it. Register measurement metadata first: registration assigns "
            + "the measurement code that the data upload is keyed by.");
    body.addClassName("measurement-dataset-empty-body");

    Div actions = new Div();
    actions.addClassName("measurement-dataset-empty-actions");

    boolean hasSamples = hasSamples();
    if (!hasSamples) {
      // The hard dependency chain is experiment design -> samples -> measurements. Saying so is
      // more useful than offering a registration the user cannot complete.
      Span prerequisite = new Span(
          "This experiment has no samples yet. Samples are the subject of a measurement, so they "
              + "have to be registered first.");
      prerequisite.addClassName("measurement-dataset-empty-prerequisite");
      actions.add(prerequisite, linkButton("Go to samples", VaadinIcon.ARROW_RIGHT,
          SampleInformationMain.class));
    } else {
      actions.add(linkButton("Register measurement metadata", VaadinIcon.PLUS,
          MeasurementMain.class));
    }

    empty.add(heading, body, actions);
    return empty;
  }

  private boolean hasSamples() {
    if (context == null) {
      return false;
    }
    String projectId = context.projectId().orElseThrow().value();
    String experimentId = context.experimentId().orElseThrow().value();
    return queryService.getExperimentProgress(projectId, experimentId)
        .map(progress -> progress.sampleCount() > 0)
        .blockOptional()
        .orElse(false);
  }

  private com.vaadin.flow.component.button.Button linkButton(String label,
      VaadinIcon icon, Class<? extends com.vaadin.flow.component.Component> target) {
    var button = new com.vaadin.flow.component.button.Button(label, icon.create());
    button.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
    button.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate(target,
        experimentRouteParameters())));
    return button;
  }

  private RouteParameters experimentRouteParameters() {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, context.projectId().orElseThrow().value()),
        new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
            context.experimentId().orElseThrow().value()));
  }

  private void removeDetailsComponent() {
    if (detailsComponent != null) {
      remove(detailsComponent);
      detailsComponent = null;
    }
    removeAllEmptyStates();
  }

  private void removeAllEmptyStates() {
    content.getChildren()
        .filter(component -> component.getClass() == Div.class)
        .filter(component -> component.getElement().getClassList()
            .contains("measurement-dataset-empty-state"))
        .toList()
        .forEach(content::remove);
  }

  private void writeUrl(ListState state) {
    if (basePath == null || suppressUrlWrite) {
      return;
    }
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    var location = new com.vaadin.flow.router.Location(basePath,
        ListStateCodec.toQueryParameters(state));
    ui.getPage().getHistory().pushState(null, location);
  }

  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    getUI().ifPresent(ui -> ui.getPage().getHistory()
        .setHistoryStateChangeHandler(routerHistoryStateChangeHandler));
  }

  private void onHistoryStateChange(HistoryStateChangeEvent event) {
    String expectedPath = currentPath();
    if (expectedPath == null || !expectedPath.equals(event.getLocation().getPath())) {
      if (routerHistoryStateChangeHandler != null) {
        routerHistoryStateChangeHandler.onHistoryStateChange(event);
      }
      return;
    }
    if (detailsComponent == null) {
      return;
    }
    suppressUrlWrite = true;
    try {
      ListState urlState = ListStateCodec.parse(event.getLocation().getQueryParameters(),
          MeasurementDataSort.DEFAULT, MeasurementDataSort.allowedSortOrders());
      detailsComponent.applyExternalState(urlState);
    } finally {
      suppressUrlWrite = false;
    }
  }

  private String currentPath() {
    if (context == null || context.projectId().isEmpty() || context.experimentId().isEmpty()) {
      return null;
    }
    return String.format(ProjectRoutes.MEASUREMENT_DATASET,
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow().value());
  }
}
