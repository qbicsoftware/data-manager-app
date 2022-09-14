package life.qbic.projectmanagement.application;

/**
 * Creates a project containing certain information.
 */
public interface ProjectCreationService {

  /**
   * Create a new project based on the information provided.
   *
   * @param title the title of the project.
   * @return a {@link ProjectCreationResponse} indicating success or failure
   */
  ProjectCreationResponse createProject(String title);

}
