package life.qbic.datamanager.views.general.download;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import java.util.function.BiFunction;
import life.qbic.application.commons.ApplicationException;
import life.qbic.projectmanagement.domain.model.project.purchase.Offer;

/**
 * The OfferDownload class extends the Anchor class and provides functionality for triggering the
 * download of an Offer file.
 */
public class OfferDownload extends Anchor {

  private final transient BiFunction<String, Long, Offer> offerDataProvider;

  public OfferDownload(BiFunction<String, Long, Offer> offerDataProvider) {
    super("_blank", "Download");
    /*
     * Using setVisisble(false), vaadin prevents any client side actions.
     * This prevents us from using JavaScript to click the link, which is the only option
     * for using anchors now.
     * Thus, we prevent the display of the link with `display: none`.
     * The link is still on the page but invisible.
     */
    getStyle().set("display", "none");

    setTarget("_blank");
    getElement().setAttribute("download", true);
    this.offerDataProvider = requireNonNull(offerDataProvider,
        "offerDataProvider must not be null");
  }

  public void trigger(String projectId, long offerId) {
    UI ui = getUI().orElseThrow(() -> new ApplicationException(
        "Offer Download component triggered but not attached to any UI."));
    Offer offer = offerDataProvider.apply(projectId, offerId);
    StreamResource resource = new StreamResource(offer.getFileName(),
        () -> new ByteArrayInputStream(offer.fileContent()));
    this.setHref(resource);
    ui.getPage().executeJs("$0.click()", this.getElement());
  }
}
