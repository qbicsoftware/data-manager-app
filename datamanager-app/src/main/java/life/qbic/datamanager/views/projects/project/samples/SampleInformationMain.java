package life.qbic.datamanager.views.projects.project.samples;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.SortOrder;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.icon.IconFactory;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.samples.SampleDetailsComponent.SampleDeletionRequested;
import life.qbic.datamanager.views.projects.project.samples.SampleDetailsComponent.SampleEditRequested;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.EditSampleDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.RegisterSampleDialog;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectCode;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleRegistrationInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SampleMetadata;
import life.qbic.projectmanagement.application.sample.SampleRegistrationServiceV2;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Information Main Component
 * <p>
 * This component hosts the components necessary to show and update the information for all
 * {@link Sample} associated with all {@link Experiment} of a {@link Project} information via the
 * provided {@link ProjectId} in the URL
 */

@Route(value = "projects/:projectId?/experiments/:experimentId?/samples", layout = ProjectMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class SampleInformationMain extends Main implements BeforeEnterObserver, BeforeLeaveObserver {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;

  /**
   * The framework's own history state change handler, captured before this view installs its own
   * (same pattern as {@code MeasurementMain}).
   */
  private History.HistoryStateChangeHandler routerHistoryStateChangeHandler;
  private final History.HistoryStateChangeHandler listStateHistoryHandler = this::onHistoryStateChange;
  private String basePath;
  private boolean suppressUrlWrite;
  private ExperimentId shownExperimentId;

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private static final Logger log = LoggerFactory.logger(SampleInformationMain.class);
  private final transient ExperimentInformationService experimentInformationService;
  private final transient DeletionService deletionService;
  private transient Component sampleDetailsComponent;
  private final Div content = new Div();

  /**
   * Milestone band: groups exist, nothing registered yet. See {@link RegistrationMilestoneBand}.
   */
  private final RegistrationMilestoneBand milestoneBand = new RegistrationMilestoneBand();

  /**
   * Prerequisite gate: no experimental group yet. See {@link SamplePrerequisiteGate}.
   */
  private final SamplePrerequisiteGate prerequisiteGate = new SamplePrerequisiteGate();

  /**
   * Header status line: states what is registered, so the page always says where it stands.
   */
  private final Span statusLine = new Span();

  private final transient ProjectInformationService projectInformationService;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final transient SampleValidationService sampleValidationService;
  private final transient SampleRegistrationServiceV2 sampleRegistrationServiceV2;
  private final transient AsyncProjectService asyncProjectService;
  private final transient UploadConfiguration uploadConfiguration;
  private final transient MeasurementService measurementService;
  private final transient SampleInformationService sampleInformationService;
  private final transient UserPermissions userPermissions;
  private final MessageSourceNotificationFactory messageFactory;
  private final Button registerButton;
  private transient Context context;

  public SampleInformationMain(@Autowired ExperimentInformationService experimentInformationService,
      @Autowired DeletionService deletionService,
      @Autowired AsyncProjectService asyncProjectService,
      ProjectInformationService projectInformationService,
      MessageSourceNotificationFactory notificationFactory,
      SampleValidationService sampleValidationService,
      SampleRegistrationServiceV2 sampleRegistrationServiceV2,
      MessageSourceNotificationFactory messageSourceNotificationFactory,
      UploadConfiguration uploadConfiguration,
      @Autowired MeasurementService measurementService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired UserPermissions userPermissions) {
    this.asyncProjectService = requireNonNull(asyncProjectService);
    this.deletionService = requireNonNull(deletionService);
    this.experimentInformationService = requireNonNull(experimentInformationService);
    this.notificationFactory = requireNonNull(notificationFactory);
    this.projectInformationService = requireNonNull(projectInformationService);
    this.sampleRegistrationServiceV2 = requireNonNull(sampleRegistrationServiceV2);
    this.sampleValidationService = requireNonNull(sampleValidationService);
    this.uploadConfiguration = requireNonNull(uploadConfiguration);
    this.measurementService = requireNonNull(measurementService);
    this.sampleInformationService = requireNonNull(sampleInformationService);
    this.userPermissions = requireNonNull(userPermissions);

    addClassName("sample");

    DownloadComponent downloadComponent = new DownloadComponent();
    this.sampleDetailsComponent = new Div();

    Span titleField = new Span();
    titleField.setText("Samples");
    titleField.addClassNames("title");

    statusLine.addClassName("sample-status-line");

    Button registerButton = new Button("Register samples", VaadinIcon.PLUS.create());
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    registerButton.addClassName("sample-register-button");
    registerButton.addClickListener(clicked -> openRegistrationDialog());
    this.registerButton = registerButton;

    Div titleBlock = new Div(titleField, statusLine);
    titleBlock.addClassName("sample-header-title-block");

    Div header = new Div(titleBlock, registerButton);
    header.addClassName("sample-main-header");

    // The gate is the only path forward from "no groups", so it is never dismissible; the
    // milestone states an achievement and can be dismissed for the session.
    prerequisiteGate.addDefineDesignRequestedListener(event -> routeToExperimentDesign());
    milestoneBand.addRegisterListener(event -> openRegistrationDialog());
    milestoneBand.addReviewDesignListener(event -> routeToExperimentDesign());

    content.add(header);
    content.addClassName("sample-main-content");
    add(prerequisiteGate, milestoneBand, content);
    add(sampleDetailsComponent);
    add(downloadComponent);

    this.messageFactory = messageSourceNotificationFactory;
  }

  private static boolean noExperimentGroupsInExperiment(Experiment experiment) {
    return experiment.getExperimentalGroups().isEmpty();
  }

  private void openRegistrationDialog() {
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId experimentId = context.experimentId().orElseThrow();

    Experiment experiment = experimentInformationService.find(projectId.value(), experimentId)
        .orElseThrow();

    if (experiment.getExperimentalGroups().isEmpty()) {
      return;
    }
    ProjectOverview projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow();
    UI ui = UI.getCurrent();
    var registerSampleDialog = RegisterSampleDialog.create(
        asyncProjectService, messageFactory,
        experimentId.value(),
        projectId.value(),
        projectOverview.projectCode(),
        uploadConfiguration,
        sampleMetadata -> submitSampleRegistration(sampleMetadata, projectId, experimentId, ui));
    // cancellation (incl. the discard-changes confirmation) is handled by the AppDialog itself
    registerSampleDialog.open();
  }

  private void submitSampleRegistration(
      List<SampleRegistrationInformation> sampleMetadata, ProjectId projectId,
      ExperimentId experimentId, UI ui) {
    var pendingToast = notificationFactory.pendingTaskToast("task.in-progress",
        new Object[]{"Sample registration for %d samples".formatted(sampleMetadata.size())},
        getLocale());
    ui.access(pendingToast::open);

    CompletableFuture<Void> registrationTask = sampleRegistrationServiceV2
        .registerSamples(sampleMetadata, projectId,
            new ExperimentReference(experimentId.value()))
        .orTimeout(5, TimeUnit.MINUTES);
    try {
      registrationTask
          .exceptionally(e -> {
            ui.access(() -> {
              //this needs to come before all the success events
              pendingToast.close();
              notificationFactory.toast("task.failed",
                  new Object[]{"Sample registration"}, getLocale()).open();
            });
            throw new HandledException(e);
          })
          .thenRun(() -> ui.access(this::setBatchAndSampleInformation))
          .thenRun(() -> ui.access(() -> {
            pendingToast.close();
            displayRegistrationSuccess();
          }))
          .exceptionally(e -> {
            //we need to make sure we do not swallow exceptions but still stay in the exceptional state.
            throw new HandledException(e); //we need the future to complete exceptionally
          });
    } catch (HandledException e) {
      // we only log the exception as the user was presented with the error already and nothing we can do here.
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Routes to the experimental design surface, where the missing experimental group lives (gate) or
   * where the design that unlocked registration can be reviewed (milestone).
   */
  private void routeToExperimentDesign() {
    String routeToExperimentPage = String.format(ProjectRoutes.EXPERIMENT,
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow().value());
    getUI().ifPresent(ui -> ui.navigate(routeToExperimentPage));
  }

  private void displayUpdateSuccess() {
    notificationFactory.toast("sample.updated.success", new String[]{}, getLocale())
        .open();
  }

  private void displayDeletionSuccess(int numberOfDeleted) {
    notificationFactory.toast("sample.deleted.success", new String[]{String.valueOf(numberOfDeleted)},
            getLocale())
        .open();
  }

  private void displayRegistrationSuccess() {
    notificationFactory.toast("sample.registered.success",
            new String[]{},
            getLocale())
        .open();

  }

  private void onEditSamplesClicked(SampleEditRequested editRequest) {
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId experimentId = context.experimentId().orElseThrow();

    Experiment experiment = experimentInformationService.find(projectId.value(), experimentId)
        .orElseThrow();

    if (experiment.getExperimentalGroups().isEmpty()) {
      return;
    }
    ProjectOverview projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow();
    var sampleIds = editRequest.sampleIds().stream()
        .map(SampleId::value)
        .collect(Collectors.toSet());
    UI ui = UI.getCurrent();
    var editSampleDialog = EditSampleDialog.create(
        asyncProjectService, messageFactory,
        sampleIds,
        experimentId.value(),
        projectId.value(),
        projectOverview.projectCode(),
        sampleValidationService,
        uploadConfiguration,
        sampleMetadata -> submitSampleUpdate(sampleMetadata, projectId, ui));
    // cancellation (incl. the discard-changes confirmation) is handled by the AppDialog itself
    editSampleDialog.open();
  }

  private void submitSampleUpdate(List<SampleMetadata> sampleMetadata, ProjectId projectId,
      UI ui) {
    var pendingToast = notificationFactory.pendingTaskToast("task.in-progress",
        new Object[]{"Sample update for %d samples".formatted(sampleMetadata.size())},
        getLocale());
    ui.access(pendingToast::open);

    CompletableFuture<Void> editTask = sampleRegistrationServiceV2.updateSamples(
            sampleMetadata,
            projectId,
            new ExperimentReference(context.experimentId().orElseThrow().value()))
        .orTimeout(5, TimeUnit.MINUTES);
    try {
      editTask
          .exceptionally(e -> {
            ui.access(() -> {
              //this needs to come before all the success events
              pendingToast.close();
              notificationFactory.toast("task.failed",
                      new String[]{"Sample update"}, getLocale())
                  .open();
            });
            throw new HandledException(e);
          })
          .thenRun(() -> ui.access(this::setBatchAndSampleInformation))
          .thenRun(() -> ui.access(() -> {
            pendingToast.close();
            displayUpdateSuccess();
          }))
          .exceptionally(e -> {
            //we need to make sure we do not swallow exceptions but still stay in the exceptional state.
            throw new HandledException(e); //we need the future to complete exceptionally
          });
    } catch (HandledException e) {
      // we only log the exception as the user was presented with the error already and nothing we can do here.
      log.error(e.getMessage(), e);
    }
  }

  private void deleteSamples(SampleDeletionRequested deletionRequest) {
    var projectId = context.projectId().orElseThrow();
    var pendingToast = notificationFactory.pendingTaskToast("task.in-progress",
        new Object[]{"Sample deletion for %d samples".formatted(deletionRequest.sampleIds().size())},
        getLocale());
    pendingToast.open();

    CompletableFuture<Void> deletionTask = deletionService.deleteSamplesAsync(projectId,
            deletionRequest.sampleIds())
        .orTimeout(5, TimeUnit.MINUTES);
    deletionTask
        .thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {
          pendingToast.close();
          displayDeletionSuccess(deletionRequest.sampleIds().size());
          deselectDeletedSamples(deletionRequest.sampleIds());
          setBatchAndSampleInformation();
        })))
        .exceptionally(e -> {
          log.error("Sample deletion failed", e);
          getUI().ifPresent(ui -> ui.access(() -> {
            pendingToast.close();
            notificationFactory.toast("task.failed",
                new Object[]{"Sample deletion"}, getLocale()).open();
          }));
          return null;
        });
  }

  /**
   * Removes the deleted sample identifiers from the grid's cross-page selection, so stale IDs of
   * removed samples do not inflate the selection counter or leak into bulk actions after deletion.
   */
  private void deselectDeletedSamples(List<SampleId> deletedSampleIds) {
    if (sampleDetailsComponent instanceof SampleDetailsComponent sampleDetails) {
      Set<String> deletedIds = deletedSampleIds.stream().map(SampleId::value)
          .collect(Collectors.toSet());
      sampleDetails.paginatedGrid().deselect(deletedIds);
    }
  }

  private void onDeleteSamplesClicked(SampleDeletionRequested deletionRequest) {
    Set<String> sampleIds = deletionRequest.sampleIds().stream().map(SampleId::value)
        .collect(Collectors.toSet());
    Set<String> measuredSampleIds = measurementService.findMeasuredSampleIds(
        context.projectId().orElseThrow(), context.experimentId().orElseThrow(), sampleIds);
    if (!measuredSampleIds.isEmpty()) {
      showBlockedDeletionDialog(measuredSampleIds);
      return;
    }
    AlertDialog.danger(this,
        "Samples will be deleted",
        "Deleting these samples will also delete the data connected to them. Proceed?",
        "Delete samples",
        "Keep samples",
        () -> deleteSamples(deletionRequest)).open();
  }

  private void showBlockedDeletionDialog(Set<String> measuredSampleIds) {
    var projectId = context.projectId().orElseThrow();
    List<Sample> measuredSamples = sampleInformationService.retrieveSamplesByIds(projectId,
        measuredSampleIds.stream().map(SampleId::parse).toList());
    SortOrder currentSort = currentSampleSort();
    measuredSamples.sort(sampleComparator(currentSort));

    var dialog = AppDialog.small();
    DialogHeader.withIcon(dialog, "Samples cannot be deleted", IconFactory.warningIcon());

    var list = new UnorderedList();
    measuredSamples.stream().limit(20)
        .forEach(sample -> list.add(new ListItem(
            "%s (%s)".formatted(sample.label(), sample.sampleCode().code()))));
    if (measuredSamples.size() > 20) {
      list.add(new ListItem("and %d more".formatted(measuredSamples.size() - 20)));
    }
    var message = new Div(new Span(
        "The following samples have associated measurements and therefore cannot be deleted:"),
        list);

    DialogBody.withoutUserInput(dialog, message);
    DialogFooter.withConfirmOnly(dialog, "Close");
    dialog.registerConfirmAction(dialog::close);
    dialog.open();
  }

  /**
   * @return the current sort order of the sample grid, or the default sample sort if the grid is
   * not available
   */
  private SortOrder currentSampleSort() {
    if (sampleDetailsComponent instanceof SampleDetailsComponent sampleDetails) {
      return sampleDetails.listState().sort();
    }
    return SampleSort.DEFAULT;
  }

  /**
   * Builds a {@link Comparator} for {@link Sample}s matching the sample grid's current sort order,
   * with the deterministic {@code sampleCode} tie-break the grid applies for pagination.
   *
   * @param sortOrder the sort order applied by the sample grid
   */
  private static Comparator<Sample> sampleComparator(SortOrder sortOrder) {
    Comparator<Sample> byProperty = sampleComparatorFor(sortOrder.propertyName());
    if (sortOrder.isDescending()) {
      byProperty = byProperty.reversed();
    }
    return byProperty.thenComparing(sample -> sample.sampleCode().code());
  }

  private static Comparator<Sample> sampleComparatorFor(String property) {
    return switch (property) {
      case "sampleId" -> Comparator.comparing(sample -> sample.sampleCode().code());
      case "sampleName" -> Comparator.comparing(Sample::label);
      case "biologicalReplicate" -> Comparator.comparing(Sample::biologicalReplicate,
          Comparator.nullsLast(String::compareTo));
      case "batch" -> Comparator.comparing(Sample::batch,
          Comparator.nullsLast(String::compareTo));
      case "condition" -> Comparator.comparing(sample -> sample.sampleCode().code());
      case "species" -> Comparator.comparing(sample -> sample.sampleOrigin().getSpecies().getLabel());
      case "specimen" -> Comparator.comparing(
          sample -> sample.sampleOrigin().getSpecimen().getLabel());
      case "analyte" -> Comparator.comparing(
          sample -> sample.sampleOrigin().getAnalyte().getLabel());
      case "analysisMethod" -> Comparator.comparing(sample -> sample.analysisMethod().label());
      case "comment" -> Comparator.comparing(sample -> sample.comment().orElse(""));
      case "registrationTime" -> Comparator.comparing(Sample::registrationTime,
          Comparator.nullsLast(Comparator.naturalOrder()));
      case "lastModified" -> Comparator.comparing(Sample::lastModified,
          Comparator.nullsLast(Comparator.naturalOrder()));
      default -> Comparator.comparing(sample -> sample.sampleCode().code());
    };
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectID = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    context = new Context().with(parsedProjectId);
    String experimentId = event.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ExperimentId.isValid(experimentId)) {
      throw new ApplicationException("invalid experiment id " + experimentId);
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
    this.context = context.with(parsedExperimentId);

    this.context = context.withProjectCode(asyncProjectService.getProjectCode(projectID)
        .blockOptional()
        .map(ProjectCode::value)
        .orElse(null));

    // URL list-state synchronization (USER-R-03, FEAT-PAG-LIST-02): capture the router handler,
    // install our own, and seed the list from the URL on direct load / reload / shared links.
    History history = UI.getCurrent().getPage().getHistory();
    History.HistoryStateChangeHandler currentHandler = history.getHistoryStateChangeHandler();
    if (currentHandler != listStateHistoryHandler) {
      routerHistoryStateChangeHandler = currentHandler;
    }
    history.setHistoryStateChangeHandler(listStateHistoryHandler);
    basePath = String.format(ProjectRoutes.SAMPLES, projectID, experimentId);

    // Building the component auto-loads the default list state and would mirror it into the URL;
    // suppress that initial write until the URL state has been applied so a reloaded or shared
    // link is restored exactly, not overwritten by the defaults.
    suppressUrlWrite = true;
    try {
      setBatchAndSampleInformation();
      if (sampleDetailsComponent instanceof SampleDetailsComponent sampleDetails) {
        ListState urlState = ListStateCodec.parse(event.getLocation().getQueryParameters(),
            SampleSort.DEFAULT, SampleSort.allowedSortOrders());
        sampleDetails.applyExternalState(urlState);
      }
    } finally {
      suppressUrlWrite = false;
    }
  }

  /**
   * Gives the history state change handler back to the router when this view is left.
   */
  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    getUI().ifPresent(ui -> ui.getPage().getHistory()
        .setHistoryStateChangeHandler(routerHistoryStateChangeHandler));
  }

  /**
   * Re-applies the list state when the browser history changes (back/forward or router-link
   * navigation). Non-sample locations are delegated back to the router handler.
   */
  private void onHistoryStateChange(HistoryStateChangeEvent event) {
    String expectedPath = currentSamplesPath();
    if (expectedPath == null || !expectedPath.equals(event.getLocation().getPath())) {
      if (routerHistoryStateChangeHandler != null) {
        routerHistoryStateChangeHandler.onHistoryStateChange(event);
      }
      return;
    }
    if (sampleDetailsComponent instanceof SampleDetailsComponent sampleDetails) {
      suppressUrlWrite = true;
      try {
        ListState urlState = ListStateCodec.parse(event.getLocation().getQueryParameters(),
            SampleSort.DEFAULT, SampleSort.allowedSortOrders());
        sampleDetails.applyExternalState(urlState);
      } finally {
        suppressUrlWrite = false;
      }
    }
  }

  private String currentSamplesPath() {
    if (context == null || context.projectId().isEmpty() || context.experimentId().isEmpty()) {
      return null;
    }
    return String.format(ProjectRoutes.SAMPLES,
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow().value());
  }

  private void writeUrl(ListState state) {
    if (basePath == null || suppressUrlWrite) {
      return;
    }
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    Location location = new Location(basePath, ListStateCodec.toQueryParameters(state));
    ui.getPage().getHistory().pushState(null, location);
  }

  private void setBatchAndSampleInformation() {
    var experiment = experimentInformationService.find(context.projectId().orElseThrow().value(),
        context.experimentId()
            .orElseThrow()).orElseThrow();
    if (noExperimentGroupsInExperiment(experiment)) {
      showRegisterGroupsDisclaimer();
      return;
    }
    if (noSamplesRegisteredInExperiment(experiment)) {
      showRegisterBatchDisclaimer();
    } else {
      reloadSampleInformation();
      showBatchAndSampleInformation();
    }
  }

  private boolean noSamplesRegisteredInExperiment(Experiment experiment) {
    var result = asyncProjectService
        .getSamples(context.projectId().orElseThrow().value(), experiment.experimentId().value())
        .blockFirst();
    return result == null;
  }

  /**
   * Blocked state: the experiment has no experimental group, so nothing can be registered yet. The
   * page stays visible (header and context) and only the grid is withheld -- the user must be able
   * to see where they are, and the gate is the single path forward.
   */
  private void showRegisterGroupsDisclaimer() {
    Experiment experiment = currentExperiment();
    prerequisiteGate.update(0, variablesOf(experiment));
    prerequisiteGate.setActionVisible(canWrite());
    prerequisiteGate.setVisible(true);
    milestoneBand.setVisible(false);
    content.setVisible(true);
    sampleDetailsComponent.setVisible(false);
    registerButton.setVisible(false);
    updateStatusLine(0, null);
  }

  /**
   * Unlocked, nothing registered: the state the page previously expressed by hiding itself. The
   * milestone band names the achievement, states the facts that unlocked it, and carries the single
   * primary action; the grid area holds text only, so exactly one call to action is on screen.
   */
  private void showRegisterBatchDisclaimer() {
    Experiment experiment = currentExperiment();
    String experimentId = context.experimentId().orElseThrow().value();
    prerequisiteGate.setVisible(false);
    boolean dismissed = RegistrationMilestoneBand.isDismissed(experimentId);
    milestoneBand.setExperiment(experimentId, experiment.getExperimentalGroups().size(),
        variablesOf(experiment));
    // The milestone states an achievement, so it is shown to everyone; only its action depends on
    // write permission. Hiding the whole band would withhold the explanation, not just the button.
    milestoneBand.setActionVisible(canWrite());
    milestoneBand.setVisible(!dismissed);
    // The page stays visible in this state, so the user sees the header, the scope and the
    // achievement. Only the grid is withheld: there is nothing to list yet.
    content.setVisible(true);
    sampleDetailsComponent.setVisible(false);
    // A read-only user is shown the state but not the action: registration would only fail.
    registerButton.setVisible(canWrite());
    updateStatusLine(0, null);
  }

  /**
   * Populated state: samples exist. Both bands are absent -- the milestone states a transition and
   * is therefore never shown as furniture to a returning user.
   */
  private void showBatchAndSampleInformation() {
    prerequisiteGate.setVisible(false);
    milestoneBand.setVisible(false);
    content.setVisible(true);
    sampleDetailsComponent.setVisible(true);
    registerButton.setVisible(canWrite());
    // The header states the experiment's registration total, which a search does not change.
    // It must not be written from the grid's page total: that total counts only the samples
    // matching the active filter, so searching silently relabelled a match count as a count of
    // registered samples -- "24 samples registered" for an experiment holding 299.
    updateStatusLine(registeredSampleCount(), null);
  }

  /**
   * The number of samples registered in the current experiment, regardless of any active search.
   *
   * <p>Distinct from the grid's page total, which counts only what the filter matches. The API
   * resolves this to {@link reactor.core.publisher.Mono#empty()} when the experiment holds no
   * samples, so an absent value means zero.</p>
   *
   * @return the experiment's registered sample count
   */
  private long registeredSampleCount() {
    return asyncProjectService
        .countSamples(context.projectId().orElseThrow().value(),
            context.experimentId().orElseThrow().value())
        .blockOptional()
        .orElse(0);
  }

  /**
   * @return whether the current user may change sample data in this project. The page used to offer
   * registration, editing and deletion to every user, so a read-only user was handed actions that
   * could only fail.
   */
  private boolean canWrite() {
    return context != null && context.projectId().map(userPermissions::editProject).orElse(false);
  }

  private static int variablesOf(Experiment experiment) {
    return experiment.variables() == null ? 0 : experiment.variables().size();
  }

  private Experiment currentExperiment() {
    return experimentInformationService.find(context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow()).orElseThrow();
  }

  /**
   * States what the page currently holds, so a populated page always answers "how many samples are
   * there?" above the fold.
   *
   * @param count    the total number of registered samples
   * @param lastRegistered the most recent registration time, or {@code null} when unknown
   */
  private void updateStatusLine(long count, Instant lastRegistered) {
    if (count <= 0) {
      statusLine.setText("0 samples registered");
      return;
    }
    String countText = count == 1 ? "1 sample registered" : "%d samples registered".formatted(count);
    if (lastRegistered == null) {
      statusLine.setText(countText);
      return;
    }
    statusLine.setText(countText + " \u00b7 last registered "
        + DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_TIME_SHORT, ZoneId.systemDefault())
            .format(lastRegistered));
  }

  private void reloadSampleInformation() {
    ExperimentId currentExperiment = context.experimentId().orElseThrow();
    boolean experimentChanged = !currentExperiment.equals(shownExperimentId);
    if (sampleDetailsComponent instanceof SampleDetailsComponent sampleDetails
        && !experimentChanged) {
      // Same experiment (e.g. after a sample update/registration): preserve the current list state
      // (page/filter/sort) and URL params and simply reload the data in place, mirroring the
      // measurement list. Recreating the component would lose the load trigger and show an empty
      // grid after editing.
      sampleDetails.refresh();
      return;
    }
    shownExperimentId = currentExperiment;
    remove(sampleDetailsComponent);
    var sampleDetails = new SampleDetailsComponent(asyncProjectService, messageFactory, context);
    sampleDetails.addSampleEditListener(this::onEditSamplesClicked);
    sampleDetails.addSampleDeletionListener(this::onDeleteSamplesClicked);
    // Mirror the list state into the URL whenever a page is loaded (page/filter/sort change),
    // so the list is restorable and shareable (USER-R-03, FEAT-PAG-LIST-02).
    sampleDetails.addPageLoadedListener(event -> {
      if (sampleDetailsComponent instanceof SampleDetailsComponent component) {
        writeUrl(component.listState());
      }
      // Deliberately no status-line update here. This event fires on every page, filter and sort
      // change, and its total is the count matching the active filter; letting it write the header
      // made the header report search results as registrations. The header is written once per
      // populated load in showBatchAndSampleInformation() from the unfiltered count instead, and
      // the filtered total is stated beside the search field by SampleDetailsComponent.
    });
    sampleDetailsComponent = sampleDetails;
    add(sampleDetailsComponent);
  }

  private static class HandledException extends RuntimeException {

    public HandledException(Throwable cause) {
      super(cause);
    }
  }
}
