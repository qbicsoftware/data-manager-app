package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.router.ParentLayout;
import life.qbic.datamanager.views.DataManagerLayout;

/**
 * Footer Component
 * <p>
 * Basic Footer Component routing the user to main components with additional information legal or
 * otherwise such as {@link LegalNotice} and {@link DataPrivacyAgreement}
 */
@ParentLayout(DataManagerLayout.class)
//@SessionScope
public class FooterComponent extends Footer {

  FooterComponent(
      String sourceCodeUrl,
      String documentationUrl,
      String apiUrl,
      String contactEmail,
      String contactSubject,
      String legalNoticeHref,
      String dataPrivacyAgreementHref) {
    setId("data-manager-footer");

    add(new Anchor(dataPrivacyAgreementHref, "Data Privacy Agreement", AnchorTarget.BLANK),
        new Anchor(legalNoticeHref, "Legal Notice", AnchorTarget.BLANK),
        new Anchor(documentationUrl, "Documentation", AnchorTarget.BLANK),
        new Anchor(apiUrl, "API", AnchorTarget.BLANK),
        new Anchor(sourceCodeUrl, "Source", AnchorTarget.BLANK),
        new Anchor("mailto:" + contactEmail.strip() + "?subject=" + contactSubject.strip(),
            "Contact"));
  }
}
