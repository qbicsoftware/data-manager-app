package life.qbic.datamanager.views.projects.project;

/**
 * <b> Interface to handle the {@link ProjectMainLayout} to the {@link ProjectMainHandler}. </b>
 *
 * @since 1.0.0
 */
public interface ProjectMainHandlerInterface {

  /**
   * Registers the {@link ProjectMainLayout} to the implementing class
   *
   * @param layout The view that is being registered
   * @since 1.0.0
   */
  void handle(ProjectMainLayout layout);
}
