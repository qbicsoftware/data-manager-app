package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import life.qbic.datamanager.views.DataManagerLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * Data privacy agreement
 * <p>
 * Main area showing the relevant legal information for the data handling and orotection performed
 * within the data-manager application
 */
@Route(value = "data-privacy-agreement", layout = DataManagerLayout.class)
@AnonymousAllowed
@PageTitle("Impressum / Data Privacy Agreement")
public class DataPrivacyAgreement extends Main {

  @Serial
  private static final long serialVersionUID = 3892163770509236678L;
  private static final Logger log = LoggerFactory.logger(DataPrivacyAgreement.class);
  private static final String DATA_PRIVACY_AGREEMENT_HTML = "impressum/DataPrivacyAgreement.html";

  public DataPrivacyAgreement() {
    String dataPrivacyAgreementHtmlContent = getDataPrivacyAgreementHtmlContent();
    // Replace href="#" with href="currentPath#id"
    String dataPrivacyAgreementWithAnchors = dataPrivacyAgreementHtmlContent.replaceAll(
        "href=\"#", replaceWithCurrentUrl());
    Html html = new Html(dataPrivacyAgreementWithAnchors);
    add(html);
    addClassName("data-privacy-agreement");
    log.debug(String.format(
        "New instance for %s(#%s) created",
        this.getClass().getSimpleName(), System.identityHashCode(this)));
  }

  private String getDataPrivacyAgreementHtmlContent() {
    String htmlContent = "";
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
        DATA_PRIVACY_AGREEMENT_HTML)) {
      if (inputStream == null) {
        throw new IOException("Resource not found in path " + DATA_PRIVACY_AGREEMENT_HTML);
      }
      htmlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Cannot get html content for the data privacy agreement: "
          + DATA_PRIVACY_AGREEMENT_HTML,
          e);
    }
    return htmlContent;
  }

  private String replaceWithCurrentUrl() {
    return "href=\"" + RouteConfiguration.forSessionScope().getUrl(this.getClass()) + "#";
  }
}
