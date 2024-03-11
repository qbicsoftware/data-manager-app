package life.qbic.datamanager.views.projects.project.rawdata;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.io.Serializable;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.CodeBlock;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.logging.api.Logger;
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
  private static final Logger log = logger(RawDataDownloadInformationComponent.class);
  private Context context;
  private static int sectionNumber = 0;
  private final Button navigateToPatPageButton = new Button("Go to Personal Access Token");
  private final Button generateDownloadUrlsButton = new Button("Download URL list");


  public RawDataDownloadInformationComponent() {
    Span title = new Span("Data Download");
    title.addClassName("title");
    addComponentAsFirst(title);
    CodeBlock codeBlock = new CodeBlock("curl", "<token>", "<URL>");
    Div generateTokenSection = generateSection("Generate Token",
        "Generate a Personal Access Token (PAT)",
        navigateToPatPageButton);
    Div downloadRawDataSection = generateSection("Download RAW data URLs",
        "Download the file with a list of URLs corresponding to the measurement you want to download.",
        generateDownloadUrlsButton);
    Div runCurlCommandSection = generateSection("Run cURL command",
        "Install cURL on your system, open it and enter the following command once for each file you want to download",
        codeBlock);
    add(generateTokenSection);
    add(downloadRawDataSection);
    add(runCurlCommandSection);
    addClassName("raw-data-download-information-component");
  }

  private static Div generateSection(String title, String text, Component... components) {
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

  public void addDownloadUrlListener(ComponentEventListener<ClickEvent<Button>> listener) {
    generateDownloadUrlsButton.addClickListener(listener);
  }
}
