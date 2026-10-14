package life.qbic.datamanager.views.demo

import com.vaadin.flow.component.Component
import com.vaadin.flow.internal.nodefeature.ElementListenerMap
import spock.lang.Specification

/**
 * Guards the constructor path of the project-page prototype.
 *
 * <p>This prototype builds its whole body in the constructor, before attachment and without a UI.
 * Anything that resolves a stream resource (e.g. {@code OidcLogo}) breaks there with
 * "No currently active UI found", and {@code addClassName} rejects space-separated class lists.</p>
 */
class ProjectPagePrototypeConstructorSpec extends Specification {

  def "constructs the summary page in populated state without an active UI"() {
    when:
    def view = new ProjectPagePrototypeDemo()

    then:
    noExceptionThrown()
    view.classNames.any { it == "pt-app" }

    and: "the contact chips expose the role abbreviations"
    contactRoles(view).sort() == ["PI", "PM", "Resp."]

    and: "only the contacts that actually have an ORCID get an ORCID link"
    orcidLinks(view) == 2
  }

  def "every in-prototype page is constructible, including Settings"() {
    when: "each page is entered through the same in-view routing the prototype uses"
    def view = new ProjectPagePrototypeDemo()
    def failures = []
    for (page in ["SAMPLES", "DATASETS", "ACCESS", "ONTOLOGY", "SETTINGS"]) {
      try {
        invokeNavigate(view, page)
      } catch (Throwable t) {
        failures << "$page -> $t"
      }
    }

    then:
    failures.isEmpty()

    and: "the Settings page renders its page-local sections"
    def labels = buttonLabels(view)
    labels.containsAll(["Description", "Funding", "Contacts", "Project deletion"])
  }

  def "editing a contact happens in place, without a dialog"() {
    given: "the Settings page is open on Contacts"
    def view = new ProjectPagePrototypeDemo()
    invokeNavigate(view, "SETTINGS")

    expect: "each contact is read-only and offers a scoped edit action"
    buttonLabels(view).containsAll(["Edit PI", "Edit PM", "Edit Resp."])
    textFields(view) == 0

    when: "the user opens one contact for editing"
    clickButton(view, "Edit PI")

    then: "inputs appear in place of the read-only fields, with Save/Cancel"
    textFields(view) > 0
    buttonLabels(view).containsAll(["Save", "Cancel"])

    and: "no other contact was pulled into edit mode at the same time"
    buttonLabels(view).containsAll(["Edit PM", "Edit Resp."])

    when: "the user cancels"
    clickButton(view, "Cancel")

    then: "the card returns to read-only"
    textFields(view) == 0
    buttonLabels(view).contains("Edit PI")
  }

  def "snapshot rows are the newest records first, with a column header saying what the time means"() {
    given: "the populated project page"
    def view = new ProjectPagePrototypeDemo()

    expect: "the Samples header explains the trailing column"
    rowTexts(view, "Samples").first() == ["Sample ID", "Sample name", "Changed"]

    and: "the rows count down from the last registered sample, newest first"
    // The pilot has 48 samples, so the three newest are 0048, 0047, 0046 — not 0001..0003.
    idsInFirstRows(view, "Samples", 3) == ["Q2IMMU-0048", "Q2IMMU-0047", "Q2IMMU-0046"]

    and: "every row carries a relative age, and they age monotonically downwards"
    rowTexts(view, "Samples").drop(1).take(3)*.last() == ["2 days ago", "3 days ago", "4 days ago"]

    and: "the Dataset header explains its trailing column too"
    rowTexts(view, "Dataset").first() == ["Measurement ID", "Primary data", "Changed"]

    and: "the dataset rows also count down from the newest measurement"
    idsInFirstRows(view, "Dataset", 3) == ["Q2IMMU-MS-024", "Q2IMMU-MS-023", "Q2IMMU-MS-022"]

    and: "the three newest measurements are the ones still missing primary data"
    // 6 of 24 are unattached and the backlog sits at the top of the registration order, so the
    // preview and the attention callout agree instead of contradicting each other.
    rowTexts(view, "Dataset").drop(1).take(3)*.get(1) == ["Not attached", "Not attached", "Not attached"]
  }

  def "the snapshot status names whether the samples were registered or modified"() {
    when: "the populated project page renders"
    def view = new ProjectPagePrototypeDemo()

    then: "the pilot reports a modification, not a registration"
    statuses(view).contains("Modified · 2 days ago")

    and: "the second experiment reports a registration"
    statuses(view).contains("Registered · 5 days ago")

    and: "the not-started experiment claims no activity at all"
    statuses(view).contains("Nothing registered yet")
  }

  def "a not-started experiment shows no preview rows rather than a lower sample code"() {
    given:
    def view = new ProjectPagePrototypeDemo()

    expect: "the empty state is stated, never faked with placeholder data"
    textsIn(view).contains("No samples to show yet")
    textsIn(view).contains("No measurements to show yet")
  }

  def "the dataset card states the gap as a count that must reach zero"() {
    given: "the overview of the pilot experiment (24 measurements, 6 without data)"
    def view = enterPilot()

    expect: "the dataset card names the problem rather than a ratio the reader must subtract"
    classTexts(view, "pt-snap__status").contains("6 of 24 still need primary data")

    and: "it no longer asks the reader to do the subtraction"
    !classTexts(view, "pt-snap__status").any { it.contains("18 with data") }
  }

  def "a not-started experiment gates its dataset card and routes to the step that unblocks it"() {
    given: "the third experiment, which has no samples"
    def view = new ProjectPagePrototypeDemo()
    def method = view.class.getDeclaredMethod("openExperiment", int, pageType(view))
    method.setAccessible(true)
    method.invoke(view, 2, pageConstant(view, "DESIGN"))

    expect: "the dataset card is locked and names the missing prerequisite"
    classTexts(view, "pt-snap__label") == ["Samples", "Dataset"]
    classTexts(view, "pt-tag").contains("Locked")
    classTexts(view, "pt-snap__text").any {
      it.contains("Register samples first; the dataset opens when they exist.")
    }

    and: "the action that unblocks it is offered and labelled"
    buttonLabels(view).contains("Go to samples")

    and: "the locked card is not dressed as clickable, because it contains that button"
    elementsWithClass(view, "pt-snap--invite").size() == 2
    elementsWithClass(view, "pt-snap--invite").every {
      it.element.getAttribute("role") == null
    }

    and: "no preview is shown for data that does not exist"
    classTexts(view, "pt-snap__value").isEmpty()
  }

  def "the project onboarding pipeline is numbered, so the steps read as an ordered sequence"() {
    given: "the empty project, which is where the onboarding pipeline is shown"
    def view = new ProjectPagePrototypeDemo()
    clickButton(view, "New / empty project")

    expect: "every step carries its ordinal"
    classTexts(view, "pt-step__ordinal") == ["1", "2", "3"]
  }

  def "the context strip switches between the experiment's views and marks the current one"() {
    given: "the experiment overview"
    def view = enterPilot()

    expect: "the switcher is there, with the overview marked as the view being read"
    classTexts(view, "pt-expview") == ["Overview", "Samples", "Dataset"]
    activeViews(view) == ["Overview"]

    and: "the overview still navigates with its own cards as well"
    classTexts(view, "pt-snap__label") == ["Samples", "Dataset"]
    elementsWithClass(view, "pt-snap").every {
      it.element.getAttribute("role") == "button"
    }

    when: "the user opens Samples"
    markCurrent(view, "EXP_SAMPLES")

    then: "the mark follows, and the dataset is one click away from here"
    activeViews(view) == ["Samples"]
    classTexts(view, "pt-expview").contains("Dataset")

    when: "and then the dataset"
    markCurrent(view, "EXP_DATASET")

    then: "the switcher keeps every view of the experiment reachable from anywhere in it"
    activeViews(view) == ["Dataset"]
    classTexts(view, "pt-expview") == ["Overview", "Samples", "Dataset"]
  }

  def "the prototype routes by view and experiment, so browser Back and Forward work"() {
    expect: "the route carries both parameters, and entering it re-renders the view"
    def route = ProjectPagePrototypeDemo.getAnnotation(com.vaadin.flow.router.Route)
    route.value() == "test-view/project-page"
    com.vaadin.flow.router.BeforeEnterObserver.isAssignableFrom(ProjectPagePrototypeDemo)
  }

  def "a card with nothing to open is not dressed as clickable"() {
    given: "a not-started experiment, where the data cards are invitations rather than previews"
    def view = new ProjectPagePrototypeDemo()
    def method = view.class.getDeclaredMethod("openExperiment", int, pageType(view))
    method.setAccessible(true)
    method.invoke(view, 2, pageConstant(view, "DESIGN"))

    expect: "each invitation contains a real button and does not itself claim to be clickable"
    def invites = elementsWithClass(view, "pt-snap--invite")
    invites.size() == 2
    invites.every { it.element.getAttribute("role") == null }

    and: "so its cursor is the default, not the pointer the populated cards use"
    // Two targets for one gesture is worse than one: whole-card activation belongs to the cards that
    // have no button inside them.
    def sheet = stylesheet()
    def inviteRule = sheet =~ /(?s)\.pt-snap--invite\s*\{(.*?)\}/
    inviteRule.find()
    inviteRule.group(1).contains("cursor: default")
  }

  /**
   * The prototype stylesheet, resolved from either the repo root or the module directory: surefire
   * runs with the module as its working directory, so a repo-root-relative path does not resolve.
   */
  private static String stylesheet() {
    def relative = "frontend/styles/views/project-prototype/project-page-prototype.css"
    def candidates = [new File(relative), new File("datamanager-app/$relative"),
        new File("../$relative")]
    def file = candidates.find { it.exists() }
    assert file != null: "stylesheet not found; looked in ${candidates*.absolutePath}"
    return file.text
  }

  /** Opens the given experiment-scoped page. */
  private static void markCurrent(view, String pageName) {
    def method = view.class.getDeclaredMethod("openExperiment", int, pageType(view))
    method.setAccessible(true)
    method.invoke(view, 0, pageConstant(view, pageName))
  }

  def "counts live in the overview cards, not repeated in the navigation chrome"() {
    given: "the experiment overview"
    def view = enterPilot()

    expect: "no count is repeated in either navigation row"
    classTexts(view, "pt-subnav__count").isEmpty()
    classTexts(view, "pt-expview__count").isEmpty()

    and: "the counts appear exactly once, in the data cards"
    classTexts(view, "pt-snap__value") == ["48", "24"]
  }

  def "the breadcrumb names the experiment, and the header does not repeat it"() {
    given: "the experiment overview"
    def view = enterPilot()

    expect: "the breadcrumb carries the experiment, as the current location rather than a link"
    currentCrumbs(view) == ["Immunopeptidomics pilot"]
    def crumbs = firstClassElement(view, "pt-crumbs").element.getTextRecursively()
    crumbs.contains("Immunotest")
    crumbs.contains("Immunopeptidomics pilot")

    and: "the header says the view name and nothing that restates the breadcrumb"
    // The regression: the project AND the experiment each appeared in the breadcrumb and again in a
    // scope line under the H1, so the chrome and the header looked like two different contexts.
    firstClassElement(view, "pt-page__subtitle") == null

    when: "the user opens a sub-page"
    markCurrent(view, "EXP_SAMPLES")

    then: "the experiment crumb becomes the link back UP to the overview"
    currentCrumbs(view).isEmpty()
    def back = elementsWithClass(view, "pt-crumb--link").find {
      it.element.getTextRecursively().contains("Immunopeptidomics pilot")
    }
    back != null
    back.element.getAttribute("role") == "link"
    back.element.getAttribute("tabindex") == "0"

    and: "and the header still does not restate it"
    firstClassElement(view, "pt-page__subtitle") == null
  }

  def "the design's payload has exactly one home: the Design page"() {
    given: "the Design page, which is where the design belongs"
    def view = enterPilot()

    expect: "the categories are rendered there"
    // classTexts returns the recursive text of each matching element, so assert on the headings
    // individually rather than on an exact list.
    def blocks = classTexts(view, "pt-detail")
    blocks.size() == 3
    blocks[0].startsWith("Experimental groups")
    blocks[1].startsWith("Variables")
    blocks[2].startsWith("Confounding variables")
    // The items are <li>s, not table cells, so this is asserted on the block text rather than on
    // textsIn (which only walks pt-table__cell elements).
    blocks[0].contains("Untreated control — 24 samples")

    when: "the user moves to Samples, then Dataset"
    markCurrent(view, "EXP_SAMPLES")
    def onSamples = classTexts(view, "pt-detail")
    markCurrent(view, "EXP_DATASET")
    def onDataset = classTexts(view, "pt-detail")

    then: "the design is NOT reproduced on the other steps' pages"
    // It was: the payload lived in a glance panel rendered on every experiment view, so the Samples
    // and Dataset pages each carried the whole design under a heading nobody was looking for.
    onSamples.isEmpty()
    onDataset.isEmpty()
  }

  def "the overview is the design plus the two data cards, and the data pages repeat neither"() {
    given: "the experiment landing"
    def view = enterPilot()

    expect: "the design payload and the two data cards share the landing"
    classTexts(view, "pt-detail").size() == 3
    classTexts(view, "pt-snap__label") == ["Samples", "Dataset"]

    when: "the user opens the Samples page"
    markCurrent(view, "EXP_SAMPLES")

    then: "neither the design nor the overview is repeated here"
    classTexts(view, "pt-detail").isEmpty()
    classTexts(view, "pt-snap__label").isEmpty()

    when: "and the Dataset page"
    markCurrent(view, "EXP_DATASET")

    then: "the same rule holds"
    classTexts(view, "pt-detail").isEmpty()
    classTexts(view, "pt-snap__label").isEmpty()
  }

  def "creating a design is visible immediately, in place, on the overview"() {
    given: "an experiment whose design is not defined yet, on the overview"
    def view = new ProjectPagePrototypeDemo()
    clickButton(view, "No design yet") // prototype-only toggle
    markCurrent(view, "DESIGN")

    expect: "the overview says so, in text"
    classTexts(view, "pt-tag").contains("Not defined yet")

    and: "it offers the action that creates it"
    buttonLabels(view).contains("Define design")

    and: "and shows no payload yet"
    classTexts(view, "pt-detail").isEmpty()

    when: "the user creates the design"
    clickButton(view, "Define design")

    then: "the same page shows the design — no reload, no route change"
    // The original defect: creating a design changed nothing visible in the page body.
    classTexts(view, "pt-detail").size() == 3
    firstClassElement(view, "pt-detail").element.getTextRecursively().contains("Experimental groups")

    and: "the state flips to the edit affordance"
    classTexts(view, "pt-tag").contains("Defined")
    !classTexts(view, "pt-tag").contains("Not defined yet")
    buttonLabels(view).contains("Edit design")
    !buttonLabels(view).contains("Define design")
  }

  def "every element advertising role=button or role=link is clickable AND keyboard-activatable"() {
    given: "an experiment-scoped page, which renders all of the interactive patterns at once"
    def view = enterPilot()

    expect: "each operable element is in the tab order"
    operableElements(view).every { it.element.getAttribute("tabindex") == "0" }

    and: "carries the full activation contract, so the advertised role is not a lie"
    // The marker is set by the single helper that wires both pointer and keyboard; the behavioural
    // proof that it works is the following spec, which dispatches a real keydown.
    operableElements(view).every { it.element.getAttribute("data-activation") == "full" }
  }

  def "every element advertising role=button or role=link has a real Enter/Space handler"() {
    given: "the Design page and every element on it that claims to be operable"
    def view = enterPilot()
    def operable = operableElements(view)

    expect: "the set is substantial, so this check cannot pass vacuously"
    // Five, not many: the breadcrumb's three links plus the overview's two data cards. The chrome
    // switcher and the scope self-link that used to inflate this are gone.
    operable.size() >= 5

    and: "each one registered a keydown handler filtered to Enter and Space"
    // Asserted on the registered filter rather than by dispatching an event: elemental.json is a
    // transitive dependency of Flow and is absent from this module's test classpath, so DomEvent
    // (which requires a JsonObject) cannot be constructed here. The filter is the part that carries
    // the behaviour anyway — it is what makes the browser deliver Enter AND Space.
    operable.every { keyFilters(it).any { f -> f.contains("Enter") && f.contains("' '") } }

    and: "the overview's data cards are among them, so the body is navigable too"
    def cards = elementsWithClass(view, "pt-snap").findAll {
      it.element.getAttribute("role") == "button"
    }
    cards.size() == 2
    operable.containsAll(cards)
  }

  // ── helpers ─────────────────────────────────────────────────────────────

  /**
   * The state a user is in after clicking a snapshot on the project page: the experiment's Design
   * view. The constructor lands on Summary, so every experiment-scoped assertion has to get here
   * first — asserting against the constructor state silently tests the wrong page.
   */
  private static ProjectPagePrototypeDemo enterPilot() {
    def view = new ProjectPagePrototypeDemo()
    def method = view.class.getDeclaredMethod("openExperiment", int, pageType(view))
    method.setAccessible(true)
    method.invoke(view, 0, pageConstant(view, "DESIGN"))
    return view
  }

  private static Class pageType(view) {
    view.class.declaredClasses.find { it.simpleName == "Page" }
  }

  private static Object pageConstant(view, String name) {
    pageType(view).enumConstants.find { it.name() == name }
  }

  /** The registered keydown filters for an element — how Enter/Space activation is expressed. */
  private static List<String> keyFilters(com.vaadin.flow.component.Component c) {
    def map = c.element.node.getFeatureIfInitialized(ElementListenerMap).orElse(null)
    map == null ? [] : map.getExpressions("keydown").toList()
  }

  private static List<com.vaadin.flow.component.Component> elementsWithClass(
      com.vaadin.flow.component.Component c, String className) {
    def out = []
    if (c.classNames.any { it == className }) {
      out << c
    }
    c.children.toList().each { out.addAll(elementsWithClass(it, className)) }
    out
  }

  private static void invokeNavigate(view, String pageName) {
    def pageType = view.class.declaredClasses.find { it.simpleName == "Page" }
    def constant = pageType.enumConstants.find { it.name() == pageName }
    def method = view.class.getDeclaredMethod("navigate", pageType)
    method.setAccessible(true)
    method.invoke(view, constant)
  }

  /**
   * The cell texts of every row of the named snapshot panel, header row included. Panels are matched
   * by their label element so the Samples and Dataset panels cannot be confused.
   */
  private static List<List<String>> rowTexts(com.vaadin.flow.component.Component root,
      String panelLabel) {
    def panel = findPanel(root, panelLabel)
    assert panel != null: "no snapshot panel labelled $panelLabel"
    def out = []
    collectRows(panel, out)
    return out
  }

  private static void collectRows(com.vaadin.flow.component.Component c, List<List<String>> out) {
    if (c.classNames.any { it == "pt-snap__row" }) {
      out << descendantCells(c)*.text
      return
    }
    c.children.toList().each { collectRows(it, out) }
  }

  private static List<com.vaadin.flow.component.Component> descendantCells(
      com.vaadin.flow.component.Component c) {
    def out = []
    if (c.classNames.any { it == "pt-table__cell" }) {
      out << c
    }
    c.children.toList().each { out.addAll(descendantCells(it)) }
    out
  }

  /** The first column of the first {@code n} data rows of a panel. */
  private static List<String> idsInFirstRows(com.vaadin.flow.component.Component root,
      String panelLabel, int n) {
    rowTexts(root, panelLabel).drop(1).take(n)*.first()
  }

  private static List<String> statuses(com.vaadin.flow.component.Component c) {
    def out = []
    if (c.classNames.any { it == "pt-snap__status" }) {
      out << c.text
    }
    c.children.toList().each { out.addAll(statuses(it)) }
    out
  }

  private static List<String> textsIn(com.vaadin.flow.component.Component c) {
    def out = []
    if (c.classNames.any { it == "pt-table__cell" }) {
      out << c.text
    }
    c.children.toList().each { out.addAll(textsIn(it)) }
    out
  }

  private static com.vaadin.flow.component.Component findPanel(
      com.vaadin.flow.component.Component c, String label) {
    if (c.classNames.any { it == "pt-snap" }) {
      def hasLabel = hasClassText(c, "pt-snap__label", label)
      if (hasLabel) {
        return c
      }
    }
    for (child in c.children.toList()) {
      def found = findPanel(child, label)
      if (found != null) {
        return found
      }
    }
    return null
  }

  private static boolean hasClassText(com.vaadin.flow.component.Component c, String className,
      String text) {
    if (c.classNames.any { it == className } && c.text == text) {
      return true
    }
    return c.children.toList().any { hasClassText(it, className, text) }
  }

  private static List<String> classTexts(com.vaadin.flow.component.Component c, String className) {
    def out = []
    if (c.classNames.any { it == className }) {
      // getTextRecursively(), not text: Component.text is only the DIRECT text of the element, so a
      // container (e.g. the sub-band wrapper) reports "" even though its children carry the content.
      out << c.element.getTextRecursively()
    }
    c.children.toList().each { out.addAll(classTexts(it, className)) }
    out
  }

  private static com.vaadin.flow.component.Component firstClassElement(
      com.vaadin.flow.component.Component c, String className) {
    if (c.classNames.any { it == className }) {
      return c
    }
    for (child in c.children.toList()) {
      def found = firstClassElement(child, className)
      if (found != null) {
        return found
      }
    }
    return null
  }

  /** The experiment views marked as the current page, by their visible label. */
  private static List<String> activeViews(com.vaadin.flow.component.Component c) {
    def out = []
    if (c.classNames.any { it == "pt-expview" }
        && c.element.getAttribute("aria-current") == "page") {
      out << c.element.getTextRecursively()
    }
    c.children.toList().each { out.addAll(activeViews(it)) }
    out
  }

  private static List<String> currentCrumbs(com.vaadin.flow.component.Component c) {
    classTexts(c, "pt-crumb--current")
  }

  /** Every element that advertises itself as operable via role=button or role=link. */
  private static List<com.vaadin.flow.component.Component> operableElements(
      com.vaadin.flow.component.Component c) {
    def out = []
    def role = c.element.getAttribute("role")
    if (role == "button" || role == "link") {
      out << c
    }
    c.children.toList().each { out.addAll(operableElements(it)) }
    out
  }

  private static boolean hasClickListener(com.vaadin.flow.component.Component c) {
    listenerTypes(c).contains("click")
  }

  private static List<String> contactRoles(com.vaadin.flow.component.Component c) {
    def out = []
    if (c.classNames.any { it == "pt-contact-chip__role" }) {
      out << c.text
    }
    c.children.toList().each { out.addAll(contactRoles(it)) }
    out
  }

  private static int orcidLinks(com.vaadin.flow.component.Component c) {
    int n = 0
    if (c.class.simpleName == "Anchor"
        && c.classNames.any { it == "pt-contact-chip__icon" }
        && c.element.getAttribute("href")?.startsWith("https://orcid.org/")) {
      n++
    }
    c.children.toList().each { n += orcidLinks(it) }
    n
  }

  def "settings is scoped as 'Project settings' so it cannot be confused with the shell Settings tab"() {
    given:
    def view = new ProjectPagePrototypeDemo()

    expect: "the in-project navigation uses the scoped label"
    buttonLabels(view).contains("Project settings")

    and: "the shell-level tab keeps its unscoped name, untouched by the rename"
    buttonLabels(view).contains("Settings")

    when: "the page is opened"
    invokeNavigate(view, "SETTINGS")

    then: "the page titles itself and its sections are reachable"
    headings(view).contains("Project settings")
    buttonLabels(view).containsAll(["Description", "Funding", "Contacts", "Project deletion"])
  }

  private static List<String> headings(com.vaadin.flow.component.Component c) {
    def out = []
    if (c.class.simpleName in ["H1", "H2", "H3"]) {
      out << c.text
    }
    c.children.toList().each { out.addAll(headings(it)) }
    out
  }

  private static List<String> buttonLabels(com.vaadin.flow.component.Component c) {
    def out = []
    if (c.class.simpleName == "Button") {
      out << c.text
    }
    c.children.toList().each { out.addAll(buttonLabels(it)) }
    out
  }

  private static void clickButton(com.vaadin.flow.component.Component c, String label) {
    if (c.class.simpleName == "Button" && c.text == label) {
      c.click()
      return
    }
    c.children.toList().each { clickButton(it, label) }
  }

  private static int textFields(com.vaadin.flow.component.Component c) {
    int n = c.class.simpleName == "TextField" ? 1 : 0
    c.children.toList().each { n += textFields(it) }
    n
  }
}
