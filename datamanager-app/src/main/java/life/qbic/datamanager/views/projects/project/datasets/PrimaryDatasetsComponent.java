package life.qbic.datamanager.views.projects.project.datasets;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.MeasurementType;
import life.qbic.datamanager.views.TagFactory;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMain;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.AssayType;
import life.qbic.projectmanagement.application.query.ExperimentProgress;

/**
 * <b>The project's primary datasets</b>
 *
 * <p>Answers "which datasets does this project have?", the produced half of the Datasets page. The
 * connected half is {@link ConnectedResourcesComponent}, which is hosted elsewhere and linked
 * here; the two are different things and are titled separately.</p>
 *
 * <p><b>A dataset is one experiment and one assay type.</b> Not one per experiment: a project
 * already holds one primary dataset per assay type, so an experiment that was measured as genomics
 * and as proteomics contributes two. A type an experiment never measured is not an empty dataset
 * and is not listed. This is the grain the read model can back — per-experiment, per-type
 * measurement counts, primary-data coverage and volume — and it keeps the per-experiment
 * attribution that a project-level rollup throws away.</p>
 *
 * <p>The rows are derived, not addressable: primary datasets have no identity of their own yet, so
 * a row cannot be linked to. Opening one goes to that experiment's measurements, which is where
 * its measurement data lives today.</p>
 *
 * @see ProjectDatasetsMain
 * @since 1.19.0
 */
public class PrimaryDatasetsComponent extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String PROJECT_ID_PARAMETER = "projectId";
  private static final String EXPERIMENT_ID_PARAMETER = "experimentId";

  private final transient ProjectQueryService projectQueryService;
  private final Div body = new Div();
  private String projectId;

  public PrimaryDatasetsComponent(ProjectQueryService projectQueryService) {
    this.projectQueryService = requireNonNull(projectQueryService,
        "projectQueryService must not be null");
    addClassName("project-datasets-primary");
    body.addClassName("project-datasets-primary-body");
    add(body);
  }

  /** Sets the project to report on and loads its datasets. */
  public void setProjectId(String projectId) {
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    reload();
  }

  // ── Loading ───────────────────────────────────────────────────────────

  private void reload() {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    body.removeAll();
    body.add(message("Loading datasets …"));
    ReactiveUiBridge.onUi(ui,
        projectQueryService.getExperimentProgress(projectId).collectList(),
        this::render,
        error -> {
          body.removeAll();
          body.add(message("The project's datasets could not be loaded."));
        });
  }

  private void render(List<ExperimentProgress> progress) {
    body.removeAll();
    List<DatasetRow> rows = datasetRows(progress);
    if (rows.isEmpty()) {
      body.add(emptyState());
      return;
    }
    body.add(rollup(rows));
    body.add(table(rows));
    body.add(note("A primary dataset is the measurement data of one experiment for one assay "
        + "type. Open one to see its measurements and primary data."));
  }

  /**
   * One row per primary dataset: one experiment, one assay type, ordered by experiment.
   *
   * <p>The experiments come back in no particular order, so the rows are sorted by name; the
   * sections of the page are otherwise listed in a stable order.</p>
   */
  private static List<DatasetRow> datasetRows(List<ExperimentProgress> progress) {
    return progress.stream()
        .sorted(Comparator.comparing(ExperimentProgress::experimentName,
            String.CASE_INSENSITIVE_ORDER))
        .flatMap(experiment -> experiment.assayTypes().stream()
            .map(type -> new DatasetRow(
                experiment.experimentId(),
                experiment.experimentName(),
                type,
                experiment.measurementCount(type),
                experiment.measurementsWithPrimaryData(type),
                experiment.primaryDataBytes(type))))
        .toList();
  }

  // ── The rollup ────────────────────────────────────────────────────────

  /**
   * The four figures a reader wants before the detail.
   *
   * <p>This states coverage, which the summary page deliberately does not: at project grain
   * "8 of 284 measurements" hides which experiment is behind it, but here every figure is backed
   * by a row that names one.</p>
   */
  private static Div rollup(List<DatasetRow> rows) {
    int measurements = rows.stream().mapToInt(DatasetRow::measurements).sum();
    int withPrimaryData = rows.stream().mapToInt(DatasetRow::withPrimaryData).sum();
    Div rollup = new Div();
    rollup.addClassName("project-datasets-rollup");
    rollup.add(stat(rows.size(), rows.size() == 1 ? "dataset" : "datasets"));
    rollup.add(stat(measurements, "measurements"));
    rollup.add(stat(withPrimaryData, "with primary data"));
    rollup.add(stat(measurements - withPrimaryData, "missing data"));
    return rollup;
  }

  private static Div stat(int value, String label) {
    Div fact = new Div();
    fact.addClassName("project-datasets-stat");
    Span number = new Span(String.valueOf(value));
    number.addClassName("project-datasets-stat-value");
    Span name = new Span(label);
    name.addClassName("project-datasets-stat-label");
    fact.add(number, name);
    return fact;
  }

  // ── The table ─────────────────────────────────────────────────────────

  private Div table(List<DatasetRow> rows) {
    Div table = new Div();
    table.addClassName("project-datasets-table");
    table.add(headerRow());
    rows.forEach(row -> table.add(datasetRow(row)));
    return table;
  }

  private static Div headerRow() {
    Div row = new Div();
    row.addClassNames("project-datasets-table-row", "project-datasets-table-row--head");
    for (String header : new String[]{"Experiment", "Assay type", "Measurements", "Primary data",
        "Size", "Status"}) {
      row.add(cell(header));
    }
    return row;
  }

  /**
   * One dataset. The whole row opens the experiment, the way the summary's rail stages do, so the
   * target is the row and not a link buried in one column.
   */
  private Div datasetRow(DatasetRow dataset) {
    Div row = new Div();
    row.addClassNames("project-datasets-table-row", "project-datasets-table-row--clickable");
    row.add(cell(dataset.experimentName()));

    Span typeCell = cell(null);
    typeCell.add(TagFactory.forMeasurement(MeasurementType.from(dataset.assayType())));
    row.add(typeCell);

    row.add(cell(String.valueOf(dataset.measurements())));
    row.add(cell(dataset.withPrimaryData() + " of " + dataset.measurements()));
    row.add(sizeCell(dataset));
    row.add(stateCell(dataset));

    Utility.activateOnClickAndKeys(row, "link", () -> openExperiment(dataset.experimentId()));
    return row;
  }

  /**
   * How much primary data this dataset holds.
   *
   * <p>"Not recorded" is a different statement from "—". A dataset can hold measurements carrying
   * primary data whose sizes the sync never recorded, and the read model cannot tell that apart
   * from empty data (migration trap 4d), so a dataset with data but no bytes must not be shown as
   * a zero — and a dataset with no data at all has no size to state.</p>
   */
  private static Span sizeCell(DatasetRow dataset) {
    if (dataset.bytes() > 0) {
      return cell(Utility.humanReadableSize(dataset.bytes()));
    }
    if (dataset.withPrimaryData() > 0) {
      Span cell = cell("Not recorded");
      cell.addClassName("project-datasets-cell--missing");
      return cell;
    }
    Span cell = cell("—");
    cell.addClassName("project-datasets-cell--missing");
    return cell;
  }

  private static Span stateCell(DatasetRow dataset) {
    boolean missing = dataset.missing() > 0;
    Span state = new Span(missing ? "Missing data" : "Complete");
    state.addClassNames("project-datasets-state",
        missing ? "project-datasets-state--attention" : "project-datasets-state--done");
    Span cell = cell(null);
    cell.add(state);
    return cell;
  }

  private static Span cell(String text) {
    Span cell = new Span();
    cell.addClassName("project-datasets-cell");
    if (text != null) {
      cell.setText(text);
    }
    return cell;
  }

  private void openExperiment(String experimentId) {
    getUI().ifPresent(ui -> ui.navigate(MeasurementMain.class, new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, projectId),
        new RouteParam(EXPERIMENT_ID_PARAMETER, experimentId))));
  }

  // ── Empty state and messages ──────────────────────────────────────────

  private static Div emptyState() {
    Div empty = new Div();
    empty.addClassName("project-datasets-empty");
    empty.add(heading("No primary datasets yet"));
    empty.add(note("A dataset appears here as soon as an experiment has measurements registered: "
        + "the measurement data of an experiment is its primary dataset."));
    return empty;
  }

  private static Span heading(String text) {
    Span heading = new Span(text);
    heading.addClassName("project-datasets-empty-title");
    return heading;
  }

  private static Span note(String text) {
    Span note = new Span(text);
    note.addClassName("project-datasets-note");
    return note;
  }

  private static Span message(String text) {
    Span message = new Span(text);
    message.addClassName("project-datasets-message");
    return message;
  }

  /** One primary dataset: the measurement data of one experiment for one assay type. */
  private record DatasetRow(String experimentId, String experimentName, AssayType assayType,
                            int measurements, int withPrimaryData, long bytes) {

    int missing() {
      return Math.max(0, measurements - withPrimaryData);
    }
  }
}
