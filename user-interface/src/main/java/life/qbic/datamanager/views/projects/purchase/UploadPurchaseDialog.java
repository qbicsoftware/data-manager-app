package life.qbic.datamanager.views.projects.purchase;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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

  private List<PurchaseItem> purchaseItemsCache;

  private Div disclaimer;

  private Div titleBox;

  private List<ComponentEventListener<ConfirmEvent<UploadPurchaseDialog>>> confirmListeners = new ArrayList<>();

  public UploadPurchaseDialog() {
    multiFileMemoryBuffer = new EditableMultiFileMemoryBuffer();
    upload = new Upload(multiFileMemoryBuffer);
    upload.setAcceptedFileTypes("application/pdf", ".pdf");
    upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);

    purchaseItemsCache = new ArrayList<>();
    titleBox = new Div();
    var title = new Span("Upload your offer");
    title.addClassName("title");
    titleBox.add(title);
    titleBox.addClassName("title-box");

    var restrictions = new Div();
    restrictions.addClassName("restrictions");
    restrictions.add(new Span("Accepted file formats: PDF (.pdf)"));
    restrictions.add(
        new Span("Maximum file size: %s MB".formatted(MAX_FILE_SIZE_BYTES / (1024 * 1024))));

    this.addClassName("purchase-item-upload");

    uploadedPurchaseItems = new Div();
    uploadedPurchaseItems.addClassName("uploaded-documents");

    this.disclaimer = new Div();
    disclaimer.addClassName("instructions");
    disclaimer.add(new Span("Set offer signed status"));
    disclaimer.add(new Paragraph("Please tick the checkbox if the offer is signed."));

    add(titleBox, upload, restrictions, disclaimer, uploadedPurchaseItems);

    upload.addSucceededListener(succeededEvent -> {
      var purchase = new PurchaseItem(succeededEvent.getFileName());
      uploadedPurchaseItems.add(purchase);
      purchaseItemsCache.add(purchase);
      toggleFileSectionIfEmpty();
    });

    this.confirmButton.setText("Save");

    upload.getElement().addEventListener("file-remove", this::processClientFileRemoveEvent)
        .addEventData("event.detail.file.name");

    this.cancelButton.addClickListener(buttonClickEvent -> this.close());
    this.confirmButton.addClickListener(buttonClickEvent -> fireConfirmEvent());

    toggleFileSectionIfEmpty();
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
    if (uploadedPurchaseItems.getChildren().toList().isEmpty()) {
      uploadedPurchaseItems.addClassName("hidden");
      disclaimer.addClassName("hidden");
    } else {
      uploadedPurchaseItems.removeClassName("hidden");
      disclaimer.removeClassName("hidden");
    }
  }

  private void removeFileFromBuffer(String fileName) {
    multiFileMemoryBuffer.remove(fileName);
  }

  private void removeFileFromDisplay(String fileName) {
    var result = purchaseItemsCache.stream().filter(purchaseItem -> purchaseItem.fileName().equals(
        fileName)).findAny();
    result.ifPresent(purchaseItem -> {
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

  public void subscribe(ComponentEventListener<ConfirmEvent<UploadPurchaseDialog>> listener) {
    this.confirmListeners.add(listener);
  }

  @Override
  public void close() {
    emptyCachedBuffer();
    removeContent();
    super.close();
  }

  private void removeContent() {
    this.purchaseItemsCache.clear();
    this.uploadedPurchaseItems.removeAll();
    this.upload.clearFileList();
  }

  private void emptyCachedBuffer() {
    this.multiFileMemoryBuffer = new EditableMultiFileMemoryBuffer();
  }
}
