package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.datamanager.views.DataManagerLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Footer Component
 * <p>
 * Basic Footer Component routing the user to main components with additional information legal or
 * otherwise such as {@link LegalNotice} and {@link DataPrivacyAgreement}
 */
@SpringComponent
@UIScope
@ParentLayout(DataManagerLayout.class)
public class FooterComponent extends Footer {

  private final RouterLink legalNoticeLink = new RouterLink("LegalNotice", LegalNotice.class);
  private final RouterLink dataPrivacyAgreement = new RouterLink("Data Privacy Agreement",
      DataPrivacyAgreement.class);
  private final Anchor sourceCodeLink = new Anchor(
      "", "Source", AnchorTarget.BLANK);

  @Autowired
  public FooterComponent(
      @Value("${qbic.communication.data-manager.source-code.url}") String sourceCodeUrl,
      @Value("${qbic.communication.contact.email}") String contactEmail) {
    setId("data-manager-footer");
    add(new Anchor(dataPrivacyAgreement.getHref(), "Data Privacy Agreement", AnchorTarget.BLANK),
        new Anchor(legalNoticeLink.getHref(), "Legal Notice", AnchorTarget.BLANK),
        new Anchor(
            "", "Source", AnchorTarget.BLANK));
  }
}
