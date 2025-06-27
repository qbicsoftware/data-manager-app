package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style.Visibility;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.files.export.download.WorkbookDownloadStreamProvider;
import life.qbic.datamanager.files.export.measurement.NGSWorkbooks;
import life.qbic.datamanager.files.export.measurement.ProteomicsWorkbooks;
import life.qbic.datamanager.files.parsing.converters.ConverterRegistry;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateListComponent.DownloadMeasurementTemplateEvent;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateSelectionComponent.Domain;
import life.qbic.datamanager.views.projects.project.measurements.registration.MeasurementUpload;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationRequestBody;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.measurement.MeasurementService.MeasurementDeletionException;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;


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
public class MeasurementMain extends Main implements BeforeEnterObserver {

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
  private final AsyncProjectService asyncService;
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private transient Context context;
  private AppDialog measurementDialog;

  public MeasurementMain(
      @Autowired MeasurementTemplateListComponent measurementTemplateListComponent,
      @Autowired MeasurementDetailsComponent measurementDetailsComponent,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired MeasurementService measurementService,
      @Autowired MeasurementPresenter measurementPresenter,
      @Autowired MeasurementValidationService measurementValidationService,
      @Autowired AsyncProjectService asyncProjectService,
      ProjectInformationService projectInformationService,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      MessageSourceNotificationFactory messageFactory,
      ExperimentInformationService experimentInformationService,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    Objects.requireNonNull(measurementTemplateListComponent);
    Objects.requireNonNull(measurementDetailsComponent);
    Objects.requireNonNull(measurementService);
    Objects.requireNonNull(measurementValidationService);
    Objects.requireNonNull(asyncProjectService);
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.measurementDetailsComponent = measurementDetailsComponent;
    this.measurementTemplateListComponent = measurementTemplateListComponent;
    this.measurementService = measurementService;
    this.measurementPresenter = measurementPresenter;
    this.measurementValidationService = measurementValidationService;
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.projectInformationService = projectInformationService;
    this.cancelConfirmationDialogFactory = cancelConfirmationDialogFactory;
    this.asyncService = asyncProjectService;

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
    this.messageSourceNotificationFactory = messageSourceNotificationFactory;
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
        event -> openRegistrationDialog());

    Button editButton = new Button("Edit");

    Button deleteButton = new Button("Delete");
    deleteButton.addClickListener(event -> onDeleteMeasurementsClicked());

    Span buttonBar = new Span(downloadButton, editButton, deleteButton, registerMeasurementButton);
    buttonBar.addClassName("button-bar");
    // measurementSearchField disabled as the search functionality is turned off due to efficiency reasons
    Span buttonsAndSearch = new Span(/*measurementSearchField,*/ buttonBar);
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
    registerMeasurements.addClickListener(event -> openRegistrationDialog());
    noMeasurementDisclaimer.addClassName("no-measurements-registered-disclaimer");
    return noMeasurementDisclaimer;
  }

  private void openRegistrationDialog() {
    this.measurementDialog = AppDialog.medium();
    DialogHeader.with(measurementDialog, "Register your measurement metadata");
    DialogFooter.with(measurementDialog, "Cancel", "Register");

    var registrationUseCase = new MeasurementUpload(asyncService, context,
        ConverterRegistry.converterFor(
            MeasurementRegistrationInformationNGS.class), messageSourceNotificationFactory);
    var templateComponent = new MeasurementTemplateSelectionComponent(
        Map.ofEntries(
            Map.entry(Domain.Genomics, new WorkbookDownloadStreamProvider() {
              @Override
              public String getFilename() {
                return FileNameFormatter.formatWithVersion("ngs_measurement_registration_sheet", 1,
                    "xlsx");
              }

              @Override
              public Workbook getWorkbook() {
                return NGSWorkbooks.createRegistrationWorkbook();
              }
            }),
            Map.entry(Domain.Proteomics, new WorkbookDownloadStreamProvider() {
              @Override
              public String getFilename() {
                return FileNameFormatter.formatWithVersion("pxp_measurement_registration_sheet", 1,
                    "xlsx");
              }

              @Override
              public Workbook getWorkbook() {
                return ProteomicsWorkbooks.createRegistrationWorkbook();
              }
            })));

    var measurementRegistrationComponent = new MeasurementRegistrationComponent(templateComponent,
        registrationUseCase, Domain.Genomics);

    DialogBody.with(measurementDialog, measurementRegistrationComponent,
        measurementRegistrationComponent);
    measurementDialog.registerCancelAction(measurementDialog::close);
    measurementDialog.registerConfirmAction(() -> {
      var requestContent = registrationUseCase.getValidationRequestContent();
      submitRequest(context.projectId().orElseThrow().value(),
          createRegistrationRequestPackage(requestContent));
      measurementDialog.close();
    });

    add(measurementDialog);
    measurementDialog.open();
  }

  private void submitRequest(String projectId,
      RegistrationRequestPackage registrationRequestPackage) {
    submitRequestNGS(projectId, registrationRequestPackage.registrationInformationNGS());
    submitRequestPxP(projectId, registrationRequestPackage.registrationInformationPxP());
  }

  private void submitRequestPxP(String projectId,
      List<MeasurementRegistrationInformationPxP> requestList) {
    if (requestList.isEmpty()) {
      return;
    }
    var preparedRequests = mergeByPoolPxP(requestList);
    submitPreparedRequest(projectId, preparedRequests);
  }

  private List<MeasurementRegistrationInformationPxP> mergeByPoolPxP(
      List<MeasurementRegistrationInformationPxP> requests) {
    // 1. we want to aggregate measurement registration information that have the same sample pool name (we omit blank pool names)
    var measurementsBySamplePool = new HashMap<String, List<MeasurementRegistrationInformationPxP>>();
    var finalMeasurements = new ArrayList<MeasurementRegistrationInformationPxP>();
    for (var measurement : requests) {
      if (measurement.samplePoolGroup().isBlank()) {
        finalMeasurements.add(measurement);
      } else {
        measurementsBySamplePool.computeIfAbsent(measurement.samplePoolGroup(),
            k -> new ArrayList<>()).add(measurement);
      }
    }
    // 2. now we need to merge sample-specific metadata of the pooled measurements
    for (var entry : measurementsBySamplePool.entrySet()) {
      // every entry has the same pool name and by definition are only distinct in their specific metadata
      var specificMetadata = entry.getValue().stream()
          .flatMap(m -> m.specificMetadata().entrySet().stream())
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      var commonMetadata = entry.getValue().getFirst();
      var pooledMeasurement = new MeasurementRegistrationInformationPxP(
          commonMetadata.technicalReplicateName(),
          commonMetadata.organisationId(),
          commonMetadata.msDeviceCURIE(),
          commonMetadata.samplePoolGroup(),
          commonMetadata.facility(),
          commonMetadata.digestionEnzyme(),
          commonMetadata.digestionMethod(),
          commonMetadata.enrichmentMethod(),
          commonMetadata.injectionVolume(),
          commonMetadata.lcColumn(),
          commonMetadata.lcmsMethod(),
          commonMetadata.labelingType(),
          specificMetadata);
      finalMeasurements.add(pooledMeasurement);
    }

    return finalMeasurements;
  }

  private RegistrationRequestPackage createRegistrationRequestPackage(
      List<? extends ValidationRequestBody> validationRequestBodies) {
    var requestsNGS = new ArrayList<MeasurementRegistrationInformationNGS>();
    var requestsPxP = new ArrayList<MeasurementRegistrationInformationPxP>();

    for (var entry : validationRequestBodies) {
      switch (entry) {
        case MeasurementRegistrationInformationNGS info -> requestsNGS.add(info);
        case MeasurementRegistrationInformationPxP info -> requestsPxP.add(info);
        default -> throw new IllegalStateException(
            "Unexpected request body of type: " + entry.getClass().getName());
      }
    }

    return new RegistrationRequestPackage(requestsNGS, requestsPxP);
  }

  private void submitPreparedRequest(String projectId,
      List<? extends MeasurementRegistrationRequestBody> registrationRequests) {
    var requests = registrationRequests.stream()
        .map(measurement -> new MeasurementRegistrationRequest(projectId, measurement)).toList();

    var registrationToast = messageFactory.pendingTaskToast("measurement.registration.in-progress",
        new Object[]{}, getLocale());
    registrationToast.open();
    var successfulCompletions = new AtomicInteger(0);
    asyncService.create(Flux.fromIterable(requests))
        .doFirst(() -> log.debug(
            "Starting registration of %d measurement requests.".formatted(requests.size())))
        .doOnEach(signal -> {
          if (signal.isOnNext()) {
            successfulCompletions.incrementAndGet();
          }
        })
        .doOnTerminate(() -> {
          closeToast(registrationToast);
          processRegistrationResults(successfulCompletions.get(), requests.size());
        })
        .subscribe();
  }

  private void submitRequestNGS(String projectId,
      List<MeasurementRegistrationInformationNGS> requestList) {
    if (requestList.isEmpty()) {
      return;
    }
    var preparedRequests = mergeByPoolNGS(requestList);
    submitPreparedRequest(projectId, preparedRequests);
  }

  private static List<MeasurementRegistrationInformationNGS> mergeByPoolNGS(
      List<MeasurementRegistrationInformationNGS> requests) {
    // 1. we want to aggregate measurement registration information that have the same sample pool name (we omit blank pool names)
    var measurementsBySamplePool = new HashMap<String, List<MeasurementRegistrationInformationNGS>>();
    var finalMeasurements = new ArrayList<MeasurementRegistrationInformationNGS>();
    for (var measurement : requests) {
      if (measurement.samplePoolGroup().isBlank()) {
        finalMeasurements.add(measurement);
      } else {
        measurementsBySamplePool.computeIfAbsent(measurement.samplePoolGroup(),
            k -> new ArrayList<>()).add(measurement);
      }
    }
    // 2. now we need to merge sample-specific metadata of the pooled measurements
    for (var entry : measurementsBySamplePool.entrySet()) {
      // every entry has the same pool name and by definition are only distinct in their specific metadata
      var specificMetadata = entry.getValue().stream()
          .flatMap(m -> m.specificMetadata().entrySet().stream())
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      var commonMetadata = entry.getValue().getFirst();
      var pooledMeasurement = new MeasurementRegistrationInformationNGS(
          commonMetadata.organisationId(),
          commonMetadata.instrumentCURIE(),
          commonMetadata.facility(),
          commonMetadata.sequencingReadType(),
          commonMetadata.libraryKit(),
          commonMetadata.flowCell(),
          commonMetadata.sequencingRunProtocol(),
          commonMetadata.samplePoolGroup(),
          specificMetadata);
      finalMeasurements.add(pooledMeasurement);
    }

    return finalMeasurements;
  }

  // To be called after all submitted requests completed by the service
  private void processRegistrationResults(int numberOfSuccesses, int numberOfRequests) {
    if (numberOfSuccesses > 0 && numberOfSuccesses == numberOfRequests) {
      // Only successful registrations
      displayRegistrationSuccess(numberOfSuccesses);
      return;
    }
    if (numberOfSuccesses > 0 && numberOfSuccesses < numberOfRequests) {
      // We have successful registrations but also failures
      displayRegistrationFailure(numberOfRequests - numberOfSuccesses);
      displayRegistrationSuccess(numberOfSuccesses);
      return;
    }
    // There were only failing requests, none succeeded
    displayRegistrationFailure(numberOfRequests - numberOfSuccesses);
  }

  private void displayRegistrationSuccess(int numberOfSuccesses) {
    getUI().ifPresent(ui -> ui.access(() -> {
      Toast toast = messageFactory.toast("measurement.registration.successful",
          new Object[]{numberOfSuccesses},
          getLocale());
      toast.open();
    }));
  }

  private void closeToast(Toast toast) {
    getUI().ifPresent(ui -> ui.access(() -> toast.close()));
  }

  private void displayRegistrationFailure(int numberOfFailures) {
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = messageFactory.toast("measurement.registration.failed",
          new Object[]{numberOfFailures},
          getLocale());
      toast.open();
    }));
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

  record RegistrationRequestPackage(
      List<MeasurementRegistrationInformationNGS> registrationInformationNGS,
      List<MeasurementRegistrationInformationPxP> registrationInformationPxP) {

    public RegistrationRequestPackage {
      List.copyOf(Objects.requireNonNull(registrationInformationNGS));
      List.copyOf(Objects.requireNonNull(registrationInformationPxP));
    }

  }

}
