package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
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
 *   <li>A brand block announces the product ("Data Manager") and links the
 *   University of Tübingen logo to uni-tuebingen.de.</li>
 *   <li>Internal routes ({@link LegalNotice}, {@link DataPrivacyAgreement}) use
 *   {@link RouterLink}s, enabling client-side navigation in the same tab.</li>
 *   <li>External links open in a new tab with {@code rel="noopener noreferrer"}.</li>
 *   <li>A copyright attribution bar closes the footer.</li>
 * </ul>
 * <p>
 * Layout: the {@code .app-footer} bar is full-bleed (the settings layout's grid spans it
 * across all its tracks), while its content lives in an inner {@code .app-footer-inner}
 * shell capped at {@code --content-shell-max-width} and centered, matching the project
 * overview and settings hub. The brand block takes one grid column, the link groups the
 * other, and the bottom bar spans both.
 * <p>
 * The component is prototype-scoped: a Vaadin component instance must not be shared
 * between layouts, so every layout receives its own instance from Spring. No
 * hand-written factory is required.
 */
@SpringComponent
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FooterComponent extends Footer {

  private static final String UT_LOGO_PATH = "login/university-tuebingen-logo.svg";
  private static final String BRAND_NAME = "Data Manager";
  private static final String BRAND_SUBTITLE_LINE_1 = "University of Tübingen Life Science";
  private static final String BRAND_SUBTITLE_LINE_2 = "Data Management";

  public FooterComponent(
      @Value("${qbic.communication.data-manager.source-code.url}") String sourceCodeUrl,
      @Value("${qbic.communication.documentation.url}") String documentationUrl,
      @Value("${qbic.communication.api.url}") String apiUrl,
      @Value("${qbic.communication.contact.email}") String contactEmail,
      @Value("${qbic.communication.contact.subject}") String contactSubject) {
    addClassName("app-footer");

    Div inner = new Div(brand(), linkGroups(sourceCodeUrl, documentationUrl, apiUrl, contactEmail,
        contactSubject), bottomBar());
    inner.addClassName("app-footer-inner");

    add(inner);
  }

  /**
   * Creates the brand block: product name, subtitle and the University of Tübingen
   * logo linking to the university website.
   */
  private static Div brand() {
    Span name = new Span(BRAND_NAME);
    name.addClassName("app-footer-brand-name");
    // Two spans: the second is rendered as a block (see footer.css), so the line
    // breaks before "Data Management" and the name stays together for the reader.
    Span subtitle = new Span(
        new Span(BRAND_SUBTITLE_LINE_1),
        new Span(BRAND_SUBTITLE_LINE_2));
    subtitle.addClassName("app-footer-brand-sub");

    Anchor logoLink = new Anchor("https://uni-tuebingen.de/", getUTLogo());
    logoLink.setTarget(AnchorTarget.BLANK);
    logoLink.addClassName("app-footer-ut-logo");
    logoLink.getElement().setAttribute("rel", "noopener noreferrer");
    logoLink.getElement().setAttribute("aria-label",
        "University of Tübingen (opens in new tab)");

    Div brand = new Div(name, subtitle, logoLink);
    brand.addClassName("app-footer-brand");
    return brand;
  }

  /**
   * Streams the University of Tübingen logo SVG through Vaadin's
   * {@link DownloadHandler} mechanism so the image resolves independently of the
   * current route (plain relative URLs break on deep routes). The pattern mirrors
   * {@link life.qbic.datamanager.views.LandingPageTitleAndLogo}.
   */
  private static Image getUTLogo() {
    DownloadHandler downloadHandler = DownloadHandler.fromInputStream(event ->
        new DownloadResponse(getUTLogoStream(), "university_tuebingen_logo.svg", "image/svg+xml",
            -1));
    Image utLogo = new Image(downloadHandler, "University of Tübingen");
    utLogo.addClassName("ut-logo");
    return utLogo;
  }

  private static java.io.InputStream getUTLogoStream() {
    java.io.InputStream stream =
        FooterComponent.class.getClassLoader().getResourceAsStream(UT_LOGO_PATH);
    if (stream == null) {
      throw new IllegalStateException("University of Tübingen logo resource not found: "
          + UT_LOGO_PATH);
    }
    return stream;
  }

  private static Div linkGroups(String sourceCodeUrl, String documentationUrl, String apiUrl,
      String contactEmail, String contactSubject) {
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
    return linkGroups;
  }

  private static Div bottomBar() {
    Div bottomBar = new Div(
        new Span("© %s QBiC – Quantitative Biology Center, University of Tübingen"
            .formatted(Year.now())));
    bottomBar.addClassName("app-footer-bottom");
    return bottomBar;
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
