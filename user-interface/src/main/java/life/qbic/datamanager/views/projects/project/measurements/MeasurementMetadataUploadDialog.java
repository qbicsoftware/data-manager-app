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
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.shared.Registration;
import elemental.json.JsonObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.IntStream;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.CancelConfirmationNotificationDialog;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.WizardDialogWindow;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.EditableMultiFileMemoryBuffer;
import life.qbic.projectmanagement.application.measurement.Labeling;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementNGSValidator.NGS_PROPERTY;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementProteomicsValidator.PROTEOMICS_PROPERTY;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.measurement.validation.ValidationResult;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
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
  private static final String VAADIN_FILENAME_EVENT = "event.detail.file.name";
  private final MeasurementValidationService measurementValidationService;
  private final EditableMultiFileMemoryBuffer uploadBuffer;
  private final transient List<MeasurementMetadataUpload<MeasurementMetadata>> measurementMetadataUploads;
  private final transient List<MeasurementFileItem> measurementFileItems;
  private final MODE mode;
  private final ProjectId projectId;
  private final UploadProgressDisplay uploadProgressDisplay;
  private final UploadItemsDisplay uploadItemsDisplay;

  public MeasurementMetadataUploadDialog(MeasurementValidationService measurementValidationService,
      MODE mode, ProjectId projectId) {
    this.projectId = requireNonNull(projectId, "projectId cannot be null");
    this.measurementValidationService = requireNonNull(measurementValidationService,
        "measurementValidationExecutor must not be null");
    this.mode = requireNonNull(mode,
        "The dialog mode needs to be defined");
    specifyCancelShortcuts(this::onCanceled);

    this.uploadBuffer = new EditableMultiFileMemoryBuffer();
    this.measurementMetadataUploads = new ArrayList<>();
    this.measurementFileItems = new ArrayList<>();
    Upload upload = new Upload(uploadBuffer);
    upload.setAcceptedFileTypes("text/tab-separated-values", "text/plain");
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

    // Synchronise the Vaadin upload component with the purchase list display
    // When a file is removed from the upload component, we also want to remove it properly from memory
    // and from any additional display
    upload.getElement().addEventListener("file-remove", this::onFileRemoved)
        .addEventData(VAADIN_FILENAME_EVENT);
    addClassName("measurement-upload-dialog");

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
    var content = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_16)).lines().toList();

    return new MetadataContent(content.isEmpty() ? null : content.get(0),
        content.size() > 1 ? content.subList(1, content.size()) : new ArrayList<>());
  }

  private static boolean isRowNotEmpty(String row) {
    return row.split("\t").length > 0;
  }

  private static Result<NGSMeasurementMetadata, String> generateNGSRequest(
      String row, Map<String, Integer> columns) {
    var columnValues = row.split("\t"); // tab separated values
    // we consider an empty row as a reason to warn, not to fail
    if (columnValues.length == 0) {
      return Result.fromValue(null);
    }

    Integer measurementIdIndex = columns.getOrDefault(MeasurementProperty.MEASUREMENT_ID.label(),
        -1);
    Integer sampleCodeColumnIndex = columns.get(NGS_PROPERTY.QBIC_SAMPLE_ID.label());
    Integer organisationColumnIndex = columns.get(NGS_PROPERTY.ORGANISATION_ID.label());
    Integer instrumentColumnIndex = columns.get(NGS_PROPERTY.INSTRUMENT.label());
    Integer facilityIndex = columns.get(NGS_PROPERTY.FACILITY.label());
    Integer readTypeIndex = columns.get(NGS_PROPERTY.SEQUENCING_READ_TYPE.label());
    Integer libraryKitIndex = columns.get(NGS_PROPERTY.LIBRARY_KIT.label());
    Integer flowCellIndex = columns.get(NGS_PROPERTY.FLOW_CELL.label());
    Integer runProtocolIndex = columns.get(NGS_PROPERTY.SEQUENCING_RUN_PROTOCOL.label());
    Integer samplePoolIndex = columns.get(NGS_PROPERTY.SAMPLE_POOL_GROUP.label());
    Integer indexI7Index = columns.get(NGS_PROPERTY.INDEX_I7.label());
    Integer indexI5Index = columns.get(NGS_PROPERTY.INDEX_I5.label());
    Integer commentIndex = columns.get(NGS_PROPERTY.COMMENT.label());

    int maxPropertyIndex = IntStream.of(sampleCodeColumnIndex,
            organisationColumnIndex,
            instrumentColumnIndex)
        .max().orElseThrow();
    if (columns.size() <= maxPropertyIndex) {
      return Result.fromError("Not enough columns provided for row: %s".formatted(row));
    }

    String measurementId = safeArrayAccess(columnValues, measurementIdIndex).orElse("");
    List<SampleCode> sampleCodes = List.of(
        SampleCode.create(safeArrayAccess(columnValues, sampleCodeColumnIndex).orElse("")));

    String organisationRoRId = safeArrayAccess(columnValues, organisationColumnIndex).orElse("");
    String instrumentCURIE = safeArrayAccess(columnValues, instrumentColumnIndex).orElse("");
    String facility = safeArrayAccess(columnValues, facilityIndex).orElse("");
    String readType = safeArrayAccess(columnValues, readTypeIndex).orElse("");
    String libraryKit = safeArrayAccess(columnValues, libraryKitIndex).orElse("");
    String flowCell = safeArrayAccess(columnValues, flowCellIndex).orElse("");
    String runProtocol = safeArrayAccess(columnValues, runProtocolIndex).orElse("");
    String samplePool = safeArrayAccess(columnValues, samplePoolIndex).orElse("");
    String indexI7 = safeArrayAccess(columnValues, indexI7Index).orElse("");
    String indexI5 = safeArrayAccess(columnValues, indexI5Index).orElse("");
    String comment = safeArrayAccess(columnValues, commentIndex).orElse("");
    NGSMeasurementMetadata metadata = new NGSMeasurementMetadata(measurementId, sampleCodes,
        organisationRoRId, instrumentCURIE, facility, readType,
        libraryKit, flowCell, runProtocol, samplePool, indexI7, indexI5, comment);
    return Result.fromValue(metadata);
  }

  private static Result<ProteomicsMeasurementMetadata, String> generatePxPRequest(
      String row, Map<String, Integer> columns) {
    var columnValues = row.split("\t"); // tab separated values
    // we consider an empty row as a reason to warn, not to fail
    if (columnValues.length == 0) {
      return Result.fromValue(null);
    }

    Integer measurementIdIndex = columns.getOrDefault(MeasurementProperty.MEASUREMENT_ID.label(),
        -1);
    Integer sampleCodeColumnIndex = columns.get(PROTEOMICS_PROPERTY.QBIC_SAMPLE_ID.label());
    Integer organisationColumnIndex = columns.get(PROTEOMICS_PROPERTY.ORGANISATION_ID.label());
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
            organisationColumnIndex,
            instrumentColumnIndex)
        .max().orElseThrow();
    if (columns.size() <= maxPropertyIndex) {
      return Result.fromError("Not enough columns provided for row: %s".formatted(row));
    }

    String measurementId = safeArrayAccess(columnValues, measurementIdIndex).orElse("");
    SampleCode sampleCode = SampleCode.create(
        safeArrayAccess(columnValues, sampleCodeColumnIndex).orElse(""));
    String organisationRoRId = safeArrayAccess(columnValues, organisationColumnIndex).orElse("");
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

    ProteomicsMeasurementMetadata metadata = new ProteomicsMeasurementMetadata(measurementId,
        sampleCode,
        organisationRoRId, instrumentCURIE, samplePoolGroup, facility, fractionName,
        digestionEnzyme,
        digestionMethod, enrichmentMethod, injectionVolume, lcColumn, lcmsMethod,
        new Labeling(labelingType, label), note);
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

  private void onFileRemoved(DomEvent domEvent) {
    JsonObject jsonObject = domEvent.getEventData();
    var fileName = jsonObject.getString(VAADIN_FILENAME_EVENT);
    removeFile(fileName);

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

  private void onUploadSucceeded(SucceededEvent succeededEvent) {
    MetadataContent content = read(
        uploadBuffer.inputStream(succeededEvent.getFileName()).orElseThrow());
    var contentHeader = content.theHeader()
        .orElseThrow(() -> new RuntimeException("No header row found"));
    var domain = measurementValidationService.inferDomainByPropertyTypes(
            parseHeaderContent(contentHeader))
        .orElseThrow(() -> new RuntimeException(
            "Header row could not be recognized, Please provide a valid template file"));
    var validationReport = switch (domain) {
      case PROTEOMICS -> validatePxP(content);
      case NGS -> validateNGS(content);
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
        case NGS -> generateNGSMetadata(content);
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
  }

  private List<NGSMeasurementMetadata> generateNGSMetadata(
      MetadataContent content) {
    var propertyColumnMap = propertyColumnMap(parseHeaderContent(content.header()));

    var results = content.rows().stream()
        .map(row -> generateNGSRequest(row, propertyColumnMap))
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

  private MeasurementValidationReport validateNGS(MetadataContent content) {
    var validationResult = ValidationResult.successful(0);
    var propertyColumnMap = propertyColumnMap(parseHeaderContent(content.header()));
    // we check if there are any rows provided or if we have only rows with empty content
    if (content.rows().isEmpty() || content.rows().stream()
        .noneMatch(MeasurementMetadataUploadDialog::isRowNotEmpty)) {
      validationResult = validationResult.combine(
          ValidationResult.withFailures(0,
              List.of("The metadata sheet seems to be empty")));
      return new MeasurementValidationReport(0, validationResult);
    }
    ConcurrentLinkedDeque<ValidationResult> concurrentLinkedDeque = new ConcurrentLinkedDeque<>();
    List<CompletableFuture<Void>> tasks = new ArrayList<>();
    for (String row : content.rows().stream()
        .filter(MeasurementMetadataUploadDialog::isRowNotEmpty).toList()) {
      tasks.add(validateNGSRow(propertyColumnMap, row).thenAccept(concurrentLinkedDeque::add));
    }

    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

    return new MeasurementValidationReport(concurrentLinkedDeque.size(),
        concurrentLinkedDeque.stream().reduce(
            validationResult, ValidationResult::combine));
  }

  private MeasurementValidationReport validatePxP(MetadataContent content) {

    var validationResult = ValidationResult.successful(0);
    var propertyColumnMap = propertyColumnMap(parseHeaderContent(content.header()));
    // we check if there are any rows provided or if we have only rows with empty content
    if (content.rows().isEmpty() || content.rows().stream()
        .noneMatch(MeasurementMetadataUploadDialog::isRowNotEmpty)) {
      validationResult = validationResult.combine(
          ValidationResult.withFailures(0,
              List.of("The metadata sheet seems to be empty")));
      return new MeasurementValidationReport(0, validationResult);
    }

    ConcurrentLinkedDeque<ValidationResult> concurrentLinkedDeque = new ConcurrentLinkedDeque<>();
    List<CompletableFuture<Void>> tasks = new ArrayList<>();
    for (String row : content.rows().stream()
        .filter(MeasurementMetadataUploadDialog::isRowNotEmpty).toList()) {
      tasks.add(validatePxPRow(propertyColumnMap, row).thenAccept(concurrentLinkedDeque::add));
    }

    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

    return new MeasurementValidationReport(concurrentLinkedDeque.size(),
        concurrentLinkedDeque.stream().reduce(
            validationResult, ValidationResult::combine));
  }

  private CompletableFuture<ValidationResult> validateNGSRow(Map<String, Integer> propertyColumnMap,
      String row) {
    var validationResult = ValidationResult.successful(0);
    var metaDataValues = row.split("\t"); // tab separated values
    // we consider an empty row as a reason to warn, not to fail
    if (metaDataValues.length == 0) {
      validationResult.combine(
          ValidationResult.successful(1, List.of("Empty row provided.")));
      return CompletableFuture.supplyAsync(() -> validationResult);
    }
    if (metaDataValues.length != propertyColumnMap.keySet().size()) {
      validationResult.combine(ValidationResult.withFailures(1, List.of("")));
    }
    var measurementIdIndex = propertyColumnMap.getOrDefault(
        MeasurementProperty.MEASUREMENT_ID.label(), -1);
    var sampleCodeColumnIndex = propertyColumnMap.get(
        NGS_PROPERTY.QBIC_SAMPLE_ID.label());
    var organisationsColumnIndex = propertyColumnMap.get(
        NGS_PROPERTY.ORGANISATION_ID.label());
    var facilityIndex = propertyColumnMap.get(NGS_PROPERTY.FACILITY.label());
    var instrumentColumnIndex = propertyColumnMap.get(
        NGS_PROPERTY.INSTRUMENT.label());
    var sequencingReadTypeIndex = propertyColumnMap.get(
        NGS_PROPERTY.SEQUENCING_READ_TYPE.label());
    var libraryKitIndex = propertyColumnMap.get(
        NGS_PROPERTY.LIBRARY_KIT.label());
    var flowCellIndex = propertyColumnMap.get(
        NGS_PROPERTY.FLOW_CELL.label());
    var sequencingRunProtocolIndex = propertyColumnMap.get(
        NGS_PROPERTY.SEQUENCING_RUN_PROTOCOL.label());
    var samplePoolIndex = propertyColumnMap.get(
        NGS_PROPERTY.SAMPLE_POOL_GROUP.label());
    var indexI7Index = propertyColumnMap.get(
        NGS_PROPERTY.INDEX_I7.label());
    var indexI5Index = propertyColumnMap.get(
        NGS_PROPERTY.INDEX_I5.label());
    Integer commentIndex = propertyColumnMap.get(NGS_PROPERTY.COMMENT.label());
    int maxPropertyIndex = IntStream.of(sampleCodeColumnIndex, organisationsColumnIndex,
        instrumentColumnIndex).max().orElseThrow();
    if (propertyColumnMap.size() <= maxPropertyIndex) {
      return CompletableFuture.supplyAsync(
          () -> validationResult.combine(ValidationResult.withFailures(1,
              List.of("Not enough columns provided for row: \"%s\"".formatted(row)))));
    }
    var measurementId = safeArrayAccess(metaDataValues, measurementIdIndex).orElse("");
    var sampleCodes = SampleCode.create(
        safeArrayAccess(metaDataValues, sampleCodeColumnIndex).orElse(""));
    var organisationRoRId = safeArrayAccess(metaDataValues, organisationsColumnIndex).orElse("");
    var instrumentCURIE = safeArrayAccess(metaDataValues, instrumentColumnIndex).orElse("");
    var facility = safeArrayAccess(metaDataValues, facilityIndex).orElse("");
    var sequencingReadType = safeArrayAccess(metaDataValues, sequencingReadTypeIndex).orElse("");
    var libraryKit = safeArrayAccess(metaDataValues, libraryKitIndex).orElse("");
    var flowCell = safeArrayAccess(metaDataValues, flowCellIndex).orElse("");
    var sequencingRunProtocol = safeArrayAccess(metaDataValues, sequencingRunProtocolIndex).orElse(
        "");
    var samplePoolGroup = safeArrayAccess(metaDataValues, samplePoolIndex).orElse("");
    var indexI7 = safeArrayAccess(metaDataValues, indexI7Index).orElse("");
    var indexI5 = safeArrayAccess(metaDataValues, indexI5Index).orElse("");
    var comment = safeArrayAccess(metaDataValues, commentIndex).orElse("");

    var metadata = new NGSMeasurementMetadata(measurementId, List.of(sampleCodes),
        organisationRoRId, instrumentCURIE, facility, sequencingReadType,
        libraryKit, flowCell, sequencingRunProtocol, samplePoolGroup, indexI7, indexI5, comment);
    var measurementNGSValidationExecutor = new MeasurementNGSValidationExecutor(
        measurementValidationService);
    return generateModeDependentValidationResult(
        measurementNGSValidationExecutor, metadata);
  }

  private CompletableFuture<ValidationResult> validatePxPRow(Map<String, Integer> propertyColumnMap,
      String row) {
    var validationResult = ValidationResult.successful(0);
    var metaDataValues = row.split("\t"); // tab separated values
    // we consider an empty row as a reason to warn, not to fail
    if (metaDataValues.length == 0) {
      validationResult.combine(
          ValidationResult.successful(1, List.of("Empty row provided.")));
      return CompletableFuture.supplyAsync(() -> validationResult);
    }
    if (metaDataValues.length != propertyColumnMap.keySet().size()) {
      validationResult.combine(ValidationResult.withFailures(1, List.of("")));
    }

    var measurementIdIndex = propertyColumnMap.getOrDefault(
        MeasurementProperty.MEASUREMENT_ID.label(), -1);
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
      return CompletableFuture.completedFuture(
          validationResult.combine(ValidationResult.withFailures(1,
              List.of("Not enough columns provided for row: \"%s\"".formatted(row)))));
    }

    var measurementId = safeArrayAccess(metaDataValues, measurementIdIndex).orElse("");
    var sampleCode = SampleCode.create(
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

    var metadata = new ProteomicsMeasurementMetadata(measurementId, sampleCode,
        organisationRoRId, instrumentCURIE, samplePoolGroup, facility, fractionName,
        digestionEnzyme,
        digestionMethod, enrichmentMethod, injectionVolume, lcColumn, lcmsMethod,
        new Labeling(labelingType, label), note);
    var measurementProteomicsValidationExecutor = new MeasurementProteomicsValidationExecutor(
        measurementValidationService);
    var finalValidationResult = generateModeDependentValidationResult(
        measurementProteomicsValidationExecutor, metadata);
    return finalValidationResult;
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
    CancelConfirmationNotificationDialog cancelDialog = new CancelConfirmationNotificationDialog()
        .withBodyText("Uploaded information has not yet been saved.")
        .withConfirmText("Discard upload")
        .withTitle("Discard uploaded information?");
    cancelDialog.open();
    cancelDialog.addConfirmListener(event -> {
      cancelDialog.close();
      fireEvent(new CancelEvent(this, true));
    });
    cancelDialog.addCancelListener(
        event -> cancelDialog.close());
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
              "Please save your excel file as UTF-16 Unicode Text (*.txt) before uploading.")
          .setClosable(false);

      var restrictions = new Div();
      restrictions.addClassName("restrictions");
      restrictions.add(new Span("Supported file formats: .txt, .tsv"));
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

      Objects.requireNonNull(mode, "Mode cannot be null");
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
