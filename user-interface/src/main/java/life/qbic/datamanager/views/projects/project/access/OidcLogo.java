package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
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
