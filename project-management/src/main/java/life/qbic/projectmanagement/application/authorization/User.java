package life.qbic.projectmanagement.application.authorization;

/**
 * <b>User concept on application security level</b>
 *
 * @since 1.0.0
 */
public record User(String id, String fullName, String platformUserName, String emailAddress,
                   String encryptedPassword,
                   boolean isActive, String oidc, String oidcIssuer) {

}
