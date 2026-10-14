package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Abbr;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 *       experiment — the experiment crumb.</li>
 *   <li>The navigation row contains nothing project-specific, so it starts at a fixed origin and
 *       renders the same geometry in every project regardless of title length.</li>
 *   <li>Content is one centered column with a fluid gutter on wide screens.</li>
 *   <li>An experiment landing that leads with the design — its payload, or the invitation to create
 *       it — and follows with the samples and dataset at a glance. Those cards are the navigation,
 *       so the experiment views are offered once, not by a rail and a context-strip switcher as
 *       well.</li>
 *   <li>Exactly one solid primary action per band; edit actions are scoped and anchored to the
 *       content they affect; identifiers use one monospace face.</li>
 *   <li>An empty state that is an invitation to act, with the actions inline.</li>
 *   <li>An <em>Experiments hub</em> that leads with onboarding when no experiment exists, and a
 *       pipeline that <em>gates</em> Samples and the Dataset (measurement metadata + primary data)
 *       behind their prerequisites — a locked step is a neutral state, not attention.</li>
 *   <li>Project-level <em>Samples</em> (one flat, read-only list with an Experiment column and
 *       quick-filter buttons) and <em>Datasets</em> (measurement metadata and registered primary
 *       data together, plus connected datasets).</li>
 * </ul>
 *
 * <h3>Prototype affordances (not part of the design)</h3>
 * <p>The dashed bar at the top switches between the populated and the freshly-created (empty)
 * project and toggles a near-worst-case project title. Those controls are deliberately excluded from
 * any production consideration. Navigation is real routing: the view and the experiment are route
 * query, so the browser's Back and Forward buttons walk the prototype like any other page.</p>
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
public class ProjectPagePrototypeDemo extends Div implements BeforeEnterObserver {

  // ── Canned data ─────────────────────────────────────────────────────────

  /** A single stage of the project pipeline, with the counts that make it actionable. */
  private record Stage(String label, int count, String unit, State state, Page target) {
    enum State { DONE, ATTENTION, TODO }
  }

  /**
   * One experiment as the project page presents it: identity, the counts that make it actionable,
   * and the recent activity the inline snapshots summarise.
   *
   * @param samplesActivity   the verb for the samples panel status, either {@code "Registered"} or
   *                          {@code "Modified"}; {@code null} when the experiment has no samples
   * @param daysSinceActivity how long ago something in this experiment last changed. Used as the
   *                          relative age of the newest row in both snapshots
   */
  private record Experiment(
      String name,
      List<String> domains,
      int samples,
      int measurements,
      int datasets,
      String samplesActivity,
      int daysSinceActivity) {

    boolean isStarted() {
      return samples + measurements + datasets > 0;
    }
  }

  private record Contact(String roleAbbreviation, String role, String name, String email,
                         String orcid) {

    boolean hasOrcid() {
      return orcid != null && !orcid.isBlank();
    }

    String orcidUrl() {
      return "https://orcid.org/" + orcid;
    }
  }

  /** A table cell with a display tone, so dummy pages read like the real surfaces. */
  private record CellView(String text, String tone) {}

  /** One key/value line in the side panel. */
  private record DetailField(String label, String value) {}

  /** A titled group of fields; headings reuse the stakeholder spec's categories. */
  private record DetailGroup(String heading, List<DetailField> fields) {}

  /** The side-panel content for a selected record. */
  private record Detail(String title, String subtitle, List<String> chips,
      List<DetailGroup> groups) {}

  /**
   * The in-prototype pages. This mirrors the real route structure (project-level surfaces plus the
   * four experiment views) without touching any real route.
   */
  private enum Page {
    SUMMARY("Summary", false),
    SAMPLES("Samples", false),
    DATASETS("Datasets", false),
    ACCESS("Access", false),
    ONTOLOGY("Ontology search", false),
    SETTINGS("Project settings", false),
    DESIGN("Overview", true),
    EXP_SAMPLES("Samples", true),
    EXP_DATASET("Dataset", true);

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
          List.of("Immunopeptidomics"), 48, 24, 18, "Modified", 2),
      new Experiment("Solely Immuno",
          List.of("Immunopeptidomics", "Proteomics"), 36, 30, 18, "Registered", 5),
      new Experiment("Smoke test edit immunopeptidomics",
          List.of("Genomics"), 0, 0, 0, null, 0));

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
          "Open dataset", Page.EXP_DATASET, 6),
      new AttentionIssue(1, "12 of 30 measurements have no primary data attached",
          "Open dataset", Page.EXP_DATASET, 12));

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
  private int sampleFilter = -1;
  private int datasetFilter = -1;
  private boolean showAllColumns = false;
  private String datasetType = "Immunopeptidomics";
  /**
   * Whether a design exists for the experiment. Prototype-only switch: a real view reads this from
   * the domain. It exists so the defect this prototype was fixed for stays demonstrable — a design
   * can be created without the user being able to see that it happened.
   */
  private boolean designDefined = true;
  private Detail detail = null;
  /** Which contact card is open for editing, or {@code null} when all are read-only. */
  private String editingContact = null;

  /** Which page-local section of the Project settings page is open. */
  private String settingsSection = CONTACTS_SECTION;

  // ── Construction ────────────────────────────────────────────────────────

  public ProjectPagePrototypeDemo() {
    addClassName("pt-app");
    body.addClassName("pt-body");
    refresh();
  }

  /**
   * Re-renders for the location the browser asked for, so Back and Forward walk the prototype's
   * history like any other page. The view and the experiment are carried as query parameters, which
   * keeps the route itself static — a parameterised route template is one more thing that can fail to
   * resolve, and this prototype does not need it.
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    QueryParameters query = event.getLocation().getQueryParameters();
    this.page = parsePage(query.getSingleParameter("view").orElse(Page.SUMMARY.name()));
    this.experimentIndex = clampExperiment(
        query.getSingleParameter("experiment").map(ProjectPagePrototypeDemo::parseInt).orElse(0));
    refresh();
  }

  private static int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException notANumber) {
      return 0;
    }
  }

  /** An unknown or absent view falls back to the project summary rather than failing the route. */
  private static Page parsePage(String name) {
    for (Page candidate : Page.values()) {
      if (candidate.name().equals(name)) {
        return candidate;
      }
    }
    return Page.SUMMARY;
  }

  /** Keeps a hand-typed or stale experiment index inside the canned data instead of throwing. */
  private static int clampExperiment(int index) {
    if (index < 0 || index >= POPULATED_EXPERIMENTS.size()) {
      return 0;
    }
    return index;
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
    openExperiment(experimentIndex, target);
  }

  /**
   * Opens a view by pushing a real location, so the browser's Back and Forward buttons work.
   *
   * <p>Without a UI — the constructor specs build the view directly — there is no history to push, so
   * the state is applied in place and the view re-renders. That keeps the specs able to drive the
   * prototype without a running browser, while the browser gets real routing.</p>
   */
  private void openExperiment(int index, Page target) {
    UI ui = UI.getCurrent();
    if (ui == null) {
      this.experimentIndex = clampExperiment(index);
      this.page = target;
      refresh();
      return;
    }
    Map<String, String> query = target.isExperimentScoped()
        ? Map.of("view", target.name(), "experiment", String.valueOf(clampExperiment(index)))
        : Map.of("view", target.name());
    ui.navigate(ProjectPagePrototypeDemo.class, QueryParameters.simple(query));
  }

  private void refresh() {
    removeAll();
    add(buildProtobar(), buildGlobalBar(), buildProjectRow(), contextRow, body);
    buildContextRow();
    body.removeAll();
    body.add(buildBody());
    if (detail != null) {
      add(buildDetailPanel());
    }
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

    // Prototype-only chrome, so it is deliberately shaped differently from the product's buttons:
    // at full accent it was the same blue as the page's one primary action and competed with it.
    Button populated = toggle("Populated project", !emptyState, () -> {
      emptyState = false;
      navigate(Page.SUMMARY);
    });
    Button empty = toggle("New / empty project", emptyState, () -> {
      emptyState = true;
      navigate(Page.SUMMARY);
    });
    Button longTitleToggle = toggle("Long project title", longTitle, () -> {
      longTitle = !longTitle;
      refresh();
    });
    Button designToggle = toggle("No design yet", !designDefined, () -> {
      designDefined = !designDefined;
      refresh();
    });
    bar.add(populated, empty, longTitleToggle, designToggle);
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

    // Navigation rows carry navigation, not numbers. The counts were repeated in the context-strip
    // pills, the pipeline cards and the snapshot panels; omitting them from BOTH navigation layers
    // removes the duplication at its source instead of shuffling it between rows.
    Div nav = new Div();
    nav.addClassName("pt-subnav__nav");
    nav.add(navItem("Summary", Page.SUMMARY), navItem("Samples", Page.SAMPLES),
        navItem("Datasets", Page.DATASETS), navItem("Access", Page.ACCESS));

    Div tools = new Div();
    tools.addClassName("pt-subnav__tools");
    // "Project settings", never bare "Settings": row 1 already carries a shell-level "Settings"
    // tab (the account/groups hub) and two adjacent rows both saying "Settings" gave no hint which
    // scope applied. Scoping the label matches the plan's rule for actions ("Edit contacts", not
    // "Edit") and keeps row 1 untouched.
    tools.add(navItem("Ontology search", Page.ONTOLOGY),
        navItem("Project settings", Page.SETTINGS));

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
   * While inside an experiment it additionally carries the experiment crumb.
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
    Span code = crumb("Q2IMMU", () -> navigate(Page.SUMMARY));
    code.addClassName("pt-code");
    crumbs.add(crumb("Projects", () -> navigate(Page.SUMMARY)), sep(), code,
        crumb(projectTitle(), () -> navigate(Page.SUMMARY)));
    row.add(crumbs);

    // The breadcrumb names the experiment: it is the one persistent statement of WHICH experiment is
    // being viewed, and the way back UP from its sub-pages. It is the only such statement — the page
    // header used to repeat it, and that second line was the duplication.
    if (page.isExperimentScoped() && !emptyState) {
      Experiment experiment = selectedExperiment();
      crumbs.add(sep(), page == Page.DESIGN
          ? currentCrumb(experiment.name())
          : crumb(experiment.name(), () -> openExperiment(experimentIndex, Page.DESIGN)));

      // The experiment's own views, in the chrome. This is the switcher that moves the user sideways
      // between the overview and the two data views without walking back up to the overview first,
      // and it marks the current view so the strip answers "where am I" as well as "where to".
      Div views = new Div();
      views.addClassName("pt-expviews");
      views.add(expView("Overview", Page.DESIGN), expView("Samples", Page.EXP_SAMPLES),
          expView("Dataset", Page.EXP_DATASET));
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
   * The crumb for the page being viewed: named, but not a link to itself. Used for the experiment
   * while its overview is open, where the crumb is the current location rather than a destination.
   */
  private Span currentCrumb(String text) {
    Span span = new Span(text);
    span.addClassName("pt-crumb");
    span.addClassName("pt-crumb--current");
    span.getElement().setAttribute("title", text);
    return span;
  }

  /**
   * One experiment view in the context strip. It is the switcher between the overview and the two
   * data views — the only one, now that the in-page rail is gone — and it marks the active view, so
   * the chrome answers both "where can I go" and "where am I".
   */
  private Div expView(String label, Page target) {
    Div view = new Div();
    view.addClassName("pt-expview");
    view.getElement().setAttribute("role", "button");
    view.getElement().setAttribute("tabindex", "0");
    view.add(textSpan(label, null));
    if (page == target) {
      view.getElement().setAttribute("aria-current", "page");
    }
    activateOnClickAndKeys(view, () -> openExperiment(experimentIndex, target));
    return view;
  }

  /**
   * A breadcrumb crumb whose text can be long (project titles allow 180 characters). It truncates
   * and keeps the full text reachable via the tooltip, so the crumb never pushes the experiment
   * views out of the strip.
   */
  private Span crumb(String text, Runnable onClick) {
    Span span = new Span(text);
    span.addClassName("pt-crumb");
    span.addClassName("pt-crumb--link");
    span.getElement().setAttribute("title", text);
    span.getElement().setAttribute("role", "link");
    span.getElement().setAttribute("tabindex", "0");
    activateOnClickAndKeys(span, onClick);
    return span;
  }

  // ── Body router ─────────────────────────────────────────────────────────

  private Div buildBody() {
    Div wrapper = new Div();
    wrapper.addClassName("pt-shell");
    switch (page) {
      case SUMMARY -> wrapper.add(identityBand(), progressBand(emptyState),
          experimentsBand(emptyState), metadataBand());
      case SAMPLES -> wrapper.add(projectSamplesPage());
      case DATASETS -> wrapper.add(datasetsPage());
      case ACCESS -> wrapper.add(accessPage());
      case ONTOLOGY -> wrapper.add(ontologyPage());
      case SETTINGS -> wrapper.add(settingsPage());
      case DESIGN -> wrapper.add(designPage());
      case EXP_SAMPLES -> wrapper.add(samplesPage());
      case EXP_DATASET -> wrapper.add(datasetPage());
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

    // Contacts sit between the title/meta block and the prose on purpose: who owns the project is
    // the kind of fact a reader scans for, and below a three-line abstract it read as an appendix.
    // Keeping it in the heading column also leaves the right-hand action column free for actions.
    heading.add(contactsRow());

    // The objective sits under the identity block: expanded, clamped to three lines.
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
    return band;
  }

  /**
   * Contacts sit directly under the title so the people responsible for a project are visible without
   * scrolling. Each contact is compressed to one line: role abbreviation as a badge, the person's
   * name, and two icon links. The ORCID iD itself is deliberately not shown as text — it is visual
   * clutter for everyone but the person who owns it — so the ORCID logo carries the link to the
   * public record instead. The envelope carries a {@code mailto:} link, keeping the address
   * available on hover/focus without spending a line on it.
   */
  private Div contactsRow() {
    Div people = new Div();
    people.addClassName("pt-people");
    people.add(textSpan("Contacts", "pt-people__label"));
    for (Contact contact : projectContacts()) {
      people.add(contactChip(contact));
    }
    // A link, not an editor: editing project metadata is rare and never urgent, so it belongs on the
    // Settings page rather than in a dialog beside the title. Scoped to what it edits, per the plan's
    // "scope edit labels" rule, and it actually navigates — the landing page stays read-only.
    people.add(settingsLink("Edit contacts", CONTACTS_SECTION));
    return people;
  }

  private Button settingsLink(String label, String section) {
    Button link = new Button(label);
    link.addClassName("pt-btn");
    link.addClassName("pt-btn--text");
    link.addClickListener(event -> {
      this.settingsSection = section;
      navigate(Page.SETTINGS);
    });
    return link;
  }

  private List<Contact> projectContacts() {
    return List.of(
        new Contact("PI", "Principal investigator", "Steffen Greiner",
            "steffen.greiner@qbic.uni-tuebingen.de", "0009-0006-0929-9338"),
        new Contact("PM", "Project manager", "Wow Cool", "wow@cool.dude", null),
        new Contact("Resp.", "Project responsible", "Lena Vogt",
            "lena.vogt@qbic.uni-tuebingen.de", "0000-0002-1825-0097"));
  }

  /**
   * One contact, compressed to a single line: {@code [PI] Steffen Greiner · orcid · mail}. The role is
   * abbreviated because the full title is four times as wide and the abbreviation is unambiguous in
   * context; the full role is exposed to assistive tech and as a tooltip.
   */
  private Div contactChip(Contact contact) {
    Div chip = new Div();
    chip.addClassName("pt-contact-chip");

    Abbr role = new Abbr(contact.roleAbbreviation());
    role.addClassName("pt-contact-chip__role");
    role.getElement().setAttribute("title", contact.role());
    role.getElement().setAttribute("aria-label", contact.role());

    Span name = new Span(contact.name());
    name.addClassName("pt-contact-chip__name");

    chip.add(role, name);

    // No placeholder when an ORCID is missing: it sits in the middle of the chip and pulls the
    // name and the envelope apart, so it reads as a person/avatar rather than as absent data. A
    // missing iD is simply not shown; the row stays compact and the two present chips stay aligned.
    if (contact.hasOrcid()) {
      Anchor orcid = new Anchor(contact.orcidUrl());
      orcid.addClassName("pt-contact-chip__icon");
      orcid.setTarget(AnchorTarget.BLANK);
      orcid.getElement().setAttribute("title", "ORCID: " + contact.orcid());
      orcid.getElement().setAttribute("aria-label",
          "Open ORCID record %s of %s in a new tab".formatted(contact.orcid(), contact.name()));
      orcidLogo(orcid);
      chip.add(orcid);
    }

    Anchor mail = new Anchor("mailto:" + contact.email(),
        icon(VaadinIcon.ENVELOPE, "pt-contact-chip__glyph"));
    mail.addClassName("pt-contact-chip__icon");
    mail.getElement().setAttribute("title", contact.email());
    mail.getElement().setAttribute("aria-label", "Email %s at %s".formatted(contact.name(),
        contact.email()));
    chip.add(mail);

    return chip;
  }

  /**
   * Adds the ORCID logo to the given anchor once it is attached.
   *
   * <p>{@link OidcLogo} extends {@code Image} and resolves its source through a stream
   * resource, which requires a current {@code UI}. This prototype builds its whole body in the
   * constructor — before attachment — so the logo cannot be created eagerly: doing so throws
   * {@code IllegalStateException: No currently active UI found}. Deferring to the attach event
   * keeps the real {@code OidcLogo} component (and therefore visual parity with the app) while
   * letting the prototype stay constructible without a UI.</p>
   */
  private static void orcidLogo(Anchor anchor) {
    anchor.addAttachListener(event -> {
      if (anchor.getChildren().findAny().isEmpty()) {
        OidcLogo logo = new OidcLogo(OidcType.ORCID);
        logo.addClassName("pt-contact-chip__logo");
        anchor.add(logo);
      }
    });
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
    activateOnClickAndKeys(chip, this::showAttentionDetail);
    return chip;
  }

  private Span sepDot() {
    Span span = new Span("·");
    span.addClassName("pt-meta__sep");
    return span;
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
      // Numbers are legitimate here: this genuinely is a sequence. Done and attention use glyphs
      // instead of ordinals. The attention glyph is the same warning triangle the exception rows
      // use, because a bold "!" in a small circle is read as "1" (or "l") and made the third step
      // look like an unnumbered error rather than outstanding work.
      Span marker = new Span();
      marker.addClassName("pt-rail__marker");
      if (stage.state() == Stage.State.DONE) {
        marker.add(icon(VaadinIcon.CHECK, "pt-rail__glyph"));
      } else if (stage.state() == Stage.State.ATTENTION) {
        marker.add(icon(VaadinIcon.WARNING, "pt-rail__glyph"));
      } else {
        marker.setText(String.valueOf(i + 1));
      }
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
    activateOnClickAndKeys(row, onClick);
    return row;
  }

  /**
   * Makes an element that carries {@code role="button"}/{@code role="link"} and
   * {@code tabindex="0"} actually reachable by keyboard.
   *
   * <p>A {@code tabindex} alone only puts the element in the tab order: the browser fires
   * {@code click} for a real {@code <button>} on Enter and Space, but for a {@code <div>} nothing
   * happens. Focus was reachable and activation was not, which is the most misleading version of
   * this bug — the element looks operable to a keyboard user and then silently does nothing.
   * Enter and Space are the two keys the button role promises, so both are wired here.</p>
   */
  private static void activateOnClickAndKeys(Component element, Runnable action) {
    element.getElement().addEventListener("click", event -> action.run());
    activateOnKeydown(element, action);
    // Records that this element carries the FULL activation contract (pointer + Enter/Space). It is
    // the one thing a test cannot otherwise observe from outside: a `role` attribute is just a
    // string, so without a marker an "every role=button is really operable" check has to reach into
    // Flow's internal listener registry. The marker keeps that assertion honest and public.
    element.getElement().setAttribute("data-activation", "full");
  }

  /**
   * Adds Enter/Space activation to an element that advertises {@code role="button"} /
   * {@code role="link"}.
   *
   * <p>Registered on the element rather than through {@code addClickListener} so the same helper
   * serves components whose type differs (Div, Span, ListItem) without a cast or type parameter.</p>
   */
  private static void activateOnKeydown(Component element, Runnable action) {
    // Enter is delivered as a filtered keydown rather than left to the browser's native
    // click-on-Enter: the rows here are <div>s, not <button>s, and a <div> with role="button" gets
    // no native activation at all. Space is listed alongside it because the button role promises
    // both; preventDefault stops Space from scrolling the page instead.
    element.getElement().addEventListener("keydown", event -> action.run())
        .setFilter("event.key === ' ' || event.key === 'Enter'")
        .addEventData("event.preventDefault()");
  }

  private static List<Stage> stages(boolean empty) {
    if (empty) {
      return List.of(
          new Stage("Experiment created", 0, "experiments", Stage.State.TODO, Page.DESIGN),
          new Stage("Samples registered", 0, "samples", Stage.State.TODO, Page.EXP_SAMPLES),
          new Stage("Dataset", 0, "measurements", Stage.State.TODO, Page.EXP_DATASET));
    }
    return List.of(
        new Stage("Experiment created", 3, "experiments", Stage.State.DONE, Page.DESIGN),
        new Stage("Samples registered", 84, "samples", Stage.State.DONE, Page.EXP_SAMPLES),
        // 36 = 18 of 24 + 18 of 30, i.e. the sum over experiments of those with primary data.
        new Stage("Dataset", 36, "measurements with data", Stage.State.ATTENTION,
            Page.EXP_DATASET));
  }

  // Experiments band ──────────────────────────────────────────────────────

  /**
   * The experiments section of the project landing page, and the single home of the experiment
   * list. With no experiment it is onboarding; otherwise every experiment is a card with an inline
   * snapshot. There is no separate experiments page and no second experiment list.
   *
   * <p>Deliberately carries no action of its own: "Add experiment" already sits in the hero next to
   * the title, and the same action twice on one page (here ~700px lower) reads as two different
   * controls and leaves the page without a single obvious primary.</p>
   */
  private Div experimentsBand(boolean empty) {
    Div band = new Div();
    band.addClassName("pt-band");

    Div head = new Div();
    head.addClassName("pt-band__head");
    H2 title = new H2();
    title.addClassName("pt-band__title");
    title.setText("Experiments");
    head.add(title);
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
          () -> openExperiment(index, Page.EXP_SAMPLES)));
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

    Div actions = new Div();
    actions.addClassName("pt-exp__actions");
    actions.add(button("Samples", false, () -> openExperiment(index, Page.EXP_SAMPLES)),
        button("Dataset", false, () -> openExperiment(index, Page.EXP_DATASET)));

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
   *
   * <p>The rows are the <b>most recently registered or modified</b> records, newest first, capped at
   * three. That is the only ordering this panel can honour and still mean something: the raw list
   * order (sample code) is already implied by the count next to it, so showing the three lowest
   * identifiers tells the user nothing they cannot infer. Recency answers the question the panel
   * exists for — <em>what has happened in this experiment since I last looked?</em> Clicking through
   * opens the full list, which is sortable by registration and modification time.</p>
   *
   * <p>Checked against the domain: {@code Sample} carries {@code registrationTime} and
   * {@code lastModified}, and {@code SamplePreviewSortKey} exposes both as sort keys. Measurements
   * only carry a registration date, so their panel ranks by registration; there is no measurement
   * "modified" indicator yet.</p>
   */
  /**
   * The snapshot panels: a count, a status line and the newest three records, opening the full view.
   *
   * <p>Carries its own "At a glance" heading. The heading was previously emitted by whichever page
   * happened to render the panels, so it existed on the experiment page and not on the project page,
   * and moving the panels broke it. Owning the heading means the two surfaces cannot disagree about
   * whether the section is titled.</p>
   */
  private Div experimentSnapshot(Experiment experiment, int index) {
    int missing = missingFor(index);
    int withData = Math.max(0, experiment.measurements() - missing);
    boolean hasSamples = experiment.samples() > 0;

    Div wrap = new Div();
    wrap.addClassName("pt-snapshot");
    wrap.add(snapshotPanel("Samples", experiment.samples(), "samples",
        experiment.samplesActivity() == null ? "Nothing registered yet"
            : experiment.samplesActivity() + " · " + daysAgo(experiment.daysSinceActivity()),
        new String[]{"Sample ID", "Sample name", "Changed"},
        samplePreview(experiment), Page.EXP_SAMPLES, index,
        hasSamples ? "Registered" : "Not started", hasSamples ? "done" : "active"));
    // Parallel wording with the Samples panel: both status lines now read "<state> · <how long
    // ago>". Previously Samples showed recency and Dataset showed completeness, so the two panels
    // answered different kinds of question in the same slot.
    String datasetState;
    String datasetTone;
    if (!hasSamples) {
      datasetState = "Locked";
      datasetTone = "locked";
    } else if (experiment.measurements() == 0) {
      datasetState = "Ready to register";
      datasetTone = "active";
    } else if (missing > 0) {
      datasetState = "Needs attention";
      datasetTone = "attention";
    } else {
      datasetState = "Complete";
      datasetTone = "done";
    }
    wrap.add(snapshotPanel("Dataset", experiment.measurements(), "measurements",
        experiment.measurements() == 0 ? "Nothing registered yet"
            : withData + " of " + experiment.measurements() + " with primary data · "
                + daysAgo(experiment.daysSinceActivity()),
        new String[]{"Measurement ID", "Primary data", "Changed"},
        datasetPreview(experiment, missing), Page.EXP_DATASET, index,
        datasetState, datasetTone));

    // NOTE: no heading here. This method also renders the per-experiment panels on the project page,
    // where an "At a glance" heading per entry would repeat three times. The heading belongs to the
    // section that owns the snapshot, so the experiment page adds it and the project page does not.
    return wrap;
  }

  /** How many of an experiment's measurements still lack primary data. */
  private int missingFor(int index) {
    return issuesFor(index).stream()
        .filter(issue -> issue.target() == Page.EXP_DATASET)
        .mapToInt(AttentionIssue::affected)
        .findFirst().orElse(0);
  }

  /**
   * One data card: a label, a lifecycle state tag, a count, a status line, the newest three records
   * and a way to the full list. The same markup serves the experiment overview and the
   * per-experiment snapshot on the project page, so the two cannot drift into different idioms for
   * the same thing.
   */
  private Div snapshotPanel(String label, int count, String unit, String status, String[] columns,
      List<List<CellView>> rows, Page target, int index, String stateLabel, String stateTone) {
    Div panel = new Div();
    panel.addClassName("pt-snap");

    Div head = new Div();
    head.addClassName("pt-snap__head");
    Div identity = new Div();
    identity.addClassName("pt-snap__id");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("pt-snap__label");
    identity.add(labelSpan);
    if (stateLabel != null) {
      identity.add(stateTag(stateLabel, stateTone));
    }
    head.add(identity);

    Span open = new Span("Open " + label.toLowerCase());
    open.addClassName("pt-snap__open");
    head.add(open);

    Div metric = new Div();
    metric.addClassName("pt-snap__metric");
    Span value = new Span(String.valueOf(count));
    value.addClassName("pt-snap__value");
    Span unitSpan = new Span(unit);
    unitSpan.addClassName("pt-snap__unit");
    metric.add(value, unitSpan);

    Span statusSpan = new Span(status);
    statusSpan.addClassName("pt-snap__status");

    // The column header lives INSIDE the list, directly above the rows, so header and values share
    // one grid. As a sibling it inherited the flex column of the panel and floated between the
    // status line and the list, aligned by nothing in particular.
    Div list = new Div();
    list.addClassName("pt-snap__rows");
    Div columnHead = new Div();
    columnHead.addClassName("pt-snap__row");
    columnHead.addClassName("pt-snap__row--cols");
    for (String column : columns) {
      columnHead.add(cell(new CellView(column, null)));
    }
    list.add(columnHead);
    rows.forEach(rowCells -> {
      Div rowDiv = new Div();
      rowDiv.addClassName("pt-snap__row");
      rowCells.forEach(cellView -> rowDiv.add(cell(cellView)));
      list.add(rowDiv);
    });

    panel.add(head, metric, statusSpan, list);
    panel.getElement().setAttribute("role", "button");
    panel.getElement().setAttribute("tabindex", "0");
    activateOnClickAndKeys(panel, () -> openExperiment(index, target));
    return panel;
  }

  /** The glance region: one panel per pipeline step, in pipeline order, so the step strip and the
   * region below it are the same three things.
   *
   * <p>Previously the region carried only Samples and Dataset. Design therefore had no body slot at
   * all, and a design could be created while the page showed no new content anywhere: its payload
   * only appeared as the three loose cards at the very bottom, which have no heading and no link to
   * the Design step. Making the region a <em>subset</em> of the steps was the defect; this makes it a
   * 1:1 and gives Design a first-class slot.</p>
   */
  /**
   * The three most recently registered or modified samples, newest first.
   *
   * <p>Prototype-only canned data: the numbers are the highest identifiers and the ages are derived
   * from the experiment's activity, not read from a query. A real implementation orders by
   * {@code registrationTime}/{@code lastModified} descending and takes the first page.</p>
   */
  private List<List<CellView>> samplePreview(Experiment experiment) {
    if (experiment.samples() == 0) {
      return List.of(row(muted("No samples to show yet")));
    }
    String[] names = {"P1_ctrl_T0", "P1_treat_T0", "P1_ctrl_T1"};
    int shown = Math.min(3, experiment.samples());
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 0; i < shown; i++) {
      // Newest first, so the numbering counts down from the last registered sample.
      int number = experiment.samples() - i;
      rows.add(row(mono(String.format("Q2IMMU-%04d", number)),
          text(names[number % names.length]),
          muted(rowAge(experiment, i))));
    }
    return rows;
  }

  /**
   * The three most recently registered measurements with their primary data, newest first.
   *
   * <p>The newest measurements are the ones still awaiting data: the facility registers metadata
   * first and attaches primary data afterwards, so the backlog sits at the top of the registration
   * order. That makes the panel agree with the attention callout instead of contradicting it.</p>
   */
  private List<List<CellView>> datasetPreview(Experiment experiment, int missing) {
    if (experiment.measurements() == 0) {
      return List.of(row(muted("No measurements to show yet")));
    }
    int shown = Math.min(3, experiment.measurements());
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 0; i < shown; i++) {
      boolean miss = i < missing;
      rows.add(row(mono(String.format("Q2IMMU-MS-%03d", experiment.measurements() - i)),
          miss ? attention("Not attached") : done("3 files · 4.2 GB"),
          muted(rowAge(experiment, i))));
    }
    return rows;
  }

  /**
   * Prototype-only: the {@code index}-th newest row is dated backwards from the experiment's last
   * activity, so the rows stay consistent with the status line above them.
   */
  private static String rowAge(Experiment experiment, int index) {
    return daysAgo(experiment.daysSinceActivity() + index);
  }

  /** Prototype-only relative age label: {@code just now}, {@code 1 day ago}, {@code n days ago}. */
  private static String daysAgo(int days) {
    if (days <= 0) {
      return "just now";
    }
    return days + (days == 1 ? " day ago" : " days ago");
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
    activateOnClickAndKeys(facet, onClick);
    return facet;
  }

  // ── Experiments hub and pipeline gating ──────────────────────────────────

  /** A step in the experiment pipeline; locked means its prerequisite is missing. */
  private enum StepState { DONE, ACTIVE, ATTENTION, LOCKED }

  /**
   * A step in the experiment pipeline: an explicit ordinal, a label, a detail line and the state.
   *
   * <p>The ordinal is not decoration. The four cards are a genuine sequence with prerequisites, so
   * numbered markers state the ordering that tint and left-to-right position alone could not; the
   * state is carried by the marker glyph (✓ / ! / ●), never by colour alone.</p>
   *
   * @param action an action label that makes a non-DONE step read as a route rather than a status,
   *               or {@code null} when the step is complete or merely informational
   */
  private record Step(String ordinal, String label, String detail, StepState state, Page target,
      String action) {}

  /** The project-level pipeline, with locked downstream steps while no experiment exists. */
  private List<Step> projectPipeline() {
    if (emptyState) {
      return List.of(
          new Step("1", "Experiment", "Create one to begin", StepState.ACTIVE, Page.DESIGN,
              null),
          new Step("2", "Samples", "Locked until an experiment exists", StepState.LOCKED,
              Page.EXP_SAMPLES, null),
          new Step("3", "Dataset", "Locked until samples exist", StepState.LOCKED,
              Page.EXP_DATASET, null));
    }
    return List.of(
        new Step("1", "Experiment", "3 created", StepState.DONE, Page.DESIGN, null),
        new Step("2", "Samples", "84 registered", StepState.DONE, Page.EXP_SAMPLES, null),
        new Step("3", "Dataset", "Missing data for 18 measurements", StepState.ATTENTION,
            Page.EXP_DATASET, "Open dataset"));
  }

  /**
   * The experiment overview, rendered on the experiment landing: the design first — its payload, or
   * the invitation that creates it — then the two data surfaces the design scopes.
   *
   * <p>These cards <em>are</em> the navigation. Each states what it holds and what is still to be
   * done, and opens the page that does it, so "what is next" is answered once, in the body. They
   * replace the numbered step rail, which restated the same three states a second time inside a page
   * the context strip was already traversing.</p>
   */
  private Div experimentOverview(Experiment experiment) {
    Div wrap = new Div();
    wrap.addClassName("pt-overview");
    wrap.add(designOverview());
    wrap.add(dataOverview(experiment));
    return wrap;
  }

  /**
   * The design, which leads the overview. Its payload when it exists; otherwise what it is and what
   * to do. The action itself stays in the page header, so the overview never carries a second
   * primary action for the same thing.
   */
  private Div designOverview() {
    Div band = new Div();
    band.addClassName("pt-band");
    band.addClassName("pt-band--glance");

    Div head = new Div();
    head.addClassName("pt-band__head");
    H2 title = new H2();
    title.addClassName("pt-band__title");
    title.setText("Design");
    head.add(title, stateTag(designDefined ? "Defined" : "Not defined yet",
        designDefined ? "done" : "active"));
    band.add(head);

    if (!designDefined) {
      Span note = new Span("No design yet. The experimental groups, variables and confounding "
          + "variables scope every sample and measurement in this experiment.");
      note.addClassName("pt-note");
      band.add(note);
      return band;
    }

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

  /** The two data surfaces at a glance, under the design that scopes them. */
  private Div dataOverview(Experiment experiment) {
    Div band = new Div();
    band.addClassName("pt-band");
    band.addClassName("pt-band--glance");

    Div head = new Div();
    head.addClassName("pt-band__head");
    H2 title = new H2();
    title.addClassName("pt-band__title");
    title.setText("Data at a glance");
    Span hint = new Span("What is registered, and what to do next.");
    hint.addClassName("pt-band__hint");
    head.add(title, hint);
    band.add(head);

    Div grid = new Div();
    grid.addClassName("pt-snapshot");
    grid.add(samplesOverview(experiment));
    grid.add(datasetOverview(experiment));
    band.add(grid);
    return band;
  }

  private Div samplesOverview(Experiment experiment) {
    if (experiment.samples() == 0) {
      return overviewInvite("Samples", "Not started", "active",
          "Samples carry the experimental metadata — species, tissue, treatment and replicates. "
              + "Upload the sample template to register them.",
          "Register samples", Page.EXP_SAMPLES);
    }
    return snapshotPanel("Samples", experiment.samples(), "samples",
        experiment.samplesActivity() == null ? "Nothing registered yet"
            : experiment.samplesActivity() + " · " + daysAgo(experiment.daysSinceActivity()),
        new String[]{"Sample ID", "Sample name", "Changed"},
        samplePreview(experiment), Page.EXP_SAMPLES, experimentIndex, "Registered", "done");
  }

  private Div datasetOverview(Experiment experiment) {
    int missing = missingFor(experimentIndex);
    if (experiment.samples() == 0) {
      // Locked, and it says so: the tag carries the padlock the gate already uses and the card's
      // border is dashed, the prototype's language for "prerequisite missing". A dataset cannot
      // exist before the samples it describes, so the card routes to the step that unblocks it
      // instead of offering a form that cannot succeed.
      return overviewInvite("Dataset", "Locked", "locked",
          "Measurements are made on samples. Register samples first; the dataset opens when they "
              + "exist.",
          "Go to samples", Page.EXP_SAMPLES);
    }
    if (experiment.measurements() == 0) {
      return overviewInvite("Dataset", "Ready to register", "active",
          "Register the measurement metadata and attach the primary data produced by the facility.",
          "Register measurements", Page.EXP_DATASET);
    }
    return snapshotPanel("Dataset", experiment.measurements(), "measurements",
        missing > 0
            ? missing + " of " + experiment.measurements() + " still need primary data"
            : "All " + experiment.measurements() + " have primary data",
        new String[]{"Measurement ID", "Primary data", "Changed"},
        datasetPreview(experiment, missing), Page.EXP_DATASET, experimentIndex,
        missing > 0 ? "Needs attention" : "Complete", missing > 0 ? "attention" : "done");
  }

  /**
   * A data card for a surface that is empty or blocked: what it is for, what to do next, and the one
   * action that does it. Unlike the populated cards it is not {@code role=button} — it contains a
   * real button, and two targets for one gesture is a worse affordance than one.
   */
  private Div overviewInvite(String label, String state, String tone, String text,
      String actionLabel, Page target) {
    Div panel = new Div();
    panel.addClassName("pt-snap");
    panel.addClassName("pt-snap--invite");
    if ("locked".equals(tone)) {
      panel.addClassName("pt-snap--locked");
    }

    Div head = new Div();
    head.addClassName("pt-snap__head");
    Div identity = new Div();
    identity.addClassName("pt-snap__id");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("pt-snap__label");
    identity.add(labelSpan, stateTag(state, tone));
    head.add(identity);
    panel.add(head);

    Span body = new Span(text);
    body.addClassName("pt-snap__text");
    panel.add(body);

    Div actions = new Div();
    actions.addClassName("pt-snap__actions");
    actions.add(button(actionLabel, false, () -> openExperiment(experimentIndex, target)));
    panel.add(actions);
    return panel;
  }

  /**
   * A state as a tag: text plus a tint, never a tint alone. The locked tag additionally carries the
   * padlock the gate already uses, so "prerequisite missing" reads as a state rather than as a bare
   * label that happens to say "Locked".
   */
  private Span stateTag(String text, String tone) {
    Span tag = new Span();
    tag.addClassName("pt-tag");
    tag.addClassName("pt-tag--" + tone);
    if ("locked".equals(tone)) {
      Icon lock = VaadinIcon.LOCK.create();
      lock.addClassName("pt-tag__icon");
      tag.add(lock);
    }
    tag.add(new Span(text));
    return tag;
  }

  /**
   * Renders the ordered steps. A locked step is deliberately neutral (dashed, muted) rather than
   * amber, so it cannot be confused with an attention gap.
   *
   * <p>Used by the project page's onboarding pipeline. It no longer marks a "current" step: that
   * signal existed so the strip could double as the experiment-view switcher, and the experiment
   * overview now navigates with its own cards.</p>
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

      // An actionable step says so. Without this the amber card was a status the user had to
      // interpret and discover was clickable; "Open dataset" states both the problem (the detail
      // line) and the way out.
      if (step.action() != null) {
        Span action = new Span(step.action());
        action.addClassName("pt-step__action");
        node.add(action);
      }

      // Presence is carried by the marker glyph, never by the colour alone: the ordinal says which
      // step this is, and the detail/action spell out what to do. The tint is redundant
      // reinforcement for people who perceive it.
      Span ordinal = new Span(step.ordinal());
      ordinal.addClassName("pt-step__ordinal");
      node.add(ordinal);

      if (navigable && step.state() != StepState.LOCKED && step.target() != null) {
        node.getElement().setAttribute("role", "button");
        node.getElement().setAttribute("tabindex", "0");
        activateOnClickAndKeys(node,
            () -> openExperiment(experimentIndex, step.target()));
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

  /**
   * The experiment's dataset. Measurement metadata and registered primary data are shown together,
   * because a measurement and its data are one fact. A dataset is 1:1 with its experiment.
   */
  private Div datasetPage() {
    Experiment experiment = emptyState ? null : selectedExperiment();
    if (experiment == null) {
      return emptyPage("Dataset", "No experiment yet",
          "No measurements registered yet",
          "Register the measurement metadata and attach the primary data produced by the facility.",
          button("Register measurements", true, null), button("Download template", false, null));
    }
    if (experiment.samples() == 0) {
      return lockedPage("Dataset", experiment.name() + " · 0 samples",
          "Register samples first",
          "Measurements are made on samples. Register the samples for this experiment before "
              + "adding measurement metadata.",
          "Go to samples", Page.EXP_SAMPLES);
    }
    if (experiment.measurements() == 0) {
      return emptyPage("Dataset", experiment.name(),
          "No measurements registered yet",
          "Register the measurement metadata and attach the primary data produced by the facility.",
          button("Register measurements", true, null), button("Download template", false, null));
    }
    int missing = missingFor(experimentIndex);
    String issueText = issuesFor(experimentIndex).stream()
        .filter(issue -> issue.target() == Page.EXP_DATASET)
        .map(AttentionIssue::text)
        .findFirst()
        .orElse(null);

    List<String> types = typesForExperiment(experiment);
    if (!types.contains(datasetType)) {
      datasetType = types.get(0);
    }

    Div band = pageBand("Dataset", experiment.name() + " · measurements and primary data",
        button("Register measurements", true, null),
        button(showAllColumns ? "Fewer columns" : "Show all columns", false,
            () -> {
              showAllColumns = !showAllColumns;
              refresh();
            }));
    band.add(datasetFacts(experiment, missing));

    if (types.size() > 1) {
      band.add(typeTabs(types));
    }

    if (showAllColumns) {
      band.add(table(fullDatasetHeaders(), fullDatasetRowsForExperiment(experiment)));
    } else {
      List<Detail> details = experimentMeasurementDetails(experiment, datasetType, missing);
      band.add(clickableTable(new String[]{"Measurement ID", "Measurement name", "Type",
              "Instrument", "Samples", "Primary data"},
          experimentMeasurementRows(experiment, datasetType, missing),
          index -> {
            if (index < details.size()) {
              openDetail(details.get(index));
            }
          }));
    }
    band.add(note(issueText != null
        ? issueText + ". Use “Attach data” on a row to link the primary data."
        : "Identity and status only — select a row for the full measurement metadata. "
            + "Type-specific fields live in the side panel."));

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
    // Wrapped in a card of its own. Rendered bare into the band, the four labels floated free
    // between the glance panels and the measurement table with no container, no divider and no
    // background — they sat directly under the Samples panel, so "Kind: Primary" read as a SAMPLE
    // metric. A surface makes plain that these numbers describe the dataset.
    Div card = new Div();
    card.addClassName("pt-factcard");
    Span cap = new Span("Dataset summary");
    cap.addClassName("pt-factcard__title");
    card.add(cap);
    Div facts = new Div();
    facts.addClassName("pt-facts");
    facts.add(fact("Kind", "Primary"));
    facts.add(fact("Measurements with data", withData + " of " + experiment.measurements()));
    facts.add(fact("Files", String.valueOf(withData * 3)));
    facts.add(fact("Total size", withData * 4 + " GB"));
    card.add(facts);
    return card;
  }

  /** One row per measurement: metadata and its primary data together. */
  private List<List<CellView>> measurementDataRows(Experiment experiment, int missing) {
    int shown = Math.min(experiment.measurements(), Math.max(8, missing));
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 1; i <= shown; i++) {
      boolean miss = i <= missing;
      rows.add(row(
          mono(String.format("Q2IMMU-%s-%03d", typeCode(datasetType), i)),
          text("Measurement " + i),
          text(datasetType),
          text(instrumentFor(datasetType)),
          text("2"),
          miss ? attention("0") : text("3"),
          miss ? muted("—") : text("4.2 GB"),
          miss ? attention("Not attached") : done("Registered")));
    }
    return rows;
  }

  private List<List<CellView>> experimentMeasurementRows(Experiment experiment, String type,
      int missing) {
    int shown = Math.min(experiment.measurements(), Math.max(8, missing));
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 1; i <= shown; i++) {
      boolean miss = i <= missing;
      rows.add(row(
          mono(String.format("Q2IMMU-%s-%03d", typeCode(type), i)),
          text("Measurement " + i),
          text(type),
          text(instrumentFor(type)),
          text("2"),
          miss ? attention("Not attached") : done("Attached")));
    }
    return rows;
  }

  private List<Detail> experimentMeasurementDetails(Experiment experiment, String type,
      int missing) {
    int shown = Math.min(experiment.measurements(), Math.max(8, missing));
    List<Detail> details = new ArrayList<>();
    for (int i = 1; i <= shown; i++) {
      String id = String.format("Q2IMMU-%s-%03d", typeCode(type), i);
      details.add(new Detail("Measurement " + i, id + " · " + experiment.name(),
          List.of(type), measurementDetailGroups(type, id)));
    }
    return details;
  }

  /**
   * The experiment landing. It is the experiment's overview: the design leads, and the two data
   * surfaces it scopes follow. There is no separate "Design" page and no step rail — the overview
   * cards are the navigation.
   */
  private Div designPage() {
    Experiment experiment = emptyState ? null : selectedExperiment();
    if (experiment == null) {
      return emptyPage("Overview", "No experiment yet",
          "No experiment yet",
          "Create an experiment to define its design and register its samples.",
          button("Create experiment", true, () -> createOrAddExperiment(true)));
    }
    Div band = pageBand("Overview", experiment.name(),
        button(designDefined ? "Edit design" : "Define design", true, () -> {
          // The design action is scoped and in-place: pressing it changes THIS page's design state
          // rather than routing away to an editor whose result the page would not show.
          designDefined = true;
          refresh();
        }));
    band.add(experimentOverview(experiment));
    return band;
  }

  /**
   * Project-level Datasets: identity + status list, segmented by measurement type (NGS /
   * Proteomics / Immunopeptidomics) exactly like the production solution. Type-specific attributes
   * are not columns; they live in the side panel. The full table is an opt-in power mode.
   */
  /**
   * Project-level Datasets: a dataset is 1:1 with an experiment, so the page starts with one
   * overview row per experiment (kind, counts, primary-data completeness, status). Opening a row
   * dives into that experiment's measurement-level view.
   */
  private Div datasetsPage() {
    if (emptyState) {
      return emptyPage("Datasets", "No experiment yet",
          "No measurements yet",
          "Create an experiment first; its measurement metadata and primary data follow from it.",
          button("Create experiment", true, () -> createOrAddExperiment(true)));
    }
    Div band = pageBand("Datasets",
        "One dataset per experiment — measurement metadata and registered primary data together",
        button("Export", false, null));

    band.add(datasetOverviewStats());
    band.add(clickableTable(
        new String[]{"Experiment", "Kind", "Measurements", "Primary data", "Files", "Size",
            "Status"},
        datasetExperimentRows(),
        index -> openExperiment(index, Page.EXP_DATASET)));
    band.add(note("A dataset is 1:1 with its experiment. Open one to dive into its measurements "
        + "and primary data."));
    band.add(connectedDatasetsSection());
    return band;
  }

  private Div connectedDatasetsSection() {
    Div section = new Div();
    Div head = new Div();
    head.addClassName("pt-band__head");
    H3 title = new H3();
    title.addClassName("pt-band__title");
    title.setText("Connected datasets");
    head.add(title, new Span("Hosted elsewhere and linked here."));
    section.add(head);
    section.add(table(new String[]{"Title", "Repository", "Access", "Related to", "Connected on"},
        List.of(
            row(text("Proteome reanalysis — immunopeptidome"), text("Zenodo"), done("Open"),
                text("Immunopeptidomics pilot"), text("2 days ago")),
            row(text("Preliminary QC report"), text("InvenioRDM"), attention("Restricted"),
                text("Project"), text("5 days ago")),
            row(text("Reference peptide list"), text("Zenodo"), done("Open"), muted("—"),
                text("3 weeks ago")))));
    section.add(note("“Connected” states only that the dataset is linked here. It may be an "
        + "analysis result, related material, or something else — unclassified."));
    return section;
  }

  private Div typeTabs(List<String> types) {
    Div tabs = new Div();
    tabs.addClassName("pt-tabs");
    for (String type : types) {
      Button tab = new Button(type);
      tab.addClassName("pt-tab");
      if (type.equals(datasetType)) {
        tab.addClassName("pt-tab--active");
      }
      tab.addClickListener(event -> {
        datasetType = type;
        refresh();
      });
      tabs.add(tab);
    }
    return tabs;
  }

  private String[] datasetIdentityHeaders() {
    return new String[]{"Measurement ID", "Measurement name", "Experiment", "Instrument",
        "Samples", "Primary data", "Registration"};
  }

  private List<List<CellView>> datasetIdentityRows() {
    List<List<CellView>> rows = new ArrayList<>();
    int counter = 1;
    for (Experiment exp : experimentsForType(datasetType)) {
      if (datasetFilter >= 0 && !POPULATED_EXPERIMENTS.get(datasetFilter).equals(exp)) {
        continue;
      }
      int missing = missingFor(POPULATED_EXPERIMENTS.indexOf(exp));
      int shown = Math.min(exp.measurements(), 6);
      for (int i = 1; i <= shown; i++) {
        boolean miss = i <= missing;
        rows.add(row(
            mono(String.format("Q2IMMU-%s-%03d", typeCode(datasetType), counter++)),
            text("Measurement " + i),
            text(exp.name()),
            text(instrumentFor(datasetType)),
            text(String.valueOf((i % 3) + 1)),
            miss ? attention("Not attached") : done("Attached"),
            text("12 Sep 2026")));
      }
    }
    if (rows.isEmpty()) {
      rows.add(row(muted("No measurements of this type.")));
    }
    return rows;
  }

  private List<Detail> datasetDetails() {
    List<Detail> details = new ArrayList<>();
    int counter = 1;
    for (Experiment exp : experimentsForType(datasetType)) {
      if (datasetFilter >= 0 && !POPULATED_EXPERIMENTS.get(datasetFilter).equals(exp)) {
        continue;
      }
      int shown = Math.min(exp.measurements(), 6);
      for (int i = 1; i <= shown; i++) {
        String id = String.format("Q2IMMU-%s-%03d", typeCode(datasetType), counter++);
        details.add(new Detail("Measurement " + i, id + " · " + datasetType,
            List.of(datasetType, "Primary data attached"),
            measurementDetailGroups(datasetType, id)));
      }
    }
    return details;
  }

  private String[] fullDatasetHeaders() {
    return switch (datasetType) {
      case "NGS" -> new String[]{"Measurement ID", "Measurement name", "Samples", "Facility",
          "Instrument", "Organisation", "Read type", "Library kit", "Flow cell", "Run protocol",
          "Registration", "Comment"};
      case "Proteomics" -> new String[]{"Measurement ID", "Measurement name", "Samples",
          "Organisation", "Facility", "MS device", "Technical replicate", "Digestion enzyme",
          "Digestion method", "Injection volume", "LCMS method", "LC column", "Enrichment",
          "Registration", "Comment"};
      default -> new String[]{"Measurement ID", "Measurement name", "Samples", "Organisation",
          "Facility", "Instrument", "MHC antibody", "MHC typing method", "Enrichment method",
          "LCMS method", "LC column", "Data acquisition", "Mass range", "Retention time range",
          "Charge range", "Ion mobility range", "Sample mass (mg)", "Sample volume (µl)",
          "Cycle/fraction", "Prep date", "MS run date", "Registration", "Comment"};
    };
  }

  private List<List<CellView>> fullDatasetRows() {
    List<List<CellView>> rows = new ArrayList<>();
    int counter = 1;
    for (Experiment exp : experimentsForType(datasetType)) {
      if (datasetFilter >= 0 && !POPULATED_EXPERIMENTS.get(datasetFilter).equals(exp)) {
        continue;
      }
      int shown = Math.min(exp.measurements(), 4);
      for (int i = 1; i <= shown; i++) {
        rows.add(fullDatasetRow(i, counter++));
      }
    }
    if (rows.isEmpty()) {
      rows.add(row(muted("No measurements of this type.")));
    }
    return rows;
  }

  private List<CellView> fullDatasetRow(int label, int counter) {
    String id = String.format("Q2IMMU-%s-%03d", typeCode(datasetType), counter);
    return switch (datasetType) {
      case "NGS" -> row(mono(id), text("Measurement " + label), text("2"),
          text("QBiC MS Facility"), text("Illumina NovaSeq"), text("QBiC"), text("Paired-end"),
          text("TruSeq"), text("NovaSeq S1"), text("Runs 1–2"), text("12 Sep 2026"),
          text("—"));
      case "Proteomics" -> row(mono(id), text("Measurement " + label), text("2"), text("QBiC"),
          text("QBiC MS Facility"), text("timsTOF Pro"), text("TR1"), text("Trypsin"),
          text("FASP"), text("2 µl"), text("MSV035"), text("Aurora"), text("—"),
          text("12 Sep 2026"), text("—"));
      default -> row(mono(id), text("Measurement " + label), text("2"), text("QBiC"),
          text("QBiC MS Facility"), text("timsTOF Pro"), text("W6/32"), text("DNA seq-based"),
          text("immunoaffinity purification"), text("MSV035"), text("Aurora"), text("DIA"),
          text("300–1800 m/z"), text("60 min"), text("2–4"), text("0.6–1.6"), text("2.5"),
          text("20"), text("Fraction01"), text("11 Sep 2026"), text("12 Sep 2026"),
          text("12 Sep 2026"), text("—"));
    };
  }

  private List<DetailGroup> measurementDetailGroups(String type, String id) {
    if (type.equals("NGS")) {
      return List.of(
          group("Measurement Identity", field("QBiC Measurement Id", id),
              field("Measurement Name", "NGS run")),
          group("Instrument", field("Instrument", "Illumina NovaSeq"),
              field("Read type", "Paired-end"), field("Library kit", "TruSeq"),
              field("Flow cell", "NovaSeq S1"), field("Run protocol", "Runs 1–2")),
          group("Organisational Provenance", field("Facility", "QBiC MS Facility"),
              field("Organisation", "QBiC")),
          group("Notes", field("Comment", "—")));
    }
    if (type.equals("Proteomics")) {
      return List.of(
          group("Measurement Identity", field("QBiC Measurement Id", id),
              field("Measurement Name", "PX run")),
          group("Sample Properties", field("Samples", "2")),
          group("Instrument", field("MS Device", "timsTOF Pro"),
              field("LCMS Method", "MSV035"), field("LC Column", "Aurora")),
          group("Sample Preparation", field("Digestion Enzyme", "Trypsin"),
              field("Digestion Method", "FASP"), field("Enrichment", "—"),
              field("Injection Volume", "2 µl")),
          group("Organisational Provenance", field("Facility", "QBiC MS Facility"),
              field("Organisation", "QBiC")),
          group("Notes", field("Comment", "—")));
    }
    return List.of(
        group("Sample Identity", field("QBiC Sample Id", "Q2IMMU-0001"),
            field("Sample Name", "P1_ctrl_T0")),
        group("Measurement Identity", field("QBiC Measurement Id", id),
            field("Measurement Name", "IP run")),
        group("Sample Properties", field("Sample Mass (mg)", "2.5"),
            field("Sample Volume (µl)", "20"), field("Cycle/Fraction Name", "Fraction01")),
        group("Analyte Properties", field("Enrichment method", "immunoaffinity purification"),
            field("MHC Antibody", "W6/32"),
            field("MHC Typing Method", "DNA seq-based with Optitype")),
        group("Organisational Provenance", field("Facility", "QBiC MS Facility"),
            field("Organisation URL", "https://ror.org/03a1kwz48")),
        group("Instrument", field("Data Acquisition", "DIA"),
            field("Instrument", "timsTOF Pro"), field("LCMS Method", "MSV035"),
            field("LC Column", "Aurora Ultimate")),
        group("Run Parameters", field("Charge range", "2–4"),
            field("Mass range (m/z)", "300–1800"),
            field("Retention time range (min)", "60"),
            field("Ion mobility range (1/k0)", "0.6–1.6")),
        group("Notes", field("Comment", "—")));
  }

  private static List<Experiment> experimentsForType(String type) {
    return POPULATED_EXPERIMENTS.stream()
        .filter(exp -> exp.domains().stream().anyMatch(domain -> domainToType(domain).equals(type)))
        .toList();
  }

  private static String domainToType(String domain) {
    return switch (domain) {
      case "Genomics" -> "NGS";
      case "Proteomics" -> "Proteomics";
      default -> "Immunopeptidomics";
    };
  }

  private static String typeCode(String type) {
    return switch (type) {
      case "NGS" -> "NGS";
      case "Proteomics" -> "PXM";
      default -> "IPX";
    };
  }

  private static String instrumentFor(String type) {
    return type.equals("NGS") ? "Illumina NovaSeq" : "timsTOF Pro";
  }

  private static List<String> typesForExperiment(Experiment experiment) {
    List<String> types = new ArrayList<>();
    for (String domain : experiment.domains()) {
      String type = domainToType(domain);
      if (!types.contains(type)) {
        types.add(type);
      }
    }
    if (types.isEmpty()) {
      types.add("Immunopeptidomics");
    }
    return types;
  }

  private Div sampleOverviewStats() {
    int total = POPULATED_EXPERIMENTS.stream().mapToInt(Experiment::samples).sum();
    int experiments = (int) POPULATED_EXPERIMENTS.stream()
        .filter(exp -> exp.samples() > 0)
        .count();
    int notMeasured = experiments;
    int measured = Math.max(0, total - notMeasured);
    Div stats = new Div();
    stats.addClassName("pt-rollup");
    stats.add(stat(total, "samples"));
    stats.add(stat(experiments, "experiments"));
    stats.add(stat(measured, "measured"));
    stats.add(stat(notMeasured, "not measured"));
    return stats;
  }

  private Div datasetOverviewStats() {
    int totalMeasurements = POPULATED_EXPERIMENTS.stream().mapToInt(Experiment::measurements).sum();
    int withData = 0;
    int missing = 0;
    for (int i = 0; i < POPULATED_EXPERIMENTS.size(); i++) {
      int gap = missingFor(i);
      missing += gap;
      withData += Math.max(0, POPULATED_EXPERIMENTS.get(i).measurements() - gap);
    }
    Div stats = new Div();
    stats.addClassName("pt-rollup");
    stats.add(stat(POPULATED_EXPERIMENTS.size(), "datasets"));
    stats.add(stat(totalMeasurements, "measurements"));
    stats.add(stat(withData, "with primary data"));
    stats.add(stat(missing, "missing data"));
    return stats;
  }

  private Div stat(int value, String label) {
    Div fact = new Div();
    fact.addClassName("pt-rollup__fact");
    Span number = new Span(String.valueOf(value));
    number.addClassName("pt-rollup__value");
    Span unit = new Span(label);
    unit.addClassName("pt-rollup__label");
    fact.add(number, unit);
    return fact;
  }

  private List<List<CellView>> datasetExperimentRows() {
    List<List<CellView>> rows = new ArrayList<>();
    for (int i = 0; i < POPULATED_EXPERIMENTS.size(); i++) {
      Experiment exp = POPULATED_EXPERIMENTS.get(i);
      if (!exp.isStarted()) {
        rows.add(row(text(exp.name()), text("Primary"), muted("0 measurements"), muted("—"),
            muted("—"), muted("—"), muted("Not started")));
        continue;
      }
      int gap = missingFor(i);
      int withData = Math.max(0, exp.measurements() - gap);
      rows.add(row(
          text(exp.name()),
          text("Primary"),
          text(exp.measurements() + " measurements"),
          text(withData + " of " + exp.measurements()),
          text((withData * 3) + " files"),
          text((withData * 4) + " GB"),
          gap > 0 ? attention("Missing data") : done("Complete")));
    }
    return rows;
  }

  private List<List<CellView>> fullDatasetRowsForExperiment(Experiment experiment) {
    List<List<CellView>> rows = new ArrayList<>();
    int shown = Math.min(experiment.measurements(), Math.max(8, missingFor(experimentIndex)));
    for (int i = 1; i <= shown; i++) {
      rows.add(fullDatasetRow(i, i));
    }
    return rows;
  }


  /**
   * Project-level Samples: identity + status list, read-only and exportable, with quick filters.
   * Selecting a row opens the full sample in the side panel; the full table is opt-in.
   */
  private Div projectSamplesPage() {
    if (emptyState) {
      return emptyPage("Samples", "No experiment yet",
          "No samples yet",
          "Samples belong to an experiment. Create the first experiment, then register samples "
              + "there.",
          button("Create experiment", true, () -> createOrAddExperiment(true)));
    }
    Div band = pageBand("Samples", "Every sample in the project, across all experiments",
        button("Export", false, null),
        button(showAllColumns ? "Fewer columns" : "Show all columns", false,
            () -> {
              showAllColumns = !showAllColumns;
              refresh();
            }));
    band.add(sampleOverviewStats());
    band.add(experimentFilter(sampleFilter, value -> {
      sampleFilter = value;
      refresh();
    }));

    if (showAllColumns) {
      band.add(table(fullSampleHeaders(), fullSampleRows()));
      band.add(note("Full table (power mode) for on-screen metadata curation."));
      return band;
    }

    List<Detail> details = sampleDetails();
    band.add(clickableTable(sampleIdentityHeaders(), sampleIdentityRows(),
        index -> {
          if (index < details.size()) {
            openDetail(details.get(index));
          }
        }));
    band.add(note("Identity and status only — select a row for the full sample. Registering and "
        + "editing samples happens inside an experiment."));
    return band;
  }

  private String[] sampleIdentityHeaders() {
    return new String[]{"Sample ID", "Sample name", "Experiment", "Condition", "Species",
        "Status"};
  }

  private String[] fullSampleHeaders() {
    return new String[]{"Sample ID", "Sample name", "Experiment", "Analysis",
        "Biological replicate", "Batch", "Condition", "Species", "Specimen", "Analyte",
        "Comment"};
  }

  private List<List<CellView>> sampleIdentityRows() {
    List<List<CellView>> rows = new ArrayList<>();
    for (int e = 0; e < POPULATED_EXPERIMENTS.size(); e++) {
      if (sampleFilter >= 0 && sampleFilter != e) {
        continue;
      }
      Experiment exp = POPULATED_EXPERIMENTS.get(e);
      int shown = Math.min(exp.samples(), 6);
      for (int i = 0; i < shown; i++) {
        rows.add(row(
            mono(sampleIdentity(e, i)),
            text(sampleName(i)),
            text(exp.name()),
            text(sampleCondition(i)),
            text("Homo sapiens"),
            i < Math.max(1, shown - 1) ? done("Measured") : attention("Not measured")));
      }
    }
    if (rows.isEmpty()) {
      rows.add(row(muted("No samples to show.")));
    }
    return rows;
  }

  private List<List<CellView>> fullSampleRows() {
    List<List<CellView>> rows = new ArrayList<>();
    for (int e = 0; e < POPULATED_EXPERIMENTS.size(); e++) {
      if (sampleFilter >= 0 && sampleFilter != e) {
        continue;
      }
      Experiment exp = POPULATED_EXPERIMENTS.get(e);
      int shown = Math.min(exp.samples(), 4);
      for (int i = 0; i < shown; i++) {
        rows.add(row(
            mono(sampleIdentity(e, i)),
            text(sampleName(i)),
            text(exp.name()),
            text("Immunopeptidomics"),
            text(String.valueOf((i % 3) + 1)),
            text("Batch1"),
            text(sampleCondition(i)),
            text("Homo sapiens"),
            text("Tumour organoid"),
            text("Peptide"),
            text("—")));
      }
    }
    if (rows.isEmpty()) {
      rows.add(row(muted("No samples to show.")));
    }
    return rows;
  }

  private List<Detail> sampleDetails() {
    List<Detail> details = new ArrayList<>();
    for (int e = 0; e < POPULATED_EXPERIMENTS.size(); e++) {
      if (sampleFilter >= 0 && sampleFilter != e) {
        continue;
      }
      Experiment exp = POPULATED_EXPERIMENTS.get(e);
      int shown = Math.min(exp.samples(), 6);
      for (int i = 0; i < shown; i++) {
        String id = sampleIdentity(e, i);
        details.add(new Detail(sampleName(i), id + " · " + exp.name(),
            List.of(sampleCondition(i)),
            List.of(
                group("Sample Identity",
                    field("QBiC Sample Id", id),
                    field("Sample Name", sampleName(i))),
                group("Sample Properties",
                    field("Condition", sampleCondition(i)),
                    field("Biological Replicate", String.valueOf((i % 3) + 1)),
                    field("Batch", "Batch1")),
                group("Sample Origin",
                    field("Species", "Homo sapiens"),
                    field("Specimen", "Tumour organoid"),
                    field("Analyte", "Peptide")),
                group("Analysis", field("Analysis to be performed", "Immunopeptidomics")),
                group("Notes", field("Comment", "—")))));
      }
    }
    return details;
  }

  private static String sampleIdentity(int experimentIndex, int offset) {
    return String.format("Q2IMMU-%04d", experimentIndex * 100 + offset + 1);
  }

  private static String sampleCondition(int offset) {
    return offset % 2 == 0 ? "Untreated" : "Treated";
  }

  private static String sampleName(int offset) {
    String[] names = {"P1_ctrl_T0", "P1_treat_T0", "P1_ctrl_T1", "P2_ctrl_T0"};
    return names[offset % names.length];
  }

  /** Quick-filter buttons over the experiments; -1 means "all experiments". */
  private Div experimentFilter(int selected, java.util.function.IntConsumer onSelect) {
    Div filters = new Div();
    filters.addClassName("pt-filters");
    filters.add(textSpan("Filter", "pt-filters__label"));
    filters.add(filterChip("All experiments", selected < 0, () -> onSelect.accept(-1)));
    for (int i = 0; i < POPULATED_EXPERIMENTS.size(); i++) {
      final int index = i;
      filters.add(filterChip(POPULATED_EXPERIMENTS.get(i).name(), selected == i,
          () -> onSelect.accept(index)));
    }
    return filters;
  }

  private Span filterChip(String text, boolean selected, Runnable onClick) {
    Span chip = new Span(text);
    chip.addClassName("pt-filter");
    if (selected) {
      chip.addClassName("pt-filter--selected");
    }
    chip.getElement().setAttribute("role", "button");
    chip.getElement().setAttribute("tabindex", "0");
    activateOnClickAndKeys(chip, onClick);
    return chip;
  }

  // ── Side panel (detail) ──────────────────────────────────────────────────

  /** A table whose rows open the side panel; the index maps to a parallel detail list. */
  private Div clickableTable(String[] headers, List<List<CellView>> rows,
      java.util.function.IntConsumer onRowClick) {
    Div table = new Div();
    table.addClassName("pt-table");
    Div head = new Div();
    head.addClassName("pt-table__row");
    head.addClassName("pt-table__row--head");
    for (String header : headers) {
      head.add(cell(new CellView(header, "head")));
    }
    table.add(head);
    for (int i = 0; i < rows.size(); i++) {
      Div rowDiv = new Div();
      rowDiv.addClassName("pt-table__row");
      rowDiv.addClassName("pt-table__row--clickable");
      rows.get(i).forEach(cellView -> rowDiv.add(cell(cellView)));
      final int index = i;
      rowDiv.addClickListener(event -> onRowClick.accept(index));
      table.add(rowDiv);
    }
    return table;
  }

  private void openDetail(Detail selected) {
    this.detail = selected;
    refresh();
  }

  private void closeDetail() {
    this.detail = null;
    refresh();
  }

  /** The side panel keeps the list context: the full record, grouped by the spec's categories. */
  private Div buildDetailPanel() {
    Div overlay = new Div();
    overlay.addClassName("pt-overlay");

    Div panel = new Div();
    panel.addClassName("pt-panel");

    Div head = new Div();
    head.addClassName("pt-panel__head");
    Div titles = new Div();
    H2 title = new H2();
    title.addClassName("pt-panel__title");
    title.setText(detail.title());
    Span subtitle = new Span(detail.subtitle());
    subtitle.addClassName("pt-panel__subtitle");
    titles.add(title, subtitle);
    Button close = new Button("Close");
    close.addClassName("pt-btn");
    close.addClassName("pt-btn--text");
    close.addClickListener(event -> closeDetail());
    head.add(titles, close);
    panel.add(head);

    if (detail.chips() != null && !detail.chips().isEmpty()) {
      Div chips = new Div();
      chips.addClassName("pt-chips");
      detail.chips().forEach(text -> chips.add(chip(text, null)));
      panel.add(chips);
    }

    Div groups = new Div();
    groups.addClassName("pt-panel__groups");
    for (DetailGroup group : detail.groups()) {
      Div groupDiv = new Div();
      groupDiv.addClassName("pt-panel__group");
      H3 heading = new H3();
      heading.addClassName("pt-panel__group-title");
      heading.setText(group.heading());
      groupDiv.add(heading);
      for (DetailField field : group.fields()) {
        Div fieldRow = new Div();
        fieldRow.addClassName("pt-panel__field");
        Span label = new Span(field.label());
        label.addClassName("pt-panel__label");
        Span value = new Span(field.value());
        value.addClassName("pt-panel__value");
        fieldRow.add(label, value);
        groupDiv.add(fieldRow);
      }
      groups.add(groupDiv);
    }
    panel.add(groups);
    overlay.add(panel);
    return overlay;
  }

  private static DetailField field(String label, String value) {
    return new DetailField(label, value);
  }

  private static DetailGroup group(String heading, DetailField... fields) {
    return new DetailGroup(heading, List.of(fields));
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

  // ── Project settings ──────────────────────────────────────────────────

  private static final String DESCRIPTION_SECTION = "Description";
  private static final String FUNDING_SECTION = "Funding";
  private static final String CONTACTS_SECTION = "Contacts";
  private static final String DANGER_SECTION = "Project deletion";

  private static final List<String> SETTINGS_SECTIONS = List.of(
      DESCRIPTION_SECTION, FUNDING_SECTION, CONTACTS_SECTION, DANGER_SECTION);

  /**
   * The Project settings page: where project metadata is <em>changed</em>, as opposed to the
   * landing page, which only shows it.
   *
   * <p>This is the deliberate answer to "who edits contacts, and how?": a dedicated page with
   * page-local sub-navigation, not a dialog. Editing is rare and never urgent, so it does not belong
   * beside the title competing with the content; and keeping every project-metadata editor in one
   * place teaches the rule once instead of scattering one-off dialogs per band. Because it is a real
   * in-view route (like every other page in this prototype), the landing page can link straight to
   * it rather than offering a dead button.</p>
   */
  private Div settingsPage() {
    Div page = new Div();
    page.addClassName("pt-settings-page");

    // The page names itself, so the label is unambiguous even without the navigation row in view.
    Div head = new Div();
    head.addClassName("pt-settings__pagehead");
    H1 h1 = new H1();
    h1.addClassName("pt-title");
    h1.setText("Project settings");
    Paragraph sub = new Paragraph("Metadata for " + projectTitle()
        + ". These values are shown read-only on the project page.");
    sub.addClassName("pt-settings__subtitle");
    head.add(h1, sub);
    page.add(head);

    Div layout = new Div();
    layout.addClassName("pt-settings");

    // Page-local sub-navigation: sections of this page, not app-wide destinations.
    Div nav = new Div();
    nav.addClassName("pt-settings__nav");
    for (String section : SETTINGS_SECTIONS) {
      Button item = new Button(section);
      item.addClassName("pt-settings__navitem");
      if (section.equals(settingsSection)) {
        item.getElement().setAttribute("aria-current", "true");
      }
      item.addClickListener(event -> {
        this.settingsSection = section;
        refresh();
      });
      nav.add(item);
    }

    Div content = new Div();
    content.addClassName("pt-settings__content");
    switch (settingsSection) {
      case DESCRIPTION_SECTION -> settingsDescription(content);
      case FUNDING_SECTION -> settingsFunding(content);
      case DANGER_SECTION -> settingsDanger(content);
      default -> settingsContacts(content);
    }

    layout.add(nav, content);
    page.add(layout);
    return page;
  }

  /** Contacts: the same people as on the landing page, but here with their identifiers editable. */
  private void settingsContacts(Div content) {
    content.add(settingsHeading("Contacts",
        "The people accountable for this project. Shown read-only on the project page."));

    for (Contact contact : projectContacts()) {
      Div card = new Div();
      card.addClassName("pt-settings__card");

      Div head = new Div();
      head.addClassName("pt-settings__cardhead");
      Span role = new Span(contact.role());
      role.addClassName("pt-settings__cardtitle");
      head.add(role);

      boolean editing = contact.roleAbbreviation().equals(editingContact);
      Div actions = new Div();
      actions.addClassName("pt-settings__cardactions");
      if (editing) {
        actions.add(button("Save", true, () -> {
          this.editingContact = null;
          refresh();
        }), button("Cancel", false, () -> {
          this.editingContact = null;
          refresh();
        }));
      } else {
        actions.add(button("Edit " + contact.roleAbbreviation(), false, () -> {
          this.editingContact = contact.roleAbbreviation();
          refresh();
        }));
      }
      head.add(actions);
      card.add(head);

      // Editing happens in place: the same fields the read-only view shows, swapped for inputs.
      // No dialog, and no separate "edit mode" page state to get lost in.
      if (editing) {
        Div form = new Div();
        form.addClassName("pt-settings__form");
        form.add(editField("Name", contact.name()),
            editField("Email", contact.email()),
            editField("ORCID iD", contact.hasOrcid() ? contact.orcid() : ""));
        Paragraph hint = new Paragraph(
            "An ORCID iD is required for the FAIR badge. Leave it empty if the person has none.");
        hint.addClassName("pt-settings__hint");
        form.add(hint);
        card.add(form);
      } else {
        Div grid = new Div();
        grid.addClassName("pt-facts");
        grid.add(settingField("Name", contact.name(), false));
        grid.add(settingField("Email", contact.email(), false));
        grid.add(settingField("ORCID iD",
            contact.hasOrcid() ? contact.orcid() : "Not recorded", !contact.hasOrcid()));
        card.add(grid);
      }

      content.add(card);
    }

    Div note = new Div();
    note.addClassName("pt-note");
    note.setText("An ORCID iD is required for the FAIR badge on the project page. "
        + "Contacts without one are listed without an ORCID link.");
    content.add(note);
  }

  private Div editField(String label, String value) {
    Div field = new Div();
    field.addClassName("pt-settings__field");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("pt-fact__label");
    TextField input = new TextField();
    input.addClassName("pt-settings__input");
    input.setValue(value == null ? "" : value);
    input.setPlaceholder(label);
    field.add(labelSpan, input);
    return field;
  }

  private void settingsDescription(Div content) {
    content.add(settingsHeading("Description",
        "The objective and design summary shown at the top of the project page."));
    Div card = new Div();
    card.addClassName("pt-settings__card");
    boolean editing = editingContact != null && editingContact.equals(DESCRIPTION_SECTION);
    card.add(settingsCardHead("Project description", editing, DESCRIPTION_SECTION));
    if (editing) {
      Div form = new Div();
      form.addClassName("pt-settings__form");
      form.add(editField("Project title", projectTitle()),
          editField("Objective", OBJECTIVE_SHORT));
      card.add(form);
    } else {
      Div grid = new Div();
      grid.addClassName("pt-facts");
      grid.add(settingField("Project title", projectTitle(), false));
      grid.add(settingField("Objective", OBJECTIVE_SHORT, false));
      card.add(grid);
    }
    content.add(card);
  }

  private void settingsFunding(Div content) {
    content.add(settingsHeading("Funding",
        "Grant or funding reference recorded for this project."));
    Div card = new Div();
    card.addClassName("pt-settings__card");
    boolean editing = editingContact != null && editingContact.equals(FUNDING_SECTION);
    card.add(settingsCardHead("Funding information", editing, FUNDING_SECTION));
    if (editing) {
      Div form = new Div();
      form.addClassName("pt-settings__form");
      form.add(editField("Funding", "123 (Test grant)"),
          editField("Data access", "Restricted — request required"));
      card.add(form);
    } else {
      Div grid = new Div();
      grid.addClassName("pt-facts");
      grid.add(settingField("Funding", "123 (Test grant)", false));
      grid.add(settingField("Data access", "Restricted — request required", false));
      card.add(grid);
    }
    content.add(card);
  }

  /** Card heading with the edit affordance for that card, so the action is scoped to what it edits. */
  private Div settingsCardHead(String title, boolean editing, String sectionKey) {
    Div head = new Div();
    head.addClassName("pt-settings__cardhead");
    Span span = new Span(title);
    span.addClassName("pt-settings__cardtitle");
    Div actions = new Div();
    actions.addClassName("pt-settings__cardactions");
    if (editing) {
      actions.add(button("Save", true, () -> {
        this.editingContact = null;
        refresh();
      }), button("Cancel", false, () -> {
        this.editingContact = null;
        refresh();
      }));
    } else {
      actions.add(button("Edit", false, () -> {
        this.editingContact = sectionKey;
        refresh();
      }));
    }
    head.add(span, actions);
    return head;
  }

  private void settingsDanger(Div content) {
    content.add(settingsHeading("Project deletion",
        "Deleting a project removes its experiments, samples and measurement metadata."));
    Div card = new Div();
    card.addClassName("pt-settings__card");
    card.addClassName("pt-settings__card--danger");
    Span title = new Span("Delete this project");
    title.addClassName("pt-settings__cardtitle");
    Paragraph text = new Paragraph(
        "This cannot be undone. Registered data in the repository is not deleted.");
    text.addClassName("pt-settings__text");
    Div actions = new Div();
    actions.addClassName("pt-settings__cardactions");
    actions.add(button("Delete project", false, null));
    card.add(title, text, actions);
    content.add(card);
  }

  private Div settingsHeading(String title, String subtitle) {
    Div head = new Div();
    head.addClassName("pt-settings__head");
    H2 h2 = new H2();
    h2.addClassName("pt-band__title");
    h2.setText(title);
    Paragraph p = new Paragraph(subtitle);
    p.addClassName("pt-settings__subtitle");
    head.add(h2, p);
    return head;
  }

  private Div settingField(String label, String value, boolean muted) {
    Div field = new Div();
    Span labelSpan = new Span(label);
    labelSpan.addClassName("pt-fact__label");
    Paragraph valueParagraph = new Paragraph(value);
    valueParagraph.addClassName("pt-fact__value");
    if (muted) {
      valueParagraph.addClassName("pt-fact__value--muted");
    }
    field.add(labelSpan, valueParagraph);
    return field;
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
    // Same pattern as the contact row: the landing page shows project metadata read-only and links
    // out to the one place that edits it.
    actions.add(settingsLink("Edit project details", FUNDING_SECTION));
    head.add(title, actions);
    band.add(head);

    Div facts = new Div();
    facts.addClassName("pt-facts");
    facts.add(fact("Project ID", "Q2IMMU"));
    facts.add(fact("Funding", "123 (Test grant)"));
    facts.add(fact("Data access", "Restricted — request required"));
    facts.add(fact("Primary data type", "Immunopeptidomics"));
    band.add(facts);

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
    // The H1 names the VIEW. The experiment is named by the breadcrumb and nowhere else, so an
    // experiment-scoped page carries no scope line even though its callers pass a subtitle; that
    // second line under the H1 was exactly the duplication. Project pages keep their subtitle.
    if (subtitle != null && !page.isExperimentScoped()) {
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

  // ── Design detail blocks ─────────────────────────────────────────────────

  /**
   * One named category of the design. Used only by the Design page — the design's groups, variables
   * and confounding variables are that page's content, not a summary to repeat on other steps.
   */
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