package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.files.export.FileNameFormatter;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.files.export.download.WorkbookDownloadStreamProvider;
import life.qbic.datamanager.files.export.measurement.NGSWorkbooks;
import life.qbic.datamanager.files.export.measurement.ProteomicsWorkbooks;
import life.qbic.datamanager.views.general.PageArea;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Lists all the stored measurement templates. Allows users to
 * download their template of interest to facilitate measurement registrations dependent on the lab
 * facility (Proteomics, Genomics, Imaging...)
 */
@SpringComponent
@UIScope
@PermitAll
public class MeasurementTemplateListComponent extends PageArea {

  private final Div measurementTemplateList;

  public MeasurementTemplateListComponent() {
    measurementTemplateList = new Div();
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
    measurementTemplateList.removeAll();
    WorkbookDownloadStreamProvider genomicsStreamProvider = new WorkbookDownloadStreamProvider() {
      @Override
      public String getFilename() {
        return FileNameFormatter.formatWithVersion("ngs_measurement_registration_sheet", 1, "xlsx");
      }

      @Override
      public Workbook getWorkbook() {
        return NGSWorkbooks.createRegistrationWorkbook();
      }
    };
    MeasurementTemplateItem genomicsTemplate = new MeasurementTemplateItem("Genomics Template");

    WorkbookDownloadStreamProvider proteomicsStreamProvider = new WorkbookDownloadStreamProvider() {
      @Override
      public String getFilename() {
        return "proteomics_measurement_registration_sheet.xlsx";
      }

      @Override
      public Workbook getWorkbook() {
        return ProteomicsWorkbooks.createRegistrationWorkbook();
      }
    };
    MeasurementTemplateItem proteomicsTemplate = new MeasurementTemplateItem("Proteomics Template");

    genomicsTemplate.onDownloadButtonClicked(
        event -> fireEvent(new DownloadMeasurementTemplateEvent(genomicsStreamProvider, this,
            event.isFromClient())));
    proteomicsTemplate.onDownloadButtonClicked(event -> fireEvent(
        new DownloadMeasurementTemplateEvent(proteomicsStreamProvider, this,
            event.isFromClient())));
    measurementTemplateList.add(genomicsTemplate, proteomicsTemplate);
  }


  public Registration addDownloadMeasurementTemplateClickListener(
      ComponentEventListener<DownloadMeasurementTemplateEvent> listener) {
    return addListener(DownloadMeasurementTemplateEvent.class, listener);
  }

  public static class DownloadMeasurementTemplateEvent extends
      ComponentEvent<MeasurementTemplateListComponent> {

    private final transient DownloadStreamProvider downloadStreamProvider;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DownloadMeasurementTemplateEvent(DownloadStreamProvider downloadStreamProvider,
        MeasurementTemplateListComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.downloadStreamProvider = downloadStreamProvider;
    }

    public DownloadStreamProvider getDownloadStreamProvider() {
      return downloadStreamProvider;
    }
  }


  private static class MeasurementTemplateItem extends Span {

    private final Button downloadButton = new Button(LumoIcon.DOWNLOAD.create());

    public MeasurementTemplateItem(String domainName) {
      var fileIcon = VaadinIcon.FILE.create();
      fileIcon.addClassName("file-icon");
      var qualityControlFileName = new Span(domainName);
      qualityControlFileName.setTitle(domainName);
      qualityControlFileName.addClassName("file-name");
      var fileNameWithIcon = new Span(fileIcon, qualityControlFileName);
      fileNameWithIcon.addClassName("file-info-with-icon");
      downloadButton.addThemeNames("tertiary-inline", "icon");
      downloadButton.setAriaLabel("Download");
      downloadButton.setTooltipText("Download");
      Span controls = new Span();
      controls.add(downloadButton);
      controls.addClassName("controls");

      add(fileNameWithIcon);
      add(controls);
      addClassName("measurement-template-list-item");
    }

    private void onDownloadButtonClicked(ComponentEventListener<ClickEvent<Button>> listener) {
      downloadButton.addClickListener(listener);
    }
  }
}
