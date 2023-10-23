package life.qbic.controlling.application;

/**
 * <b>App Context Provider</b>
 * <p>
 * Provides some utility methods to create navigation targets within the application.
 *
 * @since 1.0.0
 */
public interface AppContextProvider {

  /**
   * Returns a resolvable URL to the target project resource in the application.
   *
   * @param projectId the project id as the target web resource
   * @return a fully resolvable URL
   * @since 1.0.0
   */

  String urlToProject(String projectId);
  /**
   * Returns a resolvable URL to the target project's sample page resource in the application.
   *
   * @param projectId the project id
   * @return a fully resolvable URL
   * @since 1.0.0
   */
  String urlToSamplePage(String projectId);
}
