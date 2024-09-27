package life.qbic.datamanager.views.projects.project.measurements;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.FailedEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.FileRemovedEvent;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.shared.Registration;
import java.io.InputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import life.qbic.datamanager.parser.MeasurementMetadataConverter.MissingSampleIdException;
import life.qbic.datamanager.parser.MeasurementMetadataConverter.UnknownMetadataTypeException;
import life.qbic.datamanager.parser.MetadataConverter;
import life.qbic.datamanager.parser.ParsingResult;
import life.qbic.datamanager.parser.tsv.TSVParser;
import life.qbic.datamanager.parser.xlsx.XLSXParser;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.WizardDialogWindow;
import life.qbic.datamanager.views.general.upload.EditableMultiFileMemoryBuffer;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.util.StringUtils;


/**
 * <b>Upload Measurement Metadata Dialog</b>
 *
 * <p>Component that provides the user with a dialog to upload files to edit or add
 * {@link MeasurementMetadata}</p> to an {@link Experiment} dependent on the provided {@link MODE}
 * and {@link MeasurementValidationExecutor} with which it was initialized
 *
 * @since 1.0.0
 */
public class MeasurementMetadataUploadDialog extends WizardDialogWindow {

  public static final int MAX_FILE_SIZE_BYTES = (int) (Math.pow(1024, 2) * 16);
  @Serial
  private static final long serialVersionUID = -8253078073427291947L;
  private final MeasurementValidationService measurementValidationService;
  private final CancelConfirmationDialogFactory cancelConfirmationDialogFactory;

  private final EditableMultiFileMemoryBuffer uploadBuffer;
  private final transient List<MeasurementMetadataUpload<MeasurementMetadata>> measurementMetadataUploads;
  private final transient List<MeasurementFileItem> measurementFileItems;
  private final MODE mode;
  private final ProjectId projectId;
  private final UploadProgressDisplay uploadProgressDisplay;
  private final UploadItemsDisplay uploadItemsDisplay;

  private enum AcceptedFileType {
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    TSV("text/tab-separated-values", "text/plain");
    private final List<String> mimeTypes;

    AcceptedFileType(String... mimeTypes) {
      this.mimeTypes = List.of(mimeTypes);
    }

    static Optional<AcceptedFileType> forMimeType(String mimeType) {
      return Arrays.stream(values())
          .filter(it -> it.mimeTypes.contains(mimeType))
          .findFirst();
    }

    static Set<String> allMimeTypes() {
      return Arrays.stream(values()).flatMap(it -> it.mimeTypes.stream())
          .collect(Collectors.toUnmodifiableSet());
    }
  }

  public MeasurementMetadataUploadDialog(MeasurementValidationService measurementValidationService,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      MODE mode, ProjectId projectId) {
    this.cancelConfirmationDialogFactory = requireNonNull(cancelConfirmationDialogFactory);
    this.projectId = requireNonNull(projectId, "projectId cannot be null");
    this.measurementValidationService = requireNonNull(measurementValidationService,
        "measurementValidationExecutor must not be null");
    this.mode = requireNonNull(mode,
        "The dialog mode needs to be defined");

    this.uploadBuffer = new EditableMultiFileMemoryBuffer();
    this.measurementMetadataUploads = new ArrayList<>();
    this.measurementFileItems = new ArrayList<>();
    Upload upload = new Upload(uploadBuffer);
    upload.setAcceptedFileTypes(AcceptedFileType.allMimeTypes().toArray(String[]::new));
    upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);
    setModeBasedLabels();
    uploadItemsDisplay = new UploadItemsDisplay(upload);
    uploadItemsDisplay.toggleFileSectionIfEmpty(true);
    add(uploadItemsDisplay);
    uploadProgressDisplay = new UploadProgressDisplay(mode);
    add(uploadProgressDisplay);
    uploadProgressDisplay.setVisible(false);
    upload.addSucceededListener(this::onUploadSucceeded);
    upload.addFileRejectedListener(this::onFileRejected);
    upload.addFailedListener(this::onUploadFailed);
    upload.addFileRemovedListener(this::onFileRemoved);
    setEscAction(this::onCanceled);
    addClassName("measurement-upload-dialog");

  }

  private void setModeBasedLabels() {
    switch (mode) {
      case ADD -> {
        setHeaderTitle("Register measurements");
        confirmButton.setText("Register");
      }
      case EDIT -> {
        setHeaderTitle("Edit measurements");
        confirmButton.setText("Save");
      }
    }
  }

  /**
   * Returns the {@link MODE} with which this dialog was initialized
   */
  public MODE getMode() {
    return mode;
  }

  private void onFileRemoved(FileRemovedEvent fileRemovedEvent) {
    removeFile(fileRemovedEvent.getFileName());
  }

  private void showFile(MeasurementFileItem measurementFileItem) {
    MeasurementFileDisplay measurementFileDisplay = new MeasurementFileDisplay(measurementFileItem);
    uploadItemsDisplay.addFileToDisplay(measurementFileDisplay);
    //Todo Move logic to Display itself
    uploadItemsDisplay.toggleFileSectionIfEmpty(!measurementFileItems.isEmpty());
  }

  private void removeFile(String fileName) {
    uploadBuffer.remove(fileName);
    measurementMetadataUploads.removeIf(
        metadataUpload -> metadataUpload.fileName().equals(fileName));
    measurementFileItems.removeIf(
        measurementFileItem -> measurementFileItem.fileName().equals(fileName));
    uploadItemsDisplay.removeFileFromDisplay(fileName);
    uploadItemsDisplay.toggleFileSectionIfEmpty(!measurementFileItems.isEmpty());
  }

  private void onUploadFailed(FailedEvent failedEvent) {
    showErrorNotification("File upload was interrupted", failedEvent.getReason().getMessage());
  }

  private MeasurementValidationReport validate(List<? extends MeasurementMetadata> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return new MeasurementValidationReport(0,
          ValidationResult.withFailures(0, List.of("The metadata sheet seems to be empty")));
    }
    if (metadata.get(0) instanceof NGSMeasurementMetadata) {
      return validateNGS((List<NGSMeasurementMetadata>) metadata);
    }
    return validatePxP((List<ProteomicsMeasurementMetadata>) metadata);
  }

  private ParsingResult parseXLSX(InputStream inputStream) {
    return XLSXParser.create().parse(inputStream);
  }

  private ParsingResult parseTSV(InputStream inputStream) {
    return TSVParser.create().parse(inputStream);
  }

  private void onUploadSucceeded(SucceededEvent succeededEvent) {
    var fileName = succeededEvent.getFileName();
    Optional<AcceptedFileType> knownFileType = AcceptedFileType.forMimeType(
        succeededEvent.getMIMEType());
    if (knownFileType.isEmpty()) {
      displayError(succeededEvent.getFileName(),
          "Unsupported file type. Please make sure to upload a TSV or XLSX file.");
      return;
    }
    var parsingResult = switch (knownFileType.get()) {
      case EXCEL -> parseXLSX(uploadBuffer.inputStream(fileName).orElseThrow());
      case TSV -> parseTSV(uploadBuffer.inputStream(fileName).orElseThrow());
    };
    List<MeasurementMetadata> result;
    try {
      result = MetadataConverter.measurementConverter()
          .convert(parsingResult, mode.equals(MODE.ADD));
    } catch (
        UnknownMetadataTypeException e) { // we want to display this in the dialog, not via the notification system
      displayError(succeededEvent.getFileName(),
          "Unknown metadata file content. Please make sure to include all metadata properties, even the optional ones");
      return;
    } catch (MissingSampleIdException e) {
      displayError(succeededEvent.getFileName(), "Looks like at least one sample id is missing.");
      return;
    }

    var validationReport = validate(result);

    MeasurementFileItem measurementFileItem = new MeasurementFileItem(succeededEvent.getFileName(),
        validationReport);
    //We don't want to upload any invalid measurements in spreadsheet
    if (validationReport.validationResult.containsFailures()) {
      MeasurementMetadataUpload<MeasurementMetadata> metadataUpload = new MeasurementMetadataUpload<>(
          succeededEvent.getFileName(), Collections.emptyList());
      addFile(measurementFileItem, metadataUpload);
    } else {
      MeasurementMetadataUpload<MeasurementMetadata> metadataUpload = new MeasurementMetadataUpload(
          succeededEvent.getFileName(), result);
      addFile(measurementFileItem, metadataUpload);
    }
  }

  private void displayError(String fileName, String reason) {
    MeasurementMetadataUpload<MeasurementMetadata> metadataUpload = new MeasurementMetadataUpload<>(
        fileName, Collections.emptyList());
    MeasurementFileItem measurementFileItem = new MeasurementFileItem(
        fileName,
        new MeasurementValidationReport(1, ValidationResult.withFailures(1, List.of(
            reason))));
    addFile(measurementFileItem, metadataUpload);
  }

  private void addFile(MeasurementFileItem measurementFileItem,
      MeasurementMetadataUpload<MeasurementMetadata> metadataUpload) {
    measurementMetadataUploads.add(metadataUpload);
    measurementFileItems.add(measurementFileItem);
    showFile(measurementFileItem);
  }

  private MeasurementValidationReport validateNGS(List<NGSMeasurementMetadata> content) {
    var validationResult = ValidationResult.successful(0);
    ConcurrentLinkedDeque<ValidationResult> concurrentLinkedDeque = new ConcurrentLinkedDeque<>();
    List<CompletableFuture<Void>> tasks = new ArrayList<>();
    for (NGSMeasurementMetadata metaDatum : content) {
      tasks.add(validateNGSMetaDatum(metaDatum).thenAccept(concurrentLinkedDeque::add));
    }
    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

    return new MeasurementValidationReport(concurrentLinkedDeque.size(),
        concurrentLinkedDeque.stream().reduce(
            validationResult, ValidationResult::combine));
  }


  private MeasurementValidationReport validatePxP(List<ProteomicsMeasurementMetadata> content) {
    var validationResult = ValidationResult.successful(0);
    ConcurrentLinkedDeque<ValidationResult> concurrentLinkedDeque = new ConcurrentLinkedDeque<>();
    List<CompletableFuture<Void>> tasks = new ArrayList<>();
    for (ProteomicsMeasurementMetadata metaDatum : content) {
      tasks.add(validatePxpMetaDatum(metaDatum).thenAccept(concurrentLinkedDeque::add));
    }
    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

    return new MeasurementValidationReport(concurrentLinkedDeque.size(),
        concurrentLinkedDeque.stream().reduce(
            validationResult, ValidationResult::combine));
  }

  private CompletableFuture<ValidationResult> validateNGSMetaDatum(
      NGSMeasurementMetadata metaDatum) {
    var measurementNGSValidationExecutor = new MeasurementNGSValidationExecutor(
        measurementValidationService);
    return generateModeDependentValidationResult(
        measurementNGSValidationExecutor, metaDatum);
  }

  private CompletableFuture<ValidationResult> validatePxpMetaDatum(
      ProteomicsMeasurementMetadata metaDatum) {
    MeasurementValidationExecutor<ProteomicsMeasurementMetadata> proteomicsValidationExecutor = new MeasurementProteomicsValidationExecutor(
        measurementValidationService);
    return generateModeDependentValidationResult(
        proteomicsValidationExecutor, metaDatum);
  }

  private CompletableFuture<ValidationResult> generateModeDependentValidationResult(
      MeasurementValidationExecutor measurementValidationExecutor, MeasurementMetadata metadata) {
    return switch (mode) {
      case ADD -> measurementValidationExecutor.validateRegistration(metadata, projectId);
      case EDIT -> measurementValidationExecutor.validateUpdate(metadata, projectId);
    };
  }

  private void onFileRejected(FileRejectedEvent fileRejectedEvent) {
    //Todo Replace with error message below file if possible as outlined in https://vaadin.com/docs/latest/components/upload#best-practices
    // requires a fully setup I18n instance
    String errorMessage = fileRejectedEvent.getErrorMessage();
    showErrorNotification("File upload failed", errorMessage);
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    return addListener(CancelEvent.class, listener);
  }

  public Registration addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    return addListener(ConfirmEvent.class, listener);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    if (containsInvalidMeasurementData()) {
      showErrorNotification("Metadata still invalid",
          "Please correct your metadata first and upload it again.");
      return;
    }
    uploadItemsDisplay.setVisible(false);
    uploadProgressDisplay.setVisible(true);
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), measurementMetadataUploads));
  }

  private boolean containsInvalidMeasurementData() {
    return measurementFileItems.stream()
        .map(MeasurementFileItem::measurementValidationReport)
        .map(MeasurementValidationReport::validationResult)
        .anyMatch(ValidationResult::containsFailures);
  }


  private void showErrorNotification(String title, String description) {
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  private void onCanceled() {
    cancelConfirmationDialogFactory.cancelConfirmationDialog(
            it -> fireEvent(new CancelEvent(this, it.isFromClient())),
            "measurement.metadata.upload", getLocale())
        .open();
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    onCanceled();
  }

  @Override
  public void taskFailed(String label, String description) {
    uploadProgressDisplay.showProgressFailedDisplay(label, description);
    setConfirmButtonLabel("%s Again".formatted(mode == MODE.ADD ? "Register" : "Edit"));
    showFailed();
  }

  @Override
  public void taskSucceeded(String label, String description) {
    uploadProgressDisplay.showProgressSucceededDisplay(label, description);
    showSucceeded();
  }

  @Override
  public void taskInProgress(String label, String description) {
    uploadProgressDisplay.showInProgressDisplay(label, description);
    showInProgress();
  }

  public enum MODE {
    ADD, EDIT
  }

  record MeasurementValidationReport(int validatedRows,
                                     ValidationResult validationResult) {

  }

  private record MetadataContent(String header, List<String> rows) {

    Optional<String> theHeader() {
      return Optional.ofNullable(header);
    }
  }

  public record MeasurementMetadataUpload<T extends MeasurementMetadata>(String fileName,
                                                                         List<MeasurementMetadata> measurementMetadata) {

  }

  public record MeasurementFileItem(String fileName,
                                    MeasurementValidationReport measurementValidationReport) {

  }

  /**
   * used to display an uploaded measurement file with validation information
   */
  public static class MeasurementFileDisplay extends Div {

    @Serial
    private static final long serialVersionUID = -9075627206992036067L;
    private final transient MeasurementFileItem measurementFileItem;
    private final Div displayBox = new Div();

    public MeasurementFileDisplay(MeasurementFileItem measurementFileItem) {
      this.measurementFileItem = requireNonNull(measurementFileItem,
          "measurementFileItem must not be null");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(this.measurementFileItem.fileName()));
      fileNameLabel.addClassName("file-name");
      add(fileNameLabel);
      createDisplayBox(measurementFileItem.measurementValidationReport());
      displayBox.addClassName("validation-display-box");
      add(displayBox);
      addClassName("measurement-item");
    }

    public MeasurementFileItem measurementFileItem() {
      return measurementFileItem;
    }

    private void createDisplayBox(MeasurementValidationReport measurementValidationReport) {
      displayBox.removeAll();
      if (measurementValidationReport.validationResult().allPassed()) {
        displayBox.add(createApprovedDisplayBox(measurementValidationReport.validatedRows()));
      } else {
        displayBox.add(createInvalidDisplayBox(
            measurementValidationReport.validationResult().failures()));
      }
    }

    private Div createApprovedDisplayBox(int validMeasurementCount) {
      Div box = new Div();
      Span approvedTitle = new Span("Your data has been approved");
      Icon validIcon = VaadinIcon.CHECK_CIRCLE_O.create();
      validIcon.addClassName("success");
      Span header = new Span(validIcon, approvedTitle);
      header.addClassName("header");
      box.add(header);
      Span instruction = new Span("Please click on Register to record the sample measurement data");
      instruction.addClassName("secondary");
      Div validationDetails = new Div();
      Span approvedMeasurements = new Span(String.format("%s measurements", validMeasurementCount));
      approvedMeasurements.addClassName("bold");
      validationDetails.add(new Span("Measurement data for "), approvedMeasurements,
          new Span(" is now ready to be registered"));
      box.add(header, validationDetails, instruction);
      return box;
    }

    private Div createInvalidDisplayBox(Collection<String> invalidMeasurements) {
      Div box = new Div();
      Span approvedTitle = new Span("Invalid measurement data");
      Icon invalidIcon = VaadinIcon.CLOSE_CIRCLE_O.create();
      invalidIcon.addClassName("error");
      Span header = new Span(invalidIcon, approvedTitle);
      header.addClassName("header");
      box.add(header);
      Span instruction = new Span("Please correct the entries and re-upload the excel sheet");
      instruction.addClassName("secondary");
      Div validationDetails = new Div();
      OrderedList invalidMeasurementsList = new OrderedList(
          invalidMeasurements.stream().map(ListItem::new).toArray(ListItem[]::new));
      invalidMeasurementsList.addClassName("invalid-measurement-list");
      invalidMeasurementsList.setType(OrderedList.NumberingType.NUMBER);
      validationDetails.add(invalidMeasurementsList);
      box.add(header, validationDetails, instruction);
      return box;
    }

  }

  public static class ConfirmEvent extends ComponentEvent<MeasurementMetadataUploadDialog> {

    private final transient List<MeasurementMetadataUpload<MeasurementMetadata>> uploads;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     * @param uploads    the valid {@link MeasurementMetadataUpload}s to be registered
     */
    public ConfirmEvent(MeasurementMetadataUploadDialog source, boolean fromClient,
        List<MeasurementMetadataUpload<MeasurementMetadata>> uploads) {
      super(source, fromClient);
      requireNonNull(uploads, "uploads must not be null");
      this.uploads = uploads;
    }

    public List<MeasurementMetadataUpload<MeasurementMetadata>> uploads() {
      return new ArrayList<>(uploads);
    }
  }

  public static class CancelEvent extends ComponentEvent<MeasurementMetadataUploadDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(MeasurementMetadataUploadDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  private static class UploadItemsDisplay extends Div {

    private final Div uploadSection;
    private final Div uploadedItemsSection;
    private final Div uploadedItemsDisplays;

    public UploadItemsDisplay(Upload upload) {
      var uploadSectionTitle = new Span("Upload the measurement data");
      uploadSectionTitle.addClassName("section-title");

      var saveYourFileInfo = new InfoBox().setInfoText(
              "When uploading a tab-separated file, please save your Excel file as UTF-16 Unicode Text (*.txt) before uploading.")
          .setClosable(false);

      var restrictions = new Div();
      restrictions.addClassName("restrictions");
      restrictions.add(new Span("Supported file formats: .txt, .tsv, .xlsx"));
      restrictions.add(
          "Maximum file size: %s MB".formatted(MAX_FILE_SIZE_BYTES / Math.pow(1024, 2)));

      this.uploadSection = new Div();
      uploadSection.add(uploadSectionTitle, saveYourFileInfo, upload, restrictions);
      uploadSection.addClassName("upload-section");

      uploadedItemsSection = new Div();
      uploadedItemsSection.addClassName("uploaded-items-section");

      var uploadedItemsSectionTitle = new Span("Uploaded files");
      uploadedItemsSectionTitle.addClassName("section-title");

      uploadedItemsDisplays = new Div();
      uploadedItemsDisplays.addClassName("uploaded-measurement-items");
      uploadedItemsSection.add(uploadedItemsSectionTitle, uploadedItemsDisplays);
      add(uploadSection, uploadedItemsSection);
      addClassName("upload-items-display");
    }

    private void addFileToDisplay(MeasurementFileDisplay measurementFileDisplay) {
      uploadedItemsDisplays.add(measurementFileDisplay);
    }

    private void removeFileFromDisplay(String fileName) {
      MeasurementFileDisplay[] fileDisplays = fileDisplaysWithFileName(fileName);
      uploadedItemsDisplays.remove(fileDisplays);
    }

    private MeasurementFileDisplay[] fileDisplaysWithFileName(String fileName) {
      return uploadedItemsDisplays.getChildren()
          .filter(MeasurementFileDisplay.class::isInstance)
          .map(MeasurementFileDisplay.class::cast)
          .filter(measurementFileDisplay -> measurementFileDisplay.measurementFileItem().fileName()
              .equals(fileName))
          .toArray(MeasurementFileDisplay[]::new);
    }

    private void toggleFileSectionIfEmpty(boolean isEmpty) {
      uploadedItemsSection.setVisible(isEmpty);
    }
  }

  private static class UploadProgressDisplay extends Div {

    private final Div processInProgressDisplay;
    private final Div processFailureDisplay;
    private final Div processSucceededDisplay;

    public UploadProgressDisplay(MODE mode) {

      requireNonNull(mode, "Mode cannot be null");
      String modeBasedTask = (mode == MODE.ADD ? "register" : "update");
      Span title = new Span(
          String.format("%s" + " the measurement data", StringUtils.capitalize(modeBasedTask)));
      title.addClassNames("bold", "secondary");
      Span description = new Span(
          String.format("It may take about a minute for the %s process to complete",
              modeBasedTask));
      description.addClassName("secondary");
      add(title, description);
      this.processSucceededDisplay = new Div();
      processSucceededDisplay.setClassName("display-box");
      this.processInProgressDisplay = new Div();
      processInProgressDisplay.setClassName("display-box");
      this.processFailureDisplay = new Div();
      this.processFailureDisplay.setClassName("display-box");
      add(processSucceededDisplay, processInProgressDisplay, processFailureDisplay);
      processInProgressDisplay.setVisible(false);
      processFailureDisplay.setVisible(false);
      processSucceededDisplay.setVisible(false);
      addClassName("upload-progress-display");
    }

    private void createProgressSuccessDisplay(String label, String description) {
      Span processSucceededTitle = new Span(label);
      processSucceededTitle.addClassName("bold");
      processSucceededDisplay.add(processSucceededTitle);
      Icon successIcon = VaadinIcon.CHECK_CIRCLE_O.create();
      successIcon.addClassNames("success", "small");
      Span processSucceededDescription = new Span(successIcon, new Span(description));
      processSucceededDescription.addClassName("description");
      processSucceededDisplay.add(processSucceededDescription);
    }

    private void createProcessFailureDisplay(String label, String description) {
      Span processFailureTitle = new Span(label);
      processFailureTitle.addClassName("bold");
      Icon errorIcon = new Icon(VaadinIcon.CLOSE_CIRCLE);
      errorIcon.addClassNames("error", "small");
      Span descriptionText = new Span(description);
      descriptionText.addClassName("error-text");
      Span processFailureDescription = new Span(errorIcon, descriptionText);
      processFailureDescription.addClassNames("description");
      processFailureDisplay.add(processFailureTitle, processFailureDescription);
    }

    private void createProcessInProgressDisplay(String label, String description) {
      processInProgressDisplay.removeAll();
      Span processInProgressTitle = new Span(label);
      processInProgressTitle.addClassNames("bold");
      Span processInProgressDescription = new Span(description);
      processInProgressDescription.addClassNames("secondary");
      ProgressBar progressBar = new ProgressBar();
      progressBar.setIndeterminate(true);
      processInProgressDisplay.add(processInProgressTitle, progressBar,
          processInProgressDescription);
    }

    public void showInProgressDisplay(String label, String description) {
      processInProgressDisplay.removeAll();
      createProcessInProgressDisplay(label, description);
      processFailureDisplay.setVisible(false);
      processSucceededDisplay.setVisible(false);
      processInProgressDisplay.setVisible(true);
    }

    public void showProgressSucceededDisplay(String label, String description) {
      processSucceededDisplay.removeAll();
      createProgressSuccessDisplay(label, description);
      processInProgressDisplay.setVisible(false);
      processFailureDisplay.setVisible(false);
      processSucceededDisplay.setVisible(true);

    }

    public void showProgressFailedDisplay(String label, String description) {
      processFailureDisplay.removeAll();
      createProcessFailureDisplay(label, description);
      processSucceededDisplay.setVisible(false);
      processInProgressDisplay.setVisible(false);
      processFailureDisplay.setVisible(true);
    }
  }
}
