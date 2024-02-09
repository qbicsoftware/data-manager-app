package life.qbic.datamanager.views.projects.qualityControl;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.dom.DomEvent;
import elemental.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.EditableMultiFileMemoryBuffer;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.qualityControl.QualityControlDTO;

/**
 * <b>Upload Quality Control Dialog</b>
 * <p>
 * A dialog window that enables uploads of sample QC reports.
 *
 * @since 1.0.0
 */
public class UploadQualityControlDialog extends DialogWindow {

  private static final Logger log = logger(UploadQualityControlDialog.class);
  private static final String VAADIN_FILENAME_EVENT = "event.detail.file.name";
  private static final int MAX_FILE_SIZE_BYTES = 1024 * 1024 * 5; // 14 MiB
  @Serial
  private static final long serialVersionUID = 6602134795666762831L;
  private final Upload upload;
  private EditableMultiFileMemoryBuffer multiFileMemoryBuffer;
  private final Div uploadedQualityControlItems;
  private final List<QualityControlItem> qualityControlItemsCache = new ArrayList<>();
  private final Div uploadedItemsSectionContent;

  public UploadQualityControlDialog() {
    // Vaadin's upload component setup
    multiFileMemoryBuffer = new EditableMultiFileMemoryBuffer();

    upload = new Upload(multiFileMemoryBuffer);
    upload.setAcceptedFileTypes("application/pdf", ".pdf");
    upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);

    setHeaderTitle("Upload a Sample QC Report");
    // Title box configuration
    Span uploadSectionTitle = new Span("Upload the report");
    uploadSectionTitle.addClassName("section-title");

    // Upload restriction display configuration
    Div restrictions = new Div();
    restrictions.addClassName("restrictions");
    restrictions.add(new Span("Supported file formats: PDF, docx, xlsx"));
    restrictions.add(
        new Span("Maximum file size: %s MB".formatted(MAX_FILE_SIZE_BYTES / (1024 * 1024))));
    Div uploadSection = new Div();
    uploadSection.add(uploadSectionTitle, upload, restrictions);

    // Uploaded qualityControl items display configuration
    uploadedQualityControlItems = new Div();
    uploadedQualityControlItems.addClassName("uploaded-quality-control-items");
    uploadedItemsSectionContent = new Div();
    Span uploadedItemsSectionTitle = new Span("Link to an experiment (optional)");
    uploadedItemsSectionTitle.addClassName("section-title");
    uploadedItemsSectionContent.add(uploadedItemsSectionTitle);
    Paragraph uploadedItemsDescription = new Paragraph(
        "Please select the experiment for which the report was generated.");
    uploadedItemsDescription.addClassName("uploaded-items-description");
    uploadedItemsSectionContent.add(uploadedItemsDescription);

    Div uploadedItemsSection = new Div();
    uploadedItemsSection.add(uploadedItemsSectionContent, uploadedQualityControlItems);

    // Add upload QualityControls to the link experiment item section, where users can decide on the linked experiment
    upload.addSucceededListener(this::onUploadSucceeded);

    // Synchronise the Vaadin upload component with the purchase list display
    // When a file is removed  from the upload component, we also want to remove it properly from memory
    // and from any additional display
    upload.getElement().addEventListener("file-remove", this::processClientFileRemoveEvent)
        .addEventData(VAADIN_FILENAME_EVENT);

    // Put the elements together
    add(uploadSection, uploadedItemsSection);
    addClassName("quality-control-upload");
    confirmButton.setText("Save");
    // Init the visibility rendering once
    toggleFileSectionIfEmpty();
  }

  private void onUploadSucceeded(SucceededEvent succeededEvent) {
    var qualityControl = new QualityControlItem(succeededEvent.getFileName());
    uploadedQualityControlItems.add(qualityControl);
    qualityControlItemsCache.add(qualityControl);
    toggleFileSectionIfEmpty();
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new ConfirmEvent(this, true));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  private void processClientFileRemoveEvent(DomEvent event) {
    JsonObject jsonObject = event.getEventData();
    var fileName = jsonObject.getString(VAADIN_FILENAME_EVENT);
    removeFile(fileName);
  }

  private void removeFile(String fileName) {
    removeFileFromBuffer(fileName);
    removeFileFromDisplay(fileName);
    toggleFileSectionIfEmpty();
  }

  private void toggleFileSectionIfEmpty() {
    boolean filesUploaded = !uploadedQualityControlItems.getChildren().toList().isEmpty();
    uploadedQualityControlItems.setVisible(filesUploaded);
    uploadedItemsSectionContent.setVisible(filesUploaded);
  }

  private void removeFileFromBuffer(String fileName) {
    multiFileMemoryBuffer.remove(fileName);
  }

  private void removeFileFromDisplay(String fileName) {
    qualityControlItemsCache.stream()
        .filter(qualityControlItem -> qualityControlItem.fileName().equals(
            fileName)).findAny().ifPresent(qualityControlItem -> {
          qualityControlItemsCache.remove(qualityControlItem);
          uploadedQualityControlItems.remove(qualityControlItem);
        });
  }

  public List<QualityControlDTO> qualityControlItems() {
    return qualityControlItemsCache.stream().map(this::convertToQualityControl).toList();
  }

  private QualityControlDTO convertToQualityControl(QualityControlItem qualityControlItem) {
    try {
      var fileName = qualityControlItem.fileName();
      var experimentId = qualityControlItem.experimentId();
      var content = multiFileMemoryBuffer.inputStream(fileName)
          .orElse(new ByteArrayInputStream(new byte[]{})).readAllBytes();
      return new QualityControlDTO(fileName, experimentId, content);
    } catch (IOException e) {
      throw new ApplicationException("Failed to read quality control content", e);
    }
  }

  public void addConfirmListener(
      ComponentEventListener<ConfirmEvent> listener) {
    addListener(ConfirmEvent.class, listener);
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  @Override
  public void close() {
    emptyCachedBuffer();
    removeContent();
    toggleFileSectionIfEmpty();
    super.close();
  }

  private void removeContent() {
    qualityControlItemsCache.clear();
    uploadedQualityControlItems.removeAll();
    upload.clearFileList();
  }

  private void emptyCachedBuffer() {
    this.multiFileMemoryBuffer.clear();
  }

  public static class ConfirmEvent extends ComponentEvent<UploadQualityControlDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ConfirmEvent(UploadQualityControlDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class CancelEvent extends ComponentEvent<UploadQualityControlDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(UploadQualityControlDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
