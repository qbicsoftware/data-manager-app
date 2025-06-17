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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.converters.ConverterRegistry;
import life.qbic.datamanager.files.parsing.tsv.TSVParser;
import life.qbic.datamanager.files.parsing.xlsx.XLSXParser;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.upload.EditableMultiFileMemoryBuffer;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMetadataUploadDialog.MeasurementMetadataUpload;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationResponse;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import reactor.core.publisher.Flux;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MeasurementRegistrationNGS extends Div implements UserInput {

  public static final int MAX_FILE_SIZE_BYTES = (int) (Math.pow(1024, 2) * 16);
  private final UploadItemsDisplay uploadItemsDisplay;
  private final AsyncProjectService service;
  private final Context context;
  private final Div validationProgress;

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

  public MeasurementRegistrationNGS(AsyncProjectService service, Context context) {
    this.uploadBuffer = new EditableMultiFileMemoryBuffer();
    this.measurementFileItems = new ArrayList<>();
    this.service = service;
    this.context = context;
    this.validationProgress = new Div();
    var upload = new Upload(uploadBuffer);
    upload.setAcceptedFileTypes(AcceptedFileType.allMimeTypes().toArray(String[]::new));
    upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);
    upload.addSucceededListener(this::onUploadSucceeded);
    upload.addFileRejectedListener(this::onFileRejected);
    upload.addFailedListener(this::onUploadFailed);
    upload.addFileRemovedListener(this::onFileRemoved);
    this.uploadItemsDisplay = new UploadItemsDisplay(upload);
    this.uploadItemsDisplay.toggleFileSectionIfEmpty(true);
    add(validationProgress);
    add(uploadItemsDisplay);
  }

  private void removeFile(String fileName) {
    uploadBuffer.remove(fileName);
    measurementFileItems.removeIf(
        measurementFileItem -> measurementFileItem.fileName().equals(fileName));
    uploadItemsDisplay.removeFileFromDisplay(fileName);
    uploadItemsDisplay.toggleFileSectionIfEmpty(!measurementFileItems.isEmpty());
  }

  private void onFileRemoved(FileRemovedEvent fileRemovedEvent) {
    removeFile(fileRemovedEvent.getFileName());
  }

  private void onUploadFailed(FailedEvent failedEvent) {
    //showErrorNotification("File upload was interrupted", failedEvent.getReason().getMessage());
  }

  private void onUploadSucceeded(SucceededEvent succeededEvent) {
    var fileName = succeededEvent.getFileName();
    Optional<AcceptedFileType> knownFileType = AcceptedFileType.forMimeType(
        succeededEvent.getMIMEType());
    if (knownFileType.isEmpty()) {
      //displayError(succeededEvent.getFileName(),
      //    "Unsupported file type. Please make sure to upload a TSV or XLSX file.");
      return;
    }

    var parsingResult = switch (knownFileType.get()) {
      case EXCEL -> parseXLSX(uploadBuffer.inputStream(fileName).orElseThrow());
      case TSV -> parseTSV(uploadBuffer.inputStream(fileName).orElseThrow());
    };

    var converter = ConverterRegistry.converterFor(MeasurementRegistrationInformationNGS.class);
    var result = converter.convert(parsingResult);

    validationProgress.setVisible(true);
    var itemsToValidate = result.size();

    var requests = new ArrayList<ValidationRequest>();
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
        .doFirst(() -> setValidationProgressText("Starting validation ..."))
        .doOnNext(item -> setValidationProgressText("Processed " + counter.getAndIncrement() + " requests from " + itemsToValidate))
        .doOnNext(responses::add)
        .doOnComplete(() -> displayValidationResults(responses, itemsToValidate, fileName))
        .subscribe();
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
      uploadItemsDisplay.setVisible(true);
      uploadItemsDisplay.addFileToDisplay(new MeasurementFileDisplay(
          new MeasurementFileItem(fileName,
              new MeasurementValidationReport(totalValidations, combinedResult))));
    }));
  }

  private void showFile(MeasurementFileItem measurementFileItem) {
    MeasurementFileDisplay measurementFileDisplay = new MeasurementFileDisplay(measurementFileItem);
    uploadItemsDisplay.addFileToDisplay(measurementFileDisplay);
    uploadItemsDisplay.toggleFileSectionIfEmpty(!measurementFileItems.isEmpty());
  }


  private void addFile(MeasurementFileItem measurementFileItem,
      MeasurementMetadataUpload<MeasurementMetadata> metadataUpload) {
    measurementFileItems.add(measurementFileItem);
    showFile(measurementFileItem);
  }

  private void onFileRejected(FileRejectedEvent fileRejectedEvent) {
    String errorMessage = fileRejectedEvent.getErrorMessage();
    //showErrorNotification("File upload failed", errorMessage);
  }

  private ParsingResult parseXLSX(InputStream inputStream) {
    return XLSXParser.create().parse(inputStream);
  }

  private ParsingResult parseTSV(InputStream inputStream) {
    return TSVParser.create().parse(inputStream);
  }

  @Override
  public InputValidation validate() {
    // TODO implement
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public boolean hasChanges() {
    // TODO implement
    throw new RuntimeException("Not yet implemented");
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
