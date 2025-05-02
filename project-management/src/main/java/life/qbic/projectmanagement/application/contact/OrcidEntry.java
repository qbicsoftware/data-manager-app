package life.qbic.projectmanagement.application.contact;

/**
 * <b>OrcidEntry</b>
 *
 * <p>Contains selected information from publicly available person entries gathered via the orcid
 * API</p>
 */
public record OrcidEntry(String fullName, String emailAddress, String oidc, String oidcIssuer) {

}
