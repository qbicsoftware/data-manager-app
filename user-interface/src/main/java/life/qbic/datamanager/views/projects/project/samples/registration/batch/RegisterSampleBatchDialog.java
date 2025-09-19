package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.files.export.download.ByteArrayDownloadStreamProvider;
import life.qbic.datamanager.files.parsing.MetadataParser.ParsingException;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor.SampleInformationForNewSample;
import life.qbic.datamanager.files.parsing.xlsx.XLSXParser;
import life.qbic.datamanager.views.general.WizardDialogWindow;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.FileType;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.SucceededEvent;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.UploadedData;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
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
import reactor.core.publisher.Flux;

public class RegisterSampleBatchDialog extends WizardDialogWindow {

  private static final MimeType OPEN_XML = MimeType.valueOf(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  private final List<SampleRegistrationInformation> validatedSampleMetadata;
  private final TextField batchNameField;
  private static final Logger log = LoggerFactory.logger(RegisterSampleBatchDialog.class);
  private static final int MAX_FILE_SIZE = 25 * 1024 * 1024;
  private final Div initialView;
  private final Div inProgressView;
  private final Div failedView;
  private final Div succeededView;
  private final UploadWithDisplay uploadWithDisplay;
  private final transient MessageSourceNotificationFactory messageFactory;
  private final DownloadComponent downloadComponent;
  private final transient AsyncProjectService service;

  public RegisterSampleBatchDialog(AsyncProjectService asyncProjectService,
      MessageSourceNotificationFactory messageFactory,
      String experimentId,
      String projectId,
      String projectCode) {
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

    validatedSampleMetadata = new ArrayList<>();
    uploadWithDisplay = new UploadWithDisplay(MAX_FILE_SIZE, new FileType[]{
        new FileType(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    });
    uploadWithDisplay.addUnspecificFailureListener(
        uploadFailed ->
            /* display of the error is handled by the uploadWithDisplay component. However we do need to log with the context*/
            log.error(
                "Upload failed for project(" + projectId + ") experiment(" + experimentId + ")",
                uploadFailed.getCause()));
    uploadWithDisplay.addSuccessListener(
        uploadSucceeded -> onUploadSucceeded(experimentId, projectId,
            uploadSucceeded));
    uploadWithDisplay.addRemovedListener(uploadRemoved -> setValidatedSampleMetadata(List.of()));

    Span uploadTheSampleDataTitle = new Span("Upload the sample data");
    uploadTheSampleDataTitle.addClassName("section-title");
    Div uploadSection = new Div(uploadTheSampleDataTitle, uploadWithDisplay);
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
      UploadedData uploadedData) {
    ParsingResult parsingResult = XLSXParser.create().parse(uploadedData.inputStream());
    return new SampleInformationExtractor()
        .extractInformationForNewSamples(parsingResult);
  }

  private static InvalidUploadDisplay invalidDisplay(
      String fileName, List<ValidationResult> validationResults) {
    List<String> failureReasons = validationResults.stream()
        .flatMap(res -> res.failures().stream()).toList();
    return new InvalidUploadDisplay(fileName, failureReasons);
  }

  private static ValidationRequest convertToRequest(SampleRegistrationInformation registration,
      String projectId, String experimentId) {
    return new ValidationRequest(projectId, experimentId, registration);
  }

  private void setValidatedSampleMetadata(List<SampleRegistrationInformation> registrations) {
    this.validatedSampleMetadata.clear();
    this.validatedSampleMetadata.addAll(registrations);
  }

  private void onUploadSucceeded(
      String experimentId,
      String projectId,
      SucceededEvent uploadSucceeded) {
    UploadWithDisplay component = uploadSucceeded.getSource();
    UI ui = component.getUI().orElseThrow();
    UploadedData uploadedData = component.getUploadedData().orElseThrow();

    InProgressDisplay uploadProgressDisplay = new InProgressDisplay(uploadedData.fileName());
    component.setDisplay(uploadProgressDisplay);

    List<SampleInformationForNewSample> sampleInformationForNewSamples;
    try {
      sampleInformationForNewSamples = extractSampleInformationForNewSamples(
          uploadedData);
    } catch (ParsingException e) {
      RuntimeException runtimeException = new RuntimeException(
          "Parsing failed.", e);
      log.error("Could not complete validation. " + e.getMessage(), runtimeException);
      InvalidUploadDisplay invalidUploadDisplay = new InvalidUploadDisplay(
          uploadedData.fileName(), "Could not complete validation. " + e.getMessage());
      ui.access(() -> component.setDisplay(invalidUploadDisplay));
      throw runtimeException;
    }

    var registrations = sampleInformationForNewSamples.stream()
        .map(info -> convertToRegistration(info)).toList();

    var responseStream = executeValidation(registrations, projectId, experimentId).doOnError(cause -> {
      log.error("Validation failed.", cause);
      InvalidUploadDisplay invalidUploadDisplay = new InvalidUploadDisplay(
          uploadedData.fileName(), "Apologies, the validation failed. Please try again.");
      ui.access(() -> component.setDisplay(invalidUploadDisplay));
    }).toStream();

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
      ui.access(() -> component.setDisplay(invalidDisplay(
          uploadedData.fileName(),
          failedValidations.stream()
              .toList())));
      setValidatedSampleMetadata(List.of());
      return;
    }

    if (successfulValidations.isEmpty()) {
      // the empty case!
      ui.access(() -> component.setDisplay(
          new InvalidUploadDisplay(uploadedData.fileName(),
              "No valid sample metadata provided.")));
    }

    if (!successfulValidations.isEmpty()) {
      ui.access(() -> component
          .setDisplay(new ValidUploadDisplay(uploadedData.fileName(),
              successfulValidations.size())));
      setValidatedSampleMetadata(registrations);
    }
  }


  private SampleRegistrationInformation convertToRegistration(
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
    if (validatedSampleMetadata.isEmpty() && uploadWithDisplay.getUploadedData().isEmpty()) {
      // nothing is uploaded
      var uploadProgressDisplay = new InvalidUploadDisplay(
          "Nothing was uploaded. Please upload the sample metadata and try again.");
      uploadWithDisplay.setDisplay(uploadProgressDisplay);
      return;
    } else if (validatedSampleMetadata.isEmpty() && uploadWithDisplay.getUploadedData()
        .isPresent()) {
      // the uploaded data is not valid
      return;
    }
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(),
        batchNameField.getValue(), validatedSampleMetadata));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  static class InvalidUploadDisplay extends Div {

    public InvalidUploadDisplay(String error) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var failuresTitle = new Span(error);
      var errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
      errorIcon.addClassName("error");
      var header = new Span(errorIcon, failuresTitle);
      header.addClassName("header");
      box.add(header);
      validationBox.add(box);
      add(validationBox);
    }

    public InvalidUploadDisplay(String fileName, List<String> failureReasons) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var failuresTitle = new Span("Invalid sample metadata");
      var errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
      errorIcon.addClassName("error");
      var header = new Span(errorIcon, failuresTitle);
      header.addClassName("header");
      var instruction = new Span(
          "Please correct the entries in the uploaded file and re-upload the file.");
      instruction.addClassName("secondary");
      Div validationDetails = new Div();

      Map<String, Integer> frequencyMap = failureReasons.stream()
          .distinct()
          .collect(Collectors.toMap(
              Function.identity(),
              v -> Collections.frequency(failureReasons, v)
          ));
      frequencyMap.forEach(
          (key, frequency) -> {
            String s = frequency + " sample" + ((frequency > 1) ? "s." : ".");
            Span span = new Span(s);
            span.addClassName("bold");
            validationDetails.add(new Div(new Span(key + " for "), span));
          });
      box.add(header, validationDetails, instruction);
      validationBox.add(box);
      add(fileNameLabel, validationBox);
    }

    public InvalidUploadDisplay(String fileName, String failureReason) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var failuresTitle = new Span("Invalid sample metadata");
      var errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
      errorIcon.addClassName("error");
      var header = new Span(errorIcon, failuresTitle);
      header.addClassName("header");
      var instruction = new Span(
          "Please correct the entries in the uploaded file and re-upload the file.");
      instruction.addClassName("secondary");
      Div validationDetails = new Div();

      validationDetails.add(new Div(failureReason));
      box.add(header, validationDetails, instruction);
      validationBox.add(box);
      add(fileNameLabel, validationBox);
    }
  }

  static class ValidUploadDisplay extends Div {

    private ValidUploadDisplay(String fileName, int count) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      Div validationBox = new Div();
      validationBox.addClassName("validation-display-box");
      var box = new Div();
      var approvedTitle = new Span("Your data has been approved");
      var validIcon = VaadinIcon.CHECK_CIRCLE_O.create();
      validIcon.addClassName("success");
      var header = new Span(validIcon, approvedTitle);
      header.addClassName("header");
      var instruction = new Span("Please click Register to register your samples");
      instruction.addClassName("secondary");
      Div validationDetails = new Div();
      var approvedSamples = new Span("%d samples".formatted(count));
      approvedSamples.addClassName("bold");
      validationDetails.add(new Span("Sample data for "), approvedSamples,
          new Span(" is now ready to be registered."));
      box.add(header, validationDetails, instruction);
      validationBox.add(box);
      add(fileNameLabel, validationBox);
    }
  }

  private static class InProgressDisplay extends Div {

    private InProgressDisplay(String fileName) {
      addClassName("uploaded-item");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName("file-name");
      ProgressBar progressBar = new ProgressBar();
      progressBar.setIndeterminate(true);
      add(fileNameLabel, new Div("Validating file..."), progressBar);
    }
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
}
