package life.qbic.user.api;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record UserInfo(String id, String fullName, String emailAddress, String encryptedPassword, boolean isActive) {

}
