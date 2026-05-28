package life.qbic.datamanager.views.general.oidc;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * OidcLogo shown within the data manager application.
 * Logo source and image path is based on the information provided within the {@link OidcType}
 */
public class OidcLogo extends Image {

  private final OidcType oidcType;

  public OidcLogo(OidcType oidcType) {
    this.oidcType = oidcType;
    setSrc(getLogoResource());
  }

  private DownloadHandler getLogoResource() {
    String oidcLogoSrc = oidcType.getLogoPath();
//    //Image source cannot contain a "/" so we look for the actual file name independent in which folder path it is contained.
//    if (oidcLogoSrc.contains("/")) {
//      oidcLogoSrc = oidcType.getLogoPath().substring(oidcType.getLogoPath().lastIndexOf('/') + 1);
//    }
//    https://docs.oracle.com/javase/8/docs/technotes/guides/lang/resources.html
    return DownloadHandler.forClassResource(getClass(), oidcLogoSrc);

//        event ->
//            new DownloadResponse(
//                getClass().getClassLoader().getResourceAsStream(oidcType.getLogoPath()),
//                "orcid_logo.svg",
//                "image/svg+xml",
//                -1L
//            ));

//            oidcLogoSrc,
//            () -> getClass().getClassLoader().getResourceAsStream(oidcType.getLogoPath()));
  }
}
