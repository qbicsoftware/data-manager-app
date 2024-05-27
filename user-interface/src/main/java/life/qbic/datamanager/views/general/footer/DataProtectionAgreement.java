package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.DataManagerLayout;
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
@Route(value = "privacy-agreement", layout = DataManagerLayout.class)
@AnonymousAllowed
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
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. "
            + "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."
            + "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. "
            + "Lorem ipsum dolor sit amet,");
    dataProtectionInformation.addSection("Copyright",
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. "
            + "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."
            + "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. "
            + "Lorem ipsum dolor sit amet,");
    dataProtectionInformation.addSection("Disclaimer",
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. "
            + "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."
            + "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. "
            + "Lorem ipsum dolor sit amet,");
  }

  private void setDataProtectionCardInformation() {
    dataProtectionCardInformation.setTitle(
        "\"At vero eos et accusam\"");
    dataProtectionCardInformation.addSection("Address", createAddress());
    dataProtectionCardInformation.addSection("Switchboard:", new Span("01234567890"));
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
    section.add("""
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "\s
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. "\s
            + "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "\s
        """);
    return section;
  }

  private Anchor createCentralWebsiteAnchor() {
    return new Anchor(
        "https://uni-tuebingen.de/forschung/forschungsinfrastruktur/zentrum-fuer-quantitative-biologie-qbic/",
        "At vero eos et accusam", AnchorTarget.BLANK);
  }

  private Div createVatIdentificationSection() {
    Div section = new Div();
    section.add(
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr"
            + "sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat");
    return section;
  }

}
