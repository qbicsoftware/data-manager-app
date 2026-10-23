package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.ANALYTE;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIES;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIMEN;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContext;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.reactiveSecurity;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentDesignMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Experiment <em>setup</em>: the experiment name and its sample origins.
 *
 * <p>This surface exists because its fields have exactly one job — they become the controlled
 * options of the sample-registration template ({@code Species}, {@code Specimen} and
 * {@code Analyte}; the template's {@code Condition} column comes from the experimental groups
 * instead, see {@link ExperimentDesignSheetComponent}). It used to share a route with the design
 * editor, stacked below it, so a user whose goal was the design had to scroll past this form to
 * reach the section they came for.</p>
 *
 * <p><b>The dependency is stated, not hidden.</b> The registration template cannot be built from
 * either surface alone: this page supplies three of its dropdown columns and the design supplies the
 * fourth. The page therefore names the other surface and links to it, so finishing here and still
 * being blocked cannot come as a surprise.</p>
 *
 * @since 1.19.0
 */
@UIScope
@SpringComponent
public class ExperimentSetupComponent extends PageArea {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  private static final Logger log = LoggerFactory.logger(ExperimentSetupComponent.class);

  @Serial
  private static final long serialVersionUID = 1L;

  private final transient ExperimentInformationService experimentInformationService;
  private final transient SampleInformationService sampleInformationService;
  private final transient MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final transient Scheduler scheduler;
  private final ExperimentDesignForm designForm;

  /**
   * Guards against a second submit while the first is still in flight: a double click would
   * otherwise queue two writes and two navigations.
   *
   * <p>Written from the scheduler thread when the save completes, hence {@code volatile}.</p>
   */
  private volatile boolean saveInProgress = false;

  private final Div content = new Div();
  private final Div header = new Div();
  private final Span title = new Span();
  /**
   * The link to the design surface, whose route can only be built once the context is known.
   *
   * <p>A {@link RouterLink} needs its {@link RouteParameters} up front — it does not read them from
   * the current location — but the context only arrives in {@link #setContext(Context)}, after this
   * component's constructor has already run. Building the route eagerly in the constructor therefore
   * dereferenced a null context and broke bean creation, so the route is set in {@code setContext}
   * instead and the link is created targetless here.</p>
   */
  private final RouterLink designLink = new RouterLink();
  private final UiHandle uiHandle = new UiHandle();
  private Context context;

  @Autowired
  public ExperimentSetupComponent(
      ExperimentInformationService experimentInformationService,
      SampleInformationService sampleInformationService,
      SpeciesLookupService ontologyTermInformationService,
      TerminologyService terminologyService,
      MessageSourceNotificationFactory messageSourceNotificationFactory,
      @Qualifier("reactiveScheduler") Scheduler scheduler) {
    this.experimentInformationService = requireNonNull(experimentInformationService);
    this.sampleInformationService = requireNonNull(sampleInformationService);
    this.messageSourceNotificationFactory = requireNonNull(messageSourceNotificationFactory);
    this.scheduler = requireNonNull(scheduler);
    this.designForm = new ExperimentDesignForm(requireNonNull(ontologyTermInformationService),
        requireNonNull(terminologyService));
    addClassName("experiment-setup-component");
    layoutComponent();
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    uiHandle.bind(attachEvent.getUI());
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    uiHandle.unbind();
  }

  public void setContext(Context context) {
    this.context = requireNonNull(context);
    context.projectId().orElseThrow(
        () -> new ApplicationException("no project id in context " + context));
    context.experimentId().orElseThrow(
        () -> new ApplicationException("no experiment id in context " + context));
    designLink.setRoute(ExperimentDesignMain.class, experimentRouteParameters());
    loadExperimentInformation(context.projectId().orElseThrow(),
        context.experimentId().orElseThrow());
  }

  private void layoutComponent() {
    this.add(header);
    header.addClassName("header");
    this.add(content);
    content.addClassName("details-content");

    // The H1 names this surface's job, not the experiment: the same experiment now has two editable
    // surfaces, so "Edit experiment" would no longer say which one the user is on.
    Div titleBlock = new Div(backToOverviewButton(), title);
    titleBlock.addClassName("experiment-design-title-block");
    header.add(titleBlock);
    title.setText("Experiment setup");
    title.addClassName("title");

    content.add(sectionHeader("Experiment setup", purpose()));
    designForm.addClassName("experiment-design-form-inline");
    content.add(designForm);
  }

  /**
   * The purpose text, including the pointer to the other surface. The last sentence is the one that
   * keeps the two routes honest: it names the field this page does <em>not</em> supply and where it
   * comes from, so "I filled in setup, why can I still not register samples" has an answer on the
   * page rather than only in the sample view's error state.
   */
  private Div purpose() {
    Div purpose = new Div();
    purpose.addClassName("experiment-design-section-purpose");
    purpose.add(new Span("The experiment name and the sample origins it derives from. These become "
        + "the Species, Specimen and Analyte options in the sample-registration template, so sample "
        + "annotations stay consistent and use public ontology terms. The template's Condition "
        + "column comes from the experimental groups instead, which live on "));
    // The link text is a child, not the RouterLink's own text, so the target can be set later (see
    // the designLink field).
    designLink.removeAll();
    designLink.add(new Span("Experimental design"));
    designLink.addClassName("experiment-design-cross-link");
    purpose.add(designLink, new Span("."));
    return purpose;
  }

  private Div sectionHeader(String title, Div purpose) {
    Div section = new Div();
    section.addClassName("experiment-design-section-header");
    Div text = new Div();
    text.addClassName("experiment-design-section-text");
    Span titleSpan = new Span(title);
    titleSpan.addClassName("experiment-design-section-title");
    text.add(titleSpan, purpose);
    section.add(text, metadataSaveButton());
    return section;
  }

  private Button backToOverviewButton() {
    Button back = new Button("Back to overview", VaadinIcon.ARROW_LEFT.create());
    back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    back.addClassName("experiment-design-back");
    back.addClickListener(event -> getUI().ifPresent(
        ui -> ui.navigate(ExperimentInformationMain.class, experimentRouteParameters())));
    return back;
  }

  private Button metadataSaveButton() {
    Button save = new Button("Save experiment setup");
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    save.addClassName("experiment-metadata-save");
    save.addClickListener(event -> onSaveClicked());
    return save;
  }

  private void onSaveClicked() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    ProjectId projectId = context.projectId().orElseThrow();
    // The validationFailure branch matters here for the same reason it does on the create page:
    // without it a refused submit would do nothing visible at all, because the field errors are
    // attached to the fields rather than announced.
    designForm.writeDraftIfValid().ifPresentOrElse(
        draft -> save(projectId, experimentId, draft),
        () -> {
          designForm.focusFirstInvalidField();
          displayIncomplete();
        });
  }

  /**
   * Persists the setup and reports what happened.
   *
   * <p>The write runs on the scheduler rather than on the UI thread. It is a database round trip
   * followed by a domain-event dispatch, so on a busy instance it is slow enough to be noticed, and
   * a write made on the UI thread cannot paint anything while it runs — the pending indicator would
   * appear only once the save had already finished, which is worse than showing nothing.</p>
   *
   * <p>Success navigates back to the overview rather than staying put. The overview is where the
   * saved values are rendered, so returning there is what lets the user confirm the change took
   * effect; a toast alone left them on a form whose fields already looked correct before saving, so
   * there was nothing on screen that could show the write had happened.</p>
   * <p>Package-private rather than private so a spec can drive the save directly: reaching it
   * through the button would require populating the whole ontology form, and the behaviour worth
   * pinning — which branch runs, and whether the guard is reset — is independent of the field
   * values.</p>
   */
  void save(ProjectId projectId, ExperimentId experimentId, ExperimentDraft draft) {
    // The guard lives here rather than in the click handler so there is exactly one place that both
    // checks and sets it, adjacent to the state it protects. A second submission while one is in
    // flight would queue a second write and a second navigation.
    if (saveInProgress) {
      return;
    }
    saveInProgress = true;
    Toast pending = messageSourceNotificationFactory.pendingTaskToast("experiment.setup.saving",
        new Object[]{}, getLocale());
    pending.open();

    SecurityContext securityContext = SecurityContextHolder.getContext();
    // applySecurityContext is not optional here: @PreAuthorize on editExperimentInformation reads the
    // thread-local SecurityContextHolder, and the scheduler runs each task on its own fresh thread,
    // so without it the guard would deny every save. This is the same shape AsyncProjectServiceImpl
    // uses for its scheduler-bound work.
    //
    // fromCallable (not fromRunnable) on purpose: a Mono<Void> completes without ever emitting a
    // value, so subscribe(onNext, onError) would skip onNext entirely and the success path — toast
    // plus navigation — would silently never run. Returning the draft keeps a value in the stream.
    Mono<ExperimentDraft> call = Mono.fromCallable(() -> {
      experimentInformationService.editExperimentInformation(projectId.value(), experimentId,
          draft.getExperimentName(), draft.getSpecies(), draft.getSpecimens(),
          draft.getAnalytes());
      return draft;
    });
    applySecurityContext(call)
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        // The guard is released in doFinally rather than in the UI callbacks below, because those are
        // dropped when the UI has detached and the flag would then stay set for the lifetime of the
        // component. doFinally runs on every terminal signal, so a failure can never wedge the button.
        .doFinally(signal -> {
          saveInProgress = false;
          uiHandle.onUiAndPush(pending::close);
        })
        .subscribe(
            saved -> uiHandle.onUiAndPush(() -> onSaveSucceeded(projectId)),
            error -> uiHandle.onUiAndPush(() -> onSaveFailed(error, projectId, experimentId)));
  }

  private void onSaveSucceeded(ProjectId projectId) {
    displayToast("experiment.setup.updated.success", new Object[]{}, true);
    navigateToOverview(projectId);
  }

  /**
   * Failure keeps the user on the page with the values they entered, because navigating away would
   * discard them and leave nothing to retry. The pending indicator is already closed by the
   * terminal handler in {@link #save}.
   */
  private void onSaveFailed(Throwable error, ProjectId projectId, ExperimentId experimentId) {
    log.error("Could not save the setup of experiment %s in project %s".formatted(
        experimentId.value(), projectId.value()), error);
    displayToast("experiment.setup.updated.failed", new Object[]{}, false);
  }

  /**
   * Shows a toast, and for the success case keeps it open across the navigation that follows.
   *
   * <p>{@link Toast} closes itself on navigation by default, which would erase the confirmation in
   * the same user action that produced it.</p>
   */
  private void displayToast(String messageKey, Object[] parameters, boolean survivesNavigation) {
    Toast toast = messageSourceNotificationFactory.toast(messageKey, parameters, getLocale());
    if (survivesNavigation) {
      toast.closeOnNavigation(false);
    }
    toast.open();
  }

  private void navigateToOverview(ProjectId projectId) {
    getUI().ifPresent(ui -> ui.navigate(ExperimentInformationMain.class, new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectId.value()),
        new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER, context.experimentId().orElseThrow().value()))));
  }

  private void loadExperimentInformation(ProjectId projectId, ExperimentId experimentId) {
    Experiment experiment = experimentInformationService.find(projectId.value(), experimentId)
        .orElseThrow();
    ExperimentDraft draft = new ExperimentDraft();
    draft.setExperimentName(experiment.getName());
    draft.setSpecies(experiment.getSpecies());
    draft.setSpecimens(experiment.getSpecimens());
    draft.setAnalytes(experiment.getAnalytes());
    designForm.setDraft(draft);
    designForm.setUsedSampleOrigins(getSampleOriginUsage(experimentId));
  }

  private void displayIncomplete() {
    getUI().ifPresent(ui -> ui.access(() -> {
      Notification notification = messageSourceNotificationFactory.toast(
          "experiment.setup.incomplete", new Object[]{}, getLocale());
      notification.open();
    }));
  }

  private Map<SampleOriginType, Map<OntologyTerm, Long>> getSampleOriginUsage(
      ExperimentId experimentId) {
    Map<SampleOriginType, Map<OntologyTerm, Long>> result = new EnumMap<>(SampleOriginType.class);
    var projectId = context.projectId().orElseThrow();
    Collection<Sample> samples;
    try {
      samples = sampleInformationService.retrieveSamplesForExperiment(projectId,
          experimentId.value());
    } catch (RuntimeException e) {
      log.debug("Could not load sample ontology terms", e);
      samples = new ArrayList<>();
    }

    result.put(SPECIES,
        countTerms(samples.stream().map(sample -> sample.sampleOrigin().getSpecies())));
    result.put(SPECIMEN,
        countTerms(samples.stream().map(sample -> sample.sampleOrigin().getSpecimen())));
    result.put(ANALYTE,
        countTerms(samples.stream().map(sample -> sample.sampleOrigin().getAnalyte())));
    return result;
  }

  private static Map<OntologyTerm, Long> countTerms(Stream<OntologyTerm> terms) {
    return terms.filter(java.util.Objects::nonNull)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }

  private RouteParameters experimentRouteParameters() {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, context.projectId().orElseThrow().value()),
        new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER, context.experimentId().orElseThrow().value()));
  }
}
