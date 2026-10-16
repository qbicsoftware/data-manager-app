package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.RecentMeasurement;
import life.qbic.projectmanagement.application.query.RecentSample;
import reactor.core.publisher.Mono;

/**
 * Experiments section of the project page — the prototype's experiment entries.
 *
 * <p>Each entry is {@code name + domain chips + attention}, followed by two snapshot panels: the
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

  public void setProjectId(String projectId) {
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    reload();
  }

  /**
   * Registers a listener for the request to create a new experiment. The section owns the action;
   * the owning page owns the creation workflow.
   */
  public Registration addAddExperimentListener(
      ComponentEventListener<AddExperimentRequestedEvent> listener) {
    return addListener(AddExperimentRequestedEvent.class, listener);
  }

  private void reload() {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    content.removeAll();
    content.add(message("Loading experiments …"));
    Mono<Snapshot> snapshot = Mono.zip(
            projectQueryService.getExperimentProgress(projectId).collectList(),
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
    content.add(header);

    if (snapshot.progress().isEmpty()) {
      content.add(onboarding());
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
    Span name = new Span(experiment.experimentName());
    name.addClassName("project-exp-name");
    Div facets = new Div();
    facets.addClassName("project-exp-facets");
    domains(experiment).forEach(domain -> facets.add(chip(domain, "project-exp-chip--domain")));
    // No status chip. Every state it could report -- "Not started" and "Needs attention" -- is
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

  private Div samplesPanel(ExperimentProgress experiment, List<RecentSample> samples) {
    boolean started = experiment.sampleCount() > 0;
    String status = samples.isEmpty()
        ? "Nothing registered yet"
        : "Registered · " + relativeTime(samples.getFirst().changed());
    List<List<String>> rows = samples.stream()
        .map(sample -> List.of(sample.sampleCode(), sample.sampleName(),
            relativeTime(sample.changed())))
        .toList();
    return snapshotPanel("Samples", "samples", experiment.sampleCount(),
        started ? "Registered" : "Not started", started ? "done" : "active",
        status, new String[]{"Sample ID", "Sample name", "Changed"}, rows,
        "No samples to show yet",
        () -> navigate(SampleInformationMain.class, experiment.experimentId()));
  }

  private Div datasetPanel(ExperimentProgress experiment, List<RecentMeasurement> measurements) {
    int total = experiment.measurementCount();
    int withData = experiment.measurementsWithPrimaryData();
    int missing = experiment.measurementsWithoutPrimaryData();
    boolean hasSamples = experiment.sampleCount() > 0;

    String state;
    String tone;
    if (!hasSamples) {
      state = "Locked";
      tone = "locked";
    } else if (total == 0) {
      state = "Ready to register";
      tone = "active";
    } else if (missing > 0) {
      state = "Needs attention";
      tone = "attention";
    } else {
      state = "Complete";
      tone = "done";
    }
    String status = total == 0
        ? "Nothing registered yet"
        : withData + " of " + total + " with primary data · " + relativeTime(newest(measurements));

    List<List<String>> rows = measurements.stream()
        .map(measurement -> List.of(measurement.measurementCode(), primaryDataLabel(measurement),
            relativeTime(measurement.changed())))
        .toList();
    return snapshotPanel("Dataset", "measurements", total, state, tone, status,
        new String[]{"Measurement ID", "Primary data", "Changed"}, rows,
        "No measurements to show yet",
        () -> navigate(MeasurementMain.class, experiment.experimentId()));
  }

  private Div snapshotPanel(String label, String unit, int count, String state, String tone,
      String status, String[] columns, List<List<String>> rows, String emptyRow, Runnable open) {
    Div panel = new Div();
    panel.addClassName("project-exp-panel");

    Div head = new Div();
    head.addClassName("project-exp-panel-head");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("project-exp-panel-label");
    head.add(surfaceIcon(label), labelSpan);
    Span stateTag = new Span(state);
    stateTag.addClassNames("project-exp-panel-state", "project-exp-panel-state--" + tone);
    head.add(stateTag);
    Span openHint = new Span("Open " + label.toLowerCase());
    openHint.addClassName("project-exp-panel-open");
    head.add(openHint);
    panel.add(head);

    Div metric = new Div();
    metric.addClassName("project-exp-panel-metric");
    Span value = new Span(Integer.toString(count));
    value.addClassName("project-exp-panel-value");
    Span unitSpan = new Span(unit);
    unitSpan.addClassName("project-exp-panel-unit");
    metric.add(value, unitSpan);
    panel.add(metric);

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

    Utility.activateOnClickAndKeys(panel, "button", open);
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
        // ID -- so it takes the same treatment as the project code above.
        span.addClassName("identifier");
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

  private void navigate(Class<? extends Component> target, String experimentId) {
    RouteParameters parameters = new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, projectId),
        new RouteParam(EXPERIMENT_ID_PARAMETER, experimentId));
    getUI().ifPresent(ui -> ui.navigate(target, parameters));
  }

  private Div onboarding() {
    Div hero = new Div();
    hero.addClassName("project-experiments-onboarding");
    Span title = new Span("Create your first experiment");
    title.addClassName("project-experiments-onboarding-title");
    Span text = new Span("Experiments are where samples, measurements and primary data live — "
        + "all of it starts with one experiment.");
    text.addClassName("project-experiments-onboarding-text");
    hero.add(title, text, addExperimentButton());
    return hero;
  }

  private Button addExperimentButton() {
    Button add = new Button("Add experiment");
    add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    add.addClassName("project-experiments-add");
    add.addClickListener(click -> fireEvent(new AddExperimentRequestedEvent(this,
        click.isFromClient())));
    return add;
  }

  private static List<String> domains(ExperimentProgress experiment) {
    List<String> domains = new ArrayList<>();
    if (experiment.ngsMeasurementCount() > 0) {
      domains.add("Genomics");
    }
    if (experiment.pxpMeasurementCount() > 0) {
      domains.add("Proteomics");
    }
    if (experiment.ipMeasurementCount() > 0) {
      domains.add("Immunopeptidomics");
    }
    return domains;
  }

  private static String primaryDataLabel(RecentMeasurement measurement) {
    if (!measurement.hasPrimaryData()) {
      return "Not attached";
    }
    return measurement.primaryDataFiles() + " files · " + humanReadableBytes(
        measurement.primaryDataBytes() == null ? 0L : measurement.primaryDataBytes());
  }

  private static String humanReadableBytes(long bytes) {
    if (bytes >= 1_000_000_000L) {
      return "%.1f GB".formatted(bytes / 1_000_000_000.0);
    }
    if (bytes >= 1_000_000L) {
      return "%.1f MB".formatted(bytes / 1_000_000.0);
    }
    if (bytes >= 1_000L) {
      return "%.1f KB".formatted(bytes / 1_000.0);
    }
    return bytes + " B";
  }

  private static Instant newest(List<RecentMeasurement> measurements) {
    return measurements.stream()
        .map(RecentMeasurement::changed)
        .filter(java.util.Objects::nonNull)
        .max(Instant::compareTo)
        .orElse(null);
  }

  private static String relativeTime(Instant instant) {
    if (instant == null) {
      return "unknown";
    }
    long days = Duration.between(instant, Instant.now()).toDays();
    if (days <= 0) {
      return "just now";
    }
    return days + (days == 1 ? " day ago" : " days ago");
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
