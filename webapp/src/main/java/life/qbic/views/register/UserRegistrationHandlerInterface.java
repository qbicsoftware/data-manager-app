package life.qbic.views.register;

/**
 * <b> Interface to handle the {@link UserRegistrationLayout} to the {@link UserRegistrationHandler}. </b>
 *
 * @since 1.0.0
 */
public interface UserRegistrationHandlerInterface {

  /**
   * Registers a {@link UserRegistrationLayout} to an implementing class
   *
   * @param registerLayout The view that is being handled
   * @since 1.0.0
   */
  void handle(UserRegistrationLayout registerLayout);
}
