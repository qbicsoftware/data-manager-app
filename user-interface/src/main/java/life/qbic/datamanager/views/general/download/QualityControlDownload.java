package life.qbic.datamanager.views.general.download;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import java.util.function.BiFunction;
import life.qbic.application.commons.ApplicationException;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControlUpload;

/**
 * The QualityControlDownload class extends the Anchor class and provides functionality for
 * triggering the download of a QualityControlUpload file.
 */
public class QualityControlDownload extends Anchor {

  private final transient BiFunction<String, Long, QualityControlUpload> qualityControlDataProvider;

  public QualityControlDownload(
      BiFunction<String, Long, QualityControlUpload> qualityControlDataProvider) {
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
    this.qualityControlDataProvider = requireNonNull(qualityControlDataProvider,
        "qualityControlDataProvider must not be null");
  }

  public void trigger(String projectId, long qualityControlId) {
    UI ui = getUI().orElseThrow(() -> new ApplicationException(
        "QualityControlUpload Download component triggered but not attached to any UI."));
    QualityControlUpload qualityControlUpload = qualityControlDataProvider.apply(
        projectId, qualityControlId);
    StreamResource resource = new StreamResource(qualityControlUpload.getFileName(),
        () -> new ByteArrayInputStream(qualityControlUpload.fileContent()));
    this.setHref(resource);
    ui.getPage().executeJs("$0.click()", this.getElement());
  }
}
