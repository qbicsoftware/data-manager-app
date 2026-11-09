package life.qbic.datamanager.views.projects.project.datasets;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import life.qbic.application.commons.FileSizeFormatter;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.MeasurementType;
import life.qbic.datamanager.views.TagFactory;
import life.qbic.datamanager.views.general.EmptyState;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionTitle;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetMain;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDataDownloadGuide;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDataLocations;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetSegment;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.AssayType;
import life.qbic.projectmanagement.application.query.AssayStanding;
import life.qbic.projectmanagement.application.query.MeasurementDataRow;

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

  /**
   * The query parameter {@link MeasurementDatasetMain} reads to restore a dataset selection.
   *
   * <p>Repeated here rather than exported from that page: the parameter is part of the URL contract
   * between the two surfaces, and the page that produces the link is the one that must agree with
   * the page that consumes it.</p>
   */
  private static final String DATASET_PARAMETER = "dataset";

  /**
   * The band's DOM id, so a link from another page can land on this half of the page.
   *
   * <p>The Datasets page holds two bands, and the project page links to both -- "Measurement
   * datasets" and "Linked datasets". Without a target on the band, both links arrive at the same
   * scroll position (the top), so a reader who asked for the linked half was dropped above the band
   * they asked for and had to scroll past the other one. The id is the anchor half of that contract;
   * {@code ProjectDatasetsMain} does the scrolling, because a route change resets the scroll and the
   * fragment alone would be consumed before the band exists.</p>
   */
  public static final String BAND_ANCHOR = "measurement-datasets";

  /** The anchor of the connected half; see {@link #BAND_ANCHOR}. */
  public static final String CONNECTED_BAND_ANCHOR = "linked-datasets";

  private final transient ProjectQueryService projectQueryService;
  private final Div body = new Div();
  /**
   * The hidden anchor that performs the location export.
   *
   * <p>It must be attached to the UI to be clickable, and it lives in this component rather than in
   * a dialog because the export is triggered from the table, not from a modal: {@code
   * AppDialog} is strictly modal, so an anchor behind one is inert. The experiment page keeps its
   * copy inside its guide dialog for exactly that reason.</p>
   */
  private final life.qbic.datamanager.views.general.download.DownloadComponent downloadComponent =
      new life.qbic.datamanager.views.general.download.DownloadComponent();
  private final transient MeasurementDataLocations dataLocations;
  private final transient life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
      notifications;
  private String projectId;
  private String projectCode;

  public PrimaryDatasetsComponent(ProjectQueryService projectQueryService,
      String downloadEndpoint,
      life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory notifications) {
    this.projectQueryService = requireNonNull(projectQueryService,
        "projectQueryService must not be null");
    this.dataLocations = new MeasurementDataLocations(downloadEndpoint);
    this.notifications = requireNonNull(notifications, "notifications must not be null");
    add(downloadComponent);
    addClassName("project-datasets-primary");
    getElement().setAttribute("id", BAND_ANCHOR);
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

  /**
   * Sets the human-facing project code, used in the exported file's name.
   *
   * <p>Separate from {@link #setProjectId} because the code is resolved asynchronously by the
   * owning page, and a missing code must degrade to the file-name fallback rather than block the
   * band's data.</p>
   */
  public void setProjectCode(String projectCode) {
    this.projectCode = projectCode;
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
    } else {
      body.add(rollup(rows));
      body.add(table(rows));
      body.add(note("A primary dataset is the measurement data of one experiment for one analysis "
          + "method. Open one to see its measurements and primary data."));
    }
    // The body has content now, so a page that asked to land on this band can finally do so. The
    // event says "rendered", not "has rows": the empty state is content too, and a reader who
    // followed the link needs to land on it just the same.
    fireEvent(new DatasetsRenderedEvent(this, false));
  }

  /**
   * Raised once the band's content has been rendered, for a page that needs to act on the finished
   * DOM -- landing on this band from a link is the reason it exists.
   */
  public Registration addDatasetsRenderedListener(
      ComponentEventListener<DatasetsRenderedEvent> listener) {
    return addListener(DatasetsRenderedEvent.class, listener);
  }

  /** Fired after the primary datasets band has rendered its content. */
  public static class DatasetsRenderedEvent extends ComponentEvent<PrimaryDatasetsComponent> {

    @Serial
    private static final long serialVersionUID = 1L;

    public DatasetsRenderedEvent(PrimaryDatasetsComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * One row per primary dataset: one experiment, one omics domain, one analysis method, one
   * instrument.
   *
   * <p><b>The instrument is part of the identity and is therefore a column.</b> The read model keys a
   * dataset by all four (see {@code AssayStanding}), so two rows differing only in instrument are two
   * datasets. Without the instrument on screen those rows rendered identically -- same experiment,
   * same method, same size -- and the table appeared to have printed one row twice.</p>
   *
   * <p>The experiment is the discriminant between two datasets of the same method, which is why it
   * is a column here and a grouping on the summary page: this page is where the reader resolves
   * "which one", the summary only reports "how many".</p>
   */
  // Package-private so a spec can assert the identities the table distinguishes without a UI:
  // reload() returns early without a current UI, which would make the table unreachable in a test.
  static List<DatasetRow> datasetRows(List<AssayStanding> datasets) {
    return datasets.stream()
        .sorted(Comparator.comparing(AssayStanding::experimentName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(dataset -> dataset.assayType().ordinal())
            .thenComparing(AssayStanding::methodLabel)
            .thenComparing(dataset -> dataset.instrumentLabel() == null
                ? "" : dataset.instrumentLabel(), String.CASE_INSENSITIVE_ORDER))
        .map(dataset -> new DatasetRow(
            dataset.experimentId(),
            dataset.experimentName(),
            dataset.methodLabel(),
            dataset.instrumentLabel(),
            dataset.assayType(),
            dataset.measurements(),
            dataset.withPrimaryData(),
            dataset.primaryDataBytes()))
        .toList();
  }

  // ── The rollup ────────────────────────────────────────────────────────

  /**
   * The project's scale, in two groups: one figure about datasets, three about measurements.
   *
   * <p><b>Why grouped.</b> The row used to be four equal figures in one line -- "3 datasets",
   * "20 measurements", "3 with primary data", "17 missing data". Two grains sat side by side with
   * nothing saying which was which, so "17 missing data" read as <em>17 datasets missing data</em>,
   * directly contradicting the 3 beside it. Each group now names its grain and its figures are
   * qualified relative to it ("20 total", "3 with data", "17 missing").</p>
   *
   * <p><b>The measurement total counts attributions, not records.</b> A measurement that measured
   * samples carrying different analysis methods is attributed to each of them (ADR 0010), so this
   * sum can exceed the number of measurement records. The figure is honest at this grain -- it is
   * the number of (dataset, measurement) pairs, which is what the rows below account for -- but a
   * reader adding up the table should not be surprised when it does not match a measurement list
   * elsewhere. The tooltip states it.</p>
   */
  static Div rollup(List<DatasetRow> rows) {
    int measurements = rows.stream().mapToInt(DatasetRow::measurements).sum();
    int withPrimaryData = rows.stream().mapToInt(DatasetRow::withPrimaryData).sum();
    int missing = measurements - withPrimaryData;

    Div rollup = new Div();
    rollup.addClassName("project-datasets-rollup");

    // No qualifier: the group label already names what the figure counts, so "3 total" under
    // "DATASETS" would say it twice. The measurements group needs its labels because it holds three
    // figures with three different meanings.
    rollup.add(rollupGroup("Datasets", stat(rows.size(), null, null)));
    rollup.add(rollupGroup("Measurements",
        withDescription(stat(measurements, "total", null),
            "Counts each measurement once per dataset it belongs to: a measurement made from "
                + "samples of different analysis methods is part of several datasets."),
        stat(withPrimaryData, "with data", withPrimaryData == 0 ? "muted" : "positive"),
        stat(missing, "missing", missing == 0 ? "positive" : "attention")));
    return rollup;
  }

  /** One grain: what it counts, then its figures. */
  private static Div rollupGroup(String label, Div... stats) {
    Div group = new Div();
    group.addClassName("project-datasets-rollup-group");
    Span heading = new Span(label);
    heading.addClassName("project-datasets-rollup-group-label");
    Div figures = new Div();
    figures.addClassName("project-datasets-rollup-stats");
    figures.add(stats);
    group.add(heading, figures);
    return group;
  }

  /**
   * One figure and its qualifier, relative to its group's grain.
   *
   * @param label the qualifier ("with data", "missing"), or {@code null} when the group's own label
   *              already states what the figure counts
   * @param tone  {@code null} for neutral, or a tint marking the coverage figures, so the eye finds
   *              "how much is missing" without reading every label
   */
  private static Div stat(int value, String label, String tone) {
    Div fact = new Div();
    fact.addClassName("project-datasets-stat");
    if (tone != null) {
      fact.addClassName("project-datasets-stat--" + tone);
    }
    Span number = new Span(String.valueOf(value));
    number.addClassName("project-datasets-stat-value");
    fact.add(number);
    if (label != null && !label.isBlank()) {
      Span name = new Span(label);
      name.addClassName("project-datasets-stat-label");
      fact.add(name);
    }
    return fact;
  }

  /** Attaches a tooltip that explains how the figure is counted. */
  private static Div withDescription(Div stat, String description) {
    stat.getElement().setAttribute("title", description);
    stat.addClassName("project-datasets-stat--described");
    return stat;
  }

  // ── The table ─────────────────────────────────────────────────────────

  private Div table(List<DatasetRow> rows) {
    Div table = new Div();
    table.addClassName("project-datasets-table");
    table.add(headerRow());
    rows.forEach(row -> table.add(datasetRow(row)));
    return table;
  }

  /** The columns whose values are right-aligned, so their headers are too. */
  private static final java.util.Set<String> NUMERIC_HEADERS =
      java.util.Set.of("Measurements", "Primary data", "Size");

  static Div headerRow() {
    Div row = new Div();
    row.addClassNames("project-datasets-table-row", "project-datasets-table-row--head");
    for (String header : HEADERS) {
      Span cell = cell(header);
      if (NUMERIC_HEADERS.contains(header)) {
        // A right-aligned figure under a left-aligned header reads as belonging to the column to
        // its left. The header follows its values.
        cell.addClassName("project-datasets-cell--number");
      }
      row.add(cell);
    }
    return row;
  }

  /**
   * The columns, in reading order: what the dataset is, then how much it holds.
   *
   * <p>The instrument is not optional. It is part of the dataset's identity, so a table without it
   * cannot tell two datasets apart.</p>
   */
  static final String[] HEADERS = {"Experiment", "Omics domain", "Method",
      "Instrument", "Measurements", "Primary data", "Size", "Status", ""};

  /**
   * One dataset. The whole row opens <em>that dataset</em> on its experiment's Datasets page, the
   * way the summary's rail stages do, so the target is the row and not a link buried in one
   * column.
   *
   * <p>The destination carries the dataset, not just its experiment. Two datasets of one experiment
   * differ only by method and instrument, so linking to the experiment alone dropped the reader on
   * a page whose default selection was one of them -- and for the row they clicked, possibly the
   * other one. The {@code dataset} query parameter is the same one that page already reads to
   * restore a selection from a shared link.</p>
   */
  Div datasetRow(DatasetRow dataset) {
    Div row = new Div();
    row.addClassNames("project-datasets-table-row", "project-datasets-table-row--clickable");

    Span experimentCell = cell(dataset.experimentName());
    experimentCell.getElement().setAttribute("title", dataset.experimentName());
    row.add(experimentCell);

    // The domain as the pill the rest of the page uses, so the colour language holds: this is the
    // grouping, and it is never the dataset on its own.
    // The domain pill. `assayType()` comes from the measurement's own table (NGS / PXP / IP), not
    // from the method: proteomics and immunopeptidomics share analysis-method vocabulary, so the
    // method cannot imply the domain. A row whose method reads PROTEOMICS on a Genomics pill is
    // therefore reporting two genuinely independent fields from two different sources -- the
    // method from the sample, the domain from the measurement -- and the pair is worth being able
    // to see rather than having the view quietly reconcile.
    Span domainCell = cell(null);
    domainCell.add(TagFactory.forMeasurement(MeasurementType.from(dataset.assayType())));
    domainCell.getElement().setAttribute("title",
        "Omics domain, from the measurement table: " + dataset.assayType());
    row.add(domainCell);

    // The analysis method is the dataset's identity -- "WES" and "RNA-Seq" are both genomics but
    // are not interchangeable -- so it precedes the instrument. The full label carries the title,
    // since "Exome sequencing" repeated on every row is more text than the identity needs.
    Span methodCell = cell(dataset.methodLabel());
    methodCell.addClassName("identifier");
    methodCell.getElement().setAttribute("title", dataset.methodLabel());
    row.add(methodCell);

    // An em dash, not a blank: a measurement that records no instrument has none, and an empty cell
    // reads as a rendering fault. Substituting the method here re-creates the confusion the read
    // model's two independent columns exist to remove.
    Span instrumentCell = cell(dataset.instrumentLabel() == null
        || dataset.instrumentLabel().isBlank() ? "\u2014" : dataset.instrumentLabel());
    instrumentCell.getElement().setAttribute("title",
        dataset.instrumentLabel() == null ? "No instrument recorded" : dataset.instrumentLabel());
    if (dataset.instrumentLabel() == null || dataset.instrumentLabel().isBlank()) {
      instrumentCell.addClassName("project-datasets-cell--missing");
    }
    row.add(instrumentCell);

    Span measurementsCell = cell(String.valueOf(dataset.measurements()));
    measurementsCell.addClassName("project-datasets-cell--number");
    row.add(measurementsCell);

    // "3 of 11" rather than a bare count: the proportion is the fact a reader is judging, and the
    // status pill beside it is the one-word form of the same thing at a glance.
    Span primaryCell = cell("%d of %d".formatted(dataset.withPrimaryData(), dataset.measurements()));
    primaryCell.addClassName("project-datasets-cell--number");
    row.add(primaryCell);

    row.add(sizeCell(dataset));
    row.add(stateCell(dataset));
    row.add(downloadCell(dataset));

    Utility.activateOnClickAndKeys(row, "link", () -> openDataset(dataset));
    return row;
  }

  /**
   * How much primary data this dataset holds.
   *
   * <p>"Not recorded" is a different statement from "—". A dataset can hold measurements carrying
   * primary data whose sizes the sync never recorded, and the read model cannot tell that apart
   * from empty data (migration trap 4d), so a dataset with data but no bytes must not be shown as
   * a zero -- and a dataset with no data at all has no size to state.</p>
   */
  private static Span sizeCell(DatasetRow dataset) {
    if (dataset.bytes() > 0) {
      Span cell = cell(FileSizeFormatter.formatBytesCompact(dataset.bytes()));
      cell.addClassName("project-datasets-cell--number");
      return cell;
    }
    if (dataset.withPrimaryData() > 0) {
      Span cell = cell("Not recorded");
      cell.addClassName("project-datasets-cell--missing");
      return cell;
    }
    Span cell = cell("\u2014");
    cell.addClassName("project-datasets-cell--missing");
    return cell;
  }

  /**
   * The dataset's coverage as a pill.
   *
   * <p>Two words, from the shared vocabulary the experiment panels and the summary rail use, so
   * "Missing data" here is the same statement as "Waiting" there. The proportion is not repeated:
   * the "Primary data" column already carries it, and a pill that restated it would be the second
   * encoding of one fact the panels below were cleaned of.</p>
   */
  private static Span stateCell(DatasetRow dataset) {
    boolean missing = dataset.missing() > 0;
    Span state = new Span(missing ? "Missing data" : "Complete");
    state.addClassNames("project-datasets-state",
        missing ? "project-datasets-state--incomplete" : "project-datasets-state--done");
    Span cell = cell(null);
    cell.add(state);
    return cell;
  }

  /**
   * The row's data-download action.
   *
   * <p><b>Why the row needs it.</b> This table is where a reader answers "which datasets does this
   * project have?" and then, immediately, "where do I get that one?". Until now the only way to
   * obtain a dataset's data was to open it on the experiment's Datasets page and find the download
   * guidance there -- two navigations to reach something this row already identifies.</p>
   *
   * <p><b>Why a button and not the whole row.</b> The row already navigates to the dataset. Two
   * destinations in one click target is the ambiguity this page's cards were cleaned of: the reader
   * could not tell which click would navigate and which would download. The action is therefore its
   * own control, and its click is stopped before the row's handler sees it.</p>
   *
   * <p><b>Why it is always visible.</b> It was revealed on hover, which hid the page's only download
   * path from anyone who had not already discovered it -- an affordance that only appears once you
   * are looking for it is not an affordance. It is a quiet tertiary button in a dedicated column, so
   * a visible row of them does not compete with the data.</p>
   */
  private Div downloadCell(DatasetRow dataset) {
    Button download = new Button("Get data", VaadinIcon.DOWNLOAD.create());
    download.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    download.addClassName("project-datasets-download");
    // A dataset with nothing to fetch has nothing to export. Disabling says so before the click
    // rather than after it, and the tooltip explains the state instead of leaving a dead control.
    boolean hasData = dataset.withPrimaryData() > 0;
    download.setEnabled(hasData);
    download.getElement().setAttribute("title", hasData
        ? "How to fetch this dataset's data files"
        : "No primary data has arrived for this dataset yet");
    if (!hasData) {
      return downloadCell(download);
    }
    // The button sits inside a row that navigates on click, so the click must not reach the row's
    // listener -- otherwise asking for the data would also leave the page. The guard is installed
    // client-side, before the event bubbles, which is the pattern this codebase already uses for
    // nested interactive elements (see the column chooser on the Datasets page). Doing it from the
    // server-side click listener is too late: the row's handler has been given the same event.
    download.getElement().executeJs(
        "this.addEventListener('click', e => e.stopPropagation());");
    download.addClickListener(event -> openDownloadGuide(dataset));
    return downloadCell(download);
  }

  private static Div downloadCell(Button button) {
    Div cell = new Div(button);
    cell.addClassName("project-datasets-cell");
    return cell;
  }

  /**
   * Opens the data-download guidance for one dataset.
   *
   * <p><b>Why a dialog and not a file.</b> The application does not serve measurement data: the
   * files are produced by the facility and held by a separate download service. A bare click that
   * produced a list of URLs therefore handed the reader a file of links with no statement of what to
   * do with them -- no token, no command, no explanation. The experiment page solved this with a
   * three-step guide, and reusing it here means one explanation of the download process in the
   * application rather than two that can disagree.</p>
   *
   * <p>The guide is scoped to this row's dataset, so its export button covers exactly these
   * measurements. Its selection-scoped second action is not offered: this surface has no selection
   * to apply it to.</p>
   */
  private void openDownloadGuide(DatasetRow dataset) {
    var guide = new MeasurementDataDownloadGuide();
    guide.setTokenSettingsTarget(
        life.qbic.datamanager.views.account.PersonalAccessTokenMain.class);
    guide.setSelectionCount(0);
    // This guide is opened for one row, so its export covers one dataset -- not "all measurements"
    // of the experiment, which is what the same dialog means on the Datasets page.
    guide.setScopeLabel("Export data locations",
        "One URL per measurement in this dataset that has data.");
    guide.addExportRequestedListener(event -> exportLocations(guide, dataset));

    var dialog = life.qbic.datamanager.views.general.dialog.AppDialog.medium();
    life.qbic.datamanager.views.general.dialog.DialogHeader.with(dialog,
        "Getting the data of " + datasetLabel(dataset));
    life.qbic.datamanager.views.general.dialog.DialogFooter.withConfirmOnly(dialog, "Close");
    life.qbic.datamanager.views.general.dialog.DialogBody.withoutUserInput(dialog, guide);
    dialog.registerConfirmAction(dialog::close);
    // The guide's download anchor has to live inside the dialog: AppDialog is strictly modal, so an
    // anchor rendered in the page behind it is inert and its synthetic click never lands.
    add(dialog);
    dialog.open();
  }

  /**
   * How the dataset is named to the reader: its method, and its instrument when one was recorded.
   *
   * <p>Not the experiment name alone: two datasets of one experiment differ only by method and
   * instrument, so a dialog headed with the experiment would not say which dataset it is about.</p>
   */
  private static String datasetLabel(DatasetRow dataset) {
    String method = dataset.methodLabel() == null || dataset.methodLabel().isBlank()
        ? "measurement dataset" : dataset.methodLabel();
    return dataset.instrumentLabel() == null || dataset.instrumentLabel().isBlank()
        ? method
        : method + " \u00b7 " + dataset.instrumentLabel();
  }

  /**
   * Exports the data locations of one dataset: every measurement the dataset holds that has data.
   *
   * <p>The measurements are resolved through the same filter the experiment's Datasets page uses to
   * scope its own export, so the two surfaces agree on what "this dataset's data" means. Only
   * measurements that actually carry data are listed: a URL for a measurement whose files have not
   * arrived is a dead link, and the list's purpose is to be fetched.</p>
   */
  private void exportLocations(MeasurementDataDownloadGuide guide, DatasetRow dataset) {
    var filter = new ProjectQueryService.MeasurementDataFilter("", dataset.assayType(),
        dataset.methodLabel(), dataset.instrumentLabel(),
        MeasurementDataRow.DataStatus.AVAILABLE, List.of());
    List<String> codes = projectQueryService
        .getMeasurementData(projectId, dataset.experimentId(), 0, Integer.MAX_VALUE, filter)
        .map(MeasurementDataRow::measurementCode)
        .collectList()
        .blockOptional()
        .orElse(List.of());
    if (codes.isEmpty()) {
      // Reachable despite the button being enabled: the counts and the list are separate reads, so
      // a measurement deleted in between lands here. Say so rather than downloading an empty file.
      notifications.toast("rawdata.no-dataset-selected", new Object[]{}, getLocale()).open();
      return;
    }
    guide.triggerDownload(dataLocations.locationList(codes, projectCode,
        dataset.methodLabel() == null || dataset.methodLabel().isBlank()
            ? "measurement_dataset" : dataset.methodLabel()));
  }

  private static Span cell(String text) {
    Span cell = new Span();
    cell.addClassName("project-datasets-cell");
    if (text != null) {
      cell.setText(text);
    }
    return cell;
  }

  /**
   * Opens the dataset's own workspace: its experiment's Datasets page, with this dataset selected.
   *
   * <p>The selection travels in the {@code dataset} query parameter, which that page already reads
   * (it is how a shared link restores a selection). Without it, clicking the second of two datasets
   * that differ only by instrument landed on the page with the <em>first</em> one active.</p>
   */
  private void openDataset(DatasetRow dataset) {
    MeasurementDatasetSegment segment = MeasurementDatasetSegment.of(dataset.assayType(),
        dataset.methodLabel(), dataset.instrumentLabel());
    String path = AppRoutes.ProjectRoutes.MEASUREMENT_DATASET.formatted(projectId,
        dataset.experimentId());
    String href = path + "?" + DATASET_PARAMETER + "=" + URLEncoder.encode(segment.urlValue(),
        StandardCharsets.UTF_8);
    getUI().ifPresent(ui -> ui.getPage().open(href, "_self"));
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
  record DatasetRow(String experimentId, String experimentName,
                            String methodLabel, String instrumentLabel, AssayType assayType,
                            int measurements, int withPrimaryData, long bytes) {

    int missing() {
      return Math.max(0, measurements - withPrimaryData);
    }
  }
}
