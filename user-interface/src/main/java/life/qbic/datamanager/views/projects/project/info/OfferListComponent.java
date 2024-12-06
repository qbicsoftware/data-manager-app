package life.qbic.datamanager.views.projects.project.info;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.views.general.PageArea;

/**
 * Lists all uploaded offers. Allows users to upload and delete uploads.
 */
public class OfferListComponent extends PageArea {

  private final VirtualList<OfferInfo> delegateList;
  private final transient List<OfferInfo> offers;

  public OfferListComponent() {
    offers = new ArrayList<>();
    delegateList = new VirtualList<>();
    delegateList.setRenderer(new ComponentRenderer<>(this::renderOffer));
    delegateList.setItems(offers);
    Button upload = new Button("Upload", this::onUploadOfferClicked);
    upload.setAriaLabel("Upload");
    Span title = new Span("Offers");
    title.addClassName("title");
    Span header = new Span(title, upload);
    header.addClassName("header");
    addClassName("offer-list-component");
    delegateList.addClassName("offer-list");
    add(header, delegateList);
  }

  private void onUploadOfferClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new UploadOfferClickEvent(this, clickEvent.isFromClient()));
  }

  public void remove(long offerId) {
    Optional<OfferInfo> optionalOfferInfo = offers.stream()
        .filter(it -> it.offerId() == offerId)
        .findAny();
    optionalOfferInfo.ifPresent(
        offerInfo -> {
          offers.remove(offerInfo);
          delegateList.getDataProvider().refreshAll();
        }
    );
  }

  public void add(OfferInfo offerInfo) {
    offers.add(offerInfo);
    delegateList.getDataProvider().refreshAll();
  }

  private Component renderOffer(OfferInfo offer) {

    var offerFileName = new Span(offer.filename());
    offerFileName.setTitle(offer.filename());
    offerFileName.addClassName("file-name");

    var signedInfo = VaadinIcon.CHECK_CIRCLE.create();
    signedInfo.addClassName("signed-info");
    signedInfo.addClassName(offer.signed() ? "signed" : "unsigned");
    signedInfo.setTooltipText(offer.signed() ? "signed" : "unsigned");
    var downloadButton = new Button(LumoIcon.DOWNLOAD.create(),
        event -> onDownloadOfferClicked(
            new DownloadOfferClickEvent(offer.offerId(), this, event.isFromClient())));
    downloadButton.addThemeNames("tertiary-inline", "icon");
    downloadButton.setAriaLabel("Download");
    downloadButton.setTooltipText("Download");
    var deleteButton = new Button(LumoIcon.CROSS.create(), event -> onDeleteOfferClicked(
        new DeleteOfferClickEvent(offer.offerId(), this, event.isFromClient())));
    deleteButton.addThemeNames("tertiary-inline", "icon");
    deleteButton.setTooltipText("Delete");
    deleteButton.setAriaLabel("Delete");

    Span offerActionControls = new Span(downloadButton, deleteButton);
    offerActionControls.addClassName("controls");

    var fileIcon = VaadinIcon.FILE.create();
    fileIcon.addClassName("file-icon");
    Span fileInfo = new Span(fileIcon, offerFileName, signedInfo);
    fileInfo.addClassName("file-info");

    Span offerListItem = new Span();
    offerListItem.addClassName("offer-info");
    offerListItem.add(fileInfo, offerActionControls);

    return offerListItem;
  }

  public void setOffers(List<OfferInfo> offers) {
    this.offers.clear();
    this.offers.addAll(offers);
    delegateList.getDataProvider().refreshAll();
  }

  public void refresh() {
    delegateList.getDataProvider().refreshAll();
  }

  private void onDeleteOfferClicked(DeleteOfferClickEvent event) {
    fireEvent(event);
  }

  private void onDownloadOfferClicked(DownloadOfferClickEvent event) {
    fireEvent(event);
  }

  public Registration addDeleteOfferClickListener(
      ComponentEventListener<DeleteOfferClickEvent> listener) {
    return addListener(DeleteOfferClickEvent.class, listener);
  }

  public Registration addDownloadOfferClickListener(
      ComponentEventListener<DownloadOfferClickEvent> listener) {
    return addListener(DownloadOfferClickEvent.class, listener);
  }

  public Registration addUploadOfferClickListener(
      ComponentEventListener<UploadOfferClickEvent> listener) {
    return addListener(UploadOfferClickEvent.class, listener);
  }

  public static class DeleteOfferClickEvent extends ComponentEvent<OfferListComponent> {

    private final long offerId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DeleteOfferClickEvent(long offerId, OfferListComponent source, boolean fromClient) {
      super(source, fromClient);
      this.offerId = offerId;
    }

    public long offerId() {
      return offerId;
    }
  }

  public static class DownloadOfferClickEvent extends ComponentEvent<OfferListComponent> {

    private final long offerId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DownloadOfferClickEvent(long offerId, OfferListComponent source, boolean fromClient) {
      super(source, fromClient);
      this.offerId = offerId;
    }

    public long offerId() {
      return offerId;
    }
  }

  public static class UploadOfferClickEvent extends ComponentEvent<OfferListComponent> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public UploadOfferClickEvent(OfferListComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public record OfferInfo(Long offerId, String filename, boolean signed) {

  }
}
