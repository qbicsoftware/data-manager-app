package life.qbic.datamanager.views.projects.purchase;

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
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.CancelConfirmationNotificationDialog;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.EditableMultiFileMemoryBuffer;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.purchase.OfferDTO;

/**
 * <b>Upload Purchase Dialog</b>
 * <p>
 * A dialog window that enables uploads of purchase items such as an offer.
 *
 * @since 1.0.0
 */
public class UploadPurchaseDialog extends DialogWindow {

  private static final Logger log = logger(UploadPurchaseDialog.class);
  private static final String VAADIN_FILENAME_EVENT = "event.detail.file.name";
  private static final int MAX_FILE_SIZE_BYTES = 1024 * 1024 * 16; // 16 MiB
  @Serial
  private static final long serialVersionUID = 6602134795666762831L;
  private final Upload upload;
  private final EditableMultiFileMemoryBuffer multiFileMemoryBuffer;
  private final Div uploadedPurchaseItems;
  private final List<PurchaseItem> purchaseItemsCache = new ArrayList<>();
  private final Div uploadedItemsSectionContent;

  public UploadPurchaseDialog() {
    initCancelShortcuts(this::onCanceled);

    // Vaadin's upload component setup
    multiFileMemoryBuffer = new EditableMultiFileMemoryBuffer();

    upload = new Upload(multiFileMemoryBuffer);
    upload.setAcceptedFileTypes("application/pdf", ".pdf");
    upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);

    setHeaderTitle("Upload an offer");
    // Title box configuration
    Span uploadSectionTitle = new Span("Upload your offer");
    uploadSectionTitle.addClassName("section-title");

    // Upload restriction display configuration
    Div restrictions = new Div();
    restrictions.addClassName("restrictions");
    restrictions.add(new Span("Accepted file formats: PDF (.pdf)"));
    restrictions.add(
        new Span("Maximum file size: %s MB".formatted(MAX_FILE_SIZE_BYTES / (1024 * 1024))));

    Div uploadSection = new Div();
    uploadSection.add(uploadSectionTitle, upload, restrictions);

    // Uploaded purchase items display configuration
    uploadedPurchaseItems = new Div();
    uploadedPurchaseItems.addClassName("uploaded-purchase-items");
    uploadedItemsSectionContent = new Div();
    Span uploadedItemsSectionTitle = new Span("Set offer signed status");
    uploadedItemsSectionTitle.addClassName("section-title");
    uploadedItemsSectionContent.add(uploadedItemsSectionTitle);
    Paragraph uploadedItemsDescription = new Paragraph(
        "Please tick the checkbox if the offer is signed.");
    uploadedItemsDescription.addClassName("uploaded-items-description");
    uploadedItemsSectionContent.add(uploadedItemsDescription);

    Div uploadedItemsSection = new Div();
    uploadedItemsSection.add(uploadedItemsSectionContent, uploadedPurchaseItems);

    // Add upload offers to the purchase item section, where users can set the signed flag
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
    addClassName("purchase-item-upload");
    confirmButton.setText("Save");
    // Init the visibility rendering once
    toggleFileSectionIfEmpty();
  }

  private void onUploadSucceeded(SucceededEvent succeededEvent) {
    var purchase = new PurchaseItem(succeededEvent.getFileName());
    uploadedPurchaseItems.add(purchase);
    purchaseItemsCache.add(purchase);
    toggleFileSectionIfEmpty();
  }

  private void onUploadFailure(ComponentEvent<Upload> event) {
    ErrorMessage errorMessage = new ErrorMessage("Offer upload failed",
        "An unknown exception has occurred");
    if (event instanceof FileRejectedEvent) {
      errorMessage.descriptionTextSpan.setText(
          "Please provide a file within the file limit of %s MB".formatted(
              MAX_FILE_SIZE_BYTES / (1024 * 1024)));
    } else if (event instanceof FailedEvent) {
      errorMessage.descriptionTextSpan.setText("Offer upload was interrupted, please try again");
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
        .withTitle("Discard Offer uploads?");
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
    boolean filesUploaded = !uploadedPurchaseItems.getChildren().toList().isEmpty();
    uploadedPurchaseItems.setVisible(filesUploaded);
    uploadedItemsSectionContent.setVisible(filesUploaded);
  }

  private void removeFileFromBuffer(String fileName) {
    multiFileMemoryBuffer.remove(fileName);
  }

  private void removeFileFromDisplay(String fileName) {
    purchaseItemsCache.stream().filter(purchaseItem -> purchaseItem.fileName().equals(
        fileName)).findAny().ifPresent(purchaseItem -> {
      purchaseItemsCache.remove(purchaseItem);
      uploadedPurchaseItems.remove(purchaseItem);
    });
  }

  public List<OfferDTO> purchaseItems() {
    return purchaseItemsCache.stream().map(this::convertToOffer).toList();
  }

  private OfferDTO convertToOffer(PurchaseItem purchaseItem) {
    try {
      var fileName = purchaseItem.fileName();
      var content = multiFileMemoryBuffer.inputStream(fileName)
          .orElse(new ByteArrayInputStream(new byte[]{})).readAllBytes();
      return new OfferDTO(purchaseItem.isSigned(), fileName, content);
    } catch (IOException e) {
      throw new ApplicationException("Failed to read offer content", e);
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
    this.purchaseItemsCache.clear();
    this.uploadedPurchaseItems.removeAll();
    this.upload.clearFileList();
  }

  private void emptyCachedBuffer() {
    this.multiFileMemoryBuffer.clear();
  }

  public static class ConfirmEvent extends ComponentEvent<UploadPurchaseDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ConfirmEvent(UploadPurchaseDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class CancelEvent extends ComponentEvent<UploadPurchaseDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(UploadPurchaseDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
