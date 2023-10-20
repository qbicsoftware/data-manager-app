package life.qbic.controlling.application.authorization;

/**
 * <b>User concept on application security level</b>
 *
 * @since 1.0.0
 */
public record User(String id, String fullName, String emailAddress, String encryptedPassword,
                   boolean isActive) {

}