package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;

/**
 * A factory bean to create page footers. This is needed as footers can not be added to multiple state/page trees. Thus a new instance is required for every view.
 * The factory solves the issue where a view is created before a session is initialized.
 */
@SpringComponent
public class FooterComponentFactory implements Supplier<FooterComponent> {

  private final String sourceCodeUrl;
  private final String documentationUrl;
  private final String apiUrl;
  private final String contactEmail;
  private final String contactSubject;

  public FooterComponentFactory(
      @Value("${qbic.communication.data-manager.source-code.url}") String sourceCodeUrl,
      @Value("${qbic.communication.documentation.url}") String documentationUrl,
      @Value("${qbic.communication.api.url}") String apiUrl,
      @Value("${qbic.communication.contact.email}") String contactEmail,
      @Value("${qbic.communication.contact.subject}") String contactSubject) {
    this.sourceCodeUrl = sourceCodeUrl;
    this.documentationUrl = documentationUrl;
    this.apiUrl = apiUrl;
    this.contactEmail = contactEmail;
    this.contactSubject = contactSubject;
  }

  @Override
  public FooterComponent get() {
    final RouterLink legalNoticeLink = new RouterLink("LegalNotice", LegalNotice.class);
    final RouterLink dataPrivacyAgreement = new RouterLink("Data Privacy Agreement",
        DataPrivacyAgreement.class);
    return new FooterComponent(sourceCodeUrl, documentationUrl, apiUrl, contactEmail,
        contactSubject, legalNoticeLink.getHref(), dataPrivacyAgreement.getHref());
  }
}
