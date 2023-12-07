package life.qbic.datamanager.views.projects.project.info;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lists all uploaded offers. Allows users to upload and delete uploads.
 */
public class OfferList extends Div {

  private final VirtualList<OfferInfo> delegateList;
  private final List<OfferInfo> offers;

  public OfferList() {
    offers = new ArrayList<>();
    delegateList = new VirtualList<>();
    delegateList.setRenderer(new ComponentRenderer<>(this::renderOffer));
    delegateList.setItems(offers);
    Button upload = new Button("Upload", this::onUploadOfferClicked);
    add(upload, delegateList);
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

    var oIcon = new Icon("O");

    var offerFileName = new Span(offer.filename());
    offerFileName.addClassName("file-name");

    var signedInfo = VaadinIcon.CHECK_CIRCLE.create();
    signedInfo.addClassName("signed-info");
    signedInfo.addClassName(offer.signed() ? "signed" : "unsigned");

    var downloadButton = new Button(VaadinIcon.DOWNLOAD_ALT.create(),
        event -> onDownloadOfferClicked(
            new DownloadOfferClickEvent(offer.offerId(), this, event.isFromClient())));
    downloadButton.addClassName("download");
    downloadButton.setThemeName("tertiary");

    var deleteButton = new Button(VaadinIcon.CLOSE.create(), event -> onDeleteOfferClicked(
        new DeleteOfferClickEvent(offer.offerId(), this, event.isFromClient())));
    deleteButton.addClassName("delete");
    deleteButton.setThemeName("tertiary");

    var fileInfo = new Div();
    fileInfo.addClassName("file-info");
    fileInfo.add(oIcon, offerFileName, signedInfo);

    Div offerListItem = new Div();
    offerListItem.addClassName("offer-info");
    offerListItem.add(fileInfo, downloadButton, deleteButton);

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

  private void onUploadOfferClickListener(UploadOfferClickEvent event) {
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

  public static class DeleteOfferClickEvent extends ComponentEvent<OfferList> {

    private final long offerId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DeleteOfferClickEvent(long offerId, OfferList source, boolean fromClient) {
      super(source, fromClient);
      this.offerId = offerId;
    }

    public long offerId() {
      return offerId;
    }
  }

  public static class DownloadOfferClickEvent extends ComponentEvent<OfferList> {

    private final long offerId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DownloadOfferClickEvent(long offerId, OfferList source, boolean fromClient) {
      super(source, fromClient);
      this.offerId = offerId;
    }

    public long offerId() {
      return offerId;
    }
  }

  public static class UploadOfferClickEvent extends ComponentEvent<OfferList> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public UploadOfferClickEvent(OfferList source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public record OfferInfo(Long offerId, String filename, boolean signed) {

  }
}
