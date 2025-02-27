package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style.Visibility;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.files.export.FileNameFormatter;
import life.qbic.datamanager.files.export.download.WorkbookDownloadStreamProvider;
import life.qbic.datamanager.files.export.measurement.NGSWorkbooks;
import life.qbic.datamanager.files.export.measurement.ProteomicsWorkbooks;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMetadataUploadDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMetadataUploadDialog.MODE;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateListComponent.DownloadMeasurementTemplateEvent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.measurement.MeasurementService.MeasurementDeletionException;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Measurement Main Component
 * <p>
 * This component hosts the components necessary to show and update the Measurement information
 * associated with an {@link Experiment} within a {@link Project} via the provided
 * {@link ExperimentId} and {@link ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/measurements", layout = ExperimentMainLayout.class)
@PermitAll
public class MeasurementMain extends Main implements BeforeEnterObserver, BeforeLeaveObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(MeasurementMain.class);
  private static Disclaimer registerSamplesDisclaimer;
  private final DownloadComponent measurementTemplateDownload;
  private final MeasurementTemplateListComponent measurementTemplateListComponent;
  private final Span measurementsSelectedInfoBox = new Span();
  private final MeasurementDetailsComponent measurementDetailsComponent;
  private final transient MeasurementPresenter measurementPresenter;
  private final TextField measurementSearchField = new TextField();
  private final transient SampleInformationService sampleInformationService;
  private final transient MeasurementService measurementService;
  private final transient MeasurementValidationService measurementValidationService;
  private final Div content = new Div();
  private final InfoBox rawDataAvailableInfo = new InfoBox();
  private final Div noMeasurementDisclaimer;
  private final DownloadComponent downloadComponent;
  private final transient ProjectInformationService projectInformationService;
  private final transient CancelConfirmationDialogFactory cancelConfirmationDialogFactory;
  private final transient MessageSourceNotificationFactory messageFactory;
  private final ExperimentInformationService experimentInformationService;
  private MeasurementMetadataUploadDialog dialog;
  private transient Context context;

  public MeasurementMain(
      @Autowired MeasurementTemplateListComponent measurementTemplateListComponent,
      @Autowired MeasurementDetailsComponent measurementDetailsComponent,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired MeasurementService measurementService,
      @Autowired MeasurementPresenter measurementPresenter,
      @Autowired MeasurementValidationService measurementValidationService,
      ProjectInformationService projectInformationService,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      MessageSourceNotificationFactory messageFactory,
      ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(measurementTemplateListComponent);
    Objects.requireNonNull(measurementDetailsComponent);
    Objects.requireNonNull(measurementService);
    Objects.requireNonNull(measurementValidationService);
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.measurementDetailsComponent = measurementDetailsComponent;
    this.measurementTemplateListComponent = measurementTemplateListComponent;
    this.measurementService = measurementService;
    this.measurementPresenter = measurementPresenter;
    this.measurementValidationService = measurementValidationService;
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.projectInformationService = projectInformationService;
    this.cancelConfirmationDialogFactory = cancelConfirmationDialogFactory;

    downloadComponent = new DownloadComponent();
    measurementTemplateDownload = new DownloadComponent();
    measurementTemplateListComponent.addDownloadMeasurementTemplateClickListener(
        this::onDownloadMeasurementTemplateClicked);
    registerSamplesDisclaimer = createNoSamplesRegisteredDisclaimer();
    add(registerSamplesDisclaimer);
    noMeasurementDisclaimer = createNoMeasurementDisclaimer();
    add(noMeasurementDisclaimer);
    initContent();
    add(measurementTemplateListComponent);
    add(measurementTemplateDownload);
    add(measurementDetailsComponent);

    measurementDetailsComponent.addListener(
        selectionChangedEvent -> setSelectedMeasurementsInfo(
            selectionChangedEvent.getSource().getNumberOfSelectedMeasurements()));

    add(downloadComponent);
    addClassName("measurement");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        getClass().getSimpleName(), System.identityHashCode(this),
        measurementTemplateListComponent.getClass().getSimpleName(),
        System.identityHashCode(measurementTemplateListComponent)));
    this.experimentInformationService = experimentInformationService;
  }

  private static String convertErrorCodeToMessage(MeasurementService.ErrorCode errorCode) {
    return switch (errorCode) {
      case FAILED -> "Registration failed";
      case UNKNOWN_ORGANISATION_ROR_ID -> "Could not resolve ROR identifier.";
      case UNKNOWN_ONTOLOGY_TERM -> "Encountered unknown ontology term.";
      case WRONG_EXPERIMENT -> "There are samples that do not belong to this experiment.";
      case MISSING_ASSOCIATED_SAMPLE -> "Missing sample information for this measurement.";
      case MISSING_MEASUREMENT_ID -> "Missing measurement identifier";
      case SAMPLECODE_NOT_FROM_PROJECT -> "QBiC sample ID does not belong to this project";
      case UNKNOWN_MEASUREMENT -> "Unknown measurements, please check the identifiers.";
    };
  }

  private void initContent() {
    Span titleField = new Span();
    titleField.setText("Register Measurements");
    titleField.addClassNames("title");
    content.add(titleField);
    initRawDataAvailableInfo();
    initSearchFieldAndButtonBar();
    add(content);
    content.addClassName("measurement-main-content");
  }

  private void initSearchFieldAndButtonBar() {
    measurementSearchField.setPlaceholder("Search");
    measurementSearchField.setClearButtonVisible(true);
    measurementSearchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    measurementSearchField.addClassNames("search-field");
    measurementSearchField.setValueChangeMode(ValueChangeMode.LAZY);
    measurementSearchField.addValueChangeListener(
        event -> measurementDetailsComponent.setSearchedMeasurementValue((event.getValue())));
    Button downloadButton = new Button("Download Metadata");
    downloadButton.addClickListener(event -> downloadMetadataForSelectedTab());
    Button registerMeasurementButton = new Button("Register Measurements");
    registerMeasurementButton.addClassName("primary");
    registerMeasurementButton.addClickListener(
        event -> openRegisterMeasurementDialog());

    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> openEditMeasurementDialog());

    Button deleteButton = new Button("Delete");
    deleteButton.addClickListener(event -> onDeleteMeasurementsClicked());

    Span buttonBar = new Span(downloadButton, editButton, deleteButton, registerMeasurementButton);
    buttonBar.addClassName("button-bar");
    Span buttonsAndSearch = new Span(measurementSearchField, buttonBar);
    buttonsAndSearch.addClassName("buttonAndField");
    measurementsSelectedInfoBox.addClassName("info");
    setSelectedMeasurementsInfo(0);
    Div interactionsAndInfo = new Div(buttonsAndSearch, measurementsSelectedInfoBox);
    interactionsAndInfo.addClassName("buttonsAndInfo");
    content.add(interactionsAndInfo);
  }

  private void onDeleteMeasurementsClicked() {
    Optional<String> tabLabel = measurementDetailsComponent.getSelectedTabName();
    if (tabLabel.isEmpty()) {
      return;
    }
    String label = tabLabel.get();
    if (label.equals("Proteomics")) {
      handlePtxDeletionRequest(measurementDetailsComponent.getSelectedProteomicsMeasurements());
    }
    if (label.equals("Genomics")) {
      handleNGSDeletionRequest(measurementDetailsComponent.getSelectedNGSMeasurements());
    }
  }

  private void handlePtxDeletionRequest(Set<ProteomicsMeasurement> measurements) {
    if (measurements.isEmpty()) {
      return;
    }
    MeasurementDeletionConfirmationNotification notification =
        new MeasurementDeletionConfirmationNotification(
            "Selected proteomics measurements will be deleted", measurements.size());
    notification.open();
    notification.addConfirmListener(event -> {
      deletePtxMeasurements(measurements);
      notification.close();
    });
    notification.addCancelListener(event -> notification.close());
  }

  private void handleNGSDeletionRequest(Set<NGSMeasurement> measurements) {
    if (measurements.isEmpty()) {
      return;
    }
    MeasurementDeletionConfirmationNotification notification =
        new MeasurementDeletionConfirmationNotification(
            "Selected genomics measurements will be deleted", measurements.size());
    notification.open();
    notification.addConfirmListener(event -> {
      deleteNGSMeasurements(measurements);
      notification.close();
    });
    notification.addCancelListener(event -> notification.close());
  }

  private void deleteNGSMeasurements(Set<NGSMeasurement> measurements) {
    Result<Void, MeasurementDeletionException> result = measurementService.deleteNGSMeasurements(
        context.projectId().orElseThrow(), measurements);
    handleDeletionResults(result);
  }

  private void deletePtxMeasurements(Set<ProteomicsMeasurement> measurements) {
    Result<Void, MeasurementDeletionException> result = measurementService.deletePxPMeasurements(
        context.projectId().orElseThrow(), measurements);
    handleDeletionResults(result);
  }

  private void handleDeletionResults(Result<Void, MeasurementDeletionException> result) {
    result.onError(error -> {
      String errorMessage = switch (error.reason()) {
        case FAILED -> "Deletion failed. Please try again.";
        case DATA_ATTACHED -> "Data is attached to one or more measurements.";
      };
      showErrorNotification("Deletion failed", errorMessage);
    });
    result.onValue(v -> measurementDetailsComponent.refreshGrids());
  }

  private Dialog setupDialog(MeasurementMetadataUploadDialog dialog) {
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmListener(this::triggerMeasurementRegistration);
    return dialog;
  }

  private void triggerMeasurementRegistration(
      ConfirmEvent event) {
    var uploads = new ArrayList<>(event.uploads());
    var dialog = event.getSource();
    var mode = dialog.getMode();
    UI ui = event.getSource().getUI().orElseThrow();
    for (var upload : uploads) {
      var measurementData = upload.measurementMetadata();

      //Necessary so the dialog window switches to show the upload progress
      ui.push();
      CompletableFuture<List<Result<MeasurementId, MeasurementService.ErrorCode>>> completableFuture;
      Toast pendingToast;

      // we can close the dialog, everything that comes now is executed concurrently and there is
      // no need the user is blocked
      event.getSource().close();

      if (mode.equals(MODE.EDIT)) {
        completableFuture = measurementService.updateAll(measurementData,
            context.projectId().orElseThrow());
        pendingToast = messageFactory.pendingTaskToast("task.in-progress", new Object[]{
                "Update of #%d measurements".formatted(measurementData.stream()
                    .map(measurementMetadata -> measurementMetadata.measurementIdentifier().orElse(""))
                    .distinct().toList().size())},
            getLocale());
      } else {
        completableFuture = measurementService.registerAll(upload.measurementMetadata(),
            context.projectId().orElseThrow());
        pendingToast = messageFactory.pendingTaskToast("task.in-progress", new Object[]{
                "Registration of #%d measurements".formatted(upload.measurementMetadata().size())},
            getLocale());
      }
      ui.access(pendingToast::open);
      try {
        completableFuture.exceptionally(e -> {
              log.error(e.getMessage(), e);
              ui.access(() -> {
                pendingToast.close();
                messageFactory.toast("task.failed", new Object[]{"Registration failed"}, getLocale())
                    .open();
              });
              throw new HandledException(e);
            })
            .thenAccept(results -> {
              var errorResult = results.stream().filter(Result::isError).findAny();
              if (errorResult.isPresent()) {
                String detailedMessage = convertErrorCodeToMessage(errorResult.get().getError());
                ui.access(() -> {
                  pendingToast.close();
                  messageFactory.toast("task.failed", new Object[]{detailedMessage}, getLocale())
                      .open();
                });
              } else {
                ui.access(() -> {
                  pendingToast.close();
                  messageFactory.toast("task.finished", new Object[]{"Measurement update"},
                      getLocale()).open();
                  setMeasurementInformation();
                });
              }
            }).exceptionally(e -> {
              throw new HandledException(e);
            });
      } catch (HandledException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  private void openEditMeasurementDialog() {
    this.dialog = new MeasurementMetadataUploadDialog(measurementValidationService,
        cancelConfirmationDialogFactory,
        MODE.EDIT,
        context.projectId().orElse(null));
    setupDialog(dialog);
    dialog.open();
  }

  private void downloadMetadataForSelectedTab() {
    Optional<String> tabLabel = measurementDetailsComponent.getSelectedTabName();
    if (tabLabel.isEmpty()) {
      return;
    }
    switch (tabLabel.get()) {
      case "Proteomics": {
        downloadProteomicsMetadata();
        return;
      }
      case "Genomics": {
        downloadNGSMetadata();
        return;
      }
      default:
        throw new ApplicationException(
            "Unknown tab: " + measurementDetailsComponent.getSelectedTabName());
    }
  }

  private void downloadProteomicsMetadata() {
    ProjectId projectId = context.projectId().orElseThrow();
    String projectCode = projectInformationService.find(projectId)
        .orElseThrow()
        .getProjectCode().value();

    var experimentId = context.experimentId().orElseThrow();
    String experimentName = experimentInformationService.find(projectId.value(), experimentId)
        .orElseThrow()
        .getName();

    var proteomicsMeasurements = measurementService.findProteomicsMeasurements(
        context.experimentId().orElseThrow(() -> new ApplicationException(
            ErrorCode.GENERAL, null)),
        context.projectId().orElseThrow(() -> new ApplicationException(ErrorCode.GENERAL, null)));

    Comparator<String> natOrder = Comparator.naturalOrder();

    var result = proteomicsMeasurements.stream()
        .map(measurementPresenter::expandProteomicsPools)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(ProteomicsMeasurementEntry::measurementCode, natOrder)
            .thenComparing(ptx -> ptx.sampleInformation().sampleId(), natOrder)).toList();
    downloadComponent.trigger(new WorkbookDownloadStreamProvider() {
      @Override
      public String getFilename() {
        return FileNameFormatter.formatWithTimestampedContext(LocalDate.now(), projectCode,
            experimentName,
            "proteomics measurements", "xlsx");
      }

      @Override
      public Workbook getWorkbook() {
        return ProteomicsWorkbooks.createEditWorkbook(result);
      }
    });
  }

  private void downloadNGSMetadata() {
    ProjectId projectId = context.projectId().orElseThrow();
    String projectCode = projectInformationService.find(projectId)
        .orElseThrow()
        .getProjectCode().value();

    var experimentId = context.experimentId().orElseThrow();
    String experimentName = experimentInformationService.find(projectId.value(), experimentId)
        .orElseThrow()
        .getName();
    var ngsMeasurements = measurementService.findNGSMeasurements(experimentId, projectId);

    Comparator<String> natOrder = Comparator.naturalOrder();

    var result = ngsMeasurements.stream().map(measurementPresenter::expandNGSPools)
        .flatMap(Collection::stream)
        // sort by measurement codes first, then by sample codes
        .sorted(Comparator.comparing(NGSMeasurementEntry::measurementCode, natOrder)
            .thenComparing(ngs -> ngs.sampleInformation().sampleId(), natOrder)).toList();
    downloadComponent.trigger(new WorkbookDownloadStreamProvider() {
      @Override
      public String getFilename() {
        return FileNameFormatter.formatWithTimestampedContext(LocalDate.now(), projectCode,
            experimentName,
            "ngs measurements", "xlsx");
      }

      @Override
      public Workbook getWorkbook() {
        return NGSWorkbooks.createEditWorkbook(result);
      }
    });

  }

  private Disclaimer createNoSamplesRegisteredDisclaimer() {
    Disclaimer noSamplesRegisteredDisclaimer = Disclaimer.createWithTitle(
        "Register your samples first",
        "You have to register samples before measurement registration is possible",
        "Register Samples");
    noSamplesRegisteredDisclaimer.addDisclaimerConfirmedListener(
        this::routeToSampleCreation);
    noSamplesRegisteredDisclaimer.addClassName("no-samples-registered-disclaimer");
    return noSamplesRegisteredDisclaimer;
  }

  private Div createNoMeasurementDisclaimer() {
    Div noMeasurementDisclaimer = new Div();
    Span disclaimerTitle = new Span("Manage your measurement metadata");
    disclaimerTitle.addClassName("no-measurement-registered-title");
    noMeasurementDisclaimer.add(disclaimerTitle);
    Div noMeasurementDisclaimerContent = new Div();
    noMeasurementDisclaimerContent.addClassName("no-measurement-registered-content");
    Span noMeasurementText1 = new Span("Start by downloading the required metadata template");
    Span noMeasurementText2 = new Span(
        "Fill the metadata sheet and register your measurement metadata.");
    noMeasurementDisclaimerContent.add(noMeasurementText1);
    noMeasurementDisclaimerContent.add(noMeasurementText2);
    noMeasurementDisclaimer.add(noMeasurementDisclaimerContent);
    InfoBox availableTemplatesInfo = new InfoBox();
    availableTemplatesInfo.setInfoText(
        "You can download the measurement metadata template from the Templates component above");
    availableTemplatesInfo.setClosable(false);
    noMeasurementDisclaimer.add(availableTemplatesInfo);
    Button registerMeasurements = new Button("Register Measurements");
    registerMeasurements.addClassName("primary");
    noMeasurementDisclaimer.add(registerMeasurements);
    registerMeasurements.addClickListener(event -> openRegisterMeasurementDialog());
    noMeasurementDisclaimer.addClassName("no-measurements-registered-disclaimer");
    return noMeasurementDisclaimer;
  }

  private void routeToSampleCreation(ComponentEvent<?> componentEvent) {
    if (componentEvent.isFromClient()) {
      String currentExperimentId = context.experimentId().orElseThrow().value();
      String currentProjectId = context.projectId().orElseThrow().value();
      String routeToMeasurementPage = String.format(ProjectRoutes.SAMPLES,
          currentProjectId,
          currentExperimentId);
      log.debug(String.format(
          "Rerouting to sample page for experiment %s of project %s: %s",
          currentExperimentId, currentProjectId, routeToMeasurementPage));
      componentEvent.getSource().getUI().ifPresent(ui -> ui.navigate(routeToMeasurementPage));
    }
  }

  private void showErrorNotification(String title, String description) {
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
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
    setMeasurementInformation();
  }

  private void setMeasurementInformation() {
    ExperimentId currentExperimentId = context.experimentId().orElseThrow();
    if (!sampleInformationService.hasSamples(currentExperimentId)) {
      showRegisterSamplesDisclaimer();
      return;
    }
    if (!measurementService.hasMeasurements(currentExperimentId)) {
      showRegisterMeasurementDisclaimer();
    } else {
      showMeasurements();
    }
  }

  private void showRegisterSamplesDisclaimer() {
    noMeasurementDisclaimer.setVisible(false);
    content.setVisible(false);
    measurementDetailsComponent.setVisible(false);
    measurementTemplateListComponent.setVisible(false);
    registerSamplesDisclaimer.setVisible(true);
  }

  private void showRegisterMeasurementDisclaimer() {
    noMeasurementDisclaimer.setVisible(true);
    measurementTemplateListComponent.setVisible(true);
    content.setVisible(false);
    measurementDetailsComponent.setVisible(false);
    registerSamplesDisclaimer.setVisible(false);
  }

  private void showMeasurements() {
    noMeasurementDisclaimer.setVisible(false);
    registerSamplesDisclaimer.setVisible(false);
    content.setVisible(true);
    measurementTemplateListComponent.setVisible(true);
    measurementDetailsComponent.setContext(context);
    measurementDetailsComponent.setVisible(true);
  }

  private void onDownloadMeasurementTemplateClicked(
      DownloadMeasurementTemplateEvent downloadMeasurementTemplateEvent) {
    measurementTemplateDownload.trigger(
        downloadMeasurementTemplateEvent.getDownloadStreamProvider());
  }

  private void openRegisterMeasurementDialog() {
    this.dialog = new MeasurementMetadataUploadDialog(measurementValidationService,
        cancelConfirmationDialogFactory,
        MODE.ADD,
        context.projectId().orElse(null));
    setupDialog(dialog);
    dialog.open();
  }

  private void initRawDataAvailableInfo() {
    rawDataAvailableInfo.setInfoText(
        "Raw data results for your registered measurement are available now");
    Button navigateToDownloadRawDataButton = new Button("Go to Download Raw data");
    navigateToDownloadRawDataButton.addClickListener(this::routeToRawData);
    navigateToDownloadRawDataButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    rawDataAvailableInfo.add(navigateToDownloadRawDataButton);
    rawDataAvailableInfo.setClosable(true);
    content.add(rawDataAvailableInfo);
    rawDataAvailableInfo.setVisible(false);
  }

  private void routeToRawData(ComponentEvent<?> componentEvent) {
    if (componentEvent.isFromClient()) {
      String currentExperimentId = context.experimentId().orElseThrow().value();
      String currentProjectId = context.projectId().orElseThrow().value();
      String routeToRawDataPage = String.format(ProjectRoutes.RAWDATA,
          currentProjectId,
          currentExperimentId);
      log.debug(String.format(
          "Rerouting to raw data page for experiment %s of project %s: %s",
          currentExperimentId, currentProjectId, routeToRawDataPage));
      componentEvent.getSource().getUI().ifPresent(ui -> ui.navigate(routeToRawDataPage));
    }
  }

  private void setSelectedMeasurementsInfo(int selectedMeasurements) {
    String text = "%s measurements are currently selected.".formatted(
        String.valueOf(selectedMeasurements));
    if (selectedMeasurements > 0) {
      measurementsSelectedInfoBox.getStyle().setVisibility(Visibility.INITIAL);
    } else {
      measurementsSelectedInfoBox.getStyle().setVisibility(Visibility.HIDDEN);
    }
    measurementsSelectedInfoBox.setText(text);
  }

  static class HandledException extends RuntimeException {

    HandledException(Throwable cause) {
      super(cause);
    }

  }

  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    Optional.ofNullable(this.dialog).ifPresent(Dialog::close);
  }

}
