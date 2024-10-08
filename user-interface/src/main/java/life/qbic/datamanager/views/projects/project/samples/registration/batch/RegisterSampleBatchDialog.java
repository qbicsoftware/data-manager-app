package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.function.Predicate.not;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import life.qbic.datamanager.download.DownloadContentProvider.XLSXDownloadContentProvider;
import life.qbic.datamanager.download.DownloadProvider;
import life.qbic.datamanager.parser.ParsingResult;
import life.qbic.datamanager.parser.SampleInformationExtractor;
import life.qbic.datamanager.parser.SampleInformationExtractor.SampleInformationForNewSample;
import life.qbic.datamanager.parser.xlsx.XLSXParser;
import life.qbic.datamanager.templates.TemplateService;
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

  private void setValidatedSampleMetadata(List<SampleMetadata> validatedSampleMetadata) {
    this.validatedSampleMetadata.clear();
    this.validatedSampleMetadata.addAll(validatedSampleMetadata);
  }

  public RegisterSampleBatchDialog(SampleValidationService sampleValidationService,
      TemplateService templateService,
      String experimentId,
      String projectId) {

    initialView = new Div();
    inProgressView = new Div();
    failedView = new Div();
    succeededView = new Div();

    addClassName("register-samples-dialog");
    batchNameField = new TextField("Batch name");
    batchNameField.setRequired(true);
    batchNameField.setPlaceholder("Please enter a name for your batch");

    Div downloadMetadataSection = setupDownloadMetadataSection(templateService, experimentId,
        projectId);

    validatedSampleMetadata = new ArrayList<>();
    UploadWithDisplay uploadWithDisplay = new UploadWithDisplay(MAX_FILE_SIZE, new FileType[]{
        new FileType(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    });
    uploadWithDisplay.addFailureListener(
        uploadFailed -> {/* display of the error is handled by the uploadWithDisplay component. So nothing to do here.*/});
    uploadWithDisplay.addSuccessListener(
        uploadSucceeded -> onUploadSucceeded(sampleValidationService, experimentId, projectId,
            uploadSucceeded));

    initialView.add(batchNameField, downloadMetadataSection, uploadWithDisplay);
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

    InProgressDisplay uploadProgressDisplay = new InProgressDisplay(
        component.getUploadedData().get().fileName());
    component.setDisplay(uploadProgressDisplay);

    List<SampleInformationForNewSample> sampleInformationForNewSamples = extractSampleInformationForNewSamples(
        uploadedData);

    List<CompletableFuture<ValidationResultWithPayload<SampleMetadata>>> validations = new ArrayList<>();
    for (SampleInformationForNewSample sampleInformationForNewSample : sampleInformationForNewSamples) {
      CompletableFuture<ValidationResultWithPayload<SampleMetadata>> validation = sampleValidationService.validateNewSampleAsync(
          sampleInformationForNewSample.sampleName(),
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
    CompletableFuture<Void> validationTasks = CompletableFuture
        //allOf makes sure exceptional state is transferred to outer completable future.
        .allOf(validations.toArray(new CompletableFuture[0]))
        .orTimeout(5, TimeUnit.MINUTES);

    validationTasks
        .thenAccept(ignored -> {
          if (validations.stream().anyMatch(not(CompletableFuture::isDone))) {
            throw new IllegalStateException(
                "validation task still in execution although expected to be done");
          }
          ValidationResultWithPayload<SampleMetadata> valueIfAbsent = new ValidationResultWithPayload<>(
              ValidationResult.withFailures(
                  List.of("Validation could not complete normally.")),
              new SampleMetadata("", "", null, "", "", "", 0, null, null, null,
                  "")); //not expected to occur

          List<ValidationResultWithPayload<SampleMetadata>> validationResults = validations.stream()
              .filter(result ->
                  result.isDone() && !result.isCancelled() && !result.isCompletedExceptionally())
              .map(future -> future.getNow(valueIfAbsent))
              .toList();
          List<ValidationResultWithPayload<SampleMetadata>> failedValidations = validationResults.stream()
              .filter(validation -> validation.validationResult().containsFailures())
              .toList();
          List<ValidationResultWithPayload<SampleMetadata>> succeededValidations = validationResults.stream()
              .filter(validation -> validation.validationResult().allPassed())
              .toList();

          if (!failedValidations.isEmpty()) {
            ui.access(() -> component.setDisplay(invalidDisplay(
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
        })
        .exceptionally(e -> {
              RuntimeException runtimeException = new RuntimeException(
                  "At least one validation task could not complete.", e);
              log.error("Could not complete validation. Please try again.", runtimeException);
              InvalidUploadDisplay invalidUploadDisplay = invalidDisplay(List.of(
                  ValidationResult.withFailures(
                      List.of("Could not complete validation. Please try again."))));
              ui.access(() -> component.setDisplay(invalidUploadDisplay));
              throw runtimeException;
            }
        );
  }

  private Div setupDownloadMetadataSection(TemplateService templateService, String experimentId,
      String projectId) {
    Button downloadTemplate = new Button("Download metadata template");
    downloadTemplate.addClassName("download-metadata-button");
    downloadTemplate.addClickListener(buttonClickEvent -> {
      try (XSSFWorkbook workbook = templateService.sampleBatchRegistrationXLSXTemplate(
          projectId,
          experimentId)) {
        var downloadProvider = new DownloadProvider(
            new XLSXDownloadContentProvider(projectId + "_registration_template.xlsx", workbook));
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
    Span sectionTitle = new Span("Download metadata template");
    sectionTitle.addClassName("download-metadata-section-title");
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
      List<ValidationResult> validationResults) {
    InvalidUploadDisplay invalidUploadDisplay = new InvalidUploadDisplay();
    List<String> failureReasons = validationResults.stream()
        .flatMap(res -> res.failures().stream()).toList();
    for (String failureReason : failureReasons) {
      invalidUploadDisplay.add(new Span(failureReason));
    }
    return invalidUploadDisplay;
  }

  @Override
  public void taskFailed(String label, String description) {
    failedView.removeAll();
    failedView.add(new Span("Sample registration failed."));
    failedView.setVisible(true);
    setConfirmButtonLabel("Register Again");
    showFailed();

    initialView.setVisible(false);
    inProgressView.setVisible(false);
    succeededView.setVisible(false);
  }

  @Override
  public void taskSucceeded(String label, String description) {
    succeededView.removeAll();
    succeededView.add(new Span("Successfully registered samples!"));
    succeededView.setVisible(true);
    showSucceeded();

    initialView.setVisible(false);
    inProgressView.setVisible(false);
    failedView.setVisible(false);
  }

  @Override
  public void taskInProgress(String label, String description) {
    inProgressView.removeAll();
    ProgressBar progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    inProgressView.add(new Span("Sample registration in progress"), progressBar);
    inProgressView.setVisible(true);
    showInProgress();

    initialView.setVisible(false);
    failedView.setVisible(false);
    succeededView.setVisible(false);
  }

  static class InvalidUploadDisplay extends Div {

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
      var approvedTitle = new Span("Approved");
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
      return;
    }
    if (batchNameField.isEmpty()) {
      // if the user never focused the name field, no validation took place. Thus, the need to double-check here.
      batchNameField.setInvalid(true);
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
