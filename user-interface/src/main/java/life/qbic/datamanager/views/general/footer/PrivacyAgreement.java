package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * Data protection agreement
 * <p>
 * Main area showing the relevant legal information for the data handling and orotection performed
 * within the data-manager application
 */
@SpringComponent
@UIScope
@Route(value = "privacy-agreement", layout = DataManagerLayout.class)
@AnonymousAllowed
public class PrivacyAgreement extends Main {

  @Serial
  private static final long serialVersionUID = 3892163770509236678L;
  private static final Logger log = LoggerFactory.logger(PrivacyAgreement.class);
  private static final String PRIVACY_AGREEMENT_PATH = "impressum/PrivacyAgreement.html";

  public PrivacyAgreement() {
    String privacyAgreementHtmlContent = getPrivacyAgreementHtmlContent();
    // Replace href="#" with href="currentPath#id"
    String privacyAgreementWithAnchors = privacyAgreementHtmlContent.replaceAll(
        "href=\"#", replaceWithCurrentUrl());
    Html html = new Html(privacyAgreementWithAnchors);
    add(html);
    addClassName("privacy-agreement");
    log.debug(String.format(
        "New instance for %s(#%s) created",
        this.getClass().getSimpleName(), System.identityHashCode(this)));
  }

  private String getPrivacyAgreementHtmlContent() {
    String htmlContent = "";
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
        PRIVACY_AGREEMENT_PATH)) {
      if (inputStream == null) {
        throw new IOException("Resource not found in path " + PRIVACY_AGREEMENT_PATH);
      }
      htmlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Cannot get html content for the privacy agreement: "
          + PRIVACY_AGREEMENT_PATH,
          e);
    }
    return htmlContent;
  }

  private String replaceWithCurrentUrl() {
    return "href=\"" + RouteConfiguration.forSessionScope().getUrl(this.getClass()) + "#";
  }
}
