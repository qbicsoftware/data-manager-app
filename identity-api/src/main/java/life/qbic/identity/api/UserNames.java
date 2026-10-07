package life.qbic.identity.api;

/**
 * <b>Username rules shared across context boundaries</b>
 *
 * <p>The username is the user-visible handle shown throughout the application. To keep that handle
 * usable in compact UI contexts it is bounded to {@value #MAX_LENGTH} characters.
 *
 * <p>These constants live in the API module so that both the identity domain (which enforces the
 * rule via its username policy) and client modules such as the UI (which must constrain input
 * fields to the same limit) share a single source of truth instead of duplicating the number.
 *
 * @since 1.20.0
 */
public final class UserNames {

  /**
   * The maximum allowed number of characters of a username.
   */
  public static final int MAX_LENGTH = 20;

  private UserNames() {
    // utility class
  }

  /**
   * Indicates whether the given username exceeds {@link #MAX_LENGTH} characters, ignoring
   * surrounding whitespace.
   *
   * <p>Used to surface grandfathered usernames (created before the limit existed) to their
   * owners without rejecting them. Kept here, alongside {@link #MAX_LENGTH}, so that UI code can
   * evaluate the rule without depending on identity domain types.
   *
   * @param userName the username to inspect
   * @return {@code true} if the value is longer than {@link #MAX_LENGTH}
   * @since 1.20.0
   */
  public static boolean exceedsMaxLength(String userName) {
    return userName != null && userName.strip().length() > MAX_LENGTH;
  }
}
