package life.qbic.datamanager.views.projects.project.experiments.experiment

import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.projectmanagement.application.api.AsyncProjectService
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService
import life.qbic.projectmanagement.application.ontology.SpeciesLookupInterface
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService
import life.qbic.projectmanagement.application.ontology.TerminologySelect
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.application.sample.SamplePreviewLookup
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository
import life.qbic.projectmanagement.domain.repository.ProjectRepository
import life.qbic.projectmanagement.domain.repository.SampleRepository
import spock.lang.Specification

/**
 * Guards the constructor path of the two experiment edit surfaces.
 *
 * <p>Each surface cross-links to the other, and a {@code RouterLink} needs its
 * {@code RouteParameters} up front — it does <em>not</em> read them from the current location. The
 * route therefore cannot be built in the constructor, because the context that supplies the
 * project and experiment ids only arrives in {@code setContext(...)}, which the route host calls
 * after construction. Building it eagerly dereferenced a null context and broke Spring bean
 * creation outright:</p>
 *
 * <pre>
 * Error creating bean with name 'experimentSetupMain' … Constructor threw exception
 * </pre>
 *
 * <p>These specs instantiate the components the way Spring does — constructor only, no context —
 * so the eager-route mistake cannot come back. They assert construction and the link's structure
 * rather than any rendering, because the failure happened at construction time, before a UI
 * existed.</p>
 *
 * <p><b>On the test doubles.</b> The components take concrete service classes, and this classpath
 * has no Objenesis, so Spock cannot mock them directly. Every leaf dependency below is an
 * interface, so the collaborators are built through the real constructors with mocked interfaces
 * ({@code constructorArgs}) and none of their methods are stubbed. That is the same approach
 * {@code NewGroupFormSpec} documents.</p>
 */
class ExperimentComponentsConstructorSpec extends Specification {

  private SampleInformationService sampleInformationService() {
    return Mock(SampleInformationService, constructorArgs: [
        Mock(SamplePreviewLookup), Mock(SampleRepository)])
  }

  private ExperimentInformationService experimentInformationService() {
    return Mock(ExperimentInformationService, constructorArgs: [
        Mock(ExperimentRepository), Mock(ProjectRepository), sampleInformationService()])
  }

  private SpeciesLookupService speciesLookupService() {
    return Mock(SpeciesLookupService, constructorArgs: [Mock(SpeciesLookupInterface)])
  }

  private TerminologyService terminologyService() {
    return Mock(TerminologyService, constructorArgs: [Mock(TerminologySelect)])
  }

  private MessageSourceNotificationFactory notificationFactory() {
    return Mock(MessageSourceNotificationFactory,
        constructorArgs: [Mock(org.springframework.context.MessageSource)])
  }

  private ExperimentSetupComponent newSetupComponent() {
    return new ExperimentSetupComponent(experimentInformationService(), sampleInformationService(),
        speciesLookupService(), terminologyService(), notificationFactory(),
        Mock(reactor.core.scheduler.Scheduler))
  }

  private ExperimentDesignComponent newDesignComponent() {
    return new ExperimentDesignComponent(experimentInformationService(),
        sampleInformationService(), notificationFactory(),
        Mock(ConfoundingVariableService), Mock(AsyncProjectService))
  }

  def "the setup surface constructs without a context"() {
    when: "the component is built the way Spring builds it, before any context exists"
    def component = newSetupComponent()

    then: "construction succeeds"
    noExceptionThrown()
    component != null
  }

  def "the design surface constructs without a context"() {
    when:
    def component = newDesignComponent()

    then:
    noExceptionThrown()
    component != null
  }

  /**
   * The cross-link must carry its label as a child element rather than as the link's own text.
   *
   * <p>That structure is what makes the deferred route possible: {@code RouterLink(String, Class,
   * RouteParameters)} sets the text at construction time, but the route is only known once
   * {@code setContext} has run. Returning the field to that constructor form would silently
   * reintroduce the eager-route bug, so asserting the text is a child pins the shape the fix
   * depends on.</p>
   */
  def "the setup cross-link carries its label as a child, so its route can be set later"() {
    when:
    def links = crossLinks(newSetupComponent())

    then:
    links.size() == 1
    links.first().getText() == ""
    links.first().getChildren().any { it.element.text == "Experimental design" }
  }

  def "the design cross-link carries its label as a child, so its route can be set later"() {
    when:
    def links = crossLinks(newDesignComponent())

    then:
    links.size() == 1
    links.first().getText() == ""
    links.first().getChildren().any { it.element.text == "Experiment setup" }
  }

  /** Depth-first walk for the RouterLinks carrying the cross-link class. */
  private static List<com.vaadin.flow.router.RouterLink> crossLinks(
      com.vaadin.flow.component.Component component) {
    def found = []
    component.getChildren().forEach { child ->
      if (child instanceof com.vaadin.flow.router.RouterLink
          && child.classNames.contains("experiment-design-cross-link")) {
        found.add(child)
      }
      found.addAll(crossLinks(child))
    }
    return found
  }
}
