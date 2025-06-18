package life.qbic.datamanager.views.projects.project.measurements.registration;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.OrderedList.NumberingType;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.converters.MetadataConverterV2;
import life.qbic.datamanager.files.parsing.tsv.TSVParser;
import life.qbic.datamanager.files.parsing.xlsx.XLSXParser;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.upload.EditableMultiFileMemoryBuffer;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
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
  private final UploadItemsDisplay uploadItemsDisplay;
  private final AsyncProjectService service;
  private final Context context;
  private final Div validationProgress;
  private final MetadataConverterV2<? extends ValidationRequestBody> converter;

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
    this.uploadItemsDisplay = new UploadItemsDisplay(upload);

    // Add components to the MeasurementUpload component
    add(uploadItemsDisplay);
    add(validationProgress);

    // Trigger display refresh
    refresh();
  }

  public List<? extends ValidationRequestBody> getValidationRequestContent() {
    return validationRequestsPerFile.values().stream().flatMap(List::stream).toList();
  }

  /**
   * Triggers a refresh of child components, e.g., visibility.
   */
  private void refresh() {
    if (measurementFileItems.isEmpty()) {
      uploadItemsDisplay.hide();
      return;
    }
    uploadItemsDisplay.show();
  }

  private void removeFile(String fileName) {
    uploadBuffer.remove(fileName);
    measurementFileItems.removeIf(
        measurementFileItem -> measurementFileItem.fileName().equals(fileName));
    uploadItemsDisplay.removeFileFromDisplay(fileName);
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

    var itemsToValidate = result.size();
    var requests = new ArrayList<ValidationRequest>();

    validationRequestsPerFile.put(fileName, result);

    for (var registration : result) {
      var request = new ValidationRequest(context.projectId().orElseThrow().value(),
          context.experimentId().orElseThrow().value(), registration);
      requests.add(request);
    }
    runValidation(requests, itemsToValidate, fileName);
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
    uploadItemsDisplay.addFileToDisplay(measurementFileDisplay);
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
    var inputWithFailureSearch = measurementFileItems.stream().map(MeasurementFileItem::measurementValidationReport)
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

    public void hide() {
      uploadedItemsDisplays.setVisible(false);
    }

    public void show() {
      uploadedItemsDisplays.setVisible(true);
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
  public static class MeasurementFileDisplay extends Div {

    public static final String SECONDARY = "secondary";

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
      setDisplayBoxContent(measurementFileItem.measurementValidationReport());
      displayBox.addClassName("validation-display-box");
      add(displayBox);
      addClassName("measurement-item");
    }

    public MeasurementFileItem measurementFileItem() {
      return measurementFileItem;
    }

    private void setDisplayBoxContent(MeasurementValidationReport measurementValidationReport) {
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
      instruction.addClassName(SECONDARY);
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
      instruction.addClassName(SECONDARY);
      Div validationDetails = new Div();
      OrderedList invalidMeasurementsList = new OrderedList(
          invalidMeasurements.stream().map(ListItem::new).toArray(ListItem[]::new));
      invalidMeasurementsList.addClassName("invalid-measurement-list");
      invalidMeasurementsList.setType(NumberingType.NUMBER);
      validationDetails.add(invalidMeasurementsList);
      box.add(header, validationDetails, instruction);
      return box;
    }

  }
}
