package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
class SampleMetadataLayout extends VerticalLayout {

  public Spreadsheet sampleRegistrationSpreadsheet = new Spreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button nextButton = new Button("Next");

  SampleMetadataLayout() {
    initContent();
    this.setSizeFull();
  }

  private void initContent() {
    add(sampleRegistrationSpreadsheet);
    styleSampleRegistrationSpreadSheet();
    initButtonLayout();
  }

  private void initButtonLayout() {
    HorizontalLayout sampleMetadataButtons = new HorizontalLayout();
    nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sampleMetadataButtons.add(cancelButton, nextButton);
    this.setAlignSelf(Alignment.END, sampleMetadataButtons);
    add(sampleMetadataButtons);
  }

  private void styleSampleRegistrationSpreadSheet() {
    sampleRegistrationSpreadsheet.setSizeFull();
    sampleRegistrationSpreadsheet.setSheetSelectionBarVisible(false);
    sampleRegistrationSpreadsheet.setFunctionBarVisible(false);
  }

  public void generateMetadataSpreadsheet(MetaDataTypes metaDataTypes) throws IOException {
    /*
    //Todo Make spreadsheet factory
    String resourcePath = "";
    switch (metaDataTypes) {
      case PROTEOMICS -> resourcePath = "MetadataSheets/Suggested_PXP_Metadata.xlsx";
      case LIGANDOMICS -> resourcePath = "MetadataSheets/Suggested_Ligandomics.xlsx";
      case TRANSCRIPTOMIC_GENOMICS -> resourcePath = "MetadataSheets/Suggested_NGS_Metadata.xlsx";
      case METABOLOMICS -> resourcePath = "MetadataSheets/Suggested_Metabolomics_LCMS.xlsx";
    }
    if (!resourcePath.isBlank()) {
      InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
      if (resourceAsStream != null) {
        sampleRegistrationSpreadsheet.read(resourceAsStream);
      }
    }
    */
    List<String> tableHeaders = getTableHeaders();
    sampleRegistrationSpreadsheet.setMaxColumns(tableHeaders.size());
    CellStyle lockedCells = sampleRegistrationSpreadsheet.getWorkbook().createCellStyle();
    lockedCells.setLocked(true);
    Font font = sampleRegistrationSpreadsheet.getWorkbook().createFont();
    font.setBold(true);
    lockedCells.setBottomBorderColor(IndexedColors.BLUE.getIndex());
    int iterate = 0;
    for (String headerValue : tableHeaders) {
      Cell cell = sampleRegistrationSpreadsheet.createCell(0, iterate, headerValue);
      cell.setCellStyle(lockedCells);
      iterate++;
      sampleRegistrationSpreadsheet.refreshCells(cell);
    }
  }

  private List<String> getTableHeaders() {
    return List.of("Sample Name", "Biological Replicate", "Treatment", "Cell Line", "Species",
        "Specimen", "Analyte", "Comment");

  }

  public void reset() {
    resetChildValues();
  }

  private void resetChildValues() {
    sampleRegistrationSpreadsheet.reset();
    styleSampleRegistrationSpreadSheet();
  }

}
