package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.MeasurementType;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.CreateExperimentMain;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.AssayType;
import life.qbic.projectmanagement.application.query.DataState;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.RecentMeasurement;
import life.qbic.projectmanagement.application.query.RecentSample;
import reactor.core.publisher.Mono;

/**
 * Experiments section of the project page — the prototype's experiment entries.
 *
 * <p>Each entry is {@code name + domain chips + not-complete}, followed by two snapshot panels: the
 * newest samples and the newest measurements (with their primary-data status). The panels are the
 * way into the full views and state the same numbers the progress section does, so the entry does
 * not repeat them as separate counts.</p>
 *
 * <p>Snapshot rows come from the {@code experiment_recent_sample} / {@code experiment_recent_measurement}
 * read models through {@link ProjectQueryService}.</p>
 *
 * @since 1.19.0
 */
public class ProjectExperimentsSection extends Div {

  private static final String PROJECT_ID_PARAMETER = "projectId";
  private static final String EXPERIMENT_ID_PARAMETER = "experimentId";

  private final transient ProjectQueryService projectQueryService;
  private final Div content = new Div();
  private String projectId;

  public ProjectExperimentsSection(ProjectQueryService projectQueryService) {
    this.projectQueryService = requireNonNull(projectQueryService,
        "projectQueryService must not be null");
    addClassName("project-experiments-section");
    content.addClassName("project-experiments-content");
    add(content);
  }

  public void setProjectId(String projectId, Mono<List<ExperimentProgress>> progress) {
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    reload(requireNonNull(progress, "progress must not be null"));
  }

  /**
   * Registers a listener for the request to create a new experiment. The section owns the action;
   * the owning page owns the creation workflow.
   */
  public Registration addAddExperimentListener(
      ComponentEventListener<AddExperimentRequestedEvent> listener) {
    return addListener(AddExperimentRequestedEvent.class, listener);
  }

  private void reload(Mono<List<ExperimentProgress>> progress) {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    content.removeAll();
    content.add(message("Loading experiments …"));
    // `progress` is the page's single shared stream (cached by the parent); the recent rows are
    // specific to this section.
    Mono<Snapshot> snapshot = Mono.zip(
            progress,
            projectQueryService.getRecentSamples(projectId).collectList(),
            projectQueryService.getRecentMeasurements(projectId).collectList())
        .map(values -> new Snapshot(values.getT1(), values.getT2(), values.getT3()));
    ReactiveUiBridge.onUi(ui, snapshot, this::render, error -> {
      content.removeAll();
      content.add(message("Experiments could not be loaded."));
    });
  }

  private void render(Snapshot snapshot) {
    content.removeAll();
    Div header = new Div(heading("Experiments"));
    header.addClassName("project-experiments-header");
    // Offer creation where the experiments live, but only once there is a list to add to: with no
    // experiments the progress band above carries the call to action and a second button here
    // would repeat the same instruction in the same viewport.
    if (!snapshot.progress().isEmpty()) {
      header.add(createExperimentButton());
    }    content.add(header);

    if (snapshot.progress().isEmpty()) {
      // No onboarding hero: the progress band above already names the absence and carries the
      // call to action, and a second copy of the same button below it only split the attention.
      content.add(emptyExperiments());
      return;
    }
    Map<String, List<RecentSample>> samplesByExperiment = snapshot.samples().stream()
        .collect(Collectors.groupingBy(RecentSample::experimentId));
    Map<String, List<RecentMeasurement>> measurementsByExperiment = snapshot.measurements().stream()
        .collect(Collectors.groupingBy(RecentMeasurement::experimentId));

    Div cards = new Div();
    cards.addClassName("project-experiments-cards");
    for (ExperimentProgress experiment : snapshot.progress()) {
      cards.add(experimentCard(experiment,
          samplesByExperiment.getOrDefault(experiment.experimentId(), List.of()),
          measurementsByExperiment.getOrDefault(experiment.experimentId(), List.of())));
    }
    content.add(cards);
  }

  private Div experimentCard(ExperimentProgress experiment, List<RecentSample> samples,
      List<RecentMeasurement> measurements) {
    Div card = new Div();
    card.addClassName("project-experiment-card");

    Div identity = new Div();
    identity.addClassName("project-exp-identity");
    // The experiment's identity is the way into its overview; the snapshot panels open their own
    // surfaces (project owner, 2026-10-19).
    Anchor name = Utility.textLink(experiment.experimentName(),
        AppRoutes.ProjectRoutes.EXPERIMENT.formatted(projectId, experiment.experimentId()),
        "project-exp-name");
    Div facets = new Div();
    facets.addClassName("project-exp-facets");
    domains(experiment).forEach(domain -> facets.add(chip(domain, "project-exp-chip--domain")));
    // No status chip. Every state it could report -- "Not started" and "Not complete yet" -- is
    // already tagged on the panel that owns it, one line below, beside the count and the route
    // into that surface. Stating it here too said the same thing twice in one band, and said it
    // more weakly: nothing here named where the work actually is.
    identity.add(name, facets);
    card.add(identity);

    Div panels = new Div();
    panels.addClassName("project-exp-snapshot");
    panels.add(samplesPanel(experiment, samples));
    panels.add(datasetPanel(experiment, measurements));
    card.add(panels);
    return card;
  }

  /**
   * The experiment's newest samples.
   *
   * <p>The panel previews <em>recency</em>, which is the decision taken for both panels: the three
   * rows are the newest, "Changed" is the column that carries the meaning, and the header band
   * offers the way to the full list. The status line states the state once and then the age -- it
   * used to repeat the tag's own word ("Registered" twice in two bands) which spent a band saying
   * nothing.</p>
   */
  private Div samplesPanel(ExperimentProgress experiment, List<RecentSample> samples) {
    boolean started = experiment.sampleCount() > 0;
    String status = samples.isEmpty()
        ? "No samples registered yet"
        : "Newest sample " + Utility.relativeTime(samples.getFirst().changed());
    List<List<String>> rows = samples.stream()
        .map(sample -> List.of(sample.sampleCode(), sample.sampleName(),
            Utility.relativeTime(sample.changed())))
        .toList();
    // "Registered" was outside the declared vocabulary, and it was green -- a success colour for a
    // stage that is done but whose downstream data is not. Samples are either there or not, and that
    // is what Complete/Waiting already say.
    return snapshotPanel("Samples", "samples", experiment.sampleCount(),
        started ? DataStateLabel.word(DataState.COMPLETE) : "Not started",
        started ? "done" : "active",
        status, null, new String[]{"Sample ID", "Sample name", "Changed"}, rows,
        "No samples to show yet",
        started ? "All " + experiment.sampleCount() + " samples" : "",
        AppRoutes.ProjectRoutes.SAMPLES, experiment.experimentId());
  }

  /**
   * The experiment's measurement datasets, one row per analysis method.
   *
   * <p><b>Why the rows are omics domains and not datasets.</b> A measurement dataset is one
   * experiment for one measurement method, and the model is 1:n -- an experiment can hold more than
   * one dataset of the same method. The read model cannot separate them yet, and it cannot even
   * separate methods: it is keyed by {@code (experiment, assayType)}, where the assay type is the
   * <em>domain</em>. Listing measurement IDs instead -- what this panel used to do -- was worse,
   * because a measurement is a <em>member</em> of a dataset and says nothing about which dataset it
   * belongs to or how that dataset is doing.</p>
   *
   * <p>So the panel states the grain it can back: the project's <em>omics domains</em> (genomics,
   * proteomics, immunopeptidomics), how many of each domain's measurements carry data, and whether
   * that domain is fully available. Genomics is a domain, not a method -- WES, RNA-Seq and 16S are
   * all genomics -- and the read model exposes no finer grouping, so the panel names the domain and
   * does not claim to list datasets or methods.</p>
   *
   * <p>Splitting by analysis method, and giving each dataset an identity, is tracked in #1660.</p>
   */
  private Div datasetPanel(ExperimentProgress experiment, List<RecentMeasurement> measurements) {
    int total = experiment.measurementCount();
    boolean hasSamples = experiment.sampleCount() > 0;

    // The experiment's state is the aggregate of its datasets: complete only if every method is.
    DataState dataState = DataState.of(experiment.measurementsWithPrimaryData(), total);
    String state;
    String tone;
    if (!hasSamples) {
      state = "Locked";
      tone = "locked";
    } else if (total == 0) {
      state = "Ready to register";
      tone = "active";
    } else {
      state = DataStateLabel.word(dataState);
      tone = switch (dataState) {
        case COMPLETE -> "done";
        case PARTIAL -> "partial";
        case WAITING, AT_RISK -> "waiting";
      };
    }

    // Newest data arrival across the experiment's datasets. Still arrival rather than preparation --
    // that distinction needs dataset identity (#1660) -- but stated for what it is.
    String status = total == 0
        ? "No measurements registered yet"
        : "Newest data " + Utility.relativeTime(newest(measurements));

    List<AssayType> domains = experiment.assayTypes();
    // One row per OMICS DOMAIN, not per analysis method. The values come from ExperimentProgress'
    // assayTypes(), which is the domain -- "Genomics" covers WES, RNA-Seq, 16S and more. The column
    // header used to say "Analysis method", which was simply wrong: it named a grain the data does
    // not expose. The row leads with the domain because that is the finest grouping available.
    List<List<String>> rows = domains.stream()
        .map(domain -> List.of(
            MeasurementType.from(domain).getType(),
            "%d of %d".formatted(experiment.measurementsWithPrimaryData(domain),
                experiment.measurementCount(domain)),
            DataStateLabel.word(DataState.of(experiment.measurementsWithPrimaryData(domain),
                experiment.measurementCount(domain)))))
        .toList();

    // The metric counts DOMAINS, and says so. Genomics can back several measurement datasets, so
    // "3 measurement datasets" would be the same overclaim the panel made when it listed measurement
    // IDs. The qualifier names the grain too.
    long domainsMissingData = domains.stream()
        .filter(d -> experiment.measurementsWithoutPrimaryData(d) > 0)
        .count();
    return snapshotPanel("Measurement datasets",
        "omics domains", domains.size(), state, tone, status,
        total == 0 ? null
            : "%d of %d %s need primary data".formatted(domainsMissingData, domains.size(),
                domains.size() == 1 ? "domain" : "domains"),
        new String[]{"Omics domain", "Measurements with data", "Data availability"}, rows,
        "No measurements to show yet",
        total > 0 ? "All " + total + " measurements" : "",
        AppRoutes.ProjectRoutes.MEASUREMENT_DATASET, experiment.experimentId());
  }

  private Div snapshotPanel(String label, String unit, int count, String state, String tone,
      String status, String qualifier, String[] columns,
      List<List<String>> rows, String emptyRow, String allRowsText,
      String openHref, String experimentId) {
    Div panel = new Div();
    panel.addClassName("project-exp-panel");

    // Band 1: what this panel is, its state, and the way in.
    Div head = new Div();
    head.addClassName("project-exp-panel-head");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("project-exp-panel-label");
    head.add(surfaceIcon(label), labelSpan);
    Span stateTag = new Span(state);
    stateTag.addClassNames("project-exp-panel-state", "project-exp-panel-state--" + tone);
    head.add(stateTag);
    // A real anchor, not a styled Span. The span looked like a link, was not focusable, could not be
    // opened in a new tab and was not announced as a link -- and it sat inside a container that was
    // itself a click target, so the reader could not tell which click did what.
    Anchor openHint = Utility.textLink("Open " + label.toLowerCase(),
        openHref.formatted(projectId, experimentId), "project-exp-panel-open");
    head.add(openHint);
    panel.add(head);

    // Band 2: a fixed two-line slot. Line one is the count; line two is the qualifier and exists even
    // when empty, because its reserved height is what holds the status line and table on one baseline
    // across the pair. A content-sized band let a wrapping qualifier push everything below it a line
    // down -- the defect banding was introduced to remove.
    Div metric = new Div();
    metric.addClassName("project-exp-panel-metric");
    Div countLine = new Div();
    countLine.addClassName("project-exp-panel-count-line");
    Span value = new Span(Integer.toString(count));
    value.addClassName("project-exp-panel-value");
    Span unitSpan = new Span(unit);
    unitSpan.addClassName("project-exp-panel-unit");
    countLine.add(value, unitSpan);
    metric.add(countLine);

    Span qualifierSpan = new Span(qualifier == null ? "" : qualifier);
    qualifierSpan.addClassName("project-exp-panel-qualifier");
    metric.add(qualifierSpan);
    panel.add(metric);

    // Band 3: the state in words. One line, so both panels keep the same rhythm.
    Span statusSpan = new Span(status);
    statusSpan.addClassName("project-exp-panel-status");
    panel.add(statusSpan);

    Div list = new Div();
    list.addClassName("project-exp-panel-rows");
    list.add(row(columns, true));
    if (rows.isEmpty()) {
      list.add(placeholderRow(emptyRow));
    } else {
      rows.forEach(cells -> list.add(row(cells.toArray(new String[0]), false)));
    }
    panel.add(list);

    // The panel previews three records, so it has to say so and offer the rest. In the table's header
    // row it read as a fourth column whose cells were empty; as a footer below a hairline it reads as
    // the way out of the preview.
    if (allRowsText != null && !allRowsText.isBlank()) {
      Div footer = new Div();
      footer.addClassName("project-exp-panel-footer");
      footer.add(Utility.textLink(allRowsText + " \u2192",
          openHref.formatted(projectId, experimentId), "project-exp-panel-all"));
      panel.add(footer);
    }

    // The panel is NOT a click target. Its one destination is the "Open …" link above, and a
    // container that is also a link would announce as a button, lose the native <a> affordances
    // (middle-click, copy link address) and leave the reader guessing which click navigates.
    return panel;
  }

  /**
   * The decorative glyph that maps a panel to its subject at a glance: Samples is a flask,
   * Dataset a database.
   *
   * <p>Decoration only. The label already names the surface, so the glyph is redundant
   * reinforcement for people who scan shapes, and it must never become the only carrier of
   * meaning -- hence {@code aria-hidden}. Mirrors the prototype's {@code surfaceGlyph}.</p>
   */
  private static Icon surfaceIcon(String label) {
    Icon glyph = switch (label) {
      case "Samples" -> VaadinIcon.FLASK.create();
      case "Dataset" -> VaadinIcon.DATABASE.create();
      default -> VaadinIcon.RECORDS.create();
    };
    glyph.addClassName("project-exp-panel-icon");
    glyph.getElement().setAttribute("aria-hidden", "true");
    return glyph;
  }

  private static Div row(String[] cells, boolean header) {
    Div row = new Div();
    row.addClassName("project-exp-panel-row");
    for (int index = 0; index < cells.length; index++) {
      Span span = new Span(cells[index]);
      if (header) {
        span.addClassName("project-exp-panel-cell-header");
      } else if (index == 0) {
        // The first column is the record's identifier in both panels -- Sample ID, Measurement
        // ID -- so it takes the same treatment as the project code above. The cell truncates rather
        // than wrapping mid-code, so the untruncated value has to stay reachable: a measurement ID
        // runs to 24 characters and is the thing a reader copies.
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
    row.addClassName("project-exp-panel-row");
    row.add(new Span(text));
    return row;
  }

  private RouteParameters experimentRouteParameters(String experimentId) {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, projectId),
        new RouteParam(EXPERIMENT_ID_PARAMETER, experimentId));
  }

  /**
   * The experiments list when there is nothing in it. It does not repeat the progress band's primary
   * button, but it is not dead text either: the sentence names the action it is asking for, so it
   * carries a link to the same route. A reader who has scrolled to the section and started reading
   * it should not have to scroll back up to act.
   */
  private Div emptyExperiments() {
    Div empty = new Div();
    empty.addClassName("project-experiments-empty");
    Anchor create = Utility.textLink("Create experiment",
        AppRoutes.ProjectRoutes.EXPERIMENT_CREATION.formatted(projectId),
        "project-experiments-empty-action");
    Span text = new Span("No experiments yet. ");
    text.addClassName("project-experiments-empty-text");
    empty.add(text, create);
    return empty;
  }

  private Button createExperimentButton() {
    Button create = new Button("Create experiment");
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    create.addClassName("project-experiments-add");
    create.addClickListener(click -> fireEvent(new AddExperimentRequestedEvent(this,
        click.isFromClient())));
    return create;
  }

  private static List<String> domains(ExperimentProgress experiment) {
    return experiment.assayTypes().stream()
        .map(type -> MeasurementType.from(type).getType())
        .toList();
  }

  private static Instant newest(List<RecentMeasurement> measurements) {
    return measurements.stream()
        .map(RecentMeasurement::changed)
        .filter(java.util.Objects::nonNull)
        .max(Instant::compareTo)
        .orElse(null);
  }

  private static Span chip(String text, String modifier) {
    Span chip = new Span(text);
    chip.addClassNames("project-exp-chip", modifier);
    return chip;
  }

  private static Span heading(String text) {
    Span heading = new Span(text);
    heading.addClassName("project-experiments-heading");
    return heading;
  }

  private static Span message(String text) {
    Span message = new Span(text);
    message.addClassName("project-experiments-message");
    return message;
  }

  /** The section's data: per-experiment progress plus the newest rows for the snapshot panels. */
  private record Snapshot(List<ExperimentProgress> progress, List<RecentSample> samples,
                          List<RecentMeasurement> measurements) {

  }

  /** Fired when the user asks to create a new experiment. */
  public static class AddExperimentRequestedEvent extends
      ComponentEvent<ProjectExperimentsSection> {

    @Serial
    private static final long serialVersionUID = 1L;

    public AddExperimentRequestedEvent(ProjectExperimentsSection source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
