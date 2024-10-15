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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import life.qbic.datamanager.download.DownloadContentProvider.XLSXDownloadContentProvider;
import life.qbic.datamanager.download.DownloadProvider;
import life.qbic.datamanager.parser.ParsingResult;
import life.qbic.datamanager.parser.SampleInformationExtractor;
import life.qbic.datamanager.parser.SampleInformationExtractor.SampleInformationForExistingSample;
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
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * A dialog used for editing sample and batch information.
 *
 * @since 1.4.0
 */
public class EditSampleBatchDialog extends WizardDialogWindow {

  private static final Logger log = LoggerFactory.logger(EditSampleBatchDialog.class);

  private final List<SampleMetadata> validatedSampleMetadata;
  private final TextField batchNameField;
  private final Div initialView;
  private final Div inProgressView;
  private final Div failedView;
  private final Div succeededView;
  private static final int MAX_FILE_SIZE = 25 * 1024 * 1024;


  public EditSampleBatchDialog(SampleValidationService sampleValidationService,
      TemplateService templateService,
      BatchId batchId,
      String batchName,
      String experimentId,
      String projectId,
      String projectCode) {

    setHeaderTitle("Edit Sample Batch");
    setConfirmButtonLabel("Edit Batch");
    initialView = new Div();
    initialView.addClassName("initial-view");
    inProgressView = new Div();
    inProgressView.addClassName("in-progress-view");
    failedView = new Div();
    failedView.addClassName("failed-view");
    succeededView = new Div();
    succeededView.addClassName("succeeded-view");

    addClassName("edit-samples-dialog");
    batchNameField = new TextField("Batch name");
    batchNameField.setRequired(true);
    batchNameField.setValue(batchName);
    batchNameField.setPlaceholder("Please enter a name for your batch");
    batchNameField.addClassName("batch-name-field");

    Div downloadMetadataSection = setupDownloadMetadataSection(templateService, batchId,
        experimentId,
        projectId, projectCode);



    setHeaderTitle("Edit Sample Batch");
    validatedSampleMetadata = new ArrayList<>();

    UploadWithDisplay uploadWithDisplay = new UploadWithDisplay(
        MAX_FILE_SIZE, new FileType[]{
        new FileType(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    });
    uploadWithDisplay.addFailureListener(uploadFailed -> {
      /* display of the error is handled by the uploadWithDisplay component. So nothing to do here.*/
    });
    uploadWithDisplay.addSuccessListener(
        uploadSucceeded -> onUploadSucceeded(sampleValidationService, experimentId, projectId,
            uploadSucceeded)
    );
    uploadWithDisplay.addRemovedListener(it -> setValidatedSampleMetadata(List.of()));

    initialView.add(batchNameField, downloadMetadataSection, uploadWithDisplay);
    initialView.setVisible(true);
    inProgressView.setVisible(false);
    failedView.setVisible(false);
    succeededView.setVisible(false);
    add(initialView, inProgressView, failedView, succeededView);
  }

  static class InvalidUploadDisplay extends Div {

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
      for (int i = 1; i <= failureReasons.size(); i++) {
        String reason = failureReasons.get(i - 1);
        validationDetails.add(new Div(i + ". " + reason));
      }
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

  private static InvalidUploadDisplay invalidDisplay(String fileName,
      List<ValidationResult> validationResults) {
    List<String> failureReasons = validationResults.stream()
        .flatMap(res -> res.failures().stream()).toList();
    return new InvalidUploadDisplay(fileName, failureReasons);
  }


  private void onUploadSucceeded(SampleValidationService sampleValidationService,
      String experimentId, String projectId, SucceededEvent uploadSucceeded) {
    UploadWithDisplay component = uploadSucceeded.getSource();
    UI ui = component.getUI().orElseThrow();
    UploadedData uploadedData = component.getUploadedData().orElseThrow();

    InProgressDisplay uploadProgressDisplay = new InProgressDisplay(uploadedData.fileName());
    component.setDisplay(uploadProgressDisplay);

    List<SampleInformationForExistingSample> sampleInformationForExistingSamples = extractSampleInformationForExistingSamples(
        uploadedData);

    List<CompletableFuture<ValidationResultWithPayload<SampleMetadata>>> validations = new ArrayList<>();
    for (SampleInformationForExistingSample sampleInformationForExistingSample : sampleInformationForExistingSamples) {
      CompletableFuture<ValidationResultWithPayload<SampleMetadata>> validation = sampleValidationService.validateExistingSampleAsync(
          sampleInformationForExistingSample.sampleCode(),
          sampleInformationForExistingSample.sampleName(),
          sampleInformationForExistingSample.biologicalReplicate(),
          sampleInformationForExistingSample.condition(),
          sampleInformationForExistingSample.species(),
          sampleInformationForExistingSample.specimen(),
          sampleInformationForExistingSample.analyte(),
          sampleInformationForExistingSample.analysisMethod(),
          sampleInformationForExistingSample.comment(),
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
            ui.access(() -> component.setDisplay(invalidDisplay(uploadedData.fileName(),
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
          InvalidUploadDisplay invalidUploadDisplay = invalidDisplay(uploadedData.fileName(),
              List.of(
                  ValidationResult.withFailures(
                      List.of("Could not complete validation. Please try again."))));
              ui.access(() -> component.setDisplay(invalidUploadDisplay));
              throw runtimeException;
            }
        );
  }

  private void setValidatedSampleMetadata(List<SampleMetadata> metadata) {
    this.validatedSampleMetadata.clear();
    this.validatedSampleMetadata.addAll(metadata);
  }

  private List<SampleInformationForExistingSample> extractSampleInformationForExistingSamples(
      UploadedData uploadedData) {
    ParsingResult parsingResult = XLSXParser.create().parse(uploadedData.inputStream());
    return new SampleInformationExtractor()
        .extractInformationForExistingSamples(parsingResult);

  }

  private Div setupDownloadMetadataSection(TemplateService templateService,
      BatchId batchId,
      String experimentId,
      String projectId, String projectCode) {
    Button downloadTemplate = new Button("Download metadata template");
    downloadTemplate.addClassName("download-metadata-button");
    downloadTemplate.addClickListener(buttonClickEvent -> {
      try (XSSFWorkbook workbook = templateService.sampleBatchUpdateXLSXTemplate(
          batchId,
          projectId,
          experimentId)) {
        var downloadProvider = new DownloadProvider(
            new XLSXDownloadContentProvider(projectCode + "_edit_batch_template.xlsx", workbook));
        add(downloadProvider);
        downloadProvider.trigger();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    Div text = new Div();
    text.addClassName("download-metadata-text");
    text.setText(
        "Please download the metadata template, adapt the sample properties and upload the metadata sheet below to edit the sample batch.");
    Div downloadMetadataSection = new Div();
    downloadMetadataSection.addClassName("download-metadata");
    Span sectionTitle = new Span("Download metadata template");
    sectionTitle.addClassName("section-title");
    sectionTitle.addClassName("download-metadata-section-title");
    Div sectionContent = new Div();
    sectionContent.addClassName("download-metadata-section-content");
    sectionContent.add(text, downloadTemplate);
    downloadMetadataSection.add(sectionTitle, sectionContent);
    return downloadMetadataSection;
  }

  public Registration addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    return addListener(ConfirmEvent.class, listener);
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    return addListener(CancelEvent.class, listener);
  }

  @Override
  public void close() {
    validatedSampleMetadata.clear();
    super.close();
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
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), batchNameField.getValue(),
        Collections.unmodifiableList(validatedSampleMetadata)));

  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  @Override
  public void taskFailed(String label, String description) {
    failedView.removeAll();
    StepInformation top = new StepInformation(
        new Div("Edit the sample batch metadata"),
        new Div("It may take some time for the update to complete"),
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
    var bottom = new StepInformation(new Div("Sample batch editing failed."),
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
        new Div("Edit the sample batch metadata"),
        new Div("It may take some time for the update to complete"),
        false);

    Span successText = new Span("Sample batch updated successfully.");
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
        new Div("Edit the sample batch metadata"),
        new Div("It may take some time for the update to complete"),
        false);
    ProgressBar progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    StepInformation bottom = new StepInformation(new Div("Updating samples.."),
        progressBar, true);
    inProgressView.removeAll();
    inProgressView.add(top.asComponent(), bottom.asComponent());
    inProgressView.setVisible(true);
    showInProgress();

    initialView.setVisible(false);
    failedView.setVisible(false);
    succeededView.setVisible(false);
  }

  public static class ConfirmEvent extends ComponentEvent<EditSampleBatchDialog> {

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
    public ConfirmEvent(EditSampleBatchDialog source, boolean fromClient,
        String batchName,
        List<SampleMetadata> validatedSampleMetadata) {
      super(source, fromClient);
      this.batchName = batchName;
      this.validatedSampleMetadata = validatedSampleMetadata;
    }

    public List<SampleMetadata> validatedSampleMetadata() {
      return validatedSampleMetadata;
    }

    public String batchName() {
      return batchName;
    }
  }

  public static class CancelEvent extends ComponentEvent<EditSampleBatchDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(EditSampleBatchDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
