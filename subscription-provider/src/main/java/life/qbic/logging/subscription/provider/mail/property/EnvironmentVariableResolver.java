package life.qbic.logging.subscription.provider.mail.property;

/**
 * <b>Environment Variable Resolver</b>
 * <p>
 * Resolves a provided environment variable by name.
 *
 * @since 1.0.0
 */
public class EnvironmentVariableResolver {

  private EnvironmentVariableResolver() {}
  /**
   * Resolves an environment variable by its provided name
   *
   * @param name the name of the environment variable
   * @return the value of the environment variable or <code>null</code> when no environment variable
   * with the given name exists.
   * @since 1.0.0
   */
  public static String resolve(String name) {
    return System.getenv(name);
  }

}
