package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import life.qbic.application.commons.FileSizeFormatter;
import life.qbic.projectmanagement.application.query.MeasuredSample;
import life.qbic.projectmanagement.application.query.MeasurementDataRow;

/**
 * The full record of one <b>measurement data</b> entry, rendered beside the list while an entry is
 * open.
 *
 * <p>The list carries identity and status only; everything it omits is read here. This is what
 * makes the type-specific breadth survivable: a genomics measurement has 12 attributes, a
 * proteomics measurement 15 and an immunopeptidomics measurement 23, and a union table of those
 * would be mostly empty.</p>
 *
 * <p>The panel states the two process steps separately, because they are the thing a reader most
 * needs to distinguish: what the measurement <em>is</em> (metadata, registered here) and whether its
 * <em>data</em> has arrived (produced elsewhere, synced back eventually).</p>
 *
 * <p><b>Layout intent.</b> Fields are stacked label-over-value, deliberately: a two-column
 * key/value table was considered and rejected, because it forces the eye to jump horizontally
 * across a narrow column for every one of up to 23 short facts. The scan instead relies on a
 * spacing rhythm in which a field's label sits tight to its value, consecutive fields are close,
 * and a group boundary is visibly wider than either — plus a hairline between groups. An earlier
 * revision had this inverted (field gap 4× the label gap, group gap only 2× a field gap), which made
 * twelve lines read as one undifferentiated list and was the reported reason the panel was hard to
 * scan.</p>
 *
 * <p>Read-only: editing and deletion stay with the list's bulk actions, so opening an entry can
 * never begin a mutation.</p>
 *
 * @since 1.21.0
 */
public class MeasurementDataDetailPanel extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = -1L;

  private static final String EMPTY_VALUE = "\u2014";

  /** The group whose body is the asynchronously fetched sample listing. */
  private static final String SAMPLES_GROUP_HEADING = "Samples measured";

  private final H2 title = new H2();
  private final Span subtitle = new Span();
  private final Div body = new Div();

  /** Where the sample listing is rendered once it arrives, so the rest of the record is not
   * blocked on a second query. */
  private final Div samplesSlot = new Div();

  public MeasurementDataDetailPanel() {
    addClassName("measurement-data-detail-panel");
    setVisible(false);
    getElement().setAttribute("role", "region");
    getElement().setAttribute("aria-labelledby", "measurement-data-detail-heading");

    title.addClassName("measurement-data-detail-panel-title");
    title.setId("measurement-data-detail-heading");
    subtitle.addClassName("measurement-data-detail-panel-subtitle");

    Div heading = new Div(title, subtitle);
    heading.addClassName("measurement-data-detail-panel-heading");

    Button close = new Button(VaadinIcon.CLOSE.create());
    close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    close.addClassName("measurement-data-detail-panel-close");
    // The glyph is the whole control, so it needs a name and a hit area large enough to use.
    close.getElement().setAttribute("aria-label", "Close measurement details");
    close.addClickListener(event -> fireEvent(new Closed(this, event.isFromClient())));

    Div header = new Div(heading, close);
    header.addClassName("measurement-data-detail-panel-header");

    body.addClassName("measurement-data-detail-panel-body");
    add(header, body);
  }

  /**
   * Renders the given entry and shows the panel.
   *
   * @param row       the entry to show, never {@code null}
   * @param formatter formats an {@link Instant} in the reader's time zone; the panel does not know
   *                  the client's zone and must not guess it
   */
  public void show(MeasurementDataRow row, Function<Instant, String> formatter) {
    // Exactly one accession. It was previously printed three times — as the title fallback, in the
    // subtitle and again as a field — which made the top third of the panel a triple echo.
    title.setText(titleOf(row));
    subtitle.setText(measurementTypeLabel(row));

    body.removeAll();
    for (DetailGroup group : groupsOf(row, formatter)) {
      // The sample group has no fields of its own; its body is the fetched listing, so it must
      // survive this guard.
      if (group.fields().isEmpty() && !SAMPLES_GROUP_HEADING.equals(group.heading())) {
        continue;
      }
      body.add(groupDiv(group));
    }
    setVisible(true);
  }

  /**
   * Renders the samples this measurement measures, once they have been fetched.
   *
   * <p>The listing is fetched when the panel opens rather than carried in the list row: a
   * proteomics measurement can pool fifty or more samples, and a list query carrying them would be
   * both heavy and — through {@code group_concat_max_len} — silently truncated. The count is stated
   * in the heading so a long list never has to be counted by eye.</p>
   *
   * @param samples the samples, in sample-code order; empty when the measurement measures none
   */
  public void showSamples(List<MeasuredSample> samples) {
    samplesSlot.removeAll();
    samplesSlot.addClassName("measurement-data-detail-samples");

    if (samples.isEmpty()) {
      samplesSlot.add(mutedLine("No samples resolved for this measurement."));
      return;
    }

    Span count = new Span(samples.size() == 1
        ? "1 sample"
        : "%d samples".formatted(samples.size()));
    count.addClassName("measurement-data-detail-samples-count");
    samplesSlot.add(count);

    Div list = new Div();
    list.addClassName("measurement-data-detail-samples-list");
    // A list, not a run of text: a pool of fifty is read by scanning, and each entry has two parts
    // (the registered name and the code) that must stay paired.
    for (MeasuredSample sample : samples) {
      Div item = new Div();
      item.addClassName("measurement-data-detail-sample");
      Span label = new Span(sample.sampleLabel() == null || sample.sampleLabel().isBlank()
          ? sample.sampleCode()
          : sample.sampleLabel());
      label.addClassName("measurement-data-detail-sample-label");
      Span code = new Span(sample.sampleCode());
      code.addClassNames("measurement-data-detail-sample-code", "identifier");
      item.add(label, code);
      list.add(item);
    }
    samplesSlot.add(list);
  }

  /** Signals that the sample listing is still being fetched. */
  public void showSamplesLoading() {
    samplesSlot.removeAll();
    Span loading = new Span("Loading samples\u2026");
    loading.addClassName("measurement-data-detail-samples-loading");
    loading.getElement().setAttribute("role", "status");
    samplesSlot.add(loading);
  }

  /** Signals that the sample listing could not be fetched. */
  public void showSamplesFailed() {
    samplesSlot.removeAll();
    samplesSlot.add(mutedLine("The samples of this measurement could not be loaded."));
  }

  /** Hides the panel and clears the rendered record, so no value lingers in the DOM. */
  public void close() {
    body.removeAll();
    samplesSlot.removeAll();
    title.setText("");
    subtitle.setText("");
    setVisible(false);
  }

  /**
   * The panel's title. A measurement's name is optional and often blank; when it is, the code
   * becomes the title and is then <em>not</em> repeated as a field, so the accession appears once.
   */
  private static String titleOf(MeasurementDataRow row) {
    if (hasText(row.measurementName())) {
      return row.measurementName();
    }
    return hasText(row.measurementCode()) ? row.measurementCode() : EMPTY_VALUE;
  }

  private static boolean titleIsTheCode(MeasurementDataRow row) {
    return !hasText(row.measurementName());
  }

  private static String measurementTypeLabel(MeasurementDataRow row) {
    String type = row.measurementType();
    if (!hasText(type)) {
      return "";
    }
    return switch (type) {
      case "GENOMICS" -> "Genomics";
      case "PROTEOMICS" -> "Proteomics";
      case "IMMUNOPEPTIDOMICS" -> "Immunopeptidomics";
      default -> type;
    };
  }

  private static List<DetailGroup> groupsOf(MeasurementDataRow row,
      Function<Instant, String> formatter) {
    List<DetailGroup> groups = new ArrayList<>();

    List<DetailField> identity = new ArrayList<>();
    // Omitted when it is already the title: the accession must appear exactly once.
    if (!titleIsTheCode(row)) {
      identity.add(field("QBiC Measurement ID", row.measurementCode(), true));
    }
    // Named for what it dates: the metadata, not its data. They are different moments, often days
    // apart, and the old label left the reader guessing which one they were looking at.
    identity.add(field("Metadata registered", format(formatter, row.registeredAt()), false));
    groups.add(new DetailGroup("Measurement", identity));

    groups.add(new DetailGroup("Sample", sampleFields(row)));
    // The sample listing replaces this slot once fetched; it is emitted by showSamples.
    groups.add(new DetailGroup(SAMPLES_GROUP_HEADING, List.of()));

    List<DetailField> provenance = new ArrayList<>();
    if (hasText(row.facility())) {
      provenance.add(field("Facility", row.facility(), false));
    }
    groups.add(new DetailGroup("Organisational provenance", provenance));

    groups.add(dataGroup(row, formatter));
    return groups;
  }

  private static List<DetailField> sampleFields(MeasurementDataRow row) {
    List<DetailField> fields = new ArrayList<>();
    // Only a real, user-provided label is shown. samplePool is free text typed by the registrant
    // ("pool group 1"), never a count, so it must not be presented as one.
    if (hasText(row.samplePool()) && row.sampleCount() > 1) {
      fields.add(field("Pool label", row.samplePool(), false));
    }
    fields.add(field("Pooled", row.sampleCount() > 1
        ? "Yes \u2014 %d samples measured as one".formatted(row.sampleCount())
        : "No \u2014 one sample per measurement", false));
    return fields;
  }

  /**
   * The second process step, stated explicitly.
   *
   * <p>When the data has arrived, the facts that make it usable are collapsed into one meta line:
   * four separate label/value pairs for file count, size, suffixes and upload date occupied a third
   * of the panel while carrying a single thought. When it has not arrived, the panel says so rather
   * than rendering a row of dashes.</p>
   */
  private static DetailGroup dataGroup(MeasurementDataRow row, Function<Instant, String> formatter) {
    List<DetailField> fields = new ArrayList<>();
    // The status is a value-only field: the group is already titled "Data", so a field also
    // labelled "Data" rendered as "DATA / Data / Waiting" — the word twice for one fact.
    if (!row.hasData()) {
      fields.add(new DetailField("", "Waiting \u2014 not uploaded yet", false));
      return new DetailGroup("Data", fields);
    }
    fields.add(new DetailField("", "Available", false));
    List<String> facts = new ArrayList<>();
    if (row.fileCountOrZero() > 0) {
      facts.add(row.fileCountOrZero() == 1 ? "1 file" : row.fileCountOrZero() + " files");
    }
    // 0 bytes is ambiguous: either no data or data whose size the sync never recorded. It must
    // never be rendered as an empty dataset.
    facts.add(row.hasRecordedSize()
        ? FileSizeFormatter.formatBytes(row.dataSizeBytes())
        : "size not recorded");
    if (!row.dataFileTypes().isEmpty()) {
      facts.add(String.join(", ", row.dataFileTypes().stream().sorted().toList()));
    }
    if (row.dataUploadedAt() != null) {
      facts.add("data registered " + format(formatter, row.dataUploadedAt()));
    }
    fields.add(new DetailField("", String.join(" \u00b7 ", facts), false));
    return new DetailGroup("Data", fields);
  }

  private Div groupDiv(DetailGroup group) {
    Div groupDiv = new Div();
    groupDiv.addClassName("measurement-data-detail-group");
    // A real heading, so the group is navigable by a screen reader rather than being one more line
    // of text in a flat run.
    H3 heading = new H3(group.heading());
    heading.addClassName("measurement-data-detail-group-title");
    groupDiv.add(heading);

    Div fields = new Div();
    fields.addClassName("measurement-data-detail-fields");
    group.fields().forEach(field -> fields.add(fieldRow(field)));
    if ("Samples measured".equals(group.heading())) {
      fields.add(samplesSlot);
    }
    groupDiv.add(fields);
    return groupDiv;
  }

  private static Div fieldRow(DetailField field) {
    Div row = new Div();
    row.addClassName("measurement-data-detail-field");
    if (field.label().isEmpty()) {
      // A value-only field (the data meta line) has no label to align, so it must not reserve the
      // label line's height.
      row.addClassName("measurement-data-detail-field--valueless-label");
      Span value = new Span(field.value());
      value.addClassName("measurement-data-detail-field-value");
      row.add(value);
      return row;
    }
    Span label = new Span(field.label());
    label.addClassName("measurement-data-detail-field-label");
    Span value = new Span(field.value());
    value.addClassName("measurement-data-detail-field-value");
    if (field.monospace()) {
      value.addClassName("identifier");
    }
    row.add(label, value);
    return row;
  }

  private static Span mutedLine(String text) {
    Span span = new Span(text);
    span.addClassName("measurement-data-detail-muted");
    return span;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String format(Function<Instant, String> formatter, Instant instant) {
    if (instant == null) {
      return EMPTY_VALUE;
    }
    return formatter.apply(instant);
  }

  private static DetailField field(String label, String value, boolean monospace) {
    return new DetailField(label, hasText(value) ? value : EMPTY_VALUE, monospace);
  }

  /**
   * Registers a listener for the panel's close request.
   *
   * @param listener the listener to add
   * @return a registration to remove it again
   */
  public Registration addCloseListener(ComponentEventListener<Closed> listener) {
    return addListener(Closed.class, listener);
  }

  /** Fired when the reader asks to close the panel. */
  public static class Closed extends ComponentEvent<MeasurementDataDetailPanel> {

    @Serial
    private static final long serialVersionUID = 1L;

    public Closed(MeasurementDataDetailPanel source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  private record DetailField(String label, String value, boolean monospace) {
  }

  private record DetailGroup(String heading, List<DetailField> fields) {
  }
}
