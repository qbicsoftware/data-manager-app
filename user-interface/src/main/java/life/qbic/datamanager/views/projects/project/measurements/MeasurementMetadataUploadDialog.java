package life.qbic.datamanager.views.projects.project.measurements;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.upload.FailedEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.FinishedEvent;
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
import life.qbic.datamanager.views.projects.EditableMultiFileMemoryBuffer;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.MeasurementRegistrationRequest;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.ProteomicsValidator.PROTEOMICS_PROPERTY;
import life.qbic.projectmanagement.application.measurement.validation.ValidationResult;
import life.qbic.projectmanagement.application.measurement.validation.ValidationService;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class MeasurementMetadataUploadDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = -8253078073427291947L;
  public static final int MAX_FILE_SIZE_BYTES = (int) (Math.pow(1024, 2) * 5);
  private static final String VAADIN_FILENAME_EVENT = "event.detail.file.name";
  private final transient ValidationService validationService;
  private final EditableMultiFileMemoryBuffer uploadBuffer;
  private final List<MeasurementMetadataUpload<MeasurementMetadata>> measurementMetadataUploads;
  private final List<MeasurementFileItem> measurementFileItems;
  private final Div uploadedItemsSection;

  public MeasurementMetadataUploadDialog(ValidationService validationService) {
    this.validationService = requireNonNull(validationService,
        "validationService must not be null");
    this.uploadBuffer = new EditableMultiFileMemoryBuffer();
    this.measurementMetadataUploads = new ArrayList<>();
    this.measurementFileItems = new ArrayList<>();

    var upload = new Upload(uploadBuffer);
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

    uploadedItemsSection = new Div();
    uploadedItemsSection.addClassName("uploaded-items-section"); //TODO CSS

    var uploadedItemsSectionTitle = new Span("Uploaded files");
    uploadedItemsSectionTitle.addClassName("section-title");
    var uploadedItemDisplays = new Div();
    uploadedItemDisplays.addClassName("uploaded-items");

    uploadedItemsSection.add(uploadedItemsSectionTitle, uploadedItemDisplays);

    add(uploadSection, uploadedItemsSection);

    upload.addSucceededListener(this::onUploadSucceeded);
    upload.addFileRejectedListener(this::onFileRejected);
    upload.addFailedListener(this::onUploadFailed);
    upload.addFinishedListener(this::onUploadFinished);

    // Synchronise the Vaadin upload component with the purchase list display
    // When a file is removed from the upload component, we also want to remove it properly from memory
    // and from any additional display
    upload.getElement().addEventListener("file-remove", this::onFileRemoved)
        .addEventData(VAADIN_FILENAME_EVENT);
  }

  private void onFileRemoved(DomEvent domEvent) {
    JsonObject jsonObject = domEvent.getEventData();
    var fileName = jsonObject.getString(VAADIN_FILENAME_EVENT);
    removeFile(fileName);
  }

  private void showFile(MeasurementFileItem measurementFileItem) {
    MeasurementFileDisplay measurementFileDisplay = new MeasurementFileDisplay(measurementFileItem);
    uploadedItemsSection.add(measurementFileDisplay);
  }


  private void removeFile(String fileName) {
    MeasurementFileDisplay[] fileDisplays = uploadedItemsSection.getChildren()
        .filter(it -> it instanceof MeasurementFileDisplay)
        .map(child -> (MeasurementFileDisplay) child)
        .filter(measurementFileDisplay -> measurementFileDisplay.measurementFileItem().fileName()
            .equals(fileName))
        .toArray(MeasurementFileDisplay[]::new);
    uploadedItemsSection.remove(fileDisplays);
    measurementMetadataUploads.removeIf(
        metadataUpload -> metadataUpload.fileName().equals(fileName));
    measurementFileItems.removeIf(
        measurementFileItem -> measurementFileItem.fileName().equals(fileName));
    toggleFileSectionIfEmpty();
  }

  private void toggleFileSectionIfEmpty() {
    uploadedItemsSection.setVisible(!measurementFileItems.isEmpty());
  }

  private static List<String> parseHeaderContent(String header) {
    return Arrays.stream(header.strip().split("\t")).map(String::strip).toList();
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

  private void onUploadFailed(FailedEvent failedEvent) {
    //TODO what happens if the upload failed
  }

  private void onUploadFinished(FinishedEvent finishedEvent) {
    MetadataContent content = read(
        uploadBuffer.inputStream(finishedEvent.getFileName()).orElseThrow());
    var contentHeader = content.theHeader()
        .orElseThrow(() -> new RuntimeException("No header row found"));
    var domain = validationService
        .inferDomainByPropertyTypes(parseHeaderContent(contentHeader))
        .orElseThrow();

    var registrationRequests = switch (domain) {
      case PROTEOMICS -> generatePxPRequests(content);
      case NGS -> generateNgsRequests();
    };
    var validationReport = switch (domain) {
      case PROTEOMICS -> validatePxP(content);
      case NGS -> validateNGS();
    };
    MeasurementFileItem measurementFileItem = new MeasurementFileItem(finishedEvent.getFileName(),
        validationReport);
    MeasurementMetadataUpload<MeasurementMetadata> metadataUpload = new MeasurementMetadataUpload(
        finishedEvent.getFileName(), registrationRequests);

    addFile(measurementFileItem, metadataUpload);
  }

  private List<MeasurementRegistrationRequest<ProteomicsMeasurementMetadata>> generateNgsRequests() {
    return null;
  }

  private void addFile(MeasurementFileItem measurementFileItem,
      MeasurementMetadataUpload<MeasurementMetadata> metadataUpload) {
    measurementMetadataUploads.add(metadataUpload);
    measurementFileItems.add(measurementFileItem);
    showFile(measurementFileItem);
  }

  private static Result<MeasurementRegistrationRequest<ProteomicsMeasurementMetadata>, String> generatePxPRequest(
      String row, Map<String, Integer> columns) {
    var columnValues = row.split("\t"); // tab separated values
    // we consider an empty row as a reason to warn, not to fail
    if (columnValues.length == 0) {
      return Result.fromValue(null);
    }

    Integer sampleCodeColumnIndex = columns.get(PROTEOMICS_PROPERTY.QBIC_SAMPLE_ID.label());
    Integer oranisationColumnIndex = columns.get(PROTEOMICS_PROPERTY.ORGANISATION_ID.label());
    Integer instrumentColumnIndex = columns.get(PROTEOMICS_PROPERTY.INSTRUMENT.label());
    int maxPropertyIndex = IntStream.of(sampleCodeColumnIndex,
            oranisationColumnIndex,
            instrumentColumnIndex)
        .max().orElseThrow();
    if (columns.size() <= maxPropertyIndex) {
      return Result.fromError("Not enough columns provided for row: %s".formatted(row));
    }

    List<SampleCode> sampleCodes = parseSampleCode(columnValues[sampleCodeColumnIndex]);
    String organisationRoRId = columnValues[oranisationColumnIndex];
    String instrumentCURIE = columnValues[instrumentColumnIndex];

    ProteomicsMeasurementMetadata metadata = new ProteomicsMeasurementMetadata(sampleCodes,
        organisationRoRId, instrumentCURIE);
    MeasurementRegistrationRequest<ProteomicsMeasurementMetadata> registrationRequest = new MeasurementRegistrationRequest<>(
        sampleCodes, metadata);
    return Result.fromValue(registrationRequest);
  }

  private List<MeasurementRegistrationRequest<ProteomicsMeasurementMetadata>> generatePxPRequests(
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

    Integer sampleCodeColumnIndex = propertyColumnMap.get(
        PROTEOMICS_PROPERTY.QBIC_SAMPLE_ID.label());
    Integer oranisationColumnIndex = propertyColumnMap.get(
        PROTEOMICS_PROPERTY.ORGANISATION_ID.label());
    Integer instrumentColumnIndex = propertyColumnMap.get(
        PROTEOMICS_PROPERTY.INSTRUMENT.label());

    int maxPropertyIndex = IntStream.of(sampleCodeColumnIndex, oranisationColumnIndex,
        instrumentColumnIndex).max().orElseThrow();
    if (propertyColumnMap.size() <= maxPropertyIndex) {
      return validationResult.combine(ValidationResult.withFailures(1,
          List.of("Not enough columns provided for row: \"%s\"".formatted(row))));
    }

    List<SampleCode> sampleCodes = parseSampleCode(metaDataValues[sampleCodeColumnIndex]);
    String organisationRoRId = metaDataValues[oranisationColumnIndex];
    String instrumentCURIE = metaDataValues[instrumentColumnIndex];

    ProteomicsMeasurementMetadata metadata = new ProteomicsMeasurementMetadata(sampleCodes,
        organisationRoRId, instrumentCURIE);

    validationResult = validationResult.combine(validationService.validateProteomics(metadata));
    return validationResult;
  }

  private static List<SampleCode> parseSampleCode(String sampleCodeEntry) {
    return Arrays.stream(sampleCodeEntry.split(",")).map(SampleCode::create).toList();
  }

  private void onFileRejected(FileRejectedEvent fileRejectedEvent) {
    //TODO the uploaded file does not match the mime type or the file size is wrong
  }

  private void onUploadSucceeded(SucceededEvent succeededEvent) {
    //TODO successfully received the file, now what?
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    return addListener(CancelEvent.class, listener);
  }

  public Registration addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    return addListener(ConfirmEvent.class, listener);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    if (measurementFileItems.stream()
        .map(MeasurementFileItem::validationReport)
        .map(ValidationReport::validationResult)
        .anyMatch(ValidationResult::containsFailures)) {
      return; //only when all are valid
    }
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(), measurementMetadataUploads));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  record ValidationReport(int validatedRows, ValidationResult validationResult) {

  }

  private record MetadataContent(String header, List<String> rows) {

    Optional<String> theHeader() {
      return Optional.ofNullable(header);
    }
  }

  public record MeasurementMetadataUpload<T extends MeasurementMetadata>(String fileName,
                                                                         List<MeasurementRegistrationRequest<T>> measurementRegistrationRequests) {
    // wir nur hinzugefügt wenn vorher validiert wurde
  }

  public record MeasurementFileItem(String fileName, ValidationReport validationReport) {
    //used to display uploaded files

  }

  /**
   * used to display an uploaded measurement file with validation information
   */
  public static class MeasurementFileDisplay extends Div {

    private static final long serialVersionUID = -9075627206992036067L;
    private final MeasurementFileItem measurementFileItem;


    public MeasurementFileDisplay(MeasurementFileItem measurementFileItem) {
      this.measurementFileItem = requireNonNull(measurementFileItem,
          "measurementFileItem must not be null");
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span fileNameLabel = new Span(fileIcon, new Span(this.measurementFileItem.fileName()));
      fileNameLabel.addClassName("file-name");
      Component displayBox = createDisplayBox(measurementFileItem.validationReport());
      add(fileNameLabel, displayBox);
      addClassName("measurement");
    }

    public MeasurementFileItem measurementFileItem() {
      return measurementFileItem;
    }

    private Component createDisplayBox(ValidationReport validationReport) {
      Div displayBox = new Div(); //TODO
      displayBox.addClassName("display-box");
      return displayBox;
    }

  }

  public void markSuccessful(String filename) {
    //remove MeasurementFileItem
    //remove MeasurementMetadataUpload
  }

  public void showError(String filename, String error) {
    // add error to display of MeasurementFileItem
  }

  public static class ConfirmEvent extends ComponentEvent<MeasurementMetadataUploadDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    private final List<MeasurementMetadataUpload<MeasurementMetadata>> uploads;

    public ConfirmEvent(MeasurementMetadataUploadDialog source, boolean fromClient,
        List<MeasurementMetadataUpload<MeasurementMetadata>> uploads) {
      super(source, fromClient);
      requireNonNull(uploads, "uploads must not be null");
      this.uploads = uploads;
    }

    public List<MeasurementMetadataUpload<MeasurementMetadata>> uploads() {
      return Collections.unmodifiableList(uploads);
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
