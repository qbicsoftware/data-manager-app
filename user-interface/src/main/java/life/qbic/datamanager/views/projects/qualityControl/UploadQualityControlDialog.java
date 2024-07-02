package life.qbic.datamanager.views.projects.qualityControl;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.FailedEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.dom.DomEvent;
import elemental.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.CancelConfirmationNotificationDialog;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.EditableMultiFileMemoryBuffer;
import life.qbic.datamanager.views.projects.qualityControl.QualityControlItem.ExperimentItem;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.qualitycontrol.QualityControlReport;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Upload Quality Control Dialog</b>
 * <p>
 * A dialog window that enables uploads of sample quality control reports.
 */
public class UploadQualityControlDialog extends DialogWindow {

  private static final Logger log = logger(UploadQualityControlDialog.class);
  private static final String VAADIN_FILENAME_EVENT = "event.detail.file.name";
  private static final int MAX_FILE_SIZE_BYTES = 1024 * 1024 * 16; // 17 MiB
  @Serial
  private static final long serialVersionUID = 6602134795666762831L;
  private final Upload upload;
  private final EditableMultiFileMemoryBuffer multiFileMemoryBuffer;
  private final Div uploadedQualityControlItems;
  private final List<QualityControlItem> qualityControlItemsCache = new ArrayList<>();
  private final Div uploadedItemsSectionContent;
  private final List<ExperimentItem> selectableExperimentsForProject;

  public UploadQualityControlDialog(ProjectId projectId,
      ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(experimentInformationService,
        "experiment information service must not be null");
    initCancelShortcuts(this::onCanceled);

    //Load selectable Experiments for Sample quality control items;
    selectableExperimentsForProject = experimentInformationService.findAllForProject(projectId)
        .stream()
        .map(experiment -> new ExperimentItem(experiment.experimentId(), experiment.getName()))
        .toList();

    // Vaadin's upload component setup
    multiFileMemoryBuffer = new EditableMultiFileMemoryBuffer();
    upload = new Upload(multiFileMemoryBuffer);
    upload.setAcceptedFileTypes(AllowedFileExtension.PDF.extension(),
        AllowedFileExtension.PDF.mimetype(),
        AllowedFileExtension.EXCEL.extension(), AllowedFileExtension.EXCEL.mimetype(),
        AllowedFileExtension.WORD.extension(), AllowedFileExtension.WORD.mimetype());
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

    // Show notification if user provides invalid file
    upload.addFailedListener(this::onUploadFailure);
    upload.addFileRejectedListener(this::onUploadFailure);

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
    var qualityControl = new QualityControlItem(succeededEvent.getFileName(),
        selectableExperimentsForProject);
    uploadedQualityControlItems.add(qualityControl);
    qualityControlItemsCache.add(qualityControl);
    toggleFileSectionIfEmpty();
  }

  private void onUploadFailure(ComponentEvent<Upload> event) {
    ErrorMessage errorMessage = new ErrorMessage("Quality Control upload failed",
        "An unknown exception has occurred");
    if (event instanceof FileRejectedEvent) {
      errorMessage.descriptionTextSpan.setText(
          "Please provide a file within the file limit of %s MB".formatted(
              MAX_FILE_SIZE_BYTES / (1024 * 1024)));
    } else if (event instanceof FailedEvent) {
      errorMessage.descriptionTextSpan.setText(
          "Quality control upload was interrupted, please try again");
    }
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new ConfirmEvent(this, true));
  }

  private void onCanceled() {
    CancelConfirmationNotificationDialog cancelDialog = new CancelConfirmationNotificationDialog()
        .withBodyText("Uploads were not yet saved.")
        .withConfirmText("Discard uploads")
        .withTitle("Discard Quality Control uploads?");
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

  public List<QualityControlReport> qualityControlItems() {
    return qualityControlItemsCache.stream().map(this::convertToQualityControl).toList();
  }

  private QualityControlReport convertToQualityControl(QualityControlItem qualityControlItem) {
    try {
      var fileName = qualityControlItem.fileName();
      var experimentId = qualityControlItem.experimentId();
      var content = multiFileMemoryBuffer.inputStream(fileName)
          .orElse(new ByteArrayInputStream(new byte[]{})).readAllBytes();
      return new QualityControlReport(fileName, experimentId, content);
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


  /**
   * <b>Allowed File Extension</b>
   *
   * <p>Enumeration of all allowed File extensions in the context of the Quality Control upload.
   * Additionally provides the MIME_type and a short description as outlined by the mozilla docs <a
   * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types">...</a>
   * </p>
   */
  private enum AllowedFileExtension {

    EXCEL(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "Microsoft Excel (OpenXML)"),
    PDF(".pdf", "application/pdf",
        "Adobe Portable Document Format (PDF)"),
    WORD(
        ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "Microsoft Word (OpenXML)");

    private final String extension;
    private final String mimeType;
    private final String description;


    AllowedFileExtension(String extension, String mimeType, String description) {
      this.extension = extension;
      this.mimeType = mimeType;
      this.description = description;
    }

    public String extension() {
      return extension;
    }

    public String mimetype() {
      return mimeType;
    }

    public String description() {
      return description;
    }
  }
}
