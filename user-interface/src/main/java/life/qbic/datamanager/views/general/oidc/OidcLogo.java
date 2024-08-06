package life.qbic.datamanager.views.general.oidc;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;

/**
 * OidcLogo shown within the data manager application.
 * Logo source and image path is based on the information provided within the {@link OidcType}
 */
public class OidcLogo extends Image {

  private final OidcType oidcType;

  public OidcLogo(OidcType oidcType) {
    this.oidcType = oidcType;
    addClassName("oidc-logo");
    setSrc(getLogoResource());
  }

  private AbstractStreamResource getLogoResource() {
    String oidcLogoSrc = oidcType.getLogoPath();
    //Image source cannot contain a "/" so we look for the actual file name independent in which folder path it is contained.
    if (oidcLogoSrc.contains("/")) {
      oidcLogoSrc = oidcType.getLogoPath().substring(oidcType.getLogoPath().lastIndexOf('/') + 1);
    }
    return new StreamResource(oidcLogoSrc,
        () -> getClass().getClassLoader().getResourceAsStream(oidcType.getLogoPath()));
  }
}
