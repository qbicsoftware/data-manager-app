package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;
import java.io.InputStream;
import java.io.Serial;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.files.export.download.WorkbookDownloadStreamProvider;
import life.qbic.datamanager.files.parsing.converters.ConverterRegistry;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.measurements.processor.ProcessorRegistry;
import life.qbic.datamanager.views.projects.project.measurements.registration.MeasurementUpload;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationRequestBody;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateRequestBody;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.measurement.MeasurementService.MeasurementDeletionException;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.NGSWorkbooks;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.ProteomicsWorkbooks;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;


/**
 * Measurement Main Component
 * <p>
 * This component hosts the components necessary to show and update the Measurement information
 * associated with an {@link Experiment} within a {@link Project} via the provided
 * {@link ExperimentId} and {@link ProjectId} in the URL
 */
@Route(value = "projects/:projectId?/experiments/:experimentId?/measurements", layout = ExperimentMainLayout.class)
@PermitAll
public class MeasurementMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(MeasurementMain.class);

  public static final String UPDATE_MEASUREMENT_DESCRIPTION = "Please download your measurement metadata in order to edit it. You can modify the properties in the sheet and upload it below to save the changes.";
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private final MeasurementDetailsComponentV2 measurementDetailsComponentV2;

  private final Disclaimer registerSamplesDisclaimer;
  private final DownloadComponent measurementTemplateDownload;
  private final transient SampleInformationService sampleInformationService;
  private final transient MeasurementService measurementService;
  private final Div content = new Div();
  private final InfoBox rawDataAvailableInfo = new InfoBox();
  private final Div noMeasurementDisclaimer;
  private final DownloadComponent downloadComponent;
  private final transient MessageSourceNotificationFactory messageFactory;
  private final AsyncProjectService asyncService;
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private transient Context context;
  private AppDialog measurementDialog;
  private final ProjectContext projectContext;


  private final UiHandle uiHandle = new UiHandle();

  static class ProjectContext {

    private String projectId;
    private static final String DEFAULT_VALUE = "unknown_project";

    synchronized String projectId() {
      if (projectId == null) {
        return DEFAULT_VALUE;
      }
      return projectId;
    }

    synchronized void setProjectId(String projectId) {
      this.projectId = projectId;
    }

  }

  private static final MimeType OPEN_XML = MimeType.valueOf(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  public MeasurementMain(
      @Autowired SampleInformationService sampleInformationService,
      @Autowired MeasurementService measurementService,
      @Autowired MeasurementValidationService measurementValidationService,
      @Autowired AsyncProjectService asyncProjectService,
      MessageSourceNotificationFactory messageFactory,
      MessageSourceNotificationFactory messageSourceNotificationFactory,
      NgsMeasurementLookup ngsMeasurementLookup,
      PxpMeasurementLookup pxpMeasurementLookup) {
    Objects.requireNonNull(measurementService);
    Objects.requireNonNull(measurementValidationService);
    Objects.requireNonNull(asyncProjectService);
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.measurementService = measurementService;
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.asyncService = asyncProjectService;
    this.projectContext = new ProjectContext();
    this.downloadComponent = new DownloadComponent();
    this.measurementTemplateDownload = new DownloadComponent();
    this.registerSamplesDisclaimer = createNoSamplesRegisteredDisclaimer();
    this.noMeasurementDisclaimer = createNoMeasurementDisclaimer();
    initContent();

    addClassName("measurement");
    this.messageSourceNotificationFactory = messageSourceNotificationFactory;

    addAttachListener(event -> uiHandle.bind(event.getUI()));
    addDetachListener(ignored -> uiHandle.unbind());

    add(registerSamplesDisclaimer, noMeasurementDisclaimer, measurementTemplateDownload,
        downloadComponent);
    measurementDetailsComponentV2 = new MeasurementDetailsComponentV2(
        messageFactory,
        ngsMeasurementLookup,
        pxpMeasurementLookup);

    measurementDetailsComponentV2.addNgsRegisterListener(
        registrationRequest -> openRegistrationDialog());
    measurementDetailsComponentV2.addNgsEditListener(
        editRequest -> ngsEditDialog(editRequest.measurementIds()).open());
    measurementDetailsComponentV2.addNgsExportListener(
        exportRequest -> downloadNGSMetadata(exportRequest.measurementIds()));
    measurementDetailsComponentV2.addNgsDeletionListener(
        deletionRequest -> handleNgsDeletionRequest(
            new HashSet<>(deletionRequest.measurementIds())));

    measurementDetailsComponentV2.addPxpRegisterListener(
        registrationRequest -> openRegistrationDialog());
    measurementDetailsComponentV2.addPxpEditListener(
        editRequest -> pxpEditDialog(editRequest.measurementIds()).open());
    measurementDetailsComponentV2.addPxpExportListener(
        exportRequest -> downloadProteomicsMetadata(exportRequest.measurementIds()));
    measurementDetailsComponentV2.addPxpDeletionListener(
        deletionRequest -> handlePxpDeletionRequest(
            new HashSet<>(deletionRequest.measurementIds())));


    add(registerSamplesDisclaimer, measurementTemplateDownload, measurementDetailsComponentV2);
    log.debug(
        "Created project measurement main for " + VaadinSession.getCurrent().getSession().getId());
  }

  private void initContent() {
    Span titleField = new Span();
    titleField.setText("Register Measurements");
    titleField.addClassNames("title");
    content.add(titleField);
    initRawDataAvailableInfo();
    add(content);
    content.addClassName("measurement-main-content");
  }


  private AppDialog ngsEditDialog(List<String> selectedMeasurementIds) {
    var dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Edit Measurements");
    DialogFooter.with(dialog, "Cancel", "Update");
    var templateDownload = new MeasurementTemplateComponent(
        UPDATE_MEASUREMENT_DESCRIPTION,
        "Download Metadata",
        asyncService.measurementUpdateNGS(context.projectId().orElseThrow().value(),
            selectedMeasurementIds, OPEN_XML),
        messageFactory,
        projectContext::projectId);

    var upload = new MeasurementUpload(asyncService, context,
        ConverterRegistry.converterFor(
            MeasurementUpdateInformationNGS.class), messageFactory);
    var uploadComponent = new MeasurementUpdateComponent(templateDownload, upload);
    DialogBody.with(dialog, uploadComponent, uploadComponent);
    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      if (upload.validate().hasPassed()) {
        var validationRequests = upload.getValidationRequestContent();
        submitUpdateRequest(context.projectId().orElseThrow().value(),
            createUpdateRequestPackage(validationRequests));
        dialog.close();
      }
    });
    return dialog;
  }

  private AppDialog pxpEditDialog(List<String> selectedMeasurementIds) {
    var dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Edit Measurements");
    DialogFooter.with(dialog, "Cancel", "Update");
    var templateDownload = new MeasurementTemplateComponent(
        UPDATE_MEASUREMENT_DESCRIPTION,
        "Download Metadata",
        asyncService.measurementUpdatePxP(context.projectId().orElseThrow().value(),
            selectedMeasurementIds, OPEN_XML),
        messageFactory,
        projectContext::projectId);

    var upload = new MeasurementUpload(asyncService, context,
        ConverterRegistry.converterFor(
            MeasurementUpdateInformationNGS.class), messageFactory);
    var uploadComponent = new MeasurementUpdateComponent(templateDownload, upload);
    DialogBody.with(dialog, uploadComponent, uploadComponent);
    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      if (upload.validate().hasPassed()) {
        var validationRequests = upload.getValidationRequestContent();
        submitUpdateRequest(context.projectId().orElseThrow().value(),
            createUpdateRequestPackage(validationRequests));
        dialog.close();
      }
    });
    return dialog;
  }

  private void handlePxpDeletionRequest(Set<String> measurementIds) {
    if (measurementIds.isEmpty()) {
      return;
    }
    MeasurementDeletionConfirmationNotification notification =
        new MeasurementDeletionConfirmationNotification(
            "Selected proteomics measurements will be deleted", measurementIds.size());
    notification.open();
    notification.addConfirmListener(event -> {
      deletePxpMeasurements(measurementIds);
      notification.close();
    });
    notification.addCancelListener(event -> notification.close());
  }

  private void handleNgsDeletionRequest(Set<String> measurementIds) {
    if (measurementIds.isEmpty()) {
      return;
    }
    MeasurementDeletionConfirmationNotification notification =
        new MeasurementDeletionConfirmationNotification(
            "Selected genomics measurements will be deleted", measurementIds.size());
    notification.open();
    notification.addConfirmListener(event -> {
      deleteNgsMeasurements(measurementIds);
      notification.close();
    });
    notification.addCancelListener(event -> notification.close());
  }

  private void deleteNgsMeasurements(Set<String> measurementIds) {
    var result = measurementService.deleteNgsMeasurements(context.projectId().orElseThrow(),
        measurementIds);
    result.onError(this::handleDeletionError);
    result.onValue(ignored -> handleDeletionSuccessNgs());
  }

  private void deletePxpMeasurements(Set<String> measurementIds) {
    var result = measurementService.deletePxpMeasurements(context.projectId().orElseThrow(),
        measurementIds);
    result.onError(this::handleDeletionError);
    result.onValue(ignored -> handleDeletionSuccessPxp());
  }

  private void handleDeletionError(MeasurementDeletionException error) {
    String errorMessage = switch (error.reason()) {
      case FAILED -> "Deletion failed. Please try again.";
      case DATA_ATTACHED -> "Data is attached to one or more measurements.";
    };
    showErrorNotification("Deletion failed", errorMessage);
  }

  private void handleDeletionSuccessNgs() {
    updateComponentVisibility();
    measurementDetailsComponentV2.refreshNgs();
  }

  private void handleDeletionSuccessPxp() {
    updateComponentVisibility();
    measurementDetailsComponentV2.refreshPxp();
  }


  private void downloadProteomicsMetadata(List<String> selectedMeasurementIds) {
    ProjectId projectId = context.projectId().orElseThrow();
    var inProgressToast = messageFactory.pendingTaskToast("measurement.preparing-download",
        MessageSourceNotificationFactory.EMPTY_PARAMETERS, getLocale());

    inProgressToast.open();

    asyncService.measurementUpdatePxP(projectId.value(), selectedMeasurementIds, OPEN_XML)
        .subscribe(result -> {
          uiHandle.onUiAndPush(inProgressToast::close);
          uiHandle.onUi(() -> triggerDownload(result));
        }, error -> {
          uiHandle.onUi(inProgressToast::close);
          log.error(error.getMessage(), error);
        }, () -> uiHandle.onUi(inProgressToast::close));
  }

  private void downloadNGSMetadata(List<String> selectedMeasurementIds) {
    ProjectId projectId = context.projectId().orElseThrow();
    var inProgressToast = messageFactory.pendingTaskToast("measurement.preparing-download",
        MessageSourceNotificationFactory.EMPTY_PARAMETERS, getLocale());

    inProgressToast.open();

    asyncService.measurementUpdateNGS(projectId.value(), selectedMeasurementIds, OPEN_XML)
        .subscribe(result -> {
          uiHandle.onUiAndPush(inProgressToast::close);
          uiHandle.onUi(() -> triggerDownload(result));
        }, error -> {
          uiHandle.onUi(inProgressToast::close);
          log.error(error.getMessage(), error);
        }, () -> uiHandle.onUi(inProgressToast::close));
  }

  private void triggerDownload(DigitalObject digitalObject) {
    DownloadStreamProvider downloadStreamProvider = new DownloadStreamProvider() {
      @Override
      public String getFilename() {
        var projectId = projectContext.projectId();
        return FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(), projectId,
            "measurements", "xlsx");
      }

      @Override
      public InputStream getStream() {
        return digitalObject.content();
      }
    };
    downloadComponent.trigger(downloadStreamProvider);
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
    DialogHeader.with(measurementDialog, "Register measurements");
    DialogFooter.with(measurementDialog, "Cancel", "Register");

    var registrationMeasurementUpload = new MeasurementUpload(asyncService, context,
        ConverterRegistry.converterFor(MeasurementRegistrationInformationNGS.class),
        messageSourceNotificationFactory);
    var templateComponent = new MeasurementTemplateSelectionComponent(
        Map.ofEntries(
            Map.entry(MeasurementTemplateSelectionComponent.Domain.Genomics,
                new WorkbookDownloadStreamProvider() {
                  @Override
                  public String getFilename() {
                    return FileNameFormatter.formatWithVersion("ngs_measurement_registration_sheet",
                        1,
                        "xlsx");
                  }

                  @Override
                  public Workbook getWorkbook() {
                    return NGSWorkbooks.createRegistrationWorkbook();
                  }
                }),
            Map.entry(MeasurementTemplateSelectionComponent.Domain.Proteomics,
                new WorkbookDownloadStreamProvider() {
                  @Override
                  public String getFilename() {
                    return FileNameFormatter.formatWithVersion("pxp_measurement_registration_sheet",
                        1,
                        "xlsx");
                  }

                  @Override
                  public Workbook getWorkbook() {
                    return ProteomicsWorkbooks.createRegistrationWorkbook();
                  }
                })));

    var measurementRegistrationComponent = new MeasurementRegistrationComponent(templateComponent,
        registrationMeasurementUpload, MeasurementTemplateSelectionComponent.Domain.Genomics);

    DialogBody.with(measurementDialog, measurementRegistrationComponent,
        measurementRegistrationComponent);
    measurementDialog.registerCancelAction(measurementDialog::close);
    measurementDialog.registerConfirmAction(() -> {
      if (registrationMeasurementUpload.validate().hasPassed()) {
        var requestContent = registrationMeasurementUpload.getValidationRequestContent();
        submitRequest(context.projectId().orElseThrow().value(),
            createRegistrationRequestPackage(requestContent));
        measurementDialog.close();
      }
    });

    add(measurementDialog);
    measurementDialog.open();
  }

  private void submitUpdateRequest(String projectId, UpdateRequestPackage updateRequestPackage) {
    submitUpdateRequestNGS(projectId, updateRequestPackage.updateInformationNGS);
    submitUpdateRequestPxP(projectId, updateRequestPackage.updateInformationPxP);
  }

  private void submitUpdateRequestPxP(String projectId,
      List<MeasurementUpdateInformationPxP> updateInformationPxP) {
    if (updateInformationPxP.isEmpty()) {
      return;
    }
    var preparedRequests = mergeByPoolUpdatePxP(updateInformationPxP);
    submitPreparedUpdateRequest(projectId, preparedRequests);
  }

  private List<MeasurementUpdateInformationPxP> mergeByPoolUpdatePxP(
      List<MeasurementUpdateInformationPxP> updateInformationPxP) {
    var processor = ProcessorRegistry.processorFor(MeasurementUpdateInformationPxP.class);
    if (processor == null) {
      throw new IllegalStateException("No processor for MeasurementUpdateInformationPxP");
    }
    return processor.process(updateInformationPxP);
  }

  private List<MeasurementUpdateInformationNGS> mergeByPoolUpdateNGS(
      List<MeasurementUpdateInformationNGS> updateInformationPxP) {
    var processor = ProcessorRegistry.processorFor(MeasurementUpdateInformationNGS.class);
    if (processor == null) {
      throw new IllegalStateException("No processor for MeasurementUpdateInformationNGS");
    }
    return processor.process(updateInformationPxP);
  }

  private void submitUpdateRequestNGS(String projectId,
      List<MeasurementUpdateInformationNGS> updateInformationNGS) {
    if (updateInformationNGS.isEmpty()) {
      return;
    }
    var preparedRequests = mergeByPoolUpdateNGS(updateInformationNGS);
    submitPreparedUpdateRequest(projectId, preparedRequests);
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
    var processor = ProcessorRegistry.processorFor(MeasurementRegistrationInformationPxP.class);
    return processor.process(requests);
  }


  private UpdateRequestPackage createUpdateRequestPackage(
      List<? extends ValidationRequestBody> validationRequests) {
    var requestsNGS = new ArrayList<MeasurementUpdateInformationNGS>();
    var requestsPxP = new ArrayList<MeasurementUpdateInformationPxP>();

    for (var entry : validationRequests) {
      switch (entry) {
        case MeasurementUpdateInformationNGS info -> requestsNGS.add(info);
        case MeasurementUpdateInformationPxP info -> requestsPxP.add(info);
        default -> throw new IllegalStateException(
            "Unexpected request body of type: " + entry.getClass().getName());
      }
    }
    return new UpdateRequestPackage(requestsNGS, requestsPxP);
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
          uiHandle.onUiAndPush(() -> {
            registrationToast.close();
            processResults(successfulCompletions.get(), requests.size(),
                this::displayRegistrationSuccess, this::displayRegistrationFailure);
          });
          uiHandle.onUi(this::reloadMeasurements);
        })
        .subscribe();
  }

  private void submitPreparedUpdateRequest(String projectId,
      List<? extends MeasurementUpdateRequestBody> preparedRequests) {
    var requests = preparedRequests.stream()
        .map(measurement -> new MeasurementUpdateRequest(projectId, measurement)).toList();

    var registrationToast = messageFactory.pendingTaskToast("measurement.registration.in-progress",
        new Object[]{}, getLocale());
    registrationToast.open();
    var successfulCompletions = new AtomicInteger(0);
    asyncService.update(Flux.fromIterable(requests))
        .doFirst(() -> log.debug(
            "Starting updates of %d measurements.".formatted(requests.size())))
        .doOnEach(signal -> {
          if (signal.isOnNext()) {
            successfulCompletions.incrementAndGet();
          }
        })
        .doOnTerminate(() -> {
          uiHandle.onUiAndPush(() -> {
            registrationToast.close();
            processResults(successfulCompletions.get(), requests.size(),
                this::displayUpdateSuccess, this::displayUpdateFailure);
          });
          uiHandle.onUi(this::reloadMeasurements);
        })
        .subscribe();
  }

  private void reloadMeasurements() {
    measurementDetailsComponentV2.setContext(context);
    updateComponentVisibility();
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
    var processor = ProcessorRegistry.processorFor(MeasurementRegistrationInformationNGS.class);
    return processor.process(requests);
  }

  private void processResults(int numberOfSuccesses, int numberOfRequests,
      Consumer<Integer> onSuccess, Consumer<Integer> onFailure) {
    if (numberOfSuccesses > 0 && numberOfSuccesses == numberOfRequests) {
      // Only successful registrations
      onSuccess.accept(numberOfSuccesses);
      return;
    }
    if (numberOfSuccesses > 0 && numberOfSuccesses < numberOfRequests) {
      // We have successful registrations but also failures
      onFailure.accept(numberOfRequests - numberOfSuccesses);
      onSuccess.accept(numberOfSuccesses);
      return;
    }
    // There were only failing requests, none succeeded
    onFailure.accept(numberOfRequests - numberOfSuccesses);
  }

  private void displayRegistrationSuccess(int numberOfSuccesses) {
    Toast toast = messageFactory.toast("measurement.registration.successful",
        new Object[]{numberOfSuccesses},
        getLocale());
    toast.open();
  }

  private void displayUpdateSuccess(int numberOfSuccesses) {
    Toast toast = messageFactory.toast("measurement.update.successful",
        new Object[]{numberOfSuccesses},
        getLocale());
    toast.open();
  }

  private void displayUpdateFailure(int numberOfFailures) {
    var toast = messageFactory.toast("measurement.update.failed",
        new Object[]{numberOfFailures},
        getLocale());
    toast.open();
  }

  private void displayRegistrationFailure(int numberOfFailures) {
    var toast = messageFactory.toast("measurement.registration.failed",
        new Object[]{numberOfFailures},
        getLocale());
    toast.open();
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
    reloadMeasurements();
    asyncService.getProjectCode(context.projectId().orElseThrow().value())
        .doOnSuccess(projectCode -> projectContext.setProjectId(projectCode.value()))
        .subscribe();
  }

  private void updateComponentVisibility() {
    ExperimentId currentExperimentId = context.experimentId().orElseThrow();
    if (!sampleInformationService.hasSamples(context.projectId().orElseThrow(),
        currentExperimentId.value())) {
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
    measurementDetailsComponentV2.setVisible(false);
    registerSamplesDisclaimer.setVisible(true);
  }

  private void showRegisterMeasurementDisclaimer() {
    noMeasurementDisclaimer.setVisible(true);
    content.setVisible(false);
    measurementDetailsComponentV2.setVisible(false);
    registerSamplesDisclaimer.setVisible(false);
  }

  private void showMeasurements() {
    noMeasurementDisclaimer.setVisible(false);
    registerSamplesDisclaimer.setVisible(false);
    content.setVisible(true);
    measurementDetailsComponentV2.setContext(context);
    measurementDetailsComponentV2.setVisible(true);
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

  record RegistrationRequestPackage(
      List<MeasurementRegistrationInformationNGS> registrationInformationNGS,
      List<MeasurementRegistrationInformationPxP> registrationInformationPxP) {

    public RegistrationRequestPackage {
      List.copyOf(Objects.requireNonNull(registrationInformationNGS));
      List.copyOf(Objects.requireNonNull(registrationInformationPxP));
    }

  }

  record UpdateRequestPackage(List<MeasurementUpdateInformationNGS> updateInformationNGS,
                              List<MeasurementUpdateInformationPxP> updateInformationPxP) {

    public UpdateRequestPackage {
      List.copyOf(Objects.requireNonNull(updateInformationNGS));
      List.copyOf(Objects.requireNonNull(updateInformationPxP));
    }

  }

}
