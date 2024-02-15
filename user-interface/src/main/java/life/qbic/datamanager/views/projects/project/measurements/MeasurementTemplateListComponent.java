package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import life.qbic.datamanager.views.general.PageArea;

/**
 * Lists all the stored {@link MeasurementTemplate}. Allows users to download
 * {@link MeasurementTemplate} to facilitate measurement registrations dependent on the lab facility
 * (Proteomics, Genomics, Imaging...)
 */
@SpringComponent
@UIScope
@PermitAll
public class MeasurementTemplateListComponent extends PageArea {

  private final VirtualList<MeasurementTemplate> measurementTemplateList;

  public MeasurementTemplateListComponent() {
    measurementTemplateList = new VirtualList<>();
    measurementTemplateList.setRenderer(measurementTemplateItemRenderer());
    measurementTemplateList.addClassName("measurement-template-list");
    Span title = new Span("Templates");
    title.addClassNames("header", "title");
    measurementTemplateList.addClassName("measurement-template-list");
    addClassName("measurement-template-list-component");
    addComponentAsFirst(title);
    add(measurementTemplateList);
  }

  private ComponentRenderer<MeasurementTemplateItem, MeasurementTemplate> measurementTemplateItemRenderer() {
    return new ComponentRenderer<>(measurementTemplate -> {
      MeasurementTemplateItem measurementTemplateItem = new MeasurementTemplateItem(
          measurementTemplate);
      measurementTemplateItem.onDownloadButtonClicked(event -> fireEvent(
          new DownloadMeasurementTemplateEvent(measurementTemplate.MeasurementId(), this,
              event.isFromClient())));
      return measurementTemplateItem;
    });
  }

  public void setMeasurementTemplates(List<MeasurementTemplate> measurementTemplates) {
    measurementTemplateList.setItems(measurementTemplates);
  }

  public static class DownloadMeasurementTemplateEvent extends
      ComponentEvent<MeasurementTemplateListComponent> {

    private final long measurementTemplateId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DownloadMeasurementTemplateEvent(long measurementTemplateId,
        MeasurementTemplateListComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.measurementTemplateId = measurementTemplateId;
    }

    public long measurementTemplateId() {
      return measurementTemplateId;
    }
  }


  private static class MeasurementTemplateItem extends Span {

    private final MeasurementTemplate measurementTemplate;
    private final Span controls = new Span();
    private final Button downloadButton = new Button(LumoIcon.DOWNLOAD.create());

    public MeasurementTemplateItem(MeasurementTemplate measurementTemplate) {
      this.measurementTemplate = measurementTemplate;
      createFileInformationSection();
      createControls();
      addClassName("measurement-template-list-item");
    }

    public MeasurementTemplate measurementTemplate() {
      return measurementTemplate;
    }

    private void createFileInformationSection() {
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      var qualityControlFileName = new Span(measurementTemplate.fileName());
      qualityControlFileName.setTitle(measurementTemplate.fileName());
      qualityControlFileName.addClassName("file-name");
      add(qualityControlFileName);
    }

    private void createControls() {
      downloadButton.addThemeNames("tertiary-inline", "icon");
      downloadButton.setAriaLabel("Download");
      downloadButton.setTooltipText("Download");
      controls.add(downloadButton);
      controls.addClassName("controls");
      add(controls);
    }

    private void onDownloadButtonClicked(ComponentEventListener<ClickEvent<Button>> listener) {
      downloadButton.addClickListener(listener);
    }
  }

  public record MeasurementTemplate(long MeasurementId, String fileName) {

  }
}
