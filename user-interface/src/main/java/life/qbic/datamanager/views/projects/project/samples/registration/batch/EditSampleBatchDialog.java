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
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.download.DownloadContentProvider.XLSXDownloadContentProvider;
import life.qbic.datamanager.download.DownloadProvider;
import life.qbic.datamanager.files.export.FileNameFormatter;
import life.qbic.datamanager.files.export.sample.TemplateService;
import life.qbic.datamanager.files.parsing.MetadataParser.ParsingException;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor.SampleInformationForExistingSample;
import life.qbic.datamanager.files.parsing.xlsx.XLSXParser;
import life.qbic.datamanager.views.general.WizardDialogWindow;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.FileType;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.SucceededEvent;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.UploadedData;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.ValidationResultWithPayload;
import life.qbic.projectmanagement.application.api.SampleMetadata;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * A dialog used for editing sample and batch information.
 *
 * @since 1.4.0
 */
public class EditSampleBatchDialog extends WizardDialogWindow {

  private static final String YOUR_DATA_HAS_BEEN_APPROVED_TEXT = "Your data has been approved";
  private static final String ERROR_CSS = "error";
  private static final String EDIT_THE_SAMPLE_BATCH_METADATA_TEXT = "Edit the sample batch metadata";
  private static final String PENDING_OPERATION_NOTE = "It may take some time for the update to complete";
  private static final String UPLOADED_ITEM_CSS = "uploaded-item";
  private static final String FILE_ICON_CSS = "file-icon";
  private static final String FILE_NAME_CSS = "file-name";
  private static final String VALIDATION_DISPLAY_BOX_CSS = "validation-display-box";
  private static final String HEADER_CSS = "header";
  private static final String SECONDARY_CSS = "secondary";
  private static final Logger log = LoggerFactory.logger(EditSampleBatchDialog.class);
  private static final int MAX_FILE_SIZE = 25 * 1024 * 1024;
  private final transient List<SampleMetadata> validatedSampleMetadata;
  private final TextField batchNameField;
  private final Div initialView;
  private final Div inProgressView;
  private final Div failedView;
  private final Div succeededView;

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
    batchNameField.setErrorMessage("Please provide a name for your batch.");
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

    List<SampleInformationForExistingSample> sampleInformationForExistingSamples;
    try {
      sampleInformationForExistingSamples = extractSampleInformationForExistingSamples(
          uploadedData);
    } catch (ParsingException e) {
      RuntimeException runtimeException = new RuntimeException(
          "Parsing failed.", e);
      log.error("Could not complete validation. " + e.getMessage(), runtimeException);
      InvalidUploadDisplay invalidUploadDisplay = new InvalidUploadDisplay(
          uploadedData.fileName(),
          "Could not complete validation. " + e.getMessage());
      ui.access(() -> component.setDisplay(invalidUploadDisplay));
      throw runtimeException;
    }

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
          sampleInformationForExistingSample.confoundingVariables(),
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
              InvalidUploadDisplay invalidUploadDisplay = new InvalidUploadDisplay(
                  uploadedData.fileName(),
                  "Could not complete validation. Please try again.");
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
      try (Workbook workbook = templateService.sampleBatchUpdateXLSXTemplate(
          batchId,
          projectId,
          experimentId)) {
        var filename = FileNameFormatter.formatWithVersion(
            projectCode + "_sample metadata edit template", 1,
            "xlsx");
        var downloadProvider = new DownloadProvider(
            new XLSXDownloadContentProvider(filename, workbook));
        add(downloadProvider);
        downloadProvider.trigger();
      } catch (IOException e) {
        log.error("Writing the batch template failed.", e);
        throw new ApplicationException("Creating the template resource failed.");
      }
    });
    Div text = new Div();
    text.addClassName("download-metadata-text");
    text.setText(
        "Please download the metadata template, adapt the sample properties and upload the metadata sheet below to edit the sample batch.");
    Div downloadMetadataSection = new Div();
    downloadMetadataSection.addClassName("download-metadata");
    downloadMetadataSection.addClassName("section-with-title");
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
      batchNameField.focus();
      return;
    }
    if (batchNameField.isEmpty()) {
      // if the user never focused the name field, no validation took place. Thus, the need to double-check here.
      batchNameField.setInvalid(true);
      batchNameField.focus();
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
        new Div(EDIT_THE_SAMPLE_BATCH_METADATA_TEXT),
        new Div(PENDING_OPERATION_NOTE),
        false);

    Span errorText = new Span("There was an error registering the sample data. Please try again.");
    errorText.addClassName("error-text");
    Icon icon = VaadinIcon.CLOSE_CIRCLE.create();
    icon.addClassName(ERROR_CSS);
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
        new Div(EDIT_THE_SAMPLE_BATCH_METADATA_TEXT),
        new Div(PENDING_OPERATION_NOTE),
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
        new Div(EDIT_THE_SAMPLE_BATCH_METADATA_TEXT),
        new Div(PENDING_OPERATION_NOTE),
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

  static class InvalidUploadDisplay extends Div {


    public InvalidUploadDisplay(String fileName, List<String> failureReasons) {
      addClassName(UPLOADED_ITEM_CSS);
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName(FILE_ICON_CSS);
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName(FILE_NAME_CSS);
      Div validationBox = new Div();
      validationBox.addClassName(VALIDATION_DISPLAY_BOX_CSS);
      var box = new Div();
      var failuresTitle = new Span("Invalid sample metadata");
      var errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
      errorIcon.addClassName(ERROR_CSS);
      var header = new Span(errorIcon, failuresTitle);
      header.addClassName(HEADER_CSS);
      var instruction = new Span(
          "Please correct the entries in the uploaded file and re-upload the file.");
      instruction.addClassName(SECONDARY_CSS);
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
      addClassName(UPLOADED_ITEM_CSS);
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName(FILE_ICON_CSS);
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName(FILE_NAME_CSS);
      Div validationBox = new Div();
      validationBox.addClassName(VALIDATION_DISPLAY_BOX_CSS);
      var box = new Div();
      var failuresTitle = new Span("Invalid sample metadata");
      var errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
      errorIcon.addClassName(ERROR_CSS);
      var header = new Span(errorIcon, failuresTitle);
      header.addClassName(HEADER_CSS);
      var instruction = new Span(
          "Please correct the entries in the uploaded file and re-upload the file.");
      instruction.addClassName(SECONDARY_CSS);
      Div validationDetails = new Div();

      validationDetails.add(new Div(failureReason));
      box.add(header, validationDetails, instruction);
      validationBox.add(box);
      add(fileNameLabel, validationBox);
    }
  }

  static class ValidUploadDisplay extends Div {

    private ValidUploadDisplay(String fileName, int count) {
      addClassName(UPLOADED_ITEM_CSS);
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName(FILE_ICON_CSS);
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName(FILE_NAME_CSS);
      Div validationBox = new Div();
      validationBox.addClassName(VALIDATION_DISPLAY_BOX_CSS);
      var box = new Div();
      var approvedTitle = new Span(YOUR_DATA_HAS_BEEN_APPROVED_TEXT);
      var validIcon = VaadinIcon.CHECK_CIRCLE_O.create();
      validIcon.addClassName("success");
      var header = new Span(validIcon, approvedTitle);
      header.addClassName(HEADER_CSS);
      var instruction = new Span("Please click Register to register your samples");
      instruction.addClassName(SECONDARY_CSS);
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
      addClassName(UPLOADED_ITEM_CSS);
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName(FILE_ICON_CSS);
      Span fileNameLabel = new Span(fileIcon, new Span(fileName));
      fileNameLabel.addClassName(FILE_NAME_CSS);
      ProgressBar progressBar = new ProgressBar();
      progressBar.setIndeterminate(true);
      add(fileNameLabel, new Div("Validating file..."), progressBar);
    }
  }

  public static class ConfirmEvent extends ComponentEvent<EditSampleBatchDialog> {

    private final String batchName;
    private final transient List<SampleMetadata> validatedSampleMetadata;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source                  the source component
     * @param fromClient              <code>true</code> if the event originated from the client
     *                                side, <code>false</code> otherwise
     * @param batchName               the name of the batch
     * @param validatedSampleMetadata a list of validated sample metadata
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
