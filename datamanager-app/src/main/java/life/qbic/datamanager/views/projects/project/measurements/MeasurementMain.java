package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.files.export.download.WorkbookDownloadStreamProvider;
import life.qbic.datamanager.files.parsing.converters.ConverterRegistry;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
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
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementDomain;
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementListState;
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementListStateCodec;
import life.qbic.datamanager.views.projects.project.measurements.processor.ProcessorRegistry;
import life.qbic.datamanager.views.projects.project.measurements.registration.MeasurementUpload;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationIP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationRequestBody;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationIP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateRequestBody;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.measurement.IpMeasurementLookup;
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
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.IPWorkbooks;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.NGSWorkbooks;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.ProteomicsWorkbooks;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Measurement Main Component
 * <p>
 * This component hosts the components necessary to show and update the Measurement information
 * associated with an {@link Experiment} within a {@link Project} via the provided
 * {@link ExperimentId} and {@link ProjectId} in the URL
 */
@Route(value = "projects/:projectId?/experiments/:experimentId?/measurements", layout = ExperimentMainLayout.class)
@PermitAll
public class MeasurementMain extends Main implements BeforeEnterObserver, BeforeLeaveObserver {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(MeasurementMain.class);

  public static final String UPDATE_MEASUREMENT_DESCRIPTION = "Export the measurement metadata you want to edit. You can modify the properties in the sheet and upload it below to save the changes.";
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private final MeasurementDetailsComponent measurementDetailsComponent;
  /**
   * The framework's own history state change handler, captured before this view installs its own
   * (same pattern as {@code ProjectOverviewMain}).
   */
  private History.HistoryStateChangeHandler routerHistoryStateChangeHandler;
  private final History.HistoryStateChangeHandler listStateHistoryHandler = this::onHistoryStateChange;


  private final Disclaimer registerSamplesDisclaimer;
  private final transient SampleInformationService sampleInformationService;
  private final transient MeasurementService measurementService;
  private final Div content = new Div();
  private final InfoBox rawDataAvailableInfo = new InfoBox();
  private final Div noMeasurementDisclaimer;
  private final DownloadComponent downloadComponent;
  private final transient MessageSourceNotificationFactory messageFactory;
  private final AsyncProjectService asyncService;
  private final transient NgsMeasurementLookup ngsMeasurementLookup;
  private final transient PxpMeasurementLookup pxpMeasurementLookup;
  private final transient IpMeasurementLookup ipMeasurementLookup;
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private transient Context context;
  private final ProjectContext projectContext;
  private final UserPermissions userPermissions;


  private final UiHandle uiHandle = new UiHandle();
  private UploadConfiguration uploadConfiguration;

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
      @Autowired UserPermissions userPermissions,
      MessageSourceNotificationFactory messageFactory,
      MessageSourceNotificationFactory messageSourceNotificationFactory,
      NgsMeasurementLookup ngsMeasurementLookup,
      PxpMeasurementLookup pxpMeasurementLookup,
      IpMeasurementLookup ipMeasurementLookup,
      UploadConfiguration uploadConfiguration) {
    Objects.requireNonNull(measurementService);
    Objects.requireNonNull(measurementValidationService);
    Objects.requireNonNull(asyncProjectService);
    this.userPermissions = Objects.requireNonNull(userPermissions);
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.measurementService = measurementService;
    this.ngsMeasurementLookup = Objects.requireNonNull(ngsMeasurementLookup);
    this.pxpMeasurementLookup = Objects.requireNonNull(pxpMeasurementLookup);
    this.ipMeasurementLookup = Objects.requireNonNull(ipMeasurementLookup);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.asyncService = asyncProjectService;
    this.projectContext = new ProjectContext();
    this.downloadComponent = new DownloadComponent();
    DownloadComponent measurementTemplateDownload = new DownloadComponent();
    this.registerSamplesDisclaimer = createNoSamplesRegisteredDisclaimer();
    this.noMeasurementDisclaimer = createNoMeasurementDisclaimer();
    this.uploadConfiguration = Objects.requireNonNull(uploadConfiguration);
    initContent();

    addClassName("measurement");
    this.messageSourceNotificationFactory = messageSourceNotificationFactory;

    addAttachListener(event -> uiHandle.bind(event.getUI()));
    addDetachListener(ignored -> uiHandle.unbind());

    add(registerSamplesDisclaimer, noMeasurementDisclaimer, measurementTemplateDownload,
        downloadComponent);
    measurementDetailsComponent = new MeasurementDetailsComponent(
        messageFactory,
        ngsMeasurementLookup,
        pxpMeasurementLookup,
        ipMeasurementLookup);

    measurementDetailsComponent.addNgsRegisterListener(
        registrationRequest -> openRegistrationDialog());
    measurementDetailsComponent.addNgsEditListener(
        editRequest -> editDialog(MeasurementDomain.NGS).open());
    measurementDetailsComponent.addNgsExportListener(
        exportRequest -> downloadNGSMetadata(exportRequest.measurementIds()));
    measurementDetailsComponent.addNgsDeletionListener(
        deletionRequest -> handleNgsDeletionRequest(
            new HashSet<>(deletionRequest.measurementIds())));

    measurementDetailsComponent.addPxpRegisterListener(
        registrationRequest -> openRegistrationDialog());
    measurementDetailsComponent.addPxpEditListener(
        editRequest -> editDialog(MeasurementDomain.PXP).open());
    measurementDetailsComponent.addPxpExportListener(
        exportRequest -> downloadProteomicsMetadata(exportRequest.measurementIds()));
    measurementDetailsComponent.addPxpDeletionListener(
        deletionRequest -> handlePxpDeletionRequest(
            new HashSet<>(deletionRequest.measurementIds())));

    measurementDetailsComponent.addIpRegisterListener(
        registrationRequest -> openRegistrationDialog());
    measurementDetailsComponent.addIpEditListener(
        editRequest -> editDialog(MeasurementDomain.IP).open());
    measurementDetailsComponent.addIpExportListener(
        exportRequest -> downloadIPMetadata(exportRequest.measurementIds()));
    measurementDetailsComponent.addIpDeletionListener(
        deletionRequest -> handleIpDeletionRequest(
            new HashSet<>(deletionRequest.measurementIds())));

    add(registerSamplesDisclaimer, measurementTemplateDownload, measurementDetailsComponent);
    log.debug(
        "Created project measurement main for " + VaadinSession.getCurrent().getSession().getId());
  }

  private void initContent() {
    Span titleField = new Span();
    // UX F11: page title aligned with the workflow step ("View Measurements") instead of
    // the misleading "Register Measurements" copy
    titleField.setText("View Measurements");
    titleField.addClassNames("title");
    // global primary action in the header, top right (as in the original design):
    // registration is a top-level capability, not per-tab
    Button registerButton = new Button("Register Measurements", VaadinIcon.PLUS.create());
    registerButton.addClassName("button-bar");
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    registerButton.addClickListener(clicked -> openRegistrationDialog());
    registerMeasurementButtons.add(registerButton);
    Div header = new Div(titleField, registerButton);
    header.addClassName("measurement-main-header");
    content.add(header);
    initRawDataAvailableInfo();
    add(content);
    content.addClassName("measurement-main-content");
  }


  /**
   * Opens the measurement edit dialog for the given domain. The dialog is scope-agnostic towards
   * the (session-only) row selection: updates are identified per row via the measurement ID in the
   * uploaded sheet, so the dialog always offers to download the edit template for <em>all</em>
   * measurements of the active domain in the current experiment. When a selection exists, it is
   * additionally offered as a convenience to download a smaller, targeted template for exactly the
   * selected measurements.
   */
  private AppDialog editDialog(MeasurementDomain domain) {
    List<String> selectedMeasurementIds = List.copyOf(
        measurementDetailsComponent.selectedMeasurementIds(domain));
    return switch (domain) {
      case NGS -> ngsEditDialog(selectedMeasurementIds);
      case PXP -> pxpEditDialog(selectedMeasurementIds);
      case IP -> ipEditDialog(selectedMeasurementIds);
    };
  }

  private AppDialog ngsEditDialog(List<String> selectedMeasurementIds) {
    var dialog = AppDialog.large();
    DialogHeader.with(dialog, "Edit Genomics Measurements");
    DialogFooter.with(dialog, "Cancel", "Edit Genomics Measurements");

    var upload = new MeasurementUpload(asyncService, context,
        ConverterRegistry.converterFor(
            MeasurementUpdateInformationNGS.class), messageFactory, uploadConfiguration);
    var uploadComponent = new MeasurementUpdateComponent(
        buildTemplateSection(MeasurementDomain.NGS, selectedMeasurementIds), upload);
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
    var dialog = AppDialog.large();
    DialogHeader.with(dialog, "Edit Proteomics Measurements");
    DialogFooter.with(dialog, "Cancel", "Edit Proteomics Measurements");

    var upload = new MeasurementUpload(asyncService, context,
        ConverterRegistry.converterFor(
            MeasurementUpdateInformationPxP.class), messageFactory, uploadConfiguration);
    var uploadComponent = new MeasurementUpdateComponent(
        buildTemplateSection(MeasurementDomain.PXP, selectedMeasurementIds), upload);
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

  private AppDialog ipEditDialog(List<String> selectedMeasurementIds) {
    var dialog = AppDialog.large();
    DialogHeader.with(dialog, "Edit Immunopeptidomics Measurements");
    DialogFooter.with(dialog, "Cancel", "Edit Immunopeptidomics Measurements");

    var upload = new MeasurementUpload(asyncService, context,
        ConverterRegistry.converterFor(
            MeasurementUpdateInformationIP.class), messageFactory,
        uploadConfiguration);
    var uploadComponent = new MeasurementUpdateComponent(
        buildTemplateSection(MeasurementDomain.IP, selectedMeasurementIds), upload);
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

  /**
   * Builds the "Export Metadata" section of an edit dialog for the given domain.
   * <p>
   * The section always offers to export the edit template for <em>all</em> measurements of the
   * domain in the current experiment (the baseline that rescues users whose session selection was
   * lost), and, when a selection exists, an additional convenience button to export only the
   * selected measurements. Both buttons state their scope explicitly in the label, so the user
   * knows exactly what they will export without hunting.
   */
  private MeasurementTemplateComponent buildTemplateSection(MeasurementDomain domain,
      List<String> selectedMeasurementIds) {
    String projectId = context.projectId().orElseThrow().value();
    String experimentId = context.experimentId().orElseThrow().value();
    String domainLabel = domainLabel(domain);
    // The "export all" template mono is lazy: measurement IDs are resolved only when the user
    // clicks the button, so opening the dialog stays instant and the workbook reflects
    // the current state of the experiment at export time.
    Mono<DigitalObject> allMeasurementsTemplate = Mono.defer(() -> {
      List<String> allIds = allMeasurementIds(domain, projectId, experimentId);
      return switch (domain) {
        case NGS -> asyncService.measurementUpdateNGS(projectId, allIds, OPEN_XML);
        case PXP -> asyncService.measurementUpdatePxP(projectId, allIds, OPEN_XML);
        case IP -> asyncService.measurementUpdateIP(projectId, allIds, OPEN_XML);
      };
    });
    MeasurementTemplateComponent template = new MeasurementTemplateComponent(
        UPDATE_MEASUREMENT_DESCRIPTION,
        "Export all " + domainLabel + " measurements",
        allMeasurementsTemplate,
        messageFactory,
        projectContext::projectId);
    if (!selectedMeasurementIds.isEmpty()) {
      // the selected set is already resolved (dialog-open time); just wrap it in a deferred
      // mono so the service call happens on click
      template.addTemplateExport(
          "Export selected (%d)".formatted(selectedMeasurementIds.size()),
          Mono.defer(() -> switch (domain) {
            case NGS -> asyncService.measurementUpdateNGS(projectId, selectedMeasurementIds,
                OPEN_XML);
            case PXP -> asyncService.measurementUpdatePxP(projectId, selectedMeasurementIds,
                OPEN_XML);
            case IP -> asyncService.measurementUpdateIP(projectId, selectedMeasurementIds,
                OPEN_XML);
          }));
    }
    return template;
  }

  private static String domainLabel(MeasurementDomain domain) {
    return switch (domain) {
      case NGS -> "genomics";
      case PXP -> "proteomics";
      case IP -> "immunopeptidomics";
    };
  }

  /**
   * Resolves all measurement IDs of the given domain within the current experiment. The blank
   * search term matches every measurement of the experiment (the search filter is an
   * <code>anyContaining</code> predicate that is skipped for blank terms).
   */
  private List<String> allMeasurementIds(MeasurementDomain domain, String projectId,
      String experimentId) {
    return switch (domain) {
      case NGS -> ngsMeasurementIds(projectId, experimentId);
      case PXP -> pxpMeasurementIds(projectId, experimentId);
      case IP -> ipMeasurementIds(projectId, experimentId);
    };
  }

  private List<String> ngsMeasurementIds(String projectId, String experimentId) {
    NgsMeasurementLookup.MeasurementFilter filter =
        NgsMeasurementLookup.MeasurementFilter.forExperiment(experimentId);
    int total = ngsMeasurementLookup.countNgsMeasurements(projectId, filter);
    return ngsMeasurementLookup.lookupNgsMeasurements(projectId, 0, total, sortForExport(), filter)
        .map(NgsMeasurementLookup.MeasurementInfo::measurementId).toList();
  }

  private List<String> pxpMeasurementIds(String projectId, String experimentId) {
    PxpMeasurementLookup.MeasurementFilter filter =
        PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId);
    int total = pxpMeasurementLookup.countPxpMeasurements(projectId, filter);
    return pxpMeasurementLookup.lookupPxpMeasurements(projectId, 0, total, sortForExport(), filter)
        .map(PxpMeasurementLookup.MeasurementInfo::measurementId).toList();
  }

  private List<String> ipMeasurementIds(String projectId, String experimentId) {
    IpMeasurementLookup.MeasurementFilter filter =
        IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId);
    int total = ipMeasurementLookup.countIpMeasurements(projectId, filter);
    return ipMeasurementLookup.lookupIpMeasurements(projectId, 0, total, sortForExport(), filter)
        .map(IpMeasurementLookup.MeasurementInfo::measurementId).toList();
  }

  /**
   * A stable sort order for resolving all measurement IDs of a domain. Sorting is only needed to
   * guarantee a deterministic workbook row order; an unsorted lookup would also return every
   * measurement of the experiment.
   */
  private static Sort sortForExport() {
    return Sort.unsorted();
  }

  private void handlePxpDeletionRequest(Set<String> measurementIds) {
    if (measurementIds.isEmpty()) {
      return;
    }
    AlertDialog.danger(this,
        "Selected proteomics measurements will be deleted",
        "Are you sure you want to delete %d measurement%s?".formatted(measurementIds.size(),
            measurementIds.size() > 1 ? "s" : ""),
        "Delete measurements",
        "Keep measurements",
        () -> deletePxpMeasurements(measurementIds)).open();
  }

  private void handleNgsDeletionRequest(Set<String> measurementIds) {
    if (measurementIds.isEmpty()) {
      return;
    }
    AlertDialog.danger(this,
        "Selected genomics measurements will be deleted",
        "Are you sure you want to delete %d measurement%s?".formatted(measurementIds.size(),
            measurementIds.size() > 1 ? "s" : ""),
        "Delete measurements",
        "Keep measurements",
        () -> deleteNgsMeasurements(measurementIds)).open();
  }

  private void handleIpDeletionRequest(Set<String> measurementIds) {
    if (measurementIds.isEmpty()) {
      return;
    }
    AlertDialog.danger(this,
        "Selected immunopeptidomics measurements will be deleted",
        "Are you sure you want to delete %d measurement%s?".formatted(measurementIds.size(),
            measurementIds.size() > 1 ? "s" : ""),
        "Delete measurements",
        "Keep measurements",
        () -> deleteIpMeasurements(measurementIds)).open();
  }

  private void deleteNgsMeasurements(Set<String> measurementIds) {
    var result = measurementService.deleteNgsMeasurements(context.projectId().orElseThrow(),
        measurementIds);
    result.onError(this::handleDeletionError);
    result.onValue(ignored -> handleDeletionSuccessNgs(measurementIds));
  }

  private void deletePxpMeasurements(Set<String> measurementIds) {
    var result = measurementService.deletePxpMeasurements(context.projectId().orElseThrow(),
        measurementIds);
    result.onError(this::handleDeletionError);
    result.onValue(ignored -> handleDeletionSuccessPxp(measurementIds));
  }

  private void deleteIpMeasurements(Set<String> measurementIds) {
    var result = measurementService.deleteIpMeasurements(context.projectId().orElseThrow(),
        measurementIds);
    result.onError(this::handleDeletionError);
    result.onValue(ignored -> handleDeletionSuccessIp(measurementIds));
  }

  private void handleDeletionError(MeasurementDeletionException error) {
    String errorMessage = switch (error.reason()) {
      case FAILED -> "Deletion failed. Please try again.";
      case DATA_ATTACHED -> "Data is attached to one or more measurements.";
    };
    showErrorNotification(errorMessage);
  }

  private void handleDeletionSuccessNgs(Set<String> measurementIds) {
    displayDeletionSuccess(measurementIds.size());
    measurementDetailsComponent.removeFromSelection(MeasurementDomain.NGS, measurementIds);
    updateComponentVisibility();
    measurementDetailsComponent.refreshNgs();
  }

  private void handleDeletionSuccessPxp(Set<String> measurementIds) {
    displayDeletionSuccess(measurementIds.size());
    measurementDetailsComponent.removeFromSelection(MeasurementDomain.PXP, measurementIds);
    updateComponentVisibility();
    measurementDetailsComponent.refreshPxp();
  }

  private void handleDeletionSuccessIp(Set<String> measurementIds) {
    displayDeletionSuccess(measurementIds.size());
    measurementDetailsComponent.removeFromSelection(MeasurementDomain.IP, measurementIds);
    updateComponentVisibility();
    measurementDetailsComponent.refreshIp();
  }

  private void displayDeletionSuccess(int numberOfDeleted) {
    Toast toast = messageFactory.toast("measurement.deletion.successful",
        new Object[]{numberOfDeleted},
        getLocale());
    toast.open();
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

  private void downloadIPMetadata(List<String> selectedMeasurementIds) {
    ProjectId projectId = context.projectId().orElseThrow();
    var inProgressToast = messageFactory.pendingTaskToast("measurement.preparing-download",
        MessageSourceNotificationFactory.EMPTY_PARAMETERS, getLocale());

    inProgressToast.open();

    asyncService.measurementUpdateIP(projectId.value(), selectedMeasurementIds, OPEN_XML)
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

      @Override
      public String getContentType() {
        return MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
      }

      @Override
      public Optional<Long> contentLength() {
        return Optional.empty();
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

  private final List<Button> registerMeasurementButtons = new ArrayList<>();

  private Div createNoMeasurementDisclaimer() {
    Div disclaimer = new Div();
    Span disclaimerTitle = new Span("Manage your measurement metadata");
    disclaimerTitle.addClassName("no-measurement-registered-title");
    disclaimer.add(disclaimerTitle);
    Div noMeasurementDisclaimerContent = new Div();
    noMeasurementDisclaimerContent.addClassName("no-measurement-registered-content");
    Span noMeasurementText1 = new Span("Start by downloading the required metadata template");
    Span noMeasurementText2 = new Span(
        "Fill the metadata sheet and register your measurement metadata.");
    noMeasurementDisclaimerContent.add(noMeasurementText1);
    noMeasurementDisclaimerContent.add(noMeasurementText2);
    disclaimer.add(noMeasurementDisclaimerContent);
    InfoBox availableTemplatesInfo = new InfoBox();
    availableTemplatesInfo.setInfoText(
        "You can download the measurement metadata template from the Templates component above");
    availableTemplatesInfo.setClosable(false);
    disclaimer.add(availableTemplatesInfo);
    Button registerMeasurements = new Button("Register Measurements");
    registerMeasurements.addClassName("primary");
    registerMeasurements.addClickListener(event -> openRegistrationDialog());
    registerMeasurementButtons.add(registerMeasurements);
    disclaimer.add(registerMeasurements);
    disclaimer.addClassName("no-measurements-registered-disclaimer");
    return disclaimer;
  }

  private void openRegistrationDialog() {
    // ACL gate: registration is a mutation; never open for read-only project scope
    boolean canWrite = context != null
        && context.projectId().map(userPermissions::editProject).orElse(false);
    if (!canWrite) {
      ErrorMessage errorMessage = new ErrorMessage("Missing permissions",
          "You need write access to this project to register measurements.");
      new StyledNotification(errorMessage).open();
      return;
    }
    AppDialog measurementDialog;
    measurementDialog = AppDialog.medium();
    DialogHeader.with(measurementDialog, "Register measurements");
    DialogFooter.with(measurementDialog, "Cancel", "Register");

    var registrationMeasurementUpload = new MeasurementUpload(asyncService, context,
        ConverterRegistry.converterFor(MeasurementRegistrationInformationNGS.class),
        messageSourceNotificationFactory, uploadConfiguration);
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
                  public Optional<Long> contentLength() {
                    return Optional.empty();
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
                  public Optional<Long> contentLength() {
                    return Optional.empty();
                  }

                  @Override
                  public Workbook getWorkbook() {
                    return ProteomicsWorkbooks.createRegistrationWorkbook();
                  }
                }),
            Map.entry(MeasurementTemplateSelectionComponent.Domain.Immunopeptidomics,
                new WorkbookDownloadStreamProvider() {
                  @Override
                  public String getFilename() {
                    return FileNameFormatter.formatWithVersion(
                        "immunopeptidomics_measurement_registration_sheet",
                        1, "xlsx");
                  }

                  @Override
                  public Optional<Long> contentLength() {
                    return Optional.empty();
                  }

                  @Override
                  public Workbook getWorkbook() {
                    return IPWorkbooks.createRegistrationWorkbook();
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
    submitUpdateRequestIP(projectId, updateRequestPackage.updateInformationIP);
  }

  private void submitUpdateRequestIP(String projectId,
      List<MeasurementUpdateInformationIP> updateInformationIP) {
    if (updateInformationIP.isEmpty()) {
      return;
    }
    var preparedRequests = mergeByPoolUpdateIP(updateInformationIP);
    submitPreparedUpdateRequest(projectId, preparedRequests);
  }

  private List<MeasurementUpdateInformationIP> mergeByPoolUpdateIP(
      List<MeasurementUpdateInformationIP> updateInformationIP) {
    var processor = ProcessorRegistry.processorFor(MeasurementUpdateInformationIP.class);
    if (processor == null) {
      throw new IllegalStateException("No processor for MeasurementUpdateInformationIP");
    }
    return processor.process(updateInformationIP);
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
    submitRequestIP(projectId, registrationRequestPackage.registrationInformationIP());
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

  private void submitRequestIP(String projectId,
      List<MeasurementRegistrationInformationIP> requestList) {
    if (requestList.isEmpty()) {
      return;
    }
    var preparedRequests = mergeByPoolIP(requestList);
    submitPreparedRequest(projectId, preparedRequests);
  }

  private static List<MeasurementRegistrationInformationIP> mergeByPoolIP(
      List<MeasurementRegistrationInformationIP> requests) {
    var processor = ProcessorRegistry.processorFor(MeasurementRegistrationInformationIP.class);
    return processor.process(requests);
  }


  private UpdateRequestPackage createUpdateRequestPackage(
      List<? extends ValidationRequestBody> validationRequests) {
    var requestsNGS = new ArrayList<MeasurementUpdateInformationNGS>();
    var requestsPxP = new ArrayList<MeasurementUpdateInformationPxP>();
    var requestsIP = new ArrayList<MeasurementUpdateInformationIP>();

    for (var entry : validationRequests) {
      switch (entry) {
        case MeasurementUpdateInformationNGS info -> requestsNGS.add(info);
        case MeasurementUpdateInformationPxP info -> requestsPxP.add(info);
        case MeasurementUpdateInformationIP info -> requestsIP.add(info);
        default -> throw new IllegalStateException(
            "Unexpected request body of type: " + entry.getClass().getName());
      }
    }
    return new UpdateRequestPackage(requestsNGS, requestsPxP, requestsIP);
  }

  private RegistrationRequestPackage createRegistrationRequestPackage(
      List<? extends ValidationRequestBody> validationRequestBodies) {
    var requestsNGS = new ArrayList<MeasurementRegistrationInformationNGS>();
    var requestsPxP = new ArrayList<MeasurementRegistrationInformationPxP>();
    var requestsIP = new ArrayList<MeasurementRegistrationInformationIP>();

    for (var entry : validationRequestBodies) {
      switch (entry) {
        case MeasurementRegistrationInformationNGS info -> requestsNGS.add(info);
        case MeasurementRegistrationInformationPxP info -> requestsPxP.add(info);
        case MeasurementRegistrationInformationIP info -> requestsIP.add(info);
        default -> throw new IllegalStateException(
            "Unexpected request body of type: " + entry.getClass().getName());
      }
    }

    return new RegistrationRequestPackage(requestsNGS, requestsPxP, requestsIP);
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
    measurementDetailsComponent.setContext(context);
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
      IntConsumer onSuccess, IntConsumer onFailure) {
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

  private void showErrorNotification(String description) {
    ErrorMessage errorMessage = new ErrorMessage("Deletion failed", description);
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

    // URL list-state synchronisation (USER-R-03, ADR-0008): capture the router handler, install
    // our own, and seed the container from the URL on direct load / reload / shared links.
    History history = UI.getCurrent().getPage().getHistory();
    History.HistoryStateChangeHandler currentHandler = history.getHistoryStateChangeHandler();
    if (currentHandler != listStateHistoryHandler) {
      routerHistoryStateChangeHandler = currentHandler;
    }
    history.setHistoryStateChangeHandler(listStateHistoryHandler);
    String basePath = String.format(ProjectRoutes.MEASUREMENTS, projectID, experimentId);
    measurementDetailsComponent.setBasePath(basePath);
    MeasurementListState urlState = MeasurementListStateCodec.parse(
        event.getLocation().getQueryParameters(),
        measurementDetailsComponent.getTabPagination().listState());

    // seed the URL state before the first render so the initial fetch already reflects the
    // tab/page/size/filter/sort of a reloaded or shared link, instead of briefly rendering the
    // in-session state and re-fetching (USER-R-03, ADR-0008).
    measurementDetailsComponent.getTabPagination().applyExternalState(urlState);
    reloadMeasurements();

    asyncService.getProjectCode(context.projectId().orElseThrow().value())
        .doOnSuccess(projectCode -> projectContext.setProjectId(projectCode.value()))
        .subscribe();
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
   * navigation). Non-measurement locations are delegated back to the router handler.
   */
  private void onHistoryStateChange(HistoryStateChangeEvent event) {
    String expectedPath = currentMeasurementsPath();
    if (expectedPath == null || !expectedPath.equals(event.getLocation().getPath())) {
      if (routerHistoryStateChangeHandler != null) {
        routerHistoryStateChangeHandler.onHistoryStateChange(event);
      }
      return;
    }
    MeasurementListState urlState = MeasurementListStateCodec.parse(
        event.getLocation().getQueryParameters(),
        measurementDetailsComponent.getTabPagination().listState());
    measurementDetailsComponent.getTabPagination().applyExternalState(urlState);
  }

  private String currentMeasurementsPath() {
    if (context == null || context.projectId().isEmpty() || context.experimentId().isEmpty()) {
      return null;
    }
    return String.format(ProjectRoutes.MEASUREMENTS,
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow().value());
  }

  private void updateComponentVisibility() {
    ExperimentId currentExperimentId = context.experimentId().orElseThrow();
    ProjectId projectId = context.projectId().orElseThrow();
    // ACL: hide (not just guard) the registration affordances for read-only project scope
    boolean canWrite = userPermissions.editProject(projectId);
    registerMeasurementButtons.forEach(button -> button.setVisible(canWrite));
    if (!sampleInformationService.hasSamples(projectId,
        currentExperimentId.value())) {
      showRegisterSamplesDisclaimer();
      return;
    }
    if (!measurementService.hasMeasurements(projectId, currentExperimentId)) {
      showRegisterMeasurementDisclaimer();
    } else {
      showMeasurements();
    }
  }

  private void showRegisterSamplesDisclaimer() {
    noMeasurementDisclaimer.setVisible(false);
    content.setVisible(false);
    measurementDetailsComponent.setVisible(false);
    registerSamplesDisclaimer.setVisible(true);
  }

  private void showRegisterMeasurementDisclaimer() {
    noMeasurementDisclaimer.setVisible(true);
    content.setVisible(false);
    measurementDetailsComponent.setVisible(false);
    registerSamplesDisclaimer.setVisible(false);
  }

  private void showMeasurements() {
    noMeasurementDisclaimer.setVisible(false);
    registerSamplesDisclaimer.setVisible(false);
    content.setVisible(true);
    // edit capabilities (edit/delete) follow the caller's ACL scope on this project;
    // read-only users must never be offered mutation actions in the first place
    measurementDetailsComponent.setWriteAccess(
        userPermissions.editProject(context.projectId().orElseThrow()));
    measurementDetailsComponent.setContext(context);
    measurementDetailsComponent.setVisible(true);
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
      List<MeasurementRegistrationInformationPxP> registrationInformationPxP,
      List<MeasurementRegistrationInformationIP> registrationInformationIP) {

    public RegistrationRequestPackage {
      registrationInformationNGS = List.copyOf(Objects.requireNonNull(registrationInformationNGS));
      registrationInformationPxP = List.copyOf(Objects.requireNonNull(registrationInformationPxP));
      registrationInformationIP = List.copyOf(Objects.requireNonNull(registrationInformationIP));
    }

  }

  record UpdateRequestPackage(List<MeasurementUpdateInformationNGS> updateInformationNGS,
                              List<MeasurementUpdateInformationPxP> updateInformationPxP,
                              List<MeasurementUpdateInformationIP> updateInformationIP) {

    public UpdateRequestPackage {
      updateInformationNGS = List.copyOf(Objects.requireNonNull(updateInformationNGS));
      updateInformationPxP = List.copyOf(Objects.requireNonNull(updateInformationPxP));
      updateInformationIP = List.copyOf(Objects.requireNonNull(updateInformationIP));
    }

  }

}
