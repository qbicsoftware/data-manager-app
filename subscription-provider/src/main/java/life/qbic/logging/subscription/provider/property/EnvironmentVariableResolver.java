package life.qbic.logging.subscription.provider.property;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class EnvironmentVariableResolver {

  public static String resolve(String envVariable) {
    return System.getenv(envVariable);
  }

}
