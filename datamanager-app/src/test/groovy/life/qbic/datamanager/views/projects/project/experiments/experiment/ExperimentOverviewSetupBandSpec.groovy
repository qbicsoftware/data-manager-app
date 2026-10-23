package life.qbic.datamanager.views.projects.project.experiments.experiment

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Span
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.projectmanagement.application.api.ProjectQueryService
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.application.sample.SamplePreviewLookup
import life.qbic.projectmanagement.domain.model.OntologyTerm
import life.qbic.projectmanagement.domain.model.experiment.Experiment
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository
import life.qbic.projectmanagement.domain.repository.ProjectRepository
import life.qbic.projectmanagement.domain.repository.SampleRepository
import spock.lang.Specification

/**
 * Covers the read-only Setup band on the experiment overview.
 *
 * <p>The band reports the configured experiment name and sample origins. It was added with only a
 * purpose sentence and an "Edit setup" button and no values, so the one place a user could see an
 * experiment's state without opening an editor showed nothing — finding out what was configured
 * meant navigating to the edit route. These specs pin that the band reports what is there, and that
 * an unconfigured origin is an explicit statement rather than a silent gap.</p>
 *
 * <p>Asserts the band itself, not just its detail grid: the original defect was that the band showed
 * nothing, so a spec that called the grid helper directly would keep passing if the grid were
 * dropped from the band again. {@code reload()} returns early when there is no current {@code UI},
 * so the band is driven directly.</p>
 */
class ExperimentOverviewSetupBandSpec extends Specification {

  /**
   * The component takes concrete service classes and this classpath has no Objenesis, so the two
   * concrete collaborators are built through their real constructors with mocked interfaces
   * ({@code constructorArgs}), the approach {@code NewGroupFormSpec} documents. None of their methods
   * are stubbed — the band under test reads an {@code Experiment} passed in, not a service.
   */
  private ExperimentOverviewComponent component() {
    return new ExperimentOverviewComponent(
        Mock(ProjectQueryService),
        Mock(ExperimentInformationService, constructorArgs: [
            Mock(ExperimentRepository), Mock(ProjectRepository), sampleInformationService()]),
        Mock(ConfoundingVariableService),
        sampleInformationService())
  }

  private SampleInformationService sampleInformationService() {
    return Mock(SampleInformationService, constructorArgs: [
        Mock(SamplePreviewLookup), Mock(SampleRepository)])
  }

  def "reports the configured name and each sample origin"() {
    given: "an experiment with a name and one term per origin"
    def experiment = Experiment.create("Proteomics pilot")
    experiment.setSpecies([term("Homo sapiens")])
    experiment.setSpecimens([term("Blood plasma")])
    experiment.setAnalytes([term("Protein")])

    when:
    def grid = ExperimentOverviewComponent.setupDetailGrid(experiment)

    then: "each value is shown under the field it belongs to"
    reportedValues(grid) == [
        "Species" : ["Homo sapiens"],
        "Specimen": ["Blood plasma"],
        "Analyte" : ["Protein"],
    ]
  }

  /**
   * The regression that prompted this spec: the band existed, but reported no values, so the only
   * way to find out what was configured was to open the edit route. Asserting through the band means
   * dropping the grid from it fails here.
   */
  def "the band itself reports the configured values, not only a link to the editor"() {
    given:
    def experiment = Experiment.create("Proteomics pilot")
    experiment.setSpecies([term("Homo sapiens")])

    when:
    def band = component().setupBand(experiment)

    then: "the values are inside the band, not merely reachable from it"
    reportedValues(band)["Species"] == ["Homo sapiens"]
    and: "and the editor is still offered for changes"
    actionButtons(band).any { it.text == "Edit setup" }
  }

  /**
   * The action belongs with the heading it addresses, not trailing the values it sits under. As a
   * trailing row it read as a continuation of the content above it, and it moved down the band as
   * that content grew. These specs pin the placement so it cannot drift back to the footer.
   */
  def "the edit action sits in the band head, so it addresses the band it belongs to"() {
    given:
    def experiment = Experiment.create("Proteomics pilot")

    when:
    def band = component().setupBand(experiment)

    then: "it is carried by the head row, alongside the title"
    def head = band.getChildren()
        .filter { it.classNames.contains("experiment-overview-band-head") }
        .findFirst().orElse(null)
    head != null
    actionButtons(head).any { it.text == "Edit setup" }

    and: "there is exactly one such action, and it is no longer a trailing row of the band"
    actionButtons(band).size() == 1
  }

  def "the band head carries the title, the purpose hint and the action, so the band starts at its values"() {
    given:
    def experiment = Experiment.create("Proteomics pilot")

    when:
    def band = component().setupBand(experiment)

    then: "the head holds all three, on one line"
    def head = band.getChildren()
        .filter { it.classNames.contains("experiment-overview-band-head") }
        .findFirst().orElseThrow()
    head.getChildren().any { it.classNames.contains("experiment-overview-band-title") }
    head.getChildren().any { it.classNames.contains("experiment-overview-hint") }
    actionButtons(head).any { it.text == "Edit setup" }

    and: "the purpose text is no longer a row of its own above the values"
    !band.getChildren().any { it.classNames.contains("experiment-overview-note") }

    and: "so nothing sits between the head and the values"
    def children = band.getChildren().toList()
    children.indexOf(head) + 1 ==
        children.findIndexOf { it.classNames.contains("experiment-overview-grid") }
  }

  /**
   * The header names the view and then the experiment, in that order.
   *
   * <p>This follows the sibling tabs, whose H1s all name the view ("View Samples", "Download Raw
   * Data"), and the project settings page's title + subtitle shape. The experiment name sits beneath
   * because the breadcrumb truncates it at 42ch — for a long name the crumb arrives as "… how the …",
   * so this header is the one place the experiment can be read in full.</p>
   */
  def "the header names the view first, and the experiment beneath it in full"() {
    given:
    def experiment = Experiment.create("A wizard has fallen from the stars")

    when:
    def header = component().pageHeader(experiment)

    then: "the H1 is the view, not the entity"
    def title = header.getChildren()
        .filter { it.classNames.contains("experiment-overview-title") }
        .findFirst().orElseThrow()
    title.element.text == "Experiment overview"

    and: "the experiment name follows as the subtitle, in full"
    def subtitle = header.getChildren()
        .filter { it.classNames.contains("experiment-overview-subtitle") }
        .findFirst().orElseThrow()
    subtitle.element.text == "A wizard has fallen from the stars"
  }

  /** The buttons inside the actions slot of a subtree. */
  private static List<com.vaadin.flow.component.button.Button> actionButtons(Component root) {
    def buttons = []
    def pending = new ArrayDeque<Component>()
    pending.add(root)
    while (!pending.isEmpty()) {
      def node = pending.poll()
      node.getChildren().forEach { child ->
        if (child instanceof com.vaadin.flow.component.button.Button) {
          buttons.add(child)
        }
        pending.add(child)
      }
    }
    return buttons
  }

  /**
   * The empty state is the whole reason this band exists in the check: an experiment that has not
   * had its origins set must say so, not render four empty columns that look like a fault.
   */
  def "states that nothing is configured yet rather than showing empty blocks"() {
    given: "an experiment whose origins have not been set"
    def experiment = Experiment.create("Fresh experiment")

    when:
    def grid = ExperimentOverviewComponent.setupDetailGrid(experiment)

    then:
    reportedValues(grid) == [
        "Species" : ["None yet"],
        "Specimen": ["None yet"],
        "Analyte" : ["None yet"],
    ]
  }

  def "shows every term when an origin has several"() {
    given: "two species, which is allowed"
    def experiment = Experiment.create("Multi-species")
    experiment.setSpecies([term("Homo sapiens"), term("Mus musculus")])

    when:
    def grid = ExperimentOverviewComponent.setupDetailGrid(experiment)

    then: "both are listed, in insertion order"
    reportedValues(grid)["Species"] == ["Homo sapiens", "Mus musculus"]
  }

  /**
   * The label is what the user picked and what the registration template offers. The full term also
   * carries ontology and accession, which is far too long for this grid, so this pins that the band
   * reads the label rather than the term's {@code toString}.
   */
  def "shows the term label, not the ontology and accession"() {
    given:
    def experiment = Experiment.create("Labelled")
    experiment.setSpecies([new OntologyTerm("NCBITaxon", "2023-01-01", "http://example.org/ont",
        "Homo sapiens", "NCBITaxon:9606", "human", "http://example.org/NCBITaxon_9606")])

    when:
    def grid = ExperimentOverviewComponent.setupDetailGrid(experiment)

    then:
    reportedValues(grid)["Species"] == ["Homo sapiens"]
  }

  private static OntologyTerm term(String label) {
    return new OntologyTerm("TEST", "1.0", "http://example.org/ont", label,
        "TEST:0000001", "", "http://example.org/TEST_0000001")
  }

  /**
   * Maps each detail block's title to the values under it, so assertions name the field they are
   * about instead of relying on positional order. Walks the whole subtree, because the blocks sit
   * inside the detail grid, which itself sits inside the band.
   */
  private static Map<String, List<String>> reportedValues(Component root) {
    def result = [:]
    collectBlocks(root).forEach { block ->
      def title = block.getChildren()
          .filter { it.classNames.contains("experiment-overview-detail-title") }
          .map { it.element.text }
          .findFirst().orElse(null)
      if (title == null) {
        return
      }
      def values = []
      block.getChildren().forEach { child ->
        if (child.classNames.contains("experiment-overview-detail-item")) {
          values.add(child.element.text)
        } else if (child instanceof Span
            && child.classNames.contains("experiment-overview-note")) {
          values.add(child.text)
        }
      }
      result[title] = values
    }
    return result
  }

  /** Depth-first walk for the elements carrying the detail-block class. */
  private static List<Component> collectBlocks(Component root) {
    def blocks = []
    def pending = new ArrayDeque<Component>()
    pending.add(root)
    while (!pending.isEmpty()) {
      def node = pending.poll()
      node.getChildren().forEach { child ->
        if (child.classNames.contains("experiment-overview-detail")) {
          blocks.add(child)
        }
        pending.add(child)
      }
    }
    return blocks
  }
}
