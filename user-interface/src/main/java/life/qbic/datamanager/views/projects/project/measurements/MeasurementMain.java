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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.download.DownloadProvider;
import life.qbic.datamanager.views.general.download.MeasurementTemplateDownload;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMetadataUploadDialog.MODE;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMetadataUploadDialog.MeasurementMetadataUpload;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateListComponent.DownloadMeasurementTemplateEvent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
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
public class MeasurementMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(MeasurementMain.class);
  private static Disclaimer registerSamplesDisclaimer;
  private final MeasurementTemplateDownload measurementTemplateDownload;
  private final MeasurementTemplateListComponent measurementTemplateListComponent;
  private final MeasurementDetailsComponent measurementDetailsComponent;
  private final MeasurementPresenter measurementPresenter;
  private final TextField measurementSearchField = new TextField();
  private final transient SampleInformationService sampleInformationService;
  private final transient MeasurementService measurementService;
  private final transient MeasurementValidationService measurementValidationService;
  private final Div content = new Div();
  private final InfoBox rawDataAvailableInfo = new InfoBox();
  private final Div noMeasurementDisclaimer;
  private final ProteomicsMeasurementContentProvider proteomicsMeasurementContentProvider;
  private final DownloadProvider downloadProvider;
  private transient Context context;

  public MeasurementMain(
      @Autowired MeasurementTemplateListComponent measurementTemplateListComponent,
      @Autowired MeasurementDetailsComponent measurementDetailsComponent,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired MeasurementService measurementService,
      @Autowired MeasurementPresenter measurementPresenter,
      @Autowired MeasurementValidationService measurementValidationService) {
    Objects.requireNonNull(measurementTemplateListComponent);
    Objects.requireNonNull(measurementDetailsComponent);
    Objects.requireNonNull(measurementService);
    Objects.requireNonNull(measurementValidationService);
    this.measurementDetailsComponent = measurementDetailsComponent;
    this.measurementTemplateListComponent = measurementTemplateListComponent;
    this.measurementService = measurementService;
    this.measurementPresenter = measurementPresenter;
    this.proteomicsMeasurementContentProvider = new ProteomicsMeasurementContentProvider();
    this.downloadProvider = new DownloadProvider(proteomicsMeasurementContentProvider);
    this.measurementValidationService = measurementValidationService;
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    measurementTemplateDownload = new MeasurementTemplateDownload();
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
    add(downloadProvider);
    addClassName("measurement");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        getClass().getSimpleName(), System.identityHashCode(this),
        measurementTemplateListComponent.getClass().getSimpleName(),
        System.identityHashCode(measurementTemplateListComponent)));
  }

  private static String convertErrorCodeToMessage(MeasurementService.ErrorCode errorCode) {
    return switch (errorCode) {
      case FAILED -> "Registration failed";
      case UNKNOWN_ORGANISATION_ROR_ID -> "Could not resolve ROR identifier.";
      case UNKNOWN_ONTOLOGY_TERM -> "Encountered unknown ontology term.";
      case WRONG_EXPERIMENT -> "There are samples that do not belong to this experiment.";
      case MISSING_ASSOCIATED_SAMPLES -> "Missing sample information for this measurement.";
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
    downloadButton.addClickListener(event -> downloadMetadata());
    Button registerMeasurementButton = new Button("Register Measurements");
    registerMeasurementButton.addClassName("primary");
    registerMeasurementButton.addClickListener(
        event -> openRegisterMeasurementDialog());

    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> openEditMeasurementDialog());
    Span buttonBar = new Span(downloadButton, editButton,
        registerMeasurementButton);
    buttonBar.addClassName("button-bar");
    Span buttonAndField = new Span(measurementSearchField, buttonBar);
    buttonAndField.addClassName("buttonAndField");
    content.add(buttonAndField);
  }

  private Dialog setupDialog(MeasurementMetadataUploadDialog dialog) {
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmListener(confirmEvent -> triggerMeasurementRegistration(confirmEvent.uploads(),
        confirmEvent.getSource()));
    return dialog;
  }
      /*
      for (var upload : uploads) {
        var source = confirmEvent.getSource();
        var measurementData = upload.measurementMetadata();

        if (editMode) {
          source.taskInProgress(
              "Updating %s measurements ...".formatted(measurementData.size()),
              "This might take a minute");
          //Necessary so the dialog window switches to show the upload progress
          UI.getCurrent().push();
          measurementService.updateAll(upload.measurementMetadata(),
              context.projectId().orElseThrow()).thenAccept(results -> {
            var errorResult = results.stream().filter(Result::isError).findAny();
            if (errorResult.isPresent()) {
              var errors = errorResult.stream().filter(Result::isError)
                  .map(Result::getError)
                  .map(MeasurementMain::convertErrorCodeToMessage).distinct().toList();
              source.getUI().ifPresent(ui -> ui.access(
                  () -> source.taskFailed("Measurement Update could not be completed",
                      "The following errors occurred while updating the data:" +
                          "\n" +
                          String.join("\n", errors)
                          + "\n" + "Please try again")));
            } else {
              source.getUI().ifPresent(ui -> ui.access(
                  () -> source.taskSucceeded("Measurement Update is complete",
                      String.format("%s Measurements are now successfully updated",
                          results.size()))));
            }
          }).join(); // we wait for the update to finish
        } else {
          source.taskInProgress(
              "Registering %s measurements ...".formatted(measurementData.size()),
              "This might take a minute");
          UI.getCurrent().push();
          measurementService.registerAll(upload.measurementMetadata(),
              context.projectId().orElseThrow()).thenAccept(results -> {
            var errorResult = results.stream().filter(Result::isError).findAny();
            if (errorResult.isPresent()) {
              source.getUI()
                  .ifPresent(ui -> ui.access(
                      () -> source.taskFailed("Measurement Registration could not be completed",
                          "There was an error registering the measurement data. Please try again")));
            } else {
              source.getUI().ifPresent(ui -> ui.access(
                  () -> source.taskSucceeded("Measurement Registration is complete",
                      String.format("%s Measurements are now successfully registered",
                          results.size()))));
            }
          }).join(); // we wait for the registration to finish
        }
      }
    });
       */

  private void triggerMeasurementRegistration(
      List<MeasurementMetadataUpload<MeasurementMetadata>> measurementMetadataUploads,
      MeasurementMetadataUploadDialog measurementMetadataUploadDialog) {
    String process =
        measurementMetadataUploadDialog.getMode() == MODE.EDIT ? "update" : "registration";
    for (var upload : measurementMetadataUploads) {
      var measurementData = upload.measurementMetadata();
      measurementMetadataUploadDialog.taskInProgress(
          "%s of %s measurements ...".formatted(process, measurementData.size()),
          "This might take a minute");
      //Necessary so the dialog window switches to show the upload progress
      UI.getCurrent().push();
      CompletableFuture<List<Result<MeasurementId, MeasurementService.ErrorCode>>> completableFuture = new CompletableFuture();
      if (measurementMetadataUploadDialog.getMode().equals(MODE.EDIT)) {
        completableFuture = measurementService.updateAll(upload.measurementMetadata(),
            context.projectId().orElseThrow());
      } else {
        completableFuture = measurementService.registerAll(upload.measurementMetadata(),
            context.projectId().orElseThrow());
      }
      completableFuture.thenAccept(results -> {
        var errorResult = results.stream().filter(Result::isError).findAny();
        if (errorResult.isPresent()) {
          var errors = errorResult.stream().filter(Result::isError)
              .map(Result::getError)
              .map(MeasurementMain::convertErrorCodeToMessage).distinct().toList();
          measurementMetadataUploadDialog.getUI().ifPresent(ui -> ui.access(
              () -> measurementMetadataUploadDialog.taskFailed(
                  "Measurement %s could not be completed".formatted(process),
                  "The following errors occurred during the measurement %s:".formatted(process) +
                      //Todo format errors better with better messages for user
                      "\n" +
                      String.join("\n", errors)
                      + "\n" + "Please try again")));
        } else {
          measurementMetadataUploadDialog.getUI().ifPresent(ui -> ui.access(
              () -> measurementMetadataUploadDialog.taskSucceeded(
                  "Measurement %s is complete".formatted(process),
                  "Measurement %s for %s measurements was successful".formatted(process,
                      results.size()))));
        }
      }).join(); // we wait for the update to finish
    }
  }

  private void openEditMeasurementDialog() {
    var dialog = new MeasurementMetadataUploadDialog(measurementValidationService, MODE.EDIT,
        context.projectId().orElse(null));
    setupDialog(dialog);
    dialog.open();
  }

  private void downloadMetadata() {
    var proteomicsMeasurements = measurementService.findProteomicsMeasurements(
        context.experimentId().orElseThrow(() -> new ApplicationException(
            ErrorCode.GENERAL, null)),
        context.projectId().orElseThrow(() -> new ApplicationException(ErrorCode.GENERAL, null)));
    var result = proteomicsMeasurements.stream().map(measurementPresenter::expandPools)
        .flatMap(items -> items.stream()).toList();

    proteomicsMeasurementContentProvider.setMeasurements(result);
    downloadProvider.trigger();
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
      String routeToMeasurementPage = String.format(Projects.SAMPLES,
          currentProjectId,
          currentExperimentId);
      log.debug(String.format(
          "Rerouting to sample page for experiment %s of project %s: %s",
          currentExperimentId, currentProjectId, routeToMeasurementPage));
      componentEvent.getSource().getUI().ifPresent(ui -> ui.navigate(routeToMeasurementPage));
    }
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
    measurementTemplateDownload.trigger(downloadMeasurementTemplateEvent.measurementTemplate());
  }

  private void openRegisterMeasurementDialog() {
    var dialog = new MeasurementMetadataUploadDialog(measurementValidationService, MODE.ADD,
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
      String routeToRawDataPage = String.format(Projects.RAWDATA,
          currentProjectId,
          currentExperimentId);
      log.debug(String.format(
          "Rerouting to raw data page for experiment %s of project %s: %s",
          currentExperimentId, currentProjectId, routeToRawDataPage));
      componentEvent.getSource().getUI().ifPresent(ui -> ui.navigate(routeToRawDataPage));
    }
  }


}
