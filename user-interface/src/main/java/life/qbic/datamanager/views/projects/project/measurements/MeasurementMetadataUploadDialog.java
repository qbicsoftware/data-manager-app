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
import com.vaadin.flow.component.upload.FailedEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.shared.Registration;
import elemental.json.JsonObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.EditableMultiFileMemoryBuffer;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.ProteomicsValidator.PROTEOMICS_PROPERTY;
import life.qbic.projectmanagement.application.measurement.validation.ValidationResult;
import life.qbic.projectmanagement.application.measurement.validation.ValidationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

public class MeasurementMetadataUploadDialog extends DialogWindow {

  public static final int MAX_FILE_SIZE_BYTES = (int) (Math.pow(1024, 2) * 16);
  @Serial
  private static final long serialVersionUID = -8253078073427291947L;
  private static final String VAADIN_FILENAME_EVENT = "event.detail.file.name";
  private final transient ValidationService validationService;
  private final EditableMultiFileMemoryBuffer uploadBuffer;
  private final transient List<MeasurementMetadataUpload<MeasurementMetadata>> measurementMetadataUploads;
  private final transient List<MeasurementFileItem> measurementFileItems;
  private final Div uploadedItemsSection;
  private final Div uploadedItemsDisplays;
  private final ExperimentId experimentId;

  public MeasurementMetadataUploadDialog(ValidationService validationService,
      ExperimentId experimentId) {
    this.validationService = requireNonNull(validationService,
        "validationService must not be null");
    this.experimentId = requireNonNull(experimentId, "experimentId must not be null");
    this.uploadBuffer = new EditableMultiFileMemoryBuffer();
    this.measurementMetadataUploads = new ArrayList<>();
    this.measurementFileItems = new ArrayList<>();

    Upload upload = new Upload(uploadBuffer);
    upload.setAcceptedFileTypes("text/tab-separated-values", "text/plain");
    upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);

    setHeaderTitle("Register measurements");
    confirmButton.setText("Register");

    var uploadSectionTitle = new Span("Upload the measurement data");
    uploadSectionTitle.addClassName("section-title");

    var saveYourFileInfo = new InfoBox().setInfoText(
            "Please save your excel file as Text (Tab delimited) (*.txt) before uploading.")
        .setClosable(false);

    var restrictions = new Div();
    restrictions.addClassName("restrictions");
    restrictions.add(new Span("Supported file formats: .txt, .tsv"));
    restrictions.add("Maximum file size: %s MB".formatted(MAX_FILE_SIZE_BYTES / Math.pow(1024, 2)));

    var uploadSection = new Div();
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

    upload.addSucceededListener(this::onUploadSucceeded);
    upload.addFileRejectedListener(this::onFileRejected);
    upload.addFailedListener(this::onUploadFailed);

    // Synchronise the Vaadin upload component with the purchase list display
    // When a file is removed from the upload component, we also want to remove it properly from memory
    // and from any additional display
    upload.getElement().addEventListener("file-remove", this::onFileRemoved)
        .addEventData(VAADIN_FILENAME_EVENT);
    addClassName("measurement-upload-dialog");
    toggleFileSectionIfEmpty();
  }

  private static List<String> parseHeaderContent(String header) {
    return Arrays.stream(header.replace("*", "").strip().split("\t")).map(String::strip).toList();
  }

  private static Map<String, Integer> propertyColumnMap(List<String> properties) {
    var propertyIterator = properties.listIterator();
    Map<String, Integer> map = new HashMap<>();
    int index;
    while ((index = propertyIterator.nextIndex()) < properties.size()) {
      map.put(propertyIterator.next().toLowerCase(), index);
    }
    return map;
  }

  private static MetadataContent read(InputStream inputStream) {
    var content = new BufferedReader(new InputStreamReader(inputStream)).lines().toList();

    return new MetadataContent(content.isEmpty() ? null : content.get(0),
        content.size() > 1 ? content.subList(1, content.size()) : new ArrayList<>());
  }

  private static boolean isRowNotEmpty(String row) {
    return row.split("\t").length > 0;
  }

  private static Result<ProteomicsMeasurementMetadata, String> generatePxPRequest(
      String row, Map<String, Integer> columns) {
    var columnValues = row.split("\t"); // tab separated values
    // we consider an empty row as a reason to warn, not to fail
    if (columnValues.length == 0) {
      return Result.fromValue(null);
    }

    Integer sampleCodeColumnIndex = columns.get(PROTEOMICS_PROPERTY.QBIC_SAMPLE_ID.label());
    Integer oranisationColumnIndex = columns.get(PROTEOMICS_PROPERTY.ORGANISATION_ID.label());
    Integer instrumentColumnIndex = columns.get(PROTEOMICS_PROPERTY.INSTRUMENT.label());
    Integer samplePoolGroupIndex = columns.get(PROTEOMICS_PROPERTY.SAMPLE_POOL_GROUP.label());
    Integer facilityIndex = columns.get(PROTEOMICS_PROPERTY.FACILITY.label());
    Integer fractionNameIndex = columns.get(PROTEOMICS_PROPERTY.CYCLE_FRACTION_NAME.label());
    Integer digestionEnzymeIndex = columns.get(PROTEOMICS_PROPERTY.DIGESTION_ENZYME.label());
    Integer digestionMethodIndex = columns.get(PROTEOMICS_PROPERTY.DIGESTION_METHOD.label());
    Integer enrichmentMethodIndex = columns.get(PROTEOMICS_PROPERTY.ENRICHMENT_METHOD.label());
    Integer injectionVolumeIndex = columns.get(PROTEOMICS_PROPERTY.INJECTION_VOLUME.label());
    Integer lcColumnIndex = columns.get(PROTEOMICS_PROPERTY.LC_COLUMN.label());
    Integer lcmsMethodIndex = columns.get(PROTEOMICS_PROPERTY.LCMS_METHOD.label());
    Integer labelingTypeIndex = columns.get(PROTEOMICS_PROPERTY.LABELING_TYPE.label());
    Integer labelIndex = columns.get(PROTEOMICS_PROPERTY.LABEL.label());
    Integer noteIndex = columns.get(PROTEOMICS_PROPERTY.COMMENT.label());

    int maxPropertyIndex = IntStream.of(sampleCodeColumnIndex,
            oranisationColumnIndex,
            instrumentColumnIndex)
        .max().orElseThrow();
    if (columns.size() <= maxPropertyIndex) {
      return Result.fromError("Not enough columns provided for row: %s".formatted(row));
    }

    List<SampleCode> sampleCodes = List.of(
        SampleCode.create(safeArrayAccess(columnValues, sampleCodeColumnIndex).orElse("")));
    String organisationRoRId = safeArrayAccess(columnValues, oranisationColumnIndex).orElse("");
    String instrumentCURIE = safeArrayAccess(columnValues, instrumentColumnIndex).orElse("");
    String samplePoolGroup = safeArrayAccess(columnValues, samplePoolGroupIndex).orElse("");
    String facility = safeArrayAccess(columnValues, facilityIndex).orElse("");
    String fractionName = safeArrayAccess(columnValues, fractionNameIndex).orElse("");
    String digestionEnzyme = safeArrayAccess(columnValues, digestionEnzymeIndex).orElse("");
    String digestionMethod = safeArrayAccess(columnValues, digestionMethodIndex).orElse("");
    String enrichmentMethod = safeArrayAccess(columnValues, enrichmentMethodIndex).orElse("");
    String injectionVolume = safeArrayAccess(columnValues, injectionVolumeIndex).orElse("");
    String lcColumn = safeArrayAccess(columnValues, lcColumnIndex).orElse("");
    String lcmsMethod = safeArrayAccess(columnValues, lcmsMethodIndex).orElse("");
    String labelingType = safeArrayAccess(columnValues, labelingTypeIndex).orElse("");
    String label = safeArrayAccess(columnValues, labelIndex).orElse("");
    String note = safeArrayAccess(columnValues, noteIndex).orElse("");

    ProteomicsMeasurementMetadata metadata = new ProteomicsMeasurementMetadata("", sampleCodes,
        organisationRoRId, instrumentCURIE, samplePoolGroup, facility, fractionName,
        digestionEnzyme,
        digestionMethod, enrichmentMethod, injectionVolume, lcColumn, lcmsMethod, labelingType,
        label, note);
    return Result.fromValue(metadata);
  }

  private static List<SampleCode> parseSampleCode(String sampleCodeEntry) {
    return Arrays.stream(sampleCodeEntry.split(",")).map(SampleCode::create).toList();
  }

  private static Optional<String> safeArrayAccess(String[] array, int index) {
    try {
      return Optional.of(array[index]);
    } catch (ArrayIndexOutOfBoundsException e) {
      return Optional.empty();
    }
  }

  private void onFileRemoved(DomEvent domEvent) {
    JsonObject jsonObject = domEvent.getEventData();
    var fileName = jsonObject.getString(VAADIN_FILENAME_EVENT);
    removeFile(fileName);

  }

  private void showFile(MeasurementFileItem measurementFileItem) {
    MeasurementFileDisplay measurementFileDisplay = new MeasurementFileDisplay(measurementFileItem);
    uploadedItemsDisplays.add(measurementFileDisplay);
  }

  private void removeFile(String fileName) {
    uploadBuffer.remove(fileName);
    MeasurementFileDisplay[] fileDisplays = fileDisplaysWithFileName(fileName);
    uploadedItemsDisplays.remove(fileDisplays);
    measurementMetadataUploads.removeIf(
        metadataUpload -> metadataUpload.fileName().equals(fileName));
    measurementFileItems.removeIf(
        measurementFileItem -> measurementFileItem.fileName().equals(fileName));
    toggleFileSectionIfEmpty();
  }

  private MeasurementFileDisplay[] fileDisplaysWithFileName(String fileName) {
    return uploadedItemsDisplays.getChildren()
        .filter(MeasurementFileDisplay.class::isInstance)
        .map(MeasurementFileDisplay.class::cast)
        .filter(measurementFileDisplay -> measurementFileDisplay.measurementFileItem().fileName()
            .equals(fileName))
        .toArray(MeasurementFileDisplay[]::new);
  }

  private void toggleFileSectionIfEmpty() {
    uploadedItemsSection.setVisible(!measurementFileItems.isEmpty());
  }

  private void onUploadFailed(FailedEvent failedEvent) {
    showErrorNotification("File upload was interrupted", failedEvent.getReason().getMessage());
  }

  private void onUploadSucceeded(SucceededEvent succeededEvent) {
    MetadataContent content = read(
        uploadBuffer.inputStream(succeededEvent.getFileName()).orElseThrow());
    var contentHeader = content.theHeader()
        .orElseThrow(() -> new RuntimeException("No header row found"));
    var domain = validationService
        .inferDomainByPropertyTypes(parseHeaderContent(contentHeader))
        .orElseThrow(() -> new RuntimeException(
            "Header row could not be recognized, Please provide a valid template file"));

    var validationReport = switch (domain) {
      case PROTEOMICS -> validatePxP(content);
      case NGS -> validateNGS();
    };
    MeasurementFileItem measurementFileItem = new MeasurementFileItem(succeededEvent.getFileName(),
        validationReport);
    //We don't want to upload any invalid measurements in spreadsheet
    if (validationReport.validationResult.containsFailures()) {
      MeasurementMetadataUpload<MeasurementMetadata> metadataUpload = new MeasurementMetadataUpload<>(
          succeededEvent.getFileName(), Collections.emptyList());
      addFile(measurementFileItem, metadataUpload);
    } else {
      var measurementMetadata = switch (domain) {
        case PROTEOMICS -> generatePxPMetadata(content);
        case NGS -> null;
      };
      MeasurementMetadataUpload<MeasurementMetadata> metadataUpload = new MeasurementMetadataUpload(
          succeededEvent.getFileName(), measurementMetadata);
      addFile(measurementFileItem, metadataUpload);
    }
  }

  private void addFile(MeasurementFileItem measurementFileItem,
      MeasurementMetadataUpload<MeasurementMetadata> metadataUpload) {
    measurementMetadataUploads.add(metadataUpload);
    measurementFileItems.add(measurementFileItem);
    showFile(measurementFileItem);
    toggleFileSectionIfEmpty();
  }

  private List<ProteomicsMeasurementMetadata> generatePxPMetadata(
      MetadataContent content) {
    var propertyColumnMap = propertyColumnMap(parseHeaderContent(content.header()));

    var results = content.rows().stream()
        .map(row -> generatePxPRequest(row, propertyColumnMap))
        .toList();
    if (results.stream().anyMatch(Result::isError)) {
      return new ArrayList<>();
    }
    return results.stream()
        .filter(Result::isValue)
        .map(Result::getValue)
        .filter(Objects::nonNull)
        .toList();
  }

  private ValidationReport validateNGS() {
    return new ValidationReport(0, ValidationResult.successful(0));
  }

  private ValidationReport validatePxP(MetadataContent content) {

    var validationResult = ValidationResult.successful(0);
    var propertyColumnMap = propertyColumnMap(parseHeaderContent(content.header()));
    var evaluatedRows = 0;
    // we check if there are any rows provided or if we have only rows with empty content
    if (content.rows().isEmpty() || content.rows().stream()
        .noneMatch(MeasurementMetadataUploadDialog::isRowNotEmpty)) {
      validationResult = validationResult.combine(
          ValidationResult.withFailures(0, List.of("The metadata sheet seems to be empty")));
      return new ValidationReport(0, validationResult);
    }
    for (String row : content.rows().stream()
        .filter(MeasurementMetadataUploadDialog::isRowNotEmpty).toList()) {
      ValidationResult result = validatePxPRow(propertyColumnMap, row);
      validationResult = validationResult.combine(result);
      evaluatedRows++;
    }
    return new ValidationReport(evaluatedRows, validationResult);
  }

  private ValidationResult validatePxPRow(Map<String, Integer> propertyColumnMap, String row) {
    var validationResult = ValidationResult.successful(0);
    var metaDataValues = row.split("\t"); // tab separated values
    // we consider an empty row as a reason to warn, not to fail
    if (metaDataValues.length == 0) {
      validationResult.combine(ValidationResult.successful(1, List.of("Empty row provided.")));
      return validationResult;
    }
    if (metaDataValues.length != propertyColumnMap.keySet().size()) {
      validationResult.combine(ValidationResult.withFailures(1, List.of("")));
    }

    var sampleCodeColumnIndex = propertyColumnMap.get(
        PROTEOMICS_PROPERTY.QBIC_SAMPLE_ID.label());
    var organisationsColumnIndex = propertyColumnMap.get(
        PROTEOMICS_PROPERTY.ORGANISATION_ID.label());
    var instrumentColumnIndex = propertyColumnMap.get(
        PROTEOMICS_PROPERTY.INSTRUMENT.label());
    var samplePoolGroupIndex = propertyColumnMap.get(
        PROTEOMICS_PROPERTY.SAMPLE_POOL_GROUP.label());
    var facilityIndex = propertyColumnMap.get(PROTEOMICS_PROPERTY.FACILITY.label());
    var fractionNameIndex = propertyColumnMap.get(PROTEOMICS_PROPERTY.CYCLE_FRACTION_NAME.label());
    var digestionEnzymeIndex = propertyColumnMap.get(PROTEOMICS_PROPERTY.DIGESTION_ENZYME.label());
    var digestionMethodIndex = propertyColumnMap.get(PROTEOMICS_PROPERTY.DIGESTION_METHOD.label());
    Integer enrichmentMethodIndex = propertyColumnMap.get(
        PROTEOMICS_PROPERTY.ENRICHMENT_METHOD.label());
    Integer injectionVolumeIndex = propertyColumnMap.get(
        PROTEOMICS_PROPERTY.INJECTION_VOLUME.label());
    Integer lcColumnIndex = propertyColumnMap.get(PROTEOMICS_PROPERTY.LC_COLUMN.label());
    Integer lcmsMethodIndex = propertyColumnMap.get(PROTEOMICS_PROPERTY.LCMS_METHOD.label());
    Integer labelingTypeIndex = propertyColumnMap.get(PROTEOMICS_PROPERTY.LABELING_TYPE.label());
    Integer labelIndex = propertyColumnMap.get(PROTEOMICS_PROPERTY.LABEL.label());
    Integer noteIndex = propertyColumnMap.get(PROTEOMICS_PROPERTY.COMMENT.label());

    int maxPropertyIndex = IntStream.of(sampleCodeColumnIndex, organisationsColumnIndex,
        instrumentColumnIndex).max().orElseThrow();
    if (propertyColumnMap.size() <= maxPropertyIndex) {
      return validationResult.combine(ValidationResult.withFailures(1,
          List.of("Not enough columns provided for row: \"%s\"".formatted(row))));
    }

    var sampleCodes = SampleCode.create(
        safeArrayAccess(metaDataValues, sampleCodeColumnIndex).orElse(""));
    var organisationRoRId = safeArrayAccess(metaDataValues, organisationsColumnIndex).orElse("");
    var instrumentCURIE = safeArrayAccess(metaDataValues, instrumentColumnIndex).orElse("");
    var samplePoolGroup = safeArrayAccess(metaDataValues, samplePoolGroupIndex).orElse("");
    var facility = safeArrayAccess(metaDataValues, facilityIndex).orElse("");
    var fractionName = safeArrayAccess(metaDataValues, fractionNameIndex).orElse("");
    var digestionEnzyme = safeArrayAccess(metaDataValues, digestionEnzymeIndex).orElse("");
    var digestionMethod = safeArrayAccess(metaDataValues, digestionMethodIndex).orElse("");
    var enrichmentMethod = safeArrayAccess(metaDataValues, enrichmentMethodIndex).orElse("");
    var injectionVolume = safeArrayAccess(metaDataValues, injectionVolumeIndex).orElse("");
    var lcColumn = safeArrayAccess(metaDataValues, lcColumnIndex).orElse("");
    var lcmsMethod = safeArrayAccess(metaDataValues, lcmsMethodIndex).orElse("");
    var labelingType = safeArrayAccess(metaDataValues, labelingTypeIndex).orElse("");
    var label = safeArrayAccess(metaDataValues, labelIndex).orElse("");

    var note = safeArrayAccess(metaDataValues, noteIndex).orElse("");

    var metadata = new ProteomicsMeasurementMetadata("", List.of(sampleCodes),
        organisationRoRId, instrumentCURIE, samplePoolGroup, facility, fractionName,
        digestionEnzyme,
        digestionMethod, enrichmentMethod, injectionVolume, lcColumn, lcmsMethod, labelingType,
        label, note);

    validationResult = validationResult.combine(validationService.validateProteomics(metadata));
    return validationResult;
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
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), measurementMetadataUploads));
  }

  private boolean containsInvalidMeasurementData() {
    return measurementFileItems.stream()
        .map(MeasurementFileItem::validationReport)
        .map(ValidationReport::validationResult)
        .anyMatch(ValidationResult::containsFailures);
  }


  private void showErrorNotification(String title, String description) {
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  /**
   * modifies the dialog in a way to show that all measurements of a file were registered.
   *
   * @param filename
   */
  public void markSuccessful(String filename) {
    removeFile(filename);
  }

  /**
   * shows an error for a given measurement file
   *
   * @param filename
   * @param error
   */
  public void showError(String filename, String error) {
    for (MeasurementFileDisplay measurementFileDisplay : fileDisplaysWithFileName(filename)) {
      measurementFileDisplay.addError(error);
    }
  }

  record ValidationReport(int validatedRows, ValidationResult validationResult) {

  }

  private record MetadataContent(String header, List<String> rows) {

    Optional<String> theHeader() {
      return Optional.ofNullable(header);
    }
  }

  public record MeasurementMetadataUpload<T extends MeasurementMetadata>(String fileName,
                                                                         List<MeasurementMetadata> measurementMetadata) {

  }

  public record MeasurementFileItem(String fileName, ValidationReport validationReport) {

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
      createDisplayBox(measurementFileItem.validationReport());
      displayBox.addClassName("validation-display-box");
      add(displayBox);
      addClassName("measurement-item");
    }

    public MeasurementFileItem measurementFileItem() {
      return measurementFileItem;
    }

    public void addError(String error) {
      displayBox.removeAll();
      displayBox.add(createInvalidDisplayBox(List.of(error)));
    }

    private void createDisplayBox(ValidationReport validationReport) {
      displayBox.removeAll();
      if (validationReport.validationResult().allPassed()) {
        displayBox.add(createApprovedDisplayBox(validationReport.validatedRows()));
      } else {
        displayBox.add(createInvalidDisplayBox(validationReport.validationResult().failures()));
      }
    }

    private Div createApprovedDisplayBox(int validMeasurementCount) {
      Div box = new Div();
      Span approvedTitle = new Span("Your data has been approved");
      Icon validIcon = VaadinIcon.CHECK_CIRCLE_O.create();
      validIcon.addClassName("valid");
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
}
