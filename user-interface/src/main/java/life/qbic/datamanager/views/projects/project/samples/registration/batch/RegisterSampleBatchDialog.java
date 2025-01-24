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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.datamanager.download.DownloadContentProvider.XLSXDownloadContentProvider;
import life.qbic.datamanager.download.DownloadProvider;
import life.qbic.datamanager.exporting.xlsx.templates.TemplateService;
import life.qbic.datamanager.importing.SampleInformationExtractor;
import life.qbic.datamanager.importing.SampleInformationExtractor.SampleInformationForNewSample;
import life.qbic.datamanager.importing.parser.ParsingResult;
import life.qbic.datamanager.importing.parser.xlsx.XLSXParser;
import life.qbic.datamanager.views.general.WizardDialogWindow;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.FileType;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.SucceededEvent;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.UploadedData;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.ValidationResultWithPayload;
import life.qbic.projectmanagement.application.sample.SampleMetadata;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class RegisterSampleBatchDialog extends WizardDialogWindow {

  private final List<SampleMetadata> validatedSampleMetadata;
  private final TextField batchNameField;
  private static final Logger log = LoggerFactory.logger(RegisterSampleBatchDialog.class);
  private final Div initialView;
  private final Div inProgressView;
  private final Div failedView;
  private final Div succeededView;
  private static final int MAX_FILE_SIZE = 25 * 1024 * 1024;
  private final UploadWithDisplay uploadWithDisplay;

  private void setValidatedSampleMetadata(List<SampleMetadata> validatedSampleMetadata) {
    this.validatedSampleMetadata.clear();
    this.validatedSampleMetadata.addAll(validatedSampleMetadata);
  }

  public RegisterSampleBatchDialog(SampleValidationService sampleValidationService,
      TemplateService templateService,
      String experimentId,
      String projectId,
      String projectCode) {

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

    Div downloadMetadataSection = setupDownloadMetadataSection(templateService, experimentId,
        projectId, projectCode);

    validatedSampleMetadata = new ArrayList<>();
    uploadWithDisplay = new UploadWithDisplay(MAX_FILE_SIZE, new FileType[]{
        new FileType(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    });
    uploadWithDisplay.addFailureListener(
        uploadFailed -> {/* display of the error is handled by the uploadWithDisplay component. So nothing to do here.*/});
    uploadWithDisplay.addSuccessListener(
        uploadSucceeded -> onUploadSucceeded(sampleValidationService, experimentId, projectId,
            uploadSucceeded));
    uploadWithDisplay.addRemovedListener(uploadRemoved -> {
      setValidatedSampleMetadata(List.of());
    });

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
    add(initialView, inProgressView, failedView, succeededView);
  }

  private void onUploadSucceeded(SampleValidationService sampleValidationService,
      String experimentId,
      String projectId,
      SucceededEvent uploadSucceeded) {
    UploadWithDisplay component = uploadSucceeded.getSource();
    UI ui = component.getUI().orElseThrow();
    UploadedData uploadedData = component.getUploadedData().orElseThrow();

    InProgressDisplay uploadProgressDisplay = new InProgressDisplay(uploadedData.fileName());
    component.setDisplay(uploadProgressDisplay);

    List<SampleInformationForNewSample> sampleInformationForNewSamples = extractSampleInformationForNewSamples(
        uploadedData);

    List<CompletableFuture<ValidationResultWithPayload<SampleMetadata>>> validations = new ArrayList<>();
    for (SampleInformationForNewSample sampleInformationForNewSample : sampleInformationForNewSamples) {
      CompletableFuture<ValidationResultWithPayload<SampleMetadata>> validation = sampleValidationService.validateNewSampleAsync(
          sampleInformationForNewSample.sampleName(),
          sampleInformationForNewSample.biologicalReplicate(),
          sampleInformationForNewSample.condition(),
          sampleInformationForNewSample.species(),
          sampleInformationForNewSample.specimen(),
          sampleInformationForNewSample.analyte(),
          sampleInformationForNewSample.analysisMethod(),
          sampleInformationForNewSample.comment(),
          experimentId,
          projectId
      ).orTimeout(1, TimeUnit.MINUTES);
      validations.add(validation);
    }
    var validationTasks = CompletableFuture
        //allOf makes sure exceptional state is transferred to outer completable future.
        .allOf(validations.toArray(new CompletableFuture[0]))
        .thenApply(v -> validations.stream()
            .map(CompletableFuture::join)
            .toList())
        .orTimeout(5, TimeUnit.MINUTES);

    validationTasks
        .thenAccept(validationResults -> {

          List<ValidationResultWithPayload<SampleMetadata>> failedValidations = validationResults.stream()
              .filter(validation -> validation.validationResult().containsFailures())
              .toList();
          List<ValidationResultWithPayload<SampleMetadata>> succeededValidations = validationResults.stream()
              .filter(validation -> validation.validationResult().allPassed())
              .toList();

          if (!failedValidations.isEmpty()) {
            ui.access(() -> component.setDisplay(invalidDisplay(
                uploadedData.fileName(),
                failedValidations.stream().map(ValidationResultWithPayload::validationResult)
                    .toList())));
            setValidatedSampleMetadata(List.of());
            return;
          }
          if (!succeededValidations.isEmpty()) {
            ui.access(() -> component
                .setDisplay(new ValidUploadDisplay(uploadedData.fileName(),
                    succeededValidations.size())));
            setValidatedSampleMetadata(
                succeededValidations.stream().map(ValidationResultWithPayload::payload).toList());
          }
          if (succeededValidations.isEmpty()) {
            // the empty case!
            ui.access(() -> component.setDisplay(
                new InvalidUploadDisplay(uploadedData.fileName(),
                    "No valid sample metadata provided.")));
          }
        })
        .exceptionally(e -> {
              RuntimeException runtimeException = new RuntimeException(
                  "At least one validation task could not complete.", e);
              log.error("Could not complete validation. Please try again.", runtimeException);
          InvalidUploadDisplay invalidUploadDisplay = new InvalidUploadDisplay(
              uploadedData.fileName(), "Could not complete validation. Please try again.");
              ui.access(() -> component.setDisplay(invalidUploadDisplay));
              throw runtimeException;
            }
        );

  }

  private Div setupDownloadMetadataSection(TemplateService templateService, String experimentId,
      String projectId, String projectCode) {
    Button downloadTemplate = new Button("Download metadata template");
    downloadTemplate.addClassName("download-metadata-button");
    downloadTemplate.addClickListener(buttonClickEvent -> {
      try (XSSFWorkbook workbook = templateService.sampleBatchRegistrationXLSXTemplate(
          projectId,
          experimentId)) {
        var downloadProvider = new DownloadProvider(
            new XLSXDownloadContentProvider(projectCode + "_registration_template.xlsx", workbook));
        add(downloadProvider);
        downloadProvider.trigger();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
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

  public static class ConfirmEvent extends ComponentEvent<RegisterSampleBatchDialog> {

    private final String batchName;
    private final List<SampleMetadata> validatedSampleMetadata;


    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source                  the source component
     * @param fromClient              <code>true</code> if the event originated from the client
     *                                side, <code>false</code> otherwise
     * @param batchName
     * @param validatedSampleMetadata
     */
    public ConfirmEvent(RegisterSampleBatchDialog source, boolean fromClient,
        String batchName,
        List<SampleMetadata> validatedSampleMetadata) {
      super(source, fromClient);
      this.batchName = batchName;
      this.validatedSampleMetadata = validatedSampleMetadata;
    }

    public List<SampleMetadata> validatedSampleMetadata() {
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
