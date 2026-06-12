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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.files.export.download.ByteArrayDownloadStreamProvider;
import life.qbic.datamanager.files.parsing.MetadataParser.ParsingException;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor;
import life.qbic.datamanager.files.parsing.SampleInformationExtractor.SampleInformationForExistingSample;
import life.qbic.datamanager.files.parsing.xlsx.XLSXParser;
import life.qbic.datamanager.views.general.WizardDialogWindow;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.FileType;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.SucceededEvent;
import life.qbic.datamanager.views.general.upload.UploadWithDisplay.UploadedData;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleUploadDisplay.InProgressDisplay;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleUploadDisplay.InvalidUploadDisplay;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.ValidationResultWithPayload;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.AccessDeniedException;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.sample.SampleMetadata;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import org.springframework.util.MimeType;

/**
 * A dialog used for editing sample and batch information.
 *
 * @since 1.4.0
 */
public class EditSampleBatchDialog extends WizardDialogWindow {

  private static final MimeType OPEN_XML = MimeType.valueOf(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
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
  private final MessageSourceNotificationFactory messageFactory;
  private final DownloadComponent downloadComponent;
  private final ContentUploadComponent contentUploadComponent;


  public EditSampleBatchDialog(SampleValidationService sampleValidationService,
      AsyncProjectService service, MessageSourceNotificationFactory messageFactory,
      BatchId batchId,
      String batchName,
      String experimentId,
      String projectId,
      String projectCode, UploadConfiguration uploadConfiguration) {
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.downloadComponent = new DownloadComponent();
    this.contentUploadComponent = new ContentUploadComponent(uploadConfiguration);

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

    Div downloadMetadataSection = setupDownloadMetadataSection(service, batchId.value(),
        experimentId,
        projectId, projectCode);

    setHeaderTitle("Edit Sample Batch");
    validatedSampleMetadata = new ArrayList<>();

    contentUploadComponent.setMaxFiles(1);
    contentUploadComponent.setAcceptedFileTypes(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    contentUploadComponent.addUnspecificFailureListener(
        uploadFailed ->
            /* display of the error is handled by the uploadWithDisplay component. However, we do need to log with the context*/
            log.error(
                "Upload failed for project(" + projectId + ") experiment(" + experimentId + ")",
                uploadFailed.getCause()));

    UploadWithDisplay uploadWithDisplay = new UploadWithDisplay(
        MAX_FILE_SIZE, new FileType[]{
        new FileType(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    });
    uploadWithDisplay.addUnspecificFailureListener(
        uploadFailed ->
            /* display of the error is handled by the uploadWithDisplay component. However we do need to log with the context*/
            log.error(
                "Upload failed for project(" + projectId + ") experiment(" + experimentId + ")",
                uploadFailed.getCause()));
    uploadWithDisplay.addSuccessListener(
        uploadSucceeded -> onUploadSucceeded(sampleValidationService, experimentId, projectId,
            uploadSucceeded)
    );
    uploadWithDisplay.addRemovedListener(it -> setValidatedSampleMetadata(List.of()));

    Span uploadTheSampleDataTitle = new Span("Upload the sample data");
    uploadTheSampleDataTitle.addClassName("section-title");
    Div uploadSection = new Div(uploadTheSampleDataTitle, uploadWithDisplay,
        contentUploadComponent);
    uploadSection.addClassName("upload-section");
    uploadSection.addClassName("section-with-title");
    initialView.add(batchNameField, downloadMetadataSection, uploadSection);
    initialView.setVisible(true);
    inProgressView.setVisible(false);
    failedView.setVisible(false);
    succeededView.setVisible(false);
    add(initialView, inProgressView, failedView, succeededView, downloadComponent);
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
                .setDisplay(new SampleUploadDisplay.ValidUploadDisplay(uploadedData.fileName(),
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


  private Div setupDownloadMetadataSection(AsyncProjectService service,
      String batchId,
      String experimentId,
      String projectId, String projectCode) {
    Button downloadTemplate = new Button("Download metadata template");
    downloadTemplate.addClassName("download-metadata-button");
    downloadTemplate.addClickListener(
        buttonClickEvent -> service.sampleUpdateTemplate(projectId, experimentId, batchId,
            OPEN_XML).doOnSuccess(resource ->
            triggerDownload(resource,
                FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(), projectCode,
                    "sample metadata update template",
                    "xlsx")
            )).doOnError(this::handleError).subscribe());
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


  class SampleEditUploadDisplayController {

    private final String projectId;
    private final String experimentId;

    private SampleEditUploadDisplayController(String projectId, String experimentId) {
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
          //TODO
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
