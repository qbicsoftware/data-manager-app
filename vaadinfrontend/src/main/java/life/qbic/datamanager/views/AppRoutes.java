package life.qbic.datamanager.views;

/**
 * Defines the data manager's public application routes, that shall be visible by all components
 * within the app.
 * <p>
 * Local routes of components should be listed here with care!
 *
 * @since 1.0.0
 */
public class AppRoutes {

  /**
   * The "Main" page of the data manager app. Location, where you want the user to be directed to,
   * if no explicit path has been provided. Shows all the projects a user has access to.
   */
  public static final String PROJECTS = "";

  /**
   * The alias name for the MAIN route.
   */
  public static final String PROJECTS_ALIAS = "projects";

  /**
   * The login page that displays the credential fields to authenticate a user in the data manager
   * app.
   */
  public static final String LOGIN = "login";

  /**
   * The route to set a new password after reset
   */
  public static final String NEW_PASSWORD = "new-password";

  /**
   * The route to enable users to reset their password
   */
  public static final String RESET_PASSWORD = "reset-password";

  /**
   * The route to enable new users to create a new account
   */
  public static final String REGISTER = "register" ;
}
