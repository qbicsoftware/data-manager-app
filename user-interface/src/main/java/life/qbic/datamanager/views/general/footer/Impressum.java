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
 * Impressum
 * <p>
 * Impressum Main Component showing the relevant legal information for the data-manager application
 */
@SpringComponent
@UIScope
@Route(value = "impressum", layout = DataManagerLayout.class)
@AnonymousAllowed
public class Impressum extends Main {

  @Serial
  private static final long serialVersionUID = -2176925703483086940L;
  private static final Logger log = LoggerFactory.logger(Impressum.class);
  private final InformationComponent impressumInformation = new InformationComponent();
  private final InformationCardComponent impressumCardInformation = new InformationCardComponent();

  public Impressum() {
    setImpressumInformation();
    setImpressumCardInformation();
    add(impressumInformation);
    add(impressumCardInformation);
    impressumInformation.addClassName("impressum-information");
    impressumCardInformation.addClassName("impressum-card-information");
    addClassName("impressum");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s) and %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        impressumInformation.getClass().getSimpleName(),
        System.identityHashCode(impressumInformation),
        impressumCardInformation.getClass().getSimpleName(),
        System.identityHashCode(impressumCardInformation)));
  }

  private void setImpressumInformation() {
    impressumInformation.setTitle("Impressum");
    impressumInformation.addSection("External links",
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. "
            + "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."
            + "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. "
            + "Lorem ipsum dolor sit amet,");
    impressumInformation.addSection("Copyright",
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. "
            + "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."
            + "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. "
            + "Lorem ipsum dolor sit amet,");
    impressumInformation.addSection("Disclaimer",
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. "
            + "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. "
            + "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."
            + "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. "
            + "Lorem ipsum dolor sit amet,");
  }


  private void setImpressumCardInformation() {
    impressumCardInformation.setTitle(
        "At vero eos et accusam");
    impressumCardInformation.addSection("Address", createAddress());
    impressumCardInformation.addSection("Switchboard:", new Span("01234567890"));
    impressumCardInformation.addSection("Central email address:", createCentralEmail());
    impressumCardInformation.addSection("Internet address:", createCentralWebsiteAnchor());
    impressumCardInformation.addSection("VAT identification number:",
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
