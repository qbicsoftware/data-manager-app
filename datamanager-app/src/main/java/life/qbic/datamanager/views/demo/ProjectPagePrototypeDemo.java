package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;

/**
 * <b>Project page prototype — demo view</b>
 *
 * <p>Pure-UI prototype for the reworked project landing page and in-project navigation described in
 * {@code docs/plans/Project-management-refactor.md}. It exists to be discussed with stakeholders, not
 * to touch production behaviour: it is a static composition backed by canned data, available only
 * under the {@code development} profile.</p>
 *
 * <h3>What this prototype demonstrates</h3>
 * <ul>
 *   <li>No left navigation drawer. Navigation is a set of top rows: row 1 is the primary
 *       navigation the app already has ({@code Projects · Groups · Settings}); row 2 is the new
 *       <em>in-project</em> navigation that replaces the drawer
 *       ({@code Summary · Datasets · Access}), marking the active item with weight plus a bottom
 *       rule.</li>
 *   <li>A third, lighter context strip carries the project identity plus — only while inside an
 *       experiment — the experiment crumb and the four experiment views
 *       ({@code Design · Samples · Measurements · Dataset}).</li>
 *   <li>The navigation row contains nothing project-specific, so it starts at a fixed origin and
 *       renders the same geometry in every project regardless of title length.</li>
 *   <li>Content is one centered column with a fluid gutter on wide screens.</li>
 *   <li>A single focal element: a genuine progress rail for the experiment pipeline, whose stages are
 *       clickable and whose exception callouts route straight to the place where the work
 *       continues — the prototype is navigable end to end, with dummy sample, measurement,
 *       dataset, design and access pages.</li>
 *   <li>Exactly one solid primary action per band; edit actions are scoped and anchored to the
 *       content they affect; identifiers use one monospace face.</li>
 *   <li>An empty state that is an invitation to act, with the actions inline.</li>
 *   <li>An <em>Experiments hub</em> that leads with onboarding when no experiment exists, and a
 *       pipeline that <em>gates</em> Samples, Measurements and Primary data behind their
 *       prerequisites — a locked step is a neutral state, not attention.</li>
 * </ul>
 *
 * <h3>Prototype affordances (not part of the design)</h3>
 * <p>The dashed bar at the top switches between the populated and the freshly-created (empty)
 * project and toggles a near-worst-case project title. Those controls are deliberately excluded from
 * any production consideration. All routing is in-view state, not real Vaadin routes.</p>
 *
 * @see docs/plans/Project-management-refactor.md
 * @since 1.20.0
 */
@Profile("development")
@Route("test-view/project-page")
@UIScope
@AnonymousAllowed
@org.springframework.stereotype.Component
@CssImport("./styles/views/project-prototype/project-page-prototype.css")
public class ProjectPagePrototypeDemo extends Div {

  // ── Canned data ─────────────────────────────────────────────────────────

  /** A single stage of the project pipeline, with the counts that make it actionable. */
  private record Stage(String label, int count, String unit, State state, Page target) {
    enum State { DONE, ATTENTION, TODO }
  }

  private record Experiment(
      String name,
      List<String> domains,
      int samples,
      int measurements,
      int datasets,
      int qc,
      String lastActivity) {

    boolean isStarted() {
      return samples + measurements + datasets > 0;
    }
  }

  private record Contact(String role, String name, String email, String orcid) {

    boolean hasOidc() {
      return orcid != null && !orcid.isBlank();
    }
  }

  /** A table cell with a display tone, so dummy pages read like the real surfaces. */
  private record CellView(String text, String tone) {}

  /**
   * The in-prototype pages. This mirrors the real route structure (project-level surfaces plus the
   * four experiment views) without touching any real route.
   */
  private enum Page {
    SUMMARY("Summary", false),
    DATASETS("Datasets", false),
    ACCESS("Access", false),
    ONTOLOGY("Ontology search", false),
    DESIGN("Design", true),
    SAMPLES("Samples", true),
    MEASUREMENTS("Measurements", true),
    DATASET("Dataset", true);

    private final String label;
    private final boolean experimentScoped;

    Page(String label, boolean experimentScoped) {
      this.label = label;
      this.experimentScoped = experimentScoped;
    }

    boolean isExperimentScoped() {
      return experimentScoped;
    }
  }

  private static final List<Experiment> POPULATED_EXPERIMENTS = List.of(
      new Experiment("Immunopeptidomics pilot",
          List.of("Immunopeptidomics"), 48, 24, 18, 3, "2 days ago"),
      new Experiment("Solely Immuno",
          List.of("Immunopeptidomics", "Proteomics"), 36, 30, 18, 1, "5 days ago"),
      new Experiment("Smoke test edit immunopeptidomics",
          List.of("Genomics"), 0, 0, 0, 0, "not started"));

  /**
   * One outstanding item, attributed to the experiment it belongs to.
   *
   * <p>Attention is inherently <em>per experiment</em>: several experiments can carry outstanding
   * work at the same time, and each has its own destination. A single project-level list that jumps
   * to one arbitrary experiment would both hide the others and send the user to the wrong place.</p>
   *
   * <p>A not-started experiment is deliberately <em>not</em> attention: it is a neutral state shown
   * on the experiment itself. Attention means work in progress with a gap.</p>
   */
  private record AttentionIssue(
      int experimentIndex, String text, String action, Page target, int affected) {}

  private static final List<AttentionIssue> ATTENTION_ISSUES = List.of(
      new AttentionIssue(0, "6 of 24 measurements have no primary data attached",
          "Open measurements", Page.MEASUREMENTS, 6),
      new AttentionIssue(1, "12 of 30 measurements have no primary data attached",
          "Open measurements", Page.MEASUREMENTS, 12));

  private List<AttentionIssue> issuesFor(int index) {
    return ATTENTION_ISSUES.stream()
        .filter(issue -> issue.experimentIndex() == index)
        .toList();
  }

  /** Attention is counted in <em>experiments</em>, not in items, so the rollup matches the work. */
  private int experimentsNeedingAttention() {
    return (int) ATTENTION_ISSUES.stream()
        .map(AttentionIssue::experimentIndex)
        .distinct()
        .count();
  }

  private String attentionSummaryLabel() {
    int count = experimentsNeedingAttention();
    return count + (count == 1 ? " experiment needs attention" : " experiments need attention");
  }

  /** The distinct experiments a project-level stage is outstanding for. */
  private List<Integer> experimentsForStage(Page target) {
    return ATTENTION_ISSUES.stream()
        .filter(issue -> issue.target() == target)
        .map(AttentionIssue::experimentIndex)
        .distinct()
        .toList();
  }

  /**
   * Brings the per-experiment attention list into view. Used whenever the click originates from a
   * project-level rollup that can span more than one experiment.
   */
  private void showAttentionDetail() {
    getElement().executeJs(
        "const el = this.querySelector('.pt-progress__exceptions');"
            + "if (el) el.scrollIntoView({block: 'center'});");
  }

  private static final String OBJECTIVE_LONG =
      "Characterise the immunopeptidome of patient-derived tumour organoids to identify candidate "
          + "neoantigens for downstream vaccine development. The project compares untreated "
          + "controls against treated organoid lines across two time points, using "
          + "mass-spectrometry-based immunopeptidomics. A secondary objective is to establish a "
          + "reproducible sample-handling protocol for scarce primary material, which is expected "
          + "to reduce the minimum input required per measurement.";

  private static final String OBJECTIVE_SHORT =
      "Identify candidate neoantigens from patient-derived tumour organoids by comparing the "
          + "immunopeptidome of untreated controls against treated lines.";

  /**
   * Worst-case project title: {@code ProjectTitle} allows up to 180 characters, so the navigation
   * must still hold together with a title far longer than the row can show.
   */
  private static final String LONG_TITLE =
      "Development of a quantitative immunopeptidomics workflow for minimal-input patient-derived "
          + "tumour organoid material to identify candidate neoantigens for vaccine development";

  // ── View state ──────────────────────────────────────────────────────────

  private final Div body = new Div();
  private final Div contextRow = new Div();

  private boolean emptyState = false;
  private boolean longTitle = false;
  private Page page = Page.SUMMARY;
  private int experimentIndex = 0;

  // ── Construction ────────────────────────────────────────────────────────

  public ProjectPagePrototypeDemo() {
    addClassName("pt-app");
    body.addClassName("pt-body");
    refresh();
  }

  /** The project title currently in play, so the navigation and the page header always agree. */
  private String projectTitle() {
    if (emptyState) {
      return "New project";
    }
    return longTitle ? LONG_TITLE : "Immunotest";
  }

  private Experiment selectedExperiment() {
    return POPULATED_EXPERIMENTS.get(experimentIndex);
  }

  private void navigate(Page target) {
    this.page = target;
    refresh();
  }

  private void openExperiment(int index, Page target) {
    this.experimentIndex = index;
    this.page = target;
    refresh();
  }

  private void refresh() {
    removeAll();
    add(buildProtobar(), buildGlobalBar(), buildProjectRow(), contextRow, body);
    buildContextRow();
    body.removeAll();
    body.add(buildBody());
  }

  // ── Prototype chrome ────────────────────────────────────────────────────

  private Div buildProtobar() {
    Div bar = new Div();
    bar.addClassName("pt-protobar");

    Span label = new Span("Prototype");
    label.addClassName("pt-protobar__label");
    Span note = new Span(
        "Project landing page & in-project navigation. Row 1 is the existing primary navigation; "
            + "row 2 is the proposed in-project navigation. Controls below are demo-only.");
    bar.add(label, note, spacerBefore());

    Button populated = toggle("Populated project", !emptyState, () -> {
      emptyState = false;
      page = Page.SUMMARY;
      refresh();
    });
    Button empty = toggle("New / empty project", emptyState, () -> {
      emptyState = true;
      page = Page.SUMMARY;
      refresh();
    });
    Button longTitleToggle = toggle("Long project title", longTitle, () -> {
      longTitle = !longTitle;
      refresh();
    });
    bar.add(populated, empty, longTitleToggle);
    return bar;
  }

  private Button toggle(String text, boolean pressed, Runnable action) {
    Button button = new Button(text);
    button.addClassName("pt-toggle");
    button.getElement().setAttribute("aria-pressed", Boolean.toString(pressed));
    button.addClickListener(event -> action.run());
    return button;
  }

  private Span spacerBefore() {
    Span spacer = new Span();
    spacer.addClassName("pt-protobar__spacer");
    return spacer;
  }

  // ── Row 1: primary navigation (already provided by the app) ─────────────

  private Div buildGlobalBar() {
    Div bar = new Div();
    bar.addClassName("pt-global");
    Div row = new Div();
    row.addClassName("pt-global__row");
    row.addClassName("pt-shell");

    Span wordmark = new Span("Data Manager");
    wordmark.addClassName("pt-wordmark");
    Span dot = new Span(".");
    dot.addClassName("pt-wordmark__dot");
    wordmark.add(dot);
    Div brand = new Div(wordmark);
    brand.addClassName("pt-brand");

    // Projects · Groups · Settings — rendered by TopLevelNavigationComponent in the real app.
    Div tabs = new Div();
    tabs.addClassName("pt-primary-tabs");
    tabs.add(primaryTab("Projects", true), primaryTab("Groups", false),
        primaryTab("Settings", false));

    Div right = new Div();
    right.addClassName("pt-global__right");
    Div account = new Div(initials("SG"), textSpan("Steffen Greiner", "pt-account__name"));
    account.addClassName("pt-account");
    right.add(account);

    row.add(brand, tabs, right);
    bar.add(row);
    return bar;
  }

  private Button primaryTab(String label, boolean current) {
    Button tab = new Button(label);
    tab.addClassName("pt-primary-tab");
    if (current) {
      tab.getElement().setAttribute("aria-current", "page");
    }
    return tab;
  }

  // ── Row 2: in-project navigation (new; replaces the left drawer) ────────

  /*
   * Navigation only. The project identity deliberately does NOT live here: if it
   * shared this row, a 180-character title would shift "Datasets" and "Access" to
   * a different x-position in every project, so the same target would never be in
   * the same place twice. The row therefore starts at a fixed origin and always
   * renders the same geometry, whatever the project is called.
   */
  private Div buildProjectRow() {
    Div bar = new Div();
    bar.addClassName("pt-subnav");
    Div row = new Div();
    row.addClassName("pt-subnav__row");
    row.addClassName("pt-shell");

    // The in-project surfaces that used to live in the drawer.
    Div nav = new Div();
    nav.addClassName("pt-subnav__nav");
    nav.add(navItem("Summary", Page.SUMMARY), navItem("Datasets", Page.DATASETS),
        navItem("Access", Page.ACCESS));

    Div tools = new Div();
    tools.addClassName("pt-subnav__tools");
    tools.add(navItem("Ontology search", Page.ONTOLOGY));

    row.add(nav, tools);
    bar.add(row);
    return bar;
  }

  private Button navItem(String label, Page target) {
    Button item = new Button(label);
    item.addClassName("pt-subnav__item");
    if (page == target) {
      item.getElement().setAttribute("aria-current", "page");
    }
    item.addClickListener(event -> navigate(target));
    return item;
  }

  // ── Row 3: project + experiment context ─────────────────────────────────

  /*
   * The context strip carries the project identity (code + title), so the project
   * name is always on screen without occupying any space in the navigation row.
   * While inside an experiment it additionally carries the experiment crumb and the
   * four experiment views.
   */
  private void buildContextRow() {
    contextRow.removeAll();
    contextRow.addClassName("pt-context");
    contextRow.setVisible(true);

    Div row = new Div();
    row.addClassName("pt-context__row");
    row.addClassName("pt-shell");

    Div crumbs = new Div();
    crumbs.addClassName("pt-crumbs");
    Span code = new Span("Q2IMMU");
    code.addClassName("pt-code");
    crumbs.add(new Span("Projects"), sep(), code, crumb(projectTitle()));
    row.add(crumbs);

    if (page.isExperimentScoped() && !emptyState) {
      Experiment experiment = selectedExperiment();
      crumbs.add(sep(), crumb(experiment.name()));

      Div views = new Div();
      views.addClassName("pt-expviews");
      views.add(expView("Design", null, Page.DESIGN),
          expView("Samples", experiment.samples(), Page.SAMPLES),
          expView("Measurements", experiment.measurements(), Page.MEASUREMENTS),
          expView("Dataset", experiment.datasets(), Page.DATASET));
      row.add(views);
    }

    contextRow.add(row);
  }

  private Span sep() {
    Span span = new Span("/");
    span.addClassName("pt-crumbs__sep");
    return span;
  }

  /**
   * A breadcrumb crumb whose text can be long (project titles allow 180 characters). It truncates
   * and keeps the full text reachable via the tooltip, so the crumb never pushes the experiment
   * views out of the strip.
   */
  private Span crumb(String text) {
    Span span = new Span(text);
    span.addClassName("pt-crumb");
    span.getElement().setAttribute("title", text);
    span.getStyle().set("cursor", "pointer");
    return span;
  }

  private Div expView(String label, Integer count, Page target) {
    Div view = new Div();
    view.addClassName("pt-expview");
    view.getElement().setAttribute("role", "button");
    view.getElement().setAttribute("tabindex", "0");
    view.add(textSpan(label, null));
    if (count != null) {
      Span badge = new Span(String.valueOf(count));
      badge.addClassName("pt-expview__count");
      view.add(badge);
    }
    if (page == target) {
      view.getElement().setAttribute("aria-current", "page");
    }
    view.addClickListener(event -> openExperiment(experimentIndex, target));
    return view;
  }

  // ── Body router ─────────────────────────────────────────────────────────

  private Div buildBody() {
    Div wrapper = new Div();
    wrapper.addClassName("pt-shell");
    switch (page) {
      case SUMMARY -> wrapper.add(identityBand(), progressBand(emptyState),
          experimentsBand(emptyState), metadataBand());
      case DATASETS -> wrapper.add(datasetsPage());
      case ACCESS -> wrapper.add(accessPage());
      case ONTOLOGY -> wrapper.add(ontologyPage());
      case DESIGN -> wrapper.add(designPage());
      case SAMPLES -> wrapper.add(samplesPage());
      case MEASUREMENTS -> wrapper.add(measurementsPage());
      case DATASET -> wrapper.add(datasetPage());
    }
    return wrapper;
  }

  // Identity band ─────────────────────────────────────────────────────────

  private Div identityBand() {
    Div band = new Div();
    band.addClassName("pt-band");

    Div top = new Div();
    top.addClassName("pt-identity__top");

    Div heading = new Div();
    heading.addClassName("pt-identity__heading");
    H1 title = new H1();
    title.addClassName("pt-title");
    if (emptyState) {
      title.add("New project");
    } else {
      Span code = new Span("Q2IMMU");
      code.addClassName("pt-title__code");
      title.add(textSpan(projectTitle(), null), textSpan(" ", null), code);
    }

    Div meta = new Div();
    meta.addClassName("pt-meta");
    if (emptyState) {
      meta.add(new Span("Created just now"), sepDot(), new Span("No experiments yet"));
    } else {
      meta.add(new Span("Last modified 2 days ago"), sepDot(), new Span("3 experiments"),
          sepDot(), attentionChip(attentionSummaryLabel()));
    }
    heading.add(title, meta);

    // The objective sits directly under the title: expanded, clamped to three lines.
    Div objective = new Div();
    objective.addClassName("pt-objective");
    Paragraph text = new Paragraph(emptyState ? OBJECTIVE_SHORT : OBJECTIVE_LONG);
    text.addClassName("pt-objective__text");
    Button more = new Button("Show more");
    more.addClassName("pt-btn");
    more.addClassName("pt-btn--text");
    more.addClassName("pt-objective__toggle");
    more.addClickListener(event -> {
      boolean expanded = text.hasClassName("is-expanded");
      text.setClassName("pt-objective__text");
      if (!expanded) {
        text.addClassName("is-expanded");
      }
      more.setText(expanded ? "Show more" : "Show less");
    });
    objective.add(text, more);
    heading.add(objective);

    top.add(heading);

    Div actions = new Div();
    actions.addClassName("pt-band__actions");
    actions.add(button("Add experiment", true, () -> navigate(Page.DESIGN)),
        button("Export summary", false, null));

    top.add(actions);
    band.add(top);
    band.add(peopleRow());
    return band;
  }

  /**
   * The attention rollup chip. Because attention can hit several experiments at once, the chip is a
   * project-level count that brings the user to the per-experiment list rather than jumping into one
   * arbitrary experiment; the individual rows then route to their own experiment.
   */
  private Div attentionChip(String text) {
    Div chip = new Div();
    chip.addClassName("pt-chip");
    chip.addClassName("pt-chip--attention");
    chip.addClassName("pt-chip--action");
    chip.getElement().setAttribute("role", "button");
    chip.getElement().setAttribute("tabindex", "0");
    chip.getElement().setAttribute("title", "Show which experiments need attention");
    chip.add(textSpan(text, null), icon(VaadinIcon.ANGLE_RIGHT, null));
    chip.addClickListener(event -> showAttentionDetail());
    return chip;
  }

  private Span sepDot() {
    Span span = new Span("·");
    span.addClassName("pt-meta__sep");
    return span;
  }

  private Div peopleRow() {
    Div people = new Div();
    people.addClassName("pt-people");
    people.add(new Span("Shared with"));
    for (String name : List.of("Steffen Greiner", "Wow Cool", "Lena Vogt", "Amir Rahman")) {
      people.add(initials(initialsOf(name)));
    }
    people.add(new Span("+7 others"));
    return people;
  }

  private static String initialsOf(String name) {
    String[] parts = name.split("\\s+");
    StringBuilder initials = new StringBuilder();
    for (String part : parts) {
      if (!part.isBlank()) {
        initials.append(Character.toUpperCase(part.charAt(0)));
      }
    }
    return initials.toString();
  }

  // Progress band ─────────────────────────────────────────────────────────

  private Div progressBand(boolean empty) {
    Div band = new Div();
    band.addClassName("pt-band");

    H2 heading = new H2();
    heading.addClassName("pt-band__title");
    heading.setText("Where the project stands");
    Div head = new Div();
    head.addClassName("pt-band__head");
    head.add(heading);
    band.add(head);

    Div progress = new Div();
    progress.addClassName("pt-progress");

    Div intro = new Div();
    intro.addClassName("pt-progress__intro");
    Span headline = new Span();
    headline.addClassName("pt-progress__headline");
    if (empty) {
      headline.add(new Span("Nothing has been registered yet. "),
          strong("Start with one experiment"));
    } else {
      headline.add(strong("2 of 3 experiments"), new Span(" have registered samples"));
    }
    intro.add(headline);
    progress.add(intro);

    UnorderedList rail = new UnorderedList();
    rail.addClassName("pt-rail");
    List<Stage> stages = stages(empty);
    for (int i = 0; i < stages.size(); i++) {
      Stage stage = stages.get(i);
      ListItem item = new ListItem();
      item.addClassName("pt-rail__step");
      item.addClassName("pt-rail__step--link");
      if (stage.state() == Stage.State.DONE) {
        item.addClassName("pt-rail__step--done");
      }
      if (stage.state() == Stage.State.ATTENTION) {
        item.addClassName("pt-rail__step--attention");
      }
      item.getElement().setAttribute("role", "link");
      item.getElement().setAttribute("tabindex", "0");
      // Numbers are legitimate here: this genuinely is a sequence.
      Span marker = new Span(stage.state() == Stage.State.DONE ? "✓"
          : stage.state() == Stage.State.ATTENTION ? "!" : String.valueOf(i + 1));
      marker.addClassName("pt-rail__marker");
      Span label = new Span(stage.label());
      label.addClassName("pt-rail__label");
      Span count = new Span(stage.count() + " " + stage.unit());
      count.addClassName("pt-rail__count");
      item.add(marker, label, count);
      Page target = stage.target();
      // A project-level rollup must not pick an arbitrary experiment: if several experiments share
      // the problem, reveal the per-experiment breakdown; if exactly one does, go straight to it.
      item.addClickListener(event -> {
        if (stage.state() != Stage.State.ATTENTION) {
          openExperiment(experimentIndex, target);
          return;
        }
        List<Integer> affectedExperiments = experimentsForStage(target);
        if (affectedExperiments.size() == 1) {
          openExperiment(affectedExperiments.get(0), target);
        } else {
          showAttentionDetail();
        }
      });
      rail.add(item);
    }
    progress.add(rail);

    if (!empty) {
      Div exceptions = new Div();
      exceptions.addClassName("pt-progress__exceptions");
      exceptions.add(new Span("Needs attention"));
      // One row per issue, each naming its experiment and routing into it.
      for (AttentionIssue issue : ATTENTION_ISSUES) {
        String context = POPULATED_EXPERIMENTS.get(issue.experimentIndex()).name();
        exceptions.add(attentionRow(context, issue.text(), issue.action(),
            () -> openExperiment(issue.experimentIndex(), issue.target())));
      }
      progress.add(exceptions);
    }

    band.add(progress);
    return band;
  }

  /**
   * An exception callout that is clickable. The row names the affected experiment, states the gap and
   * its destination, and routes there — so "needs attention" always answers "and now what?".
   */
  private Div attentionRow(String context, String text, String action, Runnable onClick) {
    Div row = new Div();
    row.addClassName("pt-exception");
    row.getElement().setAttribute("role", "link");
    row.getElement().setAttribute("tabindex", "0");
    row.add(icon(VaadinIcon.WARNING, null));
    Span experiment = new Span(context);
    experiment.addClassName("pt-exception__context");
    row.add(experiment, textSpan(text, null));
    Span go = new Span(action);
    go.addClassName("pt-exception__action");
    row.add(go);
    row.addClickListener(event -> onClick.run());
    return row;
  }

  private static List<Stage> stages(boolean empty) {
    if (empty) {
      return List.of(
          new Stage("Experiment created", 0, "experiments", Stage.State.TODO, Page.DESIGN),
          new Stage("Samples registered", 0, "samples", Stage.State.TODO, Page.SAMPLES),
          new Stage("Measurements registered", 0, "measurements", Stage.State.TODO,
              Page.MEASUREMENTS),
          new Stage("Primary data registered", 0, "datasets", Stage.State.TODO, Page.DATASET),
          new Stage("Sample QC uploaded", 0, "reports", Stage.State.TODO, Page.SAMPLES));
    }
    return List.of(
        new Stage("Experiment created", 3, "experiments", Stage.State.DONE, Page.DESIGN),
        new Stage("Samples registered", 84, "samples", Stage.State.DONE, Page.SAMPLES),
        new Stage("Measurements registered", 54, "measurements", Stage.State.ATTENTION,
            Page.MEASUREMENTS),
        new Stage("Primary data registered", 2, "datasets", Stage.State.ATTENTION, Page.DATASET),
        new Stage("Sample QC uploaded", 4, "reports", Stage.State.TODO, Page.SAMPLES));
  }

  // Experiments band ──────────────────────────────────────────────────────

  /**
   * The experiments section of the project landing page, and the single home of the experiment
   * list. With no experiment it is onboarding; otherwise every experiment is a card with an inline
   * snapshot. There is no separate experiments page and no second experiment list.
   */
  private Div experimentsBand(boolean empty) {
    Div band = new Div();
    band.addClassName("pt-band");

    Div head = new Div();
    head.addClassName("pt-band__head");
    H2 title = new H2();
    title.addClassName("pt-band__title");
    title.setText("Experiments");
    Div actions = new Div();
    actions.addClassName("pt-band__actions");
    actions.add(button(empty ? "Create experiment" : "Add experiment", true,
        () -> createOrAddExperiment(empty)));
    head.add(title, actions);
    band.add(head);

    if (empty) {
      Span intro = new Span(
          "Experiments are where samples, measurements and primary data live — all of it starts "
              + "with one experiment.");
      intro.addClassName("pt-note");
      band.add(intro);

      Div hero = new Div();
      hero.addClassName("pt-onboard");
      H2 heroTitle = new H2();
      heroTitle.addClassName("pt-onboard__title");
      heroTitle.setText("Create your first experiment");
      Paragraph text = new Paragraph(
          "An experiment is the research question. Its design gives samples, measurements and "
              + "primary data their scope, so the steps below unlock in order as you go.");
      text.addClassName("pt-onboard__text");
      hero.add(heroTitle, text);
      band.add(hero);
      band.add(pipeline(projectPipeline(), "What happens after you create it?", false));
      return band;
    }

    Div list = new Div();
    list.addClassName("pt-experiments");
    for (int i = 0; i < POPULATED_EXPERIMENTS.size(); i++) {
      list.add(experimentEntry(i));
    }
    band.add(list);
    return band;
  }

  /** The empty-state action creates the first experiment; otherwise it opens the add flow. */
  private void createOrAddExperiment(boolean empty) {
    if (empty) {
      // Land on the not-started experiment so the gated pipeline is visible straight away.
      emptyState = false;
      experimentIndex = 2;
      page = Page.DESIGN;
      refresh();
    } else {
      openExperiment(0, Page.DESIGN);
    }
  }

  private Div experimentEntry(int index) {
    Experiment experiment = POPULATED_EXPERIMENTS.get(index);
    Div entry = new Div();
    entry.addClassName("pt-exp");

    Div identity = new Div();
    H3 name = new H3();
    name.addClassName("pt-exp__name");
    name.setText(experiment.name());
    name.getStyle().set("cursor", "pointer");
    name.addClickListener(event -> openExperiment(index, Page.DESIGN));
    Div facets = new Div();
    facets.addClassName("pt-exp__facets");
    experiment.domains().forEach(domain -> facets.add(domainChip(domain)));
    List<AttentionIssue> issues = issuesFor(index);
    if (!experiment.isStarted()) {
      // Neutral state, not attention: routes to the first thing to do.
      facets.add(actionFacet("Not started", false, null,
          () -> openExperiment(index, Page.SAMPLES)));
    } else if (!issues.isEmpty()) {
      // The experiment carries its own attention badge, so the project rollup is never the only
      // indication that something is outstanding here.
      AttentionIssue issue = issues.get(0);
      facets.add(actionFacet("Needs attention", true, issue.text(),
          () -> openExperiment(index, issue.target())));
    }
    identity.add(name, facets);

    Div counts = new Div();
    counts.addClassName("pt-exp__counts");
    counts.add(countFact(experiment.samples(), "samples"));
    counts.add(countFact(experiment.measurements(), "measurements"));
    counts.add(countFact(experiment.datasets(), "datasets"));
    counts.add(countFact(experiment.qc(), "QC reports"));

    Div actions = new Div();
    actions.addClassName("pt-exp__actions");
    actions.add(button("Samples", false, () -> openExperiment(index, Page.SAMPLES)),
        button("Measurements", false, () -> openExperiment(index, Page.MEASUREMENTS)),
        button("Dataset", false, () -> openExperiment(index, Page.DATASET)));

    entry.add(identity, counts, actions);
    entry.add(experimentSnapshot(experiment, index));
    return entry;
  }

  private Div countFact(int value, String unit) {
    Div fact = new Div();
    fact.addClassName("pt-exp__count");
    Span number = new Span(String.valueOf(value));
    number.addClassName("pt-strong");
    Span label = new Span(unit);
    fact.add(number, label);
    return fact;
  }

  // Experiment snapshot ────────────────────────────────────────────────────

  /**
   * An inline "at a glance" overview of an experiment's samples, measurements and primary data.
   * It exists so the user can see what is there from the project/experiments page without opening
   * each nested view; clicking a panel goes to the full view.
   */
  private Div experimentSnapshot(Experiment experiment, int index) {
    int missing = issuesFor(index).stream()
        .filter(issue -> issue.target() == Page.MEASUREMENTS || issue.target() == Page.DATASET)
        .mapToInt(AttentionIssue::affected)
        .findFirst().orElse(0);
    int withData = Math.max(0, experiment.measurements() - missing);

    Div wrap = new Div();
    wrap.addClassName("pt-snapshot");
    wrap.add(snapshotPanel("Samples", experiment.samples(), "samples",
        experiment.samples() == 0 ? "Nothing registered yet"
            : "Registered · " + experiment.lastActivity(),
        samplePreview(experiment), Page.SAMPLES, index));
    wrap.add(snapshotPanel("Measurements", experiment.measurements(), "measurements",
        experiment.measurements() == 0 ? "Nothing registered yet"
            : withData + " of " + experiment.measurements() + " have primary data",
        measurementPreview(experiment, missing), Page.MEASUREMENTS, index));
    wrap.add(snapshotPanel("Primary data", experiment.datasets(), "with data",
        experiment.datasets() == 0 ? "Nothing attached yet"
            : "Across " + withData + " measurements · " + (withData * 4) + " GB",
        dataPreview(experiment), Page.DATASET, index));
    return wrap;
  }

  private Div snapshotPanel(String label, int count, String unit, String status,
      List<List<CellView>> rows, Page target, int index) {
    Div panel = new Div();
    panel.addClassName("pt-snap");

    Div head = new Div();
    head.addClassName("pt-snap__head");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("pt-snap__label");
    Span open = new Span("Open");
    open.addClassName("pt-snap__open");
    head.add(labelSpan, open);

    Div metric = new Div();
    metric.addClassName("pt-snap__metric");
    Span value = new Span(String.valueOf(count));
    value.addClassName("pt-snap__value");
    Span unitSpan = new Span(unit);
    unitSpan.addClassName("pt-snap__unit");
    metric.add(value, unitSpan);

    Span statusSpan = new Span(status);
    statusSpan.addClassName("pt-snap__status");

    Div list = new Div();
    list.addClassName("pt-snap__rows");
    rows.forEach(rowCells -> {
      Div rowDiv = new Div();
      rowDiv.addClassName("pt-snap__row");
      rowCells.forEach(cellView -> rowDiv.add(cell(cellView)));
      list.add(rowDiv);
    });

    panel.add(head, metric, statusSpan, list);
    panel.getElement().setAttribute("role", "button");
    panel.getElement().setAttribute("tabindex", "0");
    panel.addClickListener(event -> openExperiment(index, target));
    return panel;
  }

  private List<List<CellView>> samplePreview(Experiment experiment) {
    if (experiment.samples() == 0) {
      return List.of(row(muted("No samples to show yet")));
    }
    String[] names = {"P1_ctrl_T0", "P1_treat_T0", "P1_ctrl_T1"};
    int shown = Math.min(3, experiment.samples());
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 1; i <= shown; i++) {
      rows.add(row(mono(String.format("Q2IMMU-%04d", i)),
          text(names[(i - 1) % names.length])));
    }
    return rows;
  }

  private List<List<CellView>> measurementPreview(Experiment experiment, int missing) {
    if (experiment.measurements() == 0) {
      return List.of(row(muted("No measurements to show yet")));
    }
    int shown = Math.min(3, experiment.measurements());
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 1; i <= shown; i++) {
      boolean miss = i <= missing;
      rows.add(row(mono(String.format("Q2IMMU-MS-%03d", i)),
          miss ? attention("No data") : done("With data")));
    }
    return rows;
  }

  private List<List<CellView>> dataPreview(Experiment experiment) {
    if (experiment.datasets() == 0) {
      return List.of(row(muted("No primary data yet")));
    }
    int shown = Math.min(3, experiment.datasets());
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 1; i <= shown; i++) {
      rows.add(row(mono(String.format("Q2IMMU-MS-%03d", i)),
          text("3 files"), text("4.2 GB")));
    }
    return rows;
  }

  private Span domainChip(String domain) {
    String modifier = switch (domain) {
      case "Genomics" -> "pt-chip--domain-genomics";
      case "Proteomics" -> "pt-chip--domain-proteomics";
      default -> "pt-chip--domain-immuno";
    };
    return chip(domain, modifier);
  }

  private Span chip(String text, String modifier) {
    Span chip = new Span(text);
    chip.addClassName("pt-chip");
    if (modifier != null) {
      chip.addClassName(modifier);
    }
    return chip;
  }

  /** A small interactive facet chip on an experiment entry. */
  private Div actionFacet(String label, boolean attention, String tooltip, Runnable onClick) {
    Div facet = new Div();
    facet.addClassName("pt-chip");
    facet.addClassName("pt-chip--action");
    if (attention) {
      facet.addClassName("pt-chip--attention");
    }
    if (tooltip != null) {
      facet.getElement().setAttribute("title", tooltip);
    }
    facet.getElement().setAttribute("role", "button");
    facet.getElement().setAttribute("tabindex", "0");
    facet.add(textSpan(label, null));
    facet.addClickListener(event -> onClick.run());
    return facet;
  }

  // Empty experiments band ────────────────────────────────────────────────

  private Div emptyExperimentsBand() {
    Div band = new Div();
    band.addClassName("pt-band");

    Div empty = new Div();
    empty.addClassName("pt-empty");
    H3 title = new H3();
    title.addClassName("pt-empty__title");
    title.setText("This project has no experiments yet");
    Paragraph text = new Paragraph(
        "An experiment groups the samples, measurements and primary data for one research question. "
            + "Most people start with the experiment design, then register samples.");
    text.addClassName("pt-empty__text");

    UnorderedList checklist = new UnorderedList();
    checklist.addClassName("pt-checklist");
    checklist.add(checklistItem(1, "Create the first experiment",
        "Give it a name; you can add the full design later.", Page.DESIGN));
    checklist.add(checklistItem(2, "Register samples",
        "Upload an Excel template with the sample metadata.", Page.SAMPLES));
    checklist.add(checklistItem(3, "Register measurement metadata",
        "Pick the template matching the measurement type.", Page.MEASUREMENTS));
    checklist.add(checklistItem(4, "Register primary data",
        "Link the measurement data produced by the facility.", Page.DATASET));

    empty.add(title, text, checklist);
    band.add(empty);
    return band;
  }

  private ListItem checklistItem(int step, String action, String description, Page target) {
    ListItem item = new ListItem();
    item.addClassName("pt-checklist__item");
    item.getElement().setAttribute("role", "link");
    item.getElement().setAttribute("tabindex", "0");
    Span number = new Span(step + ".");
    number.addClassName("pt-checklist__step");
    Div body = new Div();
    body.addClassName("pt-checklist__body");
    Span label = new Span(action);
    label.addClassName("pt-strong");
    Paragraph detail = new Paragraph(description);
    detail.addClassName("pt-checklist__action");
    detail.getStyle().set("margin", "0");
    detail.getStyle().set("color", "var(--pt-ink-muted)");
    body.add(label, detail);
    Span link = new Span("Start");
    link.addClassName("pt-checklist__cta");
    item.add(number, body, link);
    item.addClickListener(event -> navigate(target));
    return item;
  }

  // ── Experiments hub and pipeline gating ──────────────────────────────────

  /** A step in the experiment pipeline; locked means its prerequisite is missing. */
  private enum StepState { DONE, ACTIVE, ATTENTION, LOCKED }

  private record Step(String label, String detail, StepState state, Page target) {}

  /** The project-level pipeline, with locked downstream steps while no experiment exists. */
  private List<Step> projectPipeline() {
    if (emptyState) {
      return List.of(
          new Step("Experiment", "Create one to begin", StepState.ACTIVE, Page.DESIGN),
          new Step("Samples", "Locked until an experiment exists", StepState.LOCKED, Page.SAMPLES),
          new Step("Measurements", "Locked until samples exist", StepState.LOCKED,
              Page.MEASUREMENTS),
          new Step("Primary data", "Locked until measurements exist", StepState.LOCKED,
              Page.DATASET),
          new Step("Sample QC", "Optional", StepState.LOCKED, Page.SAMPLES));
    }
    return List.of(
        new Step("Experiment", "3 created", StepState.DONE, Page.DESIGN),
        new Step("Samples", "84 registered", StepState.DONE, Page.SAMPLES),
        new Step("Measurements", "2 experiments with gaps", StepState.ATTENTION, Page.MEASUREMENTS),
        new Step("Primary data", "Missing for 18 measurements", StepState.ATTENTION, Page.DATASET),
        new Step("Sample QC", "4 reports", StepState.ACTIVE, Page.SAMPLES));
  }

  /** The pipeline of one experiment; later stages are locked until their prerequisite exists. */
  private List<Step> experimentPipeline(Experiment experiment) {
    boolean hasSamples = experiment.samples() > 0;
    boolean hasMeasurements = experiment.measurements() > 0;
    boolean hasData = experiment.datasets() > 0;
    boolean dataGap = hasMeasurements && experiment.datasets() < experiment.measurements();

    return List.of(
        new Step("Design", "Defined", StepState.DONE, Page.DESIGN),
        new Step("Samples",
            hasSamples ? experiment.samples() + " registered" : "Nothing registered yet",
            hasSamples ? StepState.DONE : StepState.ACTIVE, Page.SAMPLES),
        new Step("Measurements",
            hasMeasurements ? experiment.measurements() + " registered"
                : hasSamples ? "Ready to register" : "Register samples first",
            hasMeasurements ? StepState.DONE : hasSamples ? StepState.ACTIVE : StepState.LOCKED,
            Page.MEASUREMENTS),
        new Step("Primary data",
            hasData ? experiment.datasets() + " of " + experiment.measurements() + " attached"
                : hasMeasurements ? "Nothing attached yet" : "Register measurements first",
            hasData ? (dataGap ? StepState.ATTENTION : StepState.DONE)
                : hasMeasurements ? StepState.ACTIVE : StepState.LOCKED,
            Page.DATASET),
        new Step("Sample QC",
            experiment.qc() > 0 ? experiment.qc() + " reports" : "Optional",
            experiment.qc() > 0 ? StepState.DONE : StepState.LOCKED, Page.SAMPLES));
  }

  private Div experimentPreamble(Experiment experiment) {
    return pipeline(experimentPipeline(experiment),
        "This experiment's path — later steps unlock as the earlier ones are done", true);
  }

  /**
   * Renders the ordered steps. A locked step is deliberately neutral (dashed, muted) rather than
   * amber, so it cannot be confused with an attention gap.
   */
  private Div pipeline(List<Step> steps, String caption, boolean navigable) {
    Div wrap = new Div();
    wrap.addClassName("pt-pipeline");
    if (caption != null) {
      Span cap = new Span(caption);
      cap.addClassName("pt-pipeline__caption");
      wrap.add(cap);
    }
    Div track = new Div();
    track.addClassName("pt-pipeline__track");
    for (Step step : steps) {
      Div node = new Div();
      node.addClassName("pt-step");
      node.addClassName("pt-step--" + step.state().name().toLowerCase());

      Span marker = new Span();
      marker.addClassName("pt-step__marker");
      switch (step.state()) {
        case DONE -> marker.add(new Span("✓"));
        case ATTENTION -> marker.add(new Span("!"));
        case ACTIVE -> marker.add(new Span("●"));
        case LOCKED -> {
          Icon lock = VaadinIcon.LOCK.create();
          lock.addClassName("pt-step__lock");
          marker.add(lock);
        }
      }
      Span label = new Span(step.label());
      label.addClassName("pt-step__label");
      Span detail = new Span(step.detail());
      detail.addClassName("pt-step__detail");
      node.add(marker, label, detail);

      if (navigable && step.state() != StepState.LOCKED && step.target() != null) {
        node.getElement().setAttribute("role", "button");
        node.getElement().setAttribute("tabindex", "0");
        node.addClickListener(event -> openExperiment(experimentIndex, step.target()));
      } else if (step.state() == StepState.LOCKED) {
        node.getElement().setAttribute("title", step.detail());
      }
      track.add(node);
    }
    wrap.add(track);
    return wrap;
  }

  /**
   * A blocked surface. States the missing prerequisite and routes to the step that unblocks it,
   * instead of rendering a registration form that cannot succeed.
   */
  private Div lockedPage(String title, String subtitle, String gateTitle, String gateText,
      String action, Page target) {
    Div band = pageBand(title, subtitle);
    Div gate = new Div();
    gate.addClassName("pt-gate");
    Icon lock = VaadinIcon.LOCK.create();
    lock.addClassName("pt-gate__icon");
    H3 heading = new H3();
    heading.addClassName("pt-gate__title");
    heading.setText(gateTitle);
    Paragraph text = new Paragraph(gateText);
    text.addClassName("pt-gate__text");
    gate.add(lock, heading, text);
    if (action != null && target != null) {
      Div actions = new Div();
      actions.addClassName("pt-band__actions");
      actions.add(button(action, false, () -> openExperiment(experimentIndex, target)));
      gate.add(actions);
    }
    band.add(gate);
    return band;
  }

  // ── Dummy in-project pages ──────────────────────────────────────────────

  private Div samplesPage() {
    Experiment experiment = emptyState ? null : selectedExperiment();
    if (experiment == null || experiment.samples() == 0) {
      return emptyPage("Samples",
          experiment == null ? "No experiment yet" : experiment.name(),
          "No samples registered yet",
          "Samples carry the experimental metadata (species, tissue, treatment, replicates). "
              + "Upload the sample template to register them in bulk.",
          button("Register samples", true, null), button("Download template", false, null));
    }
    Div band = pageBand("Samples", experiment.name() + " · " + experiment.samples() + " samples",
        button("Register samples", true, null), button("Download template", false, null));
    band.add(experimentPreamble(experiment));
    band.add(table(new String[]{"Sample ID", "Sample name", "Species", "Tissue", "Condition",
            "Replicates"},
        List.of(
            row(mono("Q2IMMU-0001"), text("P1_ctrl_T0"), text("Homo sapiens"),
                text("Tumour organoid"), text("Untreated"), text("2")),
            row(mono("Q2IMMU-0002"), text("P1_treat_T0"), text("Homo sapiens"),
                text("Tumour organoid"), text("Treated"), text("2")),
            row(mono("Q2IMMU-0003"), text("P1_ctrl_T1"), text("Homo sapiens"),
                text("Tumour organoid"), text("Untreated"), text("2")),
            row(mono("Q2IMMU-0004"), text("P2_ctrl_T0"), text("Homo sapiens"),
                text("Tumour organoid"), text("Untreated"), text("3")),
            row(mono("Q2IMMU-0005"), text("P2_treat_T0"), text("Homo sapiens"),
                text("Tumour organoid"), text("Treated"), text("3")),
            row(mono("Q2IMMU-0006"), text("QC_pool"), text("Homo sapiens"),
                text("Tumour organoid"), text("Reference"), text("1")))));
    band.add(note("Showing 6 of " + experiment.samples() + " samples."));
    return band;
  }

  private Div measurementsPage() {
    Experiment experiment = emptyState ? null : selectedExperiment();
    if (experiment == null) {
      return emptyPage("Measurements", "No experiment yet",
          "No measurements registered yet",
          "Register the measurement metadata and attach the primary data produced by the facility.",
          button("Register measurements", true, null), button("Download template", false, null));
    }
    if (experiment.samples() == 0) {
      return lockedPage("Measurements", experiment.name() + " · 0 samples",
          "Register samples first",
          "Measurements are made on samples. Register the samples for this experiment before adding "
              + "measurement metadata.",
          "Go to samples", Page.SAMPLES);
    }
    if (experiment.measurements() == 0) {
      return emptyPage("Measurements", experiment.name(),
          "No measurements registered yet",
          "Register the measurement metadata and attach the primary data produced by the facility.",
          button("Register measurements", true, null), button("Download template", false, null));
    }
    String issueText = issuesFor(experimentIndex).stream()
        .filter(issue -> issue.target() == Page.MEASUREMENTS)
        .map(AttentionIssue::text)
        .findFirst()
        .orElse(null);
    int affected = issuesFor(experimentIndex).stream()
        .filter(issue -> issue.target() == Page.MEASUREMENTS)
        .mapToInt(AttentionIssue::affected)
        .findFirst()
        .orElse(0);

    Div band = pageBand("Measurements",
        experiment.name() + " · " + experiment.measurements() + " measurements",
        button("Register measurements", true, null), button("Download template", false, null));
    band.add(experimentPreamble(experiment));

    List<List<CellView>> rows = measurementRows(experiment, affected);
    band.add(table(new String[]{"Measurement ID", "Label", "Type", "Instrument", "Samples",
        "Primary data", "QC"}, rows));

    if (issueText != null) {
      band.add(note(issueText + ". Use “Attach data” on a row to link the measurement data."));
    } else {
      band.add(note("Showing " + rows.size() + " of " + experiment.measurements()
          + " measurements. No outstanding issues for this experiment."));
    }
    return band;
  }

  /**
   * Representative measurement rows for one experiment. The number of rows shown as missing follows
   * that experiment's own outstanding count, so the page agrees with the callout that links to it.
   */
  private List<List<CellView>> measurementRows(Experiment experiment, int affected) {
    int shown = Math.min(experiment.measurements(), Math.max(8, affected));
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 1; i <= shown; i++) {
      boolean missing = i <= affected;
      rows.add(row(
          mono(String.format("Q2IMMU-MS-%03d", i)),
          text("Measurement " + i),
          text("Immunopeptidomics"),
          text("timsTOF Pro"),
          text("2"),
          missing ? attention("Missing") : done("Connected"),
          missing ? muted("Pending") : done("Passed")));
    }
    return rows;
  }

  /**
   * The experiment's dataset. A dataset is 1:1 with its experiment, so this page is simply "the data
   * of this experiment" — of kind <em>primary</em> — broken down by the measurements that produced
   * it. Datasets hosted elsewhere appear below as connected datasets, whose relationship to the
   * experiment is deliberately left unclassified.
   */
  private Div datasetPage() {
    Experiment experiment = emptyState ? null : selectedExperiment();
    if (experiment == null) {
      return emptyPage("Dataset", "No experiment yet",
          "No primary data registered yet",
          "Primary data is the output of the measurements. Link the measurement data produced by "
              + "the facility, or upload it directly.",
          button("Register primary data", true, null), button("Upload files", false, null));
    }
    if (experiment.measurements() == 0) {
      return lockedPage("Dataset", experiment.name() + " · 0 measurements",
          "Register measurements first",
          "Primary data is the output of measurements. Register measurements for this experiment "
              + "before attaching data.",
          "Go to measurements", Page.MEASUREMENTS);
    }
    if (experiment.datasets() == 0) {
      return emptyPage("Dataset", experiment.name(),
          "No primary data registered yet",
          "Primary data is the output of the measurements. Link the measurement data produced by "
              + "the facility, or upload it directly.",
          button("Register primary data", true, null), button("Upload files", false, null));
    }
    int affected = issuesFor(experimentIndex).stream()
        .filter(issue -> issue.target() == Page.MEASUREMENTS)
        .mapToInt(AttentionIssue::affected)
        .findFirst()
        .orElse(0);

    Div band = pageBand("Dataset", experiment.name() + " · primary data",
        button("Register primary data", true, null));
    band.add(experimentPreamble(experiment));
    band.add(datasetFacts(experiment, affected));
    band.add(table(new String[]{"Measurement", "Files", "File types", "Size", "State"},
        datasetRows(experiment, affected)));
    band.add(note("The dataset aggregates the primary data of this experiment's measurements. "
        + "Its scope is the experiment because the design is what makes the data analysable."));

    Div connectedHead = new Div();
    connectedHead.addClassName("pt-band__head");
    H3 connectedTitle = new H3();
    connectedTitle.addClassName("pt-band__title");
    connectedTitle.setText("Connected datasets");
    connectedHead.add(connectedTitle,
        new Span("Hosted externally. The relationship to this experiment is not classified."));
    band.add(connectedHead);
    band.add(table(new String[]{"Title", "Repository", "Access", "Related to"},
        List.of(
            row(text("Proteome reanalysis — immunopeptidome"), text("Zenodo"), done("Open"),
                text("This experiment")),
            row(text("Preliminary QC report"), text("InvenioRDM"), attention("Restricted"),
                text("Project")))));
    return band;
  }

  private Div datasetFacts(Experiment experiment, int affected) {
    int withData = experiment.measurements() - affected;
    Div facts = new Div();
    facts.addClassName("pt-facts");
    facts.add(fact("Kind", "Primary"));
    facts.add(fact("Measurements with data", withData + " of " + experiment.measurements()));
    facts.add(fact("Files", String.valueOf(withData * 3)));
    facts.add(fact("Total size", withData * 4 + " GB"));
    return facts;
  }

  /** Per-measurement primary data: the parts the experiment dataset is made of. */
  private List<List<CellView>> datasetRows(Experiment experiment, int affected) {
    int shown = Math.min(experiment.measurements(), Math.max(8, affected));
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 1; i <= shown; i++) {
      boolean missing = i <= affected;
      rows.add(row(
          mono(String.format("Q2IMMU-MS-%03d", i)),
          missing ? attention("0") : text("3"),
          missing ? muted("—") : text(".d, .tsv"),
          missing ? muted("—") : text("4.2 GB"),
          missing ? attention("Not attached") : done("Registered")));
    }
    return rows;
  }

  private Div designPage() {
    Experiment experiment = emptyState ? null : selectedExperiment();
    if (experiment == null || !experiment.isStarted()) {
      return emptyPage("Experiment design",
          experiment == null ? "No experiment yet" : experiment.name(),
          "The design is not complete yet",
          "Define the experimental groups, variables and confounding variables for this experiment.",
          button("Edit design", true, null));
    }
    Div band = pageBand("Experiment design", experiment.name(),
        button("Edit design", true, null));
    band.add(experimentPreamble(experiment));

    H2 glance = new H2();
    glance.addClassName("pt-band__title");
    glance.setText("At a glance");
    band.add(glance);
    band.add(experimentSnapshot(experiment, experimentIndex));

    Div grid = new Div();
    grid.addClassName("pt-detail-grid");
    grid.add(detailBlock("Experimental groups",
        List.of("Untreated control — 24 samples", "Treated — 24 samples")));
    grid.add(detailBlock("Variables",
        List.of("Condition (untreated / treated)", "Timepoint (T0 / T1)",
            "Biological replicate")));
    grid.add(detailBlock("Confounding variables",
        List.of("Passage number", "Batch")));
    band.add(grid);
    return band;
  }

  private Div datasetsPage() {
    if (emptyState) {
      return emptyPage("Datasets", "No experiment yet",
          "No data yet",
          "Create an experiment first; its dataset follows from the measurements you register.",
          button("Add experiment", true, null));
    }
    Div band = pageBand("Datasets",
        "The data produced in this project, and datasets connected to it",
        button("Register primary data", true, null));

    // Produced here: one dataset per experiment, because a dataset is 1:1 with its experiment.
    Div producedHead = new Div();
    producedHead.addClassName("pt-band__head");
    H3 producedTitle = new H3();
    producedTitle.addClassName("pt-band__title");
    producedTitle.setText("Produced in this project");
    producedHead.add(producedTitle, new Span("One dataset per experiment, of kind primary."));
    band.add(producedHead);
    band.add(table(new String[]{"Experiment", "Kind", "Measurements with data", "Files", "Size"},
        List.of(
            row(text("Immunopeptidomics pilot"), text("Primary"), text("18 of 24"), text("54"),
                text("76 GB")),
            row(text("Solely Immuno"), text("Primary"), text("18 of 30"), text("54"),
                text("71 GB")),
            row(text("Smoke test edit immunopeptidomics"), muted("—"), muted("0 of 0"),
                muted("—"), muted("—")))));

    Div connectedHead = new Div();
    connectedHead.addClassName("pt-band__head");
    H3 connectedTitle = new H3();
    connectedTitle.addClassName("pt-band__title");
    connectedTitle.setText("Connected datasets");
    connectedHead.add(connectedTitle, new Span("Hosted elsewhere and linked here."));
    band.add(connectedHead);
    band.add(table(new String[]{"Title", "Repository", "Access", "Related to", "Connected on"},
        List.of(
            row(text("Proteome reanalysis — immunopeptidome"), text("Zenodo"), done("Open"),
                text("Immunopeptidomics pilot"), text("2 days ago")),
            row(text("Preliminary QC report"), text("InvenioRDM"), attention("Restricted"),
                text("Project"), text("5 days ago")),
            row(text("Reference peptide list"), text("Zenodo"), done("Open"), muted("—"),
                text("3 weeks ago")))));
    band.add(note("“Connected” states only that the dataset is linked here. It may be an analysis "
        + "result, related material, or something else — the relationship is not classified. Open "
        + "datasets can be downloaded directly; restricted ones require a data access request."));
    return band;
  }

  private Div accessPage() {
    Div band = pageBand("Access", "Who can see and edit this project",
        button("Invite people", true, null));
    band.add(table(new String[]{"Person", "Role", "Since"},
        List.of(
            row(text("Steffen Greiner"), text("Owner"), text("12 Jan 2025")),
            row(text("Lena Vogt"), text("Project manager"), text("12 Jan 2025")),
            row(text("Wow Cool"), text("Collaborator"), text("3 Feb 2025")),
            row(text("Amir Rahman"), text("Viewer"), text("18 Mar 2025")))));
    return band;
  }

  private Div ontologyPage() {
    Div band = pageBand("Ontology search",
        "Look up ontology terms used in sample and measurement metadata", List.of());
    TextField search = new TextField();
    search.setPlaceholder("Search a term, e.g. “tumour organoid”");
    search.addClassName("pt-searchfield");
    band.add(search);
    band.add(table(new String[]{"Term", "Ontology", "ID"},
        List.of(
            row(text("tumour organoid"), text("OBI"), mono("OBI:0002345")),
            row(text("Homo sapiens"), text("NCBITaxon"), mono("NCBITaxon:9606")),
            row(text("immunopeptidomics"), text("EFO"), mono("EFO:0009736")))));
    return band;
  }

  // ── Metadata band ───────────────────────────────────────────────────────

  private Div metadataBand() {
    Div band = new Div();
    band.addClassName("pt-band");

    Div head = new Div();
    head.addClassName("pt-band__head");
    H2 title = new H2();
    title.addClassName("pt-band__title");
    title.setText("Project details");
    Div actions = new Div();
    actions.addClassName("pt-band__actions");
    actions.add(button("Edit project details", false, null));
    head.add(title, actions);
    band.add(head);

    Div facts = new Div();
    facts.addClassName("pt-facts");
    facts.add(fact("Project ID", "Q2IMMU"));
    facts.add(fact("Funding", "123 (Test grant)"));
    facts.add(fact("Data access", "Restricted — request required"));
    facts.add(fact("Primary data type", "Immunopeptidomics"));
    band.add(facts);

    Div contactsHead = new Div();
    contactsHead.addClassName("pt-band__head");
    H3 contactsTitle = new H3();
    contactsTitle.addClassName("pt-band__title");
    contactsTitle.setText("Contacts");
    Div contactActions = new Div();
    contactActions.addClassName("pt-band__actions");
    contactActions.add(button("Edit contacts", false, null));
    contactsHead.add(contactsTitle, contactActions);
    band.add(contactsHead);

    Div contacts = new Div();
    contacts.addClassName("pt-contacts");
    contacts.add(contactCard(new Contact("Principal investigator", "Steffen Greiner",
        "steffen.greiner@qbic.uni-tuebingen.de", "0009-0006-0929-9338")));
    contacts.add(contactCard(new Contact("Project manager", "Wow Cool", "wow@cool.dude", null)));
    contacts.add(contactCard(new Contact("Project responsible", "Lena Vogt",
        "lena.vogt@qbic.uni-tuebingen.de", "0000-0002-1825-0097")));
    band.add(contacts);

    return band;
  }

  private Div fact(String label, String value) {
    Div fact = new Div();
    Span labelSpan = new Span(label);
    labelSpan.addClassName("pt-fact__label");
    Paragraph valueParagraph = new Paragraph(value);
    valueParagraph.addClassName("pt-fact__value");
    fact.add(labelSpan, valueParagraph);
    return fact;
  }

  private Div contactCard(Contact contact) {
    Div card = new Div();
    card.addClassName("pt-contact");
    Paragraph role = new Paragraph(contact.role());
    role.addClassName("pt-contact__role");
    Paragraph name = new Paragraph(contact.name());
    name.addClassName("pt-contact__name");
    Paragraph email = new Paragraph(contact.email());
    email.addClassName("pt-contact__line");
    card.add(role, name, email);
    if (contact.hasOidc()) {
      Paragraph orcid = new Paragraph();
      orcid.addClassName("pt-contact__line");
      Span pid = new Span(contact.orcid());
      pid.addClassName("pt-id");
      orcid.add(pid, new Span("  ORCID verified"));
      card.add(orcid);
    } else {
      Paragraph missing = new Paragraph("No ORCID on file");
      missing.addClassName("pt-contact__line");
      card.add(missing);
    }
    return card;
  }

  // ── Page scaffolding ────────────────────────────────────────────────────

  private Div pageBand(String title, String subtitle, Button... actions) {
    return pageBand(title, subtitle, List.of(actions));
  }

  private Div pageBand(String title, String subtitle, List<Button> actions) {
    Div band = new Div();
    band.addClassName("pt-band");

    Div head = new Div();
    head.addClassName("pt-band__head");

    Div heading = new Div();
    H1 h1 = new H1();
    h1.addClassName("pt-title");
    h1.setText(title);
    heading.add(h1);
    if (subtitle != null) {
      Paragraph p = new Paragraph(subtitle);
      p.addClassName("pt-page__subtitle");
      heading.add(p);
    }
    head.add(heading);

    if (!actions.isEmpty()) {
      Div actionBar = new Div();
      actionBar.addClassName("pt-band__actions");
      actions.forEach(actionBar::add);
      head.add(actionBar);
    }

    band.add(head);
    return band;
  }

  private Div emptyPage(String title, String subtitle, String emptyTitle, String emptyText,
      Button... actions) {
    Div band = pageBand(title, subtitle, List.of());
    Div empty = new Div();
    empty.addClassName("pt-empty");
    H3 h3 = new H3();
    h3.addClassName("pt-empty__title");
    h3.setText(emptyTitle);
    Paragraph p = new Paragraph(emptyText);
    p.addClassName("pt-empty__text");
    empty.add(h3, p);

    if (actions.length > 0) {
      Div actionBar = new Div();
      actionBar.addClassName("pt-band__actions");
      for (Button action : actions) {
        actionBar.add(action);
      }
      empty.add(actionBar);
    }
    band.add(empty);
    return band;
  }

  // ── Table helpers ───────────────────────────────────────────────────────

  private Div table(String[] headers, List<List<CellView>> rows) {
    Div table = new Div();
    table.addClassName("pt-table");

    Div head = new Div();
    head.addClassName("pt-table__row");
    head.addClassName("pt-table__row--head");
    for (String header : headers) {
      head.add(cell(new CellView(header, "head")));
    }
    table.add(head);

    for (List<CellView> row : rows) {
      Div rowDiv = new Div();
      rowDiv.addClassName("pt-table__row");
      row.forEach(cell -> rowDiv.add(cell(cell)));
      table.add(rowDiv);
    }
    return table;
  }

  private Span cell(CellView cell) {
    Span span = new Span(cell.text());
    span.addClassName("pt-table__cell");
    if (cell.tone() != null) {
      span.addClassName("pt-cell--" + cell.tone());
    }
    return span;
  }

  private static List<CellView> row(CellView... cells) {
    return List.of(cells);
  }

  private static CellView text(String value) {
    return new CellView(value, null);
  }

  private static CellView mono(String value) {
    return new CellView(value, "mono");
  }

  private static CellView muted(String value) {
    return new CellView(value, "muted");
  }

  private static CellView attention(String value) {
    return new CellView(value, "attention");
  }

  private static CellView done(String value) {
    return new CellView(value, "done");
  }

  private Span note(String text) {
    Span note = new Span(text);
    note.addClassName("pt-note");
    return note;
  }

  private Div detailBlock(String title, List<String> items) {
    Div block = new Div();
    block.addClassName("pt-detail");
    H3 heading = new H3();
    heading.addClassName("pt-band__title");
    heading.setText(title);
    UnorderedList list = new UnorderedList();
    list.addClassName("pt-detail__list");
    items.forEach(item -> list.add(new ListItem(item)));
    block.add(heading, list);
    return block;
  }

  // ── Buttons ─────────────────────────────────────────────────────────────

  private static Button button(String label, boolean primary, Runnable action) {
    Button button = new Button(label);
    button.addClassName("pt-btn");
    if (primary) {
      button.addClassName("pt-btn--primary");
    }
    if (action != null) {
      button.addClickListener(event -> action.run());
    }
    return button;
  }

  // ── Leaf helpers ────────────────────────────────────────────────────────

  private static Span strong(String text) {
    Span span = new Span(text);
    span.addClassName("pt-strong");
    return span;
  }

  private static Span initials(String text) {
    Span span = new Span(text);
    span.addClassName("pt-initials");
    return span;
  }

  private static Span textSpan(String text, String className) {
    Span span = new Span(text);
    if (className != null) {
      span.addClassName(className);
    }
    return span;
  }

  private static Icon icon(VaadinIcon vaadinIcon, String className) {
    Icon icon = vaadinIcon.create();
    icon.addClassName("icon-size-xs");
    if (className != null) {
      icon.addClassName(className);
    }
    return icon;
  }
}