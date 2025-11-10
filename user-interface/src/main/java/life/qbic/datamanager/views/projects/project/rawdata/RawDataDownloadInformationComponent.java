package life.qbic.datamanager.views.projects.project.rawdata;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import life.qbic.datamanager.views.general.CodeBlock;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;


/**
 * Raw Data Download Information Component
 * <p></p>
 * Informs the user about the steps necessary to download the raw data associated with the
 * {@link MeasurementMetadata} of the {@link Experiment} of interest
 */

@SpringComponent
@UIScope
@PermitAll
public class RawDataDownloadInformationComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 7161304802207319605L;
  private int sectionNumber;
  private final Button navigateToPatPageButton = new Button("Go to Personal Access Token");
  private final Button generateDownloadUrlsButton = new Button("Download URL list");

  public RawDataDownloadInformationComponent() {
    Span title = new Span("Data Download");
    title.addClassName("title");
    addComponentAsFirst(title);
    initializeSections();
    addClassName("raw-data-download-information-component");
  }

  private void initializeSections() {
    sectionNumber = 0;
    Div generateTokenSection = generateSection("Generate Token",
        "Generate a Personal Access Token (PAT)",
        navigateToPatPageButton);
    Div downloadRawDataSection = generateSection("Export Dataset URLs",
        "Export the file with a list of dataset URLs corresponding to the measurements you want to download.");
    TabSheet codeTabSheet = new TabSheet();
    CodeBlock curlCodeBlock = new CodeBlock("curl", "--parallel", "--fail", "-OJ", "-H",
        "\"Authorization: Bearer <ACCESS_TOKEN>\"",
        "<DATASET_URL>");
    CodeBlock wgetCodeBlock = new CodeBlock("wget", "--content-disposition", "--trust-server-names",
        "--header",
        "\"Authorization: Bearer <ACCESS_TOKEN>\"",
        "<DATASET_URL>");
    codeTabSheet.add("curl", curlCodeBlock);
    codeTabSheet.add("wget", wgetCodeBlock);
    Div runCurlCommandSection = generateSection("Download Data",
        "Install cURL or wGet on your system, open the command line and enter one of the following command once for each file you want to download",
        codeTabSheet);
    Span additionalInfoSection = generateAdditionalInformationSection();
    add(generateTokenSection, downloadRawDataSection, runCurlCommandSection, additionalInfoSection);
  }

  private Span generateAdditionalInformationSection() {
    Anchor downloadGuideLink = new Anchor(
        "https://qbicsoftware.github.io/research-data-management/latest/rawdata/raw_data_download/#download-raw-data",
        "here", AnchorTarget.BLANK);
    Text additionalInformationText = new Text("Learn more about how to download the datasets ");
    return new Span(additionalInformationText, downloadGuideLink);
  }

  private Div generateSection(String title, String text, Component... components) {
    Div section = new Div();
    section.addClassName("section");
    Span sectionTitle = new Span(title);
    Avatar sectionNumberIcon = new Avatar(String.valueOf(++sectionNumber));
    sectionNumberIcon.addThemeVariants(AvatarVariant.LUMO_XSMALL);
    Span titleWithNumber = new Span(sectionNumberIcon, sectionTitle);
    titleWithNumber.addClassName("section-title");
    section.addComponentAsFirst(titleWithNumber);
    Paragraph paragraph = new Paragraph(text);
    section.add(paragraph);
    section.add(components);
    return section;
  }

  public void addPersonalAccessTokenNavigationListener(
      ComponentEventListener<ClickEvent<Button>> listener) {
    navigateToPatPageButton.addClickListener(listener);
  }

  /**
   * Informs the listener that the user intends to download the urls for the selected measurements
   *
   * @param listener listener which will be informed if the triggering component has been clicked
   */
  public void addDownloadUrlListener(ComponentEventListener<ClickEvent<Button>> listener) {
    generateDownloadUrlsButton.addClickListener(listener);
  }
}
