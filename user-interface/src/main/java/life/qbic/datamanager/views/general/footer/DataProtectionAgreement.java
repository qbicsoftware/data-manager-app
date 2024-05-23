package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * Data protection agreement
 * <p>
 * Main area showing the relevant legal information for the data handling and orotection performed within the
 * data-manager application
 */
@SpringComponent
@UIScope
@Route(value = "privacy-agreement", layout = UserMainLayout.class)
@PermitAll
public class DataProtectionAgreement extends Main {

  @Serial
  private static final long serialVersionUID = 3892163770509236678L;
  private static final Logger log = LoggerFactory.logger(DataProtectionAgreement.class);
  private final InformationComponent dataProtectionInformation = new InformationComponent();
  private final InformationCardComponent dataProtectionCardInformation = new InformationCardComponent();

  public DataProtectionAgreement() {
    setDataProtectionInformation();
    setDataProtectionCardInformation();
    add(dataProtectionInformation);
    add(dataProtectionCardInformation);
    dataProtectionInformation.addClassName("data-protection-information");
    dataProtectionCardInformation.addClassName("data-protection-card-information");
    addClassName("data-protection-agreement");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s) and %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        dataProtectionInformation.getClass().getSimpleName(),
        System.identityHashCode(dataProtectionInformation),
        dataProtectionCardInformation.getClass().getSimpleName(),
        System.identityHashCode(dataProtectionCardInformation)));
  }

  private void setDataProtectionInformation() {
    dataProtectionInformation.setTitle("Data Protection Agreement");
    dataProtectionInformation.addSection("External links",
        "This website of the Quantitative Biology Center (QBiC) also contains appropriately marked links or references to third-party websites. "
            + "Through the link, the Quantitative Biology Center (QBiC) merely provides access to the use of this content. "
            + "This does not imply consent to the content of linked third-party sites. "
            + "The Quantitative Biology Center (QBiC) therefore assumes no responsibility for the availability or content of such websites and no liability for any damage or injury resulting from the use, regardless of their nature, of such content. "
            + "The provider of the respective site is solely liable for this.");
    dataProtectionInformation.addSection("Copyright",
        "Copyright (c), Quantitative Biology Center (QBiC). "
            + "All rights reserved. "
            + "All content published on this website (layout, text, images, graphics, video and sound files, etc.) is subject to copyright."
            + "Any use not permitted by copyright law requires the prior express consent of the Quantitative Biology Center (QBiC). "
            + "This applies in particular to the reproduction, editing, translation, storage, processing or reproduction of content in databases or other electronic media and systems. "
            + "Photocopies and downloads of web pages may be made for private, scientific and non-commercial use. "
            + "The copyright for the word-image trademark lies expressly with the Quantitative Biology Center (QBiC). "
            + "We expressly allow and welcome the quoting of our documents and web pages as well as the setting of links to our website.");
    dataProtectionInformation.addSection("Disclaimer",
        "The information on this website has been carefully compiled and checked to the best of our knowledge and belief. "
            + "However, no guarantee is given - neither expressly nor tacitly - for the completeness, correctness or timeliness or the availability at all times of the information provided. "
            + "Liability for damages arising from the use or non-use of the information provided on this website is - to the extent permitted by law - excluded.");
  }

  private void setDataProtectionCardInformation() {
    dataProtectionCardInformation.setTitle(
        "General information in accordance with Section 5 TMG, Section 55 RStVG");
    dataProtectionCardInformation.addSection("Address", createAddress());
    dataProtectionCardInformation.addSection("Switchboard:", new Span("+49 (0) 70 71/29-72163"));
    dataProtectionCardInformation.addSection("Central email address:", createCentralEmail());
    dataProtectionCardInformation.addSection("Internet address:", createCentralWebsiteAnchor());
    dataProtectionCardInformation.addSection("VAT identification number:",
        createVatIdentificationSection());
  }

  private Anchor createCentralEmail() {
    Anchor emailLink = new Anchor();
    String email = "support@qbic.zendesk.com";
    emailLink.setHref(String.format("mailto:%s", email));
    emailLink.setText(email);
    return emailLink;
  }

  private Div createAddress() {
    Div section = new Div();
    section.add(
        "Quantitative Biology Center (QBiC), University of Tübingen"
            + "Auf der Morgenstelle 10 "
            + "72076 Tübingen"
            + "The Quantitative Biology Center (QBiC) is represented by the managing director Dr. Sven Nahnsen (email: sven.nahnsen [at] uni-tuebingen.de).\n");
    return section;
  }

  private Anchor createCentralWebsiteAnchor() {
    return new Anchor(
        "https://uni-tuebingen.de/forschung/forschungsinfrastruktur/zentrum-fuer-quantitative-biologie-qbic/",
        "http://www.qbic.uni-tuebingen.de", AnchorTarget.BLANK);
  }

  private Div createVatIdentificationSection() {
    Div section = new Div();
    section.add(
        "in accordance with Section 27a of the Sales Tax Act:8DE812383453 "
            + "Supervisory authority Ministry of Science, "
            + "Research and Art Baden Württemberg");
    return section;
  }

}
