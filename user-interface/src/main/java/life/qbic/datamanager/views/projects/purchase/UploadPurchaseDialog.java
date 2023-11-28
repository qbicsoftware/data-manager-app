package life.qbic.datamanager.views.projects.purchase;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
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
  @Serial
  private static final long serialVersionUID = 6602134795666762831L;
  private final Upload upload;
  private EditableMultiFileMemoryBuffer multiFileMemoryBuffer;
  private Div uploadedPurchaseItems;

  private List<PurchaseItem> purchaseItemsCache;
  private List<ComponentEventListener<ConfirmEvent<UploadPurchaseDialog>>> confirmListeners;

  public UploadPurchaseDialog() {
    multiFileMemoryBuffer = new EditableMultiFileMemoryBuffer();
    upload = new Upload(multiFileMemoryBuffer);
    purchaseItemsCache = new ArrayList<>();

    uploadedPurchaseItems = new Div();
    uploadedPurchaseItems.addClassName("uploaded-documents");

    add(upload, uploadedPurchaseItems);

    upload.addSucceededListener(succeededEvent -> {
      var purchase = new PurchaseItem(succeededEvent.getFileName());
      uploadedPurchaseItems.add(purchase);
      purchaseItemsCache.add(purchase);
    });

    upload.getElement().addEventListener("file-remove", this::processClientFileRemoveEvent)
        .addEventData("event.detail.file.name");

    this.cancelButton.addClickListener(buttonClickEvent -> this.close());
    this.confirmButton.addClickListener(buttonClickEvent -> fireConfirmEvent());
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


}
