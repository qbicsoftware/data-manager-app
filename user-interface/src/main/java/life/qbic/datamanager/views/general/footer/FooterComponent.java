package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Footer Component
 * <p>
 * Basic Footer Component routing the user to main components with additional information legal or otherwise
 * such as {@link Impressum}, {@link AboutUs}, {@link DataProtectionAgreement}
 */
@SpringComponent
@UIScope
public class FooterComponent extends Footer {

  private final RouterLink impressumLink = new RouterLink("Impressum", Impressum.class);
  private final RouterLink aboutUsLink = new RouterLink("About Us", AboutUs.class);
  private final RouterLink dataProtectionLink = new RouterLink("Data Protection Agreement",
      DataProtectionAgreement.class);
  private final Anchor sourceCodeLink = new Anchor(
      "https://github.com/qbicsoftware/data-manager-app", "Source");

  public FooterComponent() {
    addClassNames("data-manager-footer");
    add(aboutUsLink, dataProtectionLink, impressumLink, sourceCodeLink);
  }
}
