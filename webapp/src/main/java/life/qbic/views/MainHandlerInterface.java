package life.qbic.views;

/**
 * <b> Interface to handle the {@link MainLayout} to the {@link MainHandler}. </b>
 *
 * @since 1.0.0
 */
public interface MainHandlerInterface {

  /**
   * Registers the {@link MainLayout} to the implementing class
   *
   * @param layout The view that is being registered
   * @since 1.0.0
   */
  void handle(MainLayout layout);
}
