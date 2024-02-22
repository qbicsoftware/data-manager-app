package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.FailedEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.FinishedEvent;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.shared.Registration;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.EditableMultiFileMemoryBuffer;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
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

  public static final int MAX_FILE_SIZE_BYTES = (int) (Math.pow(1024, 2) * 5);
  private final MeasurementService measurementService;
  private final ValidationService validationService;

  private final EditableMultiFileMemoryBuffer uploadBuffer;

  public MeasurementMetadataUploadDialog(MeasurementService measurementService,
      ValidationService validationService) {
    this.validationService = validationService;
    this.measurementService = measurementService;
    this.uploadBuffer = new EditableMultiFileMemoryBuffer();
    var upload = new Upload(uploadBuffer);
    upload.setMaxFiles(1);
    upload.setAcceptedFileTypes(AcceptedFormats.TSV.mimeType(), AcceptedFormats.TXT.mimeType());
    upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);

    setHeaderTitle("Register Measurements");
    confirmButton.setText("Save");

    var uploadSectionTitle = new Span("Upload the measurement data");
    uploadSectionTitle.addClassName("section-title");

    var restrictions = new Div();
    restrictions.addClassName("restrictions"); //TODO CSS
    restrictions.add(new Span(
        "Accepted file formats: %s (%s)".formatted(AcceptedFormats.TSV.commonlyKnownAs(),
            AcceptedFormats.TSV.extensions())));
    restrictions.add("Maximum file size: %s MB".formatted(MAX_FILE_SIZE_BYTES / Math.pow(1024, 2)));

    var uploadSection = new Div();
    uploadSection.add(uploadSectionTitle, upload, restrictions);

    var uploadedFilesSection = new Div();
    uploadedFilesSection.addClassName("uploaded-items"); //TODO CSS

    var uploadedFilesSectionTitle = new Span("Uploaded files");
    uploadedFilesSectionTitle.addClassName("section-title");
    uploadedFilesSection.add(uploadedFilesSectionTitle);

    upload.addSucceededListener(this::onUploadSucceeded);
    upload.addFileRejectedListener(this::onFileRejected);
    upload.addFailedListener(this::onUploadFailed);
    upload.addFinishedListener(this::onUploadFinished);

    add(uploadSection, uploadedFilesSection);
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

  private void onUploadFailed(FailedEvent failedEvent) {
    //TODO what happens if the upload failed
  }

  private void onUploadFinished(FinishedEvent finishedEvent) {
    MetadataContent content = read(
        uploadBuffer.inputStream(finishedEvent.getFileName()).orElseThrow());
    if (content.theHeader().isEmpty()) {
      throw new RuntimeException("No header row found");
    }
    var domain = validationService.inferDomainByPropertyTypes(
            parseHeaderContent(content.theHeader().get()))
        .orElseThrow();

    switch (domain) {
      case PROTEOMICS -> validatePxP(content);
      case NGS -> validateNGS();
    }

  }

  private Optional<String> extractHeaderRow(List<String> content) {
    return content.isEmpty() ? Optional.empty() : Optional.ofNullable(content.get(0));
  }

  private void validateNGS() {

  }

  private ValidationResult validatePxP(MetadataContent content) {
    var validationResult = ValidationResult.successful(0);
    var propertyColumnMap = propertyColumnMap(parseHeaderContent(content.header()));
    if (content.rows().isEmpty()) {
      validationResult = validationResult.combine(
          ValidationResult.withFailures(1, List.of("The metadata sheet seems to be empty")));
    }
    for (String row : content.rows()) {
      var metaDataValues = row.split("/t");
      ProteomicsMeasurementMetadata metadata = null;
      try {
        metadata = new ProteomicsMeasurementMetadata(
            List.of(SampleCode.create(metaDataValues[propertyColumnMap.get(
                PROTEOMICS_PROPERTY.QBIC_SAMPLE_ID.label())])),
            metaDataValues[propertyColumnMap.get(PROTEOMICS_PROPERTY.ORGANISATION_ID.label())]);
        validationResult = validationResult.combine(validationService.validateProteomics(metadata));
      } catch (IndexOutOfBoundsException e) {
        validationResult = validationResult.combine(ValidationResult.withFailures(1,
            List.of("Not enough columns provided for row: \"%s\"".formatted(row))));
      }
    }
    return validationResult;
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
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient()));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  private enum AcceptedFormats {
    TSV("text/tab-separated-values", List.of(".tsv", ".TSV"), "TSV"),
    TXT("text/plain", List.of(".txt", ".TXT"), "TXT");
    private final String mimeType;
    private final List<String> extension;
    private final String commonlyKnownAs;

    AcceptedFormats(String mimeType, List<String> extensions, String commonlyKnownAs) {
      this.mimeType = mimeType;
      this.extension = extensions;
      this.commonlyKnownAs = commonlyKnownAs;
    }

    public String mimeType() {
      return mimeType;
    }

    public String extensions() {
      return String.join(",", extension);
    }

    public String commonlyKnownAs() {
      return commonlyKnownAs;
    }
  }

  private record MetadataContent(String header, List<String> rows) {

    Optional<String> theHeader() {
      return Optional.ofNullable(header);
    }
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
    public ConfirmEvent(MeasurementMetadataUploadDialog source, boolean fromClient) {
      super(source, fromClient);
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
