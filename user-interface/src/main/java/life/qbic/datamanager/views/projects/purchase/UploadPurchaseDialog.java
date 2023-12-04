package life.qbic.datamanager.views.projects.purchase;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
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
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;
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

  private static final int MAX_FILE_SIZE_BYTES = 1024 * 1024 * 14; // 14 MB

  @Serial
  private static final long serialVersionUID = 6602134795666762831L;
  private final Upload upload;
  private EditableMultiFileMemoryBuffer multiFileMemoryBuffer;
  private Div uploadedPurchaseItems;

  private List<PurchaseItem> purchaseItemsCache = new ArrayList<>();

  private Div disclaimer;

  private Div titleBox;

  private List<ComponentEventListener<ConfirmEvent<UploadPurchaseDialog>>> confirmListeners = new ArrayList<>();

  public UploadPurchaseDialog() {
    // Vaadin's upload component setup
    multiFileMemoryBuffer = new EditableMultiFileMemoryBuffer();
    upload = new Upload(multiFileMemoryBuffer);
    upload.setAcceptedFileTypes("application/pdf", ".pdf");
    upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);

    // Title box configuration
    titleBox = new Div();
    var title = new Span("Upload your offer");
    title.addClassName("title");
    titleBox.add(title);
    titleBox.addClassName("title-box");

    // Upload restriction display configuration
    var restrictions = new Div();
    restrictions.addClassName("restrictions");
    restrictions.add(new Span("Accepted file formats: PDF (.pdf)"));
    restrictions.add(
        new Span("Maximum file size: %s MB".formatted(MAX_FILE_SIZE_BYTES / (1024 * 1024))));

    // Uploaded purchase items display configuration
    uploadedPurchaseItems = new Div();
    uploadedPurchaseItems.addClassName("uploaded-documents");
    disclaimer = new Div();
    disclaimer.addClassName("instructions");
    disclaimer.add(new Span("Set offer signed status"));
    disclaimer.add(new Paragraph("Please tick the checkbox if the offer is signed."));

    // Add upload offers to the purchase item section, where users can set the signed flag
    upload.addSucceededListener(this::onUploadSucceeded);

    // Synchronise the Vaadin upload component with the purchase list display
    // When a file is removed  from the upload component, we also want to remove it properly from memory
    // and from any additional display
    upload.getElement().addEventListener("file-remove", this::processClientFileRemoveEvent)
        .addEventData(VAADIN_FILENAME_EVENT);

    // Put the elements together
    add(titleBox, upload, restrictions, disclaimer, uploadedPurchaseItems);
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

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    fireConfirmEvent();
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {

  }

  private void fireConfirmEvent() {
    var confirmEvent = new ConfirmEvent<>(this, true);
    confirmListeners.forEach(listener -> listener.onComponentEvent(confirmEvent));
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
    boolean filesUploaded = uploadedPurchaseItems.getChildren().toList().isEmpty();
    uploadedPurchaseItems.setVisible(filesUploaded);
    disclaimer.setVisible(filesUploaded);
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
      log.error("Failed to read uploaded purchase item content", e);
      throw new ApplicationException("Failed to read offer content");
    }
  }

  public void addConfirmListener(
      ComponentEventListener<ConfirmEvent<UploadPurchaseDialog>> listener) {
    this.confirmListeners.add(listener);
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
}
