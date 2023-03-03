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
   * The login page that displays the credential fields to authenticate a user in the data manager
   * app.
   */
  public static final String LOGIN = "login";

  /**
   * The route to set a new password after reset
   */
  public static final String NEW_PASSWORD = "registration/new-password";

  /**
   * The route to enable users to reset their password
   */
  public static final String RESET_PASSWORD = "registration/reset-password";

  /**
   * The route to enable new users to create a new account
   */
  public static final String REGISTER = "registration";

  public static class Projects {

    /**
     * The "Main" page of the data manager app. Location, where you want the user to be directed to,
     * if no explicit path has been provided. Shows all the projects a user has access to.
     */
    public static final String PROJECTS = "";
    /**
     * The alias name for the MAIN route.
     */
    public static final String PROJECTS_ALIAS = "projects/list";

    /**
     * Path to the page that shows a selected project's information
     */
    public static final String PROJECT_INFO = "projects/%s";

    /**
     * Path to create a new experiment within an existing project
     */
    public static final String EXPERIMENT_CREATION = "projects/%s/experiments/create";

    /**
     * Path to list existing project experiments
     */
    public static final String EXPERIMENTS = "projects/%s/experiments";

    /**
     * Path to investigate a specific experiment in a project
     */
    public static final String EXPERIMENT = "projects/%s/experiments/%s";

    /**
     * Path to list registered project samples
     */
    public static final String SAMPLES = "projects/%s/samples";

    /**
     * Path to investigate a specific sample's properties within a project
     */
    public static final String SAMPLE = "projects/%s/samples/%s";

    /**
     * Path to list all measurements within a project
     */
    public static final String MEASUREMENTS = "projects/%s/measurements";

    /**
     * Path to investigate a specific measurement of a project
     */
    public static final String MEASUREMENT = "projects/%s/measurements/%s";

    /**
     * Path to list all performed analyses within a project
     */
    public static final String ANALYSES = "projects/%s/analyses";

    /**
     * Path to investigate the properties of an analysis
     */
    public static final String ANALYSIS = "projects/%s/analyses/%s";

    /**
     * Path to list all attachments within a projects
     */
    public static final String ATTACHMENT = "projects/%s/attachments";

  }
}
