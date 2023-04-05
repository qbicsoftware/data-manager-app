package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.IOException;
import java.io.InputStream;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class RegisterBatchDialog extends Dialog {

  private final Span title = new Span("Upload Laboratory Metadata");
  private final VerticalLayout dialogContentLayout = new VerticalLayout();
  private final HorizontalLayout progressBarLayout = new HorizontalLayout();
  private final VerticalLayout generalInformationLayout = new VerticalLayout();
  private final TextField batchNameField = new TextField();
  private final RadioButtonGroup<metaDataTypes> dataTypeSelection = new RadioButtonGroup<>();
  private final Button generalInformationCancelButton = new Button("Cancel");
  private final Button generalInformationNextButton = new Button("Next");
  private final VerticalLayout sampleMetadataLayout = new VerticalLayout();
  private final Button sampleMetadataCancelButton = new Button("Cancel");
  private final Button sampleMetadataRegisterButton = new Button("Register");
  private Spreadsheet metadataSpreadsheet;
  private final RegisterBatchDialogHandler registerBatchDialogHandler;

  public RegisterBatchDialog() {
    add(title);
    title.addClassNames("text-2xl", "font-bold", "text-secondary");
    initContentLayout();
    registerBatchDialogHandler = new RegisterBatchDialogHandler();
  }

  private void initContentLayout() {
    initProgressBarLayout();
    initGeneralInformationLayout();
    initSampleMetaDataLayout();
    add(dialogContentLayout);
  }

  private void initProgressBarLayout() {
    dialogContentLayout.add(progressBarLayout);
    Button generalInformationButton = new Button("General Information");
    Button sampleMetadataButton = new Button("Sample Metadata");
    generalInformationButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_PRIMARY);
    sampleMetadataButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_PRIMARY);
    progressBarLayout.add(generalInformationButton, sampleMetadataButton);
  }

  private void initGeneralInformationLayout() {
    Span generalInformationHeader = new Span("General Information");
    generalInformationLayout.add(generalInformationHeader);
    generalInformationHeader.addClassNames("text-xl", "font-bold", "text-secondary");
    generalInformationLayout.add(batchNameField);
    Span dataTypeHeader = new Span("Type of Data");
    generalInformationLayout.add(dataTypeHeader);
    dataTypeHeader.addClassNames("text-l", "font-bold", "text-secondary");
    Span dataTypeDescription = new Span(
        "There is a minimum amount of information required. All samples must conform the expected metadata values. The most suitable checklist for sample registration depends on the type of the sample.");
    generalInformationLayout.add(dataTypeDescription);
    initDataTypeSelection();
    generalInformationLayout.add(dataTypeSelection);
    initGeneralInformationButtonLayout();
    dialogContentLayout.add(generalInformationLayout);
  }

  private void initDataTypeSelection() {
    dataTypeSelection.setItems(metaDataTypes.values());
    dataTypeSelection.setValue(dataTypeSelection.getListDataView().getItem(0));
    dataTypeSelection.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
    dataTypeSelection.setRenderer(new ComponentRenderer<>(metaDataTypes -> {
      Span metaDataType = new Span(metaDataTypes.metaDataType);
      Icon infoIcon = new Icon(VaadinIcon.INFO_CIRCLE);
      infoIcon.setTooltipText(metaDataTypes.metaDataDescription);
      return new HorizontalLayout(metaDataType, infoIcon);
    }));
  }

  private void initGeneralInformationButtonLayout() {
    generalInformationNextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    HorizontalLayout generalInformationButtons = new HorizontalLayout();
    generalInformationButtons.add(generalInformationCancelButton, generalInformationNextButton);
    generalInformationButtons.setWidthFull();
    generalInformationButtons.setAlignItems(Alignment.END);
    generalInformationLayout.add(generalInformationButtons);
  }

  private void initSampleMetaDataLayout() {
    HorizontalLayout sampleMetadataButtons = new HorizontalLayout();
    sampleMetadataButtons.add(sampleMetadataCancelButton, sampleMetadataRegisterButton);
    sampleMetadataButtons.setWidthFull();
    sampleMetadataButtons.setAlignItems(Alignment.END);
    sampleMetadataLayout.add(sampleMetadataButtons);
    dialogContentLayout.add(sampleMetadataLayout);
    sampleMetadataLayout.setVisible(false);
  }

  private class RegisterBatchDialogHandler {

    public RegisterBatchDialogHandler() {
      resetDialogueUponClosure();
      setGeneralInformationButtonsListeners();
      setSampleMetadataButtonsListeners();
    }

    private void setGeneralInformationButtonsListeners() {
      generalInformationCancelButton.addClickListener(buttonClickEvent -> resetAndClose());
      generalInformationNextButton.addClickListener(event -> {
        try {
          generateMetadataSpreadsheet();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        generalInformationLayout.setVisible(false);
        sampleMetadataLayout.setVisible(true);
      });
    }

    private void setSampleMetadataButtonsListeners() {
      sampleMetadataCancelButton.addClickListener(buttonClickEvent -> resetAndClose());
      //ToDo Register metadata
      sampleMetadataRegisterButton.addClickListener(buttonClickEvent -> resetAndClose());
    }

    private void generateMetadataSpreadsheet() throws IOException {
      //Todo Make spreadsheet factory
      String resourcePath = "";
      switch (dataTypeSelection.getValue()) {
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
        } else {
          metadataSpreadsheet = new Spreadsheet();
        }
        sampleMetadataLayout.add(metadataSpreadsheet);
        metadataSpreadsheet.setWidth(400, Unit.PIXELS);
        metadataSpreadsheet.setHeight(400, Unit.PIXELS);
      }
    }

    public void resetAndClose() {
      close();
      reset();
    }

    private void reset() {
      dialogContentLayout.getChildren().forEach(component -> {
        resetChildValues(component);
        resetChildValidation(component);
      });
      sampleMetadataLayout.setVisible(false);
      generalInformationLayout.setVisible(true);
    }

    private void resetChildValues(Component component) {
      component.getChildren().filter(comp -> comp instanceof HasValue<?, ?>)
          .forEach(comp -> ((HasValue<?, ?>) comp).clear());
    }

    private void resetChildValidation(Component component) {
      component.getChildren().filter(comp -> comp instanceof HasValidation)
          .forEach(comp -> ((HasValidation) comp).setInvalid(false));
    }

    private void resetDialogueUponClosure() {
      // Calls the reset method for all possible closure methods of the dialogue window:
      addDialogCloseActionListener(closeActionEvent -> resetAndClose());
    }
  }


  private enum metaDataTypes {
    LIGANDOMICS("Ligandomics", "Detailed Explanation for Ligandomics"), METABOLOMICS("Metabolomics",
        "Detailed Explanation for Metabolomics"), TRANSCRIPTOMIC_GENOMICS("Transciptomics/Genomics",
        "Detailed Explanation for Transcriptomics/Genomics"), PROTEOMICS("Proteomics",
        "Detailed Explanation for Proteomics");
    final String metaDataType;
    final String metaDataDescription;

    metaDataTypes(String metaDataType, String metaDataDescription) {
      this.metaDataType = metaDataType;
      this.metaDataDescription = metaDataDescription;
    }
  }

}
