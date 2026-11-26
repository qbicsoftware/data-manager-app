package life.qbic.datamanager.views.projects.project.experiments.experiment

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.datamanager.views.projects.project.experiments.ExperimentDesignMain
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService
import life.qbic.projectmanagement.application.ontology.SpeciesLookupInterface
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService
import life.qbic.projectmanagement.application.ontology.TerminologySelect
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.application.sample.SamplePreviewLookup
import life.qbic.projectmanagement.domain.model.experiment.Experiment
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.repository.ProjectRepository
import life.qbic.projectmanagement.domain.repository.SampleRepository
import org.springframework.context.support.StaticMessageSource
import reactor.core.scheduler.Schedulers
import spock.lang.Specification

/**
 * Covers how the experiment setup save reports its outcome.
 *
 * <p>The save used to be a bare synchronous call followed by a toast reading "Experiment metadata
 * updated" — copy from a surface that no longer exists — on a page where nothing visibly changed,
 * because the form already displayed the values that had just been written. There was no way for a
 * user to tell a successful save from a no-op.</p>
 *
 * <p>These specs pin the parts of the fix that a compiler cannot check: that a successful write
 * actually reaches the success branch, that a failed write is never reported as success, and that
 * the re-entrancy guard is released on both paths so a single failure cannot wedge the button.</p>
 *
 * <p>The reactive pipeline is driven on {@code Schedulers.immediate()} so a save completes inline —
 * the subject is the shape of the stream, not real concurrency.</p>
 */
class ExperimentSetupSaveSpec extends Specification {

  private static final String SUCCESS_KEY = "experiment.setup.updated.success"
  private static final String FAILURE_KEY = "experiment.setup.updated.failed"
  private static final String SAVING_KEY = "experiment.setup.saving"

  private static final String PROJECT_ID = "0270ce7f-4092-40e3-9c4c-ce7adb688bf5"

  private ExperimentInformationService experimentInformationService
  private SampleInformationService sampleInformationService
  private MessageSourceNotificationFactory notificationFactory
  private UI ui
  private ExperimentSetupComponent component

  def setup() {
    // setContext builds the cross-link's route, and a RouterLink resolves its href from the
    // VaadinService router registry. This installs the same minimal service/registry stub HomeLinkSpec
    // uses, so the component can be given a context without booting a Spring context or a UI.
    RouteRegistry registry = Stub(RouteRegistry)
    registry.getTargetUrl(ExperimentDesignMain, _ as RouteParameters) >> Optional.of("design")
    registry.getTargetUrl(ExperimentInformationMain, _ as RouteParameters) >> Optional.of("overview")
    VaadinService service = Stub(VaadinService)
    service.getRouter() >> new Router(registry)
    // A null context short-circuits Vaadin's dev-mode component tracker.
    service.getContext() >> null
    // Component.getLocale() resolves through the current request. Left unstubbed, Spock answers with
    // a dummy and then fails trying to build a dummy Locale; with no current request the component
    // falls back to the JVM default locale, which is what these specs assert against.
    service.getCurrentRequest() >> null
    VaadinService.setCurrent(service)
    // Every key the component can ask for has to resolve, because the factory parses the level and
    // message type out of the properties and treats a missing entry as a failure to log.
    def messageSource = new StaticMessageSource()
    def locale = Locale.getDefault()
    register(messageSource, locale, SUCCESS_KEY, "text", "Experiment setup saved.", "success")
    register(messageSource, locale, FAILURE_KEY, "text", "Could not save the experiment setup.", "error")
    register(messageSource, locale, SAVING_KEY, "text", "Saving experiment setup ...", "info")

    // Concrete classes cannot be mocked on this classpath (no Objenesis). The leaf dependencies are
    // interfaces, so the service is built through its real constructor with mocked interfaces —
    // the approach NewGroupFormSpec documents.
    experimentInformationService = Mock(ExperimentInformationService, constructorArgs: [
        Mock(ExperimentRepository), Mock(ProjectRepository), sampleInformationService()])
    // A Spy rather than a plain instance, so these specs can assert that the success or failure
    // confirmation was actually requested. Without that assertion the specs passed while the success
    // path was unreachable — the guard is released by doFinally either way, so 'the flag was
    // cleared' says nothing about whether the user was ever told. Real methods still run.
    notificationFactory = Spy(MessageSourceNotificationFactory, constructorArgs: [messageSource])

    // Component.getLocale() resolves via UI.getCurrent() and, failing that, the current request.
    // With neither, Vaadin asks Spock for a dummy Locale and fails. A current UI that reports the
    // JVM default locale is the same arrangement ProjectAccessComponentSpec uses.
    ui = Mock(UI)
    ui.getLocale() >> Locale.getDefault()
    ui.isAttached() >> true
    // UiHandle, which runs the save's completion callbacks, refuses to run them unless it is bound
    // to a UI that reports a session with the lock held. Binding it here is what makes the success
    // and failure paths observable at all: an unbound handle silently drops them, which is how an
    // earlier version of these specs 'passed' while the success path was dead code.
    def session = Mock(com.vaadin.flow.server.VaadinSession)
    session.hasLock() >> true
    ui.getSession() >> session
    UI.setCurrent(ui)

    def setup = new ExperimentSetupComponent(experimentInformationService,
        sampleInformationService(), speciesLookupService(), terminologyService(),
        notificationFactory, Schedulers.immediate())
    // setContext loads the experiment to populate the form, so find() has to answer before the
    // context is set. A real Experiment keeps the stub honest without needing a database.
    experimentInformationService.find(_ as String, _ as ExperimentId) >>
        Optional.of(Experiment.create("Proteomics pilot"))
    setup.setContext(new Context().with(ProjectId.parse(PROJECT_ID))
        .with(ExperimentId.create()))
    component = setup
    // UiHandle binds in onAttach, which a component built outside a real request never receives, and
    // an unbound handle silently drops the completion callbacks. Binding it directly wires the
    // success and failure paths the same way a request would — and is what stops these specs from
    // passing while the success path is dead code.
    setup.@uiHandle.bind(ui)
  }

  def cleanup() {
    UI.setCurrent(null)
    VaadinService.setCurrent(null)
  }

  private static void register(StaticMessageSource source, Locale locale, String key,
      String type, String text, String level) {
    source.addMessage("${key}.message.type".toString(), locale, type)
    source.addMessage("${key}.message.text".toString(), locale, text)
    source.addMessage("${key}.level".toString(), locale, level)
  }

  private SampleInformationService sampleInformationService() {
    if (sampleInformationService == null) {
      sampleInformationService = Mock(SampleInformationService, constructorArgs: [
          Mock(SamplePreviewLookup), Mock(SampleRepository)])
      // The setup surface counts how many registered samples use each origin to lock the terms in
      // use. An empty result is the honest default here: the save behaviour under test does not
      // depend on those counts.
      sampleInformationService.retrieveSamplesForExperiment(_, _) >> []
    }
    return sampleInformationService
  }

  private SpeciesLookupService speciesLookupService() {
    return Mock(SpeciesLookupService, constructorArgs: [Mock(SpeciesLookupInterface)])
  }

  private TerminologyService terminologyService() {
    return Mock(TerminologyService, constructorArgs: [Mock(TerminologySelect)])
  }

  private static ExperimentDraft draft() {
    def draft = new ExperimentDraft()
    draft.setExperimentName("Proteomics pilot")
    return draft
  }

  private void invokeSave() {
    component.save(ProjectId.parse(PROJECT_ID), ExperimentId.create(), draft())
  }

  /**
   * The bug this guards against: {@code Mono.fromRunnable} yields a {@code Mono<Void>}, which
   * completes without emitting, so {@code subscribe(onNext, onError)} never reaches onNext and the
   * success path — confirmation plus the return to the overview — was skipped entirely while the
   * pending indicator still closed. A save that worked looked exactly like one that did nothing.
   */
  def "a successful save reaches the success path, so it cannot look like a no-op"() {
    when: "a save that the service accepts"
    invokeSave()

    then: "the write happened exactly once"
    1 * experimentInformationService.editExperimentInformation(_, _, _, _, _, _)

    and: "the user is actually confirmed — the observable outcome of the success branch"
    1 * notificationFactory.toast(SUCCESS_KEY, _, _)

    and: "the guard is released"
    !component.@saveInProgress
  }

  def "a failed save reports the failure and does not claim success"() {
    given: "a write that the service refuses"
    experimentInformationService.editExperimentInformation(_, _, _, _, _, _) >>
        { throw new IllegalStateException("database is down") }

    when:
    invokeSave()

    then: "the failure is announced"
    1 * notificationFactory.toast(FAILURE_KEY, _, _)

    and: "success is never claimed"
    0 * notificationFactory.toast(SUCCESS_KEY, _, _)

    and: "the guard is released anyway, so the user can correct and retry"
    !component.@saveInProgress
  }

  /**
   * The guard is set before the write and cleared on both terminal paths. If it were only cleared on
   * success, one failed save would leave the button permanently inert — the user could fix the
   * problem and still be unable to submit.
   */
  def "the re-entrancy guard is released after a failure, so a retry is still possible"() {
    given:
    experimentInformationService.editExperimentInformation(_, _, _, _, _, _) >>
        { throw new IllegalStateException("database is down") }

    when: "a save fails"
    invokeSave()

    then: "the component is not left believing a save is still running"
    !component.@saveInProgress
  }

  def "the re-entrancy guard is released after a success"() {
    when:
    invokeSave()

    then:
    !component.@saveInProgress
  }

  def "a second save is ignored while the first is still in flight, so a double click queues nothing"() {
    given: "a save in progress, as a first click leaves it"
    component.@saveInProgress = true

    when: "a second submit arrives"
    invokeSave()

    then: "no second write is issued"
    0 * experimentInformationService.editExperimentInformation(_, _, _, _, _, _)
  }
}
