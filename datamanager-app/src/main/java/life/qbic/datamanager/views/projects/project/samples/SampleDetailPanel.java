package life.qbic.datamanager.views.projects.project.samples;

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
import java.util.List;
import java.util.function.Function;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Condition;

/**
 * <b>Sample Detail Panel</b>
 *
 * <p>The full record of a single sample, rendered beside the sample grid while a record is open.
 * The grid carries identity only (sample ID, name, condition, batch); everything the grid omits --
 * origin ontologies, laboratory metadata, the per-variable condition, and the audit timestamps --
 * is read here.</p>
 *
 * <p>The panel is a read-only surface: editing and deletion stay with the grid's bulk actions, so
 * opening a record can never begin a mutation. It is deliberately not modal -- the list stays
 * visible and scannable while the record is read, and no focus trap is needed for a surface that
 * cannot be edited.</p>
 *
 * <p>All fields are resolved from the already-loaded {@link SamplePreview}, so opening a record
 * requires no additional fetch.</p>
 *
 * @since 1.20.0
 */
public class SampleDetailPanel extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = 6910418293744171881L;

  private static final String EMPTY_VALUE = "\u2014";

  private final H2 title = new H2();
  private final Span subtitle = new Span();
  private final Div body = new Div();

  public SampleDetailPanel() {
    addClassName("sample-detail-panel");
    setVisible(false);
    getElement().setAttribute("role", "region");
    getElement().setAttribute("aria-labelledby", "sample-detail-panel-heading");

    title.addClassName("sample-detail-panel-title");
    title.setId("sample-detail-panel-heading");

    subtitle.addClassName("sample-detail-panel-subtitle");

    Div heading = new Div(title, subtitle);
    heading.addClassName("sample-detail-panel-heading");

    Button close = new Button(VaadinIcon.CLOSE.create());
    close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    close.addClassName("sample-detail-panel-close");
    close.getElement().setAttribute("aria-label", "Close sample details");
    close.addClickListener(event -> fireEvent(new Closed(this, event.isFromClient())));

    Div header = new Div(heading, close);
    header.addClassName("sample-detail-panel-header");

    body.addClassName("sample-detail-panel-body");

    add(header, body);
  }

  /**
   * Renders the given sample and shows the panel.
   *
   * @param preview       the sample to show, resolved from the already-loaded page of previews
   * @param timeFormatter formats the audit timestamps in the viewer's own time zone
   */
  public void show(SamplePreview preview, Function<Instant, String> timeFormatter) {
    title.setText(preview.sampleName());
    subtitle.setText(preview.sampleCode());

    body.removeAll();
    body.add(
        group("Sample identity", List.of(
            new DetailField("QBiC sample ID", preview.sampleCode(), null, true),
            new DetailField("Sample name", preview.sampleName(), null, false),
            new DetailField("Registration time", timeFormatter.apply(preview.registrationTime()),
                null, false),
            new DetailField("Modification time", timeFormatter.apply(preview.lastModified()),
                null, false))),
        group("Condition", conditionFields(preview)),
        group("Sample origin", List.of(
            ontologyField("Species", preview.species()),
            ontologyField("Specimen", preview.specimen()),
            ontologyField("Analyte", preview.analyte()))),
        group("Laboratory", List.of(
            new DetailField("Batch", preview.batchLabel(), null, false),
            new DetailField("Biological replicate", preview.biologicalReplicate(), null, false),
            new DetailField("Analysis to be performed", preview.analysisMethod().label(), null,
                false))),
        group("Notes", List.of(
            new DetailField("Comment", preview.comment(), null, false))));

    setVisible(true);
    // Move focus into the panel so keyboard users land on the record they just opened. The heading
    // is not normally focusable, so it is made programmatically focusable for this one purpose.
    title.getElement().setAttribute("tabindex", "-1");
    getUI().ifPresent(ui -> title.getElement().executeJs("this.focus()"));
  }

  /**
   * Hides the panel and drops the rendered record, so no stale sample stays in the DOM.
   */
  public void close() {
    body.removeAll();
    // The heading is cleared too: leaving the last sample's name and code in the DOM would keep
    // them reachable to assistive technology and to anything that reads the panel's text, even
    // while the panel is hidden.
    title.setText("");
    subtitle.setText("");
    setVisible(false);
  }

  private static List<DetailField> conditionFields(SamplePreview preview) {
    Condition condition = preview.experimentalGroup().condition();
    List<DetailField> fields = condition.getVariableLevels().stream()
        .map(level -> new DetailField(level.variableName().value(),
            ("%s %s".formatted(level.experimentalValue().value(),
                level.experimentalValue().unit().orElse(""))).trim(), null, false))
        .toList();
    // An experiment always has at least one variable once a group exists, but a defensive empty
    // list keeps the group from rendering as a heading with nothing under it.
    return fields.isEmpty() ? List.of(new DetailField("Condition", null, null, false)) : fields;
  }

  private static DetailField ontologyField(String label, OntologyTerm term) {
    // The label is the readable value; the ontology term's formatted form carries the CURIE and
    // version, which belongs in the tooltip rather than in the cell.
    return new DetailField(label, term.getLabel(), term.formatted(), false);
  }

  private static Div group(String name, List<DetailField> fields) {
    H3 heading = new H3(name);
    heading.addClassName("sample-detail-group-title");
    Div group = new Div(heading);
    group.addClassName("sample-detail-group");
    fields.forEach(field -> group.add(fieldRow(field)));
    return group;
  }

  private static Div fieldRow(DetailField field) {
    Span label = new Span(field.label());
    label.addClassName("sample-detail-field-label");

    Span value = new Span(
        field.value() == null || field.value().isBlank() ? EMPTY_VALUE : field.value());
    value.addClassName("sample-detail-field-value");
    if (field.monospace()) {
      value.addClassName("identifier");
    }
    if (field.tooltip() != null && !field.tooltip().isBlank()) {
      value.getElement().setAttribute("title", field.tooltip());
    }

    Div row = new Div(label, value);
    row.addClassName("sample-detail-field");
    return row;
  }

  public Registration addCloseListener(ComponentEventListener<Closed> listener) {
    return addListener(Closed.class, listener);
  }

  /**
   * A single label/value pair in the panel.
   *
   * @param label      the field label
   * @param value      the display value; {@code null} or blank renders as an em dash
   * @param tooltip    optional supplementary text (e.g. an ontology CURIE), may be {@code null}
   * @param monospace  whether the value is an identifier that should render monospace
   */
  private record DetailField(String label, String value, String tooltip, boolean monospace) {

  }

  /**
   * <b>Closed</b>
   *
   * <p>Indicates that the user dismissed the detail panel, so the owner can restore focus and clear
   * any open-record state.</p>
   */
  public static class Closed extends ComponentEvent<SampleDetailPanel> {

    @Serial
    private static final long serialVersionUID = 4028417105723690810L;

    public Closed(SampleDetailPanel source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
