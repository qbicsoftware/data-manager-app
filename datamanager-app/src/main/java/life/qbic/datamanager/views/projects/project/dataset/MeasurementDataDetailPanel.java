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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import life.qbic.projectmanagement.application.query.MeasurementDataRow;

/**
 * The full record of one <b>measurement data</b> entry, rendered beside the list while an entry is
 * open.
 *
 * <p>The list carries identity and status only; everything it omits is read here. This is what
 * makes the type-specific breadth survivable: a genomics measurement has 12 attributes, a
 * proteomics measurement 15 and an immunopeptidomics measurement 23, and a union table of those
 * would be mostly empty. The identity columns stay scannable and the type-specific fields live in
 * this panel.</p>
 *
 * <p>The panel states the two process steps separately, because they are the thing a reader most
 * needs to distinguish: what the measurement <em>is</em> (metadata, registered here) and whether its
 * <em>data</em> has arrived (produced elsewhere, synced back eventually). The data group is never
 * omitted — a missing datum is the normal state after registration and must be stated, not implied
 * by an absent section.</p>
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

  private final H2 title = new H2();
  private final Span subtitle = new Span();
  private final Div body = new Div();

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
   * @param formatter formats an {@link java.time.Instant} in the reader's time zone; the panel does
   *                  not know the client's zone and must not guess it
   */
  public void show(MeasurementDataRow row, Function<java.time.Instant, String> formatter) {
    title.setText(textOrDash(row.measurementName(), row.measurementCode()));
    subtitle.setText(row.measurementCode() + " \u00b7 " + row.measurementType());

    body.removeAll();
    for (DetailGroup group : groupsOf(row, formatter)) {
      if (group.fields().isEmpty()) {
        continue;
      }
      H3 heading = new H3(group.heading());
      heading.addClassName("measurement-data-detail-group-title");
      Div groupDiv = new Div(heading);
      groupDiv.addClassName("measurement-data-detail-group");
      group.fields().forEach(field -> groupDiv.add(fieldRow(field)));
      body.add(groupDiv);
    }
    setVisible(true);
  }

  /** Hides the panel and clears the rendered record, so no value lingers in the DOM. */
  public void close() {
    body.removeAll();
    title.setText("");
    subtitle.setText("");
    setVisible(false);
  }

  /**
   * Builds the grouped record. The data group is emitted for every state, including a waiting
   * entry: "no data yet" is information, not an absence of information.
   */
  private static List<DetailGroup> groupsOf(MeasurementDataRow row,
      Function<java.time.Instant, String> formatter) {
    List<DetailGroup> groups = new ArrayList<>();

    groups.add(new DetailGroup("Measurement identity", List.of(
        field("QBiC Measurement ID", row.measurementCode(), true),
        field("Measurement name", row.measurementName(), false),
        field("Registration date", format(formatter, row.registeredAt()), false))));

    groups.add(new DetailGroup("Sample", List.of(
        field("Sample pool", row.samplePool(), false),
        field("Samples", row.sampleCount() == 1 ? "1 sample" : row.sampleCount() + " samples",
            false))));

    groups.add(new DetailGroup("Organisational provenance", List.of(
        field("Facility", row.facility(), false))));

    groups.add(dataGroup(row, formatter));

    return groups;
  }

  /**
   * The second process step, stated explicitly.
   *
   * <p>When the data has arrived, the facts that make it usable for analysis are given: how many
   * files, how large, which suffixes, and when it landed. When it has not, the panel says so rather
   * than rendering a row of dashes, because a reader must be able to tell "the facility has not
   * uploaded yet" from "we hold a datum of unknown size".</p>
   */
  private static DetailGroup dataGroup(MeasurementDataRow row,
      Function<java.time.Instant, String> formatter) {
    if (!row.hasData()) {
      return new DetailGroup("Data", List.of(
          field("Status", "Waiting \u2014 not uploaded yet", false)));
    }
    List<DetailField> fields = new ArrayList<>();
    fields.add(field("Status", "Available", false));
    // file_count is nullable: absent means the external sync did not report it, not that the
    // datum is empty, so it must not be rendered as 0.
    fields.add(field("Files", row.fileCountOrZero() == 0 && !row.hasRecordedSize()
        ? "Not recorded"
        : String.valueOf(row.fileCountOrZero()), false));
    // 0 bytes is ambiguous: it means either no data or data whose size the external sync never
    // recorded. The panel must not present that as an empty dataset.
    fields.add(field("Total size", row.hasRecordedSize()
        ? life.qbic.application.commons.FileSizeFormatter.formatBytes(row.dataSizeBytes())
        : "Not recorded", false));
    fields.add(field("File suffixes", row.dataFileTypes().isEmpty()
        ? EMPTY_VALUE
        : String.join(", ", row.dataFileTypes().stream().sorted().toList()), false));
    fields.add(field("Uploaded on", format(formatter, row.dataUploadedAt()), false));
    return new DetailGroup("Data", fields);
  }

  private static String format(Function<java.time.Instant, String> formatter,
      java.time.Instant instant) {
    if (instant == null) {
      return EMPTY_VALUE;
    }
    return formatter.apply(instant);
  }

  private static String textOrDash(String preferred, String fallback) {
    if (preferred != null && !preferred.isBlank()) {
      return preferred;
    }
    return fallback == null ? EMPTY_VALUE : fallback;
  }

  private static Div fieldRow(DetailField field) {
    Div row = new Div();
    row.addClassName("measurement-data-detail-field");
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

  private static DetailField field(String label, String value, boolean monospace) {
    return new DetailField(label, value == null || value.isBlank() ? EMPTY_VALUE : value,
        monospace);
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
