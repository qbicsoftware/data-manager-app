package life.qbic.datamanager.views.general.snapshot;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import life.qbic.datamanager.views.MeasurementType;
import life.qbic.datamanager.views.general.DataStateLabel;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.projectmanagement.application.query.AssayStanding;
import life.qbic.projectmanagement.application.query.DataState;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.RecentMeasurement;
import life.qbic.projectmanagement.application.query.RecentSample;

/**
 * <b>A surface's snapshot panel: what it holds, in what state, and the way in.</b>
 *
 * <p>This is the project overview's panel, extracted so the experiment overview renders the same
 * thing. The two surfaces describe the same two data sets — an experiment's samples and its
 * measurement datasets — at the same grain, for the same reader, one click apart. They had drifted
 * into two different card anatomies with two different state vocabularies, and the experiment
 * overview was the one that had not picked up the revisions: it still carried a completeness ring
 * and a "Registered"/"Not complete yet" dialect that the project page had already retired.</p>
 *
 * <p><b>The anatomy, top to bottom, is one thing per band:</b></p>
 * <ol>
 *   <li><b>head</b> — the decorative glyph, the title (which <em>is</em> the way in), and optionally
 *       a state tag;</li>
 *   <li><b>metric</b> — a fixed two-line slot: the count, then a qualifier that gives the count its
 *       meaning;</li>
 *   <li><b>status</b> — one line of prose saying what the rows cannot;</li>
 *   <li><b>rows</b> — one grid for the whole table, not one grid per row;</li>
 *   <li><b>footer</b> — "All N …", the second way into the same surface.</li>
 * </ol>
 *
 * <p><b>One numeric statement.</b> The metric band states the count once, in words, and the panel
 * carries no completeness ring: a proportion is the ring's encoding, and "17 of 20" says it more
 * precisely. The band is a fixed two-line slot rather than content-sized, because a wrapping
 * qualifier used to push the status line and the whole table down by a line in one panel only, so
 * two panels side by side lost the baseline they were built for.</p>
 *
 * <p><b>The panel is not a click target.</b> Its one destination is the title in the header band. A
 * container that is also a link announces as a button, loses the native {@code <a>} affordances
 * (middle-click, copy link address) and leaves the reader guessing which click navigates.</p>
 *
 * @since 1.22.0
 */
public class SnapshotPanel extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private SnapshotPanel() {
    addClassName("snapshot-panel");
  }

  /**
   * Makes this panel take an equal share of a {@code display: flex} row, wrapping when narrow.
   *
   * <p>Opt-in, because the two pages lay their panels out differently. The project overview puts
   * them in a wrapping flex row, where a panel without a flex basis collapses to its content width
   * and leaves the rest of the row empty. The experiment overview's data band is a two-track grid,
   * where a flex basis would fight the grid instead of filling the cell it was placed in.</p>
   */
  public SnapshotPanel equalInRow() {
    addClassName("snapshot-panel--equal");
    return this;
  }

  /**
   * The panel's header: glyph, title (the link), and an optional state tag.
   *
   * @param label     the surface's name; it is also the link text
   * @param href      where the title navigates
   * @param stateWord the state tag's word, or {@code null} for no tag
   * @param stateTone the state tag's tone class suffix, ignored when {@code stateWord} is null
   */
  public SnapshotPanel head(String label, String href, String stateWord, String stateTone) {
    Div head = new Div();
    head.addClassName("snapshot-panel-head");
    // The title IS the way in. A separate "Open …" link beside an inert heading states one thing
    // twice and leaves the reader working out whether the two lead to the same place.
    Anchor titleLink = Utility.textLink(label, href, "snapshot-panel-label");
    head.add(surfaceIcon(label), titleLink);
    // A panel whose rows carry their own state column passes no state, and gets no tag: the tag is
    // optional, not part of the header's furniture.
    if (stateWord != null && !stateWord.isBlank()) {
      Span tag = new Span(stateWord);
      tag.addClassNames("snapshot-panel-state", "snapshot-panel-state--" + stateTone);
      head.add(tag);
    }
    add(head);
    return this;
  }

  /**
   * The metric band: the count, and the qualifier that gives it its meaning.
   *
   * <p>The qualifier is always rendered, even when empty, because its reserved height is what holds
   * two panels on one baseline. It is clamped to two lines so it can never outgrow the slot.</p>
   *
   * @param count     the number the surface holds
   * @param unit      what the count counts, e.g. {@code "samples"}; pluralised by the caller
   * @param qualifier the line under the count, or {@code null} for an empty reserved line
   */
  public SnapshotPanel metric(int count, String unit, String qualifier) {
    Div metric = new Div();
    metric.addClassName("snapshot-panel-metric");

    Div countLine = new Div();
    countLine.addClassName("snapshot-panel-count-line");
    Span value = new Span(Integer.toString(count));
    value.addClassName("snapshot-panel-value");
    Span unitSpan = new Span(unit);
    unitSpan.addClassName("snapshot-panel-unit");
    countLine.add(value, unitSpan);
    metric.add(countLine);

    Span qualifierSpan = new Span(qualifier == null ? "" : qualifier);
    qualifierSpan.addClassName("snapshot-panel-qualifier");
    metric.add(qualifierSpan);
    add(metric);
    return this;
  }

  /** One line of prose stating what the rows below cannot. */
  public SnapshotPanel status(String text) {
    Span status = new Span(text == null ? "" : text);
    status.addClassName("snapshot-panel-status");
    add(status);
    return this;
  }

  /**
   * The table: a header row and one row per record, in a single grid.
   *
   * @param columns          the column headers
   * @param rows             the records, each cell count matching {@code columns}
   * @param identifierColumn whether the first column is a code a reader copies (monospace,
   *                         truncating with the full value in a title attribute)
   * @param emptyRow         the sentence standing in for rows when there are none
   */
  public SnapshotPanel rows(String[] columns, List<List<String>> rows, boolean identifierColumn,
      String emptyRow) {
    Div list = new Div();
    list.addClassName("snapshot-panel-rows");
    // The grid's column count has to match the number of cells a row emits, or the extra cell wraps
    // onto an implicit row instead of failing loudly. Past three columns a spacer cell precedes the
    // trailing numbers, and the grid needs one track more than the row has data cells.
    int gridTracks = columns.length > 3 ? columns.length + 1 : columns.length;
    if (gridTracks > 3) {
      list.addClassName("snapshot-panel-rows--" + gridTracks + "-columns");
    }
    list.add(row(columns, true, identifierColumn));
    if (rows.isEmpty()) {
      list.add(placeholderRow(emptyRow));
    } else {
      rows.forEach(cells -> list.add(row(cells.toArray(new String[0]), false, identifierColumn)));
    }
    add(list);
    return this;
  }

  /**
   * The way out of the preview, below a hairline.
   *
   * <p>It says what is <em>behind</em> the preview rather than restating the panel's own count: a
   * panel that previews one row per dataset above "All 20 measurements" is answering two different
   * questions in one band, which reads as an arithmetic error.</p>
   *
   * @param text the link text, e.g. {@code "All 8 samples →"}; blank or {@code null} adds no footer
   * @param href where it navigates
   */
  public SnapshotPanel footer(String text, String href) {
    if (text == null || text.isBlank()) {
      return this;
    }
    Div footer = new Div();
    footer.addClassName("snapshot-panel-footer");
    footer.add(Utility.textLink(text, href, "snapshot-panel-all"));
    add(footer);
    return this;
  }

  /**
   * The decorative glyph that maps a panel to its subject: Samples is a flask, a dataset a
   * database.
   *
   * <p>Decoration only — the label names the surface, so the glyph is redundant reinforcement for
   * people who scan shapes, and it must never become the only carrier of meaning. Hence
   * {@code aria-hidden}.</p>
   *
   * <p>Matched on the word the label starts with rather than on the exact label, so a rewrite of the
   * label cannot silently drop the glyph.</p>
   */
  private static Icon surfaceIcon(String label) {
    Icon glyph = label.startsWith("Measurement") || label.startsWith("Dataset")
        ? VaadinIcon.DATABASE.create()
        : label.startsWith("Sample") ? VaadinIcon.FLASK.create() : VaadinIcon.RECORDS.create();
    glyph.addClassName("snapshot-panel-icon");
    glyph.getElement().setAttribute("aria-hidden", "true");
    return glyph;
  }

  /** Marked on the spacer cell so the grid can be laid out without counting cells by hand. */
  private static final String SPACER_CLASS = "snapshot-panel-cell-spacer";

  private static Div row(String[] cells, boolean header, boolean identifierColumn) {
    Div row = new Div();
    row.addClassName("snapshot-panel-row");
    // A spacer before the trailing numbers: it absorbs the free space so the identity group stays
    // tight on the left and the numbers keep the right edge. The CSS grid declares a matching track;
    // both are derived from the same rule, so they cannot drift.
    int spacerAt = cells.length > 3 ? cells.length - 2 : -1;
    for (int index = 0; index < cells.length; index++) {
      if (index == spacerAt) {
        Span spacer = new Span();
        spacer.addClassName(SPACER_CLASS);
        row.add(spacer);
      }
      Span span = new Span(cells[index]);
      if (header) {
        span.addClassName("snapshot-panel-cell-header");
      } else if (index == 0 && identifierColumn) {
        // Only where the first column really is a code a reader copies -- Sample ID, Measurement ID.
        // It truncates rather than wrapping mid-code, so the untruncated value has to stay
        // reachable: a measurement ID runs to 24 characters and is the thing a reader copies.
        span.addClassName("identifier");
        span.getElement().setAttribute("title", cells[index]);
      }
      row.add(span);
    }
    return row;
  }

  /** A message standing in for rows. Not a record, so it takes none of the row's cell classes. */
  private static Div placeholderRow(String text) {
    Div row = new Div();
    row.addClassName("snapshot-panel-row");
    row.add(new Span(text));
    return row;
  }

  // ── The two panels both pages render ─────────────────────────────────────

  /** Pluralises a count against its unit, e.g. {@code 1 sample} / {@code 8 samples}. */
  public static String pluralise(int count, String singular, String plural) {
    return count + " " + (count == 1 ? singular : plural);
  }

  /**
   * The experiment's newest samples, as a panel.
   *
   * <p>The rows preview <em>recency</em>, which is the decision taken for both panels: the newest
   * three, with "Changed" carrying the meaning, and the header offering the way to the full list.</p>
   *
   * <p>Samples take the {@link DataState#COMPLETE} word rather than a bespoke "Registered": samples
   * are either there or not, and "Registered" was outside the declared vocabulary. The panel has no
   * data column, so it keeps its header tag — unlike the dataset panel, whose rows carry the state
   * themselves.</p>
   *
   * @param progress  the experiment's read model
   * @param samples   the newest samples, newest first
   * @param openHref  where the panel's title and footer lead
   * @param allRowsText the footer's link text, or blank for none
   */
  public static SnapshotPanel samples(ExperimentProgress progress, List<RecentSample> samples,
      String openHref, String allRowsText) {
    boolean started = progress.sampleCount() > 0;
    String status = samples.isEmpty()
        ? "No samples registered yet"
        : "Newest sample " + Utility.relativeTime(samples.getFirst().changed());
    List<List<String>> rows = samples.stream()
        .map(sample -> List.of(sample.sampleCode(), sample.sampleName(),
            Utility.relativeTime(sample.changed())))
        .toList();
    return new SnapshotPanel()
        .head("Samples", openHref,
            started ? DataStateLabel.word(DataState.COMPLETE) : "Not started",
            started ? "done" : "active")
        .metric(progress.sampleCount(),
            progress.sampleCount() == 1 ? "sample" : "samples", null)
        .status(status)
        .rows(new String[]{"Sample ID", "Sample name", "Changed"}, rows, true,
            "No samples to show yet")
        .footer(allRowsText, openHref);
  }

  /**
   * The experiment's measurement datasets: one row per dataset, not per measurement.
   *
   * <p><b>What a row is.</b> One dataset — one experiment for one omics domain, method and
   * instrument. This is the panel's central correction over the measurement-grain list it replaces:
   * "Not complete yet" is a per-dataset condition, and a list of measurement IDs cannot say which
   * dataset is behind, which is the question the panel exists to answer. It is also the grain the
   * project overview already uses, so the two pages cannot disagree about how many datasets an
   * experiment holds or which ones they are.</p>
   *
   * <p><b>No header tag.</b> The table's "Data availability" column states every dataset's state on
   * its own row, so a tag in the header would be the same fact at a weaker grain. That is why the
   * header tag is optional in {@link #head}.</p>
   *
   * <p><b>No row cap.</b> More than four datasets in one experiment does not arise in practice, and
   * the full list is one link away.</p>
   *
   * @param progress     the experiment's read model
   * @param measurements the newest measurements, used only to state the newest data arrival
   * @param assays       the experiment's datasets, in any order
   * @param assaysFailed whether the dataset query failed, so the panel can say so instead of
   *                     reporting "no datasets" — those are opposite statements
   * @param openHref     where the panel's title leads
   * @param allRowsText  the footer's link text, or blank for none
   */
  public static SnapshotPanel datasets(ExperimentProgress progress,
      List<RecentMeasurement> measurements, List<AssayStanding> assays, boolean assaysFailed,
      String openHref, String allRowsText) {
    // One row per dataset, ordered by domain then method so the list is stable and reads as groups.
    List<AssayStanding> datasets = assays.stream()
        .sorted(java.util.Comparator.comparing((AssayStanding dataset) ->
                dataset.assayType().ordinal())
            .thenComparing(AssayStanding::methodLabel))
        .toList();

    // Four independent facts, one column each. The method and the instrument are NOT merged: a
    // single COALESCEd column showed an assay ("WGS") on one row and an instrument ("Orbitrap
    // Exploris 480") on the next with nothing to tell them apart. The instrument cell is an em dash
    // when the measurement records none; substituting the method there re-creates that confusion,
    // while an empty cell says "not recorded", which is true.
    List<List<String>> rows = datasets.stream()
        .map(dataset -> List.of(
            MeasurementType.from(dataset.assayType()).getType(),
            dataset.methodLabel(),
            dataset.instrumentLabel() == null || dataset.instrumentLabel().isBlank()
                ? "\u2014" : dataset.instrumentLabel(),
            "%d of %d".formatted(dataset.withPrimaryData(), dataset.measurements()),
            DataStateLabel.word(DataState.of(dataset.withPrimaryData(), dataset.measurements()))))
        .toList();

    long missing = datasets.stream().filter(dataset -> !dataset.complete()).count();
    String qualifier = datasets.isEmpty() || missing == 0 ? null
        : "%d of %d %s need primary data".formatted(missing, datasets.size(),
            datasets.size() == 1 ? "dataset" : "datasets");

    String status = progress.measurementCount() == 0
        ? "No measurements registered yet"
        : "Newest data " + Utility.relativeTime(newest(measurements));

    return new SnapshotPanel()
        .head("Measurement datasets", openHref, null, null)
        .metric(datasets.size(), datasets.size() == 1 ? "dataset" : "datasets", qualifier)
        .status(status)
        .rows(new String[]{"Omics domain", "Method", "Instrument", "With data",
                "Data availability"}, rows, false,
            // A failed query must not read as "this project has no datasets": those are opposite
            // statements, and the silent version of this hid a broken query once already.
            assaysFailed ? "Datasets could not be loaded" : "No measurements to show yet")
        .footer(allRowsText, openHref);
  }

  /** The newest sample or measurement timestamp, for the panel's recency line. */
  private static java.time.Instant newest(List<RecentMeasurement> measurements) {
    return measurements.stream()
        .map(RecentMeasurement::changed)
        .filter(java.util.Objects::nonNull)
        .max(java.time.Instant::compareTo)
        .orElse(null);
  }
}
