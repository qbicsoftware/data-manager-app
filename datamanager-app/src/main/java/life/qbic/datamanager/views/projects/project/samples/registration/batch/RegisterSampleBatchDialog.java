package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.files.export.download.ByteArrayDownloadStreamProvider;
import life.qbic.datamanager.files.parsing.MetadataParser.ParsingException;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor.SampleInformationForNewSample;
import life.qbic.datamanager.files.parsing.xlsx.XLSXParser;
import life.qbic.datamanager.views.general.WizardDialogWindow;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent;
import life.qbic.datamanager.views.general.upload.UploadedFilesChangeListener.FileEntry;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleUploadDisplay.InProgressDisplay;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleUploadDisplay.InvalidUploadDisplay;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleUploadDisplay.ValidUploadDisplay;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.AccessDeniedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleRegistrationInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationResponse;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import org.springframework.util.MimeType;
import org.springframework.util.unit.DataSize;
import reactor.core.publisher.Flux;

public class RegisterSampleBatchDialog extends WizardDialogWindow {

  private static final MimeType OPEN_XML = MimeType.valueOf(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  private final Map<String, List<SampleRegistrationInformation>> validatedSampleMetadata;
  private final TextField batchNameField;
  private static final Logger log = LoggerFactory.logger(RegisterSampleBatchDialog.class);
  private static final int MAX_FILE_SIZE = 25 * 1024 * 1024;
  private final Div initialView;
  private final Div inProgressView;
  private final Div failedView;
  private final Div succeededView;
  private final ContentUploadComponent contentUploadComponent;
  private final transient MessageSourceNotificationFactory messageFactory;
  private final DownloadComponent downloadComponent;
  private final transient AsyncProjectService service;

  public RegisterSampleBatchDialog(AsyncProjectService asyncProjectService,
      MessageSourceNotificationFactory messageFactory,
      String experimentId,
      String projectId,
      String projectCode,
      UploadConfiguration uploadConfiguration) {
    service = Objects.requireNonNull(asyncProjectService);
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.downloadComponent = new DownloadComponent();

    setHeaderTitle("Register Sample Batch");
    setConfirmButtonLabel("Register");

    initialView = new Div();
    initialView.addClassName("initial-view");
    inProgressView = new Div();
    inProgressView.addClassName("in-progress-view");
    failedView = new Div();
    failedView.addClassName("failed-view");
    succeededView = new Div();
    succeededView.addClassName("succeeded-view");

    addClassName("register-samples-dialog");
    batchNameField = new TextField("Batch name");
    batchNameField.addClassName("batch-name-field");
    batchNameField.setRequired(true);
    batchNameField.setErrorMessage("Please provide a name for your batch.");
    batchNameField.setPlaceholder("Please enter a name for your batch");

    Div downloadMetadataSection = setupDownloadMetadataSection(service, experimentId,
        projectId, projectCode);

    validatedSampleMetadata = new HashMap<>();

    contentUploadComponent = new ContentUploadComponent(uploadConfiguration);
    contentUploadComponent.setAcceptedFileTypes(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    contentUploadComponent.setMaxFiles(1);
    contentUploadComponent.setMaxFileSize(DataSize.ofBytes(MAX_FILE_SIZE));

    var uploadDisplay = new SampleUploadDisplay();
    Registration controllerRegistration = new SampleRegistrationUploadDisplayController(projectId,
        experimentId)
        .control(uploadDisplay, contentUploadComponent);
    uploadDisplay.addDetachListener(it -> controllerRegistration.remove());

    contentUploadComponent.addUnspecificFailureListener(
        uploadFailed ->
            /* display of the error is handled by the uploadWithDisplay component. However, we do need to log with the context*/
            log.error(
                "Upload failed for project(" + projectId + ") experiment(" + experimentId + ")",
                uploadFailed.getCause()));

    Span uploadTheSampleDataTitle = new Span("Upload the sample data");
    uploadTheSampleDataTitle.addClassName("section-title");
    Div uploadSection = new Div(uploadTheSampleDataTitle, contentUploadComponent, uploadDisplay);
    uploadSection.addClassName("upload-section");
    uploadSection.addClassName("section-with-title");
    initialView.add(batchNameField, downloadMetadataSection, uploadSection);
    initialView.setVisible(true);
    inProgressView.setVisible(false);
    failedView.setVisible(false);
    succeededView.setVisible(false);
    add(initialView, inProgressView, failedView, succeededView, downloadComponent);
  }

  private void handleError(Throwable throwable) {
    if (Objects.requireNonNull(throwable) instanceof AccessDeniedException) {
      handleAccessDeniedError();
    } else {
      handleUnexpectedError(throwable);
    }
  }

  private void handleUnexpectedError(Throwable throwable) {
    throw new ApplicationException("We are sorry, an unexpected error occurred.", throwable);
  }

  private void handleAccessDeniedError() {
    getUI().ifPresent(ui -> ui.access(
        () -> messageFactory.toast("access.denied.message", new Object[]{}, getLocale()).open()));
  }

  private static List<SampleInformationForNewSample> extractSampleInformationForNewSamples(
      InputStream inputStream) {
    ParsingResult parsingResult = XLSXParser.create().parse(inputStream);
    return new SampleInformationExtractor()
        .extractInformationForNewSamples(parsingResult);
  }

  private static ValidationRequest convertToRequest(SampleRegistrationInformation registration,
      String projectId, String experimentId) {
    return new ValidationRequest(projectId, experimentId, registration);
  }


  private static SampleRegistrationInformation convertToRegistration(
      SampleInformationForNewSample information) {
    return new SampleRegistrationInformation(
        information.sampleName(),
        information.biologicalReplicate(),
        information.condition(),
        information.species(),
        information.specimen(),
        information.analyte(),
        information.analysisMethod(),
        information.comment(),
        information.confoundingVariables()
    );
  }

  private Flux<ValidationResponse> executeValidation(
      List<SampleRegistrationInformation> registrations,
      String projectId, String experimentId) {
    var requests = registrations.stream()
        .map(registration -> convertToRequest(registration, projectId, experimentId));
    return service.validate(Flux.fromStream(requests));
  }

  private void triggerDownload(DigitalObject resource, String filename) {
    getUI().ifPresent(
        ui -> ui.access(() -> downloadComponent.trigger(new ByteArrayDownloadStreamProvider() {
          @Override
          public byte[] getBytes() {
            try (var content = resource.content()) {
              return content.readAllBytes();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }

          @Override
          public String getFilename() {
            return filename;
          }

          @Override
          public Optional<Long> contentLength() {
            return Optional.empty();
          }
        })));
  }

  private Div setupDownloadMetadataSection(AsyncProjectService service, String experimentId,
      String projectId, String projectCode) {
    Button downloadTemplate = new Button("Download metadata template");
    downloadTemplate.addClassName("download-metadata-button");
    downloadTemplate.addClickListener(
        buttonClickEvent -> service.sampleRegistrationTemplate(projectId, experimentId,
            OPEN_XML).doOnSuccess(resource ->
            triggerDownload(resource,
                FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(), projectCode,
                    "sample metadata template",
                    "xlsx")
            )).doOnError(this::handleError).subscribe());
    Div text = new Div();
    text.addClassName("download-metadata-text");
    text.setText(
        "Please download the metadata template, fill in the sample properties and upload the metadata sheet below to register the sample batch.");
    Div downloadMetadataSection = new Div();
    downloadMetadataSection.addClassName("download-metadata");
    downloadMetadataSection.addClassName("section-with-title");
    Span sectionTitle = new Span("Download metadata template");
    sectionTitle.addClassName("download-metadata-section-title");
    sectionTitle.addClassName("section-title");

    Div sectionContent = new Div();
    sectionContent.addClassName("download-metadata-section-content");
    sectionContent.add(text, downloadTemplate);
    downloadMetadataSection.add(sectionTitle, sectionContent);
    return downloadMetadataSection;
  }

  @Override
  public void taskFailed(String label, String description) {
    failedView.removeAll();
    StepInformation top = new StepInformation(
        new Div("Register the sample batch metadata"),
        new Div("It may take some time for the sample registration to complete."),
        false);

    Span errorText = new Span("There was an error registering the sample data. Please try again.");
    errorText.addClassName("error-text");
    Icon icon = VaadinIcon.CLOSE_CIRCLE.create();
    icon.addClassName("error");
    Div errorBox = new Div(
        icon,
        errorText
    );
    errorBox.addClassName("error-box");
    var bottom = new StepInformation(new Div("Sample registration could not be completed."),
        errorBox, true);
    failedView.add(top.asComponent(), bottom.asComponent());
    failedView.setVisible(true);
    setConfirmButtonLabel("Try Again");
    showFailed();

    initialView.setVisible(false);
    inProgressView.setVisible(false);
    succeededView.setVisible(false);
  }

  @Override
  public void taskSucceeded(String label, String description) {
    succeededView.removeAll();
    StepInformation top = new StepInformation(
        new Div("Register the sample batch metadata"),
        new Div("It may take some time for the sample registration to complete."),
        false);

    Span successText = new Span("Sample batch is successfully registered.");
    successText.addClassName("success-text");
    Icon icon = VaadinIcon.CHECK_CIRCLE_O.create();
    icon.addClassName("success");
    Div successBox = new Div(
        icon,
        successText
    );
    successBox.addClassName("success-box");
    var bottom = new StepInformation(new Div("Sample batch update is complete."),
        successBox, true);

    succeededView.add(top.asComponent(), bottom.asComponent());
    succeededView.setVisible(true);
    showSucceeded();

    initialView.setVisible(false);
    inProgressView.setVisible(false);
    failedView.setVisible(false);
  }

  @Override
  public void taskInProgress(String label, String description) {

    StepInformation top = new StepInformation(
        new Div("Register the sample batch metadata"),
        new Div("It may take some time for the sample registration to complete."),
        false);
    ProgressBar progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    StepInformation bottom = new StepInformation(new Div("Registering samples.."),
        progressBar, true);
    inProgressView.removeAll();
    inProgressView.add(top.asComponent(), bottom.asComponent());
    inProgressView.setVisible(true);
    showInProgress();

    initialView.setVisible(false);
    failedView.setVisible(false);
    succeededView.setVisible(false);
  }

  public Registration addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    return addListener(ConfirmEvent.class, listener);
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    return addListener(CancelEvent.class, listener);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    if (batchNameField.isInvalid()) {
      // once the user focused the batch name field at least once, the setRequired(true) validation is applied.
      batchNameField.focus();
      return;
    }
    if (batchNameField.isEmpty() || batchNameField.getValue().isBlank()) {
      // if the user never focused the name field, no validation took place. Thus, the need to double-check here.
      batchNameField.setInvalid(true);
      batchNameField.focus();
      return;
    }
    if (validatedSampleMetadata.isEmpty()) {
      // nothing to do
      return;
    }
    List<SampleRegistrationInformation> allValidatedData = validatedSampleMetadata.values().stream()
        .flatMap(List::stream)
        .distinct()
        .toList();
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(),
        batchNameField.getValue(), allValidatedData));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  public static class ConfirmEvent extends ComponentEvent<RegisterSampleBatchDialog> {

    private final String batchName;
    private final List<SampleRegistrationInformation> validatedSampleMetadata;


    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source                  the source component
     * @param fromClient              <code>true</code> if the event originated from the client
     *                                side, <code>false</code> otherwise
     * @param batchName               the name of the batch
     * @param registrations a list of validated sample metadata
     */
    public ConfirmEvent(RegisterSampleBatchDialog source, boolean fromClient,
        String batchName,
        List<SampleRegistrationInformation> registrations) {
      super(source, fromClient);
      this.batchName = batchName;
      this.validatedSampleMetadata = registrations;
    }

    public List<SampleRegistrationInformation> validatedSampleMetadata() {
      return Collections.unmodifiableList(validatedSampleMetadata);
    }

    public String batchName() {
      return batchName;
    }
  }

  public static class CancelEvent extends ComponentEvent<RegisterSampleBatchDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(RegisterSampleBatchDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  class SampleRegistrationUploadDisplayController {

    private final String projectId;
    private final String experimentId;

    private SampleRegistrationUploadDisplayController(String projectId, String experimentId) {
      this.projectId = Objects.requireNonNull(projectId);
      this.experimentId = Objects.requireNonNull(experimentId);
    }


    Registration control(SampleUploadDisplay sampleUploadDisplay,
        ContentUploadComponent contentUploadComponent) {

      Objects.requireNonNull(sampleUploadDisplay);
      Objects.requireNonNull(contentUploadComponent);
      var changeRegistration = contentUploadComponent.addChangeListener(event -> {
        var componentUI = contentUploadComponent.getUI();
        switch (event.changeType()) {
          case FILE_ADDED -> {
            // display validation started
            event.changedFiles().forEach(it -> sampleUploadDisplay.setDisplay(it.fileName(),
                new InProgressDisplay(it.fileName())));
            // validate

            event.changedFiles().forEach(fileEntry -> {
              String fileName = fileEntry.fileName();
              contentUploadComponent.getContent(fileName).ifPresentOrElse(
                  fileContent -> {
                    List<SampleInformationForNewSample> sampleInfos;

                    try {
                      sampleInfos = new ArrayList<>(
                          extractSampleInformationForNewSamples(fileContent));
                    } catch (ParsingException parsingException) {
                      log.warn(
                          "Could not parse sample file content " + parsingException.getMessage());
                      componentUI.ifPresent(
                          ui -> ui.access(() -> sampleUploadDisplay.setDisplay(fileName,
                              new InvalidUploadDisplay(fileName,
                                  "Could not complete validation. "
                                      + parsingException.getMessage()))));
                      return;
                    }
                    if (sampleInfos.isEmpty()) {
                      componentUI.ifPresent(ui -> ui.access(() -> sampleUploadDisplay.setDisplay(
                          fileName,
                          new InvalidUploadDisplay(fileName, "No valid metadata provided.")
                      )));
                      return;
                    }
                    List<SampleRegistrationInformation> registrations = sampleInfos.stream()
                        .distinct()
                        .map(RegisterSampleBatchDialog::convertToRegistration)
                        .toList();
                    if (registrations.isEmpty()) {
                      return;
                    }
                    Stream<ValidationResponse> responseStream = executeValidation(registrations,
                        projectId,
                        experimentId)
                        .doOnError(cause -> {
                          log.error("Validation failed.", cause);
                          InvalidUploadDisplay invalidUploadDisplay = new InvalidUploadDisplay(
                              fileName,
                              "Apologies, the validation failed. Please try again.");
                          componentUI.ifPresent(ui -> ui.access(
                              () -> sampleUploadDisplay.setDisplay(fileName,
                                  invalidUploadDisplay)));
                        })
                        .toStream();

                    List<ValidationResult> failedValidations = new ArrayList<>();
                    List<ValidationResult> successfulValidations = new ArrayList<>();

                    responseStream.forEach(responseResult -> {
                      if (responseResult.result().containsFailures()) {
                        failedValidations.add(responseResult.result());
                      } else {
                        successfulValidations.add(responseResult.result());
                      }
                    });

                    if (!failedValidations.isEmpty()) {
                      componentUI.ifPresent(ui -> ui.access(
                          () -> sampleUploadDisplay.setDisplay(fileName,
                              new InvalidUploadDisplay(
                                  fileName,
                                  failedValidations.stream()
                                      .flatMap(res -> res.failures().stream())
                                      .toList()))));
                      validatedSampleMetadata.put(fileName, List.of());
                      return;
                    }

                    if (!successfulValidations.isEmpty()) {
                      componentUI.ifPresent(ui -> ui.access(() -> sampleUploadDisplay
                          .setDisplay(fileName, new ValidUploadDisplay(fileName,
                              successfulValidations.size()))));
                      validatedSampleMetadata.put(fileName, registrations);
                    }
                  },
                  () -> componentUI.ifPresent(
                      ui -> ui.access(() -> sampleUploadDisplay.setDisplay(fileName,
                          new InvalidUploadDisplay(fileName,
                              "No valid sample metadata provided.")))
                  ));
            });
          }

          case FILE_REMOVED -> {
            event.changedFiles().forEach(it -> validatedSampleMetadata.remove(it.fileName()));
            sampleUploadDisplay.removeDisplay(event.changedFiles().stream().map(
                FileEntry::fileName).toList());
          }
        }
      });

      var removedRegistration = contentUploadComponent.addFileRemovedListener(
          event -> sampleUploadDisplay.removeDisplay(List.of(event.getFileName())));

      return () -> {
        changeRegistration.remove();
        removedRegistration.remove();
      };
    }
  }




}
