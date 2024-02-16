package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import jakarta.annotation.security.PermitAll;
import java.util.Arrays;
import java.util.List;
import life.qbic.datamanager.templates.TemplateDownloadFactory;
import life.qbic.datamanager.templates.TemplateDownloadFactory.Template;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;

/**
 * Lists all the stored measurement templates via the default {@link DownloadContentProvider}. Allows users to
 * download their template of interest {@link DownloadContentProvider} to facilitate measurement registrations dependent on the
 * lab facility (Proteomics, Genomics, Imaging...)
 */
@SpringComponent
@UIScope
@PermitAll
public class MeasurementTemplateListComponent extends PageArea {

  private final VirtualList<DownloadContentProvider> measurementTemplateList;

  public MeasurementTemplateListComponent() {
    measurementTemplateList = new VirtualList<>();
    measurementTemplateList.setRenderer(measurementTemplateItemRenderer());
    measurementTemplateList.addClassName("measurement-template-list");
    Span title = new Span("Templates");
    title.addClassNames("header", "title");
    measurementTemplateList.addClassName("measurement-template-list");
    addClassName("measurement-template-list-component");
    addComponentAsFirst(title);
    loadMeasurementTemplates();
    add(measurementTemplateList);
  }

  private void loadMeasurementTemplates() {
    List<DownloadContentProvider> templates = Arrays.stream(Template.values()).map(
        TemplateDownloadFactory::provider).toList();
    measurementTemplateList.setItems(templates);
  }

  private ComponentRenderer<MeasurementTemplateItem, DownloadContentProvider> measurementTemplateItemRenderer() {
    return new ComponentRenderer<>(measurementTemplate -> {
      MeasurementTemplateItem measurementTemplateItem = new MeasurementTemplateItem(
          measurementTemplate);
      measurementTemplateItem.onDownloadButtonClicked(event -> fireEvent(
          new DownloadMeasurementTemplateEvent(measurementTemplate, this,
              event.isFromClient())));
      return measurementTemplateItem;
    });
  }

  public Registration addDownloadMeasurementTemplateClickListener(
      ComponentEventListener<DownloadMeasurementTemplateEvent> listener) {
    return addListener(DownloadMeasurementTemplateEvent.class, listener);
  }

  public static class DownloadMeasurementTemplateEvent extends
      ComponentEvent<MeasurementTemplateListComponent> {

    private final DownloadContentProvider measurementTemplate;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DownloadMeasurementTemplateEvent(DownloadContentProvider measurementTemplate,
        MeasurementTemplateListComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.measurementTemplate = measurementTemplate;
    }

    public DownloadContentProvider measurementTemplate() {
      return measurementTemplate;
    }
  }


  private static class MeasurementTemplateItem extends Span {

    private final DownloadContentProvider measurementTemplate;
    private final Span controls = new Span();
    private final Button downloadButton = new Button(LumoIcon.DOWNLOAD.create());

    public MeasurementTemplateItem(DownloadContentProvider measurementTemplate) {
      this.measurementTemplate = measurementTemplate;
      createFileInformationSection();
      createControls();
      addClassName("measurement-template-list-item");
    }

    public DownloadContentProvider measurementTemplate() {
      return measurementTemplate;
    }

    private void createFileInformationSection() {
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      var qualityControlFileName = new Span(measurementTemplate.getFileName());
      qualityControlFileName.setTitle(measurementTemplate.getFileName());
      qualityControlFileName.addClassName("file-name");
      var fileNameWithIcon = new Span(fileIcon, qualityControlFileName);
      fileNameWithIcon.addClassName("file-info-with-icon");
      add(fileNameWithIcon);
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
}
