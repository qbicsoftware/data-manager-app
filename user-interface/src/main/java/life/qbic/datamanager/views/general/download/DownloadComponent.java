package life.qbic.datamanager.views.general.download;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;

public class DownloadComponent extends Anchor {

  public DownloadComponent() {
    super("_blank", "Download");
    /*
     * Using setVisible(false), vaadin prevents any client side actions.
     * This prevents us from using JavaScript to click the link, which is the only option
     * for using anchors now.
     * Thus, we prevent the display of the link with `display: none`.
     * The link is still on the page but invisible.
     */
    getStyle().set("display", "none");
    setTarget("_blank");
    getElement().setAttribute("download", true);
  }

  public synchronized void trigger(DownloadStreamProvider downloadStreamProvider) {
    UI ui = getUI().orElseThrow(() -> new ApplicationException(
        this.getClass().getSimpleName() + "component triggered but not attached to any UI."));
    StreamResource resource = new StreamResource(downloadStreamProvider.getFilename(),
        downloadStreamProvider::getStream);
    this.setHref(resource);
    ui.getPage().executeJs("$0.click()", this.getElement());
  }
}
