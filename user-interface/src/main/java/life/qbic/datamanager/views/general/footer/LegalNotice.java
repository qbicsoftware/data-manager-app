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
 * Legal Notice
 * <p>
 * Legal Notice Main Component showing the relevant legal information for the data-manager application
 */
@Route(value = "legal-notice", layout = DataManagerLayout.class)
@PageTitle("Impressum / Legal Notice")
@AnonymousAllowed
public class LegalNotice extends Main {

  @Serial
  private static final long serialVersionUID = -2176925703483086940L;
  private static final Logger log = LoggerFactory.logger(LegalNotice.class);
  private static final String LEGAL_NOTICE_HTML_PATH = "impressum/LegalNotice.html";

  public LegalNotice() {
    String legalNoticeHtmlContent = getLegalNoticeHtmlFromResource();
    // Replace href="#" with href="currentPath#id"
    String legalNoticeWithAnchors = legalNoticeHtmlContent.replaceAll(
        "href=\"#", replaceWithCurrentUrl());
    Html html = new Html(legalNoticeWithAnchors);
    add(html);
    addClassName("legal-notice");
    log.debug(String.format(
        "New instance for %s(#%s) created",
        this.getClass().getSimpleName(), System.identityHashCode(this)));
  }

  private String getLegalNoticeHtmlFromResource() {
    String htmlContent = "";
    try (InputStream inputStream = getClass().getClassLoader()
        .getResourceAsStream(LEGAL_NOTICE_HTML_PATH)) {
      if (inputStream == null) {
        throw new IOException("Resource not found in path " + LEGAL_NOTICE_HTML_PATH);
      }
      htmlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(
          "Cannot get html content for legal notice: " + LEGAL_NOTICE_HTML_PATH,
          e);
    }
    return htmlContent;
  }

  private String replaceWithCurrentUrl() {
    return "href=\"" + RouteConfiguration.forSessionScope().getUrl(this.getClass()) + "#";
  }
}
