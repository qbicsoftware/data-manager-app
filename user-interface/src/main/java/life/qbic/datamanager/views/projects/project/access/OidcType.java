package life.qbic.datamanager.views.projects.project.access;

/**
 * The Oidc Types enum contains the associated logo path, the issuer and the url of the
 * corresponding oidc a user has provided
 */
public enum OidcType {
  Orcid("https://orcid.org", "login/orcid_logo.svg", "https://orcid.org/", "Orcid"),
  SandboxOrcid("https://sandbox.orcid.org", "login/orcid_logo.svg", "https://sandbox.orcid.org/",
      "OrcId");

  private final String issuer;
  private final String logoPath;
  private final String url;
  private final String name;

  OidcType(String issuer, String logoPath, String url, String name) {
    this.issuer = issuer;
    this.logoPath = logoPath;
    this.url = url;
    this.name = name;
  }

  public String getIssuer() {
    return issuer;
  }

  public String getLogoPath() {
    return logoPath;
  }

  public String getUrl() {
    return url;
  }

  public String getName() {
    return name;
  }
}
