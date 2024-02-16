package life.qbic.datamanager.views.general.download;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import life.qbic.application.commons.ApplicationException;

/**
 * The MeasurementTemplate download class extends the Anchor class and provides functionality for
 * triggering the download of an MeasurementTemplate file.
 */
public class MeasurementTemplateDownload extends Anchor {

  public MeasurementTemplateDownload() {
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

  public void trigger(DownloadContentProvider downloadContentProvider) {
    UI ui = getUI().orElseThrow(() -> new ApplicationException(
        this.getClass().getSimpleName() + "component triggered but not attached to any UI."));
    StreamResource resource = new StreamResource(downloadContentProvider.getFileName(),
        () -> new ByteArrayInputStream(downloadContentProvider.getContent()));
    this.setHref(resource);
    ui.getPage().executeJs("$0.click()", this.getElement());
  }
}
