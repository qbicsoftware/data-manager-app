package life.qbic.datamanager.download;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import life.qbic.application.commons.ApplicationException;

/**
 * The DownloadProvider class extends the Anchor class and provides functionality for triggering
 * any kind of download based on a provided DownloadContentProvider
 */
public class DownloadProvider extends Anchor {

  private final transient DownloadContentProvider downloadContentProvider;

  public DownloadProvider(DownloadContentProvider downloadContentProvider) {
    super("_blank", "Download");
    this.downloadContentProvider = downloadContentProvider;
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
  }

  public void trigger() {
    byte[] content = downloadContentProvider.getContent();
    if(content.length > 0) {
      UI ui = getUI().orElseThrow(() -> new ApplicationException(
          "Download component triggered but not attached to any UI."));
      StreamResource resource = new StreamResource(downloadContentProvider.getFileName(),
          () -> new ByteArrayInputStream(content));
      this.setHref(resource);
      ui.getPage().executeJs("$0.click()", this.getElement());
    }
  }

}
