package life.qbic.datamanager.views.general.oidc;

/**
 * The Oidc Types enum contains the associated logo path, the issuer and the url of the
 * corresponding oidc a user has provided
 */
public enum OidcType {
  ORCID("https://orcid.org", "/login/orcid_logo.svg", "https://orcid.org/", "Orcid"),
  ORCID_SANDBOX("https://sandbox.orcid.org", "/login/orcid_logo.svg", "https://sandbox.orcid.org/",
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

  /**
   * The public researcher record for an ORCID iD.
   *
   * <p>One derivation, used by every view. It used to be built in five places, one of which hard-coded
   * {@code https://orcid.org} — so a sandbox-linked iD rendered as a link into the production registry:
   * a link that looks right and goes somewhere else.</p>
   *
   * @param oidcId the iD, in canonical form
   * @return the absolute record URL for <em>this</em> registry
   */
  public String getUrlFor(String oidcId) {
    return url + oidcId;
  }

  /**
   * Resolves the registry type for a stored issuer.
   *
   * @param issuer the issuer stored on the account
   * @return the matching type, or empty for an issuer this application does not know
   */
  public static java.util.Optional<OidcType> forIssuer(String issuer) {
    return java.util.Arrays.stream(values())
        .filter(type -> type.getIssuer().equals(issuer))
        .findFirst();
  }
}
