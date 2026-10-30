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
import life.qbic.application.commons.FileSizeFormatter;
import life.qbic.datamanager.views.general.CompletenessRing;
import life.qbic.projectmanagement.application.query.AssayType;

/**
 * The experiment's measurement datasets, one card each, above the list.
 *
 * <p>A <b>measurement dataset</b> is the experiment's data for one measurement method, so an
 * experiment has one per method it measured rather than one in total. That makes the datasets the
 * page's primary structure, not a filter: the reader's first question is "what do I have?", not
 * "which subset do I want?". An earlier revision hid the methods in a dropdown, and a reader who
 * did not open it reasonably concluded the page showed all measurements — the grouping was correct
 * and invisible.</p>
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

  private final Div cards = new Div();
  private final Map<MeasurementDatasetSegment, Div> cardBySegment = new LinkedHashMap<>();
  private MeasurementDatasetSegment selected;

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
    cardBySegment.clear();
    // Count-based column layout, following the pinned-project cards: a fixed card size is always a
    // compromise, because one, two and three datasets want different columns. A dedicated class per
    // count lets the row distribute evenly instead of leaving a lone card's width to chance.
    cards.getClassNames().removeIf(name -> name.startsWith("measurement-dataset-count-"));
    cards.addClassName("measurement-dataset-count-%d".formatted(summaries.size()));

    // With one dataset there is nothing to choose between: the card states what the list below
    // contains. Presenting it as a selected radio would tell the reader they made a choice, and
    // that something else was available — which is exactly how a lone highlighted card reads.
    if (summaries.size() == 1) {
      Div summaryCard = createCard(summaries.getFirst(), false);
      cardBySegment.put(summaries.getFirst().segment(), summaryCard);
      cards.add(summaryCard);
      cards.getElement().removeAttribute("role");
      cards.getElement().removeAttribute("aria-label");
      return;
    }

    cards.getElement().setAttribute("role", "radiogroup");
    cards.getElement().setAttribute("aria-label", "Measurement datasets");
    for (DatasetSummary summary : summaries) {
      Div card = createCard(summary, true);
      cardBySegment.put(summary.segment(), card);
      cards.add(card);
    }
    if (selected != null && cardBySegment.containsKey(selected)) {
      markSelected(selected);
    }
  }

  /**
   * Selects a dataset and notifies listeners.
   *
   * @param segment the dataset to activate
   */
  public void select(MeasurementDatasetSegment segment) {
    if (segment == selected) {
      return;
    }
    selected = segment;
    if (segment != null) {
      markSelected(segment);
    }
    fireEvent(new SelectionChanged(this, segment, true));
  }

  /**
   * Selects a dataset without notifying listeners, for restoring state from the URL.
   *
   * @param segment the dataset to show as active
   */
  public void selectSilently(MeasurementDatasetSegment segment) {
    selected = segment;
    if (segment != null) {
      markSelected(segment);
    }
  }

  /** @return the datasets currently rendered, in display order */
  public List<MeasurementDatasetSegment> getRenderedSegments() {
    return List.copyOf(cardBySegment.keySet());
  }

  /** @return the currently active dataset, or {@code null} when none is rendered yet */
  public MeasurementDatasetSegment selected() {
    return selected;
  }

  private void markSelected(MeasurementDatasetSegment segment) {
    cardBySegment.forEach((key, card) -> {
      if (card.getClassNames().contains("measurement-dataset-card--single")) {
        // The lone informational card is never marked as a choice.
        return;
      }
      boolean isSelected = key == segment;
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
   * Renders one dataset card.
   *
   * @param summary    the dataset to state
   * @param selectable whether the card is a choice among several, or the only dataset there is
   */
  private Div createCard(DatasetSummary summary, boolean selectable) {
    Div card = new Div();
    card.addClassName("measurement-dataset-card");
    if (!selectable) {
      // Informational only: no radio role, no focus, no pointer affordance.
      card.addClassName("measurement-dataset-card--single");
    } else {
      card.getElement().setAttribute("role", "radio");
    }

    // Each card *is* a dataset, so it says so: "Genomics" alone reads as a category or a filter,
    // and a reader had no way to know the row of cards was the list of the experiment's datasets.
    Span label = new Span("%s dataset".formatted(summary.segment().label()));
    label.addClassName("measurement-dataset-card-label");

    // Count on the title line, at body weight: two lines for "Genomics" and "6 measurements" made
    // the least interesting number the tallest thing in the card.
    Span count = new Span(summary.total() == 1
        ? "1 measurement"
        : "%d measurements".formatted(summary.total()));
    count.addClassName("measurement-dataset-card-count");

    Div headline = new Div(label, count);
    headline.addClassName("measurement-dataset-card-headline");

    // The coverage ring is the same component the project and experiment pages use, so the reader
    // meets one visual language for "how complete is this". It never carries the meaning alone: the
    // sentence beside it states the count, and the ring exposes role=progressbar with spoken text.
    Div ring = new Div(new CompletenessRing(summary.available(), summary.total(),
        ringAriaText(summary)));
    ring.addClassName("measurement-dataset-card-ring");
    if (summary.total() > 0 && summary.waiting() == 0) {
      // A complete dataset gets a marker beside the ring. A full ring is already a green circle,
      // but at card size that is a faint difference from a 90% arc; the marker makes "everything
      // has arrived" unmistakable without a sentence.
      ring.addClassName("measurement-dataset-card-ring--complete");
      ring.getElement().setAttribute("title",
          "All %d measurements have data available".formatted(summary.total()));
    }

    Div headlineRow = new Div(headline, ring);
    headlineRow.addClassName("measurement-dataset-card-headline-row");
    card.add(headlineRow);

    // The size is stated because it is the fact a reader needs *before* downloading: it decides
    // whether the data fits on their disk and their link. It is not derivable from anything else on
    // the page, so it earns its line — unlike the "N of M waiting" sentence, which only restated
    // the ring.
    Span size = new Span(sizeText(summary));
    size.addClassName("measurement-dataset-card-size");
    card.add(size);

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
  /**
   * States the size of the dataset's data.
   *
   * <p>Deliberately explicit about the two cases that are not a number: a dataset with no data yet
   * has no size, and a dataset whose arrival the sync never measured has a size that is unknown
   * rather than zero. Presenting either as "0 B" would tell a reader their download is free.</p>
   */
  static String sizeText(DatasetSummary summary) {
    if (summary.available() == 0) {
      return "No data yet";
    }
    if (summary.bytes() > 0) {
      return FileSizeFormatter.formatBytes(summary.bytes());
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

    /** Builds the summaries for the datasets an experiment actually has. */
    public static List<DatasetSummary> of(life.qbic.projectmanagement.application.query.ExperimentProgress progress) {
      List<DatasetSummary> summaries = new ArrayList<>();
      for (AssayType assayType : progress.assayTypes()) {
        summaries.add(new DatasetSummary(
            MeasurementDatasetSegment.from(assayType),
            progress.measurementCount(assayType),
            progress.measurementsWithPrimaryData(assayType),
            progress.primaryDataBytes(assayType)));
      }
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
