package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.time.Year;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

/**
 * The application footer shown on every page of the data manager.
 * <p>
 * The footer follows common web best practices:
 * <ul>
 *   <li>Semantic markup: a {@code <footer>} element containing labelled
 *   {@code <nav>} landmarks, one per link group, so assistive technologies can
 *   announce and navigate them.</li>
 *   <li>Internal routes ({@link LegalNotice}, {@link DataPrivacyAgreement}) use
 *   {@link RouterLink}s, enabling client-side navigation in the same tab.</li>
 *   <li>External links open in a new tab with {@code rel="noopener noreferrer"}.</li>
 *   <li>A copyright attribution bar closes the footer.</li>
 * </ul>
 * <p>
 * The component is prototype-scoped: a Vaadin component instance must not be shared
 * between layouts, so every layout receives its own instance from Spring. No
 * hand-written factory is required.
 */
@SpringComponent
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FooterComponent extends Footer {

  public FooterComponent(
      @Value("${qbic.communication.data-manager.source-code.url}") String sourceCodeUrl,
      @Value("${qbic.communication.documentation.url}") String documentationUrl,
      @Value("${qbic.communication.api.url}") String apiUrl,
      @Value("${qbic.communication.contact.email}") String contactEmail,
      @Value("${qbic.communication.contact.subject}") String contactSubject) {
    addClassName("app-footer");

    Div linkGroups = new Div(
        linkGroup("Legal",
            new RouterLink("Data Privacy Agreement", DataPrivacyAgreement.class),
            new RouterLink("Legal Notice", LegalNotice.class)),
        linkGroup("Resources",
            externalLink(documentationUrl, "Documentation"),
            externalLink(apiUrl, "API"),
            externalLink(sourceCodeUrl, "Source Code")),
        linkGroup("Contact",
            contactLink(contactEmail, contactSubject)));
    linkGroups.addClassName("app-footer-links");

    Div bottomBar = new Div(
        new Span("© %s QBiC – Quantitative Biology Center, University of Tübingen"
            .formatted(Year.now())));
    bottomBar.addClassName("app-footer-bottom");

    add(linkGroups, bottomBar);
  }

  /**
   * Creates a labelled navigation landmark with a heading and the given links.
   */
  private static Nav linkGroup(String label, Component... links) {
    H2 heading = new H2(label);
    heading.addClassName("app-footer-heading");
    Nav nav = new Nav();
    nav.add(heading);
    nav.add(links);
    nav.addClassName("app-footer-group");
    nav.getElement().setAttribute("aria-label", label);
    return nav;
  }

  /**
   * Creates an external link that opens in a new tab, hardened against tab-napping.
   */
  private static Anchor externalLink(String url, String text) {
    Anchor anchor = new Anchor(url, text, AnchorTarget.BLANK);
    anchor.getElement().setAttribute("rel", "noopener noreferrer");
    return anchor;
  }

  private static Anchor contactLink(String email, String subject) {
    String address = email.strip();
    return new Anchor("mailto:%s?subject=%s".formatted(address, subject.strip()), address);
  }
}
