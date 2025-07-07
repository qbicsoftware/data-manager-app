package life.qbic.datamanager.views.projects.project.measurements.registration;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.upload.FailedEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.FileRemovedEvent;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import java.io.InputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.converters.MetadataConverterV2;
import life.qbic.datamanager.files.parsing.tsv.TSVParser;
import life.qbic.datamanager.files.parsing.xlsx.XLSXParser;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.upload.EditableMultiFileMemoryBuffer;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.measurements.processor.ProcessorRegistry;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationResponse;
import reactor.core.publisher.Flux;

/**
 * <b>MeasurementUpload</b>
 * <p>
 * A data manager component that enables users to upload measurement files to register or update
 * measurement metadata.
 * <p>
 * The component is created with a certain type of {@link MetadataConverterV2}, such that the
 * content gets parsed and validated immediately after the upload.
 *
 * @since 1.11.0
 */
public class MeasurementUpload extends Div implements UserInput {

  public static final int MAX_FILE_SIZE_BYTES = (int) (Math.pow(1024, 2) * 16);
  private final UploadedItemsDisplay uploadedItemsDisplay;
  private final AsyncProjectService service;
  private final Context context;
  private final Div validationProgress;
  private MetadataConverterV2<? extends ValidationRequestBody> converter;

  private final Map<String, List<? extends ValidationRequestBody>> validationRequestsPerFile = new HashMap<>();
  private final MessageSourceNotificationFactory notificationFactory;

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

  private final EditableMultiFileMemoryBuffer uploadBuffer;
  private final List<MeasurementFileItem> measurementFileItems;

  public MeasurementUpload(
      AsyncProjectService service,
      Context context,
      MetadataConverterV2<? extends ValidationRequestBody> converter,
      MessageSourceNotificationFactory notificationFactory) {
    // Initial parameter validation of injected objects
    this.service = requireNonNull(service);
    this.context = requireNonNull(context);
    this.converter = requireNonNull(converter);
    this.notificationFactory = requireNonNull(notificationFactory);
    // Initial parameter instantiation of local objects
    this.measurementFileItems = new ArrayList<>();
    this.uploadBuffer = new EditableMultiFileMemoryBuffer();
    this.validationProgress = new Div();

    // Set up the upload section
    var upload = new Upload(uploadBuffer);
    upload.setAcceptedFileTypes(AcceptedFileType.allMimeTypes().toArray(String[]::new));
    upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);
    upload.addSucceededListener(this::onUploadSucceeded);
    upload.addFileRejectedListener(this::onFileRejected);
    upload.addFailedListener(this::onUploadFailed);
    upload.addFileRemovedListener(this::onFileRemoved);
    this.uploadedItemsDisplay = new UploadedItemsDisplay(upload);

    // Create the different sections
    var sectionUpload = DialogSection.with("Upload filled template",
        uploadedItemsDisplay);

    // Add components to the MeasurementUpload component
    add(sectionUpload);
    add(validationProgress);

    // Apply layout styles
    addClassNames("flex-vertical", "gap-06");

    // Trigger display refresh, to ensure the upload item display is only shown
    // if uploaded measurement files are present
    refresh();
  }

  public List<? extends ValidationRequestBody> getValidationRequestContent() {
    return validationRequestsPerFile.values().stream().flatMap(List::stream).toList();
  }

  public void setMetadataConverter(MetadataConverterV2<? extends ValidationRequestBody> converter) {
    this.converter = requireNonNull(converter);
  }

  /**
   * Toggles the visibility of the upload items display, depending on present measurement file
   * items. The uploaded items display can be hidden in case there is no uploaded measurement file
   * present.
   */
  private void refresh() {
    if (measurementFileItems.isEmpty()) {
      uploadedItemsDisplay.hide();
      return;
    }
    uploadedItemsDisplay.show();
  }

  private void removeFile(String fileName) {
    uploadBuffer.remove(fileName);
    measurementFileItems.removeIf(
        measurementFileItem -> measurementFileItem.fileName().equals(fileName));
    uploadedItemsDisplay.removeFileFromDisplay(fileName);
    validationRequestsPerFile.remove(fileName);
    refresh();
  }

  private void onFileRemoved(FileRemovedEvent fileRemovedEvent) {
    removeFile(fileRemovedEvent.getFileName());
  }

  private void onUploadFailed(FailedEvent failedEvent) {
    displayError();
  }

  private void displayUnsupportedFileType() {
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast("measurement.upload.failed.unsupported-file-type",
          new Object[]{}, getLocale());
      toast.open();
    }));
  }

  private void displayError() {
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast("measurement.upload.failed", new Object[]{},
          getLocale());
      toast.open();
    }));
  }

  private void onUploadSucceeded(SucceededEvent succeededEvent) {
    var fileName = succeededEvent.getFileName();
    Optional<AcceptedFileType> knownFileType = AcceptedFileType.forMimeType(
        succeededEvent.getMIMEType());
    if (knownFileType.isEmpty()) {
      displayUnsupportedFileType();
      return;
    }

    var parsingResult = switch (knownFileType.get()) {
      case EXCEL -> parseXLSX(uploadBuffer.inputStream(fileName).orElseThrow());
      case TSV -> parseTSV(uploadBuffer.inputStream(fileName).orElseThrow());
    };

    var result = converter.convert(parsingResult);
    var processedResult = process(result);

    var itemsToValidate = result.size();
    var requests = new ArrayList<ValidationRequest>();

    validationRequestsPerFile.put(fileName, result);

    for (var registration : processedResult) {
      var request = new ValidationRequest(context.projectId().orElseThrow().value(),
          context.experimentId().orElseThrow().value(), registration);
      requests.add(request);
    }
    runValidation(requests, itemsToValidate, fileName);
  }

  private static List<? extends ValidationRequestBody> process(
      List<? extends ValidationRequestBody> validationRequest) {
    if (validationRequest.isEmpty()) {
      return validationRequest;
    }
    var firstEntry = Objects.requireNonNull(validationRequest.get(0));
    switch (firstEntry) {
      case MeasurementRegistrationInformationNGS ignored: {
        var processor = ProcessorRegistry.processorFor(MeasurementRegistrationInformationNGS.class);
        return processor.process((List<MeasurementRegistrationInformationNGS>) validationRequest);
      }
      case MeasurementRegistrationInformationPxP ignored: {
        var processor = ProcessorRegistry.processorFor(MeasurementRegistrationInformationPxP.class);
        return processor.process((List<MeasurementRegistrationInformationPxP>) validationRequest);
      }
      default:
        throw new IllegalStateException("Unknown validation request: " + validationRequest);
    }
  }

  private void runValidation(ArrayList<ValidationRequest> requests, int itemsToValidate,
      String fileName) {
    AtomicInteger counter = new AtomicInteger();
    counter.set(1);

    List<ValidationResponse> responses = new ArrayList<>();
    service.validate(Flux.fromIterable(requests))
        .doFirst(() -> {
          showValidationProgress();
          setValidationProgressText("Starting validation ...");
        })
        .doOnNext(item -> setValidationProgressText(
            "Processed " + counter.getAndIncrement() + " requests from " + itemsToValidate))
        .doOnNext(responses::add)
        .doOnComplete(() -> {
          hideValidationProgress();
          displayValidationResults(responses, itemsToValidate, fileName);
        })
        .subscribe();
  }

  private void hideValidationProgress() {
    getUI().ifPresent(ui -> ui.access(() -> validationProgress.setVisible(false)));
  }

  private void showValidationProgress() {
    getUI().ifPresent(ui -> ui.access(() -> validationProgress.setVisible(true)));
  }

  private void setValidationProgressText(String text) {
    getUI().ifPresent(ui -> ui.access(() -> validationProgress.setText(text)));
  }

  private void displayValidationResults(List<ValidationResponse> responses, int totalValidations,
      String fileName) {
    ValidationResult result = ValidationResult.successful();
    var combinedResult = responses.stream().map(ValidationResponse::result)
        .reduce(result, ValidationResult::combine);

    getUI().ifPresent(ui -> ui.access(() -> {
      var measurementFileItem = new MeasurementFileItem(fileName,
          new MeasurementValidationReport(totalValidations, combinedResult));
      addAndDisplayFile(measurementFileItem);
    }));
  }

  private void addAndDisplayFile(MeasurementFileItem measurementFileItem) {
    measurementFileItems.add(measurementFileItem);
    MeasurementFileDisplay measurementFileDisplay = new MeasurementFileDisplay(measurementFileItem);
    uploadedItemsDisplay.addFileToDisplay(measurementFileDisplay);
    refresh();
  }

  private void onFileRejected(FileRejectedEvent fileRejectedEvent) {
    displayError();
  }

  private ParsingResult parseXLSX(InputStream inputStream) {
    return XLSXParser.create().parse(inputStream);
  }

  private ParsingResult parseTSV(InputStream inputStream) {
    return TSVParser.create().parse(inputStream);
  }

  @Override
  public InputValidation validate() {
    var inputWithFailureSearch = measurementFileItems.stream()
        .map(MeasurementFileItem::measurementValidationReport)
        .map(MeasurementValidationReport::validationResult)
        .filter(ValidationResult::containsFailures).findAny();
    if (inputWithFailureSearch.isEmpty()) {
      return InputValidation.passed();
    }
    return InputValidation.failed();
  }

  @Override
  public boolean hasChanges() {
    return !measurementFileItems.isEmpty();
  }

  private static class UploadedItemsDisplay extends Div {

    private final Div uploadSection;
    private final Div uploadedItemsDisplays;
    private final DialogSection uploadedFilesSection;

    public UploadedItemsDisplay(Upload upload) {

      var saveYourFileInfo = new InfoBox().setInfoText(
              "When uploading a tab-separated file, please save your Excel file as UTF-16 Unicode Text (*.txt) before uploading.")
          .setClosable(false);

      var restrictions = new Div();
      restrictions.addClassNames("extra-small-body-text", "flex-horizontal", "color-secondary",
          "justify-content-space-between");
      restrictions.add(new Div("Supported file formats: .txt, .tsv, .xlsx"));
      restrictions.add(new Div(
          "Maximum file size: %s MB".formatted(MAX_FILE_SIZE_BYTES / Math.pow(1024, 2))));

      this.uploadSection = new Div();
      uploadSection.add(saveYourFileInfo, upload, restrictions);
      uploadSection.addClassNames("upload-section", "flex-vertical", "gap-03");

      uploadedItemsDisplays = new Div();
      uploadedItemsDisplays.addClassName("uploaded-measurement-items");
      this.uploadedFilesSection = DialogSection.with("Review upload results",
          new Div(uploadedItemsDisplays));

      add(uploadSection, uploadedFilesSection);
      addClassNames("upload-items-display", "flex-vertical", "gap-05");
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

    public void hide() {
      uploadedFilesSection.setVisible(false);
    }

    public void show() {
      uploadedFilesSection.setVisible(true);
    }
  }

  record MeasurementValidationReport(int validatedRows,
                                     ValidationResult validationResult) {

  }

  public record MeasurementFileItem(String fileName,
                                    MeasurementValidationReport measurementValidationReport) {

  }

  /**
   * used to display an uploaded measurement file with validation information
   */
  public static class MeasurementFileDisplay extends Div implements Focusable<Div> {

    public static final String SECONDARY = "secondary";

    @Serial
    private static final long serialVersionUID = -9075627206992036067L;
    private final transient MeasurementFileItem measurementFileItem;
    private final Div displayBox = new Div();

    public MeasurementFileDisplay(MeasurementFileItem measurementFileItem) {
      this.measurementFileItem = requireNonNull(measurementFileItem,
          "measurementFileItem must not be null");
      var fileIcon = VaadinIcon.FILE_TABLE.create();
      fileIcon.addClassName("icon-size-s");
      Span fileNameLabel = new Span(fileIcon, new Span(this.measurementFileItem.fileName()));
      fileNameLabel.addClassName("file-name");

      setDisplayBoxContent(measurementFileItem.measurementValidationReport());
      displayBox.addClassNames("flex-vertical", "padding-top-bottom-04");

      add(fileNameLabel);
      add(displayBox);
      addClassNames("flex-vertical", "gap-03", "choice-box", "padding-top-bottom-04",
          "padding-left-right-04");
    }

    public MeasurementFileItem measurementFileItem() {
      return measurementFileItem;
    }

    private void setDisplayBoxContent(MeasurementValidationReport measurementValidationReport) {
      displayBox.removeAll();
      if (measurementValidationReport.validationResult().allPassed()) {
        displayBox.add(createApprovedDisplayBox(measurementValidationReport.validatedRows()));
      } else {
        displayBox.add(createInvalidDisplayBox(measurementValidationReport.validationResult()));
      }
    }

    private Div createApprovedDisplayBox(int validatedEntriesTotal) {
      var reportContent = ValidationReportContent.success(validatedEntriesTotal);
      return ValidationReportDisplay.withHeaderAndContent(
          ValidationHeader.successWithText("Your data has been approved"), reportContent);
    }

    private Div createInvalidDisplayBox(ValidationResult result) {
      var validationSummary = result.failures().stream()
          .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
      var reportContent = ValidationReportContent.failure(validationSummary);
      return ValidationReportDisplay.withHeaderAndContent(
          ValidationHeader.failureWithText("Invalid measurement data"), reportContent);
    }
  }

  static class ValidationHeader extends Div {

    static ValidationHeader successWithText(String text) {
      return new ValidationHeader(text, true);
    }

    static ValidationHeader failureWithText(String text) {
      return new ValidationHeader(text, false);
    }

    private ValidationHeader(String message, boolean isSuccess) {
      Icon icon = isSuccess ? successIcon() : failureIcon();
      var iconContainer = new Div();
      iconContainer.add(icon);
      iconContainer.addClassNames("flex-align-items-center", "flex-horizontal");
      var textContainer = new Div(message);
      textContainer.getStyle().set("font-weight", "bold");

      add(iconContainer, textContainer);

      addClassNames("flex-horizontal", "gap-04");
    }

    private static Icon successIcon() {
      Icon icon = VaadinIcon.CHECK_CIRCLE.create();
      icon.addClassNames("icon-color-success", "icon-size-xs");
      return icon;
    }

    private static Icon failureIcon() {
      Icon icon = VaadinIcon.CLOSE_CIRCLE.create();
      icon.addClassNames("icon-color-error", "icon-size-xs");
      return icon;
    }
  }

  static class ValidationReportDisplay extends Div {

    private ValidationReportDisplay(ValidationHeader header) {
      add(header);
      addClassNames("flex-vertical", "gap-04", "choice-box", "padding-top-bottom-04",
          "padding-left-right-04", "background-contrast-5pct");
    }

    static ValidationReportDisplay empty() {
      return new ValidationReportDisplay();
    }

    static ValidationReportDisplay withHeader(ValidationHeader header) {
      return new ValidationReportDisplay(header);
    }

    static ValidationReportDisplay withHeaderAndContent(ValidationHeader header,
        ValidationReportContent content) {
      var display = new ValidationReportDisplay(header);
      display.add(content);
      return display;
    }

    private ValidationReportDisplay() {

    }
  }

  static class ValidationReportContent extends Div {

    private ValidationReportContent() {

    }

    static ValidationReportContent success(int successfulItems) {
      var content = new ValidationReportContent();
      var textContainer = new Div();
      var items = new Span(successfulItems + " measurements");
      items.getStyle().set("font-weight", "bold");
      textContainer.add(
          new Text("Metadata for "),
          items,
          new Text(" is now ready to be registered.")
      );
      var disclaimer = disclaimerForSuccess();
      content.add(textContainer, disclaimer);
      return content;
    }

    static ValidationReportContent failure(Map<String, Long> reportedFailures) {
      var summary = new Div();
      summary.addClassNames("flex-vertical", "gap-02");

      for (Entry<String, Long> entry : reportedFailures.entrySet()) {
        var occurrences = new Span("%s x".formatted(entry.getValue()));
        occurrences.getStyle().set("font-weight", "bold");

        var finding = new Div();
        finding.add(occurrences, new Div(entry.getKey()));
        finding.addClassNames("flex-horizontal", "gap-02");
        summary.add(finding);
      }

      var disclaimer = disclaimerForFailure();
      var content = new ValidationReportContent();
      content.add(summary, disclaimer);
      return content;
    }

    private static Div disclaimerForSuccess() {
      var textBox = new Div(
          "Please click on Register to register the sample measurement metadata.");
      textBox.addClassNames("small-body-text", "color-secondary");
      return textBox;
    }

    private static Div disclaimerForFailure() {
      var textBox = new Div(
          "Please correct the entries in the uploaded excel sheet and re-upload.");
      textBox.addClassNames("small-body-text", "color-secondary");
      return textBox;
    }

  }
}
