package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.FileSizeFormatter;
import life.qbic.datamanager.views.general.CompletenessRing;

/**
 * The experiment's measurement datasets, one card each, above the list.
 *
 * <p>A <b>measurement dataset</b> is the experiment's data for one omics domain, one method and one
 * instrument, so an experiment has one per combination it actually measured rather than one in
 * total. That makes the datasets the page's primary structure, not a filter: the reader's first
 * question is "what do I have?", not "which subset do I want?". An earlier revision hid the methods
 * in a dropdown, and a reader who did not open it reasonably concluded the page showed all
 * measurements — the grouping was correct and invisible.</p>
 *
 * <p>The omics domain still groups the cards, because in a multi-omics experiment "Genomics,
 * Proteomics, Immunopeptidomics" is how a reader navigates. What it is not is the dataset: one domain
 * can back several datasets, which is why the card names the method and instrument under the domain
 * rather than the domain alone.</p>
 *
 * <p>Each card states the dataset's size, because that is what the reader needs in order to
 * recognise which dataset they are looking at: how many measurements it holds, how many
 * still wait for data, and how much data has arrived. A card is selected by pointer or by
 * Enter/Space, and the selection is what activates the list below it.</p>
 *
 * <p>Only methods that actually have measurements get a card. A method an experiment never measured
 * is not an empty dataset, and rendering an empty card for it would invent a dataset that does not
 * exist.</p>
 *
 * @since 1.21.0
 */
public class MeasurementDatasetSelector extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = -1L;

  /**
   * The key of the "all measurements" card.
   *
   * <p>A sentinel rather than a fourth segment: "all measurements" is not a dataset -- it is the
   * absence of a dataset filter -- and giving it a {@link MeasurementDatasetSegment} value would make
   * it a fake dataset that every segment-shaped consumer would then have to special-case. A
   * {@code null} key cannot be held in the {@link LinkedHashMap} the cards are registered in, so the
   * sentinel is a value, and only {@link #selected()} translates it back to {@code null}.</p>
   */
  private static final Object ALL = new Object();

  private final Div cards = new Div();
  private final Map<Object, Div> cardByKey = new LinkedHashMap<>();
  private MeasurementDatasetSegment selected;

  /**
   * Whether a card has been chosen at all.
   *
   * <p>Needed because {@code null} is a real selection -- the "all measurements" card -- and not
   * merely "nothing chosen yet". Without it, selecting All from a fresh page would be indistinguishable
   * from the initial state and the change event would be suppressed.</p>
   */
  private boolean hasSelection;

  public MeasurementDatasetSelector() {
    addClassName("measurement-dataset-selector");
    cards.addClassName("measurement-dataset-selector-cards");
    // A radio-like group: the cards are mutually exclusive choices, and the role says so to a
    // screen reader, which a row of plain divs would not.
    cards.getElement().setAttribute("role", "radiogroup");
    cards.getElement().setAttribute("aria-label", "Measurement datasets");
    add(cards);
  }

  /**
   * Renders one card per dataset.
   *
   * @param summaries the datasets of the experiment, in display order
   */
  public void setDatasets(List<DatasetSummary> summaries) {
    cards.removeAll();
    cardByKey.clear();
    // Count-based column layout, following the pinned-project cards: a fixed card size is always a
    // compromise, because one, two and three datasets want different columns. A dedicated class per
    // count lets the row distribute evenly instead of leaving a lone card's width to chance.
    cards.getClassNames().removeIf(name -> name.startsWith("measurement-dataset-count-"));
    // The modifier counts CARDS, not datasets: the "all measurements" card is rendered beside them,
    // so keying the column layout on the dataset count put one more card in the row than the grid had
    // tracks and pushed the last one onto a line of its own.
    cards.addClassName("measurement-dataset-count-%d".formatted(summaries.size() + 1));

    // An experiment always has the "all measurements" view to choose as well as its datasets, so
    // there are always at least two choices and the group is always a choice group. The lone-
    // informational-card case is gone with it: an experiment with one dataset still has "All" beside
    // it, and hiding that option would leave no way to see the whole experiment at once -- which is
    // the gap the All card exists to close.
    cards.getElement().setAttribute("role", "radiogroup");
    cards.getElement().setAttribute("aria-label", "Measurement datasets, or all measurements");

    Div allCard = createAllCard(summaries);
    cardByKey.put(ALL, allCard);
    cards.add(allCard);

    for (DatasetSummary summary : summaries) {
      Div card = createCard(summary);
      cardByKey.put(summary.segment(), card);
      cards.add(card);
    }
    markSelected(selected);
  }

  /**
   * Selects a dataset and notifies listeners.
   *
   * @param segment the dataset to activate
   */
  public void select(MeasurementDatasetSegment segment) {
    if (Objects.equals(segment, selected) && hasSelection) {
      return;
    }
    selected = segment;
    hasSelection = true;
    markSelected(segment);
    fireEvent(new SelectionChanged(this, segment, true));
  }

  /** Selects the "all measurements" view and notifies listeners. */
  public void selectAll() {
    if (hasSelection && selected == null) {
      return;
    }
    selected = null;
    hasSelection = true;
    markSelected(null);
    fireEvent(new SelectionChanged(this, null, true));
  }

  /**
   * Selects a dataset without notifying listeners, for restoring state from the URL.
   *
   * @param segment the dataset to show as active
   */
  public void selectSilently(MeasurementDatasetSegment segment) {
    selected = segment;
    hasSelection = true;
    markSelected(segment);
  }

  /** Restores the "all measurements" view without notifying listeners. */
  public void selectAllSilently() {
    selected = null;
    hasSelection = true;
    markSelected(null);
  }

  /** @return the datasets currently rendered, in display order */
  public List<MeasurementDatasetSegment> getRenderedSegments() {
    return cardByKey.keySet().stream()
        .filter(MeasurementDatasetSegment.class::isInstance)
        .map(MeasurementDatasetSegment.class::cast)
        .toList();
  }

  /** @return the currently active dataset, or {@code null} when "all measurements" is active */
  public MeasurementDatasetSegment selected() {
    return selected;
  }

  /** @return whether the active view is "all measurements" rather than one dataset */
  public boolean allSelected() {
    return hasSelection && selected == null;
  }

  private void markSelected(MeasurementDatasetSegment segment) {
    Object activeKey = segment == null ? ALL : segment;
    cardByKey.forEach((key, card) -> {
      boolean isSelected = key.equals(activeKey);
      card.getElement().setAttribute("aria-checked", String.valueOf(isSelected));
      card.getElement().setAttribute("tabindex", isSelected ? "0" : "-1");
      if (isSelected) {
        card.addClassName("measurement-dataset-card--selected");
      } else {
        card.removeClassName("measurement-dataset-card--selected");
      }
    });
  }

  /**
   * The "all measurements" card: the experiment as a whole, unscoped by dataset.
   *
   * <p>It is a peer of the dataset cards, not a control beside them, because it answers the same
   * question they do -- "what do I have?" -- with the widest possible answer. Without it the only way
   * to see every measurement of an experiment measured several ways was to visit each dataset in turn
   * and add the numbers up, and the <em>Method · Instrument</em> column could never differ within one
   * list, because a single dataset has one of each.</p>
   *
   * <p>It states the experiment's totals, which no dataset card can: it is the sum of them.</p>
   */
  private Div createAllCard(List<DatasetSummary> summaries) {
    Div card = new Div();
    card.addClassName("measurement-dataset-card");
    card.addClassName("measurement-dataset-card--all");
    card.getElement().setAttribute("role", "radio");

    Span label = new Span("All measurements");
    label.addClassName("measurement-dataset-card-label");
    // Says what "all" spans, so the card cannot be mistaken for another dataset: the experiment's
    // datasets, named by count. A single dataset would make this redundant, which is why the count is
    // dropped in that case.
    Span identity = new Span(summaries.size() == 1
        ? "the experiment's only dataset"
        : "across %d datasets".formatted(summaries.size()));
    identity.addClassName("measurement-dataset-card-identity");

    Div headline = new Div(label, identity);
    headline.addClassName("measurement-dataset-card-headline");

    int total = summaries.stream().mapToInt(DatasetSummary::total).sum();
    int available = summaries.stream().mapToInt(DatasetSummary::available).sum();
    Div ring = new Div(new CompletenessRing(available, total,
        "%d of %d measurements have data available".formatted(available, total)));
    ring.addClassName("measurement-dataset-card-ring");
    if (total > 0 && available >= total) {
      // The ring says "complete" on its own once the arc closes and takes the success tone; the
      // class is only a hook for the spoken/hover text now that the halo is gone.
      ring.addClassName("measurement-dataset-card-ring--complete");
      ring.getElement().setAttribute("title",
          "All %d measurements have data available".formatted(total));
    }

    Div headlineRow = new Div(headline, ring);
    headlineRow.addClassName("measurement-dataset-card-headline-row");
    card.add(headlineRow);

    long bytes = summaries.stream().mapToLong(DatasetSummary::bytes).sum();
    Span summary = new Span("%s \u00b7 %s".formatted(
        total == 1 ? "1 measurement" : "%d measurements".formatted(total),
        sizeText(available, bytes)));
    summary.addClassName("measurement-dataset-card-size");
    card.add(summary);

    card.addClickListener(event -> selectAll());
    card.getElement().setAttribute("tabindex", "0");
    card.getElement().executeJs(
        "this.addEventListener('keydown', e => {"
            + "  if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); this.click(); }"
            + "});");
    return card;
  }

  /**
   * Renders one dataset card.
   *
   * @param summary    the dataset to state
   * @param selectable whether the card is a choice among several, or the only dataset there is
   */
  private Div createCard(DatasetSummary summary) {
    Div card = new Div();
    card.addClassName("measurement-dataset-card");
    card.getElement().setAttribute("role", "radio");

    // The card IS a dataset now, so it states the dataset: its omics domain (which groups the cards
    // in a multi-omics experiment) over its method and instrument, which are what tell two datasets of
    // one domain apart. It used to read just "Genomics" and claim to be a dataset, which was the
    // opposite mistake: a domain can back several datasets, and the card could not say which.
    Span label = new Span(summary.segment().domainLabel());
    label.addClassName("measurement-dataset-card-label");

    Span identity = new Span(summary.segment().identityLabel());
    identity.addClassName("measurement-dataset-card-identity");

    Div headline = new Div(label, identity);
    headline.addClassName("measurement-dataset-card-headline");

    // The coverage ring is the same component the project and experiment pages use, so the reader
    // meets one visual language for "how complete is this". It never carries the meaning alone: it
    // exposes role=progressbar with spoken text that states the numbers, and the line beneath the
    // title gives the dataset's count and size.
    Div ring = new Div(new CompletenessRing(summary.available(), summary.total(),
        ringAriaText(summary)));
    ring.addClassName("measurement-dataset-card-ring");
    if (summary.total() > 0 && summary.waiting() == 0) {
      // No halo any more: at a true 100% the closed green arc is itself the marker, and the halo
      // drew a second circle outside it. What remains is the hover text, which states the fact in
      // words the arc cannot.
      ring.addClassName("measurement-dataset-card-ring--complete");
      ring.getElement().setAttribute("title",
          "All %d measurements have data available".formatted(summary.total()));
    }

    Div headlineRow = new Div(headline, ring);
    headlineRow.addClassName("measurement-dataset-card-headline-row");
    card.add(headlineRow);

    // Count and size on one line: they are the two measures of *how big this dataset is*, so they
    // read as one thought and answer the question a reader has before downloading — will it fit on
    // my disk and my link. The count alone, on its own line above the ring, was the denominator of
    // the ring printed separately, which is why it read as unexplained.
    Span quantities = new Span("%s \u00b7 %s".formatted(countText(summary), sizeText(summary)));
    quantities.addClassName("measurement-dataset-card-size");
    card.add(quantities);

    card.addClickListener(event -> select(summary.segment()));
    card.getElement().setAttribute("tabindex", "0");
    // A div gets no native activation, so Enter and Space are wired explicitly. Putting something
    // in the tab order without handling activation is a keyboard trap that only looks operable --
    // the same defect class the project page documented for its clickable cards.
    card.getElement().executeJs(
        "this.addEventListener('keydown', e => {"
            + "  if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); this.click(); }"
            + "});");
    return card;
  }

  /**
   * The spoken form of the ring, so assistive technology gets the same fact the arc shows.
   */
  /** The number of measurements in the dataset, pluralised. */
  static String countText(DatasetSummary summary) {
    return summary.total() == 1
        ? "1 measurement"
        : "%d measurements".formatted(summary.total());
  }

  /** States the size of one dataset's data; see {@link #sizeText(int, long)} for the two cases. */
  static String sizeText(DatasetSummary summary) {
    return sizeText(summary.available(), summary.bytes());
  }

  /**
   * States the size of what has arrived, from the raw counts.
   *
   * <p>Deliberately explicit about the two cases that are not a number: nothing has arrived, and data
   * has arrived whose size the sync never measured. Presenting either as "0 B" would tell a reader
   * their download is free.</p>
   *
   * @param available how many measurements carry data
   * @param bytes     the total size, {@code 0} when unknown
   */
  static String sizeText(int available, long bytes) {
    if (available == 0) {
      return "No data yet";
    }
    if (bytes > 0) {
      return FileSizeFormatter.formatBytesCompact(bytes);
    }
    return "Size not recorded";
  }

  private static String ringAriaText(DatasetSummary summary) {
    if (summary.total() == 0) {
      return "No measurement metadata registered yet";
    }
    if (summary.waiting() == 0) {
      return "All %d measurements have data available".formatted(summary.total());
    }
    return "%d of %d measurements have data available"
        .formatted(summary.available(), summary.total());
  }

  /**
   * States the size of what has arrived. {@code 0} bytes is ambiguous — it means either no data or
   * data whose size the external sync never recorded — so the card must not present it as an empty
   * dataset.
   */
  /**
   * Registers a listener for dataset selection changes.
   *
   * @param listener the listener to add
   * @return a registration to remove it again
   */
  public Registration addSelectionChangedListener(
      ComponentEventListener<SelectionChanged> listener) {
    return addListener(SelectionChanged.class, listener);
  }

  /**
   * One dataset's summary as the selector states it.
   *
   * @param segment   the measurement method
   * @param total     the number of measurements in this dataset
   * @param available the number of entries whose data is available
   * @param bytes     the total size of the data that has arrived, {@code 0} when not recorded
   */
  public record DatasetSummary(MeasurementDatasetSegment segment, int total, int available,
                               long bytes) {

    public DatasetSummary {
      if (total < 0 || available < 0) {
        throw new IllegalArgumentException("counts must not be negative");
      }
    }

    /** The number of entries in this dataset that still wait for their data. */
    public int waiting() {
      return Math.max(0, total - available);
    }

    /**
     * Builds one summary per measurement dataset of the experiment.
     *
     * <p>Read from the same {@link AssayStanding} rows the project summary and the Datasets page use,
     * filtered to this experiment, so the three surfaces cannot disagree about how many datasets the
     * experiment holds or which ones they are. An assay row is one dataset: one experiment, one omics
     * domain, one method, one instrument.</p>
     *
     * @param experimentId the experiment whose datasets to state
     * @param assays        the project's assay standing, in any order
     * @return the experiment's datasets, ordered by domain then method then instrument
     */
    public static List<DatasetSummary> of(String experimentId,
        List<life.qbic.projectmanagement.application.query.AssayStanding> assays) {
      List<DatasetSummary> summaries = new ArrayList<>();
      for (var assay : assays) {
        if (!assay.experimentId().equals(experimentId)) {
          continue;
        }
        summaries.add(new DatasetSummary(
            MeasurementDatasetSegment.of(assay.assayType(), assay.methodLabel(),
                assay.instrumentLabel()),
            assay.measurements(),
            assay.withPrimaryData(),
            assay.primaryDataBytes()));
      }
      summaries.sort(java.util.Comparator
          .comparing((DatasetSummary summary) -> summary.segment().assayType().ordinal())
          .thenComparing(summary -> summary.segment().methodLabel())
          .thenComparing(summary -> summary.segment().instrumentLabelOrDash()));
      return summaries;
    }
  }

  /** Fired when the reader activates a different dataset. */
  public static class SelectionChanged extends ComponentEvent<MeasurementDatasetSelector> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final MeasurementDatasetSegment segment;

    public SelectionChanged(MeasurementDatasetSelector source, MeasurementDatasetSegment segment,
        boolean fromClient) {
      super(source, fromClient);
      this.segment = segment;
    }

    /** @return the dataset that is now active */
    public MeasurementDatasetSegment segment() {
      return segment;
    }
  }
}
