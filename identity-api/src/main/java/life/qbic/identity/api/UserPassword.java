package life.qbic.identity.api;

/**
 * An encrypted password belonging to a user
 *
 * @since 1.1.0
 */
public record UserPassword(String userId, String encryptedPassword) {

}
