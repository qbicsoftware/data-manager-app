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
import java.util.List;
import life.qbic.datamanager.views.general.PageArea;

/**
 * Lists all uploaded {@link QualityControl}. Allows users to upload new {@link QualityControl} and
 * delete existing {@link QualityControl}.
 */
public class QualityControlList extends PageArea {

  private final VirtualList<QualityControl> qualityControls;

  public QualityControlList() {
    qualityControls = new VirtualList<>();
    qualityControls.setRenderer(new ComponentRenderer<>(this::renderQualityControl));
    Button upload = new Button("Upload", this::onUploadQualityControlClicked);
    upload.setAriaLabel("Upload");
    Span title = new Span("Sample QC");
    title.addClassName("title");
    Span header = new Span(title, upload);
    header.addClassName("header");
    addClassName("quality-control-list-component");
    add(header, qualityControls);
  }

  public void setItems() {

  }

  private void onUploadQualityControlClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new UploadQualityControlEvent(this, clickEvent.isFromClient()));
  }

  private Component renderQualityControl(QualityControl qualityControl) {

    var offerFileName = new Span(qualityControl.filename());
    offerFileName.setTitle(qualityControl.filename());
    offerFileName.addClassName("file-name");

    var downloadButton = new Button(LumoIcon.DOWNLOAD.create(),
        event -> onDownloadQualityControlClicked(
            new DownloadQualityControlEvent(qualityControl.qualityControlId(), this,
                event.isFromClient())));
    downloadButton.addThemeNames("tertiary-inline", "icon");
    downloadButton.setAriaLabel("Download");
    downloadButton.setTooltipText("Download");
    var deleteButton = new Button(LumoIcon.CROSS.create(), event -> onDeleteQualityControlClicked(
        new DeleteQualityControlEvent(qualityControl.qualityControlId(), this,
            event.isFromClient())));
    deleteButton.addThemeNames("tertiary-inline", "icon");
    deleteButton.setTooltipText("Delete");
    deleteButton.setAriaLabel("Delete");

    Span qualityControlActionControls = new Span(downloadButton, deleteButton);
    qualityControlActionControls.addClassName("controls");

    var fileIcon = VaadinIcon.FILE.create();
    fileIcon.addClassName("file-icon");
    Span fileInfo = new Span(fileIcon, offerFileName);

    Span qualityControlListItem = new Span();
    qualityControlListItem.addClassName("quality-control");
    qualityControlListItem.add(fileInfo, qualityControlActionControls);

    return qualityControlListItem;
  }

  public void setQualityControls(List<QualityControl> qualityControlList) {
    qualityControls.setItems(qualityControlList);
  }

  private void onDeleteQualityControlClicked(DeleteQualityControlEvent event) {
    fireEvent(event);
  }

  private void onDownloadQualityControlClicked(DownloadQualityControlEvent event) {
    fireEvent(event);
  }

  public Registration addDeleteQualityControlListener(
      ComponentEventListener<DeleteQualityControlEvent> listener) {
    return addListener(DeleteQualityControlEvent.class, listener);
  }

  public Registration addDownloadQualityControlListener(
      ComponentEventListener<DownloadQualityControlEvent> listener) {
    return addListener(DownloadQualityControlEvent.class, listener);
  }

  public Registration addUploadQualityControlListener(
      ComponentEventListener<UploadQualityControlEvent> listener) {
    return addListener(UploadQualityControlEvent.class, listener);
  }

  public void remove(long qualityControlId) {
  }

  public static class DeleteQualityControlEvent extends ComponentEvent<QualityControlList> {

    private final long qualityControlId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DeleteQualityControlEvent(long qualityControlId, QualityControlList source,
        boolean fromClient) {
      super(source, fromClient);
      this.qualityControlId = qualityControlId;
    }

    public long qualityControlId() {
      return qualityControlId;
    }
  }

  public static class DownloadQualityControlEvent extends ComponentEvent<QualityControlList> {

    private final long qualityControlId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DownloadQualityControlEvent(long qualityControlId, QualityControlList source,
        boolean fromClient) {
      super(source, fromClient);
      this.qualityControlId = qualityControlId;
    }

    public long qualityControlId() {
      return qualityControlId;
    }
  }

  public static class UploadQualityControlEvent extends ComponentEvent<QualityControlList> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public UploadQualityControlEvent(QualityControlList source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public record QualityControl(Long qualityControlId, String filename, String experimentName) {

  }
}
