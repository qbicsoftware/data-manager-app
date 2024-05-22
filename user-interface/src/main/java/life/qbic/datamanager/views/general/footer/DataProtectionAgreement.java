package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.InformationComponent;
import life.qbic.datamanager.views.general.Main;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
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
  private final Span title = new Span("Data Protection Agreement");

  public DataProtectionAgreement() {
    title.addClassName("main-title");
    addComponentAsFirst(title);
    setDataProtectionInformation();
    add(dataProtectionInformation);
    dataProtectionInformation.addClassName("data-protection-information");
    addClassName("data-protection-agreement");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        dataProtectionInformation.getClass().getSimpleName(),
        System.identityHashCode(dataProtectionInformation)));
  }

  private void setDataProtectionInformation() {
    dataProtectionInformation.addSection("External links",
        "This website of the Quantitative Biology Center (QBiC) also contains appropriately marked links or references to third-party websites. Through the link, the Quantitative Biology Center (QBiC) merely provides access to the use of this content. This does not imply consent to the content of linked third-party sites. The Quantitative Biology Center (QBiC) therefore assumes no responsibility for the availability or content of such websites and no liability for any damage or injury resulting from the use, regardless of their nature, of such content. The provider of the respective site is solely liable for this.");
    dataProtectionInformation.addSection("Copyright",
        "Copyright (c), Quantitative Biology Center (QBiC). All rights reserved. All content published on this website (layout, text, images, graphics, video and sound files, etc.) is subject to copyright. Any use not permitted by copyright law requires the prior express consent of the Quantitative Biology Center (QBiC). This applies in particular to the reproduction, editing, translation, storage, processing or reproduction of content in databases or other electronic media and systems. Photocopies and downloads of web pages may be made for private, scientific and non-commercial use. The copyright for the word-image trademark lies expressly with the Quantitative Biology Center (QBiC). We expressly allow and welcome the quoting of our documents and web pages as well as the setting of links to our website.");
    dataProtectionInformation.addSection("Disclaimer",
        "The information on this website has been carefully compiled and checked to the best of our knowledge and belief. However, no guarantee is given - neither expressly nor tacitly - for the completeness, correctness or timeliness or the availability at all times of the information provided. Liability for damages arising from the use or non-use of the information provided on this website is - to the extent permitted by law - excluded.");
  }

}
