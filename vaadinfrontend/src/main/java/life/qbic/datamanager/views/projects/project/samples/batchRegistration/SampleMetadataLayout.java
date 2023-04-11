package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import java.io.IOException;
import java.io.InputStream;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
class SampleMetadataLayout extends VerticalLayout {

  public Spreadsheet metadataSpreadsheet = new Spreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button nextButton = new Button("Next");

  SampleMetadataLayout() {
    initContent();
    this.setSizeFull();
  }

  private void initContent() {
    add(metadataSpreadsheet);
    styleMetaDataSheet();
    initButtonLayout();
  }

  private void initButtonLayout() {
    HorizontalLayout sampleMetadataButtons = new HorizontalLayout();
    nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sampleMetadataButtons.add(cancelButton, nextButton);
    this.setAlignSelf(Alignment.END, sampleMetadataButtons);
    add(sampleMetadataButtons);
  }

  private void styleMetaDataSheet() {
    metadataSpreadsheet.setSizeFull();
  }

  public void generateMetadataSpreadsheet(MetaDataTypes metaDataTypes) throws IOException {
    //Todo Make spreadsheet factory
    String resourcePath = "";
    switch (metaDataTypes) {
      case PROTEOMICS -> resourcePath = "MetadataSheets/Suggested PXP Metadata.xlsx";
      case LIGANDOMICS -> resourcePath = "MetadataSheets/Suggested_Ligandomics.xlsx";
      case TRANSCRIPTOMIC_GENOMICS ->
          resourcePath = "vaadinfrontend/src/main/resources/MetadataSheets/Suggested NGS Metadata.xlsx";
      case METABOLOMICS -> resourcePath = "MetadataSheets/Suggestion Metabolomics_LCMS.xlsx";
    }
    if (!resourcePath.isBlank()) {
      InputStream resourceAsStream = getClass().getResourceAsStream(resourcePath);
      if (resourceAsStream != null) {
        metadataSpreadsheet = new Spreadsheet(resourceAsStream);
        styleMetaDataSheet();
      }
    }
  }

  public void reset() {
    resetChildValues();
  }

  private void resetChildValues() {
    metadataSpreadsheet.reset();
    styleMetaDataSheet();
  }

}
