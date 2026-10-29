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
    for (DatasetSummary summary : summaries) {
      Div card = createCard(summary);
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

  private Div createCard(DatasetSummary summary) {
    Div card = new Div();
    card.addClassName("measurement-dataset-card");
    card.getElement().setAttribute("role", "radio");

    Span label = new Span(summary.segment().label());
    label.addClassName("measurement-dataset-card-label");

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

    Div headlineRow = new Div(headline, ring);
    headlineRow.addClassName("measurement-dataset-card-headline-row");
    card.add(headlineRow);

    // The facts are stated as text, never as colour alone, so a reader who does not perceive the
    // tint still reads the state.
    Span coverage = new Span(coverageText(summary));
    coverage.addClassName("measurement-dataset-card-coverage");
    if (summary.waiting() > 0) {
      coverage.addClassName("measurement-dataset-card-coverage--outstanding");
    }
    card.add(coverage);

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

  private static String coverageText(DatasetSummary summary) {
    if (summary.total() == 0) {
      return "No measurement metadata registered yet";
    }
    if (summary.waiting() == 0) {
      return "Data available for all %d".formatted(summary.total());
    }
    if (summary.waiting() == summary.total()) {
      return summary.total() == 1
          ? "Waiting for its data"
          : "Waiting for their data";
    }
    return "%d of %d waiting for data".formatted(summary.waiting(), summary.total());
  }

  /**
   * States the size of what has arrived. {@code 0} bytes is ambiguous — it means either no data or
   * data whose size the external sync never recorded — so the card must not present it as an empty
   * dataset.
   */
  private static String sizeText(DatasetSummary summary) {
    if (summary.available() == 0) {
      return "No data attached yet";
    }
    if (summary.bytes() > 0) {
      return FileSizeFormatter.formatBytes(summary.bytes());
    }
    return "Size not recorded";
  }

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
