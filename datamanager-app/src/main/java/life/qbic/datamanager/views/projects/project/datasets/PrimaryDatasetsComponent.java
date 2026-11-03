package life.qbic.datamanager.views.projects.project.datasets;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.MeasurementType;
import life.qbic.datamanager.views.TagFactory;
import life.qbic.datamanager.views.general.EmptyState;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionTitle;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetMain;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.AssayType;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import life.qbic.projectmanagement.application.query.AssayStanding;

/**
 * <b>The project's primary datasets</b>
 *
 * <p>Answers "which datasets does this project have?", the produced half of the Datasets page. The
 * connected half is {@link ConnectedResourcesComponent}, which is hosted elsewhere and linked
 * here; the two are different things and are titled separately.</p>
 *
 * <p><b>A dataset is one experiment for one analysis method.</b> Not one per experiment: an
 * experiment measured as WES and as RNA-Seq contributes two, and two experiments both measured as
 * WES contribute two as well. The analysis method, not the omics domain, is the identity, because
 * WES, RNA-Seq and 16S are all genomics and are not interchangeable -- a reader picks a dataset by
 * the analysis they intend to run. A method an experiment never measured is not an empty dataset and
 * is not listed.</p>
 *
 * <p>The grain comes from {@code v_project_assay_standing}, shared with the project summary page, so
 * the two surfaces cannot disagree about how many datasets a project holds. See ADR 0010.</p>
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
    // The band titles itself. Without it the page had one named half and one nameless block above
    // it, which read as a band plus an orphan rather than as two halves of one page.
    var header = new SectionHeader(
        new SectionTitle("Measurement datasets", SectionTitle.Size.MEDIUM));
    Section section = new Section.SectionBuilder().withHeader(header).build();
    body.addClassName("project-datasets-primary-body");
    section.content().add(body);
    add(section);
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
    // Read from the assay-standing read model, not from experiment_progress: this page and the
    // project summary must resolve a dataset the same way, or a project that holds "2 WES datasets"
    // on the summary would hold "1 Genomics dataset" here. See ADR 0010.
    ReactiveUiBridge.onUi(ui,
        projectQueryService.getAssayStanding(projectId).collectList(),
        this::render,
        error -> {
          body.removeAll();
          body.add(message("The project's datasets could not be loaded."));
        });
  }

  private void render(List<AssayStanding> datasets) {
    body.removeAll();
    List<DatasetRow> rows = datasetRows(datasets);
    if (rows.isEmpty()) {
      body.add(emptyState());
      return;
    }
    body.add(rollup(rows));
    body.add(table(rows));
    body.add(note("A primary dataset is the measurement data of one experiment for one analysis "
        + "method. Open one to see its measurements and primary data."));
  }

  /**
   * One row per primary dataset: one experiment, one analysis method.
   *
   * <p>The read model already returns exactly that grain -- one row per
   * {@code (experiment, analysis method)} -- so this only sorts. The experiment is the discriminant
   * between two datasets of the same method, which is why it is a column here and a grouping on the
   * summary page: this page is where the reader resolves "which one", the summary only reports "how
   * many".</p>
   */
  private static List<DatasetRow> datasetRows(List<AssayStanding> datasets) {
    return datasets.stream()
        .sorted(Comparator.comparing(AssayStanding::experimentName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(dataset -> dataset.analysisMethod().abbreviation()))
        .map(dataset -> new DatasetRow(
            dataset.experimentId(),
            dataset.experimentName(),
            dataset.analysisMethod(),
            dataset.assayType(),
            dataset.measurements(),
            dataset.withPrimaryData(),
            dataset.primaryDataBytes()))
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
    for (String header : new String[]{"Experiment", "Analysis method", "Domain", "Measurements",
        "Primary data", "Size", "Status"}) {
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

    // The analysis method is the dataset's identity -- "WES" and "RNA-Seq" are both genomics but
    // are not interchangeable -- so it leads and the domain follows as the grouping it is. The
    // abbreviation is shown and the full label carries the title, since "Exome sequencing" repeated
    // on every row is more text than the identity needs.
    Span methodCell = cell(dataset.analysisMethod().abbreviation());
    methodCell.getElement().setAttribute("title", dataset.analysisMethod().label());
    row.add(methodCell);

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
        missing ? "project-datasets-state--incomplete" : "project-datasets-state--done");
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
    getUI().ifPresent(ui -> ui.navigate(MeasurementDatasetMain.class, new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, projectId),
        new RouteParam(EXPERIMENT_ID_PARAMETER, experimentId))));
  }

  // ── Empty state and messages ──────────────────────────────────────────

  private static EmptyState emptyState() {
    // No action: nothing on this page can produce a measurement, and the component's own rule is
    // that an empty state offers no action when the item has to be created elsewhere rather than a
    // dead end. The experiment is created on the summary page.
    return new EmptyState(VaadinIcon.FLASK, "No primary datasets yet",
        "A dataset appears here as soon as an experiment has measurements registered: the "
            + "measurement data of an experiment is its primary dataset.");
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
  private record DatasetRow(String experimentId, String experimentName,
                            AnalysisMethod analysisMethod, AssayType assayType,
                            int measurements, int withPrimaryData, long bytes) {

    int missing() {
      return Math.max(0, measurements - withPrimaryData);
    }
  }
}
