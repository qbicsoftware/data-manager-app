package life.qbic.datamanager.views.projects.project.info;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoIcon;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.general.PageArea;

/**
 * Lists all uploaded {@link QualityControl}. Allows users to upload new {@link QualityControl} and
 * delete existing {@link QualityControl}.
 */
public class QualityControlListComponent extends PageArea {

  private final VirtualList<QualityControl> qualityControls;

  public QualityControlListComponent() {
    qualityControls = new VirtualList<>();
    qualityControls.setRenderer(qualityControlItemRenderer());
    Button upload = new Button("Upload", this::onUploadQualityControlClicked);
    upload.setAriaLabel("Upload");
    Span title = new Span("Sample QC");
    title.addClassName("title");
    Span header = new Span(title, upload);
    header.addClassName("header");
    addClassName("quality-control-list-component");
    qualityControls.addClassName("quality-control-list");
    add(header, qualityControls);
  }

  private ComponentRenderer<QualityControlItem, QualityControl> qualityControlItemRenderer() {
    return new ComponentRenderer<>(qualityControl -> {
      QualityControlItem qualityControlItem = new QualityControlItem(qualityControl);
      qualityControlItem.onDownloadButtonClicked(event -> fireEvent(
          new DownloadQualityControlEvent(qualityControl.qualityControlId(), this,
              event.isFromClient())));
      qualityControlItem.onDeleteButtonClicked(event -> fireEvent(new DeleteQualityControlEvent(
          qualityControl.qualityControlId(), this, event.isFromClient())));
      return qualityControlItem;
    });
  }

  private void onUploadQualityControlClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new UploadQualityControlEvent(this, clickEvent.isFromClient()));
  }

  public void setQualityControls(List<QualityControl> qualityControlList) {
    List<QualityControl> sortedList = qualityControlList.stream()
        .sorted(Comparator.comparing(
                (QualityControl qualityControl) -> qualityControl.experimentName.isEmpty())
            .thenComparing(QualityControl::experimentName)
            .thenComparing(QualityControl::filename)).toList();
    qualityControls.setItems(sortedList);
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

  public static class DeleteQualityControlEvent extends
      ComponentEvent<QualityControlListComponent> {

    private final long qualityControlId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DeleteQualityControlEvent(long qualityControlId, QualityControlListComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.qualityControlId = qualityControlId;
    }

    public long qualityControlId() {
      return qualityControlId;
    }
  }

  public static class DownloadQualityControlEvent extends
      ComponentEvent<QualityControlListComponent> {

    private final long qualityControlId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DownloadQualityControlEvent(long qualityControlId, QualityControlListComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.qualityControlId = qualityControlId;
    }

    public long qualityControlId() {
      return qualityControlId;
    }
  }

  public static class UploadQualityControlEvent extends
      ComponentEvent<QualityControlListComponent> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public UploadQualityControlEvent(QualityControlListComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public record QualityControl(Long qualityControlId, String filename, String experimentName) {

  }

  private static class QualityControlItem extends Span {

    private final transient QualityControl qualityControl;
    private final Span controls = new Span();
    private final Button downloadButton = new Button(LumoIcon.DOWNLOAD.create());
    private final Button deleteButton = new Button(LumoIcon.CROSS.create());

    public QualityControlItem(QualityControl qualityControl) {
      this.qualityControl = qualityControl;
      createFileInformationSection();
      createControls();
      addClassName("quality-control-item");
    }

    public QualityControl qualityControl() {
      return qualityControl;
    }

    private void createFileInformationSection() {
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      Span experimentName = new Span(qualityControl.experimentName());
      experimentName.addClassName("secondary");
      var qualityControlFileName = new Span(qualityControl.filename());
      qualityControlFileName.setTitle(qualityControl.filename());
      qualityControlFileName.addClassName("file-name");
      Div fileInfo = new Div(qualityControlFileName, experimentName);
      fileInfo.addClassName("file-info");
      Span iconWithFileInfo = new Span(fileIcon, fileInfo);
      iconWithFileInfo.addClassName("file-info-with-icon");
      add(iconWithFileInfo);
    }

    private void createControls() {
      downloadButton.addThemeNames("tertiary-inline", "icon");
      downloadButton.setAriaLabel("Download");
      downloadButton.setTooltipText("Download");
      deleteButton.addThemeNames("tertiary-inline", "icon");
      deleteButton.setTooltipText("Delete");
      deleteButton.setAriaLabel("Delete");
      controls.add(downloadButton, deleteButton);
      controls.addClassName("controls");
      add(controls);
    }

    private void onDownloadButtonClicked(ComponentEventListener<ClickEvent<Button>> listener) {
      downloadButton.addClickListener(listener);
    }

    private void onDeleteButtonClicked(ComponentEventListener<ClickEvent<Button>> listener) {
      deleteButton.addClickListener(listener);
    }
  }
}
