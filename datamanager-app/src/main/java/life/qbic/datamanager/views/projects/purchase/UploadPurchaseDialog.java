package life.qbic.datamanager.views.projects.purchase;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.FailedEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.FileSizeFormatter;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.views.general.AllowedFileExtension;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent;
import life.qbic.datamanager.views.general.upload.UploadedFilesChangeListener.FileEntry;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.projectmanagement.application.purchase.OfferDTO;
import org.springframework.util.unit.DataSize;

/**
 * <b>Upload Purchase Dialog</b>
 * <p>
 * A dialog window that enables uploads of purchase items such as an offer.
 *
 * @since 1.0.0
 */
public class UploadPurchaseDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 6602134795666762831L;
  private final Div uploadedPurchaseItems;
  private final List<PurchaseItem> purchaseItemsCache = new ArrayList<>();
  private final Div uploadedItemsSectionContent;
  private final ContentUploadComponent contentUploadComponent;
  private static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(16);

  public UploadPurchaseDialog(UploadConfiguration uploadConfiguration) {
    // Vaadin's upload component setup
    contentUploadComponent = new ContentUploadComponent(uploadConfiguration);
    contentUploadComponent.addUnspecificFailureListener(this::onUploadFailure);
    contentUploadComponent.setAcceptedFileTypes(
        AllowedFileExtension.PDF.mimetype()
    );
    contentUploadComponent.setMaxFileSize(MAX_FILE_SIZE);

    setHeaderTitle("Upload an offer");
    // Title box configuration
    Span uploadSectionTitle = new Span("Upload your offer");
    uploadSectionTitle.addClassName("section-title");

    Div uploadSection = new Div();
    uploadSection.add(uploadSectionTitle, contentUploadComponent);

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

    var changeRegistration = contentUploadComponent.addChangeListener(event -> {
      var componentUI = event.getSource().getUI();
      switch (event.changeType()) {
        case FILE_ADDED -> {
          for (FileEntry file : event.changedFiles()) {
            var added = new PurchaseItem(file.fileName());
            purchaseItemsCache.add(added);
            componentUI.ifPresent(ui -> ui.access(() -> uploadedPurchaseItems.add(added)));
          }
        }
        case FILE_REMOVED -> {
          var toBeRemoved = purchaseItemsCache.stream()
              .filter(Objects::nonNull)
              .filter(purchaseItem -> event.changedFiles().stream().map(FileEntry::fileName)
                  .anyMatch(it -> it.equals(purchaseItem.fileName())))
              .toList();
          toBeRemoved
              .forEach(purchaseItem -> {
                purchaseItemsCache.remove(purchaseItem);
                componentUI.ifPresent(
                    ui -> ui.access(() -> uploadedPurchaseItems.remove(purchaseItem)));
              });
        }
      }
      componentUI.ifPresent(ui -> ui.access(this::toggleFileSectionIfEmpty));
    });
    var failureRegistration = contentUploadComponent.addFileRejectedListener(this::onUploadFailure);
    addDetachListener(event -> {
      changeRegistration.remove();
      failureRegistration.remove();
    });

    // Put the elements together
    add(uploadSection, uploadedItemsSection);
    addClassName("purchase-item-upload");
    confirmButton.setText("Save");
    // Init the visibility rendering once
    toggleFileSectionIfEmpty();
  }

  private void onUploadFailure(ComponentEvent<?> event) {
    ErrorMessage errorMessage = new ErrorMessage("Offer upload failed",
        "An unknown exception has occurred");
    if (event instanceof FileRejectedEvent) {
      errorMessage.descriptionTextSpan.setText(
          "Please provide a file within the file limit of " + FileSizeFormatter.formatBytes(
              contentUploadComponent.getMaxFileSize()));
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

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new UploadPurchaseDialog.CancelEvent(this, clickEvent.isFromClient()));
  }


  private void toggleFileSectionIfEmpty() {
    boolean filesUploaded = !uploadedPurchaseItems.getChildren().toList().isEmpty();
    uploadedPurchaseItems.setVisible(filesUploaded);
    uploadedItemsSectionContent.setVisible(filesUploaded);
  }


  public List<OfferDTO> purchaseItems() {
    return purchaseItemsCache.stream().map(this::convertToOffer).toList();
  }

  private OfferDTO convertToOffer(PurchaseItem purchaseItem) {
    try {
      var fileName = purchaseItem.fileName();
      var content = contentUploadComponent.getContent(fileName)
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
