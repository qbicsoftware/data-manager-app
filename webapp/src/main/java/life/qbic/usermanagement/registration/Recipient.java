package life.qbic.usermanagement.registration;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public record Recipient(
    String address,
    String fullName) {

  public static Recipient from(String address, String fullName) {
    return new Recipient(address, fullName);
  }
}
